# Makefile for Crafting Interpreters (Lox)
SRC_DIR = .
BIN_DIR = bin
MAIN_CLASS = com.craftinginterpreters.lox.Lox

# Find all java files under com/
SOURCES := $(shell find $(SRC_DIR) -name "*.java")
FILE ?=

.PHONY: compileLox runLox clean

compileLox: $(BIN_DIR)/$(MAIN_CLASS).class

$(BIN_DIR)/$(MAIN_CLASS).class: $(SOURCES)
	mkdir -p $(BIN_DIR)
	javac $(SOURCES) -d $(BIN_DIR)

runLox: compileLox
ifeq ($(FILE),)
	java -cp $(BIN_DIR) $(MAIN_CLASS)
else
	java -cp $(BIN_DIR) $(MAIN_CLASS) $(FILE)
endif

clean:
	rm -rf $(BIN_DIR)
