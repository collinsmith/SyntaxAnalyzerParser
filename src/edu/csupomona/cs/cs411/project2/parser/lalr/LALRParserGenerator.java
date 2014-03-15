package edu.csupomona.cs.cs411.project2.parser.lalr;

import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import edu.csupomona.cs.cs411.project2.parser.AbstractParserGenerator;
import edu.csupomona.cs.cs411.project2.parser.Production;
import edu.csupomona.cs.cs411.project2.parser.slr.SLRParserGenerator;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class LALRParserGenerator extends SLRParserGenerator {
	private final LALRTables LALR_TABLES;
	private final ImmutableSet<Integer>[] FIRST;
	private final ImmutableSet<Integer>[] FOLLOW;

	public LALRParserGenerator(Path p) throws IOException {
		super(p);

		System.out.format("Generating first sets...%n");
		long dt = System.currentTimeMillis();
		this.FIRST = generateFirstSets();
		System.out.format("First sets generated in %dms%n",
			System.currentTimeMillis()-dt
		);

		System.out.format("Generating follow sets...%n");
		dt = System.currentTimeMillis();
		this.FOLLOW = generateFollowSets();
		System.out.format("Follow sets generated in %dms%n",
			System.currentTimeMillis()-dt
		);

		System.out.format("Generating LALR tables...%n");
		dt = System.currentTimeMillis();
		LALR_TABLES = generateLALRTables();
		System.out.format("LALR tables generated in %dms%n",
			System.currentTimeMillis()-dt
		);
	}

	private ImmutableSet<Integer>[] generateFirstSets() {
		Set<Integer>[] first = (Set<Integer>[])Array.newInstance(Set.class, super.getNumTerminals()+super.getNumNonterminals());
		for (int i = 0; i < super.getNumTerminals(); i++) {
			first[i] = ImmutableSet.of(i);
		}

		for (Production p : super.PRODUCTIONS) {
			int nonterminal = p.getNonterminal();
			if (first[nonterminal] == null) {
				first[nonterminal] = new HashSet<>();
			}

			first[nonterminal].add(p.peek());
		}

		ImmutableSet<Integer>[] immutableFirst = (ImmutableSet<Integer>[])Array.newInstance(ImmutableSet.class, first.length);
		for (int i = 0; i < first.length; i++) {
			immutableFirst[i] = ImmutableSet.copyOf(first[i]);
		}

		return immutableFirst;
	}

	private ImmutableSet<Integer>[] generateFollowSets() {
		Set<Integer>[] follow = (Set<Integer>[])Array.newInstance(Set.class, super.getNumTerminals()+super.getNumNonterminals());
		for (int i = 0; i < follow.length; i++) {
			follow[i] = new HashSet<>();
		}

		follow[getInitialNonterminal()].add(super.resolveSymbol("$"));

		int symbol;
		int nonterminal;
		boolean changes;
		do {
			changes = false;
			for (Production p : super.PRODUCTIONS) {
				nonterminal = p.getNonterminal();
				for (PeekingIterator<Integer> it = Iterators.peekingIterator(p.iterator()); it.hasNext(); ) {
					symbol = it.next();
					if (super.isNonterminal(symbol)) {
						if (it.hasNext()) {
							if (follow[symbol].addAll(FIRST[it.peek()])) {
								changes = true;
							}
						} else {
							if (follow[symbol].addAll(follow[nonterminal])) {
								changes = true;
							}
						}
					}
				}
			}
		} while (changes);

		ImmutableSet<Integer>[] immutableFollow = (ImmutableSet<Integer>[])Array.newInstance(ImmutableSet.class, follow.length);
		for (int i = 0; i < follow.length; i++) {
			immutableFollow[i] = ImmutableSet.copyOf(follow[i]);
		}

		return immutableFollow;
	}

	@Override
	protected void outputSymbols() {
		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(".", "output", "toy.cfg.symbols.txt"), CHARSET, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			BiMap.Entry<String, Integer>[] entries = super.SYMBOLS.entrySet().toArray(new BiMap.Entry[0]);
			Arrays.sort(entries, new Comparator<BiMap.Entry<String, Integer>>() {
				@Override
				public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
					return o1.getValue().compareTo(o2.getValue());
				}
			});

			writer.write(String.format("%3s %-24s %-24s %s%n",
				"ID",
				"Token",
				"Alternative (Optional)",
				"First Sets"
			));

			for (BiMap.Entry<String, Integer> entry : entries) {
				if (AbstractParserGenerator.EXTRA_TERMINALS_OFFSET <= entry.getValue()) {
					continue;
				} else if (entry.getValue() == getNumTerminals()) {
					writer.write(String.format("%n----------------------------------------------------------------%n"));
					writer.write(String.format("%3s %-24s %-24s %-64s %s%n",
						"ID",
						"Nonterminal",
						"",
						"First Sets",
						"Follow Sets"
					));
				}

				String alternateValue = super.SYMBOLS.inverse().get(entry.getValue()+AbstractParserGenerator.EXTRA_TERMINALS_OFFSET);
				writer.write(String.format("%3d %-24s %-24s %-64s %s%n",
					entry.getValue(),
					entry.getKey(),
					Strings.nullToEmpty(alternateValue),
					FIRST[entry.getValue()],
					FOLLOW[entry.getValue()]
				));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private LALRTables generateLALRTables() {
		final int numLongs = (int)Math.ceil((double)super.SYMBOLS.size()/Long.SIZE);

		long[] ref;
		long[][] _follow = new long[super.getNumNonterminals()][];
		for (int i = super.getNumTerminals(); i < FOLLOW.length; i++) {
			ref = new long[numLongs];
			_follow[i-super.getNumTerminals()] = ref;
			for (int symbol : FOLLOW[i]) {
				ref[symbol/Long.SIZE] |= (1<<(symbol%Long.SIZE));
			}
		}

		return LALRTables.build(SLR_TABLES, new LALRTables.Lookahead(_follow));
	}

	public LALRTables getGeneratedLALRTables() {
		return LALR_TABLES;
	}
}
