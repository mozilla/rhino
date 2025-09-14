#!/usr/bin/env node
/**
 * Integrate compat-table results with Rhino documentation
 * This creates a bridge between compat-table test results and our comprehensive docs
 */

const fs = require('fs');
const path = require('path');

class CompatTableIntegration {
    constructor() {
        this.compatTableDir = path.join(__dirname, 'compat-table-repo');
        this.rhinoDocsDir = path.join(__dirname, '..', 'docs');
    }

    loadCompatResults() {
        const results = {};
        const resultFiles = fs.readdirSync(this.compatTableDir)
            .filter(f => f.startsWith('results-') && f.endsWith('.json'));
        
        for (const file of resultFiles) {
            const suiteName = file.replace('results-', '').replace('.json', '');
            const data = JSON.parse(fs.readFileSync(path.join(this.compatTableDir, file), 'utf8'));
            results[suiteName] = this.extractRhinoResults(data);
        }
        return results;
    }

    extractRhinoResults(data) {
        const rhinoResults = {};
        const rhinoVersions = ['rhino1_8_1', 'rhino1_7_14', 'rhino1_7_13'];
        
        for (const [feature, tests] of Object.entries(data)) {
            if (typeof tests === 'object' && tests !== null) {
                // Check for direct rhino results
                for (const version of rhinoVersions) {
                    if (version in tests) {
                        rhinoResults[feature] = {
                            version,
                            supported: tests[version]
                        };
                        break;
                    }
                }
                
                // Check subtests
                if (!rhinoResults[feature]) {
                    const subtestResults = {};
                    for (const [subtest, results] of Object.entries(tests)) {
                        if (typeof results === 'object' && results !== null) {
                            for (const version of rhinoVersions) {
                                if (version in results) {
                                    subtestResults[subtest] = {
                                        version,
                                        supported: results[version]
                                    };
                                    break;
                                }
                            }
                        }
                    }
                    if (Object.keys(subtestResults).length > 0) {
                        rhinoResults[feature] = subtestResults;
                    }
                }
            }
        }
        return rhinoResults;
    }

    mapToTest262() {
        // Map compat-table features to test262 paths for cross-referencing
        return {
            'arrow functions': 'language/expressions/arrow-function',
            'Promise.prototype.finally': 'built-ins/Promise/prototype/finally',
            'Array.prototype.includes': 'built-ins/Array/prototype/includes',
            'Object.getOwnPropertyDescriptors': 'built-ins/Object/getOwnPropertyDescriptors',
            'RegExp named capture groups': 'built-ins/RegExp/named-groups',
            'RegExp Lookbehind Assertions': 'built-ins/RegExp/lookbehind',
            'default function parameters': 'language/expressions/function/default-parameters',
            'rest parameters': 'language/expressions/function/rest-parameters',
            'template literals': 'language/expressions/template-literal',
            'destructuring': 'language/statements/for-of/destructuring',
            'Proxy': 'built-ins/Proxy',
            'Reflect': 'built-ins/Reflect',
            'Symbol': 'built-ins/Symbol',
            'Map': 'built-ins/Map',
            'Set': 'built-ins/Set',
            'WeakMap': 'built-ins/WeakMap',
            'WeakSet': 'built-ins/WeakSet',
            'typed arrays': 'built-ins/TypedArray',
            'generators': 'language/statements/generators',
            'Promise': 'built-ins/Promise',
            'Object.assign': 'built-ins/Object/assign',
            'String.prototype.includes': 'built-ins/String/prototype/includes',
            'String.prototype.startsWith': 'built-ins/String/prototype/startsWith',
            'String.prototype.endsWith': 'built-ins/String/prototype/endsWith',
            'Array.from': 'built-ins/Array/from',
            'Array.of': 'built-ins/Array/of',
            'Array.prototype.find': 'built-ins/Array/prototype/find',
            'Array.prototype.findIndex': 'built-ins/Array/prototype/findIndex',
            'Array.prototype.fill': 'built-ins/Array/prototype/fill',
            'Array.prototype.copyWithin': 'built-ins/Array/prototype/copyWithin'
        };
    }

