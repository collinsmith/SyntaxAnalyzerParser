package edu.csupomona.cs.cs411.project2.preprocessor;

import edu.csupomona.cs.cs411.project1.lexer.Keywords;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class SLRTableGenerator {
	private static final String NONTERMINAL_DELIMINATOR = "[A-Z][a-zA-Z0-9]*:";

	private int numNonterminals = 0;
	private Map<String, Integer> symbolTable;
	private Map<Integer, List<Production>> productionTable;

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

				production = new Production(new LinkedList<Integer>());
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

	public void dump() throws IOException {
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
}
