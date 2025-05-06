package Debuggers;

import Interpreter.*;


import java.util.List;


public class LexerTest {

    public static void main(String[] args) {
        testLexer("SUGOD\nMUGNA NUMERO x=5, y=10\nIPAKITA: x + y\nKATAPUSAN");
        testLexer("SUGOD\nMUGNA LETRA a_1='n'\nMUGNA TINUOD t=\"OO\"\nIPAKITA: a_1 & t\nKATAPUSAN");
        testLexer("SUGOD\nMUGNA NUMERO a=100, b=200\nb = a * 5\nIPAKITA: b\nKATAPUSAN");
        testLexer("SUGOD\n-- This is a comment\nMUGNA NUMERO a=100\nIPAKITA: a\nKATAPUSAN");
        testLexer("SUGOD\nMUGNA TIPIK num=3.14\nIPAKITA: num\nKATAPUSAN");
        testLexer("SUGOD\nMUGNA LETRA c='x'\nIPAKITA: c\nKATAPUSAN");
        testLexer("SUGOD\nKUNG (a > b) PUNDOK { MUGNA NUMERO x=5 }\nKATAPUSAN");
        testLexer("SUGOD\nDAWAT: x, y\nKATAPUSAN");
        testLexer("SUGOD\nMUGNA NUMERO xyz=10\nxyz = xyz - 2\nIPAKITA: xyz\nKATAPUSAN");
    }

    private static void testLexer(String input) {
        System.out.println("Testing input:\n" + input);
        Lexer lexer = new Lexer(input);
        List<Token> tokens = lexer.scanTokens();

        // Print the tokens and their types
        for (Token token : tokens) {
            System.out.println(token);
        }

        System.out.println("\n===========================================\n");
    }


}