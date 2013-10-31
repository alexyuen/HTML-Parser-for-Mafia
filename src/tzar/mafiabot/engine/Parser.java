package tzar.mafiabot.engine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import tzar.mafiabot.gui.MafiaView;


public class Parser {
	private static final Pattern pageOfPattern = Pattern.compile("Page\\s(\\d+)\\sof\\s(\\d+)"); //ie Page 1 of 2
	private static final Pattern postNumberPattern = Pattern.compile(".*\\Wp=?(\\d+).*");
	private static final Pattern commandsPattern = Pattern.compile("##\\s?\\w+[^#]*");

	private static final Pattern dayNightHack = Pattern.compile("(?i)(day|night)\\s*(\\d+)");

	private final String thread, posts, author, post_content, bold_commands, next_button, post_edit;

	private HashSet<String> gms = new HashSet<String>();
	private Actors actors;

	private int day = 0, night = 0;
	private boolean isNight = false;
	private HashSet<String> delayedKill = new HashSet<String>();

	private File cacheFile = null;
	private boolean readingFromCache = false;
	private Elements newPosts = new Elements();

	private boolean hasPlayersList = false;
	
	private MafiaView view = null;

	public Parser(String thread, MafiaView view) {
		this.thread = thread;
		this.cacheFile = new File("MafiaBot-" + thread.hashCode() + ".cache");
		this.view = view;
		
		if (thread.contains("bluehell")) {
			posts = "div.post_block";
			author = "div.post_username";
			post_content = "div.post";
			bold_commands = "strong.bbc:not(.blockquote strong):not(strong .blockquote):matches(" + commandsPattern.pattern() + ")";
			next_button = "a[rel=next]";
			post_edit = "p.edit";
		} else if (thread.contains("mlponies") || thread.contains("roundstable")) {
			posts = "div.post";
			author = "a[href^=./memberlist]";
			post_content = "div.content";
			bold_commands = "span[style$=bold]:not(blockquote span):matchesOwn(" + commandsPattern.pattern() + ")";
			next_button = "a:matchesOwn(^Next$)"; // makes sure it matches exactly on "Next" so it doesn't go to "Next Topic"
			post_edit = "div.notice";
		} else if (thread.contains("eridanipony")) {
			posts = "table.tablebg:has(div.postbody)";
			author = "b.postauthor";
			post_content = "div.postbody";
			bold_commands = "strong:not(div.quotecontent strong):not(strong div.quotecontent):matches("+ commandsPattern.pattern() +")";
			next_button = "a:matchesOwn(^Next$)";
			post_edit = "span.gensmall";
		} else {
			posts = author = post_content = bold_commands = next_button = post_edit = null;
			System.out.println("ERROR: The specified forum " + thread + " is not yet supported.");
			view.parseCompleted();
		}
	}

	public void start() {
		readingFromCache = cacheFile.exists();

		try {
			// time the parse execution
			long startTime = System.currentTimeMillis();
			parse(thread);
			long endTime = System.currentTimeMillis();
			System.out.println("(Parse completed in " + (endTime - startTime) / 1000L + " seconds.)");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("An error has occured while parsing: " + e.toString());
			if (readingFromCache)
				System.out.println("Try deleting the cache and reparsing to see if this error occurs again.");
		}

		// parsing finished; display the vote and post count
		if (actors != null) {
			actors.printVoteCount("Current vote count " + (isNight ? "(Night " + night : "(Day " + day) + "):", hasPlayersList);
			if (day > 0) {
				actors.printPostCount(day);
			}
		}

		if (day > 0) {
			// write the new posts to the cache if the game has started
			try {
				if (newPosts.size() > 1) {
					// don't add the last post to the cache in case of mafia edits (BHP)
					newPosts.remove(newPosts.size() - 1);

					FileWriter fstream = new FileWriter(cacheFile, true);
					BufferedWriter out = new BufferedWriter(fstream);
					// write the other new posts
					for (Element post : newPosts) {
						out.write(post.outerHtml());
					}
					out.close();
				}
				System.err.println("Added " + newPosts.size() + " new posts to the cache.");
			} catch (Exception e) { 
				e.printStackTrace();
				System.out.println("An error has occured while writing to the cache: " + e.toString());
				System.out.println("Try deleting the cache and reparsing to see if this error occurs again.");
			}
		} else {
			System.err.println("A cache file was not generated because the game has not yet started.");
		}

		view.parseCompleted();
	}

