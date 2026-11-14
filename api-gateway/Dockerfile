
#api-gateway/Dockerfile

# Step 1: Use Java 25 as base image
FROM eclipse-temurin:25

# Step 2: Set working directory inside container
WORKDIR /app

# Step 3: Copy the built jar file
COPY target/api-gateway-0.0.1-SNAPSHOT.jar app.jar

# Step 4: Expose port
EXPOSE 8080

# Step 5: Run the jar
ENTRYPOINT ["java", "-jar", "app.jar"]