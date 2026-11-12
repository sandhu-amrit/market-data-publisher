# Layered, non-root image. Build from the repository root:
#   docker build -f deploy/Dockerfile -t market-data-publisher .
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw mvnw.cmd pom.xml ./
COPY config config
COPY src src
RUN chmod +x mvnw && ./mvnw -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy AS extract
WORKDIR /app
COPY --from=build /src/target/market-data-publisher-*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract --destination extracted

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd --system --gid 10001 mdp \
    && useradd --system --uid 10001 --gid mdp --home /app --shell /usr/sbin/nologin mdp \
    && mkdir -p /app/work /app/output /data \
    && chown -R mdp:mdp /app /data
COPY --from=extract --chown=mdp:mdp /app/extracted/dependencies/ ./
COPY --from=extract --chown=mdp:mdp /app/extracted/spring-boot-loader/ ./
COPY --from=extract --chown=mdp:mdp /app/extracted/snapshot-dependencies/ ./
COPY --from=extract --chown=mdp:mdp /app/extracted/application/ ./
USER mdp
EXPOSE 8080
ENV MDP_WORK_DIR=/app/work MDP_OUTPUT_DIR=/app/output
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
