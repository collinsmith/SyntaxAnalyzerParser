package edu.csupomona.cs.cs411.project2.parser;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/**
 * This class represents a table to be used within a parser generator. A table
 * is essentially just a set of initial productions along with another set of
 * productions which represents the closure of those productions. Each table
 * also has a list of symbols which represents the viable prefix associated with
 * this Table and a reference to the table which generated it.
 *
 * @author Collin Smith <collinsmith@csupomona.edu>
 */
public class Table implements Iterable<Production> {
	/**
	 * This field represents the unique identifier associated with this Table.
	 * This number should be from 0 to Integer.MAX_VALUE inclusively.
	 */
	private final int ID;

	/**
	 * This field represents the reference to the parent of this Table, from
	 * which this table was generated.
	 */
	private final Table PARENT;

	/**
	 * This field represents the mapping from symbols to "next" tables.
	 */
	private final Map<Integer, Table> TRANSITIONS;

	/**
	 * This field represents the viable prefix associated with this Table.
	 */
	private final ImmutableList<Integer> VIABLE_PREFIX;

	/**
	 * This field represents the set of initial productions of this Table.
	 * This field is used to uniquely identify this Table, as no two tables
	 * should have the same set of initial productions.
	 */
	private final ImmutableSet<Production> INITIAL_PRODUCTIONS;

	/**
	 * This field represents the set of productions which closes over the
	 * initial productions.
	 */
	private final ImmutableSet<Production> CLOSURE_PRODUCTIONS;

	/**
	 * Constructor which creates a Table with the arguments passed.
	 *
	 * @param id unique identifier of this Table
	 * @param parent Table which this Table was generated from
	 * @param viablePrefix List of integers representing the viable prefix
	 * @param initialProductions Set of initial productions for this Table
	 * @param closureProductions Set of closure productions for this Table
	 */
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

	/**
	 * Returns the unique identifier of this table.
	 *
	 * @return an integer representing the identifier of this Table
	 */
	public int getId() {
		return ID;
	}

	/**
	 * Returns the parent associated with this Table
	 *
	 * @return the parent of this table
	 */
	public Table getParent() {
		return PARENT;
	}

	/**
	 * Returns whether or not there exist a transition in this table for a
	 * given symbol.
	 *
	 * @param symbol symbol (represented as an integer) to query
	 * @return {@code true} if it does, otherwise {@code false}
	 */
	public boolean containsTransitionFor(Integer symbol) {
		return TRANSITIONS.containsKey(symbol);
	}

	/**
	 * Returns a reference to the Table transition associated with the given
	 * symbol.
	 *
	 * @param symbol symbol to check
	 * @return parent Table, or {@code null} if the transition does not exist
	 */
	public Table getTransitionFor(Integer symbol) {
		return TRANSITIONS.get(symbol);
	}

	/**
	 * Creates a transition to a child Table using a given symbol as a
	 * reference.
	 *
	 * @param symbol symbol to create the transition for
	 * @param t child Table to transition to
	 * @return old Table used as the transition or {@code null} if none
	 */
	public Table putTransition(Integer symbol, Table t) {
		return TRANSITIONS.put(symbol, t);
	}

	/**
	 * Returns an immutable view of the list of viable integers representing
	 * the viable prefix for this Table.
	 *
	 * @return the list of viable prefix symbols of this Table
	 */
	public ImmutableList<Integer> getViablePrefix() {
		return VIABLE_PREFIX;
	}

	/**
	 * Returns the immutable set of initial Productions of this Table
	 *
	 * @return the immutable set of initial Productions of this Table
	 */
	public ImmutableSet<Production> getInitialProductions() {
		return INITIAL_PRODUCTIONS;
	}

	/**
	 * Returns the immutable set of closure Productions of this Table
	 *
	 * @return the immutable set of closure Productions of this Table
	 */
	public ImmutableSet<Production> getClosureProductions() {
		return CLOSURE_PRODUCTIONS;
	}

	/**
	 * Returns an iterator which can iterate through all Productions of this
	 * Table. This includes both the initial productions and then the closure
	 * productions.
	 *
	 * @return a concatenated iterator of all Productions in this Table
	 */
	@Override
	public Iterator<Production> iterator() {
		return Iterators.concat(INITIAL_PRODUCTIONS.iterator(), CLOSURE_PRODUCTIONS.iterator());
	}

	/**
	 * Generates the {@link Metadata} for this Table to be used in the
	 * generation of any child Tables.
	 *
	 * @param nextSymbol next symbol of the child Table
	 * @param nextInitialProductions set of initial productions of the child
	 * @return Metadata to help with the creation of child Tables of this
	 */
	public Metadata getMetadataForChild(Integer nextSymbol, ImmutableSet<Production> nextInitialProductions) {
		return new Metadata(this, nextSymbol, nextInitialProductions);
	}

	/**
	 * This class represents metadata to use when creating child Tables.
	 */
	public static final class Metadata {
		/**
		 * This field represents the next symbol to add to the viable
		 * prefix for the next Table. Every element in the set of initial
		 * productions should consume this symbol.
		 */
		private final Integer NEXT_SYMBOL;

		/**
		 * This field represents the Table this Metadata belongs to.
		 */
		private final Table PARENT;

		/**
		 * This field represents the set of initial productions for the next
		 * Table.
		 */
		private final ImmutableSet<Production> NEXT_INITIAL_PRODUCTIONS;

		/**
		 * Constructs a Metadata to store the Table which created it, the
		 * initial symbol for the next Table, as well as the set of initial
		 * Productions for that Table.
		 *
		 * @param parent
		 * @param nextSymbol
		 * @param nextInitialProductions
		 */
		private Metadata(
			Table parent,
			Integer nextSymbol,
			ImmutableSet<Production> nextInitialProductions
		) {
			this.PARENT = Objects.requireNonNull(parent);
			this.NEXT_SYMBOL = Objects.requireNonNull(nextSymbol);
			this.NEXT_INITIAL_PRODUCTIONS = Objects.requireNonNull(nextInitialProductions);
		}

		/**
		 * Returns a reference to the Table which this Metadata was created
		 * from.
		 *
		 * @return the parent of this Metadata
		 */
		public Table getParent() {
			return PARENT;
		}

		/**
		 * Returns the next symbol for the child table to generate.
		 *
		 * @return the next symbol for the child table to generate
		 */
		public Integer getNextSymbol() {
			return NEXT_SYMBOL;
		}

		/**
		 * Returns the set of initial Productions to be used for the next
		 * Table.
		 *
		 * @return set of initial productions for the next Table
		 */
		public ImmutableSet<Production> getNextInitialProductions() {
			return NEXT_INITIAL_PRODUCTIONS;
		}
	}
}
