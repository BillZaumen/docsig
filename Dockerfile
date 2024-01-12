FROM eclipse-temurin:11.0.18_10-jre-jammy

RUN apt update && apt upgrade -y
RUN apt-get -y install apt-utils binutils

#
# Add the repository for libbzev packages and webnail-server.
# sed is used because setup.sh uses sudo to get root access and
# sudo is not supported by eclipse-temurin:11.0.18_10-jre-jammy.
# Similary lsb_release is not supported but /etc/os-release exists.
#
RUN . /etc/os-release && \
    curl https://billzaumen.github.io/bzdev/setup.sh | \
    sed s/'sudo -k'// | sed s/sudo// \
    | sed s/'`lsb_release -c -s`'/$VERSION_CODENAME/ | sh

#
# Be careful at this point: If a previous version of the libraries
# are cached, these may be used instead, including ones cached
# by building using a different Dockerfile.
#
RUN apt-get -y install --no-install-recommends  \
    libbzdev-base-java libbzdev-ejws-java

COPY docsig-web.jar /usr/share/bzdev

#
# Depending on the configuration, we could be using either HTTP or
# HTTPS, so expose the default ports for both.
#
EXPOSE 80/tcp
EXPOSE 443/tcp

WORKDIR usr/app

CMD ["java", "-p", "/usr/share/bzdev", "-m", "org.bzdev.docsig", \
     "/usr/app/docsig", "/usr/app/docsig.config"]

