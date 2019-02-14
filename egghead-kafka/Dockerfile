FROM openjdk:8-jdk-alpine
VOLUME /tmp
ARG DEPENDENCY=target/dependency
COPY ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY ${DEPENDENCY}/META-INF /app/META-INF
COPY ${DEPENDENCY}/BOOT-INF/classes /app
ENTRYPOINT [\
    "java",\
    "-Dcom.sun.management.jmxremote",\
    "-Dcom.sun.management.jmxremote.port=9010",\
    "-Dcom.sun.management.jmxremote.rmi.port=9010",\
    "-Dcom.sun.management.jmxremote.local.only=false",\
    "-Dcom.sun.management.jmxremote.authenticate=false",\
    "-Dcom.sun.management.jmxremote.ssl=false",\
    "-cp",\
    "app:app/lib/*",\
    "egghead.eggheadkafka.Application"\
]
EXPOSE 8080
EXPOSE 9010
