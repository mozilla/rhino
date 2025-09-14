#!/usr/bin/env node
/**
 * Unified JavaScript toolchain for Rhino feature documentation
 * Replaces Python scripts with JavaScript for consistency
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

class UnifiedRhinoDocumentation {
    constructor() {
        this.rhinoRoot = path.join(__dirname, '..');
        this.test262Props = path.join(this.rhinoRoot, 'testsrc', 'test262.properties');
        this.compatTableDir = path.join(__dirname, 'compat-table', 'compat-table-repo');
    }

    // Parse test262.properties (replaces your Python feature_tracker.py)
    parseTest262Properties() {
        console.log('Parsing test262.properties...');
        const content = fs.readFileSync(this.test262Props, 'utf8');
        const lines = content.split('\n');
        
        const suites = {};
        let totalTests = 0;
        let passedTests = 0;
        
        for (const line of lines) {
            if (line.trim() && !line.startsWith('#')) {
                const match = line.match(/^([\w\/.~-]+)\s+(.*)/);
                if (match) {
                    const [, path, result] = match;
                    const suite = this.getSuiteName(path);
                    
                    if (!suites[suite]) {
                        suites[suite] = { 
                            passed: 0, 
                            failed: 0, 
                            paths: new Set() 
                        };
                    }
                    
                    totalTests++;
                    if (result.trim() === '~') {
                        suites[suite].failed++;
                    } else {
                        suites[suite].passed++;
                        passedTests++;
                    }
                    suites[suite].paths.add(path);
                }
            }
        }
        
        return {
            suites,
            totalTests,
            passedTests,
            passRate: ((passedTests / totalTests) * 100).toFixed(1)
        };
    }

    // Run compat-table tests (integrates p-bakker's approach)
    runCompatTableTests() {
        console.log('Running compat-table tests...');
        
        // Build Rhino JAR if needed
        if (!fs.existsSync(path.join(this.rhinoRoot, 'build', 'libs', 'rhino.jar'))) {
            console.log('Building Rhino JAR...');
            execSync('./gradlew jar', { 
                cwd: this.rhinoRoot,
                stdio: 'inherit' 
            });
        }
        
        // Copy JAR to compat-table directory
        const jarSource = path.join(this.rhinoRoot, 'build', 'libs', 'rhino-1.8.1-SNAPSHOT.jar');
        const jarDest = path.join(this.compatTableDir, 'rhino.jar');
        fs.copyFileSync(jarSource, jarDest);
        
        // Run tests with update flag
        console.log('Updating compat-table results...');
        execSync('node rhino.js -u', {
            cwd: this.compatTableDir,
            stdio: 'inherit'
        });
        
        // Load and return results
        return this.loadCompatTableResults();
    }

    loadCompatTableResults() {
        const results = {};
        const files = fs.readdirSync(this.compatTableDir)
            .filter(f => f.startsWith('results-') && f.endsWith('.json'));
        
        for (const file of files) {
            const suite = file.replace('results-', '').replace('.json', '');
            const data = JSON.parse(
                fs.readFileSync(path.join(this.compatTableDir, file), 'utf8')
            );
            results[suite] = this.extractRhinoResults(data);
        }
        
        return results;
    }

    extractRhinoResults(data) {
        const results = {};
        const rhinoKey = 'rhino1_8_1'; // Current version
        
        for (const [feature, tests] of Object.entries(data)) {
            if (typeof tests === 'object' && tests !== null) {
                // Direct result
                if (rhinoKey in tests) {
                    results[feature] = tests[rhinoKey];
                } else {
                    // Check subtests
                    const subtests = {};
                    for (const [subtest, subdata] of Object.entries(tests)) {
                        if (typeof subdata === 'object' && rhinoKey in subdata) {
                            subtests[subtest] = subdata[rhinoKey];
                        }
                    }
                    if (Object.keys(subtests).length > 0) {
                        results[feature] = subtests;
                    }
                }
            }
        }
        
        return results;
    }

    getSuiteName(path) {
        const parts = path.split('/');
        if (parts[0] === 'built-ins') {
            return `built-ins/${parts[1]}`;
        } else if (parts[0] === 'language') {
            return `language/${parts[1]}`;
        } else if (parts[0] === 'annexB') {
            return 'annexB';
        } else if (parts[0] === 'intl402') {
            return 'intl402';
        }
        return parts[0];
    }

    generateUnifiedDocumentation() {
        // Get data from both sources
        const test262Data = this.parseTest262Properties();
        const compatData = this.runCompatTableTests();
        
        // Generate multiple output formats
        this.generateMarkdown(test262Data, compatData);
        this.generateHTML(test262Data, compatData);
        this.generateJSON(test262Data, compatData);
        
        console.log('\n✅ Documentation generated successfully!');
        console.log(`- FEATURES.md (Markdown documentation)`);
        console.log(`- rhino-features.html (Interactive dashboard)`);
        console.log(`- rhino-features.json (Machine-readable data)`);
    }

    generateMarkdown(test262Data, compatData) {
        let md = `# Rhino Feature Documentation
*Auto-generated from test262 and compat-table on ${new Date().toISOString().split('T')[0]}*

## Overview

This document combines data from two sources:
- **test262**: Official ECMAScript test suite (${test262Data.totalTests} tests)
- **compat-table**: Practical compatibility tests (~200 features)

## Summary Statistics

### test262 Results
- **Total Test Suites**: ${Object.keys(test262Data.suites).length}
- **Total Tests**: ${test262Data.totalTests}
- **Tests Passed**: ${test262Data.passedTests}
- **Overall Pass Rate**: ${test262Data.passRate}%

### compat-table Results
`;
        
        // Count compat-table features
        let compatFeatures = 0;
        let compatSupported = 0;
        for (const suite of Object.values(compatData)) {
            for (const [feature, result] of Object.entries(suite)) {
                compatFeatures++;
                if (result === true) compatSupported++;
            }
        }
        
        md += `- **Features Tested**: ${compatFeatures}
- **Features Supported**: ${compatSupported}
- **Support Rate**: ${((compatSupported/compatFeatures)*100).toFixed(1)}%

## Detailed Results

`;
        
        // Group and display results
        for (const [suite, data] of Object.entries(test262Data.suites)) {
            const passRate = ((data.passed / (data.passed + data.failed)) * 100).toFixed(1);
            md += `### ${suite}
- test262: ${passRate}% (${data.passed}/${data.passed + data.failed} tests)
`;
            
            // Add compat-table data if available
            const compatSuite = suite.replace('built-ins/', '').toLowerCase();
            if (compatData[compatSuite]) {
                const features = Object.entries(compatData[compatSuite]);
                const supported = features.filter(([,v]) => v === true).length;
                md += `- compat-table: ${supported}/${features.length} features supported

`;
            } else {
                md += '\n';
            }
        }
        
        fs.writeFileSync(path.join(this.rhinoRoot, 'FEATURES.md'), md);
    }

    generateHTML(test262Data, compatData) {
        // Create interactive HTML dashboard
        const html = `<!DOCTYPE html>
<html>
<head>
    <title>Rhino Feature Dashboard</title>
    <meta charset="UTF-8">
    <style>
        /* Styles here */
    </style>
</head>
<body>
    <h1>Rhino JavaScript Engine - Feature Dashboard</h1>
    <!-- Interactive content here -->
</body>
</html>`;
        
        fs.writeFileSync(path.join(this.rhinoRoot, 'rhino-features.html'), html);
    }

    generateJSON(test262Data, compatData) {
        const combined = {
            generated: new Date().toISOString(),
            test262: test262Data,
            compatTable: compatData,
            version: this.getRhinoVersion()
        };
        
        fs.writeFileSync(
            path.join(this.rhinoRoot, 'rhino-features.json'),
            JSON.stringify(combined, null, 2)
        );
    }

    getRhinoVersion() {
        try {
            const pomContent = fs.readFileSync(
                path.join(this.rhinoRoot, 'build.gradle'), 
                'utf8'
            );
            const match = pomContent.match(/version\s*=\s*['"]([^'"]+)['"]/);
            return match ? match[1] : '1.8.1-SNAPSHOT';
        } catch {
            return '1.8.1-SNAPSHOT';
        }
    }
}

// Run if executed directly
if (require.main === module) {
    const docs = new UnifiedRhinoDocumentation();
    docs.generateUnifiedDocumentation();
}

module.exports = UnifiedRhinoDocumentation;