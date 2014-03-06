package edu.csupomona.cs.cs411.project2.parser;

import edu.csupomona.cs.cs411.project1.lexer.Token;
import edu.csupomona.cs.cs411.project1.lexer.TokenStream;
import java.io.IOException;
import java.io.Writer;
import java.util.Deque;
import java.util.LinkedList;
import java.util.NoSuchElementException;

public class SLRParser implements Parser {
	private final SLRTable TABLES;

	public SLRParser(SLRTable table) {
		this.TABLES = table;
	}

	@Override
	public boolean parse(TokenStream stream, Writer writer) throws IOException {
		int state = 0;
		Deque<Integer> stack = new LinkedList<>();
		stack.push(0);

		int shift;
		int popNum;
		int production;

		Token t;
		int symbol;
		boolean accepted = false;
		Get_Next_Token: while (true) {
			t = stream.getNext();
			symbol = TABLES.getTokenAsSymbol(t);
			if (symbol != Integer.MIN_VALUE) {
				writer.write(String.format("%-3d %-16s", symbol, t));
			}

			Shift_Handler: while (true) {
				shift = TABLES.shift(state, symbol);
				if (shift != Integer.MIN_VALUE) {
					state = shift;
					stack.push(state);
					//System.out.println(stack + " push " + state);
					writer.write(String.format("[shift]%n"));
					continue Get_Next_Token;
				}

				production = TABLES.reduce(state);
				if (production == Integer.MIN_VALUE) {
					accepted = false;
					System.out.format("P in A%d is undefined%n", state);
					break Get_Next_Token;
				} else if (production == 0) {
					if (!stream.hasMore()) {
						accepted = true;
						break Get_Next_Token;
					}
				}

				popNum = TABLES.right(production);
				for (int i = 0; i < popNum; i++) {
					stack.pop();
				}

				//System.out.println(stack + " popped " + popNum);

				try {
					state = TABLES.move(stack.getFirst(), TABLES.left(production));
					stack.push(state);
					//System.out.println(stack + " push2 " + state);
					writer.write(String.format("[reduce %d]", production));
				} catch (NoSuchElementException e) {
					accepted = false;
					//System.out.format("stack is empty %d%n", production);
					break Get_Next_Token;
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
