package tzar.mafiabot.engine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import tzar.mafiabot.util.FuzzyMatch;


public class Actors {
	/**
	 * This TreeSet keeps track of all the (unique) Player objects in the below TreeMap.
	 * The TreeSet sorts its Player elements by name, in ascending order 
	 */
	// TODO find a way to extract all unique values from the TreeMap so I can get rid of this
	private TreeSet<Player>	uniquePlayers = new TreeSet<Player>();

	/** 
	 * This TreeMap is used to associate all aliases with their respective Player objects.
	 * Each alias is a key in the TreeMap, with the Player object as its value 
	 * For example, <"alias1", Player1>, <"alias2", Player1>
	 **/
	private TreeMap<String, Player> aliasPlayerMap = new TreeMap<String, Player>(String.CASE_INSENSITIVE_ORDER);

	/**
	 * This is the Element for the root of the aliases XML document
	 */
	private Element aliasDocument = null;

	/**
	 * Creates a new Actors object with default NPCs and no players
	 */
	public Actors() {
		try {
			aliasDocument = Jsoup.parse(getClass().getClassLoader().getResourceAsStream("aliases.xml"), null, "aURI", Parser.xmlParser());
		} catch (IOException e) {
			System.err.println("Error reading aliases.xml. Continuing execution without aliases.");
			aliasDocument = new Element(null, null);
		}
		addDefaultNpcs();
	}

	/**
	 * Creates a new Actors object with default NPCs and the given players
	 * @param players An array of names corresponding to the people who are playing
	 */
	public Actors(String[] players) throws Exception {
		this();

		// add players and their aliases to the hash table
		System.out.println();
		for (String name : players) {
			addPlayer(name);
		}
		System.out.println();
		//System.out.printf("%nPlayers (%d): %s%n", uniquePlayers.size(), uniquePlayers.toString());
		//System.out.printf("Aliases loaded (%d): %s%n%n", aliasPlayerMap.size(), aliasPlayerMap.toString());
	}

	/**
	 * Adds default npcs (such as No Vote) to the game so they can be voted for.
	 */
	private void addDefaultNpcs() {
		for (Element npcElement : aliasDocument.getElementsByTag("npc")) {
			String[] names = npcElement.text().split(";");
			Player npc = new Player(names[0]);
			npc.setVoteEligibility(false, true);
			npc.setCountVisiblity(false, false);

			uniquePlayers.add(npc);
			for (String name : names) {
				aliasPlayerMap.put(name.trim(), npc);
			}
		}
	}

	public void addNpc(String name) {
		Player npc = new Player(name);
		npc.setVoteEligibility(false, true);
		npc.setCountVisiblity(false, false);

		if (uniquePlayers.add(npc)) {
			aliasPlayerMap.put(name.trim(), npc);
			System.out.println("Added vote option " + name + " to the game.");
		} else {
			System.out.println("Vote option " + name + " is already in the game.");
		}
	}

	public void addPlayer(String name) {
		// check if a player with this exact name/alias already exists
		Player player = aliasPlayerMap.get(name);
		if (player != null) {
			return;
		}
		// otherwise, create a new player object for this player
		player = new Player(name);
		// add the given name to the aliases
		aliasPlayerMap.put(name, player);
		// add the player object to the set of unique players
		uniquePlayers.add(player);
		System.out.println(player.getName() + " was added to the game.");

		// check the aliases.xml for matches/close matches and import other aliases
		String matchedName = name;
		double matchedPercent = 0;
		String[] matchedLine = null;
		// look at every alias element in the aliases xml
		for (Element aliasesList : aliasDocument.getElementsByTag("alias")) {
			// compare the given name to each name in the alias element
			String[] aliases = aliasesList.text().split(";");
			for (String alias : aliases) {
				// use fuzzy search on the names in the aliases.xml to import aliases even if the name is misspelled slightly
				alias = alias.trim();
				double result = FuzzyMatch.levenshteinDistanceNormalized(name.toLowerCase(), alias.toLowerCase());
				//System.out.println(name + " " + result);
				if (result > matchedPercent && result >= 0.5) {
					matchedLine = aliases;
					matchedPercent = result;
					matchedName = alias;
					break;
				}
			}
		}

		if (matchedLine != null) {
			// aliases found
			
			// put aliases into map with the player object as its value
			for (String aliasFound : matchedLine) {
				aliasPlayerMap.put(aliasFound.trim(), player);
			}
			
			if (matchedPercent != 1) {
				System.out.println("(?) Perhaps you meant " + matchedName + " instead of " + name + "?");
			}
			System.out.println("Imported aliases for " + player.getName() + ": " + Arrays.asList(matchedLine).toString());
		}
	}

