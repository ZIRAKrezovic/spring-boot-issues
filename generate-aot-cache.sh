#!/bin/sh

./mvnw clean package

java -Djarmode=tools -jar target/demo-0.0.1-SNAPSHOT.jar extract --destination application

pushd application

java -XX:AOTCacheOutput=app.aot \
  -Dspring.context.exit=onRefresh \
  -Dspring.datasource.driver-class-name=org.postgresql.Driver \
  -Dspring.datasource.url=jdbc:postgresql://127.0.0.1:5432/aot \
  -Dspring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect \
  -Dspring.jpa.hibernate.ddl-auto=none \
  -Dspring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false \
  -jar demo-0.0.1-SNAPSHOT.jar

# Adding -Dspring.liquibase.enabled=false fixes the failure

popd