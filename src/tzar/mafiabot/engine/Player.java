package tzar.mafiabot.engine;

import java.util.ArrayList;

public class Player implements Comparable<Player> {
	// implement events?
	private String name = null;
	private boolean isNpc = false;

	private Player vote = null;
	private ArrayList<Player> voters = new ArrayList<Player>();

	private int[] posts = new int[16]; //hardcoded limit of 15 days for now
	private ArrayList<Integer> postLength = new ArrayList<Integer>();
	private int dayOfDeath = -1;

	
	public Player(String name) {
		this(name, false);
	}
	
	public Player(String name, boolean isNpc) {
		this.name = name;
		this.isNpc = isNpc;
	}

	public boolean vote(Player candidate) {
		if (candidate != null && !candidate.equals(vote)) {
			unvote();
			vote = candidate;
			candidate.voters.add(this);
			return true;
		}
		return false;
	}

	public boolean unvote() {
		if (vote != null) {
			vote.voters.remove(this);
			vote = null;
			return true;
		}
		return false;
	}

	public void pardon() {
		for (Player voter : voters) {
			voter.vote = null;
		}
		voters.clear();
	}

	public void kill(int day) {
		dayOfDeath = day;
	}
	
	public void resurrect() {
		dayOfDeath = -1;
	}
	
	public boolean isAlive() {
		return dayOfDeath == -1;
	}
	
	public boolean isVoting() {
		return vote != null;
	}
	
	public boolean isNpc() {
		return isNpc;
	}

	public void addPost(int day, int length) {
		if (day > 0) {
			posts[day] += 1;
			posts[0] += 1;
		}
		postLength.add(length);
	}

	public int getTotalPosts() {
		return posts[0];
	}

	public int getTotalVotes() {
		return voters.size();
	}
	
	public int getAvgPostLength() {
		int sum = 0;
		for (int i = 0; i < postLength.size(); i++) {
			sum += postLength.get(i);
		}
		return sum / postLength.size() / 5;
	}
	
	public String getVoters() {
		return voters.toString();
	}

	public void printPostCount(int day) {
		StringBuffer postCount = new StringBuffer(String.format("%-23s", name + ":"));
		// print out the post count for each day
		for (int i = 1; i <= day; i++) {
			if (!isAlive() && i >= dayOfDeath) {
				postCount.append(String.format("[†%3d] ", posts[i]));
			} else {
				postCount.append(String.format("[%4d] ", posts[i]));
			}
		}
		// print out total posts and avg length
		if (!isAlive()) {
			postCount.append(String.format("[†%4d] ", getTotalPosts()));
			//postCount.append(String.format("[†%4d] ", getAvgPostLength()));
		} else {
			postCount.append(String.format("[%5d] ", getTotalPosts()));
			//postCount.append(String.format("[%5d] ", getAvgPostLength()));
		}
		System.out.println(postCount.toString());
	}

	public String getName() {
		return name;
	}
	
	@Override
	public String toString() {
		return name;
	}

	@Override
	public int compareTo(Player p) {
		// default ordering is alphabetical
		return name.compareToIgnoreCase(p.name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Player && ((Player) o).name.equals(name);
	}
}
