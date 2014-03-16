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

/**
 * This class represents an abstract {@link Parser} generator which is used
 * to read in a CFG from a file and generate a list of Productions and a maps
 * of symbols (from their String representation to integer identifier) and
 * nonterminals (with their sets of Productions). This information is then
 * used to generate a specialized generator which can create specialized
 * {@link Table}s for that parser.
 *
 * @author Collin Smith <collinsmith@csupomona.edu>
 */
public abstract class AbstractParserGenerator {
	// TODO conver to use Integer.MAX_VALUE - id for alternates
	/**
	 * The offset given to those terminals which have alternatives that
	 * can be used when expressing them within the CFG given.
	 */
	protected static final int EXTRA_TERMINALS_OFFSET = 1000;

	/**
	 * Charset used when writing output.
	 */
	protected static final Charset CHARSET = Charset.forName("US-ASCII");

	/**
	 * String representation of the regular expression delimiting the location
	 * of new CFG nonterminals.
	 */
	protected static final String NONTERMINAL_DEF_REGEX = "[A-Z][a-zA-Z0-9]*:";

	/**
	 * List of all Production contained within the CFG.
	 */
	protected final ImmutableList<Production> PRODUCTIONS;

	/**
	 * Mapping of all symbols (terminal and nonterminal) contained within the
	 * CFG.
	 */
	protected final ImmutableBiMap<String, Integer> SYMBOLS;

	/**
	 * Mapping of all nonterminals to their corresponding set of productions.
	 */
	protected final ImmutableBiMap<Integer, ImmutableSet<Production>> NONTERMINALS;

	/**
	 * This field represents the number of nonterminals within the CFG.
	 */
	private int numTerminals;

	/**
	 * This field represents the number of terminals within the CFG.
	 */
	private int numNonterminals;

	/**
	 * This field represents the number of unreachable symbols within the CFG.
	 */
	private int numUnreachableSymbols;

	/**
	 * This field represents the initial nonterminal symbol id.
	 */
	private int initialNonterminal;

	/**
	 * Constructs an AbstractParserGenerator using the CFG at the given Path.
	 *
	 * @param p path to the CFG to generate a Parser for
	 * @throws IOException when the CFG file does not exist or cannot be read
	 */
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

	/**
	 * Generates the mapping of symbols from their String representation to
	 * their Integer representation. Note that this is a bidirectional
	 * mapping, so we can go from the Integer representation back to its
	 * corresponding String representation.
	 *
	 * @param p path to the CFG to generate the symbols table from
	 * @return bidirectional map from a String to its corresponding symbol id
	 * @throws IOException when the CFG file does not exist or cannot be read
	 */
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


	/**
	 * Generates the mapping of nonterminals from their nonterminal id to
	 * their corresponding sets of Productions. Note that this is a
	 * bidirectional map. Additionally, this method will populate the passed
	 * list with the list of all Productions found.
	 *
	 * @param p path to the CFG to generate the symbols table from
	 * @param productions List to add the productions found into
	 * @return bidirectional map from a nonterminal id to its corresponding
	 *	set of Productions
	 * @throws IOException when the CFG file does not exist or cannot be read
	 */
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
		for (int symbol : unusedSymbols) {
			if (symbol == 0) {
				continue;
			}

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

	/**
	 * Returns the symbol id associated with a specified token.
	 *
	 * @param token String representation of the token.
	 * @return identifier of that token or throws a ProductionException if
	 *	the token is undefined.
	 */
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

	/**
	 * Returns whether or not a given symbol identifier is a nonterminal.
	 *
	 * @param tokenId symbol id to check
	 * @return {@code true} if it is, otherwise {@code false}
	 */
	protected final boolean isNonterminal(int tokenId) {
		return numTerminals <= tokenId && tokenId < (numTerminals+numNonterminals);
	}

	/**
	 * Returns the number of nonterminals in this AbstractParserGenerator.
	 *
	 * @return total number of nonterminals found or {@link Integer#MIN_VALUE}
	 *	if that has yet to be calculated
	 */
	public final int getNumNonterminals() {
		return numNonterminals;
	}

	/**
	 * Returns the number of terminals in this AbstractParserGenerator.
	 *
	 * @return total number of terminals found or {@link Integer#MIN_VALUE}
	 *	if that has yet to be calculated
	 */
	public final int getNumTerminals() {
		return numTerminals;
	}

	/**
	 * Returns the number of unreachable Productions in this
	 * AbstractParserGenerator.
	 *
	 * @return total number of unreachable Productions found or
	 *	{@link Integer#MIN_VALUE} if that has yet to be calculated
	 */
	public final int getNumUnreachableSymbols() {
		return numUnreachableSymbols;
	}

	/**
	 * Returns the initial nonterminal of this CFG. This is typically the
	 * first nonterminal created, but subclasses may override it.
	 *
	 * @return initial nonterminal symbol of this CFG
	 */
	public final int getInitialNonterminal() {
		return initialNonterminal;
	}

	/**
	 * Changes the initial nonterminal to the one specified.
	 *
	 * @param nonterminal new initial nonterminal symbol for this CFG
	 */
	protected final void setInitialNonterminal(int nonterminal) {
		Preconditions.checkArgument(isNonterminal(nonterminal), "Invalid nonterminal ID given.");
		initialNonterminal = nonterminal;
	}

	/**
	 * Dumps two files which contain information regarding this
	 * AbstractParserGenerator. This includes a file containing all symbols
	 * and a file containing all productions. Also included are the symbol
	 * identifiers for each symbol to help readability.
	 */
	public void outputCFG() {
		outputSymbols();
		outputProductions();
	}

	/**
	 * Outputs a file containing symbol information of this
	 * AbstractParserGenerator.
	 */
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

	/**
	 * Outputs a file containing Production information of this
	 * AbstractParserGenerator.
	 */
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
