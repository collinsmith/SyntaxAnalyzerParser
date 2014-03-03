package edu.csupomona.cs.cs411.project2.preprocessor;

import java.util.Iterator;
import java.util.List;

public class Production implements Iterable<Integer> {
	private final Integer nonterminal;
	private final List<Integer> list;

	private final int dot;

	private Goto _goto;

	/**
	 * Constructs a production from a list of generated symbols, and
	 * initializes the current parser pointer to the start of that list.
	 *
	 * @param nonterminal nonterminal for this production
	 * @param l list of integers representing symbol identifiers
	 */
	public Production(Integer nonterminal, List<Integer> l) {
		this.nonterminal = nonterminal;
		this.list = l;
		this.dot = 0;
	}

	/**
	 * Constructs a production which is based on a previous production,
	 * except that this new production will have its iterator initialized
	 * where the old one left off.
	 *
	 * @param p production to base this one off of
	 */
	public Production(Production p) {
		this.nonterminal = p.nonterminal;
		this.list = p.list;
		this.dot = p.dot+1;
	}

	public void setGoto(int tableid) {
		_goto = new Goto(tableid, getHead());
	}

	@Override
	public Iterator<Integer> iterator() {
		return list.iterator();
	}

	public void add(Integer e) {
		list.add(e);
	}

	public boolean hasMore() {
		return dot < list.size();
	}

	public Integer getPrevious() {
		if (dot-1 < 0) {
			return null;
		}

		return list.get(dot-1);
	}

	public Integer getHead() {
		if (list.size() <= dot) {
			return 0;
		}

		return list.get(dot);
	}

	public boolean sameAs(Production p) {
		return this.dot == p.dot && this.list == p.list && this.nonterminal == p.nonterminal;
	}

	@Override
	public String toString() {
		int i;
		StringBuilder sb = new StringBuilder();
		sb.append(nonterminal);
		sb.append(" -> ");
		for (i = 0; i < list.size(); i++) {
			if (i == dot) {
				sb.append(".");
				sb.append(' ');
			}

			sb.append(list.get(i));
			sb.append(' ');
		}

		if (list.size() <= dot) {
			if (i == dot) {
				sb.append(".");
				sb.append(' ');
			}
		}

		if (_goto != null) {
			sb.append("\t");
			sb.append(_goto);
		}

		return sb.toString();
	}
}
