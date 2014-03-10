package edu.csupomona.cs.cs411.project2.parser;

import edu.csupomona.cs.cs411.project1.lexer.Token;
import edu.csupomona.cs.cs411.project1.lexer.TokenStream;
import java.io.IOException;
import java.io.Writer;

public class SLRParser2 implements Parser {
	private final SLRTables TABLES;

	public SLRParser2(SLRTables tables) {
		this.TABLES = tables;
	}

	@Override
	public boolean parse(TokenStream stream, Writer writer) throws IOException {
		int top = 0;
		int state = 0;
		int[] stack = new int[256];

		stack[top] = state;

		int shift;
		int production;

		Token t;
		int symbol;
		boolean accepted = false;
		Get_Next_Token: while (true) {
			t = stream.getNext();
			symbol = TABLES.getTokenId(t);
			if (symbol != Integer.MIN_VALUE) {
				writer.write(String.format("%-3d %-16s", symbol, t));
			}

			Shift_Handler: while(true) {
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
						break Get_Next_Token;
					case 1:
						if (!stream.hasMore()) {
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