	public void removePlayer(String name) {
		Player p = getPlayerFuzzy(name);
		if (p != null) {
			p.unvote();
			p.pardon();
			uniquePlayers.remove(p);
			aliasPlayerMap.remove(name);

			// remove his other aliases from the map
			Element alias = aliasDocument.getElementsMatchingOwnText(name).first();
			if (alias != null) {
				for (Element key : alias.siblingElements()) {
					aliasPlayerMap.remove(key.text());
				}
			}
			System.out.println(p.getName() + " was removed from the game.");
		}
	}

	public void vote(String voterName, String candidateName) {
		// check if the voter is playing and alive
		Player voter = getPlayerFuzzy(voterName);
		if (voter == null) {
			System.out.printf("(x) Rejected spectator vote from \"%s\" on \"%s\".%n", voterName, candidateName);
		} else if (!voter.isAlive()) {
			System.out.printf("(x) Rejected dead vote from \"%s\" on \"%s\".%n", voter, candidateName);
		} else if (!voter.canVote()) {
			System.out.println("Ignored vote from " + voter.getName() + ".");
		} else {
			Player candidate = getPlayerFuzzy(candidateName);
			if (candidate == null) { // could not find player
				System.out.printf("(x) Rejected vote from \"%s\" on unknown player \"%s\".%n", voter, candidateName);
			} else if (!candidate.isAlive()) {
				System.out.printf("(x) Rejected vote from \"%s\" on dead player \"%s\".%n", voter, candidate);
			} else if (!voter.isVoting(candidate)) {
				voter.vote(candidate);

				String output = String.format("%s voted for %s", voter, candidateName);
				if (!candidate.getName().equalsIgnoreCase(candidateName)) {
					output = output + " (" + candidate.getName() + ")";
				}
				if (candidate.getTotalVotes() >= getPlayersAlive().size() / 2 + 1) {
					output = "(!) " + output + ". HAMMER!!"; 
				}
				System.out.println(output);
			}
		}
	}

	public void unvote(String voter) {
		Player v = aliasPlayerMap.get(voter);
		if (v != null && v.isVoting()) {
			v.unvote();
			System.out.printf("%s unvoted%n", voter);
		}
	}

	public void clearVotes() {
		for (Player p : uniquePlayers) {
			p.pardon();
		}
		System.out.println("\nAll votes have been cleared.\n");
	}

	public void pardon(String name) {
		Player p = getPlayerFuzzy(name);
		if (p != null) {
			p.pardon();
			System.out.println(p.getName() + " was pardoned.");
		} else {
			System.out.println("(x) Pardon failed: " + name + " was not found."); 
		}
	}

	public String kill(String name, int dayOfDeath) {
		Player p = getPlayerFuzzy(name);
		if (p != null) {
			if (p.isAlive()) {
				p.pardon();
				p.unvote();
				p.kill(dayOfDeath);
				return p.getName();
			} else {
				System.out.println("(x) Kill failed: " + p.getName() + " is already dead.");
			}
		} else {
			System.out.println("(x) Kill failed: " + name + " was not found.");
		}
		return null;
	}

	public void resurrect(String name) {
		Player p = getPlayerFuzzy(name);
		if (p != null) {
			if (!p.isAlive()) {
				p.resurrect();
				System.out.println(p.getName() + " was resurrected.");
			} else {
				System.out.println("(x) Resurrect failed: " + p.getName() + " is already alive. "); 
			}
		} else { 
			System.out.println("(x) Resurrect failed: " + name + " was not found.");
		}
	}

	public void takeVote(String name) {
		Player p = getPlayerFuzzy(name);
		if (p != null) {
			p.setVoteEligibility(false, true);
			System.out.println("Removed " + p.getName() +"'s ability to vote!");
		} else {
			System.out.println("(x) Take vote failed: " + name + " was not found.");
		}
	}

	public void giveVote(String name) {
		Player p = getPlayerFuzzy(name);
		if (p != null) {
			p.setVoteEligibility(true, true);
			System.out.println("Reinstated " + p.getName() +"'s ability to vote!");
		} else {
			System.out.println("(x) Give vote failed: " + name + " was not found.");
		}
	}

