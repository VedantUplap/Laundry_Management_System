#!/usr/bin/env bash
# =============================================================================
# LSMS — Laundry Service Management System
# One-Click Root Launcher for Java + JDBC Web Application Server
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAVA_DIR="$SCRIPT_DIR/java-jdbc"

echo "==============================================================="
echo "   🧺 Starting LSMS Web Application (Java + JDBC Backend)     "
echo "==============================================================="

cd "$JAVA_DIR"
./build_and_run.sh server
