package edu.csupomona.cs.cs411.project2.parser.generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public final class Production implements Iterable<Integer> {
	private static final String RHS_DELIMIATER = "->";

	private final Integer SYMBOL;
	private final Production ANCESTOR;
	private final int POSITION_MARKER;

	private List<Integer> rhs;

	public Production(Integer symbol, List<Integer> rhs) {
		this.SYMBOL = symbol;
		this.ANCESTOR = this;
		this.POSITION_MARKER = 0;

		this.rhs = rhs;
	}

	private Production(Production p) {
		this.SYMBOL = p.SYMBOL;
		this.ANCESTOR = p.ANCESTOR;
		this.POSITION_MARKER = p.POSITION_MARKER+1;

		this.rhs = p.rhs;
	}

	public Production getAncestor() {
		return ANCESTOR;
	}

	public boolean hasNext() {
		return POSITION_MARKER < rhs.size();
	}

	public Production getNext() {
		return new Production(this);
	}

	public Integer peek() {
		if (!hasNext()) {
			return null;
		}

		return rhs.get(POSITION_MARKER);
	}

	public void add(Integer symbol) {
		rhs.add(symbol);
	}

	public int size() {
		return rhs.size();
	}

	public void setImmutable() {
		if (rhs instanceof ArrayList) {
			((ArrayList)rhs).trimToSize();
		}

		rhs = Collections.unmodifiableList(rhs);
	}

	@Override
	public Iterator<Integer> iterator() {
		return rhs.listIterator(POSITION_MARKER);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		if (!(obj instanceof Production)) {
			return false;
		}

		Production other = (Production)obj;
		if (this.SYMBOL != other.SYMBOL || this.POSITION_MARKER != other.POSITION_MARKER || this.size() != other.size()) {
			return false;
		}

		return this.rhs.equals(other.rhs);
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 31 * hash + Objects.hashCode(this.SYMBOL);
		hash = 31 * hash + this.POSITION_MARKER;
		hash = 31 * hash + Objects.hashCode(this.rhs);
		return hash;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(String.format("%d %s", SYMBOL, RHS_DELIMIATER));

		int i = 0;
		for (Integer symbol : rhs) {
			if (i == POSITION_MARKER) {
				sb.append(" .");
			}

			sb.append(String.format(" %d", symbol));
			i++;
		}

		if (POSITION_MARKER == rhs.size()) {
			sb.append(" .");
		}

		return sb.toString();
	}
}
