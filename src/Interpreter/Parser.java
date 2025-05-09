package Interpreter;

import java.util.*;

import static Interpreter.BisayaMain.printDebug;

public class Parser {
    private static final boolean DEBUG = BisayaMain.DEBUG;
    private final List<Token> tokens;
    private int current = 0;
    private final Map<String, TokenType> variableTypes = new HashMap<>();
    private static final Set<TokenType> KEYWORDS = new HashSet<>(Arrays.asList(
            TokenType.SUGOD, TokenType.KATAPUSAN, TokenType.MUGNA, TokenType.NUMERO, TokenType.LETRA,
            TokenType.TINUOD, TokenType.TIPIK, TokenType.IPAKITA, TokenType.DAWAT, TokenType.KUNG,
            TokenType.KUNG_WALA, TokenType.KUNG_DILI, TokenType.PUNDOK, TokenType.ALANG_SA,
            TokenType.UG, TokenType.O, TokenType.DILI
    ));
    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public List<Stmt> parse() {
        printDebug("Starting parsing...");

        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            try {
                Stmt stmt = declaration();
                if (stmt != null) {
                    statements.add(stmt);
                }
            } catch (RuntimeException error) {
                printDebug("DEBUG: Error parsing statement: " + error.getMessage());
                synchronize();
            }
        }
        printDebug("DEBUG: Parsed " + statements.size() + " statements");
        return statements;
    }

    public Map<String, TokenType> getVariableTypes() {
        return variableTypes;
    }

    private Stmt declaration() {
        try {
            if (match(TokenType.SUGOD)) return block();
            if (match(TokenType.MUGNA)) return varDeclaration();
            return statement();
        } catch (RuntimeException error) {
            throw new Error("DEBUG: Error in declaration: " + error.getMessage());
//            synchronize();
//            return null;
        }
    }

    private Stmt statement() {
        if (match(TokenType.IPAKITA)) return printStatement();
        if (match(TokenType.DAWAT)) return inputStatement();
        if (match(TokenType.KUNG)) return ifStatement();
        if (match(TokenType.PUNDOK)) {
            printDebug("DEBUG: Found PUNDOK");
            consume(TokenType.LBRACE, "Expect '{' after PUNDOK.");
            List<Stmt> statements = new ArrayList<>();
            while (!check(TokenType.RBRACE) && !isAtEnd()) {
                Stmt stmt = statement();
                if (stmt != null) {
                    statements.add(stmt);
                    printDebug("DEBUG: Added statement to block: " + stmt);
                }
            }
            consume(TokenType.RBRACE, "Expect '}' after block.");
            return new Stmt.Block(statements);
        }
        if (match(TokenType.ALANG_SA)) {
            printDebug("DEBUG: Found ALANG SA statement");
            return whileStatement();
        }
        return expressionStatement();
    }

    private Stmt printStatement() {
        printDebug("DEBUG: Parsing print statement");
        consume(TokenType.COLON, "Expect ':' after IPAKITA.");
        List<Expr> expressions = new ArrayList<>();
        do {
            Expr expr = expression();
            expressions.add(expr);
            printDebug("DEBUG: Added expression to print: " + expr);
        } while (match(TokenType.CONCAT));
        if (check(TokenType.IDENTIFIER) || check(TokenType.STRING) || check(TokenType.NUMBER) || check(TokenType.COMMA)) {
            throw new Error("Expected concatenation operator");
        }
        printDebug("DEBUG: Print statement parsed with " + expressions.size() + " expressions");
        return new Stmt.Print(expressions);
    }

    private Stmt inputStatement() {
        printDebug("DEBUG: Parsing input statement");
        consume(TokenType.COLON, "Expect ':' after DAWAT.");
        List<Token> variables = new ArrayList<>();
        do {
            Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");
            variables.add(name);
            printDebug("DEBUG: Added variable to input: " + name.lexeme + " with varType: " + variableTypes.getOrDefault(name.lexeme, null));
        } while (match(TokenType.COMMA));
        printDebug("DEBUG: Input statement parsed with " + variables.size() + " variables");
        return new Stmt.Input(variables);
    }

    private Stmt ifStatement() {
        consume(TokenType.LPAREN, "Expect '(' after 'KUNG'.");
        Expr condition = expression();
        consume(TokenType.RPAREN, "Expect ')' after if condition.");
        Stmt thenBranch = statement();
        Stmt elseBranch = null;

        if (match(TokenType.KUNG_DILI)) {
            consume(TokenType.LPAREN, "Expect '(' after 'KUNG DILI'.");
            Expr elseCondition = expression();
            consume(TokenType.RPAREN, "Expect ')' after KUNG DILI condition.");
            Stmt elseThenBranch = statement();
            Stmt elseElseBranch = null;

            if (match(TokenType.KUNG_WALA)) {
                elseElseBranch = statement();
            }

            elseBranch = new Stmt.If(elseCondition, elseThenBranch, elseElseBranch);
        } else if (match(TokenType.KUNG_WALA)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt block() {
        printDebug("DEBUG: Parsing block");
        List<Stmt> statements = new ArrayList<>();
        while (!check(TokenType.KATAPUSAN) && !isAtEnd()) {
            Stmt stmt = declaration();
            if (stmt != null) {
                statements.add(stmt);
                printDebug("DEBUG: Added statement to block: " + stmt);
            }
        }
        consume(TokenType.KATAPUSAN, "Expect 'KATAPUSAN' after block.");
        printDebug("DEBUG: Block parsed with " + statements.size() + " statements");
        return new Stmt.Block(statements);
    }

    private Stmt whileStatement() {
        printDebug("DEBUG: Starting while statement parsing");
        consume(TokenType.LPAREN, "Expect '(' after 'ALANG SA'.");
        printDebug("DEBUG: Found opening parenthesis");

        // Parse initialization
        Stmt initializer;
        if (match(TokenType.IDENTIFIER)) {
            Token name = previous();
            printDebug("DEBUG: Found identifier: " + name.lexeme);
            if (match(TokenType.ASSIGN)) {
                printDebug("DEBUG: Found assignment operator");
                Expr value = expression();
                printDebug("DEBUG: Parsed initializer value: " + value);
                initializer = new Stmt.Expression(new Expr.Assign(name, value));
            } else {
                throw new RuntimeException("Expect '=' after variable name.");
            }
        } else {
            initializer = expressionStatement();
        }
        printDebug("DEBUG: Parsed initializer: " + initializer);

        // Parse condition
        printDebug("DEBUG: Looking for comma");
        consume(TokenType.COMMA, "Expect ',' after initialization.");
        printDebug("DEBUG: Found comma, parsing condition");
        Expr condition = expression();
        printDebug("DEBUG: Parsed condition: " + condition);

        // Parse increment
        printDebug("DEBUG: Looking for second comma");
        consume(TokenType.COMMA, "Expect ',' after condition.");
        printDebug("DEBUG: Found second comma, parsing increment");
        Expr increment = null;
        if (match(TokenType.IDENTIFIER)) {
            Token name = previous();
            printDebug("DEBUG: Found identifier for increment: " + name.lexeme);
            // Manually check for ++ to control token pointer
            if (check(TokenType.PLUS) && peekNext() != null && peekNext().type == TokenType.PLUS) {
                advance(); // Consume first PLUS
                advance(); // Consume second PLUS
                printDebug("DEBUG: Found ++ operator");
                increment = new Expr.Assign(name, new Expr.Binary(
                        new Expr.Variable(name),
                        new Token(TokenType.PLUS, "+", null, name.line),
                        new Expr.Literal(1.0)
                ));
                printDebug("DEBUG: Created increment expression: " + name.lexeme + "++");
            }
        }
        if (increment == null) {
            throw new RuntimeException("Expect increment after comma.");
        }
        printDebug("DEBUG: Looking for closing parenthesis, current token: " + peek());
        consume(TokenType.RPAREN, "Expect ')' after for clauses.");
        printDebug("DEBUG: Found closing parenthesis");

        // Parse body
        printDebug("DEBUG: Parsing loop body");
        Stmt body;
        if (match(TokenType.PUNDOK)) {
            printDebug("DEBUG: Found PUNDOK");
            consume(TokenType.LBRACE, "Expect '{' after PUNDOK.");
            printDebug("DEBUG: Found opening brace");
            List<Stmt> statements = new ArrayList<>();
            while (!check(TokenType.RBRACE) && !isAtEnd()) {
                Stmt stmt = statement();
                if (stmt != null) {
                    statements.add(stmt);
                    printDebug("DEBUG: Added statement to block: " + stmt);
                }
            }
            consume(TokenType.RBRACE, "Expect '}' after block.");
            printDebug("DEBUG: Found closing brace");
            body = new Stmt.Block(statements);
        } else {
            body = statement();
        }
        printDebug("DEBUG: Parsed body: " + body);

        // Create a block that contains the increment after the body
        List<Stmt> bodyStatements = new ArrayList<>();
        bodyStatements.add(body);
        bodyStatements.add(new Stmt.Expression(increment));
        Stmt bodyWithIncrement = new Stmt.Block(bodyStatements);
        printDebug("DEBUG: Created body with increment");

        // Create the while loop
        Stmt whileLoop = new Stmt.While(condition, bodyWithIncrement);
        printDebug("DEBUG: Created while loop");

        // Create a block that contains the initializer and the while loop
        List<Stmt> statements = new ArrayList<>();
        statements.add(initializer);
        statements.add(whileLoop);
        printDebug("DEBUG: Created final block with initializer and while loop");
        return new Stmt.Block(statements);
    }

    // Add helper method to peek at the next token
    private Token peekNext() {
        if (current + 1 >= tokens.size()) return null;
        return tokens.get(current + 1);
    }

    private Stmt varDeclaration() {
        printDebug("DEBUG: Parsing variable declaration");
        TokenType type = null;
        if (match(TokenType.NUMERO, TokenType.TINUOD, TokenType.LETRA, TokenType.TIPIK)) {
            type = previous().type;
            printDebug("DEBUG: Found type declaration: " + type);
        }

        List<Stmt> declarations = new ArrayList<>();
        do {
            // Ensure next token is IDENTIFIER
            if (check(TokenType.ASSIGN)) {
                throw new Error("Expected variable name after comma");
            }

            Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");
            printDebug("DEBUG: Found variable name: " + name.lexeme);
//            if (name.lexeme.equals(name.lexeme.toUpperCase())) {
//                throw new Error("Cannot use keywords as variable name");
//            }
//            // Store type in variableTypes map instead of name.varType
//            if (KEYWORDS.contains(name)) {
//                throw new RuntimeException("Cannot use keyword '" + name.lexeme + "' as a variable name.");
//            }
            if (type != null) {
                variableTypes.put(name.lexeme, type);
                printDebug("DEBUG: Registered variable " + name.lexeme + " with type " + type);
            }

            // Handle the initializer
            Expr initializer = null;
            if (match(TokenType.ASSIGN)) {
                printDebug("DEBUG: Found assignment operator");
                if (check(TokenType.COMMA) || check(TokenType.EOF)) {
                    throw new Error("Missing value after '='");
                }
                initializer = expression();
                printDebug("DEBUG: Parsed initializer: " + initializer);

                // Perform type checking
                if (initializer instanceof Expr.Literal) {
                    Object value = ((Expr.Literal) initializer).value;

                    switch (type) {
                        case LETRA:
                            if (!(value instanceof Character)) {
                                throw new Error("LETRA can only be assigned a character");
                            }
                            break;
                        case NUMERO:
                        case TIPIK:
                            if (!(value instanceof Double)) {
                                throw new Error("NUMERO or TIPIK can only be assigned a number.");
                            }
                            break;
                        case TINUOD:
                            if (!(value instanceof String && (value.equals("OO") || value.equals("DILI")))) {
                                throw new Error("TINUOD must be 'OO' or 'DILI'.");
                            }
                            break;
                    }
                } else {
                    printDebug("WARNING: Initializer is not a literal; type-checking skipped.");
                }
            } else {
                // Initialize with default value based on type
                if (type == TokenType.NUMERO || type == TokenType.TIPIK) {
                    initializer = new Expr.Literal(0.0);
                    printDebug("DEBUG: Using default NUMERO/TIPIK value: 0.0");
                } else if (type == TokenType.TINUOD) {
                    initializer = new Expr.Literal("DILI");
                    printDebug("DEBUG: Using default TINUOD value: DILI");
                } else if (type == TokenType.LETRA) {
                    initializer = new Expr.Literal("");
                    printDebug("DEBUG: Using default LETRA value: ''");
                }
            }

            declarations.add(new Stmt.Var(name, initializer));
        } while (match(TokenType.COMMA));

        if (previous().type == TokenType.COMMA) {
            throw new Error("Trailing comma without a following variable");
        }

        if (declarations.size() == 1) {
            return declarations.get(0);
        }

        return new Stmt.Block(declarations);
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        return new Stmt.Expression(expr);
    }

    private Expr expression() {
        try {
            printDebug("DEBUG: Starting expression parsing at token: " + peek());
            Expr expr = assignment();
            printDebug("DEBUG: Successfully parsed expression: " + expr);
            return expr;
        } catch (RuntimeException e) {
            printDebug("DEBUG: Error in expression(): " + e.getMessage() + " at token: " + peek());
            throw e;
        }
    }

    private Expr assignment() {
        Expr expr = logical();
        printDebug("DEBUG: Logical expression: " + expr);
        while (match(TokenType.ASSIGN)) {
            Token equals = previous();
            printDebug("DEBUG: Parsing assignment with operator: " + equals);
            Expr value = assignment();
            printDebug("DEBUG: Assignment value: " + value);
            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }
            throw new RuntimeException("Invalid assignment target at token: " + peek());
        }
        return expr;
    }

    private Expr logical() {
        Expr expr = equality();
        printDebug("DEBUG: Logical expression: " + expr);
        while (match(TokenType.UG, TokenType.O)) {
            Token operator = previous();
            printDebug("DEBUG: Logical operator: " + operator);
            Expr right = equality();
            printDebug("DEBUG: Logical right: " + right);
            expr = new Expr.Binary(expr, operator, right);
        }
        printDebug("DEBUG: Logical returning: " + expr);
        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();
        printDebug("DEBUG: Equality expression: " + expr);
        while (match(TokenType.EQUAL, TokenType.NOT_EQUAL)) {
            Token operator = previous();
            printDebug("DEBUG: Equality operator: " + operator);
            Expr right = comparison();
            printDebug("DEBUG: Equality right: " + right);
            expr = new Expr.Binary(expr, operator, right);
        }
       printDebug("DEBUG Equality returning: " + expr);
        return expr;
    }

    private Expr comparison() {
       printDebug("DEBUG Entering comparison, current token: " + peek() + ", position: " + current);
        Expr expr = term();
       printDebug("DEBUG Comparison term: " + expr);
        while (match(TokenType.LESS, TokenType.LESS_EQUAL, TokenType.GREATER, TokenType.GREATER_EQUAL)) {
            Token operator = previous();
           printDebug("DEBUG Comparison operator: " + operator);
            Expr right = term();
           printDebug("DEBUG Comparison right: " + right);
            expr = new Expr.Binary(expr, operator, right);
        }
       printDebug("DEBUG Comparison returning: " + expr + ", next token: " + peek() + ", position: " + current);
        return expr;
    }

    private Expr term() {
       printDebug("DEBUG Entering term, current token: " + peek() + ", position: " + current);
        Expr expr = factor();
       printDebug("DEBUG Term factor: " + expr);
        while (match(TokenType.PLUS, TokenType.MINUS, TokenType.CONCAT)) {
            Token operator = previous();
           printDebug("DEBUG Term operator: " + operator);
            Expr right = factor();
           printDebug("DEBUG Term right: " + right);
            expr = new Expr.Binary(expr, operator, right);
        }
       printDebug("DEBUG Term returning: " + expr + ", next token: " + peek() + ", position: " + current);
        return expr;
    }

    private Expr factor() {
       printDebug("DEBUG Entering factor, current token: " + peek() + ", position: " + current);
        Expr expr = unary();
       printDebug("DEBUG Factor unary: " + expr);
        while (match(TokenType.MULTIPLY, TokenType.DIVIDE, TokenType.MODULO)) {
            Token operator = previous();
           printDebug("DEBUG Factor operator: " + operator);
            Expr right = unary();
           printDebug("DEBUG Factor right: " + right);
            expr = new Expr.Binary(expr, operator, right);
        }
       printDebug("DEBUG Factor returning: " + expr + ", next token: " + peek() + ", position: " + current);
        return expr;
    }

    private Expr unary() {
       printDebug("DEBUG Entering unary, current token: " + peek() + ", position: " + current);
        if (match(TokenType.MINUS, TokenType.DILI)) {
            Token operator = previous();
           printDebug("DEBUG Unary operator: " + operator);
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        Expr expr = primary();
       printDebug("DEBUG Unary returning: " + expr + ", next token: " + peek() + ", position: " + current);
        return expr;
    }

    private Expr primary() {
       printDebug("DEBUG Entering primary, current token: " + peek() + ", position: " + current);
        if (match(TokenType.TINUOD)) {
           printDebug("DEBUG Parsed TINUOD");
            return new Expr.Literal(true);
        }
        if (match(TokenType.NUMERO, TokenType.STRING, TokenType.CHAR, TokenType.TIPIK)) {
           printDebug("DEBUG Parsed literal: " + previous().literal + " at token: " + previous());
            return new Expr.Literal(previous().literal);
        }
        if (match(TokenType.NEWLINE)) {
           printDebug("DEBUG Parsed NEWLINE");
            return new Expr.Literal("\n");
        }
        if (match(TokenType.IDENTIFIER)) {
            Token name = previous();
           printDebug("DEBUG Parsed identifier: " + name.lexeme + " at token: " + name);
            if (check(TokenType.PLUS) && peekNext() != null && peekNext().type == TokenType.PLUS) {
                advance(); // Consume first PLUS
                advance(); // Consume second PLUS
               printDebug("DEBUG Parsed increment: " + name.lexeme + "++");
                return new Expr.Assign(name, new Expr.Binary(
                        new Expr.Variable(name),
                        new Token(TokenType.PLUS, "+", null, name.line),
                        new Expr.Literal(1.0)
                ));
            }
            return new Expr.Variable(name);
        }
        if (match(TokenType.LPAREN)) {
           printDebug("DEBUG Parsing grouped expression");
            Expr expr = expression();
            consume(TokenType.RPAREN, "Expect ')' after expression.");
           printDebug("DEBUG Parsed grouped expression: " + expr);
            return new Expr.Grouping(expr);
        }
       printDebug("DEBUG Unexpected token in primary: " + peek());
        throw new RuntimeException("Expect expression at token: " + peek().type + " lexeme: " + peek().lexeme);
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw new Error(message);
    }

    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON) return;
            switch (peek().type) {
                case SUGOD:
                case KATAPUSAN:
                case MUGNA:
                case IPAKITA:
                case KUNG:
                case PUNDOK:
                case ALANG_SA:
                    return;
            }
            advance();
        }
    }

    public interface Expr {
        <R> R accept(Visitor<R> visitor);

        interface Visitor<R> {
            R visitLiteralExpr(Literal expr);
            R visitGroupingExpr(Grouping expr);
            R visitUnaryExpr(Unary expr);
            R visitBinaryExpr(Binary expr);
            R visitVariableExpr(Variable expr);
            R visitAssignExpr(Assign expr);
        }

        class Literal implements Expr {
            public final Object value;

            public Literal(Object value) {
                this.value = value;
            }

            @Override
            public <R> R accept(Visitor<R> visitor) {
                return visitor.visitLiteralExpr(this);
            }
        }

        class Grouping implements Expr {
            public final Expr expression;

            public Grouping(Expr expression) {
                this.expression = expression;
            }

            @Override
            public <R> R accept(Visitor<R> visitor) {
                return visitor.visitGroupingExpr(this);
            }
        }

        class Unary implements Expr {
            public final Token operator;
            public final Expr right;

            public Unary(Token operator, Expr right) {
                this.operator = operator;
                this.right = right;
            }

            @Override
            public <R> R accept(Visitor<R> visitor) {
                return visitor.visitUnaryExpr(this);
            }
        }

        class Binary implements Expr {
            public final Expr left;
            public final Token operator;
            public final Expr right;

            public Binary(Expr left, Token operator, Expr right) {
                this.left = left;
                this.operator = operator;
                this.right = right;
            }

            @Override
            public <R> R accept(Visitor<R> visitor) {
                return visitor.visitBinaryExpr(this);
            }
        }

        class Variable implements Expr {
            public final Token name;

            public Variable(Token name) {
                this.name = name;
            }

            @Override
            public <R> R accept(Visitor<R> visitor) {
                return visitor.visitVariableExpr(this);
            }
        }

        class Assign implements Expr {
            public final Token name;
            public final Expr value;

            public Assign(Token name, Expr value) {
                this.name = name;
                this.value = value;
            }

            @Override
            public <R> R accept(Visitor<R> visitor) {
                return visitor.visitAssignExpr(this);
            }
        }
    }

    public interface Stmt {
        <R> R accept(Visitor<R> visitor);

        interface Visitor<R> {
            R visitExpressionStmt(Expression stmt);
            R visitPrintStmt(Print stmt);
            R visitVarStmt(Var stmt);
            R visitBlockStmt(Block stmt);
            R visitIfStmt(If stmt);
            R visitWhileStmt(While stmt);
            R visitInputStmt(Input stmt);
        }

        class Expression implements Stmt {
            public final Expr expression;

            public Expression(Expr expression) {
                this.expression = expression;
            }

            @Override
            public <R> R accept(Visitor<R> visitor) {
                return visitor.visitExpressionStmt(this);
            }
        }

        class Print implements Stmt {
            public final List<Expr> expressions;

            public Print(List<Expr> expressions) {
                this.expressions = expressions;
            }

            @Override
            public <R> R accept(Visitor<R> visitor) {
                return visitor.visitPrintStmt(this);
            }
        }

        class Var implements Stmt {
            public final Token name;
            public final Expr initializer;

            public Var(Token name, Expr initializer) {
                this.name = name;
                this.initializer = initializer;
            }

            @Override
            public <R> R accept(Visitor<R> visitor) {
                return visitor.visitVarStmt(this);
            }
        }

        class Block implements Stmt {
            public final List<Stmt> statements;

            public Block(List<Stmt> statements) {
                this.statements = statements;
            }

            @Override
            public <R> R accept(Visitor<R> visitor) {
                return visitor.visitBlockStmt(this);
            }
        }

        class If implements Stmt {
            public final Expr condition;
            public final Stmt thenBranch;
            public final Stmt elseBranch;

            public If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
                this.condition = condition;
                this.thenBranch = thenBranch;
                this.elseBranch = elseBranch;
            }

            @Override
            public <R> R accept(Visitor<R> visitor) {
                return visitor.visitIfStmt(this);
            }
        }

        class Input implements Stmt {
            public final List<Token> variables;

            public Input(List<Token> variables) {
                this.variables = variables;
            }

            @Override
            public <R> R accept(Visitor<R> visitor) {
                return visitor.visitInputStmt(this);
            }
        }

        class While implements Stmt {
            public final Expr condition;
            public final Stmt body;

            public While(Expr condition, Stmt body) {
                this.condition = condition;
                this.body = body;
            }

            @Override
            public <R> R accept(Visitor<R> visitor) {
                return visitor.visitWhileStmt(this);
            }
        }
    }
}