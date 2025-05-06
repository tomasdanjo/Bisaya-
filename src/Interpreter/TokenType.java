package Interpreter;

public enum TokenType {
    // Keywords
    SUGOD, KATAPUSAN, MUGNA, NUMERO, LETRA, TINUOD, TIPIK,
    IPAKITA, DAWAT, KUNG, KUNG_WALA, KUNG_DILI, PUNDOK,
    ALANG_SA, UG, O, DILI,

    // Operators
    PLUS, MINUS, MULTIPLY, DIVIDE, MODULO,
    GREATER, LESS, GREATER_EQUAL, LESS_EQUAL,
    EQUAL, NOT_EQUAL, ASSIGN,
    NOT,

    // Literals
    NUMBER, CHAR, BOOLEAN, STRING,

    // Special characters
    IDENTIFIER, COMMA, SEMICOLON, LPAREN, RPAREN,
    LBRACE, RBRACE, CONCAT, NEWLINE, ESCAPE,
    COLON,
    EOF,

    // Single-character tokens
    DOT
}