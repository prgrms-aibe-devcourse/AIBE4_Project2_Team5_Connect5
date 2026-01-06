FROM gradle:8-jdk17-temurin AS build
WORKDIR /app

# Gradle 빌드 설정 복사 (의존성 캐싱 최적화)
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# 의존성 다운로드 (캐싱)
RUN gradle dependencies --no-daemon || true

# 소스 코드 복사
COPY src ./src

# Gradle을 사용하여 JAR 파일 빌드 (테스트는 생략)
RUN gradle bootJar --no-daemon -x test

# JRE로 애플리케이션 실행
FROM eclipse-temurin:17-jre

WORKDIR /app

# 빌드한 JAR 파일을 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 애플리케이션이 사용하는 포트 노출
EXPOSE 8080

# Spring 프로필을 'prod'로 설정
ENV SPRING_PROFILES_ACTIVE=prod

# JAR 파일 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
