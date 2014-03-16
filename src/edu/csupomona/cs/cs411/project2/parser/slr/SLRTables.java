package edu.csupomona.cs.cs411.project2.parser.slr;

import edu.csupomona.cs.cs411.project1.lexer.Token;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * This class represents a compact, serializable set of arrays which store
 * information that can be used by an {@link SLRParser}.
 *
 * @author Collin Smith <collinsmith@csupomona.edu>
 */
public final class SLRTables implements Serializable {
	/**
	 * Index within the second dimension of the shift and reduce arrays where
	 * the symbols are stored.
	 */
	public static final int SYM = 0;

	/**
	 * Index within the second dimension of the shift and reduce arrays where
	 * the next elements are stored.
	 */
	public static final int NXT = 1;

	/**
	 * Goto tables for this object
	 */
	private final GotoTable GOTO;

	/**
	 * Shift tables for this object
	 */
	private final ShiftTable SHIFT;

	/**
	 * Reduce tables for this object
	 */
	private final ReduceTable REDUCE;

	/**
	 * Production tables for this object
	 */
	private final ProductionTable PRODUCTION;

	/**
	 * This field represents the number of shift-reduce conflicts detected
	 * while generating the SLRTables. This is mainly stored so that this
	 * table knows how many conflicts it has.
	 */
	private final int NUM_SHIFT_REDUCE_CONFLICTS;

	/**
	 * This field represents the number of reduce-reduce conflicts detected
	 * while generating the SLRTables. This is mainly stored so that this
	 * table knows how many conflicts it has.
	 */
	private final int NUM_REDUCE_REDUCE_CONFLICTS;

	/**
	 * Constructs a SLRTables with the given arguments.
	 *
	 * @param _shift shift tables
	 * @param reduce reduce tables
	 * @param _goto goto tables
	 * @param production production tables
	 * @param numShiftReduceConflicts number of shift-reduce conflicts
	 * @param numReduceReduceConflicts number of reduce-reduce conflicts
	 */
	private SLRTables(ShiftTable _shift, ReduceTable reduce, GotoTable _goto, ProductionTable production, int numShiftReduceConflicts, int numReduceReduceConflicts) {
		if (_shift.SWITCH.length != reduce.REDUCE.length || reduce.REDUCE.length != _goto.SWITCH.length) {
			throw new IllegalArgumentException("Table sizes do not match!");
		}

		this.GOTO = _goto;
		this.SHIFT = _shift;
		this.REDUCE = reduce;
		this.PRODUCTION = production;

		this.NUM_SHIFT_REDUCE_CONFLICTS = numShiftReduceConflicts;
		this.NUM_REDUCE_REDUCE_CONFLICTS = numReduceReduceConflicts;
	}

	/**
	 * Builds a SLRTables with the given arguments.
	 *
	 * @param _shift shift tables
	 * @param reduce reduce tables
	 * @param _goto goto tables
	 * @param production production tables
	 * @param numShiftReduceConflicts number of shift-reduce conflicts
	 * @param numReduceReduceConflicts number of reduce-reduce conflicts
	 * @return the SLRTables generated
	 */
	public static SLRTables build(ShiftTable _shift, ReduceTable reduce, GotoTable _goto, ProductionTable production, int numShiftReduceConflicts, int numReduceReduceConflicts) {
		return new SLRTables(_shift, reduce, _goto, production, numShiftReduceConflicts, numReduceReduceConflicts);
	}

	/**
	 * Returns the shift action associated with a given state and symbol.
	 *
	 * @param table state of the parser
	 * @param symbol symbol to check shift action for
	 * @return next table to perform shift on or {@link Integer#MIN_VALUE} if
	 *	none exists
	 */
	public int shift(int table, int symbol) {
		int id = SHIFT.SWITCH[table];
		if (id == Integer.MIN_VALUE) {
			return Integer.MIN_VALUE;
		}

		int cache;
		while ((cache = SHIFT.SHIFT[id][SYM]) != Integer.MIN_VALUE && cache != symbol) {
			id++;
		}

		return cache == symbol ? SHIFT.SHIFT[id][NXT] : Integer.MIN_VALUE;
	}

	/**
	 * Returns the table which the specified state should reduce to.
	 *
	 * @param table state of the parser
	 * @return table to reduce to or {@link Integer#MIN_VALUE} if none exists
	 */
	public int reduce(int table) {
		return REDUCE.REDUCE[table];
	}

	/**
	 * Returns the goto action associated with a given state and symbol.
	 *
	 * @param table state of the parser
	 * @param symbol symbol to check goto action for
	 * @return next table to perform goto on or {@link Integer#MIN_VALUE} if
	 *	none exists
	 */
	public int move(int table, int symbol) {
		int id = GOTO.SWITCH[table];
		if (id == Integer.MIN_VALUE) {
			return Integer.MIN_VALUE;
		}

		int cache;
		while ((cache = GOTO.GOTO[id][SYM]) != Integer.MIN_VALUE && cache != symbol) {
			id++;
		}

		return cache == symbol ? GOTO.GOTO[id][NXT] : Integer.MIN_VALUE;
	}

	/**
	 * Returns the nonterminal identifier associated with a given production
	 * id.
	 *
	 * @param production production to check
	 * @return identifier for the left-hand side nonterminal
	 */
	public int getNonterminalId(int production) {
		return PRODUCTION.LHS[production];
	}

	/**
	 * Returns the number of symbols generated by a given production id.
	 *
	 * @param production production to check
	 * @return number of symbols the production generates
	 */
	public int getRHSSize(int production) {
		return PRODUCTION.RHS[production];
	}

