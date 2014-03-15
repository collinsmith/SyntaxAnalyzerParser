package edu.csupomona.cs.cs411.project2.parser.lalr;

import edu.csupomona.cs.cs411.project1.lexer.Token;
import edu.csupomona.cs.cs411.project1.lexer.TokenStream;
import edu.csupomona.cs.cs411.project1.lexer.ToyKeywords;
import edu.csupomona.cs.cs411.project2.parser.Parser;
import edu.csupomona.cs.cs411.project2.parser.slr.SLRTables;
import java.io.IOException;
import java.io.Writer;

public class LALRParser implements Parser {
	private final SLRTables SLR_TABLES;
	private final LALRTables LALR_TABLES;

	public LALRParser(LALRTables lalrTables) {
		this.LALR_TABLES = lalrTables;
		this.SLR_TABLES = this.LALR_TABLES.getSLRTables();
	}

	@Override
	public boolean parse(TokenStream stream, Writer writer) throws IOException {
		int top = 0;
		int state = 0;

		// TODO convert to use a dynamic int-based array stack that can grow
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
