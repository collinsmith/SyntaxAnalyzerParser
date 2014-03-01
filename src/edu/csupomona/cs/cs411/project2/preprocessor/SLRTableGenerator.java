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
	private Map<String, Integer> symbols;
	private Map<Integer, List<List<Integer>>> productions;

	public SLRTableGenerator(Path p) throws IOException {
		productions = new HashMap<>();
		symbols = new LinkedHashMap<>();

		generateNonterminals(p);
		for (Keywords k : Keywords.values()) {
			symbols.put(k.name(), numNonterminals+k.ordinal()+1);
			if (Keywords.ACTUAL_KEYWORDS.contains(k) || Keywords.OPERATORS.contains(k)) {
				symbols.put(k.getRegex(), numNonterminals+k.ordinal()+1);
			}
		}

		// TODO I know I'm not stupid enough to do it, but productions should be immutable
		generateProductions(p);
		for (Entry<Integer, List<List<Integer>>> productionList : productions.entrySet()) {
			for (List<Integer> production : productionList.getValue()) {
				production = Collections.unmodifiableList(production);
			}

			productionList.setValue(Collections.unmodifiableList(productionList.getValue()));
		}

		symbols = Collections.unmodifiableMap(symbols);
		productions = Collections.unmodifiableMap(productions);
	}

	private void generateNonterminals(Path p) throws IOException {
		Charset charset = Charset.forName("US-ASCII");
		try (BufferedReader file = Files.newBufferedReader(p, charset);) {
			String line;
			while ((line = file.readLine()) != null) {
				if (line.matches(NONTERMINAL_DELIMINATOR)) {
					symbols.put(line.substring(0, line.length()-1), ++numNonterminals);
					productions.put(numNonterminals, new LinkedList<List<Integer>>());
				}
			}
		}
	}

	private void generateProductions(Path p) throws IOException {
		Charset charset = Charset.forName("US-ASCII");
		try (BufferedReader file = Files.newBufferedReader(p, charset);) {
			String line;
			String[] tokens;
			Integer currentProduction = null;
			List<List<Integer>> productionRules;
			List<Integer> production;
			while ((line = file.readLine()) != null) {
				if (line.isEmpty()) {
					continue;
				}

				if (line.matches(NONTERMINAL_DELIMINATOR)) {
					currentProduction = symbols.get(line.substring(0, line.length()-1));
					assert currentProduction <= numNonterminals;
					continue;
				}

				production = new LinkedList<>();
				productionRules = productions.get(currentProduction);
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
		if (symbols.containsKey(token)) {
			return symbols.get(token);
		}

		return -1;
	}

	public void dump() throws IOException {
		Charset charset = Charset.forName("US-ASCII");
		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(".", "output", "toy.sym.dump.txt"), charset, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			for (Entry<String, Integer> entry : symbols.entrySet()) {
				writer.write(String.format("%3d %s%n", entry.getValue(), entry.getKey()));
			}
		}

		Map<Integer, String> reverseEngineeredSymbols = new HashMap<>();
		for (Entry<String, Integer> entry : symbols.entrySet()) {
			if (entry.getKey().charAt(0) != '_' && !Character.isUpperCase(entry.getKey().charAt(0))) {
				continue;
			}

			reverseEngineeredSymbols.put(entry.getValue(), entry.getKey());
		}

		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(".", "output", "toy.cfg.dump.txt"), charset, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			for (Entry<Integer, List<List<Integer>>> entry : productions.entrySet()) {
				writer.write(String.format("%s:%n", reverseEngineeredSymbols.get(entry.getKey())));
				for (List<Integer> l : entry.getValue()) {
					writer.write(String.format("\t"));
					for (Integer i : l) {
						writer.write(String.format("%s ", reverseEngineeredSymbols.get(i)));
					}

					writer.write(String.format("%n"));
				}

				writer.write(String.format("%n"));
			}
		}
	}
}
