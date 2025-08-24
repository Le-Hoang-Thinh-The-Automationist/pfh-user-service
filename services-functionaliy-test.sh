#!/bin/bash

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

# Parse arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    -t|--type)
      TEST_TYPE="$2"
      shift 2
      ;;
    *)
      echo "❌ Unknown option: $1"
      echo "Usage: ./services-functionaliy-test.sh [-t unit|int]"
      exit 1
      ;;
  esac
done

# Setup environment and run tests
echo "🔧 Setting up environment for $TEST_TYPE tests..."
docker compose up -d postgre_db

# Run testing
echo "Execute the test for $TEST_TYPE tests..."
case "$TEST_TYPE" in
  unit)
    echo "✅ Begin execute unit tests.."
    mvn clean test -B -Dtest=**/component/**/*
    ;;
  int)
    echo "✅ Begin execute integration tests.."
    mvn clean verify -B -Dtest=**/functionality/**/*
    ;;
  *)
    echo "❌ Invalid test type: $TEST_TYPE"
    echo "Please use 'unit' or 'integration'"
    exit 1
    ;;
esac

# Clean up after test
docker compose down
