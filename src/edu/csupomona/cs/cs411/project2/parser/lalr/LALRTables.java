package edu.csupomona.cs.cs411.project2.parser.lalr;

import edu.csupomona.cs.cs411.project2.parser.slr.SLRTables;

public final class LALRTables {
	private final SLRTables SLR_TABLES;
	private final Lookahead LOOKAHEAD;

	private LALRTables(SLRTables slrTables, Lookahead lookahead) {
		this.SLR_TABLES = slrTables;
		this.LOOKAHEAD = lookahead;
	}

	public static LALRTables build(SLRTables slrTables, Lookahead lookahead) {
		return new LALRTables(slrTables, lookahead);
	}

	public SLRTables getSLRTables() {
		return SLR_TABLES;
	}

	public boolean lookahead(int production, int symbol) {
		return ((LOOKAHEAD.LOOKAHEAD[production][symbol/Long.SIZE])&(symbol%Long.SIZE)) != 0;
	}

	public static class Lookahead {
		private final long[][] LOOKAHEAD;

		public Lookahead(long[][] _lookahead) {
			this.LOOKAHEAD = _lookahead;
		}
	}
}
