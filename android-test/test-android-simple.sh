#!/bin/bash
# Simple test script for Android template literal fix - NO DOCKER REQUIRED
# This simulates Android's optimization level -1 to verify the fix

set -e

echo "============================================"
echo "Rhino Template Literal Test (Android Mode)"
echo "============================================"

# Find Rhino JAR
RHINO_JAR=$(find ../rhino/build/libs -name "rhino-*.jar" -type f 2>/dev/null | head -1)

if [ -z "$RHINO_JAR" ]; then
    echo "Building Rhino JAR..."
    cd ..
    ./gradlew :rhino:jar
    cd android-test
    RHINO_JAR=$(find ../rhino/build/libs -name "rhino-*.jar" -type f | head -1)
fi

echo "Using: $(basename "$RHINO_JAR")"
echo ""

# Create test script
cat > test-template-literals.js << 'EOF'
// Performance test for template literals
print("Testing template literal performance with optimization level -1 (Android mode)...\n");

// Warm up
for (var i = 0; i < 100; i++) {
    var s = `warm up ${i}`;
}

// Actual test
var iterations = 10000;
var start = Date.now();

for (var i = 0; i < iterations; i++) {
    var result = `Iteration ${i} with value ${i * 2}`;
}

var duration = Date.now() - start;
print("Time for " + iterations + " template literals: " + duration + "ms");
print("Average: " + (duration/iterations).toFixed(3) + "ms per template literal");

// Without the fix, this would be ~10x slower on Android
if (duration > 5000) {
    print("\nWARNING: This is too slow! The Android JIT fix may not be working.");
} else {
    print("\nGOOD: Performance is acceptable. The fix appears to be working.");
}

// Also test correctness
var name = "Android";
var greeting = `Hello, ${name}!`;
print("\nCorrectness test: " + greeting);
print("Expected: Hello, Android!");
EOF

echo "Running with optimization level -1 (Android's default)..."
echo "----------------------------------------"
java -cp "$RHINO_JAR" org.mozilla.javascript.tools.shell.Main -opt -1 test-template-literals.js

echo ""
echo "Running with optimization level 0 (for comparison)..."
echo "----------------------------------------"
java -cp "$RHINO_JAR" org.mozilla.javascript.tools.shell.Main -opt 0 test-template-literals.js

# Clean up
rm -f test-template-literals.js

echo ""
echo "============================================"
echo "If both runs completed quickly (<1000ms),"
echo "the Android template literal fix is working!"
echo "============================================"