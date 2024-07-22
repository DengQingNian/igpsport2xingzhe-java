FROM openjdk:17-jdk-alpine
WORKDIR /
COPY target/igps2xingzhe-0.0.1-SNAPSHOT.jar /
EXPOSE 18877

CMD ["java", "-jar", "igps2xingzhe-0.0.1-SNAPSHOT.jar", "-Xmx100m"]
