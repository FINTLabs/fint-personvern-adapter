FROM gradle:9.8.2-jdk25 AS builder
USER root
COPY . .
RUN gradle --no-daemon build -x test

FROM gcr.io/distroless/java25-debian13
ENV JAVA_TOOL_OPTIONS=-XX:+ExitOnOutOfMemoryError
COPY --from=builder /home/gradle/build/libs/fint-personvern-adapter-*.jar /data/app.jar
CMD ["/data/app.jar"]
