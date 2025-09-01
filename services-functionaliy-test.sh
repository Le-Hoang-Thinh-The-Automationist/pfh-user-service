#!/bin/bash

#--------------------------------------------------------
# Print usage/help and exit
#--------------------------------------------------------
usage() {
  cat <<EOF
Usage: $0 -t <unit|int> -s <scope-value>

Options:
  -t, --type   TEST_TYPE (required; e.g. "unit" or "int")
  -s, --scope  SCOPE    (required; e.g. a module or feature name)
  -h, --help   Show this help message
EOF
  exit 1
}


# Load .env file if it exists
if [ -f .env ]; then
  export $(grep -v '^#' .env | xargs)
fi

# Assign default values if not already set
export APP_NAME=${APP_NAME:localhost}
export APP_PORT=${APP_PORT:-8080}
export DB_HOST=${DB_HOST:-localhost}
export DB_PORT=${DB_PORT:-5432}
export DB_NAME=${DB_NAME:-mydatabase}
export DB_USERNAME=${DB_USERNAME:-myuser}
export DB_PASSWORD=${DB_PASSWORD:-mypassword}


# Default test type
TEST_TYPE="unit"
TEST_SCOPE="**"

#--------------------------------------------------------
# Parse arguments
#--------------------------------------------------------
while [[ $# -gt 0 ]]; do
  case "$1" in
    -t|--type)
      if [[ -z "${2-}" || "${2:0:1}" == "-" ]]; then
        echo "Error: '$1' requires a non-empty argument."
        usage
      fi
      TEST_TYPE="$2"
      shift 2
      ;;
    -s|--scope)
      if [[ -z "${2-}" || "${2:0:1}" == "-" ]]; then
        echo "Error: '$1' requires a non-empty argument."
        usage
      fi
      SCOPE="$2"
      shift 2
      ;;
    -h|--help)
      usage
      ;;
    *)
      echo "❌ Unknown option: $1"
      usage
      ;;
  esac
done

# Setup environment and run tests
echo "🔧 Setting up environment for $TEST_TYPE tests..."

# Run testing
echo "Execute the test for $TEST_TYPE tests..."
exit_code=0
case "$TEST_TYPE" in
  unit)
    echo "✅ Begin execute unit tests.."
    mvn clean test -Dtest=**/component/$TEST_SCOPE/*
    if [ $? -ne 0 ]; then
      echo "❌ Unit tests failed. Exiting..."
      exit_code=1
    fi
    ;;
  int)
    echo "✅ Begin execute integration tests.."
    mvn clean verify -Dtest=**/functionality/$TEST_SCOPE/*
    if [ $? -ne 0 ]; then
      echo "❌ Integration tests failed. Exiting..."
      exit_code=1
    fi
    ;;
  *)
    echo "❌ Invalid test type: $TEST_TYPE"
    echo "Please use 'unit' or 'integration'"
      exit_code=1
    ;;
esac

# Clean up after test
exit $exit_code
