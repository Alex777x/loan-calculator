FROM openjdk:21-jdk-slim
COPY target/loan-calculator-0.0.1.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
