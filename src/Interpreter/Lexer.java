package Interpreter;

import java.util.*;

public class Lexer {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;

    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("SUGOD", TokenType.SUGOD);
        keywords.put("KATAPUSAN", TokenType.KATAPUSAN);
        keywords.put("MUGNA", TokenType.MUGNA);
        keywords.put("NUMERO", TokenType.NUMERO);
        keywords.put("LETRA", TokenType.LETRA);
        keywords.put("TINUOD", TokenType.TINUOD);
        keywords.put("TIPIK", TokenType.TIPIK);
        keywords.put("IPAKITA", TokenType.IPAKITA);
        keywords.put("DAWAT", TokenType.DAWAT);
        keywords.put("KUNG", TokenType.KUNG);
        keywords.put("KUNG WALA", TokenType.KUNG_WALA);
        keywords.put("KUNG DILI", TokenType.KUNG_DILI);
        keywords.put("PUNDOK", TokenType.PUNDOK);
        keywords.put("ALANG SA", TokenType.ALANG_SA);
        keywords.put("UG", TokenType.UG);
        keywords.put("O", TokenType.O);
        keywords.put("DILI", TokenType.DILI);
    }

    public Lexer(String source) {
        this.source = source;
    }

    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }

        tokens.add(new Token(TokenType.EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(': addToken(TokenType.LPAREN); break;
            case ')': addToken(TokenType.RPAREN); break;
            case '{': addToken(TokenType.LBRACE); break;
            case '}': addToken(TokenType.RBRACE); break;
            case ',': addToken(TokenType.COMMA); break;
            case ':': addToken(TokenType.COLON); break;
            case '&': addToken(TokenType.CONCAT); break;
            case '+': addToken(TokenType.PLUS); break;
            case '-':
                if (match('-')) {
                    // A comment goes until the end of the line.
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else {
                    addToken(TokenType.MINUS);
                }
                break;
            case '*': addToken(TokenType.MULTIPLY); break;
            case '/': addToken(TokenType.DIVIDE); break;
            case '%': addToken(TokenType.MODULO); break;
            case '=':
                if (match('=')) {
                    addToken(TokenType.EQUAL);
                } else {
                    addToken(TokenType.ASSIGN);
                }
                break;
            case '<':
                if (match('>')) {
                    addToken(TokenType.NOT_EQUAL);
                } else if (match('=')) {
                    addToken(TokenType.LESS_EQUAL);
                } else {
                    addToken(TokenType.LESS);
                }
                break;
            case '>':
                if (match('=')) {
                    addToken(TokenType.GREATER_EQUAL);
                } else {
                    addToken(TokenType.GREATER);
                }
                break;
            case ' ':
            case '\r':
            case '\t':
                // Ignore whitespace.
                break;
            case '\n':
                line++;
                break;
            case '"':
                string();
                break;
            case '\'':
                character();
                break;
            case '[':
                if (match('#') && match(']')) {
                    addToken(TokenType.STRING, "#");
                } else {
                    throw new RuntimeException("Unexpected character: [");
                }
                break;
            case '$':
                addToken(TokenType.NEWLINE);
                break;
            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    throw new RuntimeException("Unexpected character: " + c);
                }
                break;
        }
    }

    private void character() {
        while (peek() != '\'' && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }

        if (isAtEnd()) {
            throw new RuntimeException("Unterminated character literal.");
        }

        advance(); // The closing '

        String value = source.substring(start + 1, current - 1);
        if (value.length() != 1) {
            throw new RuntimeException("Character literal must contain exactly one character.");
        }
        addToken(TokenType.CHAR, value.charAt(0));
    }

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }

        if (isAtEnd()) {
            throw new RuntimeException("Unterminated string.");
        }

        advance(); // The closing "

        String value = source.substring(start + 1, current - 1);
        addToken(TokenType.STRING, value);
    }

    private void number() {
        while (isDigit(peek())) advance();

        // If a '.' is encountered and followed by digits, it's a TIPIK (decimal)
        if (peek() == '.' && isDigit(peekNext())) {
            advance(); // Consume the '.'
            while (isDigit(peek())) advance(); // Consume the digits after the '.'

            String numberString = source.substring(start, current);
            try {
                double value = Double.parseDouble(numberString);
                addToken(TokenType.TIPIK, value);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid number format: " + numberString);
            }
        } else {
            // If it's just an integer, create a NUMERO token
            String numberString = source.substring(start, current);
            try {
                double value = Double.parseDouble(numberString);
                addToken(TokenType.NUMERO, value);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid number format: " + numberString);
            }
        }
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        String text = source.substring(start, current);

        // Handle special case for multi-word keywords
        if (text.equals("KUNG")) {
            if (peek() == ' ') {
                advance();
                start = current;
                while (isAlphaNumeric(peek())) advance();
                String secondPart = source.substring(start, current);
                if (secondPart.equals("DILI")) {
                    addToken(TokenType.KUNG_DILI);
                    return;
                } else if (secondPart.equals("WALA")) {
                    addToken(TokenType.KUNG_WALA);
                    return;
                }
            }
            addToken(TokenType.KUNG); // Add KUNG token if not KUNG DILI or KUNG WALA
            return;
        } else if (text.equals("ALANG")) {
            if (peek() == ' ') { // Check for a space after "ALANG"
                advance(); // Consume space
                start = current; // Reset start index for "SA"
                while (isAlphaNumeric(peek())) advance();
                String secondPart = source.substring(start, current);

                if (secondPart.equals("SA")) {
                    addToken(TokenType.ALANG_SA);
                    return;
                }
            }
        }

        // Default behavior for single-word keywords or identifiers
        TokenType type = keywords.get(text);
        if (type == null) type = TokenType.IDENTIFIER;
        addToken(type);
    }
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private char advance() {
        return source.charAt(current++);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }
}

