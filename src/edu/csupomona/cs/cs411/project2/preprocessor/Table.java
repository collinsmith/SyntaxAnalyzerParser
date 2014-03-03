package edu.csupomona.cs.cs411.project2.preprocessor;

import java.util.Iterator;
import java.util.List;

public class Table implements Iterable<Production> {
	private int tableid;
	private List<Production> initialProductions;
	private List<Production> closure;

	private int _goto;
	private Integer from;

	public Table(int tableid, List<Production> initialProductions, List<Production> closure) {
		this.tableid = tableid;
		this.initialProductions = initialProductions;
		this.closure = closure;
	}

	public void setGoto(int _goto, Integer from) {
		this._goto = _goto;
		this.from = from;
	}

	@Override
	public Iterator<Production> iterator() {
		final Iterator<Production> initialIterator = initialProductions.iterator();
		final Iterator<Production> closureIterator = closure.iterator();
		return new Iterator<Production>() {
			@Override
			public boolean hasNext() {
				return initialIterator.hasNext() || closureIterator.hasNext();
			}

			@Override
			public Production next() {
				if (initialIterator.hasNext()) {
					return initialIterator.next();
				}

				return closureIterator.next();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("Not supported.");
			}
		};
	}

	public List<Production> getInitialProductions() {
		return initialProductions;
	}

	public List<Production> getClosure() {
		return closure;
	}

	@Override
	public String toString() {
		return String.format("A%d, goto(%d, %d)", tableid, _goto, from);
	}

	public int getTableId() {
		return tableid;
	}

	public void print() {
		System.out.format("%s%n", this);

		for (Production p : initialProductions) {
			System.out.println("I:" + p);
		}

		for (Production p : closure) {
			System.out.println("  " + p);
		}

		System.out.println();
	}
}
