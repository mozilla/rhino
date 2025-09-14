#!/usr/bin/env python3
"""
Rhino Feature Tracker - Automatically documents ALL test262 results
Parses test262.properties and generates comprehensive feature documentation
"""

import re
import json
from pathlib import Path
from datetime import datetime
from collections import defaultdict

class ComprehensiveFeatureTracker:
    def __init__(self):
        self.base_dir = Path(__file__).parent.parent
        self.test262_properties = self.base_dir / 'tests' / 'testsrc' / 'test262.properties'
        self.features_output = self.base_dir / 'FEATURES.md'
        self.json_output = self.base_dir / 'rhino-features.json'
        self.html_output = self.base_dir / 'rhino-features.html'
        self.all_features = defaultdict(dict)
        
    def parse_test262_properties(self):
        """Parse ALL entries from test262.properties"""
        print("Parsing test262.properties comprehensively...")
        
        with open(self.test262_properties, 'r') as f:
            lines = f.readlines()
        
        current_category = None
        current_path = None
        
        for line in lines:
            line = line.strip()
            
            # Skip comments and empty lines
            if not line or line.startswith('#'):
                continue
            
            # Check if line is disabled (starts with ~)
            is_disabled = line.startswith('~')
            if is_disabled:
                line = line[1:].strip()
            
            # Parse test result line: "path/to/test 123/456 (78.90%)"
            match = re.match(r'^([\w\-\/\.]+)\s+(\d+)\/(\d+)\s+\((.+)%\)', line)
            if match:
                path = match.group(1)
                failed = int(match.group(2))
                total = int(match.group(3))
                fail_rate = float(match.group(4))
                
                # Calculate pass statistics
                passed = total - failed
                pass_rate = 100 - fail_rate
                
                # Categorize by top-level path
                category = self._categorize_path(path)
                
                self.all_features[category][path] = {
                    'passed': passed,
                    'total': total,
                    'pass_rate': pass_rate,
                    'disabled': is_disabled,
                    'path': path
                }
                continue
            
            # Parse path-only lines
            if re.match(r'^[\w\-\/\.]+$', line):
                path = line
                category = self._categorize_path(path)
                
                # If no stats provided, assume it's a category header
                if path not in self.all_features[category]:
                    self.all_features[category][path] = {
                        'passed': 0,
                        'total': 0,
                        'pass_rate': 0 if is_disabled else 100,
                        'disabled': is_disabled,
                        'path': path
                    }
    
    def _categorize_path(self, path):
        """Categorize test paths into logical groups"""
        if path.startswith('built-ins/'):
            # Further categorize built-ins
            parts = path.split('/')
            if len(parts) >= 2:
                object_name = parts[1]
                if object_name in ['Array', 'String', 'Object', 'Number', 'Boolean']:
                    return f'Core Objects - {object_name}'
                elif object_name in ['Promise', 'Symbol', 'Proxy', 'Reflect']:
                    return 'ES6+ Objects'
                elif object_name in ['Map', 'Set', 'WeakMap', 'WeakSet']:
                    return 'Collections'
                elif object_name in ['Math', 'Date', 'RegExp', 'JSON']:
                    return 'Built-in Objects'
                elif object_name.startswith('Intl'):
                    return 'Internationalization'
                else:
                    return 'Other Built-ins'
            return 'Built-ins'
        elif path.startswith('language/'):
            # Categorize language features
            if 'expressions' in path:
                return 'Language - Expressions'
            elif 'statements' in path:
                return 'Language - Statements'
            elif 'types' in path:
                return 'Language - Types'
            else:
                return 'Language Features'
        elif path.startswith('annexB/'):
            return 'Annex B (Legacy)'
        elif path.startswith('intl402/'):
            return 'Internationalization (Intl402)'
        elif path.startswith('harness/'):
            return 'Test Harness'
        else:
            return 'Other'
    
    def generate_markdown(self):
        """Generate comprehensive Markdown documentation"""
        lines = []
        lines.append('# Rhino Feature Documentation')
        lines.append(f'*Auto-generated from test262.properties on {datetime.now().strftime("%Y-%m-%d")}*')
        lines.append('')
        lines.append('## Overview')
        lines.append('')
        lines.append('This document provides comprehensive documentation of ALL features tested in Rhino.')
        lines.append('Data is extracted directly from test262.properties, showing actual test pass rates.')
        lines.append('')
        
        # Calculate summary statistics
        total_features = 0
        total_passed = 0
        total_tests = 0
        
        for category, features in self.all_features.items():
            for path, data in features.items():
                total_features += 1
                total_passed += data['passed']
                total_tests += data['total']
        
        overall_pass_rate = (total_passed / total_tests * 100) if total_tests > 0 else 0
        
        lines.append('## Summary Statistics')
        lines.append('')
        lines.append(f'- **Total Test Suites**: {total_features}')
        lines.append(f'- **Total Tests**: {total_tests:,}')
        lines.append(f'- **Tests Passed**: {total_passed:,}')
        lines.append(f'- **Overall Pass Rate**: {overall_pass_rate:.1f}%')
        lines.append('')
        
        # Generate section for each category
        for category in sorted(self.all_features.keys()):
            features = self.all_features[category]
            
            lines.append(f'## {category}')
            lines.append('')
            
            # Calculate category statistics
            cat_total = sum(f['total'] for f in features.values())
            cat_passed = sum(f['passed'] for f in features.values())
            cat_rate = (cat_passed / cat_total * 100) if cat_total > 0 else 0
            
            lines.append(f'**Category Pass Rate**: {cat_rate:.1f}% ({cat_passed:,}/{cat_total:,} tests)')
            lines.append('')
            
            # Create table for features
            lines.append('| Feature | Pass Rate | Tests Passed | Status |')
            lines.append('|---------|-----------|--------------|--------|')
            
            # Sort features by pass rate (descending)
            sorted_features = sorted(features.items(), 
                                    key=lambda x: x[1]['pass_rate'], 
                                    reverse=True)
            
            for path, data in sorted_features[:50]:  # Show top 50 per category
                status = self._get_status(data['pass_rate'])
                status_badge = self._get_status_badge(status)
                
                # Clean up path for display
                display_name = path.split('/')[-1] if '/' in path else path
                
                lines.append(f"| {display_name} | {data['pass_rate']:.1f}% | "
                           f"{data['passed']}/{data['total']} | {status_badge} |")
            
            if len(features) > 50:
                lines.append(f'| ... and {len(features) - 50} more | | | |')
            
            lines.append('')
        
        return '\n'.join(lines)
    
    def _get_status(self, pass_rate):
        """Determine status based on pass rate"""
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
    
    def _get_status_badge(self, status):
        """Get status badge for markdown"""
        badges = {
            'Full': '✅ Full',
            'Mostly': '🟢 Mostly',
            'Partial': '🟡 Partial',
            'Limited': '🟠 Limited',
            'None': '❌ None'
        }
        return badges.get(status, status)
    
    def generate_json(self):
        """Generate comprehensive JSON data"""
        data = {
            'generated': datetime.now().isoformat(),
            'rhino_version': self._get_rhino_version(),
            'categories': {}
        }
        
        for category, features in self.all_features.items():
            cat_data = {
                'features': {},
                'statistics': {
                    'total_features': len(features),
                    'total_tests': sum(f['total'] for f in features.values()),
                    'passed_tests': sum(f['passed'] for f in features.values())
                }
            }
            
            for path, feature_data in features.items():
                cat_data['features'][path] = feature_data
            
            data['categories'][category] = cat_data
        
        return json.dumps(data, indent=2)
    
    def generate_html(self):
        """Generate fully static HTML dashboard with pre-rendered content"""
        # Calculate statistics
        total_features = 0
        total_passed = 0
        total_tests = 0
        
        for category, features in self.all_features.items():
            for path, data in features.items():
                total_features += 1
                total_passed += data['passed']
                total_tests += data['total']
        
        overall_pass_rate = (total_passed / total_tests * 100) if total_tests > 0 else 0
        
        # Generate stats cards HTML
        stats_html = f"""
        <div class="stat-card">
            <h3>Total Categories</h3>
            <div style="font-size: 2em; font-weight: bold;">{len(self.all_features)}</div>
        </div>
        <div class="stat-card">
            <h3>Total Test Suites</h3>
            <div style="font-size: 2em; font-weight: bold;">{total_features:,}</div>
        </div>
        <div class="stat-card">
            <h3>Total Tests</h3>
            <div style="font-size: 2em; font-weight: bold;">{total_tests:,}</div>
        </div>
        <div class="stat-card">
            <h3>Overall Pass Rate</h3>
            <div style="font-size: 2em; font-weight: bold;">{overall_pass_rate:.1f}%</div>
            <div class="progress-bar" style="margin-top: 10px;">
                <div class="progress-fill" style="width: {overall_pass_rate:.1f}%"></div>
            </div>
        </div>
        """
        
        # Generate category sections HTML
        categories_html = ""
        for category in sorted(self.all_features.keys()):
            features = self.all_features[category]
            
            # Calculate category statistics
            cat_total = sum(f['total'] for f in features.values())
            cat_passed = sum(f['passed'] for f in features.values())
            cat_rate = (cat_passed / cat_total * 100) if cat_total > 0 else 0
            
            # Generate table rows for ALL features
            sorted_features = sorted(features.items(), 
                                    key=lambda x: x[1]['pass_rate'], 
                                    reverse=True)
            
            table_rows = ""
            for path, feat in sorted_features:
                table_rows += f"""
                    <tr>
                        <td>{path}</td>
                        <td>{feat['pass_rate']:.1f}%</td>
                        <td>{feat['passed']}/{feat['total']}</td>
                    </tr>
                """
            
            categories_html += f"""
            <div class="category-section" data-category="{category.lower()}">
                <h2>{category}</h2>
                <p>{len(features)} features | {cat_total:,} tests | {cat_rate:.1f}% pass rate</p>
                <div class="progress-bar">
                    <div class="progress-fill" style="width: {cat_rate:.1f}%"></div>
                </div>
                <details>
                    <summary>View Details</summary>
                    <table>
                        <thead>
                            <tr>
                                <th>Feature</th>
                                <th>Pass Rate</th>
                                <th>Tests</th>
                            </tr>
                        </thead>
                        <tbody>
                            {table_rows}
                        </tbody>
                    </table>
                </details>
            </div>
            """
        
        html = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Rhino Comprehensive Feature Dashboard</title>
    <style>
        body {{ 
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            margin: 0;
            padding: 20px;
            background: #f5f5f5;
        }}
        .container {{ max-width: 1400px; margin: 0 auto; }}
        .header {{ 
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 30px;
            border-radius: 10px;
            margin-bottom: 30px;
        }}
        .stats-grid {{
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 20px;
            margin-bottom: 30px;
        }}
        .stat-card {{
            background: white;
            padding: 20px;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }}
        .stat-card h3 {{
            margin: 0 0 10px 0;
            color: #666;
        }}
        .category-section {{
            background: white;
            padding: 20px;
            border-radius: 10px;
            margin-bottom: 20px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }}
        .progress-bar {{
            width: 100%;
            height: 20px;
            background: #e0e0e0;
            border-radius: 10px;
            overflow: hidden;
            margin-top: 10px;
        }}
        .progress-fill {{
            height: 100%;
            background: linear-gradient(90deg, #4caf50, #8bc34a);
        }}
        table {{
            width: 100%;
            border-collapse: collapse;
            margin-top: 15px;
        }}
        th, td {{
            padding: 10px;
            text-align: left;
            border-bottom: 1px solid #e0e0e0;
        }}
        th {{
            background: #f5f5f5;
            font-weight: 600;
        }}
        .search-box {{
            padding: 10px;
            width: 100%;
            border: 1px solid #ddd;
            border-radius: 5px;
            margin-bottom: 20px;
            box-sizing: border-box;
        }}
        details {{
            margin-top: 15px;
        }}
        summary {{
            cursor: pointer;
            padding: 10px;
            background: #f5f5f5;
            border-radius: 5px;
            margin-bottom: 10px;
        }}
        summary:hover {{
            background: #e8e8e8;
        }}
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>Rhino Comprehensive Feature Dashboard</h1>
            <p>Complete test262 results - All {len(self.all_features)} categories documented</p>
            <p>Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}</p>
        </div>
        
        <input type="text" class="search-box" id="searchBox" placeholder="Search features..." />
        
        <div class="stats-grid" id="statsGrid">
            {stats_html}
        </div>
        
        <div id="categorySections">
            {categories_html}
        </div>
    </div>
    
    <script>
        // Simple search functionality
        document.getElementById('searchBox').addEventListener('input', function(e) {{
            const searchTerm = e.target.value.toLowerCase();
            const sections = document.querySelectorAll('.category-section');
            
            sections.forEach(section => {{
                const category = section.getAttribute('data-category');
                if (searchTerm === '' || category.includes(searchTerm)) {{
                    section.style.display = 'block';
                }} else {{
                    section.style.display = 'none';
                }}
            }});
        }});
    </script>
