---
name: check
description: Run quality checks (lint, typecheck, compile) on SellerX
allowed-tools: Bash
user-invocable: true
---

Run quality checks on SellerX. Usage: `/check frontend`, `/check backend`, or `/check all`

The target is: $ARGUMENTS

## Frontend Checks

Run these in sequence, report results for each:

### TypeScript Check
```bash
cd /Users/furkanyigit/Desktop/Sellerx/sellerx-frontend && npx tsc --noEmit 2>&1 | tail -30
```
Count total errors from output.

### ESLint Check
```bash
cd /Users/furkanyigit/Desktop/Sellerx/sellerx-frontend && npm run lint 2>&1 | tail -30
```
Count total warnings and errors.

## Backend Checks

### Compile Check
```bash
cd /Users/furkanyigit/Desktop/Sellerx/sellerx-backend && ./mvnw compile -q 2>&1 | tail -30
```
Report success or show compilation errors.

## Output Format

Present a summary table:

| Check       | Target   | Result | Issues |
|-------------|----------|--------|--------|
| TypeScript  | Frontend | ...    | N errors |
| ESLint      | Frontend | ...    | N warnings, M errors |
| Compile     | Backend  | ...    | N errors |

Then list the top 5 most important errors (if any) with file locations.

## Rules

- If no argument provided, run ALL checks (same as `/check all`)
- Show only summary by default, not full output
- If checks pass with 0 errors, celebrate briefly
- For frontend: TypeScript errors are more critical than ESLint warnings
