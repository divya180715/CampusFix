# Campus Fix Multi-Stage Dockerfile
# Stage 1: Build Maven WAR
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Apache Tomcat 10.1 (Jakarta EE 10 compatible)
FROM tomcat:10.1-jdk17-temurin
LABEL maintainer="Campus Fix Engineering Team"
LABEL description="Smart Campus Complaint and Maintenance Management System"

# Remove default Tomcat webapps for a clean runtime
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy packaged WAR to ROOT so it serves at http://localhost:8080/
COPY --from=build /app/target/campus-fix.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
CMD ["catalina.sh", "run"]
