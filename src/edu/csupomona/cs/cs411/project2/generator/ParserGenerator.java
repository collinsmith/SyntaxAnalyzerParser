package edu.csupomona.cs.cs411.project2.generator;

import edu.csupomona.cs.cs411.project1.lexer.ToyKeywords;
import edu.csupomona.cs.cs411.project2.parser.SLRTable;
import edu.csupomona.cs.cs411.project2.parser.SLRTable.GotoTable;
import edu.csupomona.cs.cs411.project2.parser.SLRTable.ReduceTable;
import edu.csupomona.cs.cs411.project2.parser.SLRTable.ShiftTable;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ParserGenerator {
	private static final String NONTERMINAL_DELIMINATOR = "[A-Z][a-zA-Z0-9]*:";

	private final Map<String, Integer> SYMBOLS;
	private final Map<Integer, List<Production>> PRODUCTIONS;

	private final List<Production> PRODUCTIONS_LIST;
	private final Map<Integer, String> SYMBOLS_REVERSE;
	private final Map<Production, List<Integer>> PRODUCTIONS_REVERSE;

	private final Map<List<Production>, Table> TABLES;

	private int numProductions;
	private int numNonterminals;
	private Integer initialNonterminal;

	private int shiftReduces;
	private int reduceReduces;
	private int tablesAvoided;
	private int unusedProductions;
	private boolean[] productionsUsed;

	public ParserGenerator(Path p) throws IOException {
		long dt = System.nanoTime();
		System.out.format("Populating symbols table...");
		this.SYMBOLS = createSymbolsTable(p);
		Map<Integer, String> symbolsTableReverse = new HashMap<>();
		for (Entry<String, Integer> entry : SYMBOLS.entrySet()) {
			symbolsTableReverse.put(entry.getValue(), entry.getKey());
		}

		this.SYMBOLS_REVERSE = Collections.unmodifiableMap(symbolsTableReverse);
		System.out.format("%dms\t\t%d nonterminals%n", TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-dt), numNonterminals);

		dt = System.nanoTime();
		System.out.format("Populating productions table...");

		List<Production> productionsList = new ArrayList<>();
		this.PRODUCTIONS = createProductionsTable(p, productionsList);
		for (List<Production> productions : PRODUCTIONS.values()) {
			for (Production production : productions) {
				production.setImmutable();
			}
		}

		PRODUCTIONS_LIST = Collections.unmodifiableList(productionsList);

		List<Integer> nonterminalsWithProduction;
		Map<Production, List<Integer>> productionsTableReverse = new HashMap<>();
		for (Entry<Integer, List<Production>> entry : PRODUCTIONS.entrySet()) {
			for (Production production : entry.getValue()) {
				nonterminalsWithProduction = productionsTableReverse.get(production);
				if (nonterminalsWithProduction == null) {
					nonterminalsWithProduction = new LinkedList<>();
					productionsTableReverse.put(production, nonterminalsWithProduction);
				}

				nonterminalsWithProduction.add(entry.getKey());
			}
		}

		this.PRODUCTIONS_REVERSE = Collections.unmodifiableMap(productionsTableReverse);
		System.out.format("%dms\t%d productions%n", TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-dt), numProductions);

		dt = System.nanoTime();
		System.out.format("Creating SLR tables...%n");
		shiftReduces = Integer.MIN_VALUE;
		reduceReduces = 0;
		tablesAvoided = 0;
		unusedProductions = 0;
		productionsUsed = new boolean[numProductions];

		TABLES = generateTables();
		System.out.format("...Tables generated in %dms %d tables%n", TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-dt), TABLES.size());

		for (int i = 0; i < productionsUsed.length; i++) {
			if (!productionsUsed[i]) {
				unusedProductions++;
				System.out.format("\tUnused production: #%-3d\t%s%n", i, PRODUCTIONS_LIST.get(i));
			}
		}

		System.out.format("%d unused productions%n", unusedProductions);
	}

	private Map<String, Integer> createSymbolsTable(Path p) throws IOException {
		Map<String, Integer> symbolsTable = new LinkedHashMap<>();
		Charset c = Charset.forName("US-ASCII");
		try (BufferedReader reader = Files.newBufferedReader(p, c)) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.matches(NONTERMINAL_DELIMINATOR)) {
					numNonterminals++;
					line = line.substring(0, line.length()-1);
					symbolsTable.put(line, numNonterminals);
					if (initialNonterminal == null) {
						initialNonterminal = numNonterminals;
					}
				}
			}
		}

		for (ToyKeywords k : ToyKeywords.values()) {
			int keywordId = numNonterminals+k.getId()+1;
			symbolsTable.put(k.name(), keywordId);
			if (!k.isRegex()) {
				symbolsTable.put(k.getRegex(), keywordId);
			}
		}

		return Collections.unmodifiableMap(symbolsTable);
	}

	private Map<Integer, List<Production>> createProductionsTable(Path p, List<Production> productionsList) throws IOException {
		Map<Integer, List<Production>> productionsTable = new LinkedHashMap<>();
		Charset c = Charset.forName("US-ASCII");
		try (BufferedReader reader = Files.newBufferedReader(p, c)) {
			Production production;
			List<Production> productions;
			Integer currentProductionSymbol = null;

			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isEmpty()) {
					continue;
				}

				if (line.matches(NONTERMINAL_DELIMINATOR)) {
					line = line.substring(0, line.length()-1);
					currentProductionSymbol = SYMBOLS.get(line);
					if (numNonterminals < currentProductionSymbol) {
						throw new NonterminalSymbolException(line);
					}

					continue;
				}

				productions = productionsTable.get(currentProductionSymbol);
				if (productions == null) {
					productions = new LinkedList<>();
					productionsTable.put(currentProductionSymbol, productions);
				}

				production = new Production(numProductions++, currentProductionSymbol, new ArrayList<Integer>());
				productions.add(production);
				productionsList.add(production);

				line = line.trim();
				String[] tokens = line.split("\\s+");
				for (String token : tokens) {
					if (token.isEmpty()) {
						continue;
					}

					int tokenId = resolveSymbol(token);
					if (tokenId == -1) {
						throw new InvalidSymbolException(token, line);
					}

					production.addSymbol(tokenId);
				}
			}
		}

		for (List<Production> productionsForNonterminal : productionsTable.values()) {
			//Collections.sort(productionsForNonterminal);
			productionsForNonterminal = Collections.unmodifiableList(productionsForNonterminal);
		}

		return Collections.unmodifiableMap(productionsTable);
	}

	private int resolveSymbol(String token) {
		Integer id = SYMBOLS.get(token);
		if (id == null) {
			return -1;
		}

		return id;
	}

	private Map<List<Production>, Table> generateTables() {
		Map<List<Production>, Table> tables = new LinkedHashMap<>();

		Queue<TableMetadata> generations = new LinkedList();
		generations.offer(new TableMetadata(null, PRODUCTIONS.get(initialNonterminal), null));
		while (!generations.isEmpty()) {
			generateTables(tables, generations, generations.poll());
		}

		return Collections.unmodifiableMap(tables);
	}

	private void generateTables(Map<List<Production>, Table> tables, Queue<TableMetadata> generations, TableMetadata tableMetadata) {
		List<Production> initialProductions = tableMetadata.getInitialProductions();
		Table parentTable = tableMetadata.getParentTable();
		Table existingTable = tables.get(initialProductions);
		if (existingTable != null) {
			parentTable.putTransition(tableMetadata.getGoto().getSymbol(), existingTable);
			tablesAvoided++;
			return;
		} else {
			// TODO change this sloppy implementation to use the tables map
			// need to implement hash function where order of list doesn't matter
			Set<Production> initialProductionsForTable;
			for (Table t : tables.values()) {
				initialProductionsForTable = new HashSet<>(t.getInitialProductions());
				initialProductionsForTable.removeAll(initialProductions);
				if (initialProductionsForTable.isEmpty()) {
					parentTable.putTransition(tableMetadata.getGoto().getSymbol(), t);
					tablesAvoided++;
					return;
				}
			}
		}

		List<Production> closureProductions = new LinkedList<>(initialProductions);
		for (Production initialProduction : initialProductions) {
			closeOverProduction(closureProductions, initialProduction);
		}

		closureProductions.removeAll(initialProductions);
		Table t = new Table(
			tables.size(),
			initialProductions,
			closureProductions,
			tableMetadata.getGoto()
		);

		tables.put(initialProductions, t);
		if (parentTable != null) {
			parentTable.putTransition(tableMetadata.getGoto().getSymbol(), t);
		}

		Set<Integer> symbolsToBeParsed = new HashSet<>();
		Set<Integer> productionsWithReduce = new HashSet<>();
		for (Production p : t) {
			productionsUsed[p.getProductionId()] = true;
			if (p.hasMoreSymbols()) {
				Integer nextSymbol = p.getNextSymbol();
				if (symbolsToBeParsed.contains(nextSymbol)) {
					continue;
				}

				symbolsToBeParsed.add(nextSymbol);
				List<Production> initialProductionsForNewTable = new LinkedList<>();
				for (Production sibling : t) {
					if (sibling.getNextSymbol() != nextSymbol) {
						continue;
					}

					initialProductionsForNewTable.add(new Production(sibling));
				}

				TableMetadata newTableMetadata = new TableMetadata(
					t,
					initialProductionsForNewTable,
					new Goto(t.getTableId(), nextSymbol)
				);

				generations.offer(newTableMetadata);
			} else {
				Integer productionId = p.getProductionId();
				if (productionsWithReduce.contains(productionId)) {
					System.out.format("Reduce-reduce conflict in table A%d, production %s%n", t.getTableId(), p);
				}

				productionsWithReduce.add(productionId);
			}
		}

		if (1 < productionsWithReduce.size()) {
			System.out.format("Table A%d has %d reduce productions%n", t.getTableId(), productionsWithReduce.size());
			reduceReduces++;
		}
	}

	private void closeOverProduction(List<Production> closureProductions, Production p) {
		if (!p.hasMoreSymbols()) {
			return;
		}

		Integer nextSymbol = p.getNextSymbol();
		if (nextSymbol <= numNonterminals) {
			for (Production closureProduction : PRODUCTIONS.get(nextSymbol)) {
				if (closureProductions.contains(closureProduction)) {
					continue;
				}

				closureProductions.add(closureProduction);
				closeOverProduction(closureProductions, closureProduction);
			}
		}
	}

	public void outputConvertedCFG() {
		Charset charset = Charset.forName("US-ASCII");
		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(".", "output", "toy.cfg.symbols.txt"), charset, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			for (Entry<String, Integer> entry : SYMBOLS.entrySet()) {
				writer.write(String.format("%3d %s%n", entry.getValue(), entry.getKey()));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(".", "output", "toy.cfg.converted.txt"), charset, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			for (Entry<Integer, List<Production>> entry : PRODUCTIONS.entrySet()) {
				writer.write(String.format("%s:%n", SYMBOLS_REVERSE.get(entry.getKey())));
				for (Production p : entry.getValue()) {
					writer.write(String.format("%3d\t", p.getProductionId()));
					for (Integer i : p) {
						writer.write(String.format("%s ", SYMBOLS_REVERSE.get(i)));
					}

					writer.write(String.format("%n"));
				}

				writer.write(String.format("%n"));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void outputTables() {
		Charset charset = Charset.forName("US-ASCII");
		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(".", "output", "toy.tables.txt"), charset, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			for (Table t : TABLES.values()) {
				writer.write(String.format("%s%n", t));
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
		if (!p.hasMoreSymbols()) {
			writer.write(String.format("%s\t%-24s reduce(%d)%n",
				isInitialProduction ? "I:" : "",
				p,
				p.getProductionId()
			));

			return;
		}

		Integer onSymbol = p.getNextSymbol();
		Table t2 = t.getTransition(onSymbol);
		writer.write(String.format("%s\t%-24s %s",
			isInitialProduction ? "I:" : "",
			p,
			new Goto(t2.getTableId(), onSymbol)
		));

		if (numNonterminals < onSymbol) {
			writer.write(String.format("\tshift"));
		}

		writer.write(String.format("%n"));
	}

	public SLRTable getSLRTable() {
		long dt = System.nanoTime();
		System.out.format("Generating SLR tables...%n");

		int[] shiftSwitch = new int[TABLES.size()];
		ArrayList<Integer> shiftShiftSymbol = new ArrayList<>();
		ArrayList<Integer> shiftShiftNext = new ArrayList<>();
		shiftShiftSymbol.add(Integer.MIN_VALUE);
		shiftShiftNext.add(Integer.MIN_VALUE);

		int reducePointer = 0;
		int[] reduceReduce = new int[TABLES.size()];

		int[] gotoSwitch = new int[TABLES.size()];
		ArrayList<Integer> gotoGotoSymbol = new ArrayList<>();
		ArrayList<Integer> gotoGotoNext = new ArrayList<>();
		gotoGotoSymbol.add(Integer.MIN_VALUE);
		gotoGotoNext.add(Integer.MIN_VALUE);

		int tableId;
		shiftReduces = 0;
		for (Table t : TABLES.values()) {
			tableId = t.getTableId();

			shiftSwitch[tableId] = Integer.MIN_VALUE;

			reducePointer = tableId;
			reduceReduce[reducePointer] = Integer.MIN_VALUE;

			gotoSwitch[tableId] = Integer.MIN_VALUE;
			for (Production p : t) {
				if (!p.hasMoreSymbols()) {
					if (reduceReduce[reducePointer] != Integer.MIN_VALUE) {
						System.out.format("\tReduce-reduce conflict has occured in table A%d%n", reducePointer);
						continue;
					}

					reduceReduce[reducePointer] = p.getProductionId();
					continue;
				}

				Integer onSymbol = p.getNextSymbol();
				Table t2 = t.getTransition(onSymbol);
				if (numNonterminals < onSymbol) {
					if (shiftSwitch[tableId] == Integer.MIN_VALUE) {
						shiftSwitch[tableId] = shiftShiftSymbol.size();
					}

					shiftShiftSymbol.add(onSymbol);
					shiftShiftNext.add(t2.getTableId());
				} else {
					if (gotoSwitch[tableId] == Integer.MIN_VALUE) {
						gotoSwitch[tableId] = gotoGotoSymbol.size();
					}

					gotoGotoSymbol.add(onSymbol);
					gotoGotoNext.add(t2.getTableId());
				}
			}


			if (reduceReduce[reducePointer] != Integer.MIN_VALUE && shiftSwitch[tableId] != Integer.MIN_VALUE) {
				System.out.format("\tShift-reduce conflict in table A%d%n", tableId);
				shiftReduces++;
			}

			if (shiftSwitch[tableId] != Integer.MIN_VALUE) {
				shiftShiftSymbol.add(Integer.MIN_VALUE);
				shiftShiftNext.add(Integer.MIN_VALUE);
			}

			if (gotoSwitch[tableId] != Integer.MIN_VALUE) {
				gotoGotoSymbol.add(Integer.MIN_VALUE);
				gotoGotoNext.add(Integer.MIN_VALUE);
			}
		}

		int[][] shiftShift = new int[shiftShiftSymbol.size()][2];
		for (int i = 0; i < shiftShift.length; i++) {
			shiftShift[i][SLRTable.SYMBOL] = shiftShiftSymbol.get(i);
			shiftShift[i][SLRTable.NEXT] = shiftShiftNext.get(i);
		}

		int[][] gotoGoto = new int[gotoGotoSymbol.size()][2];
		for (int i = 0; i < gotoGoto.length; i++) {
			gotoGoto[i][SLRTable.SYMBOL] = gotoGotoSymbol.get(i);
			gotoGoto[i][SLRTable.NEXT] = gotoGotoNext.get(i);
		}

		ShiftTable _shift = new SLRTable.ShiftTable(shiftSwitch, shiftShift);
		ReduceTable _reduce = new SLRTable.ReduceTable(reduceReduce);
		GotoTable _goto = new SLRTable.GotoTable(gotoSwitch, gotoGoto);

		Production p;
		int[] _left = new int[numProductions];
		int[] _right = new int[numProductions];
		for (int i = 0; i < numProductions; i++) {
			p = PRODUCTIONS_LIST.get(i);
			_left[i] = p.getNonterminal();
			_right[i] = p.getSymbolsNum();
		}
		
		System.out.format("SLR tables generated in %dms%n", TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-dt));
		System.out.format("%d shift-reduce conflicts%n", shiftReduces);
		return new SLRTable(numNonterminals, _shift, _reduce, _goto, _left, _right, shiftReduces, reduceReduces, unusedProductions);
	}
}
