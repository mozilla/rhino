#!/usr/bin/env python3
"""
Feature Tracker for Rhino JavaScript Engine

This script analyzes test262.properties to generate comprehensive feature documentation.
It creates a maintainable, auto-updated feature compatibility list.

Usage: python3 tools/feature_tracker.py [--update-docs]
"""

import re
import json
import sys
from datetime import datetime
from pathlib import Path

# Load ES Feature Categories from external file
def load_feature_categories():
    """Load ES feature mapping from external text file"""
    txt_path = Path(__file__).parent / 'es-features.txt'
    feature_categories = {}
    
    try:
        with open(txt_path, 'r') as f:
            for line in f:
                line = line.strip()
                # Skip comments and empty lines
                if not line or line.startswith('#'):
                    continue
                    
                # Parse pipe-delimited format: ES_VERSION|Feature Name|test262_path
                parts = line.split('|')
                if len(parts) == 3:
                    version, feature_name, test_path = [part.strip() for part in parts]
                    if version not in feature_categories:
                        feature_categories[version] = {}
                    feature_categories[version][feature_name] = test_path
        
        return feature_categories
    except FileNotFoundError:
        # Fallback to minimal hardcoded set if file not found
        return {
            'ES2015': {
                'Promises': 'built-ins/Promise'
            }
        }

