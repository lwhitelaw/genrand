FROM eclipse-temurin:21 AS build-env
# install maven
RUN apt-get update
RUN apt-get install -y maven
# copy in source and build it
WORKDIR /build
COPY ./pom.xml ./pom.xml
RUN mvn dependency:resolve
COPY ./src ./src
RUN mvn package
RUN mv $(ls target/genrand-*.jar) target/genrandapi.jar
# build done
# create runtime env
FROM eclipse-temurin:21 AS runtime-env
WORKDIR /runtime
COPY --from=build-env /build/target/genrandapi.jar genrandapi.jar
EXPOSE 80 443
ENTRYPOINT ["java","-Xmx256M","-jar","genrandapi.jar"]
