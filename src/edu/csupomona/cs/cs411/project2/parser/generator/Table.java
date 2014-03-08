package edu.csupomona.cs.cs411.project2.parser.generator;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Table implements Iterable<Production> {
	private final int ID;
	private final Table PARENT;
	private final List<Integer> VIABLE_PREFIX;
	private final Map<Integer, Table> TRANSITIONS;
	private final Set<Production> INITIAL_PRODUCTIONS;
	private final Set<Production> CLOSURE_PRODUCTIONS;

	public Table(int id, Table parent, List<Integer> viablePrefix, Collection<Production> initialProductions, Collection<Production> closureProductions) {
		this.ID = id;
		this.PARENT = parent;
		this.TRANSITIONS = new HashMap<>();
		this.VIABLE_PREFIX = Collections.unmodifiableList(viablePrefix);
		this.INITIAL_PRODUCTIONS = Collections.unmodifiableSet(new HashSet<>(initialProductions));
		this.CLOSURE_PRODUCTIONS = Collections.unmodifiableSet(new HashSet<>(closureProductions));
	}

	public int getId() {
		return ID;
	}

	public Table getParent() {
		return PARENT;
	}

	public List<Integer> getViablePrefix() {
		return VIABLE_PREFIX;
	}

	public Set<Production> getInitialProductions() {
		return INITIAL_PRODUCTIONS;
	}

	public Set<Production> getClosureProductions() {
		return CLOSURE_PRODUCTIONS;
	}

	public boolean containsTransitionForSymbol(Integer symbol) {
		return TRANSITIONS.containsKey(symbol);
	}

	public void putTransition(Integer symbol, Table t) {
		TRANSITIONS.put(symbol, t);
	}

	public Table getTransition(Integer symbol) {
		return TRANSITIONS.get(symbol);
	}

	public boolean containsAll(Collection<Production> productions) {
		return INITIAL_PRODUCTIONS.containsAll(productions);
	}

	@Override
	public Iterator<Production> iterator() {
		return new Iterator<Production>() {
			private final Iterator<Production> INITIALS = INITIAL_PRODUCTIONS.iterator();
			private final Iterator<Production> CLOSURES = CLOSURE_PRODUCTIONS.iterator();

			@Override
			public boolean hasNext() {
				return INITIALS.hasNext() || CLOSURES.hasNext();
			}

			@Override
			public Production next() {
				return INITIALS.hasNext() ? INITIALS.next() : CLOSURES.next();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("Not supported.");
			}
		};
	}

	public Metadata getMetadata(Set<Production> nextInitialProductions, Integer nextSymbol) {
		return new Metadata(this, nextInitialProductions, nextSymbol);
	}

	public static final class Metadata {
		private final Table PARENT;
		private final Integer NEXT_SYMBOL;
		private final Set<Production> NEXT_INITIAL_PRODUCTIONS;

		private Metadata(Table parent, Set<Production> nextInitialProductions, Integer nextSymbol) {
			this.PARENT = parent;
			this.NEXT_SYMBOL = nextSymbol;
			this.NEXT_INITIAL_PRODUCTIONS = Collections.unmodifiableSet(nextInitialProductions);
		}

		public Table getParent() {
			return PARENT;
		}

		public Integer getNextSymbol() {
			return NEXT_SYMBOL;
		}

		public Set<Production> getNextInitialProductions() {
			return NEXT_INITIAL_PRODUCTIONS;
		}
	}
}
