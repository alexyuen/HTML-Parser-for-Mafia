package tzar.mafiabot.engine;

import java.util.ArrayList;
import java.util.LinkedList;

public class Player implements Comparable<Player> {
	// implement events?
	private String name = null;
	private ArrayList<String> aliases = new ArrayList<String>();

	private boolean canVote = true;
	private boolean canBeVoted = true;

	private int voteNum = 1;
	private int voteWeight = 1;
	private LinkedList<Player> votes = new LinkedList<Player>();
	private ArrayList<Player> voters = new ArrayList<Player>();

	private ArrayList<Integer> postCount = new ArrayList<Integer>();
	private int cumulativePostLength = 0;
	private int expiryDate = -1;

	private boolean isNpc = false;

	public Player(String name) {
		this.name = name;
		aliases.add(name);
	}
	
	public void addAlias(String alias) {
		aliases.add(alias);
	}
	
	public String[] getAliases() {
		return aliases.toArray(new String[aliases.size()]);
	}
	
	public void setNpc(boolean isNpc) {
		this.isNpc = isNpc;
	}
	
	public boolean isNpc() {
		return isNpc;
	}

	public void vote(Player candidate) {
		votes.addLast(candidate);
		while (votes.size() > voteNum) {
			Player previous = votes.pollFirst();
			while (previous.voters.remove(this));
		}
		for (int i = 0; i < voteWeight; i++) {
			candidate.voters.add(this);
		}
	}

	public void unvote() {
		// stop voting for everyone we are currently voting for
		for (Player candidate : votes) {
			// remove all occurrences of this object from the voters of the other object 
			while (candidate.voters.remove(this));
		}
		votes.clear();
	}

	public void pardon() {
		for (Player voter : voters) {
			while (voter.votes.remove(this));
		}
		voters.clear();
	}

	public void kill(int day) {
		expiryDate = day;
	}

	public void revive() {
		expiryDate = -1;
	}

	public boolean isAlive() {
		return expiryDate == -1;
	}

	public boolean isVoting() {
		return votes.size() > 0;
	}

	public boolean isVoting(Player p) {
		return isVoting() && votes.contains(p);
	}

	public boolean canVote() {
		return this.canVote;
	}

	public boolean isVotable() {
		return canBeVoted;
	}

	public void setVoteEligibility(boolean canVote, boolean canBeCandidate) {
		this.canVote = canVote;
		this.canBeVoted = canBeCandidate;
	}

	public int getVoteNum() {
		return this.voteNum;
	}

	public int getVoteWeight() {
		return this.voteWeight;
	}

	public void setVotePrestige(int num, int weight) {
		this.voteNum = num;
		this.voteWeight = weight;
	}

	public int getTotalVotes() {
		return voters.size();
	}

	public String getVoters() {
		return voters.toString();
	}

	public void addPost(int day, int length) {
		cumulativePostLength += length;
		while (postCount.size() <= day) {
			postCount.add(0);
		}
		postCount.set(day, postCount.get(day) + 1);

	}

	public int getTotalPosts() {
		int sum = 0;
		for (int i : postCount) {
			sum += i;
		}
		return sum;
	}

	public ArrayList<Integer> getPostCount() {
		return postCount;
	}

	public int getAvgPostLength() {
		return cumulativePostLength / getTotalPosts() / 5;
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
