package edu.csupomona.cs.cs411.project2.parser.lalr;

import com.google.common.collect.ImmutableSet;
import edu.csupomona.cs.cs411.project2.parser.slr.SLRParserGenerator;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

public final class LALRParserGenerator extends SLRParserGenerator {
	private final ImmutableSet<Integer>[] FIRST;

	public LALRParserGenerator(Path p) throws IOException {
		super(p);

		FIRST = computeFirstSets();
	}

	private ImmutableSet<Integer>[] computeFirstSets() {
		Set<Integer>[] first = (Set<Integer>[])new Object[super.getNumNonterminals()+super.getNumTerminals()];



		ImmutableSet<Integer>[] immutableFirst = (ImmutableSet<Integer>[])new Object[first.length];
		for (int i = 0; i < first.length; i++) {
			immutableFirst[i] = ImmutableSet.copyOf(first[i]);
		}

		return immutableFirst;
	}
}
