---
name: db-migrate
description: Create a new Flyway migration file with correct numbering
allowed-tools: Bash, Read, Write
user-invocable: true
---

Create a new Flyway database migration for SellerX. Usage: `/db-migrate <description>`

The description is: $ARGUMENTS

## Steps

1. **Find the latest migration number**:
   ```bash
   ls /Users/furkanyigit/Desktop/Sellerx/sellerx-backend/src/main/resources/db/migration/V*.sql | sort -V | tail -1
   ```
   Extract the version number (e.g., V118 → 118).

2. **Calculate next version**: latest + 1

3. **Format the filename**:
   - Convert description to snake_case (lowercase, spaces to underscores)
   - Remove special characters
   - Format: `V{next}__{description}.sql`
   - Example: `V119__add_payment_status_column.sql`

4. **Create the migration file** at:
   `sellerx-backend/src/main/resources/db/migration/V{next}__{description}.sql`

   With this template content:
   ```sql
   -- Migration: V{next}__{description}
   -- Created: {current_date}
   -- Description: {original_description}

   -- TODO: Add your SQL here

   ```

5. **Confirm creation**: Show the full file path and remind:
   - Edit the file to add actual SQL
   - Test locally: `./db.sh connect` then `\dt` to verify
   - Backend will auto-apply on next startup (Flyway)

## Rules

- NEVER skip migration numbers (gaps cause Flyway errors)
- NEVER modify existing migrations (create a new one instead)
- If no description provided, ask the user for one
- Migration names should be descriptive: `add_x`, `create_x_table`, `alter_x`, `drop_x`, `fix_x`
