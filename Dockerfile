FROM openjdk:17-alpine
WORKDIR /app

ENV TOKEN = ""
ENV GROUP_ID = 0
ENV URL = ""
ENV EMAIL = ""
ENV PASSWORD = ""
ENV BOARD_ID = ""
ENV BOT_LANG = ""
ENV CHECKING_INTERVAL = "30.seconds"

COPY target/scala-2.13/*.jar /app/b2b_backend.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
