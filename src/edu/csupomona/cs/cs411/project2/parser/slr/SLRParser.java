package edu.csupomona.cs.cs411.project2.parser.slr;

import edu.csupomona.cs.cs411.project1.lexer.Token;
import edu.csupomona.cs.cs411.project1.lexer.TokenStream;
import edu.csupomona.cs.cs411.project1.lexer.ToyKeywords;
import edu.csupomona.cs.cs411.project2.parser.Parser;
import java.io.IOException;
import java.io.Writer;

/**
 * This class represents an SLR parser which can parse any LR(0) grammar.
 *
 * @author Collin Smith <collinsmith@csupomona.edu>
 */
public class SLRParser implements Parser {
	/**
	 * This field represents the tables that this parser uses.
	 */
	private final SLRTables SLR_TABLES;

	/**
	 * Constructs an SLR Parser which will use the given tables to shift,
	 * reduce and goto.
	 *
	 * @param tables tables containing the action information of this Parser.
	 */
	public SLRParser(SLRTables tables) {
		this.SLR_TABLES = tables;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean parse(TokenStream stream, Writer writer) throws IOException {
		int top = 0;
		int state = 0;
		int[] stack = new int[256];

		stack[top] = state;

		int shift = Integer.MIN_VALUE;
		int production = Integer.MIN_VALUE;

		Token t;
		int symbol;
		boolean accepted = false;
		Get_Next_Token:
		while (true) {
			t = stream.next();
			symbol = SLR_TABLES.getTokenId(t);
			if (symbol != Integer.MIN_VALUE) {
				writer.write(String.format("%-3d %-16s", symbol, t));
			}

			Shift_Handler:
			while(true) {
				shift = SLR_TABLES.shift(state, symbol);
				if (shift != Integer.MIN_VALUE) {
					writer.write(String.format("[shift]%n"));
					state = shift;
					stack[++top] = state;
					continue Get_Next_Token;
				}

				production = SLR_TABLES.reduce(state);
				switch (production) {
					case Integer.MIN_VALUE:
						accepted = false;
						System.out.format("\tReduction undefined in table A%d%n", state);
						break Get_Next_Token;
					case 0:
						// When current reduction is back to A0 because EOF is found, we're done
						if (t == ToyKeywords._EOF) {
							accepted = true;
							break Get_Next_Token;
						}
					default:
						writer.write(String.format("[reduce %d]", production));
						top -= SLR_TABLES.getRHSSize(production);
						state = SLR_TABLES.move(stack[top], SLR_TABLES.getNonterminalId(production));
						stack[++top] = state;
				}
			}
		}

		if (accepted) {
			writer.write(String.format("%n[accept]"));
		} else {
			writer.write(String.format("%n[reject]"));
		}

		return accepted;
	}
}