# Legacy hardcoded categories for fallback
FEATURE_CATEGORIES = {
    'ES2015': {
        'Arrow Functions': 'language/expressions/arrow-function',
        'Classes': 'language/statements/class',
        'Template Literals': 'language/expressions/template-literal',
        'Destructuring': 'language/expressions/assignment/destructuring',
        'Default Parameters': 'language/expressions/function/default-parameters',
        'Rest Parameters': 'language/expressions/function/rest-parameters',
        'Spread Operator': 'language/expressions/spread',
        'for...of Loop': 'language/statements/for-of',
        'Symbols': 'built-ins/Symbol',
        'Generators': 'built-ins/GeneratorFunction',
        'Promises': 'built-ins/Promise',
        'Map': 'built-ins/Map',
        'Set': 'built-ins/Set',
        'WeakMap': 'built-ins/WeakMap',
        'WeakSet': 'built-ins/WeakSet',
        'Proxy': 'built-ins/Proxy',
        'Reflect': 'built-ins/Reflect',
        'Array.from': 'built-ins/Array/from',
        'Array.of': 'built-ins/Array/of',
        'Object.assign': 'built-ins/Object/assign'
    },
    'ES2016': {
        'Exponentiation Operator': 'language/expressions/exponentiation',
        'Array.includes': 'built-ins/Array/prototype/includes'
    },
    'ES2017': {
        'async/await': 'language/expressions/async-function',
        'Object.values': 'built-ins/Object/values',
        'Object.entries': 'built-ins/Object/entries',
        'Object.getOwnPropertyDescriptors': 'built-ins/Object/getOwnPropertyDescriptors',
        'String.padStart': 'built-ins/String/prototype/padStart',
        'String.padEnd': 'built-ins/String/prototype/padEnd',
        'Trailing Commas': 'language/expressions/trailing-comma'
    },
    'ES2018': {
        'Async Iterators': 'built-ins/AsyncIteratorPrototype',
        'Promise.finally': 'built-ins/Promise/prototype/finally',
        'Rest/Spread Properties': 'language/expressions/object/rest-spread',
        'RegExp Named Groups': 'built-ins/RegExp/named-groups',
        'RegExp Lookbehind': 'built-ins/RegExp/lookbehind',
        'RegExp s flag': 'built-ins/RegExp/dotAll'
    },
    'ES2019': {
        'Array.flat': 'built-ins/Array/prototype/flat',
        'Array.flatMap': 'built-ins/Array/prototype/flatMap',
        'Object.fromEntries': 'built-ins/Object/fromEntries',
        'String.trimStart': 'built-ins/String/prototype/trimStart',
        'String.trimEnd': 'built-ins/String/prototype/trimEnd',
        'Symbol.description': 'built-ins/Symbol/prototype/description',
        'JSON.stringify': 'built-ins/JSON/stringify'
    },
    'ES2020': {
        'BigInt': 'built-ins/BigInt',
        'BigInt64Array': 'built-ins/BigInt64Array',
        'BigUint64Array': 'built-ins/BigUint64Array',
        'Promise.allSettled': 'built-ins/Promise/allSettled',
        'globalThis': 'built-ins/global',
        'Optional Chaining (?.)': 'language/expressions/optional-chaining',
        'Nullish Coalescing (??)': 'language/expressions/coalesce',
        'String.matchAll': 'built-ins/String/prototype/matchAll',
        'Dynamic Import': 'language/expressions/dynamic-import',
        'import.meta': 'language/expressions/import.meta'
    },
    'ES2021': {
        'String.replaceAll': 'built-ins/String/prototype/replaceAll',
        'Promise.any': 'built-ins/Promise/any',
        'WeakRef': 'built-ins/WeakRef',
        'FinalizationRegistry': 'built-ins/FinalizationRegistry',
        'Logical Assignment (&&=)': 'language/expressions/logical-assignment',
        'Numeric Separators': 'language/expressions/numeric-separator'
    },
    'ES2022': {
        'Class Fields': 'language/statements/class/fields',
        'Private Methods': 'language/statements/class/private-methods',
        'Static Class Fields': 'language/statements/class/static',
        'Top-level await': 'language/module-code/top-level-await',
        'Error.cause': 'built-ins/Error/cause',
        'Array.at': 'built-ins/Array/prototype/at',
        'String.at': 'built-ins/String/prototype/at',
        'Object.hasOwn': 'built-ins/Object/hasOwn',
        'RegExp d flag': 'built-ins/RegExp/match-indices'
    },
    'ES2023': {
        'Array.findLast': 'built-ins/Array/prototype/findLast',
        'Array.findLastIndex': 'built-ins/Array/prototype/findLastIndex',
        'Array.toReversed': 'built-ins/Array/prototype/toReversed',
        'Array.toSorted': 'built-ins/Array/prototype/toSorted',
        'Array.toSpliced': 'built-ins/Array/prototype/toSpliced',
        'Array.with': 'built-ins/Array/prototype/with',
        'Hashbang Grammar': 'language/comments/hashbang'
    },
    'ES2024': {
        'Array.fromAsync': 'built-ins/Array/fromAsync',
        'Promise.withResolvers': 'built-ins/Promise/withResolvers',
        'Object.groupBy': 'built-ins/Object/groupBy',
        'Map.groupBy': 'built-ins/Map/groupBy',
        'String.isWellFormed': 'built-ins/String/prototype/isWellFormed',
        'String.toWellFormed': 'built-ins/String/prototype/toWellFormed',
        'ArrayBuffer.transfer': 'built-ins/ArrayBuffer/prototype/transfer',
        'RegExp v flag': 'built-ins/RegExp/unicode-sets'
    },
    'ES2025': {
        'Iterator Helpers': 'built-ins/Iterator',
        'Set Methods': 'built-ins/Set/prototype',
        'Promise.try': 'built-ins/Promise/try',
        'Math.sumPrecise': 'built-ins/Math/sumPrecise',
        'Math.f16round': 'built-ins/Math/f16round',
        'Error.isError': 'built-ins/Error/isError',
        'RegExp.escape': 'built-ins/RegExp/escape',
        'Temporal': 'built-ins/Temporal'
    }
}

