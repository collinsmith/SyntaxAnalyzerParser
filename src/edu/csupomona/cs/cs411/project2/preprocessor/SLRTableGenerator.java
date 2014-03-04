package edu.csupomona.cs.cs411.project2.preprocessor;

import edu.csupomona.cs.cs411.project1.lexer.Keywords;
import edu.csupomona.cs.cs411.project2.parser.SLRTable;
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

public class SLRTableGenerator {
	private static final String NONTERMINAL_DELIMINATOR = "[A-Z][a-zA-Z0-9]*:";

	private int numNonterminals = 0;
	private Map<String, Integer> symbolTable;
	private Map<Integer, List<Production>> productionTable;

	private Integer initialNonterminal = null;
	private List<Table> slrTables;

	private Queue<List<Production>> workLoad;

	private ArrayList<Integer> shiftListSwitch;
	private ArrayList<Integer[]> shiftList;

	private ArrayList<Integer> reduceList;

	private ArrayList<Integer> gotoListSwitch;
	private ArrayList<Integer[]> gotoList;

	public SLRTableGenerator(Path p) throws IOException {
		productionTable = new HashMap<>();
		symbolTable = new LinkedHashMap<>();

		generateNonterminals(p);
		for (Keywords k : Keywords.values()) {
			symbolTable.put(k.name(), numNonterminals+k.ordinal()+1);
			if (Keywords.ACTUAL_KEYWORDS.contains(k) || Keywords.OPERATORS.contains(k)) {
				symbolTable.put(k.getRegex(), numNonterminals+k.ordinal()+1);
			}
		}

		// TODO I know I'm not stupid enough to do it, but productions should be immutable
		generateProductions(p);
		for (Entry<Integer, List<Production>> productionList : productionTable.entrySet()) {
			//for (List<Integer> production : productionList.getValue()) {
			//	production = Collections.unmodifiableList(production);
			//}

			productionList.setValue(Collections.unmodifiableList(productionList.getValue()));
		}

		symbolTable = Collections.unmodifiableMap(symbolTable);
		productionTable = Collections.unmodifiableMap(productionTable);
	}

	private void generateNonterminals(Path p) throws IOException {
		Charset charset = Charset.forName("US-ASCII");
		try (BufferedReader file = Files.newBufferedReader(p, charset);) {
			String line;
			while ((line = file.readLine()) != null) {
				if (line.matches(NONTERMINAL_DELIMINATOR)) {
					symbolTable.put(line.substring(0, line.length()-1), ++numNonterminals);
					productionTable.put(numNonterminals, new LinkedList<Production>());

					if (initialNonterminal == null) {
						initialNonterminal = numNonterminals;
					}
				}
			}
		}
	}

	private void generateProductions(Path p) throws IOException {
		Charset charset = Charset.forName("US-ASCII");
		try (BufferedReader file = Files.newBufferedReader(p, charset);) {
			Production production;
			List<Production> productionRules;
			Integer currentProduction = null;

			String line;
			String[] tokens;
			while ((line = file.readLine()) != null) {
				if (line.isEmpty()) {
					continue;
				}

				if (line.matches(NONTERMINAL_DELIMINATOR)) {
					currentProduction = symbolTable.get(line.substring(0, line.length()-1));
					assert currentProduction <= numNonterminals;
					continue;
				}

				production = new Production(currentProduction, new ArrayList<Integer>());
				productionRules = productionTable.get(currentProduction);
				productionRules.add(production);

				line = line.trim();
				tokens = line.split(" ");
				for (String token : tokens) {
					if (token.isEmpty()) {
						continue;
					}

					int tokenId = resolveIndex(token);
					if (tokenId != -1) {
						production.add(tokenId);
					} else {
						System.err.format("Unresolved token: \"%s\"%n", token);
						System.err.format("Line: \"%s\"%n", line);
					}
				}
			}
		}
	}

	private int resolveIndex(String token) {
		if (symbolTable.containsKey(token)) {
			return symbolTable.get(token);
		}

		return -1;
	}

	public boolean isNonterminal(String s) {
		Integer id = symbolTable.get(s);
		if (id == null) {
			return false;
		}

		return id <= numNonterminals;
	}

	private int copy = 0;
	private volatile int _goto;

