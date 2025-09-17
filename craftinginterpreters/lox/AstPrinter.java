package com.craftinginterpreters.lox;

import java.util.List;

class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<Void> {
  void print(Expr expr) {
    System.out.println(expr.accept(this));
  }

  void print(List<Stmt> statements) {
    for (Stmt statement : statements) {
      Object value = statement.accept(this);
      if (value != null) {
        System.out.println(value);
      }
    }
  }

  @Override
  public String visitBinaryExpr(Expr.Binary expr) {
    return parenthesize(expr.operator.lexeme, expr.left, expr.right);
  }

  @Override
  public String visitGroupingExpr(Expr.Grouping expr) {
    return parenthesize("group", expr.expression);
  }

  @Override
  public String visitLiteralExpr(Expr.Literal expr) {
    if (expr.value == null) return "nil";
    return expr.value.toString();
  }

  @Override
  public String visitUnaryExpr(Expr.Unary expr) {
    return parenthesize(expr.operator.lexeme, expr.right);
  }

  @Override
  public String visitVariableExpr(Expr.Variable expr) {
    return expr.name.lexeme;
  }

  @Override
  public String visitAssignExpr(Expr.Assign expr) {
    return parenthesize("assign " + expr.name.lexeme, expr.value);
  }

  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    System.out.println(parenthesize("var " + stmt.name.lexeme, stmt.initializer));
    return null;
  }

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    System.out.println("{block");
    print(stmt.statements);
    System.out.println("}");
    return null;
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    System.out.println(parenthesize("expr", stmt.expression));
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    System.out.println(parenthesize("print", stmt.expression));
    return null;
  }

  private String parenthesize(String name, Expr... exprs) {
    StringBuilder builder = new StringBuilder();

    builder.append("(").append(name);
    for (Expr expr : exprs) {
      if (expr != null) {
        builder.append(" ");
        builder.append(expr.accept(this));
      }
    }
    builder.append(")");

    return builder.toString();
  }

  public static void main(String[] args) {
    Expr expression = new Expr.Binary(
      new Expr.Unary(
        new Token(TokenType.MINUS, "-", null, 1),
        new Expr.Literal(123)
      ),
      new Token(TokenType.STAR, "*", null, 1),
      new Expr.Grouping(
        new Expr.Literal(45.67)
      )
    );

    new AstPrinter().print(expression);
    // Expected: (* (- 123) (group 45.67))
  }
}
