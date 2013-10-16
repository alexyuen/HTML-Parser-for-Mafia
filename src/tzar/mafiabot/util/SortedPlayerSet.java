package tzar.mafiabot.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

import tzar.mafiabot.engine.Player;

class SortedPlayerSet extends LinkedList<Player> {
	private static final long serialVersionUID = 1L;
	
	private Comparator<Player> comparator = null;

	@Override
	public boolean add(Player p) {
		throw new UnsupportedOperationException();
	}
	@Override
	public void add(int index, Player p) {
		throw new UnsupportedOperationException();
	}
	@Override
	public boolean addAll(int index, Collection<? extends Player> c) {
		throw new UnsupportedOperationException();
	}
	@Override
	public boolean addAll(Collection<? extends Player> c) {
		throw new UnsupportedOperationException();
	};
	
	public boolean insertSorted(Player p) {
		if (getPlayerIgnoreCase(p.getName()) == null) {
			super.add(p);
			Collections.sort(this, comparator);
			return true;
		}
		return false;
	}
	
	public boolean insertAll(Collection<? extends Player> s) {
		for (Player p : s) {
			this.insertSorted(p);
		}
		return true;
	}
	
	private Player getPlayerIgnoreCase(String name) {
		for (Player p : this) {
			if (p.getName().equalsIgnoreCase(name)) {
				return p;
			}
		}
		return null;
	}
	
	public void resort(Comparator<Player> c) {
		comparator = c;
		Collections.sort(this, c);
	}
}