package tzar.mafiabot.engine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import tzar.mafiabot.gui.MafiaView;


public class Parser {
	private static Pattern pageOfPattern = Pattern.compile("Page\\s(\\d+)\\sof\\s(\\d+)"); //ie Page 1 of 2
	private static Pattern postNumberPattern = Pattern.compile(".*\\W(p=?|entry)(\\d+).*");
	private static Pattern commandsPattern = Pattern.compile("##\\s?\\w+[^#]*");

	private static final Pattern dayNightHack = Pattern.compile("(?i)\\b(day|night)\\s+(\\d+|\\w{3,})"); // |\\w{3,}

	private final String thread, posts, author, post_content, bold_commands, next_button, post_edit;

	private HashSet<String> gms = new HashSet<String>();
	private Actors actors;

	private File cacheFile = null;
	private boolean readingFromCache = false;
	private Elements newPosts = new Elements();

	private boolean hasPlayersList = false;

	private MafiaView view = null;

	private Cycle cycle = new Cycle();

	public Parser(String thread, MafiaView view) {
		this.thread = thread;
		this.cacheFile = new File("MafiaBot-" + thread.hashCode() + ".cache");
		this.view = view;

		if (thread.contains("bluehell") || thread.contains("w3dhub")) {
			posts = "div.post_block";
			author = "span.author.vcard";
			post_content = "div.post_body";
			bold_commands = "strong:not(blockquote strong):not(strong blockquote):matches(" + commandsPattern.pattern() + ")";
			next_button = "a[rel=next]";
			post_edit = "p.edit";
			//postNumberPattern = Pattern.compile(".*#entry(\\d+).*");
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
			System.out.println();
			System.out.println("Parse completed in " + (endTime - startTime) / 1000L + " seconds.");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("(!) An error has occured while parsing: " + e.toString());
			if (readingFromCache)
				System.out.println("Try deleting the cache and reparsing to see if this error occurs again.");
		}

		// parsing finished; display the vote and post count
		if (actors != null) {
			actors.printVoteCount("Current vote count (" + cycle.toString() + "):");
			if (cycle.today() > 0) {
				actors.printPostCount(cycle.today());
			}
		}

		if (cycle.today() > 0) {
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
				System.out.println("(!) An error has occured while writing to the cache: " + e.toString());
				System.out.println("(!) Try deleting the cache and reparse to see if this error occurs again.");
			}
		} else {
			System.err.println("A cache file was not generated because the game has not yet started.");
		}

