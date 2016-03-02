package tzar.mafiabot.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;

import tzar.mafiabot.util.FuzzyMatch;


public class Actors {
	/**
	 * This TreeSet keeps track of all the (unique) Player objects in the below TreeMap.
	 * The TreeSet sorts its Player elements by name, in ascending order 
	 */
	private TreeSet<Player>	allPlayers = new TreeSet<Player>();

	/**
	 * Creates a new Actors object with default NPCs and no players
	 */
	public Actors() {
		addDefaultNpcs();
	}

	/**
	 * Creates a new Actors object with default NPCs and the given players
	 * @param players An array of names corresponding to the people who are playing
	 */
	public Actors(String[] players) throws Exception {
		this();
		for (String name : players) {
			addPlayer(name);
		}
	}

	private void addDefaultNpcs() {
		addNpc("No lynch", "No one");
	}

	public void addNpc(String name, String... aliases) {
		Player npc = new Player(name);
		npc.setNpc(true);
		npc.setVoteEligibility(false, true);
		
		for (String s : aliases) {
			npc.addAlias(s);
		}

		if (allPlayers.add(npc)) {
			System.out.println("Added vote option " + name + " to the game.");
		} else {
			System.out.println("Vote option " + name + " is already in the game.");
		}
	}

	public void addPlayer(String name) {
		allPlayers.add(new Player(name));
	}

	public void removePlayer(String name) {
		Player p = getPlayer(name);
		if (p != null) {
			p.unvote();
			p.pardon();
			allPlayers.remove(p);
			System.out.println(p.getName() + " was removed from the game.");
		}
	}
	
	public void addAlias(String player, String alias) {
		Player p = getPlayer(player);
		if (p != null) {
			p.addAlias(alias);
			System.out.printf("Added alias \"%s\" for \"%s\"%n", alias, player);
		} else {
			System.out.printf("(x) %s was not found.%n", player);
		}
	}

