FROM wtzbzdev/ejws:17-jre-alpine

RUN mkdir -p /usr/share/doc/libbzdev-doc

COPY bzdevapi.zip /usr/share/doc/libbzdev-doc/api.zip

COPY docsig-web.jar /usr/share/bzdev

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

CMD ["java", "-p", "/usr/share/bzdev", "-m", "org.bzdev.docsig", \
     "/usr/app/docsig", "/usr/app/docsig.config" ]
