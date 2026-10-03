FROM eclipse-temurin:21-jre-jammy
# Build the server with gradle jar before docker build.
RUN groupadd --gid 10001 protonova && useradd --uid 10001 --gid 10001 --no-create-home protonova \
    && mkdir -p /opt/proto-nova/run /var/lib/proto-nova/worldRoot /var/lib/proto-nova/tls-request \
    && chown -R protonova:protonova /opt/proto-nova /var/lib/proto-nova
COPY --chown=protonova:protonova build/libs/proto-nova-Server-*.jar /opt/proto-nova/app.jar
COPY --chown=protonova:protonova assets/ /opt/proto-nova/assets/
COPY docker-entrypoint.sh /opt/proto-nova/entrypoint.sh
RUN chmod 755 /opt/proto-nova/entrypoint.sh \
    && ln -s /opt/proto-nova/assets /opt/proto-nova/run/assets \
    && ln -s /var/lib/proto-nova/worldRoot /opt/proto-nova/run/worldRoot \
    && ln -s /var/lib/proto-nova/tls-request /opt/proto-nova/run/tls-request \
    && ln -s /var/lib/proto-nova/proto-nova.properties /opt/proto-nova/run/proto-nova.properties \
    && ln -s /var/lib/proto-nova/keystore.jks /opt/proto-nova/run/keystore.jks \
    && ln -s /var/lib/proto-nova/keystore.jks.password /opt/proto-nova/run/keystore.jks.password
WORKDIR /opt/proto-nova/run
USER protonova
VOLUME ["/var/lib/proto-nova"]
EXPOSE 7675 7674
ENTRYPOINT ["/opt/proto-nova/entrypoint.sh"]
CMD ["java", "-jar", "/opt/proto-nova/app.jar", "--headless"]
