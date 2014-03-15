package edu.csupomona.cs.cs411.project2.parser;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import edu.csupomona.cs.cs411.project1.lexer.ToyKeywords;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public abstract class AbstractParserGenerator {
	protected static final int EXTRA_TERMINALS_OFFSET = 1000;
	protected static final Charset CHARSET = Charset.forName("US-ASCII");
	protected static final String NONTERMINAL_DEF_REGEX = "[A-Z][a-zA-Z0-9]*:";

	protected final ImmutableList<Production> PRODUCTIONS;
	protected final ImmutableBiMap<String, Integer> SYMBOLS;
	protected final ImmutableBiMap<Integer, ImmutableSet<Production>> NONTERMINALS;

	private int numTerminals;
	private int numNonterminals;
	private int numUnreachableSymbols;

	private int initialNonterminal;

	public AbstractParserGenerator(Path p) throws IOException {
		Preconditions.checkNotNull(p);
		numTerminals = Integer.MIN_VALUE;
		numNonterminals = Integer.MIN_VALUE;
		numUnreachableSymbols = Integer.MIN_VALUE;

		initialNonterminal = Integer.MIN_VALUE;

		System.out.format("Creating symbols table...%n");
		long dt = System.currentTimeMillis();
		this.SYMBOLS = createSymbolsTable(p);
		System.out.format("Symbols table created in %dms; %d symbols (%d terminals, %d nonterminals)%n",
			System.currentTimeMillis()-dt,
			numTerminals+numNonterminals,
			numTerminals,
			numNonterminals
		);

		System.out.format("Creating productions table and list...%n");
		dt = System.currentTimeMillis();
		ArrayList<Production> productions = new ArrayList<>();
		this.NONTERMINALS = createProductionsTable(p, productions);
		productions.trimToSize();
		this.PRODUCTIONS = ImmutableList.copyOf(productions);
		System.out.format("Productions table and list created in %dms; %d productions (%d unreachable symbols)%n",
			System.currentTimeMillis()-dt,
			this.PRODUCTIONS.size(),
			numUnreachableSymbols
		);
	}

	private ImmutableBiMap<String, Integer> createSymbolsTable(Path p) throws IOException {
		numTerminals = numNonterminals = 0;
		BiMap<String, Integer> symbols = HashBiMap.create();
		for (ToyKeywords k : ToyKeywords.values()) {
			symbols.put(k.name(), k.getId());
			if (!k.isRegex()) {
				symbols.put(k.getRegex(), k.getId()+EXTRA_TERMINALS_OFFSET);
			}

			numTerminals++;
		}

		try (BufferedReader br = Files.newBufferedReader(p, CHARSET)) {
			int id;
			String line;
			while ((line = br.readLine()) != null) {
				if (!line.matches(NONTERMINAL_DEF_REGEX)) {
					continue;
				}

				id = numTerminals+numNonterminals;
				if (initialNonterminal == Integer.MIN_VALUE) {
					initialNonterminal = id;
				}

				line = line.substring(0, line.length()-1);
				symbols.put(line, id);
				numNonterminals++;
			}
		}

		return ImmutableBiMap.copyOf(symbols);
	}

	private ImmutableBiMap<Integer, ImmutableSet<Production>> createProductionsTable(Path p, List<Production> productions) throws IOException {
		BiMap<Integer, Set<Production>> nonterminals = HashBiMap.create();
		Set<Integer> usedSymbols = new HashSet<>();
		try (BufferedReader br = Files.newBufferedReader(p, CHARSET)) {
			Production production;
			Set<Production> nonterminalProductions;
			Integer currentNonterminal = null;

			int id;
			String line;
			String[] tokens;
			while ((line = br.readLine()) != null) {
				if (line.isEmpty()) {
					continue;
				}

				if (line.matches(NONTERMINAL_DEF_REGEX)) {
					line = line.substring(0, line.length()-1);
					currentNonterminal = SYMBOLS.get(line);
					if (currentNonterminal == null) {
						throw new ProductionException("Nonterminal %s is not within the set of symbols!",
							line
						);
					} else if (currentNonterminal < numTerminals || EXTRA_TERMINALS_OFFSET <= currentNonterminal) {
						throw new ProductionException("Terminal %s cannot be declared as a production nonterminal!",
							SYMBOLS.inverse().get(currentNonterminal)
						);
					}

					usedSymbols.add(currentNonterminal);
					continue;
				}

				if (currentNonterminal == null) {
					throw new ProductionException("Production %s does not belong to any declared nonterminal!",
						line
					);
				}

				nonterminalProductions = nonterminals.get(currentNonterminal);
				if (nonterminalProductions == null) {
					nonterminalProductions = new HashSet<>();
					nonterminals.put(currentNonterminal, nonterminalProductions);
				}

				ArrayList<Integer> productionList = new ArrayList<>();

				line = line.trim();
				tokens = line.split("\\s+");
				for (String token : tokens) {
					if (token.isEmpty()) {
						continue;
					}

					int tokenId = resolveSymbol(token);
					usedSymbols.add(tokenId);
					productionList.add(tokenId);
				}

				productionList.trimToSize();
				production = new Production(currentNonterminal, ImmutableList.copyOf(productionList));
				if (!productions.contains(production)) {
					productions.add(production);
				}

				nonterminalProductions.add(production);
			}
		}

		numUnreachableSymbols = 0;
		Set<Integer> unusedSymbols = new HashSet<>(SYMBOLS.values());
		unusedSymbols.removeAll(usedSymbols);
		for (Integer symbol : unusedSymbols) {
			if (EXTRA_TERMINALS_OFFSET <= symbol) {
				continue;
			}

			numUnreachableSymbols++;
			System.out.format("%s (%d) is unreachable%n", SYMBOLS.inverse().get(symbol), symbol);
		}


		BiMap<Integer, ImmutableSet<Production>> immutableNonterminals = HashBiMap.create();
		for (Entry<Integer, Set<Production>> entry : nonterminals.entrySet()) {
			immutableNonterminals.put(entry.getKey(), ImmutableSet.copyOf(entry.getValue()));
		}

		return ImmutableBiMap.copyOf(immutableNonterminals);
	}

	protected final int resolveSymbol(String token) {
		Preconditions.checkNotNull(token);
		Integer tokenId = SYMBOLS.get(token);
		if (tokenId == null) {
			throw new ProductionException("Symbol %s is undefined!",
				token
			);
		}

		if (EXTRA_TERMINALS_OFFSET <= tokenId) {
			tokenId -= EXTRA_TERMINALS_OFFSET;
		}

		return tokenId;
	}

	protected final boolean isNonterminal(int tokenId) {
		return numTerminals <= tokenId && tokenId < (numTerminals+numNonterminals);
	}

	public final int getNumNonterminals() {
		return numNonterminals;
	}

	public final int getNumTerminals() {
		return numTerminals;
	}

	public final int getNumUnreachableSymbols() {
		return numUnreachableSymbols;
	}

	public final int getInitialNonterminal() {
		return initialNonterminal;
	}

	public final void setInitialNonterminal(int nonterminal) {
		Preconditions.checkArgument(isNonterminal(nonterminal), "Invalid nonterminal ID given.");
		initialNonterminal = nonterminal;
	}

	public void outputCFG() {
		outputSymbols();
		outputProductions();
	}

	protected void outputSymbols() {
		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(".", "output", "toy.cfg.symbols.txt"), CHARSET, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			BiMap.Entry<String, Integer>[] entries = SYMBOLS.entrySet().toArray(new BiMap.Entry[0]);
			Arrays.sort(entries, new Comparator<BiMap.Entry<String, Integer>>() {
				@Override
				public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
					return o1.getValue().compareTo(o2.getValue());
				}
			});

			writer.write(String.format("%3s %-32s %s%n",
				"ID",
				"Token",
				"Alternative (Optional)"
			));

			for (BiMap.Entry<String, Integer> entry : entries) {
				if (EXTRA_TERMINALS_OFFSET <= entry.getValue()) {
					continue;
				} else if (entry.getValue() == numTerminals) {
					writer.write(String.format("%n----------------------------------------------------------------%n"));
					writer.write(String.format("%3s %s%n",
						"ID",
						"Nonterminal"
					));
				}

				String alternateValue = SYMBOLS.inverse().get(entry.getValue()+EXTRA_TERMINALS_OFFSET);
				writer.write(String.format("%3d %s%n",
					entry.getValue(),
					entry.getKey(),
					Strings.nullToEmpty(alternateValue)
				));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void outputProductions() {
		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(".", "output", "toy.cfg.productions.txt"), CHARSET, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			BiMap.Entry<Integer, Set<Production>>[] entries = NONTERMINALS.entrySet().toArray(new BiMap.Entry[0]);
			Arrays.sort(entries, new Comparator<BiMap.Entry<Integer, Set<Production>>>() {
				@Override
				public int compare(Map.Entry<Integer, Set<Production>> o1, Map.Entry<Integer, Set<Production>> o2) {
					return o1.getKey().compareTo(o2.getKey());
				}
			});

			writer.write(String.format("%-4s %-16s%n",
				"ID",
				"Nonterminal"
			));

			for (BiMap.Entry<Integer, Set<Production>> entry : entries) {
				writer.write(String.format("[%d] %s:%n",
					entry.getKey(),
					SYMBOLS.inverse().get(entry.getKey())
				));

				Production[] productions = entry.getValue().toArray(new Production[0]);
				Arrays.sort(productions, new Comparator<Production>() {
					@Override
					public int compare(Production o1, Production o2) {
						return PRODUCTIONS.indexOf(o1) - PRODUCTIONS.indexOf(o2);
					}
				});

				for (Production p : productions) {
					writer.write(String.format("\t%3d\t", PRODUCTIONS.indexOf(p)));
					for (Integer i : p) {
						writer.write(String.format("%s[%d] ", SYMBOLS.inverse().get(i), i));
					}

					writer.write(String.format("%n"));
				}

				writer.write(String.format("%n"));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
