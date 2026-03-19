FROM eclipse-temurin:17-jdk-alpine

# Create app user (security)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy jar
COPY target/backend-0.0.1-SNAPSHOT.jar app.jar

# Change ownership
RUN chown -R appuser:appgroup /app

# Switch user
USER appuser

# Expose port
EXPOSE 8080

# Run app (with memory optimization for free tier)
ENTRYPOINT ["java","-Xms128m","-Xmx256m","-jar","app.jar"]