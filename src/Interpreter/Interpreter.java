package Interpreter;

import java.util.*;

public class Interpreter implements Parser.Expr.Visitor<Object>, Parser.Stmt.Visitor<Void> {
    private final Map<String, Object> globals = new HashMap<>();
    private Map<String, Object> environment = globals;

    public void interpret(List<Parser.Stmt> statements) {
        try {
            for (Parser.Stmt statement : statements) {
                execute(statement);
            }
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

        System.out.println("DEBUG: Binary operation " + expr.operator.type + " with left=" + left + " right=" + right);

        switch (expr.operator.type) {
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }
                if (left instanceof String && right instanceof String) {
                    return (String)left + (String)right;
                }
                if (left instanceof Double && right instanceof Character) {
                    return (double)left + 1.0;
                }
                throw new RuntimeException("Operands must be two numbers or two strings.");
            case CONCAT:
                return stringify(left) + stringify(right);
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case MULTIPLY:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
            case DIVIDE:
                checkNumberOperands(expr.operator, left, right);
                if ((double)right == 0) throw new RuntimeException("Division by zero.");
                return (double)left / (double)right;
            case MODULO:
                checkNumberOperands(expr.operator, left, right);
                if ((double)right == 0) throw new RuntimeException("Modulo by zero.");
                return (double)left % (double)right;
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double)left > (double)right ? "OO" : "DILI";
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left >= (double)right ? "OO" : "DILI";
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left < (double)right ? "OO" : "DILI";
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left <= (double)right ? "OO" : "DILI";
            case EQUAL:
                if (left instanceof Character && right instanceof Character) {
                    return ((Character)left).equals((Character)right) ? "OO" : "DILI";
                }
                return isEqual(left, right) ? "OO" : "DILI";
            case NOT_EQUAL:
                if (left instanceof Character && right instanceof Character) {
                    return !((Character)left).equals((Character)right) ? "OO" : "DILI";
                }
                return !isEqual(left, right) ? "OO" : "DILI";
            case UG:
                return (isTruthy(left) && isTruthy(right)) ? "OO" : "DILI";
            case O:
                return (isTruthy(left) || isTruthy(right)) ? "OO" : "DILI";
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
            if (value instanceof String) {
                if (value.equals("#")) {
                    output.append("#");
                } else if (value.equals("\n")) {
                    output.append("\n");
                } else {
                    output.append(value);
                }
            } else {
                output.append(stringify(value));
            }
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
            // Set default value based on type
            switch (stmt.name.lexeme) {
                case "NUMERO":
                    value = 0.0;
                    System.out.println("DEBUG: Using default NUMERO value: 0.0");
                    break;
                case "TINUOD":
                    value = false;
                    System.out.println("DEBUG: Using default TINUOD value: false");
                    break;
                case "LETRA":
                    value = "";
                    System.out.println("DEBUG: Using default LETRA value: ''");
                    break;
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
        System.out.println("DEBUG: isTruthy(" + condition + ") = " + isTruthy(condition));
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

            // Try to parse as number first
            try {
                typedValue = Double.parseDouble(value);
            } catch (NumberFormatException e) {
                // If not a number, check if it's a character
                if (value.length() == 1) {
                    typedValue = value.charAt(0);
                } else {
                    // Otherwise treat as string
                    typedValue = value;
                }
            }

            System.out.println("DEBUG: Assigning " + variable.lexeme + " = " + typedValue);
            environment.put(variable.lexeme, typedValue);
        }

        return null;
    }

    private void executeBlock(List<Parser.Stmt> statements, Map<String, Object> environment) {
        for (Parser.Stmt statement : statements) {
            execute(statement);
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
        if (object == null) {
            System.out.println("DEBUG: isTruthy(null) = false");
            return false;
        }
        if (object instanceof Boolean) {
            boolean result = (Boolean)object;
            System.out.println("DEBUG: isTruthy(" + object + ") = " + result);
            return result;
        }
        if (object instanceof String) {
            String str = (String)object;
            boolean result = str.equals("OO");
            System.out.println("DEBUG: isTruthy(" + str + ") = " + result);
            return result;
        }
        if (object instanceof Double) {
            boolean result = (Double)object != 0.0;
            System.out.println("DEBUG: isTruthy(" + object + ") = " + result);
            return result;
        }
        if (object instanceof Character) {
            System.out.println("DEBUG: isTruthy(" + object + ") = true");
            return true;
        }
        System.out.println("DEBUG: isTruthy(" + object + ") = true");
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        if (a instanceof Boolean && b instanceof Boolean) {
            return ((Boolean)a).equals((Boolean)b);
        }
        if (a instanceof String && b instanceof String) {
            return ((String)a).equals((String)b);
        }
        if (a instanceof Double && b instanceof Double) {
            return ((Double)a).equals((Double)b);
        }
        if (a instanceof Character && b instanceof Character) {
            return ((Character)a).equals((Character)b);
        }
        return false;
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
        if (object instanceof Boolean) {
            return (Boolean)object ? "OO" : "DILI";
        }
        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }
        if (object instanceof Character) {
            return String.valueOf(object);
        }
        return object.toString();
    }
}