	public void setVoteWeight(String name, int weight) {
		Player p = getPlayerFuzzy(name);
		if (p == null) {
			System.out.println("(x) Set vote weight failed: " + name + " was not found.");
		} else {
			p.setVotePrestige(p.getVoteNum(), weight);
			System.out.println("Set " + p.getName() + "'s vote weight to " + weight);
		}
	}

	public void setVoteNum(String name, int num) {
		Player p = getPlayerFuzzy(name);
		if (p == null) {
			System.out.println("(x) Set vote num failed: " + name + " was not found.");
		} else {
			p.setVotePrestige(num, p.getVoteWeight());
			System.out.println(p.getName() + " now has " + num + " votes.");
		}
	}


	public void addPost(String name, int day, int length) {
		Player p = aliasPlayerMap.get(name);
		if (p == null) {
			p = getPlayerFuzzy(name);
		} 
		if (p != null) {
			p.addPost(day, length);
		}
	}

	/**
	 * Prints the post count. 
	 * It will be displayed as a table, with the rows corresponding to each player, and columns indicating the day
	 * @param day The number of days (columns) to display.
	 */
	public void printPostCount(int day) {
		System.out.printf("%n[code]%n");
		System.out.println("Post count:");
		// determine longest name for prettier output
		int longestName = 0;
		for (Player p : uniquePlayers) {
			if (p.getName().length() > longestName) {
				longestName = p.getName().length();
			}
		}
		longestName += 3;

		// print out the column header first
		String header = String.format("%-" + longestName + "s", " ");
		for (int i = 1 ; i <= day; i++) {
			header += String.format("[%3s] ", "D" + i);
		}
		header += "[Total]";
		//header.append("[Total] [Avg Words/Post]");
		System.out.println(header.toString());
		// have to use an arraylist to sort by an unnatural order or else players with equal posts would be discarded
		ArrayList<Player> playersSortedByPosts = asSortedList(uniquePlayers, new SortByPosts());
		for (Player p : playersSortedByPosts) {
			if (p.showInPostCount() || p.getTotalPosts() > 0) {
				// make sure the post count has a value for the current day
				ArrayList<Integer> postCount = p.getPostCount();
				while (postCount.size() <= day) {
					postCount.add(0);
				}
				// print name
				String result = String.format("%-" + longestName + "s", p.getName() + ":");
				for (int i = 1; i <= day; i++) {
					// print the post count for each day
					result += String.format("[%3d] ", postCount.get(i));
				}
				// print total posts
				result += String.format("[%4d] ", p.getTotalPosts());
				//result += String.format("[%4d] ", p.getAvgPostLength());
				System.out.println(result);
			}
		}
		System.out.printf("[/code]%n%n");
	}

	/**
	 * Prints the vote count.
	 * @param header Additional information to display before the vote count is printed
	 * @param hasPlayerList Has the GM provided a player list?
	 */
	public void printVoteCount(String header, boolean hasPlayerList) {
		// print the header
		System.out.printf("%n[code]%n" + header + "%n");

		// get a list of all players alive
		ArrayList<Player> allPlayersAlive = getPlayersAlive();

		// sort the players by decreasing number of votes
		ArrayList<Player> allPlayersSorted = asSortedList(uniquePlayers, new SortByVotes());

		// keep a list of non-voters
		TreeSet<Player> novotes = new TreeSet<Player>();

		int hammer = allPlayersAlive.size() / 2 + 1;
		boolean candidateExists = false;

		// determine longest name for prettier output
		int longestName = 0;
		for (Player p : allPlayersSorted) {
			if (p.getTotalVotes() > 0 && p.getName().length() > longestName) {
				longestName = p.getName().length();
			}
		}
		longestName += 2;

		for (Player p : allPlayersSorted) {
			// if the player has votes on him, print out the voters
			if (p.getTotalVotes() > 0) {
				candidateExists = true;
				if (hasPlayerList) {
					System.out.printf("(%d/%d) %-" + longestName + "s %s%n", p.getTotalVotes(), hammer, p.getName(), p.getVoters());
				} else {
					System.out.printf("(%d) %-" + longestName + "s %s%n", p.getTotalVotes(), p.getName(), p.getVoters());
				}
			}
			// if the player has not voted, add him to the list of non-voters
			if (p.isAlive() && p.showInVoteCount() && !p.isVoting()) {
				novotes.add(p);
			}

		}

		// if the GM has given the bot a player list, print additional information
		if (hasPlayerList) {
			// list players with no votes
			System.out.printf("(%d/%d) %-" + longestName + "s %s%n", novotes.size(), allPlayersAlive.size(), "No vote:", novotes.toString());

			// list the players that are alive
			System.out.printf("%n%d players alive: %n", allPlayersAlive.size());
			for (Player p : allPlayersAlive) {
				System.out.println(p.getName());
			}

			// list the players that are dead
			ArrayList<Player> allPlayersDead = getPlayersDead();
			if (allPlayersDead.size() > 0) {
				System.out.printf("%n%d players dead:%n", allPlayersDead.size());
				for (Player p : allPlayersDead) {
					System.out.println(p.getName());
				}
			}
		} else if (!candidateExists) {
			System.out.printf("No votes have been cast.%n");
		}
		System.out.printf("[/code]%n%n");

	}

