# Use Eclipse Temurin 17 runtime as base image
FROM eclipse-temurin:17-jre

# Set working directory
WORKDIR /app

# Copy the GraphHopper web service JAR file
COPY web/target/graphhopper-web-*.jar graphhopper-web.jar

# Copy the Docker-specific configuration file
COPY config-docker.yml config.yml

# Create directory for graphhopper volume (graph-cache will be created within this)
RUN mkdir -p /app/graphhopper

# Define volume for GraphHopper data
VOLUME ["/app/graphhopper"]

# Expose the default GraphHopper port
EXPOSE 8989

# Set JVM options for container environment
ENV JAVA_OPTS="-Xmx2g -Xms2g"

# Run GraphHopper web service
# Note: Users need to mount OSM data file and set via environment variable or provide config
CMD ["sh", "-c", "java $JAVA_OPTS -jar graphhopper-web.jar server config.yml"]