#!/bin/bash
# Local Android testing script for Rhino template literal fix
# This script sets up a Docker-based Android emulator to test the JIT compilation issue

set -e

echo "============================================"
echo "Rhino Android Template Literal Testing Script"
echo "============================================"

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "Error: Docker is not installed. Please install Docker first."
    exit 1
fi

# Build Rhino JAR if not already built
if [ ! -f "../rhino/build/libs/rhino-1.7.16-SNAPSHOT.jar" ]; then
    echo "Building Rhino JAR..."
    cd ..
    ./gradlew :rhino:jar
    cd android-test
fi

# Create Dockerfile for Android testing environment
cat > Dockerfile << 'EOF'
FROM openjdk:11-jdk-slim

# Install necessary packages
RUN apt-get update && apt-get install -y \
    wget \
    unzip \
    git \
    && rm -rf /var/lib/apt/lists/*

# Install Android SDK
ENV ANDROID_SDK_ROOT /opt/android-sdk
ENV PATH ${PATH}:${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools

RUN mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools && \
    cd ${ANDROID_SDK_ROOT}/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip && \
    unzip -q commandlinetools-linux-9477386_latest.zip && \
    mv cmdline-tools latest && \
    rm commandlinetools-linux-9477386_latest.zip

# Accept licenses
RUN yes | sdkmanager --licenses || true

# Install Android SDK components
RUN sdkmanager "platform-tools" "platforms;android-33" "build-tools;33.0.2"

# Create app directory
WORKDIR /app

# Copy test files
COPY ../rhino/build/libs/rhino-*.jar ./rhino.jar
COPY template-literal-benchmark.js ./
COPY verify-issue-1365.js ./
COPY JitVerificationHelper.java ./

# Compile JIT verification helper
RUN javac JitVerificationHelper.java

CMD ["java", "-cp", ".:rhino.jar", "JitVerificationHelper"]
EOF

# Create verification test script
cat > verify-android-fix.js << 'EOF'
// Verify that template literals work correctly on Android
print("Starting Android template literal verification...");

// Test 1: Basic template literal
var name = "Android";
var greeting = `Hello, ${name}!`;
print("Test 1 - Basic: " + greeting);

// Test 2: Multiple expressions
var x = 5, y = 10;
var result = `${x} + ${y} = ${x + y}`;
print("Test 2 - Multiple expressions: " + result);

// Test 3: Performance test (this would be slow without the fix)
var start = Date.now();
for (var i = 0; i < 1000; i++) {
    var s = `Iteration ${i}`;
}
var duration = Date.now() - start;
print("Test 3 - Performance: " + duration + "ms for 1000 iterations");

// Test 4: Tagged template
function myTag(strings, ...values) {
    return strings[0] + values[0] + strings[1];
}
var tagged = myTag`Count: ${42}!`;
print("Test 4 - Tagged: " + tagged);

print("\nAll tests completed successfully!");
print("If this runs quickly (~100ms), the Android JIT fix is working.");
EOF

echo "Building Docker image..."
docker build -t rhino-android-test .

echo "\nRunning Android tests..."
docker run --rm rhino-android-test

echo "\n============================================"
echo "To test on a real Android device:"
echo "1. Copy rhino.jar to your Android project"
echo "2. Run the verify-android-fix.js script"
echo "3. Check logcat for JIT compilation messages"
echo "============================================"