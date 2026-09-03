#!/usr/bin/env bash
# =============================================================================
# LSMS — Build and Run Script for Java JDBC Module
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIB_DIR="$SCRIPT_DIR/lib"
SRC_DIR="$SCRIPT_DIR/src"
BIN_DIR="$SCRIPT_DIR/bin"
JAR_FILE="$LIB_DIR/mysql-connector-j-8.3.0.jar"

echo "==============================================================="
echo "   🧺 Compiling LSMS Java JDBC Module                          "
echo "==============================================================="

mkdir -p "$BIN_DIR"

# Check if JAR exists
if [ ! -f "$JAR_FILE" ]; then
    echo "⚠️ MySQL JDBC Connector JAR not found in $LIB_DIR, downloading..."
    mkdir -p "$LIB_DIR"
    curl -L -o "$JAR_FILE" https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar
fi

# Find all Java source files
JAVA_FILES=$(find "$SRC_DIR" -name "*.java")

# Compile
javac -cp "$JAR_FILE" -d "$BIN_DIR" $JAVA_FILES
echo "✅ Compilation successful! Compiled classes saved to $BIN_DIR"

# Run option
if [ "$1" == "test" ]; then
    echo -e "\n--- Running Automated JDBC Verification Test ---"
    java -cp "$BIN_DIR:$JAR_FILE" com.lsms.TestJDBC
else
    echo -e "\n--- Launching Interactive JDBC Console Application ---"
    java -cp "$BIN_DIR:$JAR_FILE" com.lsms.App
fi
