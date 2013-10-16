package tzar.mafiabot.util;

import java.util.HashSet;
import java.util.Set;

public class FuzzyMatch {
	/**
	 * @return lexical similarity value in the range [0,1] using the Dice Coefficient (comparing bigrams)
	 * 
	 * Source:
	 * http://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/Dice%27s_coefficient
	 */
	public static double diceCoefficient(String s1, String s2) {
		Set<String> nx = new HashSet<String>();
		Set<String> ny = new HashSet<String>();

		for (int i = 0; i < s1.length() - 1; i++) {
			nx.add(s1.substring(i, i + 2));
		}
		for (int j = 0; j < s2.length() - 1; j++) {
			ny.add(s2.substring(j, j + 2));
		}

		Set<String> intersection = new HashSet<String>(nx);
		intersection.retainAll(ny);
		double totcombigrams = intersection.size();

		return (2 * totcombigrams) / (nx.size() + ny.size());
	}

	/**
	 * Computes the Levenshtein Distance between two strings
	 * @return The number of edits required to change str1 into str2
	 * 
	 * Source:
	 * http://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/Levenshtein_distance
	 */
	public static int levenshteinDistance(CharSequence str1, CharSequence str2) {
		int[][] distance = new int[str1.length() + 1][str2.length() + 1];

		for (int i = 0; i <= str1.length(); i++)
			distance[i][0] = i;
		for (int j = 0; j <= str2.length(); j++)
			distance[0][j] = j;

		for (int i = 1; i <= str1.length(); i++)
			for (int j = 1; j <= str2.length(); j++)
				distance[i][j] = minimum(
						distance[i - 1][j] + 1,
						distance[i][j - 1] + 1,
						distance[i - 1][j - 1]
								+ ((str1.charAt(i - 1) == str2.charAt(j - 1)) ? 0 : 1));

		return distance[str1.length()][str2.length()];
	}
	private static int minimum(int a, int b, int c) {
		return Math.min(Math.min(a, b), c);
	}
	public static double levenshteinDistanceNormalized(CharSequence str1, CharSequence str2) {
		return 1 - (double) levenshteinDistance(str1, str2) / (double) Math.max(str1.length(), str2.length());
	}

	/**
	 * Returns normalized score, with 0.0 meaning no similarity at all, and 1.0
	 * meaning full equality.
	 * 
	 * Source:
	 * http://code.google.com/p/duke/source/browse/src/main/java/no/priv/garshol/duke/JaroWinkler.java?r=e32a5712dbd51f1d4c81e84cfa438468e217a65d
	 */
	public static double jaroWinklerDistance(String s1, String s2) {
		if (s1.equals(s2))
			return 1.0;

		// ensure that s1 is shorter than or same length as s2
		if (s1.length() > s2.length()) {
			String tmp = s2;
			s2 = s1;
			s1 = tmp;
		}

		// (1) find the number of characters the two strings have in common.
		// note that matching characters can only be half the length of the
		// longer string apart.
		int maxdist = s2.length() / 2;
		int c = 0; // count of common characters
		int t = 0; // count of transpositions
		int prevpos = -1;
		for (int ix = 0; ix < s1.length(); ix++) {
			char ch = s1.charAt(ix);

			// now try to find it in s2
			for (int ix2 = Math.max(0, ix - maxdist); ix2 < Math.min(s2.length(), ix + maxdist); ix2++) {
				if (ch == s2.charAt(ix2)) {
					c++; // we found a common character
					if (prevpos != -1 && ix2 < prevpos)
						t++; // moved back before earlier
					prevpos = ix2;
					break;
				}
			}
		}

		// we don't divide t by 2 because as far as we can tell, the above
		// code counts transpositions directly.

		// we might have to give up right here
		if (c == 0)
			return 0.0;

		// first compute the score
		double score = ((c / (double) s1.length()) + (c / (double) s2.length()) + ((c - t) / (double) c)) / 3.0;

		// (2) common prefix modification
		int p = 0; // length of prefix
		int last = Math.min(4, s1.length());
		for (; p < last && s1.charAt(p) == s2.charAt(p); p++);

		score = score + ((p * (1 - score)) / 10);

		return score;
	}

}
