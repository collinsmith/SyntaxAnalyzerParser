package edu.csupomona.cs.cs411.project2.parser.slr;

import edu.csupomona.cs.cs411.project1.lexer.Token;
import edu.csupomona.cs.cs411.project1.lexer.TokenStream;
import edu.csupomona.cs.cs411.project1.lexer.ToyKeywords;
import edu.csupomona.cs.cs411.project2.parser.Parser;
import java.io.IOException;
import java.io.Writer;

public class SLRParser implements Parser {
	private final SLRTables TABLES;

	public SLRParser(SLRTables tables) {
		this.TABLES = tables;
	}

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
			symbol = TABLES.getTokenId(t);
			if (symbol != Integer.MIN_VALUE) {
				writer.write(String.format("%-3d %-16s", symbol, t));
			}

			Shift_Handler:
			while(true) {
				shift = TABLES.shift(state, symbol);
				if (shift != Integer.MIN_VALUE) {
					writer.write(String.format("[shift]%n"));
					state = shift;
					stack[++top] = state;
					continue Get_Next_Token;
				}

				production = TABLES.reduce(state);
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
						top -= TABLES.getRHSSize(production);
						state = TABLES.move(stack[top], TABLES.getNonterminalId(production));
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
