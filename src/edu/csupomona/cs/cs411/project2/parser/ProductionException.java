package edu.csupomona.cs.cs411.project2.parser;

/**
 * This class represents the RuntimeException that will be thrown when there
 * is an error found in the CFG of a {@link Parser}. Some basic variations of
 * this include throwing this exception when a symbol is used that is not a
 * keyword or nonterminal which was not defined.
 *
 * @author Collin Smith <collinsmith@csupomona.edu>
 */
public class ProductionException extends RuntimeException {
	/**
	 * Constructs an empty ProductionException
	 */
	public ProductionException() {
		//...
	}

	/**
	 * Constructs a ProductionException with the specified message.
	 *
	 * @param msg message to throw
	 */
	public ProductionException(String msg) {
		super(msg);
	}

	/**
	 * Constructs a ProductionException with the specified message format.
	 *
	 * @param format format of the message
	 * @param args fill in for the message
	 */
	public ProductionException(String format, Object... args) {
		super(String.format(format, args));
	}
}
