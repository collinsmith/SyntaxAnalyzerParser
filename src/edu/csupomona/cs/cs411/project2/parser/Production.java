package edu.csupomona.cs.cs411.project2.parser;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public class Production implements Iterable<Integer> {
	private static final String RHS_DELIMIATER = "->";

	private final Integer NONTERMINAL;
	private final int POSITION_MARKER;
	private final Production ANCESTOR;
	private final ImmutableList<Integer> RHS;

	public Production(Integer nonterminal, ImmutableList<Integer> rhs) {
		this.ANCESTOR = this;
		this.POSITION_MARKER = 0;
		this.RHS = Objects.requireNonNull(rhs);
		this.NONTERMINAL = Objects.requireNonNull(nonterminal);
	}

	private Production(Production p) {
		assert p != null;
		this.RHS = p.RHS;
		this.ANCESTOR = p.ANCESTOR;
		this.NONTERMINAL = p.NONTERMINAL;
		this.POSITION_MARKER = p.POSITION_MARKER+1;
	}

	public Integer getNonterminal() {
		return NONTERMINAL;
	}

	public Production getAncestor() {
		return ANCESTOR;
	}

	public boolean hasNext() {
		return POSITION_MARKER < RHS.size();
	}

	public Production next() {
		return new Production(this);
	}

	public Integer current() {
		if (POSITION_MARKER == 0) {
			return null;
		}

		return RHS.get(POSITION_MARKER-1);
	}

	public Integer peek() {
		if (!hasNext()) {
			return null;
		}

		return RHS.get(POSITION_MARKER);
	}

	public int size() {
		return RHS.size();
	}

	@Override
	public Iterator<Integer> iterator() {
		return RHS.listIterator(POSITION_MARKER);
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
		if (this.NONTERMINAL != other.NONTERMINAL || this.POSITION_MARKER != other.POSITION_MARKER || this.size() != other.size()) {
			return false;
		}

		return this.RHS.equals(other.RHS);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.NONTERMINAL, this.POSITION_MARKER, this.RHS);
	}

	public String toString(Map<Integer, String> translator) {
		Preconditions.checkNotNull(translator);

		StringBuilder sb = new StringBuilder(String.format("%16s %s", translator.get(NONTERMINAL), RHS_DELIMIATER));

		int i = 0;
		for (Integer symbol : RHS) {
			if (i == POSITION_MARKER) {
				sb.append(" .");
			}

			sb.append(String.format(" %s", translator.get(symbol)));
			i++;
		}

		if (POSITION_MARKER == RHS.size()) {
			sb.append(" .");
		}

		return sb.toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(String.format("%d %s", NONTERMINAL, RHS_DELIMIATER));

		int i = 0;
		for (Integer symbol : RHS) {
			if (i == POSITION_MARKER) {
				sb.append(" .");
			}

			sb.append(String.format(" %d", symbol));
			i++;
		}

		if (POSITION_MARKER == RHS.size()) {
			sb.append(" .");
		}

		return sb.toString();
	}
}
