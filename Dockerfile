FROM wtzbzdev/ejwscert:17-jdk-alpine AS build

RUN mkdir -p /usr/share/doc/libbzdev-doc

COPY bzdevapi.zip /usr/share/doc/libbzdev-doc/api.zip

COPY docsig-web.jar /usr/share/bzdev

RUN jlink --module-path /usr/share/bzdev \
	  --add-modules org.bzdev.docsig,jdk.crypto.ec,org.bzdev.certbotmgr \
	  --compress=2 --no-header-files --no-man-pages \
	  --output opt/docsig

RUN rm -rf /opt/java/openjdk
RUN rm -rf /usr/share/bzdev

FROM scratch

COPY --from=build / /
ENV PATH=/opt/docsig/bin:$PATH
ENV JAVA_HOME=/opt/docsig

# Default configuration used initially so a user can get
# documentation.
RUN mkdir -p /usr/app
RUN echo port=80 > /usr/app/docsig.config

#
# Depending on the configuration, we could be using either HTTP or
# HTTPS, so expose the default ports for both.
#
EXPOSE 80/tcp
EXPOSE 443/tcp

WORKDIR usr/app

CMD [ "java", "-m", "org.bzdev.docsig", \
     "/usr/app/docsig", "/usr/app/docsig.config" ]
