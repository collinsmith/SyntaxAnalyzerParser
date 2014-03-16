package edu.csupomona.cs.cs411.project2.parser;

import edu.csupomona.cs.cs411.project1.lexer.TokenStream;
import java.io.IOException;
import java.io.Writer;

/**
 * This interface represents the methods necessary in order to construct a
 * Parser.
 *
 * @author Collin Smith <collinsmith@csupomona.edu>
 */
public interface Parser {
	/**
	 * Parses the given TokenStream and writes output into the passed Writer.
	 *
	 * @param stream TokenStream to retrieve Tokens from
	 * @param writer Writer to write output in
	 * @return {code true} if this TokenStream is accepted by the language,
	 *	otherwise {@code false}
	 *
	 * @throws IOException if there is an error writing to that file
	 */
	boolean parse(TokenStream stream, Writer writer) throws IOException;
}
