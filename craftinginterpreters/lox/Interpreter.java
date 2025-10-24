package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
  final Environment globals = new Environment();
  private Environment environment = globals;
  private final Map<Expr, Integer> locals = new HashMap<>();
  private final double[] rainfall = {11.4, 0.0, 0.4, 13.1, 8.0, 6.0, 2.2, 0.2, 0.2, 0.0};

  Interpreter() {
    globals.define("clock", new LoxCallable() {
      @Override
      public int arity() { return 0; }

      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        return (double)System.currentTimeMillis() / 1000.0;
      }

      @Override
      public String toString() { return "<native fn>"; }
    });

    globals.define("calcDamFlowOutWithThreshold", new LoxCallable() {
      @Override
      public int arity() { 
        return 2; // threshold and flowIn
      }

      /**
       * Calculates dam outflow based on a flow threshold
       * @flowIn double[10], represents the inflow to the dam for 10 days
       * @threshold double, represents the flow threshold used in the calculation
       * @return double[], representing the outflow from the dam over 10 days
       */
      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        double[] flowIn = (double[]) arguments.get(0);
        double threshold = (double) arguments.get(1);
        double[] flowOut = new double[flowIn.length];

        for (int day = 0; day < flowIn.length; day++) {
          if (flowIn[day] > threshold) {
            flowOut[day] = flowIn[day] * 0.75; // release 75% if above threshold
          } else {
            flowOut[day] = flowIn[day]; // release full flow
          }
        }

        return flowOut;
      }

      @Override
      public String toString() { return "<native fn>"; }
    });

    globals.define("calcDamFlowOutBasedOnRainfall", new LoxCallable() {

      @Override
      public int arity() { 
        return 1; // flowIn
      }

    /**
     * Calculates dam outflow based on fixed rainfall data
     * @flowIn double[10], represents the inflow to the dam for 10 days
     * @return double[10], representing the outflow from the dam over 10 days
     */
      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        final double RAIN_THRESHOLD = 10.0; // mm
        double[] flowIn = (double[]) arguments.get(0);
        double[] flowOut = new double[flowIn.length];

        for (int day = 0; day < flowIn.length; day++) {
          if (rainfall[day] > RAIN_THRESHOLD) {
            flowOut[day] = flowIn[day] * 0.75; // release 75% if above rain threshold
          } else {
            flowOut[day] = flowIn[day]; // release full flow
          }
        }

        return flowOut;
      }

      @Override
      public String toString() { return "<native fn>"; }
    });

    globals.define("calcDamFlowBlockHalf", new LoxCallable() {

      @Override
      public int arity() { 
        return 1; // flowIn
      }

    /**
     * Calculates dam outflow by halving the inflow
     * @flowIn double[10], represents the inflow to the dam for 10 days
     * @return double[10], representing the outflow from the dam over 10 days
     */
      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        double[] flowIn = (double[]) arguments.get(0);
        double[] flowOut = new double[flowIn.length];

        for (int day = 0; day < flowIn.length; day++) {
          flowOut[day] = flowIn[day] / 2.0; // block half the flow
        }

        return flowOut;
      }

      @Override
      public String toString() { return "<native fn>"; }
    });
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    Object value = null;
    if (stmt.value != null) value = evaluate(stmt.value);

    throw new Return(value);
  }

  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    LoxFunction function = new LoxFunction(stmt, environment);
    environment.define(stmt.name.lexeme, function);
    return null;
  }

  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    if (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.thenBranch);
    } else if (stmt.elseBranch != null) {
      execute(stmt.elseBranch);
    }
    return null;
  }

  @Override
  public Object visitCallExpr(Expr.Call expr) {
    Object callee = evaluate(expr.callee);

    List<Object> arguments = new ArrayList<>();

    for (Expr argument : expr.arguments) { 
      arguments.add(evaluate(argument));
    }

    if (!(callee instanceof LoxCallable)) {
      throw new RuntimeError(expr.paren,
          "Can only call functions and classes.");
    }

    LoxCallable function = (LoxCallable)callee;

    if (arguments.size() != function.arity()) {
      throw new RuntimeError(expr.paren, "Expected " +
          function.arity() + " arguments but got " +
          arguments.size() + ".");
    }

    return function.call(this, arguments);
  }

  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    return lookUpVariable(expr.name, expr);
  }

  private Object lookUpVariable(Token name, Expr expr) {
    Integer distance = locals.get(expr);
    if (distance != null) {
      return environment.getAt(distance, name.lexeme);
    } else {
      return globals.get(name);
    }
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
  public Void visitWhileStmt(Stmt.While stmt) {
    while (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.body);
    }
    return null;
  }

  @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    Object value = evaluate(expr.value);

    Integer distance = locals.get(expr);
    if (distance != null) {
      environment.assignAt(distance, expr.name, value);
    } else {
      globals.assign(expr.name, value);
    }

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

  @Override
  public Object visitLogicalExpr(Expr.Logical expr) {
    Object left = evaluate(expr.left);

    if (expr.operator.type == TokenType.OR) {
      if (isTruthy(left)) return left;
    } else {
      if (!isTruthy(left)) return left;
    }

    return evaluate(expr.right);
  }

  private void execute(Stmt stmt) {
    stmt.accept(this);
  }

  void resolve(Expr expr, int depth) {
    locals.put(expr, depth);
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