class FeatureTracker:
    def __init__(self):
        self.base_dir = Path(__file__).parent.parent
        self.test262_properties = self.base_dir / 'tests' / 'testsrc' / 'test262.properties'
        self.features_output = self.base_dir / 'FEATURES.md'
        self.json_output = self.base_dir / 'rhino-features.json'
        self.results = {}
    
    def parse_test262_properties(self):
        """Parse test262.properties to extract test results"""
        with open(self.test262_properties, 'r') as f:
            content = f.read()
        
        lines = content.split('\n')
        current_path = ''
        is_disabled = False
        
        for line in lines:
            trimmed = line.strip()
            
            # Skip comments and empty lines
            if not trimmed or trimmed.startswith('#'):
                continue
            
            # Check if entire suite is disabled
            if trimmed.startswith('~'):
                is_disabled = True
                current_path = trimmed[1:].strip()
                self.results[current_path] = {
                    'passed': 0,
                    'total': 0,
                    'pass_rate': 0,
                    'disabled': True
                }
                continue
            
            # Parse test suite with results: "built-ins/Array 268/3077 (8.71%)"
            match = re.match(r'^([\w\-\/]+)\s+(\d+)\/(\d+)\s+\((.+)%\)', trimmed)
            if match:
                current_path = match.group(1)
                failed = int(match.group(2))
                total = int(match.group(3))
                fail_rate = float(match.group(4))
                
                # Calculate passed count
                passed = total - failed
                pass_rate = 100 - fail_rate
                
                self.results[current_path] = {
                    'passed': passed,
                    'total': total,
                    'pass_rate': pass_rate,
                    'disabled': False
                }
                is_disabled = False
                continue
            
            # Simple path without results
            if re.match(r'^[\w\-\/]+$', trimmed):
                current_path = trimmed
                if not is_disabled and current_path not in self.results:
                    # Assume it's all passing if no failure count given
                    self.results[current_path] = {
                        'passed': 0,
                        'total': 0,
                        'pass_rate': 100,
                        'disabled': False
                    }
    
    def find_test_support(self, test_path):
        """Find test support for a given path"""
        # Direct match
        if test_path in self.results:
            return self.results[test_path]
        
        # Check for partial path matches
        for path, result in self.results.items():
            if test_path in path or path in test_path:
                return result
        
        # Check for parent path
        parts = test_path.split('/')
        while len(parts) > 1:
            parts.pop()
            parent = '/'.join(parts)
            if parent in self.results:
                return self.results[parent]
        
        # No results found
        return {
            'passed': 0,
            'total': 0,
            'pass_rate': 0,
            'disabled': False,
            'not_found': True
        }
    
    def get_status(self, pass_rate):
        """Get status based on pass rate"""
        if pass_rate >= 95:
            return 'Full'
        elif pass_rate >= 75:
            return 'Mostly'
        elif pass_rate >= 25:
            return 'Partial'
        elif pass_rate > 0:
            return 'Limited'
        else:
            return 'None'
    
    def calculate_feature_support(self):
        """Calculate feature support based on test262 results"""
        feature_support = {}
        feature_categories = load_feature_categories()
        
        for version, features in feature_categories.items():
            feature_support[version] = {}
            
            for feature_name, test_path in features.items():
                support = self.find_test_support(test_path)
                feature_support[version][feature_name] = {
                    'path': test_path,
                    **support,
                    'status': self.get_status(support['pass_rate'])
                }
        
        return feature_support
    
    def generate_markdown(self, feature_support):
        """Generate markdown documentation"""
        today = datetime.now().strftime('%Y-%m-%d')
        
        markdown = f"""# Rhino ECMAScript Feature Support
*Auto-generated from test262 results on {today}*

## Overview

This document provides a comprehensive list of ECMAScript features and their support status in Rhino.
It is automatically generated from test262 test suite results.

## Feature Support Legend

- **Full Support** (95-100% tests passing)
- **Mostly Supported** (75-94% tests passing)
- **Partial Support** (25-74% tests passing)
- **Limited Support** (1-24% tests passing)
- **Not Supported** (0% tests passing)

## Summary by ECMAScript Version

| Version | Full | Mostly | Partial | Limited | None | Overall |
|---------|------|--------|---------|---------|------|---------|
"""
        
        # Calculate summary
        for version, features in feature_support.items():
            counts = {'full': 0, 'mostly': 0, 'partial': 0, 'limited': 0, 'none': 0}
            total_pass_rate = 0
            feature_count = 0
            
            for feature in features.values():
                if 'Full' in feature['status']:
                    counts['full'] += 1
                elif 'Mostly' in feature['status']:
                    counts['mostly'] += 1
                elif 'Partial' in feature['status']:
                    counts['partial'] += 1
                elif 'Limited' in feature['status']:
                    counts['limited'] += 1
                else:
                    counts['none'] += 1
                
                total_pass_rate += feature.get('pass_rate', 0)
                feature_count += 1
            
            avg_pass_rate = total_pass_rate / feature_count if feature_count > 0 else 0
            markdown += f"| {version} | {counts['full']} | {counts['mostly']} | {counts['partial']} | {counts['limited']} | {counts['none']} | {avg_pass_rate:.1f}% |\n"
        
        # Detailed features by version
        for version, features in feature_support.items():
            markdown += f"\n## {version} Features\n\n"
            markdown += "| Feature | Status | Pass Rate | Tests |\n"
            markdown += "|---------|--------|-----------|-------|\n"
            
            # Sort by pass rate
            sorted_features = sorted(features.items(), key=lambda x: x[1]['pass_rate'], reverse=True)
            
            for name, info in sorted_features:
                tests = f"{info['passed']}/{info['total']}" if info['total'] > 0 else "N/A"
                pass_rate = f"{info['pass_rate']:.1f}%" if 'pass_rate' in info else "0%"
                
                markdown += f"| {name} | {info['status']} | {pass_rate} | {tests} |\n"
        
        # Working features section
        markdown += "\n## Fully Working Modern Features\n\n"
        markdown += "These ES2017+ features have >95% test262 pass rate:\n\n"
        
        for version in ['ES2017', 'ES2018', 'ES2019', 'ES2020', 'ES2021', 'ES2022', 'ES2023', 'ES2024', 'ES2025']:
            if version not in feature_support:
                continue
            
            working = [name for name, info in feature_support[version].items() 
                      if info['pass_rate'] >= 95]
            
            if working:
                markdown += f"### {version}\n"
                for feature in working:
                    markdown += f"- {feature}\n"
                markdown += "\n"
        
        # Not supported section
        markdown += "\n## Not Supported Features\n\n"
        markdown += "These features have 0% test262 pass rate or are disabled:\n\n"
        
        for version, features in feature_support.items():
            not_supported = [name for name, info in features.items() 
                           if info['pass_rate'] == 0 or info.get('disabled', False)]
            
            if not_supported:
                markdown += f"### {version}\n"
                for feature in not_supported:
                    markdown += f"- {feature}\n"
                markdown += "\n"
        
        markdown += """
## Maintenance

This document is automatically generated by `tools/feature_tracker.py`.
To update:

```bash
# Update test262 results
./gradlew test -DupdateTest262properties=all

# Regenerate this document
python3 tools/feature_tracker.py --update-docs
```

## Contributing

To add support for a new feature:
1. Implement the feature in Rhino
2. Run test262 tests
3. Update test262.properties
4. Regenerate this document

## See Also

- [test262.properties](tests/testsrc/test262.properties) - Raw test results
- [rhino-features.json](rhino-features.json) - Machine-readable feature data
"""
        
        return markdown
    
    def generate_json(self, feature_support):
        """Generate JSON output for programmatic access"""
        output = {
            'generated': datetime.now().isoformat(),
            'rhino_version': self.get_rhino_version(),
            'features': feature_support,
            'summary': self.generate_summary(feature_support)
        }
        
        return json.dumps(output, indent=2)
    
    def generate_html_json(self, feature_support):
        """Generate JSON embedded in HTML for web integration"""
        json_data = self.generate_json(feature_support)
        summary = self.generate_summary(feature_support)
        
        html_template = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Rhino JavaScript Engine - ECMAScript Feature Support</title>
    <style>
        body {{ font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', system-ui, sans-serif; }}
        .container {{ max-width: 1200px; margin: 0 auto; padding: 20px; }}
        .summary {{ background: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0; }}
        .feature-grid {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px; }}
        .feature-card {{ border: 1px solid #e1e5e9; border-radius: 8px; padding: 16px; }}
        .status-full {{ background: #d4edda; }}
        .status-mostly {{ background: #d1ecf1; }}
        .status-partial {{ background: #fff3cd; }}
        .status-limited {{ background: #f8d7da; }}
        .status-none {{ background: #f8d7da; }}
        .progress-bar {{ width: 100%; height: 20px; background: #e9ecef; border-radius: 10px; overflow: hidden; }}
        .progress-fill {{ height: 100%; transition: width 0.3s ease; }}
        .progress-full {{ background: #28a745; }}
        .progress-mostly {{ background: #17a2b8; }}
        .progress-partial {{ background: #ffc107; }}
        .progress-limited {{ background: #dc3545; }}
        .progress-none {{ background: #dc3545; }}
        pre {{ background: #f8f9fa; padding: 15px; border-radius: 5px; overflow: auto; }}
    </style>
</head>
<body>
    <div class="container">
        <h1>Rhino JavaScript Engine - ECMAScript Feature Support</h1>
        <p><em>Generated on {datetime.now().strftime('%Y-%m-%d at %H:%M:%S UTC')}</em></p>
        
        <div class="summary">
            <h2>Summary</h2>
            <p><strong>Total Features:</strong> {summary['total_features']}</p>
            <p><strong>Fully Supported:</strong> {summary['fully_supported']} ({summary['fully_supported']/summary['total_features']*100:.1f}%)</p>
            <p><strong>Partially Supported:</strong> {summary['partially_supported']} ({summary['partially_supported']/summary['total_features']*100:.1f}%)</p>
            <p><strong>Not Supported:</strong> {summary['not_supported']} ({summary['not_supported']/summary['total_features']*100:.1f}%)</p>
        </div>
        
        <div id="feature-data" style="display: none;">
            <script type="application/json">
{json_data}
            </script>
        </div>
        
        <p><strong>Raw JSON Data:</strong> <button onclick="document.getElementById('raw-json').style.display = document.getElementById('raw-json').style.display === 'none' ? 'block' : 'none'">Toggle</button></p>
        <pre id="raw-json" style="display: none;">{json_data}</pre>
        
        <script>
            // Feature data is available in the script tag above
            const featureData = JSON.parse(document.querySelector('#feature-data script').textContent);
            console.log('Rhino Feature Data:', featureData);
            
            // Add interactive features here
            window.rhinoFeatureData = featureData;
        </script>
    </div>
</body>
</html>"""
        
        return html_template
    
    def get_rhino_version(self):
        """Get Rhino version from gradle.properties"""
        try:
            gradle_props = self.base_dir / 'gradle.properties'
            with open(gradle_props, 'r') as f:
                for line in f:
                    if line.startswith('version='):
                        return line.split('=')[1].strip()
        except:
            pass
        return 'unknown'
    
    def generate_summary(self, feature_support):
        """Generate summary statistics"""
        summary = {
            'total_features': 0,
            'fully_supported': 0,
            'partially_supported': 0,
            'not_supported': 0,
            'by_version': {}
        }
        
        for version, features in feature_support.items():
            version_stats = {
                'total': 0,
                'supported': 0,
                'partial': 0,
                'none': 0,
                'avg_pass_rate': 0
            }
            
            for feature in features.values():
                summary['total_features'] += 1
                version_stats['total'] += 1
                
                if feature['pass_rate'] >= 95:
                    summary['fully_supported'] += 1
                    version_stats['supported'] += 1
                elif feature['pass_rate'] > 0:
                    summary['partially_supported'] += 1
                    version_stats['partial'] += 1
                else:
                    summary['not_supported'] += 1
                    version_stats['none'] += 1
                
                version_stats['avg_pass_rate'] += feature.get('pass_rate', 0)
            
            if version_stats['total'] > 0:
                version_stats['avg_pass_rate'] /= version_stats['total']
            
            summary['by_version'][version] = version_stats
        
        return summary
    
    def run(self, update_docs=False):
        """Run the feature tracker"""
        print('Parsing test262.properties...')
        self.parse_test262_properties()
        
        print('Calculating feature support...')
        feature_support = self.calculate_feature_support()
        
        print('Generating documentation...')
        markdown = self.generate_markdown(feature_support)
        json_output = self.generate_json(feature_support)
        html_output = self.generate_html_json(feature_support)
        
        if update_docs:
            print('Writing FEATURES.md...')
            with open(self.features_output, 'w') as f:
                f.write(markdown)
            
            print('Writing rhino-features.json...')
            with open(self.json_output, 'w') as f:
                f.write(json_output)
                
            print('Writing rhino-features.html...')
            html_output_path = self.base_dir / 'rhino-features.html'
            with open(html_output_path, 'w') as f:
                f.write(html_output)
            
            print('Documentation updated successfully!')
            print('Generated files:')
            print('  - FEATURES.md (human-readable documentation)')
            print('  - rhino-features.json (machine-readable data)')
            print('  - rhino-features.html (web-ready interactive page)')
        else:
            print('\nPreview (first 50 lines):')
            print('=' * 60)
            print('\n'.join(markdown.split('\n')[:50]))
            print('=' * 60)
            print('\nRun with --update-docs to write files')
        
        # Print summary
        summary = self.generate_summary(feature_support)
        total = summary['total_features']
        if total > 0:
            print('\nSummary:')
            print(f"   Total Features: {total}")
            print(f"   Fully Supported: {summary['fully_supported']} ({summary['fully_supported']/total*100:.1f}%)")
            print(f"   Partially Supported: {summary['partially_supported']} ({summary['partially_supported']/total*100:.1f}%)")
            print(f"   Not Supported: {summary['not_supported']} ({summary['not_supported']/total*100:.1f}%)")

if __name__ == '__main__':
    update_docs = '--update-docs' in sys.argv
    tracker = FeatureTracker()
    tracker.run(update_docs)