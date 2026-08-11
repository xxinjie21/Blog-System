# ========== 阶段一：构建 ==========
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# 先拉取依赖（利用 Docker 层缓存，pom 不变则此层不重跑）
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

# ========== 阶段二：运行 ==========
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
# 只复制编译产物 jar，镜像体积最小化
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
