package com.craftinginterpreters.lox;

class Token {
  // Fix set of possible token types (reserved words/chars/symbols?)
  final TokenType type;
  // Raw string value found in script
  final String lexeme;
  // Living runtime object that will be used by the interpreter later
  final int line; 

  final Object literal;
  Token(TokenType type, String lexeme, Object literal, int line) {
    this.type = type;
    this.lexeme = lexeme;
    this.literal = literal;
    this.line = line;
  }

  public String toString() {
    return type + " " + lexeme + " " + literal;
  }
}
