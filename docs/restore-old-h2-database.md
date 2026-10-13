# Restore the original local H2 database to Railway

This is a destructive, opt-in recovery procedure. It replaces rows in these tables on the configured destination database: users, products, orders, order_items, cart_items, reviews, wishlist, and delivery_addresses. It does not import migration-history tables. It preserves the original numeric IDs and resets identity counters.

## Safety rules

- Keep an untouched copy of the original `melonmartdb.mv.db` file.
- Never upload or commit the database file, JSON export, passwords, or restore key to GitHub.
- The app must be built and deployed with this restore endpoint before running the client.
- The endpoint is disabled unless Railway has `MELONMART_RESTORE_OLD_DB_KEY` set to a random secret of at least 32 characters.
- Use the same secret locally as `MELONMART_RESTORE_OLD_DB_KEY`; remove it from Railway immediately after a successful restore so the endpoint becomes unavailable again.
- The client offers a non-destructive `--check-only` precheck and requires `--replace-all-data` for the actual transfer. It insists on HTTPS and prints row counts only.
- Do not change `MELONMART_DB_URL`, the `/data` volume mount, or the persistent volume.

## Windows PowerShell procedure

Run this from the repository root on the computer that has the old H2 database. Make a separate backup first.

1. Build the app and its dependency classpath:

   ```powershell
   mvn -DskipTests package dependency:build-classpath "-Dmdep.outputFile=target/classpath.txt"
   ```

2. Set the old local database URL and deployed restore endpoint. Adjust the old path only if your database is somewhere else:

   ```powershell
   $env:MELONMART_OLD_DB_URL = 'jdbc:h2:file:C:/apache-tomcat-9.0.121/bin/data/melonmartdb;MODE=MySQL;AUTO_SERVER=TRUE'
   $env:MELONMART_RESTORE_TARGET_URL = 'https://melonmart-app-production.up.railway.app/melonmart/api/v1/restore/old-database'
   ```

3. Enter the exact temporary restore key that you configured in Railway without putting it in a command line:

   ```powershell
   $secure = Read-Host 'Temporary Railway restore key' -AsSecureString
   $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
   try {
       $env:MELONMART_RESTORE_OLD_DB_KEY = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
   } finally {
       [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
   }
   ```

4. Run the non-destructive source precheck. It does not send data or change Railway:

   ```powershell
   $cp = Get-Content target/classpath.txt -Raw
   java -cp "target/classes;$cp" com.melon.melonmart.tools.OldDatabaseRestoreClient --check-only
   ```

   Confirm the counts match the original database (users 8, products 8, orders 14, order_items 15, cart_items 1, reviews 5, wishlist 1, delivery_addresses 0). If any count differs, stop and investigate.

5. Only after the precheck passes and the backup is confirmed, review the target URL carefully and run the explicit destructive operation:

   ```powershell
   java -cp "target/classes;$cp" com.melon.melonmart.tools.OldDatabaseRestoreClient --replace-all-data
   ```

6. Only continue if the response says `"success":true` and the counts match the old database. Then remove the temporary key from Railway and clear the local environment variables:

   ```powershell
   Remove-Item Env:MELONMART_RESTORE_OLD_DB_KEY
   Remove-Item Env:MELONMART_OLD_DB_URL
   Remove-Item Env:MELONMART_RESTORE_TARGET_URL
   ```

Do not run the client until the endpoint code has been reviewed, built, and deployed. This procedure has not yet been run against the Railway database.
