package edu.csupomona.cs.cs411.project2.parser;

import edu.csupomona.cs.cs411.project1.lexer.TokenStream;
import java.io.IOException;
import java.io.Writer;

public interface Parser {
	boolean parse(TokenStream stream, Writer writer) throws IOException;
}
