package Interpreter;

import java.util.*;

public class Interpreter implements Parser.Expr.Visitor<Object>, Parser.Stmt.Visitor<Void> {
    private final Map<String, Object> globals = new HashMap<>();
    private Map<String, Object> environment = globals;
    private final Parser parser;

    public Interpreter(Parser parser) {
        this.parser = parser;
    }

    public void interpret(List<Parser.Stmt> statements) {
        try {
            System.out.println("Starting interpretation...");
            for (Parser.Stmt statement : statements) {
                execute(statement);
            }
            System.out.println("Interpretation complete");
        } catch (RuntimeException error) {
            throw error;
        }
    }

    @Override
    public Object visitLiteralExpr(Parser.Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitGroupingExpr(Parser.Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitUnaryExpr(Parser.Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right;
            case NOT:
                return !isTruthy(right);
        }

        return null;
    }

    @Override
    public Object visitBinaryExpr(Parser.Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        System.out.println("DEBUG: Binary operation " + expr.operator.type + " with left=" + left + " (" + left.getClass().getName() + ") right=" + right + " (" + right.getClass().getName() + ")");

        switch (expr.operator.type) {
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (Double)left + (Double)right;
                }
                if (left instanceof String || right instanceof String) {
                    return stringify(left) + stringify(right);
                }
                if (left instanceof Character && right instanceof Character) {
                    return String.valueOf(left) + String.valueOf(right);
                }
                throw new RuntimeException("Operands must be numbers, strings, or characters.");
            case CONCAT:
                return stringify(left) + stringify(right);
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (Double)left - (Double)right;
            case MULTIPLY:
                checkNumberOperands(expr.operator, left, right);
                return (Double)left * (Double)right;
            case DIVIDE:
                checkNumberOperands(expr.operator, left, right);
                if ((Double)right == 0) throw new RuntimeException("Division by zero.");
                return (Double)left / (Double)right;
            case MODULO:
                checkNumberOperands(expr.operator, left, right);
                if ((Double)right == 0) throw new RuntimeException("Modulo by zero.");
                return (Double)left % (Double)right;
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (Double)left > (Double)right ? "OO" : "DILI";
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (Double)left >= (Double)right ? "OO" : "DILI";
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (Double)left < (Double)right ? "OO" : "DILI";
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (Double)left <= (Double)right ? "OO" : "DILI";
            case EQUAL:
                if (left instanceof Character && right instanceof Character) {
                    return ((Character)left).equals((Character)right) ? "OO" : "DILI";
                }
                return isEqual(left, right) ? "OO" : "DILI";
            case NOT_EQUAL:
                if (left instanceof Character && right instanceof Character) {
                    return !((Character)left).equals((Character)right) ? "OO" : "DILI";
                }
                if ((left instanceof Double && right instanceof Character) || (left instanceof Character && right instanceof Double)) {
                    throw new RuntimeException("Cannot compare number with character.");
                }
                return !isEqual(left, right) ? "OO" : "DILI";
            case UG:
                return isTruthy(left) && isTruthy(right) ? "OO" : "DILI";
            case O:
                return isTruthy(left) || isTruthy(right) ? "OO" : "DILI";
        }
        return null;
    }

    @Override
    public Object visitVariableExpr(Parser.Expr.Variable expr) {
        Object value = lookUpVariable(expr.name);
        System.out.println("DEBUG: Reading variable " + expr.name.lexeme + " with value " + value);
        return value;
    }

    @Override
    public Object visitAssignExpr(Parser.Expr.Assign expr) {
        Object value = evaluate(expr.value);
        System.out.println("DEBUG: Assigning " + expr.name.lexeme + " = " + value);
        environment.put(expr.name.lexeme, value);
        return value;
    }

    @Override
    public Void visitExpressionStmt(Parser.Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Parser.Stmt.Print stmt) {
        StringBuilder output = new StringBuilder();
        for (Parser.Expr expr : stmt.expressions) {
            Object value = evaluate(expr);
            System.out.println("DEBUG: Printing value: " + value);
            output.append(stringify(value));
        }
        System.out.print(output.toString());
        return null;
    }

