#!/usr/bin/env bash
# =============================================================================
# LSMS — Build and Run Script for Java Web Server & JDBC Module
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
LIB_DIR="$SCRIPT_DIR/lib"
SRC_DIR="$SCRIPT_DIR/src"
BIN_DIR="$SCRIPT_DIR/bin"

echo "==============================================================="
echo "   🧺 Compiling LSMS Java Backend & JDBC Module                "
echo "==============================================================="

mkdir -p "$BIN_DIR"
mkdir -p "$LIB_DIR"

# Check required libraries
MYSQL_JAR="$LIB_DIR/mysql-connector-j-8.3.0.jar"
GSON_JAR="$LIB_DIR/gson-2.10.1.jar"
JBCRYPT_JAR="$LIB_DIR/jbcrypt-0.4.jar"

if [ ! -f "$MYSQL_JAR" ]; then
    echo "⚠️ MySQL JDBC Connector JAR missing, downloading..."
    curl -sL -o "$MYSQL_JAR" https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar
fi
if [ ! -f "$GSON_JAR" ]; then
    echo "⚠️ Gson JAR missing, downloading..."
    curl -sL -o "$GSON_JAR" https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar
fi
if [ ! -f "$JBCRYPT_JAR" ]; then
    echo "⚠️ JBcrypt JAR missing, downloading..."
    curl -sL -o "$JBCRYPT_JAR" https://repo1.maven.org/maven2/org/mindrot/jbcrypt/0.4/jbcrypt-0.4.jar
fi

# Build classpath of all JARs
CP="$BIN_DIR:$LIB_DIR/*"

# Find all Java source files
JAVA_FILES=$(find "$SRC_DIR" -name "*.java")

# Compile
javac -cp "$LIB_DIR/*" -d "$BIN_DIR" $JAVA_FILES
echo "✅ Compilation successful! Output stored in $BIN_DIR"

# Run option
MODE="${1:-server}"

if [ "$MODE" == "test" ]; then
    echo -e "\n--- Running Automated JDBC Verification Test ---"
    java -cp "$CP" com.lsms.TestJDBC
elif [ "$MODE" == "console" ]; then
    echo -e "\n--- Launching Interactive CLI Console ---"
    java -cp "$CP" com.lsms.App
else
    echo -e "\n--- Starting LSMS Java Web Application Server ---"
    java -cp "$CP" com.lsms.server.HttpServerApp
fi
