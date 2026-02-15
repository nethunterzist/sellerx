---
name: status
description: Check SellerX project status - database, backend, frontend, and git
allowed-tools: Bash
---

Check the current status of the SellerX development environment and report:

## Steps

1. **Database (PostgreSQL port 5432)**:
   ```bash
   pg_isready -h localhost -p 5432 2>/dev/null && echo "DB: Running" || echo "DB: Not running (start with: ./db.sh start)"
   ```

2. **Backend (Spring Boot port 8080)**:
   ```bash
   curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health 2>/dev/null | grep -q "200" && echo "Backend: Running (healthy)" || echo "Backend: Not running (start with: cd sellerx-backend && ./mvnw spring-boot:run)"
   ```

3. **Frontend (Next.js port 3000)**:
   ```bash
   curl -s -o /dev/null -w "%{http_code}" http://localhost:3000 2>/dev/null | grep -qE "200|302" && echo "Frontend: Running" || echo "Frontend: Not running (start with: cd sellerx-frontend && npm run build && npm start)"
   ```

4. **Git Status Summary**:
   ```bash
   echo "Branch: $(git branch --show-current)"
   echo "Modified: $(git diff --name-only | wc -l | tr -d ' ') files"
   echo "Untracked: $(git ls-files --others --exclude-standard | wc -l | tr -d ' ') files"
   echo "Staged: $(git diff --cached --name-only | wc -l | tr -d ' ') files"
   ```

## Output Format

Present results in a clean table:

| Service    | Status | Port |
|------------|--------|------|
| Database   | ...    | 5432 |
| Backend    | ...    | 8080 |
| Frontend   | ...    | 3000 |

Then show git status summary below the table.
