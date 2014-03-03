package edu.csupomona.cs.cs411.project2.generator;

import java.util.List;

public class TableMetadata {
	//private final List<Production> PREVIOUS_PRODUCTIONS;
	private final List<Production> INITIAL_PRODUCTIONS;
	private final Goto GOTO;
	private final Table PARENT_TABLE;

	public TableMetadata(/*List<Production> previousProductions, */Table parentTable, List<Production> initialProductions, Goto _goto) {
		//this.PREVIOUS_PRODUCTIONS = previousProductions;
		this.PARENT_TABLE = parentTable;
		this.INITIAL_PRODUCTIONS = initialProductions;
		this.GOTO = _goto;
	}

	//public List<Production> getPreviousProductions() {
	//	return PREVIOUS_PRODUCTIONS;
	//}

	public Table getParentTable() {
		return PARENT_TABLE;
	}

	public List<Production> getInitialProductions() {
		return INITIAL_PRODUCTIONS;
	}

	public Goto getGoto() {
		return GOTO;
	}
}
