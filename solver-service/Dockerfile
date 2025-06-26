# Use an appropriate base image with Java (e.g., OpenJDK 17)
FROM eclipse-temurin:17-jdk-jammy

# Set a working directory
WORKDIR /app

# --- OR-Tools Native Libraries ---
# Create a directory for native libraries
RUN mkdir -p /app/lib/native

# Option 1: Copy pre-downloaded .so file (Recommended for CI/CD)
# Ensure 'libjniortools.so' for Linux x86-64 is in a 'native-libs/linux-x86-64' directory 
# at the same level as this Dockerfile when building.
# COPY native-libs/linux-x86-64/libjniortools.so /app/lib/native/libjniortools.so

# Option 2: Download and extract OR-Tools during Docker build (Less ideal for reproducibility if URL changes)
# Replace v9.9 and the specific tar.gz filename with the version you are using (from pom.xml).
# Ensure the version matches the ortools-java dependency version.
ARG ORTOOLS_VERSION=9.9.3992 
ADD https://github.com/google/or-tools/releases/download/v9.9/or-tools_amd64_java_v${ORTOOLS_VERSION}.tar.gz /tmp/ortools.tar.gz
RUN tar -xzf /tmp/ortools.tar.gz -C /tmp \
    && cp /tmp/or-tools_java_v${ORTOOLS_VERSION}/lib/libjniortools.so /app/lib/native/libjniortools.so \
    && rm -rf /tmp/*
# Note: The exact path within the tar.gz might vary slightly between OR-Tools versions. Verify it.

# Set the Java library path to include the native libraries directory
ENV JAVA_OPTS="-Djava.library.path=/app/lib/native"

# --- Application JAR ---
# This assumes your build process (e.g., Maven) creates a fat JAR in the 'target' directory
COPY target/solver-service-0.0.1-SNAPSHOT.jar app.jar

# Expose the port your application listens on (e.g., 8080 for Spring Boot)
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]