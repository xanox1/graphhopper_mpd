# Use Eclipse Temurin 17 runtime as base image
FROM eclipse-temurin:17-jre

# Set working directory
WORKDIR /app

# Copy the GraphHopper web service JAR file
COPY web/target/graphhopper-web-*.jar graphhopper-web.jar

# Create directory for graphhopper volume (graph-cache will be created within this)
RUN mkdir -p /app/graphhopper

# Define volume for GraphHopper data
VOLUME ["/app/graphhopper"]

# Expose the default GraphHopper port
EXPOSE 8989

# Set JVM options for container environment
ENV JAVA_OPTS="-Xmx2g -Xms2g"

# Run GraphHopper web service
# Note: Configuration file should be mounted in the volume at /app/graphhopper/config.yml
# OSM data file should also be mounted in the volume
CMD ["sh", "-c", "java $JAVA_OPTS -jar graphhopper-web.jar server /app/graphhopper/config.yml"]