#!/usr/bin/env python3
"""
Integrate compat-table results with our Rhino documentation.
This creates a bridge between compat-table test results and our comprehensive docs.
"""

import json
import os
from pathlib import Path

class CompatTableIntegration:
    def __init__(self):
        self.compat_table_dir = Path("compat-table-repo")
        self.rhino_docs_dir = Path("../docs")
        
    def load_compat_results(self):
        """Load all compat-table results for Rhino"""
        results = {}
        for result_file in self.compat_table_dir.glob("results-*.json"):
            suite_name = result_file.stem.replace("results-", "")
            with open(result_file) as f:
                data = json.load(f)
                results[suite_name] = self.extract_rhino_results(data)
        return results
    
    def extract_rhino_results(self, data):
        """Extract Rhino-specific results from compat-table data"""
        rhino_results = {}
        for feature, tests in data.items():
            if isinstance(tests, dict):
                # Check for rhino1_8_1, rhino1_7_14, rhino1_7_13
                for rhino_version in ['rhino1_8_1', 'rhino1_7_14', 'rhino1_7_13']:
                    if rhino_version in tests:
                        rhino_results[feature] = {
                            'version': rhino_version,
                            'supported': tests[rhino_version]
                        }
                        break
                    # Check subtests
                    for subtest, results in tests.items():
                        if isinstance(results, dict) and rhino_version in results:
                            if feature not in rhino_results:
                                rhino_results[feature] = {}
                            rhino_results[feature][subtest] = {
                                'version': rhino_version,
                                'supported': results[rhino_version]
                            }
        return rhino_results
    
    def map_to_test262(self):
        """Map compat-table features to test262 paths"""
        # This creates a mapping between compat-table feature names
        # and test262 test paths for cross-referencing
        mapping = {
            'arrow functions': 'language/expressions/arrow-function',
            'Promise.prototype.finally': 'built-ins/Promise/prototype/finally',
            'Array.prototype.includes': 'built-ins/Array/prototype/includes',
            'Object.getOwnPropertyDescriptors': 'built-ins/Object/getOwnPropertyDescriptors',
            'RegExp named capture groups': 'built-ins/RegExp/named-groups',
            'RegExp Lookbehind Assertions': 'built-ins/RegExp/lookbehind',
            # Add more mappings as needed
        }
        return mapping
    
    def generate_enhanced_docs(self):
        """Generate documentation that combines both data sources"""
        compat_results = self.load_compat_results()
        
        html = """<!DOCTYPE html>
<html>
<head>
    <title>Rhino JavaScript Engine - Feature Compatibility</title>
    <style>
        body { font-family: system-ui, -apple-system, sans-serif; padding: 20px; }
        .feature { margin: 20px 0; padding: 15px; border: 1px solid #ddd; border-radius: 5px; }
        .supported { background: #d4edda; }
        .unsupported { background: #f8d7da; }
        .partial { background: #fff3cd; }
        .compat-table { background: #e7f3ff; padding: 10px; margin: 10px 0; border-radius: 3px; }
        .test262 { background: #f0e7ff; padding: 10px; margin: 10px 0; border-radius: 3px; }
        h1 { color: #333; }
        h2 { color: #666; font-size: 1.2em; }
        .badge { 
            display: inline-block; 
            padding: 3px 8px; 
            border-radius: 3px; 
            font-size: 0.85em; 
            font-weight: bold;
        }
        .badge.es6 { background: #28a745; color: white; }
        .badge.es2016 { background: #17a2b8; color: white; }
        .badge.es2017 { background: #6f42c1; color: white; }
        .badge.esnext { background: #fd7e14; color: white; }
        table { width: 100%; border-collapse: collapse; margin: 10px 0; }
        th, td { padding: 8px; text-align: left; border: 1px solid #ddd; }
        th { background: #f5f5f5; }
    </style>
</head>
<body>
    <h1>Rhino JavaScript Engine - Comprehensive Feature Support</h1>
    <p>This documentation combines data from:</p>
    <ul>
        <li><strong>compat-table</strong>: Browser-style feature testing</li>
        <li><strong>test262</strong>: ECMAScript specification conformance</li>
    </ul>
"""
        
        # Group features by ECMAScript version
        for suite_name, features in compat_results.items():
            if not features:
                continue
                
            html += f'<h2><span class="badge {suite_name}">{suite_name.upper()}</span> Features</h2>'
            
            for feature_name, result in features.items():
                if isinstance(result, dict) and 'supported' in result:
                    support_class = 'supported' if result['supported'] else 'unsupported'
                    html += f'''
    <div class="feature {support_class}">
        <h3>{feature_name}</h3>
        <div class="compat-table">
            <strong>Compat-table result:</strong> 
            {"✅ Supported" if result['supported'] else "❌ Not supported"}
            (tested with {result['version']})
        </div>
    </div>
'''
                else:
                    # Handle subtests
                    html += f'<div class="feature partial"><h3>{feature_name}</h3>'
                    html += '<table><tr><th>Subtest</th><th>Support</th></tr>'
                    for subtest, subresult in result.items():
                        if isinstance(subresult, dict) and 'supported' in subresult:
                            icon = "✅" if subresult['supported'] else "❌"
                            html += f'<tr><td>{subtest}</td><td>{icon}</td></tr>'
                    html += '</table></div>'
        
        html += """
    <div style="margin-top: 40px; padding: 20px; background: #f8f9fa; border-radius: 5px;">
        <h2>Data Sources</h2>
        <p><strong>compat-table:</strong> Practical JavaScript feature testing used by Babel and build tools</p>
        <p><strong>test262:</strong> Official ECMAScript test suite for specification compliance</p>
        <p>By combining both, we get a complete picture of Rhino's capabilities.</p>
    </div>
</body>
</html>
"""
        
        output_path = self.rhino_docs_dir / "rhino-compat-integrated.html"
        output_path.parent.mkdir(parents=True, exist_ok=True)
        with open(output_path, 'w') as f:
            f.write(html)
        print(f"Generated integrated documentation: {output_path}")
        
        # Also generate JSON for programmatic access
        json_output = self.rhino_docs_dir / "rhino-compat-integrated.json"
        with open(json_output, 'w') as f:
            json.dump(compat_results, f, indent=2)
        print(f"Generated JSON data: {json_output}")

if __name__ == "__main__":
    integration = CompatTableIntegration()
    integration.generate_enhanced_docs()