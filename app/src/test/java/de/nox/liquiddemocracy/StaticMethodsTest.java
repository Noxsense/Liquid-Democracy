package de.nox.liquiddemocracy;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.InputStream;
import java.util.Scanner;
import java.util.Map;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Test here some simple static methods implemented for printing and parsing commands. */
public class StaticMethodsTest {

	private String[] testLines = {
		"Alice pick Pizza", // valid
		"Bob delegate Caroline", // valid
		"Dad", // invalid (no action)
		" pick", // invalid (no voter)
		"delegate Mom", // invalid (no action), delegate is the voter, no 3rd word => choice == null
		"pick Apple", // invalid (no action), pick is the voter, no 3rd word => choice == null
		" pick Apple", // invalid empty named voter => no voter
		"Son pick", // valid, but the choice is finally invalid
		"Daughter delegate", // valid but the choice is finally invalid
		"Caroline pick Salad", // vald
		"Dave delegate Eve", // valid (but finally invalid)
		"Eve delegate Mum", // valid (but finally invalid)
		"Mum delegate Eve", // valid (but finally invalid)
		"grammar picks apple",
		"grammer-supp delegates grammar",
		"second pick Apple",
		" third pick Apple "
	};

	private Main.ReadCommand[] expectedCommands = {
		new Main.ReadCommand("Alice", "pick", "Pizza"),
		new Main.ReadCommand("Bob", "delegate", "Caroline"),
		new Main.ReadCommand("Dad", null, null), // invalid (2)
		new Main.ReadCommand(null, "pick", null), // invalid (3)
		new Main.ReadCommand("delegate", null, null), // invalid (4)
		new Main.ReadCommand("pick", null, null), // invalid (5)
		new Main.ReadCommand(null, "pick", "Apple"), // invalid (6)
		new Main.ReadCommand("Son", "pick", null),
		new Main.ReadCommand("Daughter", "delegate", null),
		new Main.ReadCommand("Caroline", "pick", "Salad"),
		new Main.ReadCommand("Dave", "delegate", "Eve"),
		new Main.ReadCommand("Eve", "delegate", "Mum"),
		new Main.ReadCommand("Mum", "delegate", "Eve"),
		new Main.ReadCommand("grammar", "pick", "apple"), // s cut off
		new Main.ReadCommand("grammer-supp", "delegate", "grammar"), // s cut off
		new Main.ReadCommand("second", "pick", "Apple"), // not the same as apple
		new Main.ReadCommand("third", "pick", "Apple") // trimmed
	};

	private int testLineSize = this.testLines.length;

	/** Test if lines parse to commands.
	 * Assumptions: There are no newlines in one line.
	 * But if there were, the regex '\s+' may split it or it will become part of the names accepting all spaces.
	 */
	@Test
	public void testParseLine() {
		Main.ReadCommand cmd, expectedCmd;

		for (int i = 0; i < testLineSize; i++) {
			cmd = Main.readLine(testLines[i]);
			expectedCmd = expectedCommands[i];

			assertEquals("action", expectedCmd.action, cmd.action);
			assertEquals("voter", expectedCmd.voter, cmd.voter);
			assertEquals("choice", expectedCmd.choice, cmd.choice);
			assertEquals("isValid", expectedCmd.isValid(), cmd.isValid());

			// test directly: invalid cases
			if (i > 1 && i < 7) assertEquals(false, cmd.isValid());
		}
	}

