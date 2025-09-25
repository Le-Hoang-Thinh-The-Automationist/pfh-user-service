#!/bin/bash

#--------------------------------------------------------
# Print usage/help and exit
#--------------------------------------------------------
usage() {
  cat <<EOF
Usage: $0 -t <unit|int> -s <service> -f <functionality-path>

Options:
  -t, --type            TEST_TYPE     (required; e.g. "unit" or "int")
  -s, --service         SERVICE       (required; e.g. a service)
  -f, --functionality   FUNCTIONALITY (required; e.g. a functionality)
  -h, --help            Show this help message
EOF
  exit 1
}

CURRRENT_PATH=$(pwd)

# Load .env file if it exists
if [ -f .env ]; then
  export $(grep -v '^#' .env | xargs)
fi

# Default test type
TEST_TYPE="unit"
TEST_FUNCTIONALITY="**"
TEST_SERVICE=""

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
    -s|--service)
      if [[ -z "${2-}" || "${2:0:1}" == "-" ]]; then
        echo "Error: '$1' requires a non-empty argument."
        usage
      fi
      TEST_SERVICE="$2"
      shift 2
      ;;
    -f|--functionality)
      if [[ -z "${2-}" || "${2:0:1}" == "-" ]]; then
        echo "Error: '$1' requires a non-empty argument."
        usage
      fi
      TEST_FUNCTIONALITY="$2"
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

# 
echo "The current service under test is $TEST_SERVICE"

if [ -z $TEST_SERVICE ]; then
  echo "❌ The service must not be empty"
  exit 1
fi

cd "$CURRRENT_PATH/$TEST_SERVICE"

# Run testing
echo "Execute the test for $TEST_TYPE tests..."
exit_code=0
case "$TEST_TYPE" in
  unit)
    echo "✅ Begin execute unit tests.."
    mvn clean test -Dtest=**/component/$TEST_FUNCTIONALITY/*
    if [ $? -ne 0 ]; then
      echo "❌ Unit tests failed. Exiting..."
      exit_code=1
    fi
    ;;
  int)
    echo "✅ Begin execute integration tests.."
    mvn clean verify -Dtest=**/functionality/$TEST_FUNCTIONALITY/*
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

cd "$CURRRENT_PATH"

# Clean up after test
exit $exit_code