</body>
</html>"""
        
        return html
    
    def _get_rhino_version(self):
        """Get Rhino version from gradle.properties"""
        try:
            gradle_props = self.base_dir / 'gradle.properties'
            with open(gradle_props, 'r') as f:
                for line in f:
                    if line.startswith('version='):
                        return line.split('=')[1].strip()
        except:
            pass
        return '1.8.1-SNAPSHOT'
    
    def run(self):
        """Run the comprehensive feature tracker"""
        print("Starting comprehensive feature documentation...")
        
        # Parse ALL test262 results
        self.parse_test262_properties()
        
        print(f"Found {len(self.all_features)} categories")
        total_features = sum(len(features) for features in self.all_features.values())
        print(f"Documenting {total_features} total test suites")
        
        # Generate outputs
        print("Generating Markdown...")
        markdown = self.generate_markdown()
        with open(self.features_output, 'w') as f:
            f.write(markdown)
        
        print("Generating JSON...")
        json_data = self.generate_json()
        with open(self.json_output, 'w') as f:
            f.write(json_data)
        
        print("Generating HTML...")
        html = self.generate_html()
        with open(self.html_output, 'w') as f:
            f.write(html)
        
        print("Documentation complete!")
        print(f"Generated files:")
        print(f"  - {self.features_output}")
        print(f"  - {self.json_output}")
        print(f"  - {self.html_output}")

if __name__ == '__main__':
    tracker = ComprehensiveFeatureTracker()
    tracker.run()