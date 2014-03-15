package edu.csupomona.cs.cs411.project2;

import edu.csupomona.cs.cs411.project1.lexer.Lexer;
import edu.csupomona.cs.cs411.project1.lexer.Token;
import edu.csupomona.cs.cs411.project1.lexer.TokenStream;
import edu.csupomona.cs.cs411.project1.lexer.ToyLexer;
import edu.csupomona.cs.cs411.project2.parser.Parser;
import edu.csupomona.cs.cs411.project2.parser.lalr.LALRParser;
import edu.csupomona.cs.cs411.project2.parser.lalr.LALRParserGenerator;
import edu.csupomona.cs.cs411.project2.parser.lalr.LALRTables;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Main {
	private static final Path OUTPUT_PATH = Paths.get(".", "output");

	public static void main(String[] args) throws IOException {
		LALRParserGenerator g = new LALRParserGenerator(Paths.get(".", "res", "toy.cfg.txt"));
		g.outputCFG();
		g.outputTables();

		LALRTables lalrTables = g.getGeneratedLALRTables();
		lalrTables.getSLRTables().outputTableInfo();

		Lexer<Token> lexer = new ToyLexer();
		Parser parser = new LALRParser(lalrTables);

		for (String arg : args) {
			Path p = Paths.get(arg);
			DirectoryStream<Path> files = Files.newDirectoryStream(p);
			for (Path path : files) {
				if (!Files.isReadable(path)) {
					continue;
				}

				scanAndParse(lexer, parser, path);
			}
		}
	}

	private static void scanAndParse(Lexer<Token> lexer, Parser parser, Path p) throws IOException {
		String fileName = p.getFileName().toString();
		Path outFile = OUTPUT_PATH.resolve(fileName.substring(0, fileName.lastIndexOf('.')) + ".output.txt");
		Charset charset = Charset.forName("US-ASCII");
		try (BufferedWriter writer = Files.newBufferedWriter(outFile, charset, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			try (BufferedReader br = Files.newBufferedReader(p, charset)) {
				System.out.format("Analyzing %s...%n", fileName);
				long dt = System.currentTimeMillis();
				TokenStream tokenStream = lexer.lex(br);
				boolean accepted = parser.parse(tokenStream, writer);
				System.out.format("%s scanned and parsed in %dms; %1$s has been %s%n", fileName, System.currentTimeMillis()-dt, accepted ? "ACCEPTED" : "REJECTED");
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
