---
name: test-changed
description: Run tests only for files changed in git
allowed-tools: Bash, Read, Grep, Glob
user-invocable: true
---

Find and run tests for files that have been modified in git (staged + unstaged + untracked).

## Steps

### 1. Find Changed Files
```bash
# Get all changed files (modified, staged, untracked)
git diff --name-only HEAD 2>/dev/null
git diff --name-only --cached 2>/dev/null
git ls-files --others --exclude-standard 2>/dev/null
```

### 2. Categorize Changes

Separate into backend (.java) and frontend (.ts/.tsx) changes.

### 3. Find Corresponding Tests

**For Java files** (`sellerx-backend/src/main/java/**/*.java`):
- Map `src/main/java/com/ecommerce/sellerx/{package}/{ClassName}.java`
- To `src/test/java/com/ecommerce/sellerx/{package}/{ClassName}Test.java`
- Check if test file exists before adding to run list

**For TypeScript files** (`sellerx-frontend/**/*.ts` or `*.tsx`):
- Map `{path}/{filename}.ts` → `__tests__/{path}/{filename}.test.ts`
- Also check for co-located tests: `{path}/{filename}.test.ts`
- Check if test file exists before adding to run list

### 4. Run Tests

**Backend tests** (if any found):
```bash
cd /Users/furkanyigit/Desktop/Sellerx/sellerx-backend
./mvnw test -Dtest=ClassName1,ClassName2,ClassName3
```

**Frontend tests** (if any found):
```bash
cd /Users/furkanyigit/Desktop/Sellerx/sellerx-frontend
npm run test -- --run path/to/test1.test.ts path/to/test2.test.ts
```

### 5. Report Results

Show:
- Number of changed files analyzed
- Number of test files found and executed
- Test results summary (pass/fail)
- Any changed files that have NO corresponding test (flag as "untested changes")

## Edge Cases

- If no changed files: report "No changes detected"
- If no tests found for changes: report "No test files found for changed code"
- If a test file itself was changed: include it in the run