	public void vote(String voterName, String candidateName) {
		// check if the voter is playing and alive
		Player voter = getPlayer(voterName);
		if (voter == null) {
			System.out.printf("(x) Rejected spectator vote from \"%s\" on \"%s\".%n", voterName, candidateName);
		} else if (!voter.isAlive()) {
			System.out.printf("(x) Rejected dead vote from \"%s\" on \"%s\".%n", voter, candidateName);
		} else if (!voter.canVote()) {
			System.out.println("Ignored vote from " + voter.getName() + ".");
		} else {
			Player candidate = getPlayer(candidateName);
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
				if (candidate.getTotalVotes() >= getPlayersAlive().length / 2 + 1) {
					output = "(!) " + output + ". HAMMER!!"; 
				}
				System.out.println(output);
			}
		}
	}

	public void unvote(String voter) {
		Player v = getPlayer(voter);
		if (v != null && v.isVoting()) {
			v.unvote();
			System.out.printf("%s unvoted%n", voter);
		}
	}

	public void clearVotes() {
		for (Player p : allPlayers) {
			p.pardon();
		}
		System.out.println("\nAll votes have been cleared.\n");
	}

	public void pardon(String name) {
		Player p = getPlayer(name);
		if (p != null) {
			p.pardon();
			System.out.println(p.getName() + " was pardoned.");
		} else {
			System.out.println("(x) Pardon failed: " + name + " was not found."); 
		}
	}

	public String kill(String name, int dayOfDeath) {
		Player p = getPlayer(name);
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
		Player p = getPlayer(name);
		if (p != null) {
			if (!p.isAlive()) {
				p.revive();
				System.out.println(p.getName() + " was revived.");
			} else {
				System.out.println("(x) Revive failed: " + p.getName() + " is already alive. "); 
			}
		} else { 
			System.out.println("(x) Revive failed: " + name + " was not found.");
		}
	}

	public void takeVote(String name) {
		Player p = getPlayer(name);
		if (p != null) {
			p.setVoteEligibility(false, true);
			System.out.println("Removed " + p.getName() +"'s ability to vote!");
		} else {
			System.out.println("(x) Take vote failed: " + name + " was not found.");
		}
	}

	public void giveVote(String name) {
		Player p = getPlayer(name);
		if (p != null) {
			p.setVoteEligibility(true, true);
			System.out.println("Reinstated " + p.getName() +"'s ability to vote!");
		} else {
			System.out.println("(x) Give vote failed: " + name + " was not found.");
		}
	}

	public void setVoteWeight(String name, int weight) {
		Player p = getPlayer(name);
		if (p != null) {
			p.setVotePrestige(p.getVoteNum(), weight);
			System.out.println("Set " + p.getName() + "'s vote weight to " + weight);
		} else {
			System.out.println("(x) Set vote weight failed: " + name + " was not found.");
		}
	}

	public void setMultiVote(String name, int num) {
		Player p = getPlayer(name);
		if (p != null) {
			p.setVotePrestige(num, p.getVoteWeight());
			System.out.println(p.getName() + " now has " + num + " votes.");
		} else {
			System.out.println("(x) Set multi vote failed: " + name + " was not found.");
		}
	}


	public void addPost(String name, int day, int length) {
		/*
		Player p = getPlayer(name);
		if (p != null) {
			p.addPost(day, length);
		}
		 */
		processAction(name, p -> p.addPost(day, length));
	}

	private void processAction(String name, Consumer<Player> function) {
		Player p = getPlayer(name);
		if (p != null) {
			function.accept(p);
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
		int longestName = allPlayers
				.stream()
				.max((p1, p2) -> p1.getName().length() - p2.getName().length())
				.get()
				.getName()
				.length() + 3;

		// print out the column header first
		String header = String.format("%-" + longestName + "s", " ");
		for (int i = 1 ; i <= day; i++) {
			header += String.format("[%3s] ", "D" + i);
		}
		header += "[Total]";
		//header.append("[Total] [Avg Words/Post]");
		System.out.println(header.toString());
		// an arraylist is used to sort by an unnatural order or else players with equal posts would be discarded
		//ArrayList<Player> playersSortedByPosts = asSortedList(uniquePlayers, new SortByPosts());
		//ArrayList<Player> playersSortedByPosts = asSortedList(allPlayers, (Player p1, Player p2) -> { return p2.getTotalPosts() - p1.getTotalPosts(); });
		Player[] playersSortedByPosts = allPlayers.stream().sorted((p1, p2) -> p2.getTotalPosts() - p1.getTotalPosts()).toArray(Player[]::new);

		for (Player p : playersSortedByPosts) {
			if (!p.isNpc()) {
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
	 */
	public void printVoteCount(String header) {
		// print the header
		System.out.printf("%n[code]%n" + header + "%n");

		// get a list of all players alive
		Player[] allPlayersAlive = getPlayersAlive();

		// sort the players by decreasing number of votes
		Player[] playersSortedByVotes = allPlayers
				.stream()
				.sorted((p1, p2) -> p2.getTotalVotes() - p1.getTotalVotes())
				.toArray(Player[]::new);

		// determine longest name for prettier output
		int longestName = 0;
		for (Player p : playersSortedByVotes) {
			if (p.getTotalVotes() > 0 && p.getName().length() > longestName) {
				longestName = p.getName().length();
			}
		}
		longestName += 2;

		// keep a list of non-voters
		TreeSet<Player> novotes = new TreeSet<Player>();

		int hammer = allPlayersAlive.length / 2 + 1;

		for (Player p : playersSortedByVotes) {
			// if the player has votes on him, print out the voters
			if (p.isNpc() || p.getTotalVotes() > 0) {
				System.out.printf("(%d) %-" + longestName + "s %s%n", p.getTotalVotes(), p.getName(), p.getVoters());
			}
			// if the player has not voted, add him to the list of non-voters
			if (!p.isNpc() && p.isAlive() && !p.isVoting()) {
				novotes.add(p);
			}
		}

		// list players with no votes
		System.out.printf("(%d) %-" + longestName + "s %s%n", novotes.size(), "No vote", novotes.toString());
		System.out.printf("Hammer is at %d votes.%n", hammer);

		// list the players that are alive
		System.out.printf("%n%d players alive: %n", allPlayersAlive.length);
		for (Player p : allPlayersAlive) {
			System.out.println(p.getName());
		}

		// list the players that are dead
		Player[] allPlayersDead = getPlayersDead();
		if (allPlayersDead.length > 0) {
			System.out.printf("%n%d players dead:%n", allPlayersDead.length);
			for (Player p : allPlayersDead) {
				System.out.println(p.getName());
			}
		}

		System.out.printf("[/code]%n%n");

	}

	/**
	 * Searches for the player object corresponding to the given name
	 * @param name The name of the player to search for
	 * @return The Player object whose name is the closest match
	 */
	private Player getPlayer(String name) {
		double matchedPercent = 0;
		Player matchedPlayer = null;
		for (Player p : allPlayers) {
			for (String alias : p.getAliases()) {
				// if the given name is a substring of an alias, that alias is probably the right one

				if (stringContainsCharsInOrder(alias,name)) {
					return p;
				}
				// otherwise, do a fuzzy match
				double result = FuzzyMatch.jaroWinklerDistance(name.toLowerCase(), alias.toLowerCase());
				if (result > matchedPercent && result > 0.8) {
					matchedPlayer = p;
					matchedPercent = result;
				}
			}
		}

		// display matching debug if a match was found
		if (matchedPercent != 0) {
			System.err.format("Closest match to \"%s\" was \"%s\" with %.2f%% matching.%n", name, matchedPlayer.getName(), matchedPercent * 100);
		}

		return matchedPlayer;
	}

	// returns true if input string contains all chars of match string
	private boolean stringContainsCharsInOrder(String input, String chars) {
		StringBuilder sb = new StringBuilder("(?i)");
		for (char c : chars.toCharArray()) {
			sb.append(c + ".*");
		}
		return input.matches(sb.toString());
	}

	/**
	 * Returns a string containing the names of all unique players and npcs
	 */
	@Override
	public String toString() {
		return allPlayers.toString();
	}

	public void printPlayers() {
		Player[] players = getPlayersAlive();
		System.err.println("Players alive at phase start: " + players.length + " " + Arrays.toString(players));
		players = getPlayersDead();
		System.err.println("Players dead at phase start: " + players.length + " " + Arrays.toString(players));
	}

	private Player[] getPlayersAlive() {
		return getPlayers((Player p) -> p.isAlive());
	}

	private Player[] getPlayersDead() {
		return getPlayers((Player p) -> !p.isAlive() && !p.isNpc());
	}

	private Player[] getPlayers(Predicate<Player> pred) {
		return allPlayers.stream().filter(pred).toArray(Player[]::new);
		/*
		ArrayList<Player> players = new ArrayList<Player>();
		for (Player p : allPlayers) {
			if (pred.test(p)) {
				players.add(p);
			}
		}
		return players;
		 */
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
