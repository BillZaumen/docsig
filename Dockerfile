FROM wtzbzdev/ejwsacme:17-jdk-alpine AS build

RUN mkdir -p /usr/share/doc/libbzdev-doc \
    && mkdir -p /etc/docsig

COPY CURRENT_EJWS_VERSION /CURRENT_EJWS_VERSION
RUN (cmp EJWS_VERSION CURRENT_EJWS_VERSION || \
     (echo ERROR: base ejws version out of date ; exit 1)) \
    && rm CURRENT_EJWS_VERSION

COPY docsig.config /etc/docsig/docsig.config
COPY bzdevapi.zip /usr/share/doc/libbzdev-doc/api.zip
COPY docsig-web.jar /usr/share/bzdev
COPY docsig-verify.jar /usr/share/bzdev

RUN jlink --module-path /usr/share/bzdev \
	  --add-modules \
	  org.bzdev.docsig,org.bzdev.docsig.verify,$EJWS_ACME_MODULES \
	  --compress=2 --no-header-files --no-man-pages \
	  --include-locales=en --strip-debug --output /opt/docsig

RUN rm -rf /opt/java/openjdk
RUN rm -rf /usr/share/bzdev
COPY reset.sh /opt/docsig/reset
RUN chmod u+x /opt/docsig/reset

FROM scratch

COPY --from=build / /
ENV PATH=/opt/docsig/bin:$PATH
ENV JAVA_HOME=/opt/docsig

# Default configuration used initially so a user can get
# documentation.
RUN mkdir -p /usr/app

#
# Depending on the configuration, we could be using either HTTP,
# HTTPS or both, so expose the default ports.
#
EXPOSE 80/tcp
EXPOSE 443/tcp

WORKDIR /usr/app

CMD [ "java", "-m", "org.bzdev.docsig", \
     "/usr/app/docsig", "/usr/app/docsig" ]
