#!/usr/bin/env bash
set -e
echo "========================================================"
echo "  Building Minecraft Paper Plugin"
echo "========================================================"

if ! command -v mvn &> /dev/null; then
    echo "[ERROR] Maven (mvn) not found in PATH!"
    echo "Please install Maven and a compatible JDK."
    exit 1
fi

mvn clean package
echo "========================================================"
echo "  [SUCCESS] Build complete! Output in target/"
echo "========================================================"