	private void parse(String url) throws Exception {
		System.err.println(url);
		//load the page
		final Element thisThreadPage = (readingFromCache ? Jsoup.parse(cacheFile, null) : Jsoup.connect(url).timeout(20000).get());

		// update the GUI progress bar
		//Element progress = thisThreadPage.select("a[href=#]:matchesOwn("+pageOfPattern.pattern()+")").first();
		final Element progress = thisThreadPage.select("a[href=#]:matches("+pageOfPattern.pattern()+"), td.nav:matches("+pageOfPattern.pattern()+")").first();
		if (progress != null) {
			System.out.printf("%n" + progress.text().replace(" ", "") + "%n"); // Print out the current page
			Matcher pageOfMatcher = pageOfPattern.matcher(progress.text());
			if (pageOfMatcher.find()) {
				double current = Double.parseDouble(pageOfMatcher.group(1));
				double end = Double.parseDouble(pageOfMatcher.group(2));
				//System.out.println(current + " " + end);
				view.setProgress((int) (current / end * 100));
			}
		} else if (!readingFromCache) {
			view.setProgress(100);
		} else {
			// successfully opened the cache file
			view.setProgress(1);
		}

		// check to see if we are jumping posts
		// this is used after reading the cache to jump to the next post in the thread
		final Matcher postNumberMatcher = postNumberPattern.matcher(url);

		// get every post
		final Elements allThePostsOnThisPage = thisThreadPage.select(posts);
		// parse each post (post = the entire post, including avatar, heading, and signature)
		for (final Element post : allThePostsOnThisPage) {
			if (view.isStopped()) {
				// Stop execution if stopped button was pressed
				break;
			}

			// check if jumping posts
			if (postNumberMatcher.matches()) {
				String currentPostURL = post.select("a[href~="+ postNumberPattern.pattern() + "]:not("+post_content+" a)").last().absUrl("href");
				//System.out.println(currentPostURL);
				Matcher currentPostNumberMatcher = postNumberPattern.matcher(currentPostURL);
				if (currentPostNumberMatcher.matches() && 
						Integer.parseInt(currentPostNumberMatcher.group(1)) <= Integer.parseInt(postNumberMatcher.group(1))) {
					//System.out.println(currentPostNumberMatcher.group(1) + " " + postNumberMatcher.group(1));
					// if the current post number <= post number in the URL, jump to the next post
					continue;
				}
			}

			//System.out.println(post.outerHtml()); // Displays the post HTML for debugging

			// if reading from the thread and this post is not the last post in the thread, append it to the list of new posts 
			if (!readingFromCache) {
				newPosts.add(post);
			}
			// store the new day number here to advance the day at the end of the post
			int newDay = 0, newNight = 0;

			final String poster = post.select(author).first().text().trim();
			
			Element postLink = post.select("a[href~="+ postNumberPattern.pattern() + "]:not("+post_content+" a)").last();
			postLink.setBaseUri(thread);
			String postURL = postLink.absUrl("href");

			final Element postContent = post.select(post_content).first();
			final Elements allBoldTagsInThisPost = postContent.select(bold_commands);

			// First poster is automatically granted GM permissions
			if (gms.isEmpty()) {
				addGM(poster);
			}

			// try to determine phase change if the GM does not use any of the ## commands at all
			if (!hasPlayersList) {
				if (gms.contains(poster)) {
					// look at all elements in the post that contain "day \d+" or "night \d+"
					next:
					for (Element nextDay : postContent.getElementsMatchingOwnText(dayNightHack.pattern())) {
						Matcher m = dayNightHack.matcher(nextDay.text());
						while (m.find()) {
							// a new phase was found
							if (actors == null) {
								// if there was no actors yet, create one
								actors = new Actors();
							}
							System.err.println(nextDay.text());
							int num = Integer.parseInt(m.group(2));
							if (m.group(1).equalsIgnoreCase("day") && num > day) {
								newDay = num;
							} else if (m.group(1).equalsIgnoreCase("night") && num > night) {
								newNight = num;
							}
							break next;
						}
					}
				} else if (actors != null) {
					actors.addPlayer(poster);
				}
			}
			// find all the bold tags in this post
			for (Element aBoldTag : allBoldTagsInThisPost) {
				// search for multiple commands within the same bold tag
				Matcher multipleCommands = commandsPattern.matcher(aBoldTag.text());
				while (multipleCommands.find()) {
					String action = multipleCommands.group().trim();

					// split the input string into a array of two strings; element 1 is the ##command, element 2 contains the parameters
					String[] tokens = action.split(" ", 2);
					String command = tokens[0].toLowerCase();
					String parameter = (tokens.length > 1 ? tokens[1] : null);
					
					// support for ## commands (has a space between ## and the command)
					if (command.equals("##")) {
						tokens = action.split(" ", 3);
						command = command + tokens[1].toLowerCase();
						parameter = (tokens.length > 2 ? tokens[2] : null);
					}

					// Player commands
					if (actors != null) {
						if (command.matches("##\\s?vote") && parameter != null) {
							/*
							if (day > 0 && !hasPlayersList) {
								actors.addPlayer(parameter);
							}
							*/
							actors.vote(poster, parameter);
							continue;
						} else if (command.matches("##\\s?unvote")) {
							actors.unvote(poster);
							continue;
						} else if (!gms.contains(poster)) {
							//System.out.println(command);
							System.out.println("(!) " + poster + " used action: " + action + " --> " + postURL);
							continue;
						}
					} else if (!gms.contains(poster)) {
						System.out.println("(!) Ignored action \"" + action + "\" from " + poster + " due to lack of ##players list.");
						continue;
					}

					// GM commands
					// possibly rewrite to switch statement?
					if (gms.contains(poster)) {
						if (parameter == null) {
							// all commands with no required parameters go here
							if (command.equals("##purgevotes")) {
								actors.clearVotes();
							} else {
								System.out.println("(!) Error: No parameters were given for command " + command);
							}
						} else if (command.equals("##players") && !hasPlayersList) {
							//System.out.printf("Players list found: ");
							// get the html of the ##players list
							TextNode t = TextNode.createFromEncoded(aBoldTag.html(), "aURI");
							// make an array with each element containing a player name
							String[] playerList = t.getWholeText().split("<br />\\s*");
							// create the internal player list using the array
							actors = new Actors(Arrays.copyOfRange(playerList, 1, playerList.length));
							hasPlayersList = true;
						} else if (command.equals("##gm")) {
							addGM(parameter);
						} else if (command.equals("##removegm")) {
							removeGM(parameter);
						} else if (command.matches("##((day)|(night))")) {
							if (actors == null) {
								System.out.println("A player list was not found! Grabbing players as we go along...");
								actors = new Actors();
								hasPlayersList = false;
							} else {
								hasPlayersList = true;
							}
							// support for advancing the day immediately
							String[] multipleParams = parameter.split(" ");
							if (multipleParams.length > 1 && multipleParams[1].trim().equalsIgnoreCase("NOW")) {
								if (command.equals("##day")) {
									newPhase(true, Integer.parseInt(multipleParams[0]));
								} else {
									newPhase(false,Integer.parseInt(multipleParams[0]));
								}
							} // otherwise end the day after all commands in this post have been resolved
							else if (command.equals("##day")) {
								// advance the day at the end of this post
								newDay = Integer.parseInt(multipleParams[0]);
							} else {
								newNight = Integer.parseInt(multipleParams[0]);
							}
						} else if (actors == null) {
							System.out.println("(!) Ignored action \"" + action + "\" from " + poster + " due to lack of ##players list.");
							continue;
						} else if (command.matches("##add(player)?")) {
							actors.addPlayer(parameter);
						} else if (command.matches("##remove((player)|(metaclass)|(npc))?")) {
							actors.removePlayer(parameter);
						} else if (command.matches("##add((metaclass)|(npc))")) {
							actors.addNpc(parameter);
						} else if (command.matches("##takevote")) {
							actors.takeVote(parameter);
						} else if (command.matches("##givevote")) {
							actors.giveVote(parameter);
						} else if (command.equals("##strikevote")) {
							actors.unvote(parameter);
						} else if (command.equals("##pardon")) {
							actors.pardon(parameter);
						} else if (command.equals("##proxyvote")) {
							String[] params = parameter.split(" ");
							if (params.length == 2) {
								actors.vote(params[0], params[1]); 
							} else if (params.length > 2) {
								// TODO: need to handle case where voter/candidate names are multiple words (use quotation marks?)
								actors.vote(params[0], params[1]); 
							} else {
								System.out.println("(x) Proxyvote failed: Please specify someone to vote for.");
							}
						} else if (command.equals("##lynch")) {
							delayedKill.add(parameter);
						} else if (command.equals("##nk")) {
							delayedKill.add(parameter);
						} else if (command.equals("##tk") || command.equals("##kill")) {
							String name = actors.kill(parameter, day);
							if (name != null) {
								System.out.println(name + " was killed.");
							}
						} else if (command.equals("##suicide")) {
							String name = actors.kill(parameter, day);
							if (name != null) {
								System.out.println(name + " suicided.");
							}
						} else if (command.equals("##resurrect")) {
							actors.resurrect(parameter);
						} else if (command.equals("##nightposts")) {
							// TODO: implement counting nightposts
						} else {
							System.out.println("(!) Unrecognized GM command: " + action + " --> " + postURL);
						}
					}
					// next command in the same bold tag
				}
				// next command in another bold tag
			}
			if (day > 0) {
				//System.err.println(postContent.ownText());
				actors.addPost(poster, day, postContent.ownText().length());
			}
			if (newNight > 0) {
				newPhase(false, newNight);
			}
			if (newDay > 0) {
				newPhase(true, newDay);
			}
			if (!gms.contains(poster) && !post.select(post_edit).isEmpty()) {
				// print out warning that the poster edited their post!
				System.out.println("(!!) " + poster + " edited their post! --> " + postURL);
			}
			// next post
		}

		// check if the stop button has been pressed
		if (!view.isStopped()) {
			if (readingFromCache) {
				// finished parsing the cache; load the thread to check for new posts
				readingFromCache = false;
				view.setProgress(2);
				Element linkToLastPost = allThePostsOnThisPage.last().select("a[href~="+ postNumberPattern.pattern() + "]:not("+post_content+" a)").last();
				linkToLastPost.setBaseUri(thread);
				parse(linkToLastPost.absUrl("href"));
			} else {
				// parse the next page
				// Get the button that opens the next page.
				Element nextButton = thisThreadPage.select(next_button).last();
				if (nextButton != null)
					parse(nextButton.absUrl("href"));
			}
		}
		// otherwise do nothing
	}

	private void delayedKill() {
		for (String player : delayedKill) {
			// added 1 to the day of death because the player was actually alive today
			String name = actors.kill(player, day + 1);
			if (name != null) {
				if (!isNight) {
					System.out.println(name + " was lynched.");
				} else {
					System.out.println(name + " was nightkilled.");
				}
			}
		}
		delayedKill.clear();
	}

	private void newPhase(boolean isDay, int num) {
		actors.printVoteCount("End of " + (isNight ? "Night " + night : "Day " + day) + " vote count:", hasPlayersList);
		delayedKill();
		actors.clearVotes();
		if (isDay) {
			day = num;
			isNight = false;
			view.setPhase("Day " + day);
			actors.printPlayers();
		} else {
			night = num;
			isNight = true;
			view.setPhase("Night " + num);
		}
	}


	private void addGM(String name) {
		if (gms.add(name)) {
			System.out.println(name + " was granted GM permissions. Current GMs: " + gms.toString());
		}
	}

	private void removeGM(String name) {
		if (gms.remove(name)) {
			System.out.println(name + " had their GM permissions revoked. Current GMs: " + gms.toString());
		}
	}

}
