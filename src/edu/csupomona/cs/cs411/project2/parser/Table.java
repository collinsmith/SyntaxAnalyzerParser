package edu.csupomona.cs.cs411.project2.parser;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public class Table implements Iterable<Production> {
	private final int ID;
	private final Table PARENT;
	private final Map<Integer, Table> TRANSITIONS;
	private final ImmutableList<Integer> VIABLE_PREFIX;
	private final ImmutableSet<Production> INITIAL_PRODUCTIONS;
	private final ImmutableSet<Production> CLOSURE_PRODUCTIONS;

	public Table(
		int id,
		Table parent,
		ImmutableList<Integer> viablePrefix,
		ImmutableSet<Production> initialProductions,
		ImmutableSet<Production> closureProductions
	) {
		this.ID = id;
		this.PARENT = parent;
		this.TRANSITIONS = new HashMap<>();
		this.VIABLE_PREFIX = Objects.requireNonNull(viablePrefix);
		this.INITIAL_PRODUCTIONS = Objects.requireNonNull(initialProductions);
		this.CLOSURE_PRODUCTIONS = Objects.requireNonNull(closureProductions);
	}

	public int getId() {
		return ID;
	}

	public Table getParent() {
		return PARENT;
	}

	public boolean containsTransitionFor(Integer symbol) {
		return TRANSITIONS.containsKey(symbol);
	}

	public Table getTransitionFor(Integer symbol) {
		return TRANSITIONS.get(symbol);
	}

	public Table putTransition(Integer symbol, Table t) {
		return TRANSITIONS.put(symbol, t);
	}

	public ImmutableList<Integer> getViablePrefix() {
		return VIABLE_PREFIX;
	}

	public ImmutableSet<Production> getInitialProductions() {
		return INITIAL_PRODUCTIONS;
	}

	public ImmutableSet<Production> getClosureProductions() {
		return CLOSURE_PRODUCTIONS;
	}

	@Override
	public Iterator<Production> iterator() {
		return Iterators.concat(INITIAL_PRODUCTIONS.iterator(), CLOSURE_PRODUCTIONS.iterator());
	}

	public Metadata getMetadataForChild(Integer nextSymbol, ImmutableSet<Production> nextInitialProductions) {
		return new Metadata(this, nextSymbol, nextInitialProductions);
	}

	public static final class Metadata {
		private final Integer NEXT_SYMBOL;
		private final Table PARENT;
		private final ImmutableSet<Production> NEXT_INITIAL_PRODUCTIONS;

		private Metadata(
			Table parent,
			Integer nextSymbol,
			ImmutableSet<Production> nextInitialProductions
		) {
			this.PARENT = Objects.requireNonNull(parent);
			this.NEXT_SYMBOL = Objects.requireNonNull(nextSymbol);
			this.NEXT_INITIAL_PRODUCTIONS = Objects.requireNonNull(nextInitialProductions);
		}

		public Table getParent() {
			return PARENT;
		}

		public Integer getNextSymbol() {
			return NEXT_SYMBOL;
		}

		public ImmutableSet<Production> getNextInitialProductions() {
			return NEXT_INITIAL_PRODUCTIONS;
		}
	}
}
