# Rhino ECMAScript Feature Support
*Auto-generated from test262 results on 2025-09-14*

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
| ES2015 | 3 | 7 | 8 | 0 | 2 | 65.4% |
| ES2016 | 1 | 1 | 0 | 0 | 0 | 94.6% |
| ES2017 | 5 | 0 | 0 | 0 | 2 | 68.5% |
| ES2018 | 0 | 0 | 5 | 0 | 1 | 37.3% |
| ES2019 | 2 | 3 | 1 | 0 | 1 | 75.1% |
| ES2020 | 2 | 3 | 2 | 0 | 3 | 56.1% |
| ES2021 | 1 | 0 | 2 | 0 | 3 | 30.7% |
| ES2022 | 2 | 2 | 1 | 0 | 4 | 46.4% |
| ES2023 | 0 | 7 | 0 | 0 | 0 | 90.3% |
| ES2024 | 3 | 4 | 2 | 0 | 0 | 81.3% |
| ES2025 | 2 | 2 | 2 | 1 | 1 | 56.8% |

## ES2015 Features

| Feature | Status | Pass Rate | Tests |
|---------|--------|-----------|-------|
| WeakSet | Full | 98.8% | 84/85 |
| Template Literals | Full | 96.5% | 55/57 |
| Object.assign | Full | 96.5% | 3289/3410 |
| Reflect | Mostly | 92.2% | 141/153 |
| Array.from | Mostly | 91.5% | 2816/3077 |
| Array.of | Mostly | 91.5% | 2816/3077 |
| Map | Mostly | 82.8% | 169/204 |
| Symbols | Mostly | 79.8% | 75/94 |
| Proxy | Mostly | 77.8% | 242/311 |
| Set | Mostly | 77.2% | 294/381 |
| WeakMap | Partial | 71.6% | 101/141 |
| Generators | Partial | 65.2% | 15/23 |
| Destructuring | Partial | 62.1% | 301/485 |
| Arrow Functions | Partial | 56.0% | 192/343 |
| Default Parameters | Partial | 43.6% | 115/264 |
| Rest Parameters | Partial | 43.6% | 115/264 |
| for...of Loop | Partial | 41.7% | 313/751 |
| Promises | Partial | 40.1% | 256/639 |
| Classes | None | 0.0% | N/A |
| Spread Operator | None | 0.0% | N/A |

## ES2016 Features

| Feature | Status | Pass Rate | Tests |
|---------|--------|-----------|-------|
| Exponentiation Operator | Full | 97.7% | 43/44 |
| Array.includes | Mostly | 91.5% | 2816/3077 |

## ES2017 Features

| Feature | Status | Pass Rate | Tests |
|---------|--------|-----------|-------|
| Object.values | Full | 96.5% | 3289/3410 |
| Object.entries | Full | 96.5% | 3289/3410 |
| Object.getOwnPropertyDescriptors | Full | 96.5% | 3289/3410 |
| String.padStart | Full | 95.2% | 1154/1212 |
| String.padEnd | Full | 95.2% | 1154/1212 |
| async/await | None | 0.0% | N/A |
| Trailing Commas | None | 0.0% | N/A |

## ES2018 Features

| Feature | Status | Pass Rate | Tests |
|---------|--------|-----------|-------|
| RegExp Named Groups | Partial | 47.8% | 893/1868 |
| RegExp Lookbehind | Partial | 47.8% | 893/1868 |
| RegExp s flag | Partial | 47.8% | 893/1868 |
| Rest/Spread Properties | Partial | 40.3% | 472/1170 |
| Promise.finally | Partial | 40.1% | 256/639 |
| Async Iterators | None | 0.0% | N/A |

## ES2019 Features

| Feature | Status | Pass Rate | Tests |
|---------|--------|-----------|-------|
| String.trimStart | Full | 95.2% | 1154/1212 |
| String.trimEnd | Full | 95.2% | 1154/1212 |
| Array.flat | Mostly | 91.5% | 2816/3077 |
| Array.flatMap | Mostly | 91.5% | 2816/3077 |
| Symbol.description | Mostly | 79.8% | 75/94 |
| JSON.stringify | Partial | 72.7% | 120/165 |
| Object.fromEntries | None | 0.0% | 0/19 |

## ES2020 Features

