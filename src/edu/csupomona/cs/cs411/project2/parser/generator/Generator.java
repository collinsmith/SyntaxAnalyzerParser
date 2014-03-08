package edu.csupomona.cs.cs411.project2.parser.generator;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public final class Generator {
	private static final int EXTRA_TERMINALS_OFFSET = 1000;
	private static final Charset CHARSET = Charset.forName("US-ASCII");
	private static final String NONTERMINAL_DEF_REGEX = "[A-Z][a-zA-Z0-9]*:";

	private final List<Production> PRODUCTIONS;
	private final BiMap<String, Integer> SYMBOLS;
	private final Map<Set<Production>, Table> TABLES;
	private final BiMap<Integer, Set<Production>> NONTERMINALS;

	private int numTerminals;
	private int numNonterminals;
	private int numUnreachableSymbols;

	private int numWithReduceReduce;
	private int numUnrepeatedTables;

	private int initialNonterminal;

	public Generator(Path p) throws IOException {
		numTerminals = Integer.MIN_VALUE;
		numNonterminals = Integer.MIN_VALUE;
		numUnreachableSymbols = Integer.MIN_VALUE;

		numWithReduceReduce = Integer.MIN_VALUE;
		numUnrepeatedTables = Integer.MIN_VALUE;

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
		List<Production> productions = new ArrayList<>();
		this.NONTERMINALS = createProductionsTable(p, productions);
		this.PRODUCTIONS = Collections.unmodifiableList(productions);
		System.out.format("Productions table and list created in %dms; %d productions (%d unreachable symbols)%n",
			System.currentTimeMillis()-dt,
			this.PRODUCTIONS.size(),
			numUnreachableSymbols
		);

		System.out.format("Generating tables...%n");
		dt = System.currentTimeMillis();
		this.TABLES = generateParserTables();
		System.out.format("Tables generated in %dms; %d tables%n",
			System.currentTimeMillis()-dt,
			this.TABLES.size()
		);
	}

	private BiMap<String, Integer> createSymbolsTable(Path p) throws IOException {
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

	private BiMap<Integer, Set<Production>> createProductionsTable(Path p, List<Production> productions) throws IOException {
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
					nonterminalProductions = new HashSet<>(8);
					nonterminals.put(currentNonterminal, nonterminalProductions);
				}

				production = new Production(currentNonterminal, new ArrayList<Integer>());
				if (!productions.contains(production)) {
					productions.add(production);
				}

				nonterminalProductions.add(production);

				line = line.trim();
				tokens = line.split("\\s+");
				for (String token : tokens) {
					if (token.isEmpty()) {
						continue;
					}

					int tokenId = resolveSymbol(token);
					usedSymbols.add(tokenId);
					production.add(tokenId);
				}
			}
		}

		for (Production production : productions) {
			production.setImmutable();
		}

		for (Set<Production> productionSet : nonterminals.values()) {
			productionSet = Collections.unmodifiableSet(productionSet);
		}

		numUnreachableSymbols = 0;
		Set<Integer> unusedSymbols = new HashSet<>(SYMBOLS.values());
		unusedSymbols.removeAll(usedSymbols);
		for (Integer symbol : unusedSymbols) {
			if (EXTRA_TERMINALS_OFFSET <= symbol) {
				continue;
			}

			System.out.format("%s (%d) is unreachable%n", SYMBOLS.inverse().get(symbol), symbol);
		}

		return ImmutableBiMap.copyOf(nonterminals);
	}

	private int resolveSymbol(String token) {
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

	private boolean isNonterminal(int tokenId) {
		return numTerminals <= tokenId && tokenId < (numTerminals+numNonterminals);
	}

	public void outputCFG() {
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
				writer.write(String.format("%3d %-32s %s%n",
					entry.getValue(),
					entry.getKey(),
					alternateValue != null ? alternateValue : ""
				));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

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

	private Map<Set<Production>, Table> generateParserTables() {
		Map<Set<Production>, Table> tables = new LinkedHashMap<>();

		numWithReduceReduce = 0;
		numUnrepeatedTables = 0;

		Queue<Table.Metadata> queue = new LinkedList<>();
		do {
			generateTable(tables, queue);
		} while (!queue.isEmpty());


		return tables;
	}

	private void generateTable(Map<Set<Production>, Table> tables, Queue<Table.Metadata> queue) {
		Table parent;
		Integer symbol;
		Set<Production> productions;
		Table.Metadata metadata = queue.poll();
		if (metadata == null) {
			parent = null;
			symbol = null;
			productions = NONTERMINALS.get(initialNonterminal);
		} else {
			parent = metadata.getParent();
			symbol = metadata.getNextSymbol();
			productions = metadata.getNextInitialProductions();

			Table existingTable = tables.get(productions);
			if (existingTable != null) {
				parent.putTransition(symbol, existingTable);
				numUnrepeatedTables++;
				return;
			}
		}

		Set<Production> closures = new HashSet<>();
		for (Production initialProduction : productions) {
			closeOverProduction(productions, closures, initialProduction);
		}

		List<Integer> viablePrefix;
		if (parent != null) {
			viablePrefix = new LinkedList<>(parent.getViablePrefix());
			viablePrefix.add(symbol);
		} else {
			viablePrefix = new LinkedList<>();
		}

		int tableId = tables.size();
		Table t = new Table(
			tableId,
			parent,
			viablePrefix,
			productions,
			closures
		);

		tables.put(productions, t);
		if (parent != null) {
			parent.putTransition(symbol, t);
		}

		Queue<Production> remainingProductions = new LinkedList<>(productions);
		remainingProductions.addAll(closures);

		Production p;
		Set<Production> reduces = new HashSet<>();
		while (!remainingProductions.isEmpty()) {
			p = remainingProductions.poll();
			if (p.hasNext()) {
				Integer nextSymbol = p.peek();
				Set<Production> productionsWithSameNextSymbol = new HashSet<>();
				for (Production sibling : remainingProductions) {
					if (sibling.hasNext() && sibling.peek() == nextSymbol) {
						productionsWithSameNextSymbol.add(sibling);
					}
				}

				remainingProductions.removeAll(productionsWithSameNextSymbol);
				productionsWithSameNextSymbol.add(p);

				Set<Production> nextInitialProductions = new HashSet<>();
				for (Production productionWithSameNextSymbol : productionsWithSameNextSymbol) {
					nextInitialProductions.add(productionWithSameNextSymbol.getNext());
				}

				Table.Metadata nextTable = t.getMetadata(nextInitialProductions, nextSymbol);
				queue.offer(nextTable);
			} else {
				reduces.add(p);
			}
		}

		if (1 < reduces.size()) {
			numWithReduceReduce++;
			System.out.format("Table A%d has %d reduce productions%n", tableId, reduces.size());
			for (Production reduce : reduces) {
				System.out.format("\t%s%n", reduce);
			}
		}
	}

	private void closeOverProduction(Set<Production> initials, Set<Production> closures, Production p) {
		if (!p.hasNext()) {
			return;
		}

		Integer nextSymbol = p.peek();
		if (isNonterminal(nextSymbol)) {
			for (Production closure : NONTERMINALS.get(nextSymbol)) {
				if (initials.contains(closure) || closures.contains(closure)) {
					continue;
				}

				closures.add(closure);
				closeOverProduction(initials, closures, closure);
			}
		}
	}

	public void outputTables() {
		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(".", "output", "toy.tables.txt"), CHARSET, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			for (Table t : TABLES.values()) {
				writer.write(String.format("A%d: V%s", t.getId(), t.getViablePrefix()));
				if (t.getParent() != null) {
					writer.write(String.format(" = goto(A%d, %d)",
						t.getParent().getId(),
						t.getViablePrefix().get(t.getViablePrefix().size()-1)
					));
				}

				writer.write(String.format("%n"));

				for (Production p : t.getInitialProductions()) {
					writeProduction(writer, t, p, true);
				}

				for (Production p : t.getClosureProductions()) {
					writeProduction(writer, t, p, false);
				}

				writer.write(String.format("%n"));
				writer.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeProduction(BufferedWriter writer, Table t, Production p, boolean isInitialProduction) throws IOException {
		if (!p.hasNext()) {
			writer.write(String.format("%s\t%-24s reduce(%d)%n",
				isInitialProduction ? "I:" : "",
				p,
				PRODUCTIONS.indexOf(p.getAncestor())
			));

			return;
		}

		Integer onSymbol = p.peek();
		writer.write(String.format("%s\t%-24s goto(A%d, %s)",
			isInitialProduction ? "I:" : "",
			p,
			t.getTransition(onSymbol).getId(),
			onSymbol
		));

		if (!isNonterminal(onSymbol)) {
			writer.write(String.format("\tshift"));
		}

		writer.write(String.format("%n"));
	}
}
