package edu.csupomona.cs.cs411.project2;

import edu.csupomona.cs.cs411.project1.lexer.Lexer;
import edu.csupomona.cs.cs411.project1.lexer.Token;
import edu.csupomona.cs.cs411.project2.preprocessor.SLRTableGenerator;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Main {
	private static final Path OUTPUT_PATH = Paths.get(".", "output");

	public static void main(String[] args) throws IOException {
		SLRTableGenerator g = new SLRTableGenerator(Paths.get(".", "res", "toy.cfg.txt"));
		g.dump();

		for (String arg : args) {
			Path p = Paths.get(arg);
			DirectoryStream<Path> files = Files.newDirectoryStream(p);
			for (Path path : files) {
				if (!Files.isReadable(path)) {
					continue;
				}

				analyzeFile(path);
			}
		}
	}

	private static void analyzeFile(Path p) {
		String fileName = p.getFileName().toString();
		Path outFile = OUTPUT_PATH.resolve(fileName.substring(0, fileName.lastIndexOf('.')) + ".output.txt");
		Charset charset = Charset.forName("US-ASCII");
		try (BufferedWriter writer = Files.newBufferedWriter(outFile, charset, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			try (		InputStream in = Files.newInputStream(p);
					BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
				System.out.format("Analyzing %s%n", p);
				Lexer l = new Lexer(br);
				for (Token t : l) {
					writer.write(String.format(" %s", t.toString()));
				}

				writer.write(String.format("%n"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