| Feature | Status | Pass Rate | Tests |
|---------|--------|-----------|-------|
| globalThis | Full | 100.0% | 29/29 |
| String.matchAll | Full | 95.2% | 1154/1212 |
| Nullish Coalescing | Mostly | 91.7% | 22/24 |
| BigInt | Mostly | 90.7% | 68/75 |
| BigInt64Array | Mostly | 90.7% | 68/75 |
| Optional Chaining | Partial | 52.6% | 20/38 |
| Promise.allSettled | Partial | 40.1% | 256/639 |
| BigUint64Array | None | 0.0% | N/A |
| Dynamic Import | None | 0.0% | N/A |
| import.meta | None | 0.0% | N/A |

## ES2021 Features

| Feature | Status | Pass Rate | Tests |
|---------|--------|-----------|-------|
| String.replaceAll | Full | 95.2% | 1154/1212 |
| Logical Assignment | Partial | 48.7% | 38/78 |
| Promise.any | Partial | 40.1% | 256/639 |
| WeakRef | None | 0.0% | N/A |
| FinalizationRegistry | None | 0.0% | N/A |
| Numeric Separators | None | 0.0% | N/A |

## ES2022 Features

| Feature | Status | Pass Rate | Tests |
|---------|--------|-----------|-------|
| Object.hasOwn | Full | 96.5% | 3289/3410 |
| String.at | Full | 95.2% | 1154/1212 |
| Array.at | Mostly | 91.5% | 2816/3077 |
| Error.cause | Mostly | 86.8% | 46/53 |
| RegExp d flag | Partial | 47.8% | 893/1868 |
| Class Fields | None | 0.0% | N/A |
| Private Methods | None | 0.0% | N/A |
| Static Class Fields | None | 0.0% | 0/14 |
| Top-level await | None | 0.0% | N/A |

## ES2023 Features

| Feature | Status | Pass Rate | Tests |
|---------|--------|-----------|-------|
| Array.findLast | Mostly | 91.5% | 2816/3077 |
| Array.findLastIndex | Mostly | 91.5% | 2816/3077 |
| Array.toReversed | Mostly | 91.5% | 2816/3077 |
| Array.toSorted | Mostly | 91.5% | 2816/3077 |
| Array.toSpliced | Mostly | 91.5% | 2816/3077 |
| Array.with | Mostly | 91.5% | 2816/3077 |
| Hashbang Grammar | Mostly | 82.7% | 43/52 |

## ES2024 Features

| Feature | Status | Pass Rate | Tests |
|---------|--------|-----------|-------|
| Object.groupBy | Full | 96.5% | 3289/3410 |
| String.isWellFormed | Full | 95.2% | 1154/1212 |
| String.toWellFormed | Full | 95.2% | 1154/1212 |
| Array.fromAsync | Mostly | 91.5% | 2816/3077 |
| ArrayBuffer.transfer | Mostly | 91.5% | 2816/3077 |
| ArrayBuffer.resize | Mostly | 91.5% | 2816/3077 |
| Map.groupBy | Mostly | 82.8% | 169/204 |
| RegExp v flag | Partial | 47.8% | 893/1868 |
| Promise.withResolvers | Partial | 40.1% | 256/639 |

## ES2025 Features

| Feature | Status | Pass Rate | Tests |
|---------|--------|-----------|-------|
| Math.sumPrecise | Full | 96.6% | 316/327 |
| Math.f16round | Full | 96.6% | 316/327 |
| Error.isError | Mostly | 86.8% | 46/53 |
| Set Methods | Mostly | 77.2% | 294/381 |
| RegExp.escape | Partial | 47.8% | 893/1868 |
| Promise.try | Partial | 40.1% | 256/639 |
| Iterator Helpers | Limited | 9.3% | 40/432 |
| Temporal | None | 0.0% | 0/4255 |

## Fully Working Modern Features

These ES2017+ features have >95% test262 pass rate:

### ES2017
- Object.values
- Object.entries
- Object.getOwnPropertyDescriptors
- String.padStart
- String.padEnd

### ES2019
- String.trimStart
- String.trimEnd

### ES2020
- globalThis
- String.matchAll

### ES2021
- String.replaceAll

### ES2022
- String.at
- Object.hasOwn

### ES2024
- Object.groupBy
- String.isWellFormed
- String.toWellFormed

### ES2025
- Math.sumPrecise
- Math.f16round


## Not Supported Features

These features have 0% test262 pass rate or are disabled:

### ES2015
- Classes
- Spread Operator

### ES2017
- async/await
- Trailing Commas

### ES2018
- Async Iterators

### ES2019
- Object.fromEntries

### ES2020
- BigUint64Array
- Dynamic Import
- import.meta

### ES2021
- WeakRef
- FinalizationRegistry
- Numeric Separators

### ES2022
- Class Fields
- Private Methods
- Static Class Fields
- Top-level await

### ES2025
- Temporal


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
