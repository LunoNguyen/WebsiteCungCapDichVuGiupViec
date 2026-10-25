# syntax=docker/dockerfile:1
# ============================================================
# Website cung cấp dịch vụ giúp việc – Spring Boot 4.1 / Java 26
# Build:  docker build -t giupviec-web .
# Chạy kèm MySQL, Redis, MinIO: docker compose up -d --build
# ============================================================

# ---------- Giai đoạn 1: build file jar bằng Maven Wrapper ----------
FROM eclipse-temurin:26-jdk AS build
WORKDIR /src

# Tải dependency trước để Docker cache lại khi chỉ sửa mã nguồn
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && sed -i 's/\r$//' mvnw
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -DskipTests \
    && cp target/*.jar /src/app.jar

# ---------- Giai đoạn 2: image chạy, chỉ có JRE ----------
FROM eclipse-temurin:26-jre
WORKDIR /app

ENV TZ=Asia/Ho_Chi_Minh \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Ho_Chi_Minh"

# Chạy bằng người dùng thường, không dùng root
RUN groupadd --system app && useradd --system --gid app --home /app app
COPY --from=build --chown=app:app /src/app.jar app.jar
USER app

EXPOSE 8083
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
