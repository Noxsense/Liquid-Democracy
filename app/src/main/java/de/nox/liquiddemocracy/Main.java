/**
 * Liquid Democracy.
 * A simple program that implements the basic set up of liquid democracy.
 *
 * There following rules need to be implemented:
 * - there are n voters and m alternatives
 * - voters can delegate their choices to other voters
 * - voters delegating themselves (directly or indirectly) become invalid
 * - result: a list of alternatives with at least one vote
 * - also: invalid voters should be counted and reported
 *
 * The input of the program is a stream read from std in with the following formatation:
 * - "{Voter's name} pick {alternative}" (direct pick)
 * - or "{Voter's Name} delegate {Second Voter's Name}" (delegation)
 *
 * Where names and alternatives are alpha numeric strings without any space.
 *
 * The output should be formatted as following:
 * - "{summed votes} {alternative or invalid count}"
 * And sorted in descending order.
 *
 *
 * Explainations why I chose this problem.
 * Reading the two different tasks I mapped two main ideas to the tasks, a calendar and a graph accordingly.
 * From that I assumed the second one might be more interesting and maybe also more useful as small tool.
 *
 *
 * Input:
 * The input is CASE SENSITIVE. Picking "Apple" and "apple" are two different things.
 * The verb action does support the third person's s (picks, delegates).
 * The if a Voter 'picks' another Voter, their name is a new alternative. (Eve pick Dave: New Alternative "Dave")
 * Inserting an empty line (just return), the input chances ends end the results are printed.
 *
 * The input is read over System.in, therefore a file can also be piped in.
 * cat example.txt | java -jar build/libs/app.jar de.nox.liquiddemocracy.Main --open
 *
 * @author Ngoc (Nox) Le
 * @date 2021-05-20
 * @version 0.1
 *
 */
package de.nox.liquiddemocracy;

// for printing
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

// for system.in reading.
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {

	public final static String RESULTF = "    %4d %s\n"; // formatting the results.

	public final static void main(String[] args) {

		LiquidDemocracy democracy = new LiquidDemocracy();

		boolean warned = false;
		boolean openVotes = Arrays.asList(args).contains("--open");

		/* Read from System.in; May also catch NullPointerException (very unlikely here) */
		try {
			/* Read the input from standard in. */
			BufferedReader buffReader = new BufferedReader(new InputStreamReader(System.in));

			String line = "";
			ReadCommand command;

			while ((line = buffReader.readLine()) != null && line.length() != 0) {

				command = readLine(line);

				/* Skip no action / no voter. */
				if (!command.isValid()) {
					System.err.println("[Warning] Invalid line, skip this line (\"" + line + "\").");
					warned = true;
					continue;
				}

				/* Do the actual democracy vote. */
				switch (command.action) {
					case "pick": democracy.pick(command.voter, command.choice); break;
					case "delegate": democracy.delegate(command.voter, command.choice); break;

					default: democracy.pick(command.voter, null); break; // invalid
				}
			}

		} catch (IOException | NullPointerException e) {
			System.err.println("[Error] Unexpected error: " + e);
			e.printStackTrace();
		}

		/* Free line below warnings. */
		if (warned) System.out.println();

		printSortedResult(democracy.getResults());

		/* If open was demanded, show who pick what. */
		if (openVotes) {
			System.out.println("\nOpen Votes:");
			democracy.getResultingChoices().forEach((voter, choice) -> {
					if (choice != null) {
						System.out.printf("    %-15s -->  %15s\n", voter, choice);
					} else {
						System.out.printf("  ! %-15s %21s\n", voter, "(invalid choice)");
					}
				});
		}
	}

	/** Just a simple Triple. */
	public static class ReadCommand {
		final String voter, action, choice;

		private final String formatted;

		/** Constructor. */
		ReadCommand(String voter, String action, String choice) {
			this.voter = voter;
			this.action = action;
			this.choice = choice;

			formatted = String.format("%s %s %s", voter, action, choice);
		}

		@Override public String toString() {
			return formatted;
		}

		/** Command is valid if the action and a voter are given.
		 * @return true if action and voter are non-null. */
		boolean isValid() {
			return voter != null && action != null;
		}
	}

	/** Parse a normal String line into a simple command.
	 * @return command with (voter, action, choice). */
	public static ReadCommand readLine(String line) {
		String[] words;
		String voter, action, choice;

		/* split on action.
		 * Regex: split on, CASE SENSITIVE.
		 * - space group followed by "pick(s)" or "delegate(s)"
		 * - and any space group following "pick(s)" or "delegate(s)"
		 * Note: Following splits s from the action. */
		words = line.split("\\s+(?=(pick|delegates?))|(?<=(pick|delegate))s?\\s+");

		/* Define the words. */
		return new ReadCommand(
				// voter
				words.length > 0 && words[0].length() > 0 ? words[0].trim() : null,

				// action
				words.length > 1 && words[1].matches("(pick|delegate)") ? words[1] : null,

				// choice
				words.length > 2 && words[2].length() > 0 ? words[2].trim() : null
				);
	}

	/** Pretty printint the results like requested, also ordering the pure results. */
	public static void printSortedResult(LiquidDemocracy.Result results) {
		if (results != null) {
			/* Sort valid choices. */
			results.choices.entrySet().stream()

				/* Reverse ordering. */
				.sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))

				/* Print out. */
				.forEach(e -> System.out.printf(RESULTF, e.getValue(), e.getKey()));

			/* Append invalid choices. */
			System.out.printf(RESULTF, results.invalidVoteCount, "Invalid");
		} else {
			System.out.println("No results!");
		}
	}
}
