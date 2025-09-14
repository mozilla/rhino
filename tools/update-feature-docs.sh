#!/bin/bash

# Update Feature Documentation
# This script combines test262 and compat-table data for comprehensive documentation

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "📊 Updating Rhino Feature Documentation"
echo "========================================"
echo ""
echo "This enhanced version combines:"
echo "  • test262 specification compliance (3,025+ test suites)"
echo "  • compat-table practical testing (~200 features)"
echo ""

# Check if Node.js is available (replaces Python requirement)
if ! command -v node &> /dev/null; then
    echo "❌ Error: Node.js is required but not installed"
    exit 1
fi

# Check if test262.properties exists
if [ ! -f "$PROJECT_ROOT/tests/testsrc/test262.properties" ] && [ ! -f "$PROJECT_ROOT/testsrc/test262.properties" ]; then
    echo "❌ Error: test262.properties not found"
    echo "  Run './gradlew test' first to generate test results"
    exit 1
fi

# Run the JavaScript feature tracker (replaces Python version)
echo "🔍 Analyzing test262 results..."
echo "🌐 Setting up compat-table tests..."
node "$PROJECT_ROOT/tools/feature_tracker.js"

# Check if files were created
if [ -f "$PROJECT_ROOT/FEATURES.md" ] && [ -f "$PROJECT_ROOT/rhino-features.json" ] && [ -f "$PROJECT_ROOT/rhino-features.html" ]; then
    echo "✅ Feature documentation updated successfully!"
    echo ""
    echo "Files created/updated:"
    echo "  - FEATURES.md (human-readable documentation)"
    echo "  - rhino-features.json (machine-readable data)"
    echo "  - rhino-features.html (web-ready interactive page)"
    
    # Show summary
    echo ""
    echo "📈 Feature Support Summary:"
    python3 -c "
import json
with open('$PROJECT_ROOT/rhino-features.json', 'r') as f:
    data = json.load(f)
    total_features = 0
    total_tests = 0
    total_passed = 0
    for cat_data in data['categories'].values():
        total_features += cat_data['statistics']['total_features']
        total_tests += cat_data['statistics']['total_tests']
        total_passed += cat_data['statistics']['passed_tests']
    if total_tests > 0:
        pass_rate = (total_passed / total_tests * 100)
        print(f'   Total Categories: {len(data[\"categories\"])}')
        print(f'   Total Test Suites: {total_features:,}')
        print(f'   Total Tests: {total_tests:,}')
        print(f'   Tests Passed: {total_passed:,}')
        print(f'   Overall Pass Rate: {pass_rate:.1f}%')
"
else
    echo "❌ Error: Failed to generate documentation files"
    exit 1
fi

echo ""
echo "To commit these changes:"
echo "  git add FEATURES.md rhino-features.json"
echo "  git commit -m 'docs: Update feature documentation from test262 results'"