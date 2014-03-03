package edu.csupomona.cs.cs411.project2.generator;

public class NonterminalSymbolException extends RuntimeException {
	public NonterminalSymbolException(String symbol) {
		super(String.format("Expected nonterminal symbol, found \"%s\"", symbol));
	}
}
