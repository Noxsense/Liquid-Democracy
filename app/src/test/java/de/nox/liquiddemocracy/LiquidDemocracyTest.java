package de.nox.liquiddemocracy;

import java.util.Map;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/** The LiquidDemocracyTest.
 * Unit tests to test corner cases of the public input. */
public class LiquidDemocracyTest {

	private static void printChoices(String message, Map<String, String> choices) {
		System.out.println(message);
		choices.forEach((k,v) -> System.out.printf("%-10s chose %13s\n", k, v));
		System.out.println();
	}

	/** Do the main example. */
	@Test public void testExampleInput() {
		LiquidDemocracy democracy = new LiquidDemocracy();

		democracy.pick("Alice", "Pizza");
		democracy.delegate("Bob", "Carol");
		democracy.pick("Carol", "Salad");
		democracy.delegate("Dave", "Eve");
		democracy.delegate("Eve", "Mallory");
		democracy.delegate("Mallory", "Eve");

		Map<String, String> choices = democracy.getResultingChoices(); // each voter with their (indirect) choice
		printChoices("Main Example", choices);

		LiquidDemocracy.Result results = democracy.getResults();

		// example output: matches

		assertEquals(2l, results.choices.get("Salad").longValue());
		assertEquals(1l, results.choices.get("Pizza").longValue());
		assertEquals(3l, results.invalidVoteCount);

		// overall sum of voters and alternatives
		assertEquals(6, democracy.getVoters().size());
		assertEquals(2, democracy.getAlternatives().size()); // pizza, salad

		// each indirect and direct choice is mapped correctly
		assertEquals("Pizza", choices.get("Alice"));
		assertEquals("Salad", choices.get("Carol"));
		assertEquals("Salad", choices.get("Bob")); // indirectly

		assertEquals(null, choices.get("Dave")); // invalid choice (indirectly)

		assertEquals(null, choices.get("Eve")); // invalid choice
		assertEquals(null, choices.get("Mallory")); // invalid choice
	}

	/** Valid Option is called 'Invalid', this should be a valid choice. */
	@Test public void testPickAlternativeInvalid() {
		LiquidDemocracy democracy = new LiquidDemocracy();

		democracy.pick("Alice", "Invalid"); // real and valid alternative
		democracy.delegate("Bob", "Carol");
		democracy.pick("Carol", "Salad");
		democracy.delegate("Dave", "Eve");
		democracy.delegate("Eve", "Mallory");
		democracy.delegate("Mallory", "Eve");

		Map<String, String> choices = democracy.getResultingChoices(); // each voter with their (indirect) choice
		printChoices("Valid 'Invalid'", choices);

		LiquidDemocracy.Result results = democracy.getResults(); // count of votes

		assertEquals(null, results.choices.get("Pizza")); // no vote ever
		assertEquals(2l, results.choices.get("Salad").longValue());

		assertEquals("Invalid", choices.get("Alice")); // validy chosen Invalid
		assertEquals(null, choices.get("Dave")); // invalid choice

		// assertEquals("Alternative 'Invalid' Votes 1", 1l, results.invalidVoteCount);

		// TODO issue: maps choice <Invalid> and <Invalid Choice together>

		assertEquals("Invalid Votes 3", 3l, results.invalidVoteCount); // invalid results
	}

	/** Test with wanted and unwanted null arguments. */
	@Test public void testNull() {
		LiquidDemocracy democracy = new LiquidDemocracy();

		// actually an acceptable invalid choice (invalid +1)
		democracy.pick("Alice", null);

		// invalid command => throw NullPointerException
		try {
			democracy.pick(null, "Pizza");
			fail("NullPointerException for first voter not thrown.");
		} catch (NullPointerException e) {}

		// invalid command => throw NullPointerException
		try {
			democracy.delegate(null, "Eve");
			fail("NullPointerException for first voter not thrown.");
		} catch (NullPointerException e) {}

		// acceptable invalid choice.
		democracy.delegate("Mallory", null);

		LiquidDemocracy.Result results = democracy.getResults();

		assertEquals(2, democracy.getVoters().size()); // just int
		assertEquals(0, democracy.getAlternatives().size()); // just int

		assertEquals(2l, results.invalidVoteCount);
	}

