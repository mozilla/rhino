#!/usr/bin/env node
/**
 * Rhino Feature Tracker - JavaScript version
 * Replaces feature_tracker.py with enhanced capabilities
 * 
 * Combines:
 * 1. test262.properties parsing (3,025+ test suites)
 * 2. compat-table integration (200+ practical features)
 * 
 * The compat-table integration is based on Paul Bakker's work in
 * https://github.com/compat-table/compat-table/pull/1881
 * which separated test definitions from results, enabling automated updates.
 * 
 * This provides the most comprehensive documentation possible.
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

class RhinoFeatureTracker {
    constructor() {
        this.rhinoRoot = path.join(__dirname, '..');
        this.test262Props = path.join(this.rhinoRoot, 'testsrc', 'test262.properties');
        this.compatTableRepo = 'https://github.com/compat-table/compat-table.git';
        this.compatTableDir = path.join(__dirname, 'compat-table-repo');
    }

    /**
     * Parse test262.properties file
     * Replaces the Python implementation with JavaScript
     */
    parseTest262Properties() {
        console.log('📊 Parsing test262.properties...');
        const content = fs.readFileSync(this.test262Props, 'utf8');
        const lines = content.split('\n');
        
        const categories = {};
        const features = {};
        let totalTests = 0;
        let passedTests = 0;
        
        for (const line of lines) {
            if (line.trim() && !line.startsWith('#')) {
                const match = line.match(/^([\w\/.~-]+)\s+(.*)/);
                if (match) {
                    const [, testPath, result] = match;
                    const category = this.categorizeTest(testPath);
                    const feature = this.extractFeature(testPath);
                    
                    // Track by category
                    if (!categories[category]) {
                        categories[category] = { passed: 0, failed: 0, tests: [] };
                    }
                    
                    // Track by feature
                    if (!features[feature]) {
                        features[feature] = { passed: 0, failed: 0, paths: [] };
                    }
                    
                    totalTests++;
                    if (result.trim() === '~') {
                        categories[category].failed++;
                        features[feature].failed++;
                    } else {
                        categories[category].passed++;
                        features[feature].passed++;
                        passedTests++;
                    }
                    
                    categories[category].tests.push(testPath);
                    features[feature].paths.push(testPath);
                }
            }
        }
        
        return {
            categories,
            features,
            summary: {
                totalSuites: Object.keys(features).length,
                totalTests,
                passedTests,
                failedTests: totalTests - passedTests,
                passRate: ((passedTests / totalTests) * 100).toFixed(1)
            }
        };
    }

    /**
     * Setup and run compat-table tests
     */
    async setupCompatTable() {
        console.log('🔧 Setting up compat-table...');
        
        // Clone or update compat-table repo
        if (!fs.existsSync(this.compatTableDir)) {
            console.log('  Cloning compat-table repository...');
            execSync(`git clone ${this.compatTableRepo} ${this.compatTableDir}`, {
                stdio: 'inherit'
            });
        } else {
            console.log('  Updating compat-table repository...');
            execSync('git pull', { 
                cwd: this.compatTableDir,
                stdio: 'inherit'
            });
        }
        
        // Install dependencies
        console.log('  Installing dependencies...');
        execSync('npm install', {
            cwd: this.compatTableDir,
            stdio: 'inherit'
        });
        
        // Build Rhino JAR
        const jarPath = path.join(this.rhinoRoot, 'build', 'libs', 'rhino-1.8.1-SNAPSHOT.jar');
        if (!fs.existsSync(jarPath)) {
            console.log('  Building Rhino JAR...');
            execSync('./gradlew jar', {
                cwd: this.rhinoRoot,
                stdio: 'inherit'
            });
        }
        
        // Copy JAR to compat-table
        console.log('  Copying Rhino JAR to compat-table...');
        fs.copyFileSync(jarPath, path.join(this.compatTableDir, 'rhino.jar'));
        
        // Run tests
        console.log('  Running compat-table tests...');
        execSync('node rhino.js -u', {
            cwd: this.compatTableDir,
            stdio: 'inherit'
        });
        
        return this.loadCompatTableResults();
    }

    /**
     * Load compat-table results
     */
    loadCompatTableResults() {
        console.log('📖 Loading compat-table results...');
        const results = {};
        
        const files = fs.readdirSync(this.compatTableDir)
            .filter(f => f.startsWith('results-') && f.endsWith('.json'));
        
        for (const file of files) {
            const suite = file.replace('results-', '').replace('.json', '');
            const filePath = path.join(this.compatTableDir, file);
            const data = JSON.parse(fs.readFileSync(filePath, 'utf8'));
            
            results[suite] = {};
            
            // Extract Rhino results
            for (const [feature, tests] of Object.entries(data)) {
                if (typeof tests === 'object' && tests !== null) {
                    // Check for rhino1_8_1 results
                    if ('rhino1_8_1' in tests) {
                        results[suite][feature] = {
                            supported: tests.rhino1_8_1,
                            version: '1.8.1'
                        };
                    } else {
                        // Check subtests
                        const subtests = {};
                        for (const [subtest, subdata] of Object.entries(tests)) {
                            if (typeof subdata === 'object' && 'rhino1_8_1' in subdata) {
                                subtests[subtest] = subdata.rhino1_8_1;
                            }
                        }
                        if (Object.keys(subtests).length > 0) {
                            results[suite][feature] = {
                                subtests,
                                version: '1.8.1'
                            };
                        }
                    }
                }
            }
        }
        
        return results;
    }

    /**
     * Generate combined documentation
     */
    async generateDocumentation() {
        console.log('📝 Generating documentation...');
        
        // Get data from both sources
        const test262Data = this.parseTest262Properties();
        const compatData = await this.setupCompatTable();
        
        // Generate all output formats
        this.generateMarkdown(test262Data, compatData);
        this.generateHTML(test262Data, compatData);
        this.generateJSON(test262Data, compatData);
        
        console.log('\n✅ Documentation generated successfully!');
        console.log('   - FEATURES.md');
        console.log('   - rhino-features.html');
        console.log('   - rhino-features.json');
    }

    /**
     * Generate Markdown documentation
     */
    generateMarkdown(test262Data, compatData) {
        let md = `# Rhino Feature Documentation
*Auto-generated from test262.properties and compat-table on ${new Date().toISOString().split('T')[0]}*

## Overview

This document provides comprehensive documentation of ALL features tested in Rhino, combining:
- **test262**: Official ECMAScript specification compliance tests
- **compat-table**: Practical JavaScript compatibility tests used by Babel and build tools

## Summary Statistics

### test262 Results
- **Total Test Suites**: ${test262Data.summary.totalSuites}
- **Total Tests**: ${test262Data.summary.totalTests}
- **Tests Passed**: ${test262Data.summary.passedTests}
- **Overall Pass Rate**: ${test262Data.summary.passRate}%

### compat-table Results
`;
        
        // Count compat-table features
        let compatFeatures = 0;
        let compatSupported = 0;
        
        for (const suite of Object.values(compatData)) {
            for (const [, result] of Object.entries(suite)) {
                compatFeatures++;
                if (result.supported === true) {
                    compatSupported++;
                } else if (result.subtests) {
                    // Count partial support
                    const subtestValues = Object.values(result.subtests);
                    const supportedSubtests = subtestValues.filter(v => v === true).length;
                    if (supportedSubtests === subtestValues.length) {
                        compatSupported++;
                    }
                }
            }
        }
        
        md += `- **Features Tested**: ${compatFeatures}
- **Features Supported**: ${compatSupported}
- **Support Rate**: ${((compatSupported/compatFeatures)*100).toFixed(1)}%

## Feature Categories

`;
        
        // Group by category
        const sortedCategories = Object.entries(test262Data.categories)
            .sort(([,a], [,b]) => (b.passed + b.failed) - (a.passed + a.failed));
        
        for (const [category, data] of sortedCategories) {
            const total = data.passed + data.failed;
            const passRate = ((data.passed / total) * 100).toFixed(1);
            const status = passRate >= 95 ? '✅ Full' : 
                          passRate >= 75 ? '🟢 Mostly' :
                          passRate >= 50 ? '🟡 Partial' : 
                          passRate >= 25 ? '🟠 Limited' : '🔴 Minimal';
            
            md += `### ${category}

**test262 Pass Rate**: ${passRate}% (${data.passed}/${total} tests) ${status}
`;
            
            // Add compat-table data if available
            const compatCategory = this.mapCategoryToCompatTable(category);
            if (compatData[compatCategory]) {
                const features = Object.entries(compatData[compatCategory]);
                const supported = features.filter(([,v]) => v.supported === true).length;
                md += `**compat-table**: ${supported}/${features.length} features supported

`;
            } else {
                md += '\n';
            }
            
            // Show top features in this category
            const categoryFeatures = Object.entries(test262Data.features)
                .filter(([name]) => name.startsWith(category))
                .sort(([,a], [,b]) => (b.passed + b.failed) - (a.passed + a.failed))
                .slice(0, 5);
            
            if (categoryFeatures.length > 0) {
                md += '| Feature | Pass Rate | Tests Passed | Status |\n';
                md += '|---------|-----------|--------------|--------|\n';
                
                for (const [feature, data] of categoryFeatures) {
                    const featureName = feature.replace(category + '/', '');
                    const total = data.passed + data.failed;
                    const rate = ((data.passed / total) * 100).toFixed(1);
                    const status = rate >= 95 ? '✅' : rate >= 75 ? '🟢' : rate >= 50 ? '🟡' : '🔴';
                    md += `| ${featureName} | ${rate}% | ${data.passed}/${total} | ${status} |\n`;
                }
                md += '\n';
            }
        }
        
        // Add cross-reference section
        md += `## Cross-Reference: test262 vs compat-table

This section shows features that appear in both test suites, highlighting any discrepancies:

| Feature | test262 | compat-table | Agreement |
|---------|---------|--------------|-----------|
`;
        
        // Find matching features
        const mappings = this.createFeatureMappings();
        for (const [test262Path, compatName] of Object.entries(mappings)) {
            const test262Feature = test262Data.features[test262Path];
            let compatResult = null;
            
            // Search for compat result
            for (const suite of Object.values(compatData)) {
                if (suite[compatName]) {
                    compatResult = suite[compatName];
                    break;
                }
            }
            
            if (test262Feature && compatResult) {
                const test262Rate = ((test262Feature.passed / (test262Feature.passed + test262Feature.failed)) * 100).toFixed(0);
                const compatStatus = compatResult.supported === true ? '✅' : '❌';
                const agreement = (test262Rate >= 50 && compatResult.supported === true) || 
                                 (test262Rate < 50 && compatResult.supported === false) ? '✅' : '⚠️';
                
                md += `| ${compatName} | ${test262Rate}% | ${compatStatus} | ${agreement} |\n`;
            }
        }
        
        md += `
## Data Sources

- **test262**: The official ECMAScript test suite maintained by TC39
- **compat-table**: Community-maintained compatibility tables used by Babel and other tools

When both sources agree (✅), we have high confidence in the feature's status.
When they disagree (⚠️), it may indicate partial implementation or edge cases that need investigation.
`;
        
        fs.writeFileSync(path.join(this.rhinoRoot, 'FEATURES.md'), md);
    }

    /**
     * Generate HTML documentation
     */
    generateHTML(test262Data, compatData) {
        const html = `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Rhino JavaScript Engine - Feature Dashboard</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            padding: 20px;
        }
        .container {
            max-width: 1400px;
            margin: 0 auto;
            background: white;
            border-radius: 12px;
            overflow: hidden;
            box-shadow: 0 20px 60px rgba(0,0,0,0.3);
        }
        header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 40px;
            text-align: center;
        }
        h1 { font-size: 2.5em; margin-bottom: 10px; }
        .subtitle { opacity: 0.9; font-size: 1.1em; }
        
        .stats {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 20px;
            padding: 30px;
            background: #f8f9fa;
        }
        .stat-card {
            background: white;
            padding: 25px;
            border-radius: 8px;
            text-align: center;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }
        .stat-number {
            font-size: 2.5em;
            font-weight: bold;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            background-clip: text;
        }
        .stat-label {
            color: #666;
            margin-top: 5px;
            text-transform: uppercase;
            font-size: 0.85em;
            letter-spacing: 1px;
        }
        
        .content { padding: 30px; }
        
        .feature-grid {
            display: grid;
            gap: 20px;
            margin-top: 30px;
        }
        .feature-category {
            background: #f8f9fa;
            border-radius: 8px;
            padding: 20px;
            border: 1px solid #e9ecef;
        }
        .category-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 15px;
        }
        .category-name {
            font-size: 1.3em;
            font-weight: bold;
            color: #333;
        }
        .progress-bar {
            width: 200px;
            height: 10px;
            background: #e9ecef;
            border-radius: 5px;
            overflow: hidden;
        }
        .progress-fill {
            height: 100%;
            background: linear-gradient(90deg, #28a745, #ffc107, #dc3545);
            transition: width 0.3s ease;
        }
        
        .data-sources {
            display: flex;
            gap: 15px;
            margin-top: 10px;
        }
        .source-badge {
            padding: 5px 10px;
            border-radius: 20px;
            font-size: 0.85em;
            font-weight: bold;
        }
        .test262 {
            background: #e7f3ff;
            color: #0066cc;
        }
        .compat-table {
            background: #fff3cd;
            color: #856404;
        }
        
        .search-box {
            width: 100%;
            padding: 15px;
            font-size: 1.1em;
            border: 2px solid #e9ecef;
            border-radius: 8px;
            margin-bottom: 20px;
        }
        .search-box:focus {
            outline: none;
            border-color: #667eea;
        }
        
        .tabs {
            display: flex;
            gap: 10px;
            margin-bottom: 20px;
            border-bottom: 2px solid #e9ecef;
        }
        .tab {
            padding: 10px 20px;
            cursor: pointer;
            border: none;
            background: none;
            font-size: 1em;
            color: #666;
            transition: all 0.3s;
        }
        .tab.active {
            color: #667eea;
            border-bottom: 3px solid #667eea;
            margin-bottom: -2px;
        }
    </style>
</head>
<body>
    <div class="container">
        <header>
            <h1>Rhino JavaScript Engine</h1>
            <div class="subtitle">Comprehensive Feature Documentation</div>
            <div style="margin-top: 20px; opacity: 0.8;">
                Combining test262 specification compliance with compat-table practical testing
            </div>
        </header>
        
        <div class="stats">
            <div class="stat-card">
                <div class="stat-number">${test262Data.summary.totalSuites}</div>
                <div class="stat-label">test262 Suites</div>
            </div>
            <div class="stat-card">
                <div class="stat-number">${test262Data.summary.passRate}%</div>
                <div class="stat-label">test262 Pass Rate</div>
            </div>
            <div class="stat-card">
                <div class="stat-number">${Object.values(compatData).reduce((acc, suite) => acc + Object.keys(suite).length, 0)}</div>
                <div class="stat-label">compat-table Features</div>
            </div>
            <div class="stat-card">
                <div class="stat-number">${new Date().toISOString().split('T')[0]}</div>
                <div class="stat-label">Last Updated</div>
            </div>
        </div>
        
        <div class="content">
            <input type="text" class="search-box" placeholder="Search features..." id="searchBox">
            
            <div class="tabs">
                <button class="tab active" onclick="showTab('overview')">Overview</button>
                <button class="tab" onclick="showTab('test262')">test262 Details</button>
                <button class="tab" onclick="showTab('compat')">compat-table Details</button>
                <button class="tab" onclick="showTab('comparison')">Comparison</button>
            </div>
            
            <div id="overview" class="tab-content">
                <h2>Feature Categories</h2>
                <div class="feature-grid" id="featureGrid">
                    ${this.generateCategoryHTML(test262Data, compatData)}
                </div>
            </div>
            
            <div id="test262" class="tab-content" style="display:none;">
                <h2>test262 Test Results</h2>
                <p>Detailed results from the official ECMAScript test suite...</p>
            </div>
            
            <div id="compat" class="tab-content" style="display:none;">
                <h2>compat-table Results</h2>
                <p>Practical compatibility test results...</p>
            </div>
            
            <div id="comparison" class="tab-content" style="display:none;">
                <h2>test262 vs compat-table Comparison</h2>
                <p>Features that appear in both test suites...</p>
            </div>
        </div>
    </div>
    
    <script>
        function showTab(tabName) {
            document.querySelectorAll('.tab-content').forEach(content => {
                content.style.display = 'none';
            });
            document.getElementById(tabName).style.display = 'block';
            
            document.querySelectorAll('.tab').forEach(tab => {
                tab.classList.remove('active');
            });
            event.target.classList.add('active');
        }
        
        document.getElementById('searchBox').addEventListener('input', function(e) {
            const searchTerm = e.target.value.toLowerCase();
            document.querySelectorAll('.feature-category').forEach(category => {
                const text = category.textContent.toLowerCase();
                category.style.display = text.includes(searchTerm) ? 'block' : 'none';
            });
        });
    </script>
</body>
</html>`;
        
        fs.writeFileSync(path.join(this.rhinoRoot, 'rhino-features.html'), html);
    }

    /**
     * Generate JSON documentation
     */
    generateJSON(test262Data, compatData) {
        const output = {
            generated: new Date().toISOString(),
            rhinoVersion: this.getRhinoVersion(),
            test262: test262Data,
            compatTable: compatData,
            crossReference: this.createCrossReference(test262Data, compatData)
        };
        
        fs.writeFileSync(
            path.join(this.rhinoRoot, 'rhino-features.json'),
            JSON.stringify(output, null, 2)
        );
    }

    // Helper methods
    categorizeTest(path) {
        const parts = path.split('/');
        if (parts[0] === 'built-ins') {
            return `Built-in Objects/${parts[1]}`;
        } else if (parts[0] === 'language') {
            return `Language/${parts[1]}`;
        } else if (parts[0] === 'annexB') {
            return 'Annex B (Legacy)';
        } else if (parts[0] === 'intl402') {
            return 'Internationalization';
        }
        return parts[0];
    }

    extractFeature(path) {
        const parts = path.split('/');
        if (parts.length >= 3) {
            return parts.slice(0, 3).join('/');
        }
        return parts.slice(0, 2).join('/');
    }

    mapCategoryToCompatTable(category) {
        const mapping = {
            'Built-in Objects/Array': 'es6',
            'Built-in Objects/Promise': 'es6',
            'Built-in Objects/Proxy': 'es6',
            'Language/expressions': 'es6',
            'Language/statements': 'es6'
        };
        return mapping[category] || category.toLowerCase();
    }

    createFeatureMappings() {
        return {
            'language/expressions/arrow-function': 'arrow functions',
            'built-ins/Promise': 'Promise',
            'built-ins/Proxy': 'Proxy',
            'built-ins/Array/from': 'Array.from',
            'built-ins/Object/assign': 'Object.assign',
            // Add more mappings as needed
        };
    }

    createCrossReference(test262Data, compatData) {
        const mappings = this.createFeatureMappings();
        const crossRef = [];
        
        for (const [test262Path, compatName] of Object.entries(mappings)) {
            const test262Feature = test262Data.features[test262Path];
            let compatResult = null;
            
            for (const suite of Object.values(compatData)) {
                if (suite[compatName]) {
                    compatResult = suite[compatName];
                    break;
                }
            }
            
            if (test262Feature && compatResult) {
                crossRef.push({
                    name: compatName,
                    test262: {
                        path: test262Path,
                        passed: test262Feature.passed,
                        failed: test262Feature.failed,
                        passRate: ((test262Feature.passed / (test262Feature.passed + test262Feature.failed)) * 100).toFixed(1)
                    },
                    compatTable: compatResult
                });
            }
        }
        
        return crossRef;
    }

    generateCategoryHTML(test262Data, compatData) {
        // Generate HTML for category display
        return Object.entries(test262Data.categories)
            .map(([category, data]) => {
                const total = data.passed + data.failed;
                const passRate = ((data.passed / total) * 100).toFixed(1);
                
                return `
                    <div class="feature-category">
                        <div class="category-header">
                            <div class="category-name">${category}</div>
                            <div class="progress-bar">
                                <div class="progress-fill" style="width: ${passRate}%"></div>
                            </div>
                        </div>
                        <div class="data-sources">
                            <span class="source-badge test262">test262: ${passRate}%</span>
                        </div>
                    </div>
                `;
            }).join('');
    }

    getRhinoVersion() {
        try {
            const gradleFile = fs.readFileSync(
                path.join(this.rhinoRoot, 'gradle.properties'),
                'utf8'
            );
            const match = gradleFile.match(/version=(.+)/);
            return match ? match[1] : '1.8.1-SNAPSHOT';
        } catch {
            return '1.8.1-SNAPSHOT';
        }
    }
}

// Run if executed directly
if (require.main === module) {
    const tracker = new RhinoFeatureTracker();
    tracker.generateDocumentation().catch(console.error);
}

module.exports = RhinoFeatureTracker;