	/** Test Main.printSortedResult.
	 * Out put should be in Sytem.out, the Output should also be ordered and formatted.
	 * But keep invalids to the end.
	 */
	@Test
	public void testResultOutput() {
		// "monitor" the standard out
		final PrintStream stdout = System.out;
		final ByteArrayOutputStream monitorOut = new ByteArrayOutputStream();
		System.setOut(new PrintStream(monitorOut)); // set in between.

		final String format = Main.RESULTF;

		final String salad = "Salad";
		final String pizza = "Pizza";
		final String vegan = "Vegan Burger";

		String out; // expected out.

		LiquidDemocracy democracy = new LiquidDemocracy();

		democracy.pick("Alice", pizza);
		democracy.delegate("Bob", "Carol");
		democracy.pick("Carol", salad);
		democracy.delegate("Dave", "Eve");
		democracy.delegate("Eve", "Mallory");
		democracy.delegate("Mallory", "Eve");

		// 3 lines, with this order: Salad (2), Pizza (1), Invalid (3)
		out = String.format(format + format + format,
				2, salad,
				1, pizza,
				3, "Invalid");

		// print to "System.out"
		Main.printSortedResult(democracy.getResults());

		assertEquals(out, monitorOut.toString());

		// clear stream.
		((ByteArrayOutputStream) monitorOut).reset();

		// ---

		// this makes a second vote for pizza: Sort by Name.
		democracy.pick("Pizza2", pizza);

		// [P]izza -> [S]alad
		out = String.format(format + format + format,
				2, pizza,
				2, salad,
				3, "Invalid");

		// print to "System.out"
		Main.printSortedResult(democracy.getResults());

		assertEquals(out, monitorOut.toString());

		// clear stream.
		((ByteArrayOutputStream) monitorOut).reset();

		// ---

		// append new alternative with just one note, but still before invalid.
		democracy.pick("Appendig", vegan);

		// [P]izza -> [S]alad
		out = String.format(format + format + format + format,
				2, pizza,
				2, salad,
				1, vegan,
				3, "Invalid");

		// print to "System.out"
		Main.printSortedResult(democracy.getResults());

		assertEquals(out, monitorOut.toString());

	}

	/** Test Main.main(String[]).
	 * Mock input stream and test the output.  */
	@Test
	public void testMain() {
		String input = String.join("\n", testLines);

		// Salad 2x (Bob, Caroline)
		// Pizza 1x (Alice)
		// Apple 2x (second, third)
		// apple 2x (grammer, grammer-supp)
		// >Invalid< 5x (Son, Daughter, Dave, Eve, Mum)
		// total fail 4x: Dad, pick, delegate Mom, pick Apple
		//
		// sorted by count, then by name. invalid to the end. (test in LiquidDemocracy)
		// => 5 lines.
		//
		// and empty line after input.

		String expected = String.format(
				"\n"
				+ Main.RESULTF + Main.RESULTF + Main.RESULTF + Main.RESULTF + Main.RESULTF,
				2, "apple", // all small letter before big
				2, "Apple",
				2, "Salad",
				1, "Pizza",
				5, "Invalid"  // invalid always in the end.
				);

		String monitored;

		InputStream stdin = System.in;

		System.setIn(new ByteArrayInputStream(input.getBytes()));
		Scanner scanner = new Scanner(System.in);

		final PrintStream stderr = System.err;
		final ByteArrayOutputStream monitorError = new ByteArrayOutputStream();
		System.setErr(new PrintStream(monitorError)); // set in between.

		final PrintStream stdout = System.out;
		final ByteArrayOutputStream monitorOut = new ByteArrayOutputStream();
		System.setOut(new PrintStream(monitorOut)); // set in between.

		// no args.
		Main.main(new String[]{});

		monitored = monitorOut.toString();

		assertEquals(expected, monitored);
		assertTrue(monitorError.toString().length() > 0); // printed some warning for sure.
		((ByteArrayOutputStream) monitorOut).reset();

		// insert again.
		System.setIn(new ByteArrayInputStream(input.getBytes()));

		// no with --open.
		Main.main(new String[]{"--open"});
		monitored = monitorOut.toString();

		assertTrue(monitored.startsWith(expected + "\nOpen Votes:"));
		// contains lines. but not always in the same order.
		((ByteArrayOutputStream) monitorOut).reset();
	}
}
