# Base dotnet image
FROM defradigital/java:jre-21

USER root

COPY target/trade-demo-backend-*.jar /trade-demo-backend.jar

EXPOSE 8080

CMD [ "java", "-jar", "trade-demo-backend.jar" ]
