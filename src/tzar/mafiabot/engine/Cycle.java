package tzar.mafiabot.engine;

import java.util.HashSet;

import tzar.mafiabot.gui.MafiaView;

class Cycle {
	private int day = 0, night = 0;
	private boolean isNight = false;
	private HashSet<String> delayedKill = new HashSet<String>();

	private int nextDay = 0, nextNight = 0;
	private boolean advance = false;

	public int today() {
		return day;
	}

	public int tonight() {
		return night;
	}

	public void setNextDay(int day) {
		advance = true;
		nextDay = day;
	}

	public void setNextNight(int night) {
		advance = true;
		nextNight = night;
	}

	// Kills the player at the end of the cycle
	public void kill(String player) {
		delayedKill.add(player);
	}

	private void murderInnocents(Actors actors) {
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

	// Advances the day/night cycle
	public void advance(Actors actors, MafiaView view) {
		if (!advance) return;
		
		actors.printVoteCount("End of " + toString() + " vote count:");
		murderInnocents(actors);
		actors.clearVotes();
		if (nextDay > day) {
			day = nextDay;
			isNight = false;
			view.setPhase("Day " + day);
		} else if (nextNight > night) {
			night = nextNight;
			isNight = true;
			view.setPhase("Night " + night);
		}
		actors.printPlayers();
		advance = false;
	}

	// returns a string in the form of "Night 1" or "Day 1"
	public String toString() {
		return (isNight ? "Night " + night : "Day " + day);
	}
}
