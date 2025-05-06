package Debuggers;

import Interpreter.Parser;

public class ParserTest {

    public interface Expr{
        <R> R accept(Parser.Expr.Visitor<R> visitor);

        interface Visitor<R> {
            R visitLiteralExpr(Parser.Expr.Literal expr);
            R visitGroupingExpr(Parser.Expr.Grouping expr);
            R visitUnaryExpr(Parser.Expr.Unary expr);
            R visitBinaryExpr(Parser.Expr.Binary expr);
            R visitVariableExpr(Parser.Expr.Variable expr);
            R visitAssignExpr(Parser.Expr.Assign expr);
        }
    }
}
