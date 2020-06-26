FROM gradle:4.10.2-jdk8-alpine as builder
USER root
COPY . .
RUN gradle --no-daemon build

FROM gcr.io/distroless/java
ENV JAVA_TOOL_OPTIONS -XX:+ExitOnOutOfMemoryError
COPY --from=builder /home/gradle/build/deps/external/*.jar /data/
COPY --from=builder /home/gradle/build/deps/fint/*.jar /data/
COPY --from=builder /home/gradle/build/libs/fint-sse-adapter-skeleton-*.jar /data/fint-sse-adapter-skeleton.jar
CMD ["/data/fint-sse-adapter-skeleton.jar"]
