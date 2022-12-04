FROM registry.access.redhat.com/ubi8/ubi

ARG GATEWAY_VERSION=1.9.5
ENV JAVA_HOME /usr/lib/jvm/zulu11

RUN \
    set -xeu && \
    yum -y -q install https://cdn.azul.com/zulu/bin/zulu-repo-1.0.0-1.noarch.rpm && \
    yum -y -q install zulu11-jdk less && \
    yum -q clean all && \
    rm -rf /var/cache/yum && \
    groupadd gateway --gid 1000 && \
    useradd gateway --uid 1000 --gid 1000 && \
    mkdir -p /gateway/bin /gateway/logs && \
    chown -R "gateway:gateway" /gateway 

COPY --chown=gateway:gateway gateway-ha/target/gateway-ha-$GATEWAY_VERSION-jar-with-dependencies.jar /gateway/gateway-ha.jar
COPY --chown=gateway:gateway gateway-ha/gateway-ha-config.yml /gateway/config.yml
COPY --chown=gateway:gateway run-gateway /gateway/bin/run-gateway
COPY --chown=gateway:gateway gateway-ha/src/main/resources/rules/routing_rules.yml /gateway/routing_rules.yml

EXPOSE 8080 8090 8091
USER gateway:gateway
ENV LANG en_US.UTF-8
RUN chmod a+x /gateway/bin/run-gateway
CMD ["/gateway/bin/run-gateway"]