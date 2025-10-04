# 실행용 (CI에서 미리 bootJar 생성)
FROM eclipse-temurin:21-jre-alpine

ENV TZ=Asia/Seoul \
    JAVA_OPTS=""

WORKDIR /app

# CI가 만든 JAR 이미지로 복사 (build/libs/*.jar)
COPY build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
