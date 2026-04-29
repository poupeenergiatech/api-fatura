#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

export JAVA_HOME="$SCRIPT_DIR/.tools/jdk-17.0.2"
export M2_HOME="$SCRIPT_DIR/.tools/apache-maven-3.9.6"
export PATH="$JAVA_HOME/bin:$M2_HOME/bin:$PATH"

JAR="$SCRIPT_DIR/target/leitor-fatura-energia-1.0.0.jar"

if [ ! -f "$JAR" ]; then
  echo "Compilando projeto..."
  (cd "$SCRIPT_DIR" && mvn package -q)
  echo "Compilação concluída."
fi

if [ $# -lt 1 ]; then
  echo "Uso:"
  echo "  Servidor:  ./run.sh --server [porta]          (padrão: 8080)"
  echo "  CLI:       ./run.sh <fatura.pdf> [saida.json]"
  echo "  Dump:      ./run.sh <fatura.pdf> --dump"
  exit 1
fi

java -jar "$JAR" "$@"
