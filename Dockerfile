FROM quay.io/ukhomeofficedigital/openjdk8:v1.8.0.131

ENV USER user_pttg_ip_api
ENV USER_ID 1000
ENV GROUP group_pttg_ip_api
ENV NAME pttg-ip-api

ENV JAR_PATH build/libs

RUN yum update -y glibc && \
    yum update -y nss && \
    yum update -y bind-license

WORKDIR /app

RUN groupadd -r ${GROUP} && \
    useradd -r -u ${USER_ID} -g ${GROUP} ${USER} -d /app && \
    mkdir -p /app && \
    chown -R ${USER}:${GROUP} /app

COPY ${JAR_PATH}/${NAME}*.jar /app
COPY run.sh /app

RUN chmod a+x /app/run.sh

EXPOSE 8081

USER ${USER_ID}

ENTRYPOINT /app/run.sh
