# Build the WAR with the same Java major version used by the application.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B clean verify

# The project uses javax.servlet APIs, so run it on Tomcat 9 (not Tomcat 10+).
FROM tomcat:9.0-jdk17-temurin
ENV PORT=8080
ENV MELONMART_DB_URL="jdbc:h2:file:/data/melonmartdb;MODE=MySQL;AUTO_SERVER=TRUE"
WORKDIR /usr/local/tomcat
RUN rm -rf webapps/*
RUN mkdir -p /data
COPY --from=build /workspace/target/melonmart.war /usr/local/tomcat/webapps/melonmart.war
COPY docker-entrypoint.sh /usr/local/bin/melonmart-entrypoint.sh
EXPOSE 8080
ENTRYPOINT ["sh", "/usr/local/bin/melonmart-entrypoint.sh"]
