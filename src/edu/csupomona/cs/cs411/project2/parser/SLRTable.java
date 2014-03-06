package edu.csupomona.cs.cs411.project2.parser;

import edu.csupomona.cs.cs411.project1.lexer.Token;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class SLRTable implements Serializable {
	public static final int SYMBOL = 0;
	public static final int NEXT = 1;

	private final int NUM_TABLES;
	private final int NUM_NONTERMINALS;
	private final int SHIFT_REDUCE_CONFLICTS;
	private final int REDUCE_REDUCE_CONFLICTS;
	private final int UNUSED_PRODUCTIONS;

	private final ShiftTable _shift;
	private final ReduceTable _reduce;
	private final GotoTable _goto;

	private final int[] _left;
	private final int[] _right;

	public SLRTable(int num_nonterminals, ShiftTable _shift, ReduceTable _reduce, GotoTable _goto, int[] _left, int[] _right, int shiftReduces, int reduceReduces, int unusedProductions) {
		if (_shift._switch.length != _reduce._reduce.length || _reduce._reduce.length != _goto._switch.length) {
			throw new IllegalArgumentException("Table sizes do not match!");
		}

		this._shift = _shift;
		this._reduce = _reduce;
		this._goto = _goto;

		this._left = _left;
		this._right = _right;

		this.NUM_TABLES = _shift._switch.length;
		this.NUM_NONTERMINALS = num_nonterminals;
		this.SHIFT_REDUCE_CONFLICTS = shiftReduces;
		this.REDUCE_REDUCE_CONFLICTS = reduceReduces;
		this.UNUSED_PRODUCTIONS = unusedProductions;
	}

	public int getTokenAsSymbol(Token t) {
		if (t == null) {
			return Integer.MIN_VALUE;
		}

		return NUM_NONTERMINALS+t.getId()+1;
	}

	public int shift(int table, int symbol) {
		int id = _shift._switch[table];
		if (id == Integer.MIN_VALUE) {
			return Integer.MIN_VALUE;
		}

		int cache;
		while ((cache = _shift._shift[id][SYMBOL]) != Integer.MIN_VALUE && cache != symbol) {
			id++;
		}

		return cache == symbol ? _shift._shift[id][NEXT] : Integer.MIN_VALUE;
	}

	public int reduce(int table) {
		return _reduce._reduce[table];
	}

	public int left(int production) {
		return _left[production];
	}

	public int right(int production) {
		return _right[production];
	}

	public int move(int table, int symbol) {
		int id = _goto._switch[table];
		if (id == Integer.MIN_VALUE) {
			return Integer.MIN_VALUE;
		}

		int cache;
		while ((cache = _goto._goto[id][SYMBOL]) != Integer.MIN_VALUE && cache != symbol) {
			id++;
		}

		return cache == symbol ? _goto._goto[id][NEXT] : Integer.MIN_VALUE;
	}

	public void outputTableInfo() {
		Charset charset = Charset.forName("US-ASCII");
		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(".", "output", "toy.slrtables.txt"), charset, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			writer.append(String.format("%-8s: switch = %d%n", "Shift", _shift._switch.length));
			writer.append(String.format("%8s  shift = %d%n", "", _shift._shift.length));
			writer.append(String.format("%-8s: reduce = %d%n", "Reduce", _reduce._reduce.length));
			writer.append(String.format("%-8s: switch = %d%n", "Goto", _goto._switch.length));
			writer.append(String.format("%8s  goto = %d%n", "", _goto._goto.length));
			writer.append(String.format("%n%n"));

			writer.append(String.format("%d tables%n", NUM_TABLES));
			writer.append(String.format("%d unused productions%n", UNUSED_PRODUCTIONS));
			writer.append(String.format("%d shift-reduce conflicts%n", SHIFT_REDUCE_CONFLICTS));
			writer.append(String.format("%d reduce-reduce conflicts%n", REDUCE_REDUCE_CONFLICTS));
			writer.append(String.format("%n%n"));

			writer.append(String.format("%-30s|%-12s|%-30s%n", "SHIFT", "REDUCE", "GOTO"));
			writer.append(String.format("%-6s%-6s%-6s%-6s%-6s|%-6s%-6s|%-6s%-6s%-6s%-6s%-6s%n", "", "switch", "", "symbol", "next", "", "reduce", "", "switch", "", "symbol", "next"));

			int i;
			for (i = 0; i < _shift._switch.length; i++) {
				writer.append(String.format("%-6s%-6s%-6s%-6s%-6s|%-6s%-6s|%-6s%-6s%-6s%-6s%-6s%n",
					String.format("A%d", i), convertValue(_shift._switch[i]), i+1, convertValue(_shift._shift[i+1][SYMBOL]), convertValue(_shift._shift[i+1][NEXT]),
					String.format("A%d", i), convertValue(_reduce._reduce[i]),
					String.format("A%d", i), convertValue(_goto._switch[i]), i+1, convertValue(_goto._goto[i+1][SYMBOL]), convertValue(_goto._goto[i+1][NEXT])
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
		private int[] _switch;
		private int[][] _shift;

		public ShiftTable(int[] _switch, int[][] _shift) {
			this._switch = _switch;
			this._shift = _shift;
		}
	}

	public static class ReduceTable {
		private int[] _reduce;

		public ReduceTable(int[] _reduce) {
			this._reduce = _reduce;
		}
	}

	public static class GotoTable {
		private int[] _switch;
		private int[][] _goto;

		public GotoTable(int[] _switch, int[][] _goto) {
			this._switch = _switch;
			this._goto = _goto;
		}
	}
}
