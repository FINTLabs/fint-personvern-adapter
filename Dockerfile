FROM gradle:4.10.3-jdk8-slim as builder
USER root
COPY . .
RUN gradle --no-daemon build

FROM gcr.io/distroless/java:8
ENV JAVA_TOOL_OPTIONS -XX:+ExitOnOutOfMemoryError
COPY --from=builder /home/gradle/build/deps/external/*.jar /data/
COPY --from=builder /home/gradle/build/deps/fint/*.jar /data/
COPY --from=builder /home/gradle/build/libs/fint-personvern-adapter-*.jar /data/fint-personvern-adapter.jar
CMD ["/data/fint-personvern-adapter.jar"]