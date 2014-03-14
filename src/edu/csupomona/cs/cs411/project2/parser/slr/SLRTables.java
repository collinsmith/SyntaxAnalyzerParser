package edu.csupomona.cs.cs411.project2.parser.slr;

import edu.csupomona.cs.cs411.project1.lexer.Token;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public final class SLRTables {
	public static final int SYM = 0;
	public static final int NXT = 1;

	private final GotoTable GOTO;
	private final ShiftTable SHIFT;
	private final ReduceTable REDUCE;
	private final ProductionTable PRODUCTION;

	private final int NUM_SHIFT_REDUCE_CONFLICTS;
	private final int NUM_REDUCE_REDUCE_CONFLICTS;

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

	public static SLRTables build(ShiftTable _shift, ReduceTable reduce, GotoTable _goto, ProductionTable production, int numShiftReduceConflicts, int numReduceReduceConflicts) {
		return new SLRTables(_shift, reduce, _goto, production, numShiftReduceConflicts, numReduceReduceConflicts);
	}

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

	public int reduce(int table) {
		return REDUCE.REDUCE[table];
	}

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

	public int getNonterminalId(int production) {
		return PRODUCTION.LHS[production];
	}

	public int getRHSSize(int production) {
		return PRODUCTION.RHS[production];
	}

	public int getTokenId(Token t) {
		if (t == null) {
			return Integer.MIN_VALUE;
		}

		return t.getId();
	}

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

	private String convertValue(int i) {
		if (i == Integer.MIN_VALUE) {
			return "";
		}

		return Integer.toString(i);
	}

	public static class ShiftTable {
		private final int[] SWITCH;
		private final int[][] SHIFT;

		public ShiftTable(int[] _switch, int[][] shift) {
			this.SWITCH = _switch;
			this.SHIFT = shift;
		}
	}

	public static class ReduceTable {
		private final int[] REDUCE;

		public ReduceTable(int[] reduce) {
			this.REDUCE = reduce;
		}
	}

	public static class GotoTable {
		private final int[] SWITCH;
		private final int[][] GOTO;

		public GotoTable(int[] _switch, int[][] _goto) {
			this.SWITCH = _switch;
			this.GOTO = _goto;
		}
	}

	public static class ProductionTable {
		private final int[] LHS;
		private final int[] RHS;

		public ProductionTable(int[] lhs, int[] rhs) {
			this.LHS = lhs;
			this.RHS = rhs;
		}
	}
}
