---
name: deploy
description: Deploy SellerX to production via GitHub Actions
allowed-tools: Bash, Read
user-invocable: true
---

Deploy SellerX components to production. Usage: `/deploy backend`, `/deploy frontend`, or `/deploy all`

The argument is: $ARGUMENTS

## Pre-Deploy Checks

1. **Check for uncommitted changes**:
   ```bash
   git status --porcelain
   ```
   If there are uncommitted changes, STOP and warn the user. Ask if they want to commit first.

2. **Verify we are on main branch**:
   ```bash
   git branch --show-current
   ```
   If not on `main`, warn the user. Deployments should be from `main`.

3. **Check remote is up to date**:
   ```bash
   git fetch origin main && git log HEAD..origin/main --oneline
   ```
   If behind remote, warn the user to pull first.

## Deploy Process

Based on the argument:

### `backend`
```bash
git push origin main
echo "Backend deploy triggered. GitHub Actions will:"
echo "  1. Build & test backend"
echo "  2. Build Docker image → GHCR"
echo "  3. Deploy to server"
echo ""
echo "Monitor: gh run list --workflow=deploy-backend.yml --limit 3"
```

### `frontend`
```bash
git push origin main
echo "Frontend deploy triggered. GitHub Actions will:"
echo "  1. Build frontend"
echo "  2. Build Docker image → GHCR"
echo "  3. Deploy to server"
echo ""
echo "Monitor: gh run list --workflow=deploy-frontend.yml --limit 3"
```

### `all`
```bash
git push origin main
echo "Full deploy triggered. Both workflows will run."
echo ""
echo "Monitor: gh run list --limit 5"
```

## Post-Deploy

After push, show:
1. Link to GitHub Actions runs
2. Health check commands:
   - Backend: `curl -k https://uwgss4kkocg4kks8880kwwwo.157.180.78.53.sslip.io/actuator/health`
   - Frontend: `curl -k https://sellerx.157.180.78.53.sslip.io`
3. Note: Deploys take 3-10 minutes. Backend is slower due to test suite.

## Safety Rules

- NEVER force push
- ALWAYS check for uncommitted changes first
- ALWAYS confirm with user before pushing
- If no argument provided, ask the user what to deploy
