package tzar.mafiabot.engine;

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
	// This TreeSet is a set of all the (unique) Player objects in the above TreeMap. 
	// I know it's ugly needing to keep track of two objects like this, 
	// but I haven't yet found a way to get unique keys from the TreeMap.
	private TreeSet<Player>	uniquePlayers = new TreeSet<Player>();

	// This TreeMap is used to associate all aliases with their respective Player objects
	// Each alias is a key in the TreeMap, with the Player object as its value 
	// eg <"alias1", Player1>, <"alias2", Player1>
	private TreeMap<String, Player> aliasPlayerMap = new TreeMap<String, Player>(String.CASE_INSENSITIVE_ORDER);

	// This is the Element for the root of the aliases XML document
	private Element aliasDocument = Jsoup.parse(getClass().getClassLoader().getResourceAsStream("aliases.xml"), null, "aURI", Parser.xmlParser());


	/**
	 * Creates a new Actors object with default NPCs and no players
	 * @throws Exception
	 */
	public Actors() throws Exception {
		addDefaultNpcs();
	}

	/**
	 * Creates a new Actors object with default NPCs and starting players
	 * @param players Players to start with
	 * @throws Exception
	 */
	public Actors(String[] players) throws Exception {
		// add players and their aliases to the hash table
		System.out.println();
		for (String name : players) {
			addPlayer(name);
		}
		System.out.println();
		//System.out.printf("%nPlayers (%d): %s%n", uniquePlayers.size(), uniquePlayers.toString());
		//System.out.printf("Aliases loaded (%d): %s%n%n", aliasPlayerMap.size(), aliasPlayerMap.toString());

		// add default npcs and their aliases to the hash table
		addDefaultNpcs();
	}

	private void addDefaultNpcs() {
		for (Element npcElement : aliasDocument.getElementsByTag("npc")) {
			String[] names = npcElement.text().split(";");
			Player npc = new Player(names[0], true);
			uniquePlayers.add(npc);
			for (String name : names) {
				aliasPlayerMap.put(name.trim(), npc);
			}
		}
	}

	public void addNpc(String name) {
		Player npc = new Player(name, true);
		if (uniquePlayers.add(npc)) {
			aliasPlayerMap.put(name.trim(), npc);
			System.out.println("Added vote option " + name + " to the game.");
		} else {
			System.out.println("Vote option " + name + " is already in the game.");
		}
	}

	public void addPlayer(String name) {
		// check if a player with this exact name already exists
		Player player = aliasPlayerMap.get(name);
		if (player != null) {
			System.out.println("(!) " + player.getName() + " is already in the game.");
			return;
		}

		// check for name misspellings or other aliases given in the aliases xml
		String[] matchedLine = null;
		String matchedName = name;
		double matchedPercent = 0;
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
		// create a new player object for this player
		player = new Player(matchedName);

		if (matchedLine != null) {
			// put aliases into map
			for (String aliasFound : matchedLine) {
				aliasPlayerMap.put(aliasFound.trim(), player);
			}
			// aliases found
			if (matchedPercent != 1) {
				System.out.println("Correcting " + name + " to " + matchedName + ".");
			}
			System.out.println("Imported aliases for " + matchedName + ": " + Arrays.asList(matchedLine).toString());

		}
		// add the given name as the player's alias
		// (for the case where the name wasn't found and just in case the spellcheck was wrong)
		aliasPlayerMap.put(name, player);
		// add the player object to the set of unique players
		uniquePlayers.add(player);

		System.err.println(player.getName() + " was added to the game.");
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

	public void vote(String voter, String candidate) {
		// check if the voter is playing and alive
		Player v = getPlayerFuzzy(voter);
		if (v != null && v.isAlive()) {
			if (!v.canVote()) {
				System.out.println("Ignored vote from " + v.getName() + ".");
				return;
			}
			// get the lynch candidate with a fuzzy string search
			Player c = getPlayerFuzzy(candidate);
			// if the candidate exists
			if (c != null && c.isAlive()) {
				if (v.vote(c)) {
					StringBuffer result = new StringBuffer();
					if (candidate.equalsIgnoreCase(c.getName())) {
						result.append(String.format("%s voted for %s", voter, c.getName()));
					} else {
						result.append(String.format("%s voted for %s (%s)", voter, candidate, c.getName()));
					}
					if (c.getTotalVotes() >= getPlayersAlive().size() / 2 + 1) {
						result.append(". HAMMER!!");
						result.insert(0, "(!) ");
					}
					System.out.println(result.toString());
				}
			} else if (c == null) { // could not find player
				System.out.printf("(x) Rejected vote from \"%s\" on unknown player \"%s\".%n", voter, candidate);
			} else {
				System.out.printf("(x) Rejected vote from \"%s\" on dead player \"%s\".%n", voter, candidate);
			}
		} else {
			System.out.printf("(x) Rejected spectator/dead vote from \"%s\" on \"%s\".%n", voter, candidate);
		}
	}

	public void unvote(String voter) {
		Player v = aliasPlayerMap.get(voter);
		if (v != null && v.unvote()) {
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
			p.allowVote(false);
			System.out.println("Removed " + p.getName() +"'s ability to vote!");
		}
	}
	
	public void giveVote(String name) {
		Player p = getPlayerFuzzy(name);
		if (p != null) {
			p.allowVote(true);
			System.out.println("Reinstated " + p.getName() +"'s ability to vote!");
		}
	}

	public void addPost(String name, int day, int length) {
		Player p = aliasPlayerMap.get(name);
		if (p != null) {
			p.addPost(day, length);
		}
	}

	public void printPostCount(int day) {
		System.out.printf("%n[code]%n");
		System.out.println("Post count:");
		// print out the column header first
		StringBuffer header = new StringBuffer(String.format("%-23s", " "));
		for (int i = 1 ; i <= day; i++) {
			header.append(String.format("[%4s] ", "D" + i));
		}
		header.append("[Total]");
		//header.append("[Total] [Avg Words/Post]");
		System.out.println(header.toString());
		// have to use an arraylist to sort by an unnatural order or else elements would get discarded
		ArrayList<Player> playersSortedByPosts = asSortedList(uniquePlayers, new SortByPosts());
		for (Player p : playersSortedByPosts) {
			if (!p.isNpc() || p.getTotalPosts() > 0) {
				p.printPostCount(day);
			}
		}
		System.out.printf("[/code]%n%n");
	}

	public void printVoteCount(String header, boolean hasPlayerList) {
		// print the header
		System.out.printf("%n[code]" + header + "%n");

		boolean votesCasted = false;
		// get a list of all players alive
		ArrayList<Player> allPlayersAlive = getPlayersAlive();

		// Print out the number of votes required to hammer
		//System.out.printf("Hammer falls at %d votes!%n", allPlayersAlive.size() / 2 + 1);

		// sort the players by decreasing number of votes
		ArrayList<Player> allPlayersSorted = asSortedList(uniquePlayers, new SortByVotes());

		// keep a list of non-voters
		TreeSet<Player> novotes = new TreeSet<Player>();

		int hammer = allPlayersAlive.size() / 2 + 1;

		for (Player p : allPlayersSorted) {
			// if the player has votes on him, print out the voters
			if (p.getTotalVotes() > 0) {
				votesCasted = true;
				if (hasPlayerList) {
					//System.out.printf("%-20s %2d/%d %s%n", p.getName() + ":", p.getTotalVotes(), hammer, p.getVoters());
					System.out.printf("(%d/%d) %s %s%n", p.getTotalVotes(), hammer, p.getName(), p.getVoters());
				} else {
					System.out.printf("(%d) %s %s%n", p.getTotalVotes(), p.getName(), p.getVoters());
				}
			}
			// if the player has not voted, add him to the list of non-voters
			if (p.isAlive() && !p.isNpc() && !p.isVoting()) {
				novotes.add(p);
			}
		}

		if (hasPlayerList) {
			// list players with no votes
			//System.out.printf("%-20s %2d %" + String.valueOf(hammer).length() + "s %s%n", "No vote:", novotes.size(), " ", (novotes.size() > 0 ? novotes.toString() : ""));
			System.out.printf("(%d/%d) No vote: %s%n", novotes.size(), allPlayersAlive.size(), (novotes.size() > 0 ? novotes.toString() : ""));

			// list the players that are alive
			//System.out.printf("%nPlayers alive:\t%2d %s%n", allPlayersAlive.size(), allPlayersAlive.toString());
			System.out.printf("%n%d players alive:%n", allPlayersAlive.size());
			for (Player p : allPlayersAlive)
				System.out.println(p.getName());

			// list the players that are dead
			ArrayList<Player> allPlayersDead = getPlayersDead();

			if (allPlayersDead.size() > 0) {
				//System.out.printf("Players dead:\t%2d %s%n%n", allPlayersDead.size(), allPlayersDead.toString());
				System.out.printf("%n%d players dead:%n", allPlayersDead.size());
				for (Player p : allPlayersDead)
					System.out.println(p.getName());
			}
		} else if (!votesCasted) {
			System.out.printf("No votes have been cast.%n%n");
		}
		System.out.println("[/code]\n");

	}

	/*
	public String searchPlayerFuzzy(String name) {
		Player p = getPlayerFuzzy(name);
		return (p != null ? p.getName() : null);
	}
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

	// Returns a string containing the names of all unique players and npcs
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
			if (p.isAlive() && !p.isNpc()) {
				playersAlive.add(p);
			}
		}
		return playersAlive;
	}

	private ArrayList<Player> getPlayersDead() {
		ArrayList<Player> playersDead = new ArrayList<Player>();
		for (Player p : uniquePlayers) {
			if (!p.isAlive() && !p.isNpc()) {
				playersDead.add(p);
			}
		}
		return playersDead;
	}

	/**
	 * Sorts a set in an unnatural order. An ArrayList is used because using a set means equal elements are discarded 
	 * (eg players with equal votes)
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

	/*
	// code for generating jfreechart pie charts (if I ever use them)
	private void generatePieChart(List<Player> players) {
		DefaultPieDataset result = new DefaultPieDataset();
		for (Player p : players) {
				result.setValue(p.toString(), p.getTotalPosts());
		}
		JFreeChart chart = ChartFactory.createPieChart3D("Posts", result, true, true, false);
		ChartFrame chartFrame = new ChartFrame(chart.getTitle().getText(), chart);
		chartFrame.pack();
		//chartFrame.setResizable(false);
		chartFrame.setVisible(true);
	}
	 */
}
