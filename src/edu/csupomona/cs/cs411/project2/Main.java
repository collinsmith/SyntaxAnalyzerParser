package edu.csupomona.cs.cs411.project2;

import edu.csupomona.cs.cs411.project1.lexer.Lexer;
import edu.csupomona.cs.cs411.project1.lexer.Token;
import edu.csupomona.cs.cs411.project1.lexer.TokenStream;
import edu.csupomona.cs.cs411.project2.parser.Parser;
import edu.csupomona.cs.cs411.project2.parser.generator.Generator;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Main {
	private static final Path OUTPUT_PATH = Paths.get(".", "output");

	public static void main(String[] args) throws IOException {
		Generator g = new Generator(Paths.get(".", "res", "toy.cfg.txt"));
		g.outputCFG();
		g.outputTables();

		/*ParserGenerator g = new ParserGenerator(Paths.get(".", "res", "toy.cfg.txt"));
		g.outputConvertedCFG();
		g.outputTables();

		SLRTable t = g.getSLRTable();
		t.outputTableInfo();

		Lexer<Token> lexer = new ToyLexer();
		SLRParser parser = new SLRParser(t);

		for (String arg : args) {
			Path p = Paths.get(arg);
			DirectoryStream<Path> files = Files.newDirectoryStream(p);
			for (Path path : files) {
				if (!Files.isReadable(path)) {
					continue;
				}

				scanAndParse(lexer, parser, path);
			}
		}*/
	}

	private static void scanAndParse(Lexer<Token> lexer, Parser parser, Path p) throws IOException {
		String fileName = p.getFileName().toString();
		Path outFile = OUTPUT_PATH.resolve(fileName.substring(0, fileName.lastIndexOf('.')) + ".output.txt");
		Charset charset = Charset.forName("US-ASCII");
		try (BufferedWriter writer = Files.newBufferedWriter(outFile, charset, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			try (		InputStream in = Files.newInputStream(p);
					BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
				//System.out.format("Analyzing %s%n", fileName);
				TokenStream tokenStream = lexer.lex(br);
				boolean accepted = parser.parse(tokenStream, writer);
				System.out.format("%s has been %s%n", fileName, accepted ? "ACCEPTED" : "REJECTED");
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
