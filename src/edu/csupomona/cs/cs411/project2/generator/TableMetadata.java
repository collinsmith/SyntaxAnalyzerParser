package edu.csupomona.cs.cs411.project2.generator;

import java.util.List;

public class TableMetadata {
	private final Goto GOTO;
	private final Table PARENT_TABLE;
	private final List<Production> INITIAL_PRODUCTIONS;

	public TableMetadata(Table parentTable, List<Production> initialProductions, Goto _goto) {
		this.GOTO = _goto;
		this.PARENT_TABLE = parentTable;
		this.INITIAL_PRODUCTIONS = initialProductions;
	}

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
