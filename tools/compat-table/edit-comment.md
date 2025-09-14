# Edit for existing comment

Find this part in your comment:
```
Kangax compat-table:
- ~90 hand-picked features
```

Replace with:
```
Kangax compat-table:
- ~200-300 manually curated ECMAScript features
```

Or replace the entire comparison section with:

---

**Kangax compat-table strengths:**
- Tests approximately 200-300 manually curated ECMAScript features
- Cross-engine comparison
- High-level feature support overview

**Proposed test262-based system strengths:**
- Documents 3,025 test suites covering 35,407 individual tests
- Automated from actual test results
- Comprehensive coverage of ALL test262 suites
- Exact pass rates, not binary yes/no
- Updates automatically with every code change

These approaches complement each other well - compat-table for cross-engine comparison, our system for detailed Rhino capabilities.