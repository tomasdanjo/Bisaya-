package Interpreter;

import java.util.*;

public class Parser {
    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            try {
                Stmt stmt = declaration();
                if (stmt != null) {
                    statements.add(stmt);
                }
            } catch (RuntimeException error) {
                System.out.println("DEBUG: Error parsing statement: " + error.getMessage());
                synchronize();
            }
        }
        System.out.println("DEBUG: Parsed " + statements.size() + " statements");
        return statements;
    }

    private Stmt declaration() {
        try {
            if (match(TokenType.SUGOD)) return block();
            if (match(TokenType.MUGNA)) return varDeclaration();
            return statement();
        } catch (RuntimeException error) {
            System.out.println("DEBUG: Error in declaration: " + error.getMessage());
            synchronize();
            return null;
        }
    }

    private Stmt statement() {
        if (match(TokenType.IPAKITA)) return printStatement();
        if (match(TokenType.DAWAT)) return inputStatement();
        if (match(TokenType.KUNG)) return ifStatement();
        if (match(TokenType.PUNDOK)) {
            System.out.println("DEBUG: Found PUNDOK");
            consume(TokenType.LBRACE, "Expect '{' after PUNDOK.");
            List<Stmt> statements = new ArrayList<>();
            while (!check(TokenType.RBRACE) && !isAtEnd()) {
                Stmt stmt = statement();
                if (stmt != null) {
                    statements.add(stmt);
                    System.out.println("DEBUG: Added statement to block: " + stmt);
                }
            }
            consume(TokenType.RBRACE, "Expect '}' after block.");
            return new Stmt.Block(statements);
        }
        if (match(TokenType.ALANG_SA)) {
            System.out.println("DEBUG: Found ALANG SA statement");
            return whileStatement();
        }
        return expressionStatement();
    }

    private Stmt printStatement() {
        System.out.println("DEBUG: Parsing print statement");
        consume(TokenType.COLON, "Expect ':' after IPAKITA.");
        List<Expr> expressions = new ArrayList<>();
        do {
            Expr expr = expression();
            expressions.add(expr);
            System.out.println("DEBUG: Added expression to print: " + expr);
        } while (match(TokenType.COMMA));
        System.out.println("DEBUG: Print statement parsed with " + expressions.size() + " expressions");
        return new Stmt.Print(expressions);
    }

    private Stmt inputStatement() {
        System.out.println("DEBUG: Parsing input statement");
        consume(TokenType.COLON, "Expect ':' after DAWAT.");
        List<Token> variables = new ArrayList<>();
        do {
            Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");
            variables.add(name);
            System.out.println("DEBUG: Added variable to input: " + name.lexeme);
        } while (match(TokenType.COMMA));
        System.out.println("DEBUG: Input statement parsed with " + variables.size() + " variables");
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
        System.out.println("DEBUG: Parsing block");
        List<Stmt> statements = new ArrayList<>();
        while (!check(TokenType.KATAPUSAN) && !isAtEnd()) {
            Stmt stmt = declaration();
            if (stmt != null) {
                statements.add(stmt);
                System.out.println("DEBUG: Added statement to block: " + stmt);
            }
        }
        consume(TokenType.KATAPUSAN, "Expect 'KATAPUSAN' after block.");
        System.out.println("DEBUG: Block parsed with " + statements.size() + " statements");
        return new Stmt.Block(statements);
    }
    
    private Stmt whileStatement() {
        System.out.println("DEBUG: Starting while statement parsing");
        consume(TokenType.LPAREN, "Expect '(' after 'ALANG SA'.");
        System.out.println("DEBUG: Found opening parenthesis");

        // Parse initialization
        Stmt initializer;
        if (match(TokenType.IDENTIFIER)) {
            Token name = previous();
            System.out.println("DEBUG: Found identifier: " + name.lexeme);
            if (match(TokenType.ASSIGN)) {
                System.out.println("DEBUG: Found assignment operator");
                Expr value = expression();
                System.out.println("DEBUG: Parsed initializer value: " + value);
                initializer = new Stmt.Expression(new Expr.Assign(name, value));
            } else {
                throw new RuntimeException("Expect '=' after variable name.");
            }
        } else {
            initializer = expressionStatement();
        }
        System.out.println("DEBUG: Parsed initializer: " + initializer);

        // Parse condition
        System.out.println("DEBUG: Looking for comma");
        consume(TokenType.COMMA, "Expect ',' after initialization.");
        System.out.println("DEBUG: Found comma, parsing condition");
        Expr condition = expression();
        System.out.println("DEBUG: Parsed condition: " + condition);

        // Parse increment
        System.out.println("DEBUG: Looking for second comma");
        consume(TokenType.COMMA, "Expect ',' after condition.");
        System.out.println("DEBUG: Found second comma, parsing increment");
        Expr increment = null;
        if (match(TokenType.IDENTIFIER)) {
            Token name = previous();
            System.out.println("DEBUG: Found identifier for increment: " + name.lexeme);
            if (match(TokenType.PLUS, TokenType.PLUS)) {
                System.out.println("DEBUG: Found ++ operator");
                increment = new Expr.Assign(name, new Expr.Binary(
                        new Expr.Variable(name),
                        new Token(TokenType.PLUS, "+", null, name.line),
                        new Expr.Literal(1.0)
                ));
                System.out.println("DEBUG: Created increment expression: " + name.lexeme + "++");
            }
        }
        if (increment == null) {
            throw new RuntimeException("Expect increment after comma.");
        }
        System.out.println("DEBUG: Looking for closing parenthesis");
        consume(TokenType.RPAREN, "Expect ')' after for clauses.");
        System.out.println("DEBUG: Found closing parenthesis");

        // Parse body
        System.out.println("DEBUG: Parsing loop body");
        Stmt body;
        if (match(TokenType.PUNDOK)) {
            System.out.println("DEBUG: Found PUNDOK");
            consume(TokenType.LBRACE, "Expect '{' after PUNDOK.");
            System.out.println("DEBUG: Found opening brace");
            List<Stmt> statements = new ArrayList<>();
            while (!check(TokenType.RBRACE) && !isAtEnd()) {
                Stmt stmt = statement();
                if (stmt != null) {
                    statements.add(stmt);
                    System.out.println("DEBUG: Added statement to block: " + stmt);
                }
            }
            consume(TokenType.RBRACE, "Expect '}' after block.");
            System.out.println("DEBUG: Found closing brace");
            body = new Stmt.Block(statements);
        } else {
            body = statement();
        }
        System.out.println("DEBUG: Parsed body: " + body);

        // Create a block that contains the increment after the body
        List<Stmt> bodyStatements = new ArrayList<>();
        bodyStatements.add(body);
        bodyStatements.add(new Stmt.Expression(increment));
        Stmt bodyWithIncrement = new Stmt.Block(bodyStatements);
        System.out.println("DEBUG: Created body with increment");

        // Create the while loop
        Stmt whileLoop = new Stmt.While(condition, bodyWithIncrement);
        System.out.println("DEBUG: Created while loop");

        // Create a block that contains the initializer and the while loop
        List<Stmt> statements = new ArrayList<>();
        statements.add(initializer);
        statements.add(whileLoop);
        System.out.println("DEBUG: Created final block with initializer and while loop");
        return new Stmt.Block(statements);
    }

    private Stmt varDeclaration() {
        System.out.println("DEBUG: Parsing variable declaration");
        TokenType type = null;
        if (match(TokenType.NUMERO, TokenType.TINUOD, TokenType.LETRA)) {
            type = previous().type;
            System.out.println("DEBUG: Found type declaration: " + type);
        }

        List<Stmt> declarations = new ArrayList<>();
        do {
            Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");
            System.out.println("DEBUG: Found variable name: " + name.lexeme);
            name.varType = type;  // Store the type for later use

            // Handle the initializer
            Expr initializer = null;
            if (match(TokenType.ASSIGN)) {
                System.out.println("DEBUG: Found assignment operator");
                initializer = expression();
                System.out.println("DEBUG: Parsed initializer: " + initializer);
            } else {
                // Initialize with default value based on type
                if (type == TokenType.NUMERO) {
                    initializer = new Expr.Literal(0.0);
                    System.out.println("DEBUG: Using default NUMERO value: 0.0");
                } else if (type == TokenType.TINUOD) {
                    initializer = new Expr.Literal("DILI");
                    System.out.println("DEBUG: Using default TINUOD value: DILI");
                } else if (type == TokenType.LETRA) {
                    initializer = new Expr.Literal('\0');
                    System.out.println("DEBUG: Using default LETRA value: \\0");
                }
            }

            declarations.add(new Stmt.Var(name, initializer));
        } while (match(TokenType.COMMA));

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
        return assignment();
    }

    private Expr assignment() {
        Expr expr = logical();
        if (match(TokenType.ASSIGN)) {
            Token equals = previous();
            Expr value = assignment();
            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }
            throw new RuntimeException("Invalid assignment target.");
        }
        return expr;
    }

    private Expr logical() {
        Expr expr = equality();
        while (match(TokenType.UG, TokenType.O)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();
        while (match(TokenType.EQUAL, TokenType.NOT_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr comparison() {
        Expr expr = term();
        while (match(TokenType.LESS, TokenType.LESS_EQUAL,
                TokenType.GREATER, TokenType.GREATER_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr term() {
        Expr expr = factor();
        while (match(TokenType.PLUS, TokenType.MINUS, TokenType.CONCAT)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr factor() {
        Expr expr = unary();
        while (match(TokenType.MULTIPLY, TokenType.DIVIDE, TokenType.MODULO)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr unary() {
        if (match(TokenType.MINUS, TokenType.NOT)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return primary();
    }

    private Expr primary() {
        if (match(TokenType.TINUOD)) return new Expr.Literal(true);
        if (match(TokenType.TIPIK)) return new Expr.Literal(false);
        if (match(TokenType.NUMBER, TokenType.STRING, TokenType.CHAR)) {
            return new Expr.Literal(previous().literal);
        }
        if (match(TokenType.NEWLINE)) {
            return new Expr.Literal("\n");
        }
        if (match(TokenType.IDENTIFIER)) {
            return new Expr.Variable(previous());
        }
        if (match(TokenType.LPAREN)) {
            Expr expr = expression();
            consume(TokenType.RPAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }
        throw new RuntimeException("Expect expression.");
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
        throw new RuntimeException(message);
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
    }
}