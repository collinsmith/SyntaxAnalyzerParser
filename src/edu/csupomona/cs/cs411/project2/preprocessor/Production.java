package edu.csupomona.cs.cs411.project2.preprocessor;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class Production implements ListIterator<Integer>, Iterable<Integer> {
	private final List<Integer> list;
	private final ListIterator<Integer> iterator;

	/**
	 * Constructs a production from a list of generated symbols, and
	 * initializes the current parser pointer to the start of that list.
	 *
	 * @param l list of integers representing symbol identifiers
	 */
	public Production(List<Integer> l) {
		this.list = l;
		this.iterator = l.listIterator();
	}

	/**
	 * Constructs a production which is based on a previous production,
	 * except that this new production will have its iterator initialized
	 * where the old one left off.
	 *
	 * @param p production to base this one off of
	 */
	private Production(Production p) {
		this.list = p.list;
		this.iterator = p.list.listIterator(p.iterator.nextIndex());
	}

	@Override
	public Iterator<Integer> iterator() {
		return new Production(this);
	}

	@Override
	public boolean hasNext() {
		return iterator.hasNext();
	}

	@Override
	public Integer next() {
		return iterator.next();
	}

	@Override
	public int nextIndex() {
		return iterator.nextIndex();
	}

	@Override
	public void add(Integer e) {
		list.add(e);
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public boolean hasPrevious() {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public Integer previous() {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public int previousIndex() {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void set(Integer e) {
		throw new UnsupportedOperationException("Not supported.");
	}
}
