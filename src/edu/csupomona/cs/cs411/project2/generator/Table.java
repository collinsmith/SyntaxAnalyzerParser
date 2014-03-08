package edu.csupomona.cs.cs411.project2.generator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Table implements Iterable<Production> {
	private final Goto FROM;
	private final int TABLEID;
	private final List<Production> INITIAL_PRODUCTIONS;
	private final List<Production> CLOSURE_PRODUCTIONS;
	private final Map<Integer, Table> GOTO;

	public Table(int tableId, List<Production> initialProductions, List<Production> closureProductions, Goto from) {
		this.FROM = from;
		this.TABLEID = tableId;
		this.INITIAL_PRODUCTIONS = initialProductions;
		this.CLOSURE_PRODUCTIONS = closureProductions;
		this.GOTO = new HashMap<>();
	}

	public Goto getGoto() {
		return FROM;
	}

	public int getTableId() {
		return TABLEID;
	}

	public List<Production> getInitialProductions() {
		return INITIAL_PRODUCTIONS;
	}

	public List<Production> getClosureProductions() {
		return CLOSURE_PRODUCTIONS;
	}

	public boolean containsTransition(Integer symbol) {
		return GOTO.containsKey(symbol);
	}

	public void putTransition(Integer symbol, Table t) {
		if (GOTO.containsKey(symbol)) {
			System.out.println("shift-shift conflict in table A" + getTableId());
		}

		GOTO.put(symbol, t);
	}

	public Table getTransition(Integer symbol) {
		return GOTO.get(symbol);
	}

	@Override
	public Iterator<Production> iterator() {
		return new Iterator<Production>() {
			final Iterator<Production> part1 = INITIAL_PRODUCTIONS.iterator();
			final Iterator<Production> part2 = CLOSURE_PRODUCTIONS.iterator();
			@Override
			public boolean hasNext() {
				return part1.hasNext() || part2.hasNext();
			}

			@Override
			public Production next() {
				if (part1.hasNext()) {
					return part1.next();
				}

				return part2.next();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("Not supported.");
			}
		};
	}

	@Override
	public String toString() {
		return String.format("A%d, %s", TABLEID, FROM);
	}
}
