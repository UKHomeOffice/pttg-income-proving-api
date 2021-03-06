#!/usr/bin/env bash

NAME=${NAME:-pttg-ip-api}
JAR=$(find . -name ${NAME}*.jar | head -1)

certfiles=$(awk '/-----BEGIN CERTIFICATE-----/{filename="acpca"NR; print filename}; {print >filename}' /certs/acp-root.crt)

for file in ${certfiles}
do
keytool -import -alias "${file}" -file "${file}" -keystore ./truststore.jks -noprompt -storepass changeit -trustcacerts
rm "${file}"
done

keytool -importkeystore -destkeystore /app/truststore.jks -srckeystore /opt/jdk/jre/lib/security/cacerts -srcstorepass changeit -noprompt -storepass changeit &> /dev/null


java ${JAVA_OPTS} -Djavax.net.ssl.trustStore=/app/truststore.jks \
                  -Dcom.sun.management.jmxremote.local.only=false \
                  -Djava.security.egd=file:/dev/./urandom -jar "${JAR}"
