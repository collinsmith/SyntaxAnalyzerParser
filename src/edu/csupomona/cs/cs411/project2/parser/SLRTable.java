package edu.csupomona.cs.cs411.project2.parser;

import java.io.Serializable;

public class SLRTable implements Serializable {
	private transient final int SYMBOL = 0;
	private transient final int NEXT = 1;

	private final int NUM_TABLES;

	private final Shift _shift;
	private final Reduce _reduce;
	private final Goto _goto;

	public SLRTable(int num_tables, Shift _shift, Reduce _reduce, Goto _goto) {
		NUM_TABLES = num_tables;

		this._shift = _shift;
		this._reduce = _reduce;
		this._goto = _goto;
	}

	public void print() {
		System.out.format("Shift:%n\tSwitch: %d%n\tShift: %d (%d)%n", _shift._switch.length, _shift._shift.length, _shift._shift[0].length);
		System.out.format("Reduce:%n\tReduce: %d%n", _reduce._reduce.length);
		System.out.format("Goto:%n\tSwitch: %d%n\tGoto: %d (%d)%n", _goto._switch.length, _goto._goto.length, _goto._goto[0].length);
	}

	public static class Shift {
		private int[] _switch;
		private int[][] _shift;

		public Shift(int[] _switch, int[][] _shift) {
			this._switch = _switch;
			this._shift = _shift;
		}
	}

	public static class Reduce {
		private int[] _reduce;

		public Reduce(int[] _reduce) {
			this._reduce = _reduce;
		}
	}

	public static class Goto {
		private int[] _switch;
		private int[][] _goto;

		public Goto(int[] _switch, int[][] _goto) {
			this._switch = _switch;
			this._goto = _goto;
		}
	}
}
