# Melon Mart deployment

Melon Mart is a Java 17 Maven WAR application that uses the **Java EE `javax.servlet` API**. Use **Apache Tomcat 9**; Tomcat 10+ changes the servlet namespace and is not compatible without a migration.

## Build and run on local Tomcat

1. Install JDK 17, Maven, and Apache Tomcat 9.
2. From the repository root, run:

   ```sh
   mvn -B clean verify
   ```

3. Copy `target/melonmart.war` to Tomcat's `webapps/` directory as `melonmart.war`.
4. Start Tomcat and open `http://localhost:8080/melonmart/`.
5. Check `http://localhost:8080/melonmart/api/health`. A healthy response should report both application and database status as `UP`.

The existing `src/main/resources/db.properties` keeps the developer's historical Windows H2 path. For a different machine, configure `MELONMART_DB_URL`, `MELONMART_DB_USERNAME`, and `MELONMART_DB_PASSWORD` in the Tomcat process environment instead of editing the database file or resetting it. For example, a Linux host with a persistent data directory can use:

```sh
MELONMART_DB_URL='jdbc:h2:file:/var/lib/melonmart/melonmartdb;MODE=MySQL;AUTO_SERVER=TRUE'
MELONMART_DB_USERNAME=sa
MELONMART_DB_PASSWORD=''
```

Create the parent directory and ensure the Tomcat process can write to it before starting the application. Back up an existing H2 database before changing its URL. Do not point multiple independent application instances at the same embedded H2 file.

## Docker, Render, and Railway

The repository includes a multi-stage `Dockerfile`. It builds and tests the WAR with Java 17, then runs it on Tomcat 9. The container serves the app at `/melonmart/`, and its entrypoint uses the platform-provided `PORT` when present.

- **Render:** create a Docker web service from this repository. Set a persistent disk mount path to `/data` and use the default `MELONMART_DB_URL` or explicitly set it to `jdbc:h2:file:/data/melonmartdb;MODE=MySQL;AUTO_SERVER=TRUE`. Render's filesystem is ephemeral without a persistent disk; a service without persistent storage can lose H2 changes on restart/redeploy.
- **Railway:** deploy from the repository using the Dockerfile and attach a volume mounted at `/data`. Keep the H2 URL pointed at `/data/melonmartdb`.
- For either host, set the DB environment variables in the service settings. Do not commit secrets or real database files to Git.

The application health endpoint is `/melonmart/api/health`. The container currently expects a single running instance when using its embedded H2 file database. If you choose to run multiple replicas, use a database architecture designed for that instead of sharing one H2 file.

## Database safety notes

On startup, the application applies versioned migrations and then executes the repository's schema and seed scripts. The scripts are intended to be idempotent, but always back up important data before deploying a schema change. The container image deliberately excludes local `*.mv.db`, `*.trace.db`, and `*.war` files from the Docker build context.

## What this deployment setup does not prove

A successful Maven build verifies compilation and automated tests; it does not simulate every browser workflow or prove the target host's environment variables, storage mount, external AI provider key, or live health check. After deployment, test registration/login, product browsing, cart changes, checkout, order history, seller actions, admin actions, wishlist, reviews, and Welp with a fresh test account.