		view.parseCompleted();
	}

	private void parse(String url) throws Exception {
		System.err.println(url);
		//load the page
		final Element thisThreadPage = (readingFromCache ? Jsoup.parse(cacheFile, null) : Jsoup.connect(url).userAgent("Mozilla").timeout(10000).get());

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
						Integer.parseInt(currentPostNumberMatcher.group(2)) <= Integer.parseInt(postNumberMatcher.group(2))) {
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

			final String poster = post.select(author).first().text().trim();
			//System.out.println(poster);

			Element postLink = post.select("a[href~="+ postNumberPattern.pattern() + "]:not("+post_content+" a)").last();
			postLink.setBaseUri(thread);
			String postURL = postLink.absUrl("href");

			final Element postContent = post.select(post_content).first();
			final Elements allBoldTagsInThisPost = postContent.select(bold_commands);

			// try to determine phase change if the GM does not use any of the ## commands at all
			if (isGM(poster)) {
				// look at all elements in the post that contain "day \d+" or "night \d+"
				for (Element nextDay : postContent.getElementsMatchingOwnText(dayNightHack.pattern())) {
					//System.err.println(nextDay.text());
					Matcher m = dayNightHack.matcher(nextDay.text());
					if (m.find()) {
						// a new phase was found
						if (actors == null) {
							System.out.println("Grabbing players as we go along...");
							actors = new Actors();
						}
						int num;
						try {
							num = Integer.parseInt(m.group(2));
						} catch (NumberFormatException e) {
							// try converting to number from word
							HashMap<String,Integer> map = new HashMap<String,Integer>();
							map.put("one",1); map.put("two",2);  map.put("three",3);  map.put("four",4);
							map.put("five",5);  map.put("six",6);  map.put("seven",7);  map.put("eight",8);
							map.put("nine",9);  map.put("ten",10);  map.put("eleven",11);  map.put("twelve", 12);
							map.put("thirteen",13);  map.put("fourteen",14);  map.put("fifteen",15);  map.put("sixteen",16);
							if (map.get(m.group(2).toLowerCase()) != null) {
								num = map.get(m.group(2).toLowerCase());
							} else { // not a number
								continue;
							}
						}
						if (m.group(1).equalsIgnoreCase("day") && num > cycle.today()) {
							cycle.setNextDay(num);
						} else if (m.group(1).equalsIgnoreCase("night") && num > cycle.tonight()) {
							cycle.setNextNight(num);
						}
					}
				}
			} else if (actors != null && !hasPlayersList) {
				actors.addPlayer(poster);
			}

			// First poster is automatically granted GM permissions
			if (gms.isEmpty()) {
				addGM(poster);
			}

			// find all the bold tags in this post
			for (Element aBoldTag : allBoldTagsInThisPost) {
				// search for multiple commands within the same bold tag
				Matcher multipleCommands = commandsPattern.matcher(aBoldTag.text());
				while (multipleCommands.find()) {
					String action = multipleCommands.group().trim().replaceFirst("##\\s+", "##");

					// split the input string into a array of two strings; element 1 is the ##command, element 2 contains the parameters
					String[] tokens = action.split(" ", 2);
					String command = tokens[0].toLowerCase();
					String args = (tokens.length > 1 ? tokens[1] : null);

					// Player commands
					if (actors != null) {
						if (command.equals("##vote") && args != null) {
							/*
							if (day > 0 && !hasPlayersList) {
								actors.addPlayer(parameter);
							}
							 */
							actors.vote(poster, args);
							continue;
						} else if (command.equals("##unvote")) {
							actors.unvote(poster);
							continue;
						} else if (!isGM(poster)) {
							//System.out.println(command);
							System.out.println("(!) " + poster + " used action: " + action + " --> " + postURL);
							continue;
						}
					} else if (!isGM(poster)) {
						System.out.println("(!) Ignored action \"" + action + "\" from " + poster + " due to lack of ##players list.");
						continue;
					}

					// GM commands

					if (isGM(poster)) {

						switch (command) {
						// all commands with no required parameters go here
						case "##purgevotes":
							if (actors != null) {
								actors.clearVotes();
							}
							continue;
							
						default:	
						}

						if (args == null) {
							System.out.println("(!) No parameters were given for command " + command);
							continue;
						}

						switch (command) {
						case "##players":
							// get the html of the ##players list
							TextNode t = TextNode.createFromEncoded(aBoldTag.html(), "aURI");
							System.out.print(t.text().toString());
							// make an array with each element containing a player name
							String[] playerList = t.text().split("<br>\\s*");
							// create the internal player list using the array
							actors = new Actors(Arrays.copyOfRange(playerList, 1, playerList.length));
							hasPlayersList = true;
							continue;

						case "##gm":
							addGM(args);
							continue;

						case "##removegm":
							removeGM(args);
							continue;

						case "##day":
						case "##night":
							// functionality moved,
							// purposely empty for backwards compatibility
							continue;
							
						default:
						}

						if (actors == null) 
							continue;

						switch (command) {
						case "##addplayer":
							actors.addPlayer(args);
							break;

						case "##addmetaclass":
						case "##addnpc":
							actors.addNpc(args);
							break;

						case "##removeplayer":
						case "##removemetaclass":
						case "##removenpc":
							actors.removePlayer(args);
							break;

						case "##takevote":
							actors.takeVote(args);
							break;

						case "##givevote": 
							actors.giveVote(args);
							break;

						case "##setvoteweight":
							Matcher voteWeight = Pattern.compile("(\\.+)\\s+(\\d+)").matcher(args);
							if (voteWeight.matches()) {
								int num = Integer.parseInt(voteWeight.group(2));
								actors.setVoteWeight(voteWeight.group(1), num);
							} else {
								System.out.println("(!) Usage: ##setvoteweight <player> <num>");
							}
							break;

						case "##setmultivote":
							Matcher multivote = Pattern.compile("(\\.+)\\s+(\\d+)").matcher(args);
							if (multivote.matches()) {
								int num = Integer.parseInt(multivote.group(2));
								actors.setMultiVote(multivote.group(1), num);
							} else {
								System.out.println("(!) Usage: ##setmultivote <player> <number of votes>");
							}
							break;

						case "##strikevote":
							actors.unvote(args);
							break;

						case "##pardon":
							actors.pardon(args);
							break;

						case "##proxyvote":
							String[] proxy = args.split(" ");
							if (proxy.length == 2) {
								actors.vote(proxy[0], proxy[1]); 
							} else if (proxy.length > 2) {
								//need to improve case where voter & candidate names are multiple words
								actors.vote(proxy[0], proxy[proxy.length - 1]); 
							} else {
								System.out.println("(x) Proxyvote failed: Please specify someone to vote for.");
							}
							break;

						case "##lynch":
						case "##nk":
							cycle.kill(args);
							break;

						case "##tk":
						case "##kill":
						case "##suicide":
							String name = actors.kill(args, cycle.today());
							if (name != null) {
								System.out.println(name + " was killed.");
							}
							break;

						case "##resurrect":
						case "##revive":
							actors.resurrect(args);
							break;
						default:
							System.out.println("(!) Unrecognized GM command: " + action + " --> " + postURL);
						}

					}
					// next command in the same bold tag
				}
				// next command in another bold tag
			}
			if (cycle.today() > 0) {
				//System.err.println(postContent.ownText());
				actors.addPost(poster, cycle.today(), postContent.ownText().length());
			}
			cycle.advance(actors, view);

			if (!isGM(poster) && !post.select(post_edit).isEmpty()) {
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

	private boolean isGM(String name) {
		return gms.contains(name);
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
