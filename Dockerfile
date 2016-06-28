FROM quay.io/ukhomeofficedigital/openjdk8:v0.1.2

ENV MONGO_HOST localhost
ENV MONGO_PORT 28017
ENV HMRC_API_ENDPOINT localhost
ENV USER pttg
ENV GROUP pttg
ENV NAME pttg-income-proving-api

ARG JAR_PATH
ARG VERSION

WORKDIR /app

RUN groupadd -r ${GROUP} && \
    useradd -r -g ${USER} ${GROUP} -d /app && \
    mkdir -p /app && \
    chown -R ${USER}:${GROUP} /app

ADD ${JAR_PATH}/${NAME}-${VERSION}.jar /app
ADD run.sh /app

RUN chmod a+x /app/run.sh

EXPOSE 8081

ENTRYPOINT /app/run.sh