	/** Adding more votes will recalculate the results. */
	@Test public void testLateVotes() {
		LiquidDemocracy democracy = new LiquidDemocracy();

		// example scenario.
		democracy.pick("Alice", "Pizza");
		democracy.delegate("Bob", "Carol");
		democracy.pick("Carol", "Salad");
		democracy.delegate("Dave", "Eve");
		democracy.delegate("Eve", "Mallory");
		democracy.delegate("Mallory", "Eve");

		LiquidDemocracy.Result results = democracy.getResults();

		assertEquals(2l, results.choices.get("Salad").longValue()); // bob and carol
		assertEquals(1l, results.choices.get("Pizza").longValue()); // alice
		assertEquals(3l, results.invalidVoteCount); // eve, dave, mallory

		assertEquals(6, democracy.getVoters().size());

		// late delegation

		democracy.delegate("Late", "Alice");

		Map<String, String> updateChoices0 = democracy.getResultingChoices();
		printChoices("Late Delegation", updateChoices0);

		LiquidDemocracy.Result updatedResults0 = democracy.getResults();

		assertEquals(2l, updatedResults0.choices.get("Salad").longValue()); // still the same
		assertEquals(2l, updatedResults0.choices.get("Pizza").longValue()); // has now Late (they chose Alice)
		assertEquals(3l, updatedResults0.invalidVoteCount); // still the same

		assertEquals(7, democracy.getVoters().size()); // one more.

		assertEquals("Pizza", updateChoices0.get("Alice"));
		assertEquals("Pizza", updateChoices0.get("Late")); // (Alice)

		assertEquals(false, results == updatedResults0); // new object.

		// late pick

		democracy.pick("Later", "New Pick");
		// NOTE: Input will not have space, but the code can handle spaces.

		LiquidDemocracy.Result updatedResults1 = democracy.getResults();

		assertEquals(1l, updatedResults1.choices.get("New Pick").longValue()); // new choice
		assertEquals(2l, updatedResults1.choices.get("Salad").longValue()); // unchanged
		assertEquals(2l, updatedResults1.choices.get("Pizza").longValue()); // unchanged
		assertEquals(3l, updatedResults1.invalidVoteCount); // unchanged

		assertEquals(8, democracy.getVoters().size()); // one more.
		assertEquals(3, democracy.getAlternatives().size()); // one more as well.

		assertEquals(false, updatedResults0 == updatedResults1); // new object.

		// late invalid

		democracy.delegate("Late Invalid", "Late Invalid"); // self

		LiquidDemocracy.Result updatedResults2 = democracy.getResults();

		assertEquals(1l, updatedResults2.choices.get("New Pick").longValue()); // unchanged
		assertEquals(2l, updatedResults2.choices.get("Salad").longValue()); // unchanged
		assertEquals(2l, updatedResults2.choices.get("Pizza").longValue()); // unchanged
		assertEquals(4l, updatedResults2.invalidVoteCount); // more invalids.

		assertEquals(9, democracy.getVoters().size()); // one more.
		assertEquals(3, democracy.getAlternatives().size()); // no new pick.

		// new object.
		assertEquals(false, updatedResults1 == updatedResults2);
	}

	/** Test where the voter picks three times, only the first should be valid. */
	@Test public void testVoteAgain() {
		LiquidDemocracy democracy = new LiquidDemocracy();

		Map<String, String> choices;
		LiquidDemocracy.Result results;

		// chose invalid.
		democracy.delegate("A", "A");

		results = democracy.getResults();
		assertEquals("#Voters", 1, democracy.getVoters().size());
		assertEquals("#Invalid", 1, results.invalidVoteCount);

		// reset to valid (pizza)
		democracy.pick("A", "Pizza");

		choices = democracy.getResultingChoices();
		results = democracy.getResults();
		assertEquals("#Voters", 1, democracy.getVoters().size());
		assertEquals("#Invalid", 0, results.invalidVoteCount);
		assertEquals("#Alternatives", 1, democracy.getAlternatives().size());
		assertEquals("#Pizza", 1l, results.choices.get("Pizza").longValue());
		assertEquals("Pizza", choices.get("A"));

		// reset to valid (salad)
		democracy.pick("A", "Salad");
		results = democracy.getResults();
		choices = democracy.getResultingChoices();

		assertEquals("#Voters", 1, democracy.getVoters().size());
		assertEquals("#Alternatives", 2, democracy.getAlternatives().size()); // initiated both salad and pizza
		assertEquals(0l, results.invalidVoteCount); // no invalid
		assertEquals(1l, results.choices.get("Salad").longValue());
		assertEquals("Salad", choices.get("A"));
		assertEquals(null, results.choices.get("Pizza")); // not longer chosen by any

		// never set vote
		assertEquals(null, results.choices.get("Never"));
	}

	/** Testing a very long cycle, all invalid. */
	@Test public void testLongLinedCycle() {
		LiquidDemocracy democracy = new LiquidDemocracy();
		LiquidDemocracy.Result results;

		int votersCount = 1000;

		// valid line delegation
		for (int i = 0; i < votersCount - 1; i ++) {
			democracy.delegate("A " + i, "A " + (i + 1));
		}

		// last delegation: a tiny cycle.
		democracy.delegate("A " + (votersCount - 1), "A " + (votersCount - 2));

		results = democracy.getResults();

		assertEquals("#Voters", votersCount, democracy.getVoters().size());
		assertEquals("#Alternatives", 0, democracy.getAlternatives().size()); // no alternative
		assertEquals("#Invalid", votersCount, results.invalidVoteCount); // all invalid

		// reset last delegation: make it valid. ==> The last one was the ony cycling.

		String pick = "Apple for All";
		democracy.pick("A " + (votersCount - 1), pick); // last in line is valid now

		results = democracy.getResults();

		assertEquals("#Voters (now valid)", votersCount, democracy.getVoters().size());
		assertEquals("#Alternatives (now valid)", 1, democracy.getAlternatives().size());
		assertEquals("#Invalid (now valid)", 0, results.invalidVoteCount); // no invalid
		assertEquals("#Apple (now valid)", (long) votersCount, results.choices.get(pick).longValue()); // all valid
	}
}
