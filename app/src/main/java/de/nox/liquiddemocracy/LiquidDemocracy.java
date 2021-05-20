package de.nox.liquiddemocracy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** The LiquidDemocracy class.
  * This is a graph (optimally a tree) structure that lists all alternatives and voters.
  *
  *
  * Issue: Nullable Names.
  * Solution:
  * If the first/only voter is  null, throw a NullPointerException, otherwise their vote is just invalid.
  *
  *
  * Issue: Alternative called "Invalid".
  * With the given formation it is indistinguishable from the count of invalid votes .
  * Simple Solution: Prepend a prefix to indicate proper alternatives.
  * Other Solutions: Don't show just the strings and names, but also the underlying types.
  *
  *
  * Issue: Invalid Votes (Cycles, delegating self).
  * Simple Solution: Count all possible votes, subtract valid votes.
  * Proper Solution: Who were the invalid voters, detect cycles and their participants.
  *
  * Improvements: Open Selection, display (indirect) alternatives.
  * In the end it can be displayed who chose what, including the invalids.
  *
  * This is designed and implemented to be extendible and improvable and fulfilling
  * the first requested SPEC (counting the voters) in the moment it was needed.
  * So calculating what was chosen by whom is an own earlier step.
  *
  * @throws NullPointerException if the first voter is null
  */
public class LiquidDemocracy {

	private Map<String, Voter> voters; // all voters (with name as key for quick access)
	private Map<String, Alternative> alternatives; // all alternatives (with name as key for quick access)

	private Map<Voter, Alternative> choices; // calculating
	private boolean calculated = false;

	private final InvalidChoice INVALID_CHOICE = new InvalidChoice();

	public LiquidDemocracy() {
		this.voters = new HashMap<>();
		this.alternatives = new HashMap<>();
		this.choices = new HashMap<>();
	}

	/** A node is either an alternative or a voter in this liquid democracy tree. */
	private abstract class Node {
		protected Set<Voter> chosenBy;

		private Node() {
			chosenBy = new HashSet<>();
		}

		/** Add a new voter who picked or delegated their vote to this node. */
		public void isChosenBy(Voter v) {
			this.chosenBy.add(v);
		}
	}

	/** Voter is a node in the tree, they can pick and delegate. */
	private class Voter extends Node {

		public final String name; // key value => equality.
		private Node choice; // additional, redundant, but used double link (better backtracking, insuring no infinity loops)

		public Voter(String name, Node choice) {
			this.name = name;
			this.choice = choice;
		}

		@Override public String toString() {
			return name;
		}

		@Override public boolean equals(Object that) {
			return that != null && that instanceof Voter && this.name.equals(((Voter) that).name);
		}

		public Node getChoice() {
			return this.choice;
		}

		public void setChoice(Node choice) {
			// only the last choice counts.
			this.choice = choice;
		}
	}

	/** Alternative is a node in a tree, they can only be picked. */
	private class Alternative extends Node {
		public final String name; // key value => equality.

		public Alternative(String name) {
			this.name = name;
		}

		@Override public String toString() {
			return isValid() ? name : "Invalid Choice";
		}

		@Override public boolean equals(Object that) {
			return that != null && that instanceof Alternative && this.name.equals(((Alternative) that).name);
		}

		/** Quicker view of if this is invalid. */
		public boolean isValid() {
			return name != null && !(this instanceof InvalidChoice);
		}
	}

	/** InvalidChoice to identify invalid choices. */
	private class InvalidChoice extends Alternative {
		public InvalidChoice() {
			super(null);
		}
	}

	/** Public Result for the Democracy.
	 * Displaying the latest calculation. This class will not change, if the results change later.
	 * It is a simple tuple of valid choices and their counts and one invalid vote counter.
	 */
	public static class Result {
		public final Map<String, Long> choices;
		public final long invalidVoteCount;

		public Result(Map<String, Long> choices, long invalidVoteCount) {
			this.choices = choices;
			this.invalidVoteCount = invalidVoteCount;
		}
	}

	/** Get the voter with a given name.
	 * If they not exists, create a new node.
	 * @param name name of the requested voter.
	 * @return voter of the map voters with the given name.
	 * @throws NullPointerException if the name is null.
	 */
	private Voter getVoter(String name) throws NullPointerException {
		if (name == null) {
			throw new NullPointerException("Voter name must not be null.");
		}
		if (!voters.containsKey(name)) {
			Voter newVoter = new Voter(name, null);
			voters.put(name, newVoter);
		}
		return voters.get(name);
	}

	/** Get the alternative with the given name.
	 * @param name name of the requested alternative.
	 * @return alternative of the map alternatives with the given name.
	 * @throws NullPointerException if the name is null.
	 */
	private Alternative getAlternative(String name) throws NullPointerException {
		if (name == null) {
			throw new NullPointerException("Alternative name must not be null.");
		}
		if (!alternatives.containsKey(name)) {
			Alternative newAlternative = new Alternative(name);
			alternatives.put(name, newAlternative);
		}
		return alternatives.get(name);
	}

	/** Return the name of all voters.
	 * Wrap it so it will not influence the results. */
	public Set<String> getVoters() {
		return new HashSet<String>(voters.keySet());
	}

	/** Return the name of all alternatives.
	 * Wrap it so it will not influence the results. */
	public Set<String> getAlternatives() {
		return new HashSet<String>(alternatives.keySet());
	}

