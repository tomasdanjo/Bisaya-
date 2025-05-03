package Interpreter;

public class Token {
    public final TokenType type;
    public final String lexeme;
    public final Object literal;
    public final int line;
    public TokenType varType;  // For storing variable type (NUMERO, TINUOD, LETRA)

    public Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
        this.varType = null;
    }

    @Override
    public String toString() {
        return "Token{" +
                "type=" + type +
                ", lexeme='" + lexeme + '\'' +
                ", literal=" + literal +
                ", line=" + line +
                ", varType=" + varType +
                '}';
    }
}