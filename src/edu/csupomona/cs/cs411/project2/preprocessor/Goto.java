package edu.csupomona.cs.cs411.project2.preprocessor;

public class Goto {
	private int _goto;
	private Integer symbol;

	public Goto(int _goto, Integer symbol) {
		this._goto = _goto;
		this.symbol = symbol;
	}

	@Override
	public String toString() {
		return String.format("goto(A%d, %d)", _goto, symbol);
	}
}