    generateEnhancedDocs() {
        const compatResults = this.loadCompatResults();
        
        let html = `<!DOCTYPE html>
<html>
<head>
    <title>Rhino JavaScript Engine - Feature Compatibility</title>
    <style>
        body { 
            font-family: system-ui, -apple-system, sans-serif; 
            padding: 20px;
            max-width: 1200px;
            margin: 0 auto;
        }
        .summary {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 30px;
            border-radius: 10px;
            margin-bottom: 30px;
        }
        .summary h1 { margin: 0 0 10px 0; }
        .stats {
            display: flex;
            gap: 30px;
            margin-top: 20px;
        }
        .stat {
            background: rgba(255,255,255,0.2);
            padding: 15px;
            border-radius: 5px;
        }
        .stat-number {
            font-size: 2em;
            font-weight: bold;
        }
        .feature { 
            margin: 20px 0; 
            padding: 15px; 
            border: 1px solid #ddd; 
            border-radius: 5px;
            transition: transform 0.2s;
        }
        .feature:hover {
            transform: translateX(5px);
            box-shadow: 0 2px 5px rgba(0,0,0,0.1);
        }
        .supported { background: #d4edda; border-color: #28a745; }
        .unsupported { background: #f8d7da; border-color: #dc3545; }
        .partial { background: #fff3cd; border-color: #ffc107; }
        .compat-table { 
            background: #e7f3ff; 
            padding: 10px; 
            margin: 10px 0; 
            border-radius: 3px; 
        }
        .test262 { 
            background: #f0e7ff; 
            padding: 10px; 
            margin: 10px 0; 
            border-radius: 3px; 
        }
        h1 { color: #333; }
        h2 { 
            color: #666; 
            font-size: 1.2em;
            border-bottom: 2px solid #eee;
            padding-bottom: 5px;
        }
        .badge { 
            display: inline-block; 
            padding: 3px 8px; 
            border-radius: 3px; 
            font-size: 0.85em; 
            font-weight: bold;
            margin-right: 5px;
        }
        .badge.es5 { background: #6c757d; color: white; }
        .badge.es6 { background: #28a745; color: white; }
        .badge.es2016plus { background: #17a2b8; color: white; }
        .badge.esnext { background: #fd7e14; color: white; }
        table { 
            width: 100%; 
            border-collapse: collapse; 
            margin: 10px 0; 
        }
        th, td { 
            padding: 8px; 
            text-align: left; 
            border: 1px solid #ddd; 
        }
        th { background: #f5f5f5; }
        .icon-yes { color: #28a745; }
        .icon-no { color: #dc3545; }
        .filter-buttons {
            margin: 20px 0;
            display: flex;
            gap: 10px;
        }
        .filter-btn {
            padding: 8px 16px;
            border: 1px solid #ddd;
            background: white;
            border-radius: 5px;
            cursor: pointer;
            transition: all 0.2s;
        }
        .filter-btn:hover {
            background: #f0f0f0;
        }
        .filter-btn.active {
            background: #667eea;
            color: white;
            border-color: #667eea;
        }
    </style>
</head>
<body>
    <div class="summary">
        <h1>Rhino JavaScript Engine - Comprehensive Feature Support</h1>
        <p>Automated compatibility testing powered by compat-table and test262</p>
        <div class="stats">
            <div class="stat">
                <div class="stat-number" id="total-features">0</div>
                <div>Total Features Tested</div>
            </div>
            <div class="stat">
                <div class="stat-number" id="supported-features">0</div>
                <div>Supported Features</div>
            </div>
            <div class="stat">
                <div class="stat-number" id="support-percentage">0%</div>
                <div>Support Rate</div>
            </div>
        </div>
    </div>
    
    <div class="filter-buttons">
        <button class="filter-btn active" onclick="filterFeatures('all')">All Features</button>
        <button class="filter-btn" onclick="filterFeatures('supported')">✅ Supported</button>
        <button class="filter-btn" onclick="filterFeatures('unsupported')">❌ Not Supported</button>
        <button class="filter-btn" onclick="filterFeatures('partial')">⚠️ Partial Support</button>
    </div>
    
    <div id="features-container">
`;
        
        let totalFeatures = 0;
        let supportedFeatures = 0;
        
        // Process and count features
        for (const [suiteName, features] of Object.entries(compatResults)) {
            if (!features || Object.keys(features).length === 0) continue;
            
            html += `<h2><span class="badge ${suiteName}">${suiteName.toUpperCase()}</span> Features</h2>`;
            
            for (const [featureName, result] of Object.entries(features)) {
                totalFeatures++;
                
                if (typeof result === 'object' && result !== null && 'supported' in result) {
                    const isSupported = result.supported === true;
                    if (isSupported) supportedFeatures++;
                    
                    const supportClass = isSupported ? 'supported' : 'unsupported';
                    const icon = isSupported ? '✅' : '❌';
                    const status = isSupported ? 'Supported' : 'Not supported';
                    
                    html += `
    <div class="feature ${supportClass}" data-status="${supportClass}">
        <h3>${icon} ${featureName}</h3>
        <div class="compat-table">
            <strong>Compat-table result:</strong> ${status}
            (tested with ${result.version.replace('rhino', 'Rhino ').replace(/_/g, '.')})
        </div>
`;
                    
                    // Add test262 mapping if available
                    const test262Path = this.mapToTest262()[featureName];
                    if (test262Path) {
                        html += `
        <div class="test262">
            <strong>Test262 path:</strong> ${test262Path}
        </div>
`;
                    }
                    
                    html += `    </div>\n`;
                } else {
                    // Handle features with subtests
                    let hasPartialSupport = false;
                    let subtestCount = 0;
                    let supportedSubtests = 0;
                    
                    for (const [, subresult] of Object.entries(result)) {
                        if (typeof subresult === 'object' && subresult !== null && 'supported' in subresult) {
                            subtestCount++;
                            if (subresult.supported === true) {
                                supportedSubtests++;
                                hasPartialSupport = true;
                            }
                        }
                    }
                    
                    if (subtestCount > 0) {
                        const supportClass = supportedSubtests === subtestCount ? 'supported' : 
                                            supportedSubtests === 0 ? 'unsupported' : 'partial';
                        if (supportedSubtests === subtestCount) supportedFeatures++;
                        
                        html += `
    <div class="feature ${supportClass}" data-status="${supportClass}">
        <h3>⚠️ ${featureName} (${supportedSubtests}/${subtestCount} subtests)</h3>
        <table>
            <tr><th>Subtest</th><th>Support</th></tr>
`;
                        
                        for (const [subtest, subresult] of Object.entries(result)) {
                            if (typeof subresult === 'object' && subresult !== null && 'supported' in subresult) {
                                const icon = subresult.supported ? '✅' : '❌';
                                html += `            <tr><td>${subtest}</td><td>${icon}</td></tr>\n`;
                            }
                        }
                        
                        html += `        </table>\n    </div>\n`;
                    }
                }
            }
        }
        
        const percentage = totalFeatures > 0 ? Math.round((supportedFeatures / totalFeatures) * 100) : 0;
        
        html += `
    </div>
    
    <div style="margin-top: 40px; padding: 20px; background: #f8f9fa; border-radius: 5px;">
        <h2>Data Sources</h2>
        <p><strong>compat-table:</strong> Practical JavaScript feature testing used by Babel and build tools</p>
        <p><strong>test262:</strong> Official ECMAScript test suite for specification compliance</p>
        <p>By combining both, we get a complete picture of Rhino's capabilities.</p>
        <p style="margin-top: 20px; color: #666;">
            Generated: ${new Date().toISOString()}<br>
            Rhino version: 1.8.1-SNAPSHOT
        </p>
    </div>
    
    <script>
        // Update statistics
        document.getElementById('total-features').textContent = '${totalFeatures}';
        document.getElementById('supported-features').textContent = '${supportedFeatures}';
        document.getElementById('support-percentage').textContent = '${percentage}%';
        
        // Filter functionality
        function filterFeatures(status) {
            const features = document.querySelectorAll('.feature');
            const buttons = document.querySelectorAll('.filter-btn');
            
            buttons.forEach(btn => btn.classList.remove('active'));
            event.target.classList.add('active');
            
            features.forEach(feature => {
                if (status === 'all') {
                    feature.style.display = 'block';
                } else {
                    feature.style.display = feature.dataset.status === status ? 'block' : 'none';
                }
            });
        }
    </script>
</body>
</html>
`;
        
        // Ensure docs directory exists
        if (!fs.existsSync(this.rhinoDocsDir)) {
            fs.mkdirSync(this.rhinoDocsDir, { recursive: true });
        }
        
        // Write HTML file
        const htmlPath = path.join(this.rhinoDocsDir, 'rhino-compat-integrated.html');
        fs.writeFileSync(htmlPath, html);
        console.log(`Generated integrated documentation: ${htmlPath}`);
        
        // Write JSON file
        const jsonPath = path.join(this.rhinoDocsDir, 'rhino-compat-integrated.json');
        fs.writeFileSync(jsonPath, JSON.stringify(compatResults, null, 2));
        console.log(`Generated JSON data: ${jsonPath}`);
        
        // Print summary
        console.log(`\nSummary:`);
        console.log(`- Total features tested: ${totalFeatures}`);
        console.log(`- Supported features: ${supportedFeatures}`);
        console.log(`- Support rate: ${percentage}%`);
    }
}

// Run if executed directly
if (require.main === module) {
    const integration = new CompatTableIntegration();
    integration.generateEnhancedDocs();
}

module.exports = CompatTableIntegration;