FROM eclipse-temurin:17-jre
WORKDIR /app
COPY shiyiju-admin-1.0.0-SNAPSHOT.jar app.jar
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV SERVER_PORT=8080
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 CMD curl -f http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]