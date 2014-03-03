package edu.csupomona.cs.cs411.project2.generator;

public class InvalidSymbolException extends RuntimeException {
	public InvalidSymbolException(String symbol, String line) {
		super(String.format("Invalid symbol \"%s\" on line \"%s\"", symbol, line));
	}
}