	/**
	 * Does a fuzzy search for the player object corresponding to the given name
	 * @param name The name of the player to search for
	 * @return The Player object whose name is the closest match
	 */
	private Player getPlayerFuzzy(String name) {
		Player matchedPlayer = aliasPlayerMap.get(name);
		// check if an exact match for that alias exists
		if (matchedPlayer != null) {
			return matchedPlayer;
		}
		// if not, loop through all the aliases in the TreeMap and use fuzzy match to find the closest one
		double matchedPercent = 0;
		for (String alias : aliasPlayerMap.keySet()) {
			// if the given name is a substring of an alias, that alias is probably the right one
			// so return the player object associated with it
			if (alias.toLowerCase().contains(name.toLowerCase())) {
				return aliasPlayerMap.get(alias);
			}
			// otherwise, do a fuzzy match
			double result = FuzzyMatch.jaroWinklerDistance(name.toLowerCase(), alias.toLowerCase());
			if (result > matchedPercent && result > 0.8) {
				matchedPlayer = aliasPlayerMap.get(alias);
				matchedPercent = result;
			}
		}

		// display matching debug if a match was found
		if (matchedPercent != 0) {
			System.err.format("Closest match to \"%s\" was \"%s\" with %.2f%% matching.%n", name, matchedPlayer.getName(), matchedPercent * 100);
		}

		return matchedPlayer;
	}

	/**
	 * Returns a string containing the names of all unique players and npcs
	 */
	@Override
	public String toString() {
		return uniquePlayers.toString();
	}

	public void printPlayers() {
		ArrayList<Player> players = getPlayersAlive();
		System.err.println("Players alive at day start: " + players.size() + " " + players.toString());
		players = getPlayersDead();
		System.err.println("Players dead at day start: " + players.size() + " " + players.toString());
	}

	private ArrayList<Player> getPlayersAlive() {
		ArrayList<Player> playersAlive = new ArrayList<Player>();
		for (Player p : uniquePlayers) {
			if (p.isAlive() && p.showInVoteCount()) {
				playersAlive.add(p);
			}
		}
		return playersAlive;
	}

	private ArrayList<Player> getPlayersDead() {
		ArrayList<Player> playersDead = new ArrayList<Player>();
		for (Player p : uniquePlayers) {
			if (!p.isAlive() && p.showInVoteCount()) {
				playersDead.add(p);
			}
		}
		return playersDead;
	}

	/**
	 * Sorts a set in an unnatural order. Returns an ArrayList because a set means equal elements are discarded (eg players with equal votes)
	 * @return An ArrayList containing the elements of the input set sorted by the order specified by the Comparator
	 */
	private <T> ArrayList<T> asSortedList(Set<T> base, Comparator<T> comparator) {
		ArrayList<T> sorted = new ArrayList<T>(base);
		Collections.sort(sorted, comparator);
		return sorted;
	}

	// Comparators
	private static class SortByPosts implements Comparator<Player> {
		public int compare(Player p1, Player p2) {
			/*
			// list alive players before dead players
			if (p1.isAlive() && !p2.isAlive()) {
				return -1;
			} else if (!p1.isAlive() && p2.isAlive()) {
				return 1;
			}
			 */
			return p2.getTotalPosts() - p1.getTotalPosts();
		}
	}

	private static class SortByVotes implements Comparator<Player> {
		public int compare(Player p1, Player p2) {
			return p2.getTotalVotes() - p1.getTotalVotes();
		}
	}
}
