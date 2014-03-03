package edu.csupomona.cs.cs411.project2.generator;

public class Goto {
	private final int GOTO;
	private final Integer SYMBOL;

	public Goto(int _goto, Integer symbol) {
		this.GOTO = _goto;
		this.SYMBOL = symbol;
	}

	public int getTableId() {
		return GOTO;
	}

	public Integer getSymbol() {
		return SYMBOL;
	}

	@Override
	public String toString() {
		return String.format("goto(A%d, %d)", GOTO, SYMBOL);
	}
}