	/** Get all latest indirect choices.
	 * This will create an internal state, where the `Voter to Alternative` is mapped.
	 * This state will not be returned to the public as it is,
	 * but will be the base of two distinct public returning
	 * methods: @see getResultingChoices and @see getResults,
	 * which either print each corresponding name, or sums up the typed alternatives accordingly.
	 *
	 * @return set of voter's name mapped to their (indirectly) chosen alternative's name.
	 * */
	private Map<Voter, Alternative> calculateIndirectChoices() {
		if (!calculated) {
			choices.clear(); // reset.

			Set<Voter> also = new HashSet<>();
			Set<Voter> done = new HashSet<>();

			/* PREVIOUSLY I had a recursive attempt with something as following:
			 *
			 *  -- that may not terminate if called from a cycling participant
			 *  fun Node.getAllChosenBy() = chosenBy.flatMap { n -> getAllChosenBy() }
			 *
			 * -- then function call
			 * for (a in Agents) {
			 *    allChosenBy = a.getAllChosenBy()
			 *    choices += completeChosenBy.associateWith(a)
			 * }
			 *
			 * Though this recursive Node method needed the programmer / caller to not call it
			 * with possible cycling voters but always start with agents to terminate.
			 * Or had a more difficult way to implement the avoidance of cycling (with accumulators or so on).
			 *
			 * The invalid voters would also by just defined by being the leftover voters.
			 */

			/* HERE, this algorithm will "climb up" the choice branch to look what the voter chose.
			 * It will also push their choice to the ones who delegated to this voter.
			 */
			for (Voter v : voters.values()) {
				also.clear();
				also.add(v);

				if (done.contains(v)) continue; // already put.

				Node choice = v.getChoice();

				while (choice != null) {

					/* Nothing chosen or the rooting alternative. */
					if (choice == null || choice instanceof Alternative) break;

					/* Here all non-null voters. */

					/* Cycle => reset Alternative to null (no final Alternative is chosen).
					 * The choice / the delegated voter of the voter has chosen someone who is already
					 * in this delegation hierarchy / relation. */
					if (also.contains(choice)) {
						choice = null;
						break;
					}

					/* "Also", also what my delegation chose will be my choice.
					 * Discover cycles, if an also is chosen again. */
					also.add((Voter) choice);

					/* "Also" who chose this voter will have the same choice.
					 * To discover cycles earlier and also make a shortcut if possible */
					also.addAll(v.chosenBy);

					// check if the delegated voter already has a calculated choice and maybe use this.
					choice = choices.containsKey((Voter) choice)
						? choices.get((Voter) choice) // do not recalculate the already calculated.
						: ((Voter) choice).getChoice() // get the delegated choice.
						;
				}

				/* Invalid or alternative.. */
				for (Voter a : also) choices.put(a, choice != null ? (Alternative) choice : INVALID_CHOICE);

				done.addAll(also); // all done.
			}

			calculated = true;
		}

		return choices;
	}

	/** Return for each voter their indirectly voted choice. */
	public Map<String, String> getResultingChoices() {
		if (!calculated) calculateIndirectChoices();

		Map<String, String> voterToAlternative = new HashMap<>();

		/* Map to <Voter.name, Alternative.name>. */
		choices.forEach((v,k) -> voterToAlternative.put(v.name, k != null ? k.name : null));

		return voterToAlternative;
	}

	/** Count latest votes for each alternative.
	 * @return set of alternative names mapped to their counts.
	 */
	public Result getResults() {
		/* Fetch latest results. */
		calculateIndirectChoices(); // update

		/* Count for each alternative (or invalid choice) their received votes.
		 * grouping by choice, counting the group members. */
		Map<Alternative, Long> alternativeResults
			= choices
			.entrySet().stream()
			.collect(Collectors.groupingBy( e ->
						e.getValue(), // key: alternative
						Collectors.counting()) // value: group member size
					);

		/* Map alternatives to string. Split valid and invalid choices. */
		long invalidVoteCount = alternativeResults.getOrDefault(INVALID_CHOICE, 0l);

		/* Filter valid choices, map valid alternative's name (now key: String). */
		Map<String, Long> results
			= alternativeResults.entrySet().stream()
			.filter(e -> e.getKey().isValid())
			.collect(Collectors.toMap(e -> e.getKey().toString(), Map.Entry::getValue));

		return new Result(results, invalidVoteCount);
	}

	/** Add a new delegation.
	 * @param v0 voter.
	 * @param v1 second voter who gets v0 strength.
	 * @throws NullPointerException if any name is null.
	 */
	public void delegate(String v0, String v1) throws NullPointerException {
		Voter voter0, voter1;

		if (v0 == null) {
			/* Unacceptable command. */
			throw new NullPointerException("Voters must not be null");
		}

		voter0 = getVoter(v0); // node of voters

		if (v1 != null) {
			voter1 = getVoter(v1); // node of voters
			voter1.isChosenBy(voter0); // add new chosen by.
			voter0.setChoice(voter1); // also (re) set v0's choice.
			calculated = false;
		} else {
			// just invalid voting
			// System.err.println("Voter (" + v0 + ") made an invalid choice.");
		}
	}

	/** Add a new pick.
	 * @param v0 voter
	 * @param a0 the alternative the voter picks.
	 * @throws NullPointerException if voter's name is null, otherwise only vote is invalid.
	 */
	public void pick(String v0, String a0) throws NullPointerException {
		if (v0 == null) {
			/* Unacceptable command. */
			throw new NullPointerException("Voters must not be null");
		}

		Voter voter = getVoter(v0);

		if (a0 != null) {
			/* The registered alternative is chosen by the registered voter. */
			Alternative alternative = this.getAlternative(a0);

			alternative.isChosenBy(voter);
			voter.setChoice(alternative); // also (re)set v0's choice.
			calculated = false;
		} else {
			// just invalid voting
			// System.err.println("Voter (" + v0 + ") made an invalid choice.");
		}
	}
}
