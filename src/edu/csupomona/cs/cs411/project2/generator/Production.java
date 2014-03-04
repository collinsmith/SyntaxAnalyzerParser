package edu.csupomona.cs.cs411.project2.generator;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class Production implements Iterable<Integer> {
	private static final String PRODUCTION_DELIMINATOR = "->";

	private final int POINTER;
	private final int PRODUCTIONID;
	private final Production PARENT;
	private final Integer NONTERMINAL;
	private final List<Integer> PRODUCTIONS;

	//private Goto _goto;
	private boolean immutable;

	public Production(int productionId, Integer nonterminal, List<Integer> productions) {
		this.POINTER = 0;
		this.PARENT = null;
		this.NONTERMINAL = nonterminal;
		this.PRODUCTIONS = productions;
		this.PRODUCTIONID = productionId;

		//this._goto = null;
		this.immutable = false;
	}

	public Production(Production parent) {
		this.PARENT = parent;
		this.POINTER = this.PARENT.POINTER+1;
		this.NONTERMINAL = this.PARENT.NONTERMINAL;
		this.PRODUCTIONS = this.PARENT.PRODUCTIONS;
		this.PRODUCTIONID = this.PARENT.PRODUCTIONID;

		//this._goto = null;
		this.immutable = false;
	}

	public Production getParent() {
		return PARENT;
	}

	public Production getAncestor() {
		if (PARENT == null) {
			return this;
		}

		return PARENT.getAncestor();
	}

	public int getProductionId() {
		return PRODUCTIONID;
	}

	//public void setGoto(int tableId) {
	//	_goto = new Goto(tableId, getNextSymbol());
	//}

	//public void setGoto(Goto _goto) {
	//	this._goto = _goto;
	//}

	public void addSymbol(Integer i) {
		if (immutable) {
			throw new UnsupportedOperationException("This production is now immutable");
		}

		PRODUCTIONS.add(i);
	}

	public void setImmutable() {
		immutable = true;
	}

	public boolean hasMoreSymbols() {
		return POINTER < PRODUCTIONS.size();
	}

	public Integer getNextSymbol() {
		if (PRODUCTIONS.size() <= POINTER) {
			// TODO was 0, now null. How will this effect?
			return null;
		}

		return PRODUCTIONS.get(POINTER);
	}

	@Override
	public Iterator<Integer> iterator() {
		return PRODUCTIONS.listIterator(POINTER);
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}

		if (!(o instanceof Production)) {
			return false;
		}

		Production p = (Production)o;
		if (this.POINTER != p.POINTER || this.PARENT != p.PARENT || this.NONTERMINAL != p.NONTERMINAL) {
			return false;
		}

		boolean equal = this.PRODUCTIONS.size() == p.PRODUCTIONS.size();
		if (!equal) {
			return false;
		}

		for (int i = 0; i < this.PRODUCTIONS.size(); i++) {
			equal = this.PRODUCTIONS.get(i) == p.PRODUCTIONS.get(i);
			if (!equal) {
				return false;
			}
		}

		return true;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 19 * hash + this.POINTER;
		hash = 19 * hash + Objects.hashCode(this.PARENT);
		hash = 19 * hash + Objects.hashCode(this.NONTERMINAL);
		hash = 19 * hash + Objects.hashCode(this.PRODUCTIONS);
		//hash = 19 * hash + Objects.hashCode(this._goto);
		return hash;
	}

	@Override
	public String toString() {
		StringBuilder production = new StringBuilder();
		production.append(String.format("%s %s", NONTERMINAL, PRODUCTION_DELIMINATOR));
		for (int i = 0; i < PRODUCTIONS.size(); i++) {
			if (i == POINTER) {
				production.append(" .");
			}

			production.append(String.format(" %d", PRODUCTIONS.get(i)));
		}

		if (POINTER == PRODUCTIONS.size()) {
			production.append(" .");
		}

		/*StringBuilder productionWithGoto = new StringBuilder();
		if (_goto != null) {
			productionWithGoto.append(String.format("%-24s %s", production.toString(), _goto));
		} else {
			productionWithGoto.append(String.format("%-24s", production.toString()));
		}*/

		return production.toString();
	}
}
