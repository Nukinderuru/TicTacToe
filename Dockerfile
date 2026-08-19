FROM docker.io/library/node:22-alpine AS frontend-builder

WORKDIR /app/view

COPY view/package.json view/package-lock.json ./
RUN npm ci

COPY view/ ./
RUN npm run build

FROM docker.io/library/gradle:8.14.3-jdk21 AS backend-builder

WORKDIR /app

COPY gradle gradle
COPY gradlew build.gradle.kts settings.gradle.kts ./
COPY src ./src
COPY --from=frontend-builder /app/view/dist ./view/dist

RUN ./gradlew installDist --no-daemon

FROM docker.io/library/eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=backend-builder /app/build/install/TicTacToe ./
COPY --from=frontend-builder /app/view/dist ./view/dist

EXPOSE 8080

CMD ["./bin/TicTacToe"]
