package Interpreter;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class BisayaMain {
    public static final boolean DEBUG = false;

    public static void main(String[] args) {
        if (args.length > 1) {
            System.out.println("Usage: java BisayaMain [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    public static void printDebug(String message){
        if (DEBUG) System.out.println(message);
    }

    private static void runFile(String path) {
        try {
            // Try multiple possible locations
            File file = new File(path);
            if (!file.exists()) {
                file = new File("src/TestCases" + path);
            }
            if (!file.exists()) {
                file = new File("../src/TestCases" + path);
            }
            if (!file.exists()) {
                file = new File("untitled/src/TestCases" + path);
            }
            if (!file.exists()) {
                System.err.println("Error: Could not find file '" + path + "'");
                System.err.println("Tried locations:");
                System.err.println("- " + new File(path).getAbsolutePath());
                System.err.println("- " + new File("src/" + path).getAbsolutePath());
                System.err.println("- " + new File("../src/" + path).getAbsolutePath());
                System.err.println("- " + new File("untitled/src/" + path).getAbsolutePath());
                System.err.println("Current directory: " + System.getProperty("user.dir"));
                System.exit(74);
            }

            byte[] bytes = Files.readAllBytes(file.toPath());
            run(new String(bytes, "UTF-8"));
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            System.err.println("Current directory: " + System.getProperty("user.dir"));
            System.exit(74);
        }
    }

    private static void runPrompt() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Bisaya++ Interpreter");
        System.out.println("Type 'exit' to quit");

        while (true) {
            System.out.print("> ");
            String line = scanner.nextLine();
            if (line.equals("exit")) break;
            run(line);
        }
    }

    private static void run(String source) {
        try {

            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.scanTokens();



            Parser parser = new Parser(tokens);
            List<Parser.Stmt> statements = parser.parse();

            Interpreter interpreter = new Interpreter(parser);
            interpreter.interpret(statements);
        } catch (RuntimeException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}