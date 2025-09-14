**Correction to my previous comment:**

I need to clarify the scope of each approach:

**Kangax compat-table:**
- Tests approximately 200-300 manually curated ECMAScript features
- Uses custom JavaScript tests for cross-browser comparison
- Provides high-level yes/no/partial support indicators

**This PR's test262-based system:**
- Documents **3,025 test suites** covering **35,407 individual tests**
- Provides comprehensive coverage of ALL test262 features
- Shows exact pass rates (e.g., Array: 2816/3077 = 91.5%)
- Automatically updates with every code change

Both approaches complement each other well - compat-table excels at cross-engine comparison with its curated feature set, while our system provides exhaustive Rhino-specific documentation based on the official ECMAScript test suite compliance.