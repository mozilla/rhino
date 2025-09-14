#!/usr/bin/env node
/**
 * Integrated Feature Tracker
 * Combines:
 * 1. test262.properties parsing (your PR #2089 approach)
 * 2. compat-table test results (p-bakker's approach)
 * 
 * This gives us TWO perspectives on each feature:
 * - Official spec compliance (test262)
 * - Practical compatibility (compat-table)
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

class IntegratedFeatureTracker {
    constructor() {
        this.rhinoRoot = path.join(__dirname, '..', '..');
        this.test262Props = path.join(this.rhinoRoot, 'testsrc', 'test262.properties');
        this.compatTableDir = path.join(__dirname, 'compat-table-repo');
    }

    // Parse test262.properties (your existing approach)
    parseTest262Properties() {
        const content = fs.readFileSync(this.test262Props, 'utf8');
        const lines = content.split('\n');
        const features = {};
        
        for (const line of lines) {
            if (line.includes('~')) {
                const [path, result] = line.split(' ');
                if (path && result) {
                    const feature = this.extractFeatureName(path);
                    if (!features[feature]) {
                        features[feature] = { passed: 0, failed: 0, paths: [] };
                    }
                    if (result.trim() === '~') {
                        features[feature].failed++;
                    } else {
                        features[feature].passed++;
                    }
                    features[feature].paths.push(path);
                }
            }
        }
        
        return features;
    }

    // Load compat-table results (p-bakker's approach)
    loadCompatTableResults() {
        const results = {};
        const resultFiles = fs.readdirSync(this.compatTableDir)
            .filter(f => f.startsWith('results-') && f.endsWith('.json'));
        
        for (const file of resultFiles) {
            const suite = file.replace('results-', '').replace('.json', '');
            const data = JSON.parse(fs.readFileSync(path.join(this.compatTableDir, file), 'utf8'));
            
            // Extract Rhino results
            for (const [feature, tests] of Object.entries(data)) {
                if (!results[feature]) {
                    results[feature] = {};
                }
                
                // Check for rhino results
                const rhinoVersions = ['rhino1_8_1', 'rhino1_7_14', 'rhino1_7_13'];
                for (const version of rhinoVersions) {
                    if (tests[version] !== undefined) {
                        results[feature].compatTable = {
                            suite,
                            supported: tests[version],
                            version
                        };
                        break;
                    }
                    
                    // Check subtests
                    for (const [subtest, subresults] of Object.entries(tests)) {
                        if (typeof subresults === 'object' && subresults[version] !== undefined) {
                            if (!results[feature].subtests) {
                                results[feature].subtests = {};
                            }
                            results[feature].subtests[subtest] = {
                                supported: subresults[version],
                                version
                            };
                        }
                    }
                }
            }
        }
        
        return results;
    }

    // Map between test262 paths and compat-table features
    createFeatureMapping() {
        return {
            // ES6+ features
            'language/expressions/arrow-function': 'arrow functions',
            'language/statements/for-of': 'for..of loops',
            'language/expressions/template-literal': 'template literals',
            'language/expressions/function/default-parameters': 'default function parameters',
            'language/expressions/function/rest-parameters': 'rest parameters',
            'language/statements/generators': 'generators',
            'language/statements/class': 'classes',
            
            // Built-ins
            'built-ins/Promise': 'Promise',
            'built-ins/Promise/prototype/finally': 'Promise.prototype.finally',
            'built-ins/Proxy': 'Proxy',
            'built-ins/Reflect': 'Reflect',
            'built-ins/Symbol': 'Symbol',
            'built-ins/Map': 'Map',
            'built-ins/Set': 'Set',
            'built-ins/WeakMap': 'WeakMap',
            'built-ins/WeakSet': 'WeakSet',
            'built-ins/TypedArray': 'typed arrays',
            
            // Array methods
            'built-ins/Array/from': 'Array.from',
            'built-ins/Array/of': 'Array.of',
            'built-ins/Array/prototype/includes': 'Array.prototype.includes',
            'built-ins/Array/prototype/find': 'Array.prototype.find',
            'built-ins/Array/prototype/findIndex': 'Array.prototype.findIndex',
            'built-ins/Array/prototype/fill': 'Array.prototype.fill',
            'built-ins/Array/prototype/copyWithin': 'Array.prototype.copyWithin',
            'built-ins/Array/prototype/flat': 'Array.prototype.flat',
            'built-ins/Array/prototype/flatMap': 'Array.prototype.flatMap',
            
            // String methods
            'built-ins/String/prototype/includes': 'String.prototype.includes',
            'built-ins/String/prototype/startsWith': 'String.prototype.startsWith',
            'built-ins/String/prototype/endsWith': 'String.prototype.endsWith',
            'built-ins/String/prototype/repeat': 'String.prototype.repeat',
            'built-ins/String/prototype/padStart': 'String.prototype.padStart',
            'built-ins/String/prototype/padEnd': 'String.prototype.padEnd',
            'built-ins/String/prototype/matchAll': 'String.prototype.matchAll',
            'built-ins/String/prototype/replaceAll': 'String.prototype.replaceAll',
            
            // Object methods
            'built-ins/Object/assign': 'Object.assign',
            'built-ins/Object/entries': 'Object.entries',
            'built-ins/Object/values': 'Object.values',
            'built-ins/Object/getOwnPropertyDescriptors': 'Object.getOwnPropertyDescriptors',
            'built-ins/Object/fromEntries': 'Object.fromEntries',
            'built-ins/Object/hasOwn': 'Object.hasOwn',
            
            // RegExp
            'built-ins/RegExp/property-escapes': 'RegExp Unicode Property Escapes',
            'built-ins/RegExp/named-groups': 'RegExp named capture groups',
            'built-ins/RegExp/lookbehind': 'RegExp Lookbehind Assertions',
            'built-ins/RegExp/dotAll': 's (dotAll) flag for regular expressions',
            
            // BigInt
            'built-ins/BigInt': 'BigInt',
            'built-ins/BigInt64Array': 'BigInt64Array',
            'built-ins/BigUint64Array': 'BigUint64Array'
        };
    }

    // Merge both data sources
    mergeResults() {
        const test262Data = this.parseTest262Properties();
        const compatData = this.loadCompatTableResults();
        const mapping = this.createFeatureMapping();
        
        const merged = {};
        
        // Start with test262 data
        for (const [path, data] of Object.entries(test262Data)) {
            const feature = this.extractFeatureName(path);
            merged[feature] = {
                test262: {
                    passed: data.passed,
                    failed: data.failed,
                    total: data.passed + data.failed,
                    passRate: ((data.passed / (data.passed + data.failed)) * 100).toFixed(1)
                }
            };
            
            // Check if we have compat-table data for this feature
            const compatName = mapping[path];
            if (compatName && compatData[compatName]) {
                merged[feature].compatTable = compatData[compatName].compatTable || null;
                merged[feature].compatSubtests = compatData[compatName].subtests || null;
            }
        }
        
        // Add compat-table features not in test262
        for (const [compatName, data] of Object.entries(compatData)) {
            const mappedPath = Object.keys(mapping).find(k => mapping[k] === compatName);
            if (!mappedPath) {
                // This is a compat-table-only feature
                merged[`compat:${compatName}`] = {
                    compatTable: data.compatTable || null,
                    compatSubtests: data.subtests || null,
                    test262: null
                };
            }
        }
        
        return merged;
    }

    extractFeatureName(path) {
        const parts = path.split('/');
        if (parts[0] === 'built-ins') {
            return parts.slice(0, 3).join('/');
        } else if (parts[0] === 'language') {
            return parts.slice(0, 3).join('/');
        }
        return parts[0];
    }

    generateEnhancedHTML() {
        const merged = this.mergeResults();
        
        let html = `<!DOCTYPE html>
<html>
<head>
    <title>Rhino JavaScript Engine - Comprehensive Feature Analysis</title>
    <style>
        body { 
            font-family: system-ui, -apple-system, sans-serif; 
            padding: 20px;
            max-width: 1400px;
            margin: 0 auto;
            background: #f5f5f5;
        }
        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 30px;
            border-radius: 10px;
            margin-bottom: 30px;
        }
        .feature-card {
            background: white;
            border-radius: 8px;
            padding: 20px;
            margin: 15px 0;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            transition: transform 0.2s;
        }
        .feature-card:hover {
            transform: translateY(-2px);
            box-shadow: 0 4px 8px rgba(0,0,0,0.15);
        }
        .feature-name {
            font-size: 1.2em;
            font-weight: bold;
            color: #333;
            margin-bottom: 10px;
        }
        .data-source {
            display: inline-block;
            padding: 5px 10px;
            border-radius: 5px;
            margin: 5px;
            font-size: 0.9em;
        }
        .test262 {
            background: #e7f3ff;
            border: 1px solid #3498db;
        }
        .compat-table {
            background: #fff3cd;
            border: 1px solid #ffc107;
        }
        .supported {
            color: #28a745;
            font-weight: bold;
        }
        .unsupported {
            color: #dc3545;
            font-weight: bold;
        }
        .partial {
            color: #ffc107;
            font-weight: bold;
        }
        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
            margin: 20px 0;
        }
        .stat-card {
            background: white;
            padding: 15px;
            border-radius: 8px;
            text-align: center;
        }
        .stat-number {
            font-size: 2em;
            font-weight: bold;
            color: #667eea;
        }
        .stat-label {
            color: #666;
            margin-top: 5px;
        }
        .comparison {
            display: flex;
            gap: 20px;
            align-items: center;
        }
        .vs {
            font-weight: bold;
            color: #999;
        }
    </style>
</head>
<body>
    <div class="header">
        <h1>Rhino JavaScript Engine - Unified Feature Documentation</h1>
        <p>Combining test262 specification compliance with compat-table practical testing</p>
    </div>
    
    <div class="stats-grid">
        <div class="stat-card">
            <div class="stat-number">${Object.keys(merged).length}</div>
            <div class="stat-label">Total Features Tracked</div>
        </div>
        <div class="stat-card">
            <div class="stat-number">${Object.values(merged).filter(f => f.test262).length}</div>
            <div class="stat-label">test262 Features</div>
        </div>
        <div class="stat-card">
            <div class="stat-number">${Object.values(merged).filter(f => f.compatTable).length}</div>
            <div class="stat-label">compat-table Features</div>
        </div>
        <div class="stat-card">
            <div class="stat-number">${Object.values(merged).filter(f => f.test262 && f.compatTable).length}</div>
            <div class="stat-label">Features in Both</div>
        </div>
    </div>
`;
        
        // Generate feature cards
        for (const [feature, data] of Object.entries(merged)) {
            html += `<div class="feature-card">
                <div class="feature-name">${feature}</div>
                <div class="comparison">`;
            
            if (data.test262) {
                const status = data.test262.passRate >= 95 ? 'supported' : 
                              data.test262.passRate >= 50 ? 'partial' : 'unsupported';
                html += `
                    <div class="data-source test262">
                        <strong>test262:</strong> 
                        <span class="${status}">${data.test262.passRate}%</span>
                        (${data.test262.passed}/${data.test262.total} tests)
                    </div>`;
            }
            
            if (data.test262 && data.compatTable) {
                html += '<span class="vs">vs</span>';
            }
            
            if (data.compatTable) {
                const status = data.compatTable.supported === true ? 'supported' : 
                              data.compatTable.supported === false ? 'unsupported' : 'partial';
                html += `
                    <div class="data-source compat-table">
                        <strong>compat-table:</strong> 
                        <span class="${status}">${data.compatTable.supported === true ? '✓' : '✗'}</span>
                    </div>`;
            }
            
            html += `</div>`;
            
            // Show subtests if available
            if (data.compatSubtests) {
                html += '<div style="margin-top: 10px; padding-left: 20px; font-size: 0.9em;">';
                for (const [subtest, result] of Object.entries(data.compatSubtests)) {
                    const icon = result.supported ? '✓' : '✗';
                    const color = result.supported ? 'green' : 'red';
                    html += `<div style="color: ${color};">${icon} ${subtest}</div>`;
                }
                html += '</div>';
            }
            
            html += '</div>';
        }
        
        html += `
    <div style="margin-top: 40px; padding: 20px; background: white; border-radius: 8px;">
        <h2>Data Sources</h2>
        <p><strong>test262:</strong> Official ECMAScript test suite - shows specification compliance</p>
        <p><strong>compat-table:</strong> Practical JavaScript feature tests - shows real-world compatibility</p>
        <p>When both sources agree, we have high confidence in the feature's status. 
           When they disagree, it may indicate partial implementation or edge cases.</p>
    </div>
</body>
</html>`;
        
        // Save the enhanced documentation
        const outputPath = path.join(this.rhinoRoot, 'ENHANCED-FEATURES.html');
        fs.writeFileSync(outputPath, html);
        console.log(`Generated enhanced documentation: ${outputPath}`);
        
        // Also save as JSON
        const jsonPath = path.join(this.rhinoRoot, 'enhanced-features.json');
        fs.writeFileSync(jsonPath, JSON.stringify(merged, null, 2));
        console.log(`Generated JSON data: ${jsonPath}`);
        
        // Print summary
        const bothSources = Object.values(merged).filter(f => f.test262 && f.compatTable);
        const agreements = bothSources.filter(f => {
            const test262Good = f.test262.passRate >= 50;
            const compatGood = f.compatTable.supported === true;
            return test262Good === compatGood;
        });
        
        console.log('\nAnalysis Summary:');
        console.log(`- Features in both sources: ${bothSources.length}`);
        console.log(`- Agreement rate: ${((agreements.length / bothSources.length) * 100).toFixed(1)}%`);
    }
}

// Run if executed directly
if (require.main === module) {
    const tracker = new IntegratedFeatureTracker();
    tracker.generateEnhancedHTML();
}

module.exports = IntegratedFeatureTracker;