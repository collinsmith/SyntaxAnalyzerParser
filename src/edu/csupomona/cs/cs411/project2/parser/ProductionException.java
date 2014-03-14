package edu.csupomona.cs.cs411.project2.parser;

public class ProductionException extends RuntimeException {
	public ProductionException() {
		//...
	}

	public ProductionException(String msg) {
		super(msg);
	}

	public ProductionException(String format, Object... args) {
		super(String.format(format, args));
	}
}