	public SLRTable generateTables() {
		shiftListSwitch = new ArrayList<>();
		shiftList = new ArrayList<>();

		reduceList = new ArrayList<>();

		gotoListSwitch = new ArrayList<>();
		gotoList = new ArrayList<>();

		// generate table with initial nonterminal productions as initial items
		slrTables = new ArrayList<>();
		workLoad = new LinkedList<>();
		workLoad.offer(productionTable.get(initialNonterminal));
		while (!workLoad.isEmpty()) {
			_goto = 0;
			generateTables(workLoad.poll());
			_goto++;
		}

		Charset charset = Charset.forName("US-ASCII");
		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(".", "output", "toy.slrtables.dump.txt"), charset, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			for (Table t : slrTables) {
				writer.write(String.format("%s%n", t));

				for (Production p : t.getInitialProductions()) {
					writer.write(String.format("I:%s%n", p));
				}

				for (Production p : t.getClosure()) {
					writer.write(String.format("  %s%n", p));
				}

				writer.write(String.format("%n"));
				writer.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.format("%d table(s) generated%n", slrTables.size());
		System.out.format("%d tables avoided%n", copy);

		return new SLRTable(
			slrTables.size(),
			new SLRTable.ShiftTable(compressShiftListSwitch(), compressShiftList()),
			new SLRTable.ReduceTable(compressReduceList()),
			new SLRTable.GotoTable(compressGotoListSwitch(), compressGotoList())
		);
	}

	private void generateTables(List<Production> initialProductions) {
		//boolean flag = false;
		boolean tablesMatch;
		nextTable: for (Table t : slrTables) {
			//flag = true;
			if (t.getInitialProductions().size() != initialProductions.size()) {
				continue nextTable;
			}

			tablesMatch = true;
			for (Production p : t.getInitialProductions()) {
				for (Production p2 : initialProductions) {
					tablesMatch = p.sameAs(p2);
					//tablesMatch = p.sameAs(new Production(p2));
					if (!tablesMatch) {
						continue nextTable;
					}
				}
			}

			if (tablesMatch) {
				for (Production p : initialProductions) {
					p.setGoto(t.getTableId());
				}

				copy++;
				return;
			}
		}

		//if (flag) {
		//	for (Production p : initialProductions) {
		//		p = new Production(p);
		//	}
		//}

		List<Production> closure = new LinkedList<>(initialProductions);
		for (Production p : initialProductions) {
			// generate a recursive closure for each production in the inital list
			generateClosureForProduction(initialProductions, closure, p);
		}

		Set<Integer> symbolsParsed = new HashSet<>();
		closure.removeAll(initialProductions);
		Table t = new Table(slrTables.size(), initialProductions, closure);
		t.setGoto(_goto, initialProductions.get(0).getPrevious());
		slrTables.add(t);
		for (Production p : t) {
			if (p.hasMore()) {
				Integer head = p.getHead();
				if (symbolsParsed.contains(head)) {
					continue;
				}

				List<Production> initialProductionsForNextTable = new LinkedList<>();
				symbolsParsed.add(head);

				for (Production sibling : t) {
					Integer siblingHead = sibling.getHead();
					if (head == siblingHead) {
						initialProductionsForNextTable.add(new Production(sibling));
						//initialProductionsForNextTable.add(sibling);
					}
				}

				workLoad.offer(initialProductionsForNextTable);
			}
		}
	}

	private void generateClosureForProduction(List<Production> initialProductions, List<Production> closure, Production p) {
		if (p.hasMore()) {
			// if the next symbol in a production after the dot is a nonterminal, recursively generate more closures
			Integer head = p.getHead();
			if (head <= numNonterminals) {
				for (Production child : productionTable.get(head)) {
					// Only close over this child production if it hasn't been done already
					if (!closure.contains(child)) {
						closure.add(child);
						generateClosureForProduction(initialProductions, closure, child);
					}
				}
			}
		}
	}

	public void dumpConvertedCFGData() throws IOException {
		Charset charset = Charset.forName("US-ASCII");
		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(".", "output", "toy.sym.dump.txt"), charset, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			for (Entry<String, Integer> entry : symbolTable.entrySet()) {
				writer.write(String.format("%3d %s%n", entry.getValue(), entry.getKey()));
			}
		}

		Map<Integer, String> reverseEngineeredSymbols = new HashMap<>();
		for (Entry<String, Integer> entry : symbolTable.entrySet()) {
			if (entry.getKey().charAt(0) != '_' && !Character.isUpperCase(entry.getKey().charAt(0))) {
				continue;
			}

			reverseEngineeredSymbols.put(entry.getValue(), entry.getKey());
		}

		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(".", "output", "toy.cfg.dump.txt"), charset, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			for (Entry<Integer, List<Production>> entry : productionTable.entrySet()) {
				writer.write(String.format("%s:%n", reverseEngineeredSymbols.get(entry.getKey())));
				for (Production p : entry.getValue()) {
					writer.write(String.format("\t"));
					for (Integer i : p) {
						writer.write(String.format("%s ", reverseEngineeredSymbols.get(i)));
					}

					writer.write(String.format("%n"));
				}

				writer.write(String.format("%n"));
			}
		}
	}

	private int[] compressShiftListSwitch() {
		return compressSwitch(shiftListSwitch);
	}

	private int[] compressReduceList() {
		return compressSwitch(reduceList);
	}

	private int[] compressGotoListSwitch() {
		return compressSwitch(gotoListSwitch);
	}

	private int[] compressSwitch(ArrayList<Integer> list) {
		int[] to = new int[list.size()];

		int i = 0;
		for (Integer cell : list) {
			to[i] = cell;
		}

		return to;
	}

	private int[][] compressShiftList() {
		return compress(shiftList);
	}

	private int[][] compressGotoList() {
		return compress(gotoList);
	}

	private int[][] compress(ArrayList<Integer[]> list) {
		int[][] to = new int[list.size()+1][2];
		to[0][0] = Integer.MIN_VALUE;
		to[0][1] = Integer.MIN_VALUE;

		int i = 1;
		for (Integer[] cell : list) {
			to[i][0] = cell[0];
			to[i][1] = cell[1];
		}

		return to;
	}
}
