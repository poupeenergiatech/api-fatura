FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /build
COPY Java/pom.xml .
RUN mvn dependency:go-offline -q
COPY Java/src ./src
RUN mvn package -q -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /build/target/leitor-fatura-energia-1.0.0.jar app.jar
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server $PORT"]
