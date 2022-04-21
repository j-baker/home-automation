FROM docker.io/azul/zulu-openjdk-alpine:17.0.2-17.32.13 AS build
ADD *.tgz /
RUN mv /home-automation-* /server

FROM docker.io/azul/zulu-openjdk-alpine:17.0.2-17.32.13
COPY --from=build /server /server
EXPOSE 8080
RUN apk update && apk add bash
LABEL org.opencontainers.image.source https://github.com/j-baker/home-automation
CMD ["service/bin/init.sh", "console"]
WORKDIR /server