	/**
	 * Returns the identifier associated with a given {@link Token}
	 *
	 * @param t token to check
	 * @return identifier of that Token or {@link Integer#MIN_VALUE} if the
	 *	Token is {@code null}
	 */
	public int getTokenId(Token t) {
		if (t == null) {
			return Integer.MIN_VALUE;
		}

		return t.getId();
	}

	/**
	 * Outputs the table to a file. Note that this only generates the file for
	 * as many tables as there are. There may actually be a very long list of
	 * shift and goto elements, which unfortunately cannot be printed easily.
	 */
	public void outputTableInfo() {
		Charset charset = Charset.forName("US-ASCII");
		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(".", "output", "toy.slrtables.txt"), charset, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			writer.append(String.format("%-8s: switch = %d%n", "Shift", SHIFT.SWITCH.length));
			writer.append(String.format("%8s  shift = %d%n", "", SHIFT.SHIFT.length));
			writer.append(String.format("%-8s: reduce = %d%n", "Reduce", REDUCE.REDUCE.length));
			writer.append(String.format("%-8s: switch = %d%n", "Goto", GOTO.SWITCH.length));
			writer.append(String.format("%8s  goto = %d%n", "", GOTO.GOTO.length));
			writer.append(String.format("%n%n"));

			writer.append(String.format("%d tables%n", SHIFT.SWITCH.length));
			writer.append(String.format("%d shift-reduce conflicts%n", NUM_SHIFT_REDUCE_CONFLICTS));
			writer.append(String.format("%d reduce-reduce conflicts%n", NUM_REDUCE_REDUCE_CONFLICTS));
			writer.append(String.format("%n%n"));

			writer.append(String.format("%-30s|%-12s|%-30s%n", "SHIFT", "REDUCE", "GOTO"));
			writer.append(String.format("%-6s%-6s%-6s%-6s%-6s|%-6s%-6s|%-6s%-6s%-6s%-6s%-6s%n", "", "switch", "", "symbol", "next", "", "reduce", "", "switch", "", "symbol", "next"));

			// TODO this will only do shift number of tables and not the entire thing
			int i;
			for (i = 0; i < SHIFT.SWITCH.length; i++) {
				writer.append(String.format("%-6s%-6s%-6s%-6s%-6s|%-6s%-6s|%-6s%-6s%-6s%-6s%-6s%n",
					String.format("A%d", i), convertValue(SHIFT.SWITCH[i]), i+1, convertValue(SHIFT.SHIFT[i+1][SYM]), convertValue(SHIFT.SHIFT[i+1][NXT]),
					String.format("A%d", i), convertValue(REDUCE.REDUCE[i]),
					String.format("A%d", i), convertValue(GOTO.SWITCH[i]), i+1, convertValue(GOTO.GOTO[i+1][SYM]), convertValue(GOTO.GOTO[i+1][NXT])
				));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns the String representation of a given integer, or an empty
	 * String if the value is {@link Integer#MIN_VALUE}.
	 *
	 * @param i integer to convert
	 * @return String representation of {code i}, or an empty String if
	 *	{code i} is {@link Integer#MIN_VALUE}.
	 */
	private String convertValue(int i) {
		if (i == Integer.MIN_VALUE) {
			return "";
		}

		return Integer.toString(i);
	}

	/**
	 * This class encapsulates the shift tables used in a {@link SLRParser}
	 */
	public static class ShiftTable {
		/**
		 * Switch array which points to the starting index for a given
		 * table within the shift array.
		 */
		private final int[] SWITCH;

		/**
		 * Contains a list of shift transitions associated with each table,
		 * separated by a {@link Integer#MIN_VALUE} pair.
		 */
		private final int[][] SHIFT;

		/**
		 * Constructs a shift table with the specified arguments.
		 *
		 * @param _switch switch array for this table
		 * @param shift shift array for this table
		 */
		public ShiftTable(int[] _switch, int[][] shift) {
			this.SWITCH = _switch;
			this.SHIFT = shift;
		}
	}

	/**
	 * This class encapsulates the reduce tables used in a {@link SLRParser}
	 */
	public static class ReduceTable {
		/**
		 * Array containing the reduction table ids for each table.
		 */
		private final int[] REDUCE;

		/**
		 * Constructs a reduce table with the specified argument.
		 *
		 * @param reduce reduce array for this table
		 */
		public ReduceTable(int[] reduce) {
			this.REDUCE = reduce;
		}
	}

	/**
	 * This class encapsulates the goto tables used in a {@link SLRParser}
	 */
	public static class GotoTable {
		/**
		 * Switch array which points to the starting index for a given
		 * table within the goto array.
		 */
		private final int[] SWITCH;

		/**
		 * Contains a list of goto transitions associated with each table,
		 * separated by a {@link Integer#MIN_VALUE} pair.
		 */
		private final int[][] GOTO;

		/**
		 * Constructs a shift table with the specified arguments.
		 *
		 * @param _switch switch array for this table
		 * @param _goto goto array for this table
		 */
		public GotoTable(int[] _switch, int[][] _goto) {
			this.SWITCH = _switch;
			this.GOTO = _goto;
		}
	}

	/**
	 * This class encapsulates the production tables used in a
	 * {@link SLRParser}
	 */
	public static class ProductionTable {
		/**
		 * Stores the nonterminal id for every production id.
		 */
		private final int[] LHS;

		/**
		 * Stores the number of symbols on the rhs of every production id.
		 */
		private final int[] RHS;

		/**
		 * Constructs a production table with the specified arguments.
		 *
		 * @param lhs lhs array for this table
		 * @param rhs rhs array for this table
		 */
		public ProductionTable(int[] lhs, int[] rhs) {
			this.LHS = lhs;
			this.RHS = rhs;
		}
	}
}