    @Override
    public Void visitVarStmt(Parser.Stmt.Var stmt) {
        System.out.println("DEBUG: Declaring variable " + stmt.name.lexeme);
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
            System.out.println("DEBUG: Initializing " + stmt.name.lexeme + " with value: " + value);
        } else {
            TokenType varType = parser.getVariableTypes().getOrDefault(stmt.name.lexeme, null);
            if (varType == TokenType.NUMERO) {
                value = 0.0;
                System.out.println("DEBUG: Using default NUMERO value: 0.0");
            } else if (varType == TokenType.TINUOD) {
                value = "DILI";
                System.out.println("DEBUG: Using default TINUOD value: DILI");
            } else if (varType == TokenType.LETRA) {
                value = "";
                System.out.println("DEBUG: Using default LETRA value: ''");
            }
        }
        environment.put(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitBlockStmt(Parser.Stmt.Block stmt) {
        System.out.println("DEBUG: Executing block with " + stmt.statements.size() + " statements");
        executeBlock(stmt.statements, environment);
        return null;
    }

    @Override
    public Void visitIfStmt(Parser.Stmt.If stmt) {
        Object condition = evaluate(stmt.condition);
        System.out.println("DEBUG: If condition evaluated to: " + condition);
        if (isTruthy(condition)) {
            System.out.println("DEBUG: Executing then branch");
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            System.out.println("DEBUG: Executing else branch");
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitWhileStmt(Parser.Stmt.While stmt) {
        System.out.println("DEBUG: Starting while loop");
        while (isTruthy(evaluate(stmt.condition))) {
            System.out.println("DEBUG: While condition is true, executing body");
            execute(stmt.body);
        }
        System.out.println("DEBUG: While loop finished");
        return null;
    }

    @Override
    public Void visitInputStmt(Parser.Stmt.Input stmt) {
        System.out.println("DEBUG: Processing input statement");
        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine();
        String[] values = input.split(",");

        if (values.length != stmt.variables.size()) {
            throw new RuntimeException("Expected " + stmt.variables.size() + " values, got " + values.length);
        }

        for (int i = 0; i < stmt.variables.size(); i++) {
            Token variable = stmt.variables.get(i);
            String value = values[i].trim();
            Object typedValue;

            try {
                TokenType varType = parser.getVariableTypes().getOrDefault(variable.lexeme, null);
                if (varType == TokenType.NUMERO) {
                    typedValue = Double.parseDouble(value);
                    System.out.println("DEBUG: Parsed NUMERO input as Double: " + typedValue);
                } else if (varType == TokenType.TINUOD) {
                    typedValue = value.equalsIgnoreCase("OO") ? "OO" : "DILI";
                    System.out.println("DEBUG: Parsed TINUOD input as String: " + typedValue);
                } else if (varType == TokenType.LETRA) {
                    if (value.length() == 1 && Character.isLetter(value.charAt(0))) {
                        typedValue = value.charAt(0);
                        System.out.println("DEBUG: Parsed LETRA input as Character: " + typedValue);
                    } else {
                        throw new RuntimeException("Invalid input for LETRA variable '" + variable.lexeme + "': '" + value + "' is not a letter.");
                    }
                } else {
                    typedValue = value;
                    System.out.println("DEBUG: Parsed input as String: " + typedValue);
                }
            } catch (NumberFormatException e) {
                TokenType varType = parser.getVariableTypes().getOrDefault(variable.lexeme, null);
                if (varType == TokenType.LETRA) {
                    if (value.length() == 1 && Character.isLetter(value.charAt(0))) {
                        typedValue = value.charAt(0);
                        System.out.println("DEBUG: Parsed LETRA input as Character (fallback): " + typedValue);
                    } else {
                        throw new RuntimeException("Invalid input for LETRA variable '" + variable.lexeme + "': '" + value + "' is not a letter.");
                    }
                } else {
                    throw new RuntimeException("Invalid input format for variable " + variable.lexeme + ": " + value);
                }
            }

            System.out.println("DEBUG: Assigning " + variable.lexeme + " = " + typedValue + " (" + typedValue.getClass().getName() + ")");
            environment.put(variable.lexeme, typedValue);
        }

        return null;
    }

    private void executeBlock(List<Parser.Stmt> statements, Map<String, Object> environment) {
        Map<String, Object> previous = this.environment;
        try {
            this.environment = environment;
            for (Parser.Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    private void execute(Parser.Stmt stmt) {
        stmt.accept(this);
    }

    private Object evaluate(Parser.Expr expr) {
        return expr.accept(this);
    }

    private Object lookUpVariable(Token name) {
        if (environment.containsKey(name.lexeme)) {
            return environment.get(name.lexeme);
        }
        throw new RuntimeException("Undefined variable '" + name.lexeme + "'.");
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof String) {
            return object.equals("OO");
        }
        if (object instanceof Double) {
            return (Double)object != 0.0;
        }
        if (object instanceof Character) {
            return true;
        }
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        if (a instanceof String && b instanceof String) {
            return ((String)a).equals((String)b);
        }
        if (a instanceof Double && b instanceof Double) {
            return ((Double)a).equals((Double)b);
        }
        if (a instanceof Character && b instanceof Character) {
            return ((Character)a).equals((Character)b);
        }
        return a.equals(b);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeException("Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeException("Operands must be numbers.");
    }

    private String stringify(Object object) {
        if (object == null) return "nil";
        if (object instanceof String) {
            if (object.equals("OO")) return "OO";
            if (object.equals("DILI")) return "DILI";
            return (String)object;
        }
        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }
        return object.toString();
    }
}