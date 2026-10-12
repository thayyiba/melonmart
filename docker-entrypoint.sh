#!/bin/sh
set -eu

# Render and Railway provide PORT at runtime. Keep Tomcat's default for local use.
PORT_VALUE="${PORT:-8080}"
case "$PORT_VALUE" in
  ''|*[!0-9]*)
    echo "PORT must be a numeric TCP port." >&2
    exit 1
    ;;
esac

# Change only Tomcat's HTTP connector, not its shutdown or AJP ports.
sed -i "s/<Connector port=\"8080\"/<Connector port=\"$PORT_VALUE\"/" "$CATALINA_HOME/conf/server.xml"

# The default H2 URL points inside /data. Mount a persistent volume there in production.
mkdir -p /data

exec "$CATALINA_HOME/bin/catalina.sh" run
