package com.craftinginterpreters.lox;

import java.util.Arrays;
import java.util.List;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
  private Environment environment = new Environment();
  private final double[] rainfall = {11.4, 0.0, 0.4, 0.0, 0.0, 2.0, 0.2, 0.2, 0.2, 0.0};

  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    return environment.get(expr.name);
  }

 @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    Object value = null;
    if (stmt.initializer != null) {
      value = evaluate(stmt.initializer);
    }

    environment.define(stmt.name.lexeme, value);
    return null;
  }

  @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    Object value = evaluate(expr.value);
    environment.assign(expr.name, value);
    return value;
  }

  @Override
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }

  void interpret(List<Stmt> statements) {
    try {
      for (Stmt statement : statements) {
        execute(statement);
      }
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
    }
  }

  private void execute(Stmt stmt) {
    stmt.accept(this);
  }

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    executeBlock(stmt.statements, new Environment(environment));
    return null;
  }

  void executeBlock(List<Stmt> statements, Environment environment) {
    Environment previous = this.environment;
    try {
      this.environment = environment;

      for (Stmt statement : statements) {
        execute(statement);
      }
    } finally {
      this.environment = previous;
    }
  }

  private String stringify(Object object) {
    if (object == null) return "nil";

    if (object instanceof Double) {
      String text = object.toString();
      if (text.endsWith(".0")) {
        text = text.substring(0, text.length() - 2);
      }
      return text;
    }

    if (object instanceof double[] aDoubles) {
      return Arrays.toString(aDoubles);
    }

    return object.toString();
  }

  @Override
  public Object visitGroupingExpr(Expr.Grouping expr) {
    return evaluate(expr.expression);
  }

  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    evaluate(stmt.expression);
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    Object value = evaluate(stmt.expression);
    System.out.println(stringify(value));
    return null;
  }

  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case MINUS:
        checkNumberOperand(expr.operator, right);
        return -(double)right;
      case BANG:
        return !isTruthy(right);
    }

    // Unreachable.
    return null;
  }

  private boolean isTruthy(Object object) {
    if (object == null) return false;
    if (object instanceof Boolean) return (boolean)object;
    return true;
  }

  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right); 

    switch (expr.operator.type) {
      case GREATER:
        checkNumberOperands(expr.operator, left, right);
        return (double)left > (double)right;
      case GREATER_EQUAL:
        checkNumberOperands(expr.operator, left, right);
        return (double)left >= (double)right;
      case LESS:
        checkNumberOperands(expr.operator, left, right);
        return (double)left < (double)right;
      case LESS_EQUAL:
         checkNumberOperands(expr.operator, left, right);
        return (double)left <= (double)right;
      case BANG_EQUAL: return !isEqual(left, right);
      case EQUAL_EQUAL: return isEqual(left, right);
      case MINUS:
        checkNumberOperands(expr.operator, left, right);
        return (double)left - (double)right;
      case LEFT_SHIFT:
        checkLeftShiftOperands(expr.operator, left, right);
        return sumScaledBaseDistributions((double [])left, (double [])right);
      case HASHTAG:
        checkHashTagOperands(expr.operator, left, right);
        return calcScaledBaseDistribution((double [])left, (double)right);
      case LOGICAL_AND:
        checkNumberOperands(expr.operator, left, right);
        return calcBaseDistribution((double)left, (double)right);
      case PLUS:
        if (left instanceof Double && right instanceof Double) {
          return (double)left + (double)right;
        } 

        if (left instanceof String && right instanceof String) {
          return (String)left + (String)right;
        }

        throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
      case SLASH:
        return (double)left / (double)right;
      case STAR:
        return (double)left * (double)right;
    }

    // Unreachable.
    return null;
  }

  private double[] sumScaledBaseDistributions(double[] baseFlowA, double[] baseFlowB) {
    double[] dailyFlow = new double[baseFlowA.length];

    for (int i = 0; i < baseFlowA.length; i++) {
      dailyFlow[i] = Math.round((baseFlowA[i] + baseFlowB[i]) * 100.0) / 100.0;
    }

    return dailyFlow;
  }

  private double[] calcScaledBaseDistribution(double[] baseFlow, double rainunit) {
      double[] scaledFlow = new double[baseFlow.length];

      for (int i = 0; i < baseFlow.length; i++) {
        scaledFlow[i] = baseFlow[i] * rainunit * rainfall[i];
        scaledFlow[i] = Math.round(scaledFlow[i] * 100.0) / 100.0;
      }

      return scaledFlow;
  }

  private double[] calcBaseDistribution(double peak, double tail) {
    int days = 10;
    double[] flow = new double[days];

    double sum = 0.0;
    for (int i = 0; i < days; i++) {
      // Gaussian-style distribution centered at peak, width = tail
      flow[i] = Math.exp(-Math.pow(i - peak, 2) / (2.0 * tail * tail));
      sum += flow[i];
    }

    for (int i = 0; i < days; i++) {
      flow[i] = Math.round((flow[i] / sum) * 100.0) / 100.0;
    }

    return flow;
  }

  private boolean isEqual(Object a, Object b) {
    if (a == null && b == null) return true;
    if (a == null) return false;

    return a.equals(b);
  }

  private void checkNumberOperand(Token operator, Object operand) {
    if (operand instanceof Double) return;
    throw new RuntimeError(operator, "Operand must be a number.");
  }

  private void checkNumberOperands(Token operator, Object left, Object right) {
    if (left instanceof Double && right instanceof Double) return;
    
    throw new RuntimeError(operator, "Operands must be numbers.");
  }


  private void checkHashTagOperands(Token operator, Object left, Object right) {
    if (left instanceof double[] && right instanceof Double) return;
    
    throw new RuntimeError(operator, "Operands must be a river flow (a^b) and a number, e.g. (a^b)@c.");
  }

  private void checkLeftShiftOperands(Token operator, Object left, Object right) {
    if (left instanceof double[] leftArr && right instanceof double[] rightArr) {
      if (leftArr.length == rightArr.length) return;
      throw new RuntimeError(operator, "River flow distributions must have the same length.");
    }

    throw new RuntimeError(operator, "Operands must be river flows (double[]), e.g. (a^b) << (c^d).");
  }
}
