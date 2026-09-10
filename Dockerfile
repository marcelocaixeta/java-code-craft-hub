# ---------- Stage 1: build (Maven + JDK 17, nada instalado no host) ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copia primeiro só o pom para aproveitar cache de dependências
COPY pom.xml .
RUN mvn -q dependency:go-offline

# Depois copia o código e compila
COPY src ./src
RUN mvn -q clean package -DskipTests

# ---------- Stage 2: run (só JRE, imagem leve) ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# Jar gerado no stage anterior (finalName=app -> app.jar)
COPY --from=build /app/target/app.jar app.jar

# Arquivo de dados (será criado pelo app se não existir)
# O volume no compose mapeia ./courses.json -> /app/courses.json
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
