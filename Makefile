# GNU Makefile

TARGET=testsource

JFILES = $(wildcard src/org.bzdev.docsig/org/bzdev/docsig/*.java)
JFILES2 = $(wildcard src/org.bzdev.docsig.verify/org/bzdev/docsig/verify/*.java)

JAVADOC_VERSION = $(shell javadoc --version | sed -e 's/javadoc //' \
                | sed -e 's/[.].*//')

DOCKER_VERSION = 1.0


RESOURCES = src/org.bzdev.docsig/org/bzdev/docsig/signature.tpl \
	src/org.bzdev.docsig/org/bzdev/docsig/intro.tpl \
	src/org.bzdev.docsig/org/bzdev/docsig/email.tpl

docsig-web.jar:  docsig-verify.jar \
		$(JFILES) $(RESOURCES) src/org.bzdev.docsig/module-info.java
	rm -rf mods/org.bzdev.docsig/
	mkdir -p mods/org.bzdev.docsig/
	javac --release 11 -d mods/org.bzdev.docsig -p /usr/share/bzdev \
		src/org.bzdev.docsig/module-info.java $(JFILES)
	cp $(RESOURCES) mods/org.bzdev.docsig/org/bzdev/docsig
	javadoc -d api --module-path /usr/share/bzdev:docsig-verify.jar \
		--main-stylesheet stylesheet$(JAVADOC_VERSION).css \
		--module-source-path src \
		--add-modules org.bzdev.docsig.verify \
		-linkoffline \
           https://docs.oracle.com/en/java/javase/11/docs/api/ \
                    file:///usr/share/doc/openjdk-11-doc/api \
		--module org.bzdev.docsig.verify
	zip -r api.zip api
	cp api.zip mods/org.bzdev.docsig/org/bzdev/docsig/api.zip
	mkdir -p mods/org.bzdev.docsig/org/bzdev/docsig
	cp docsig-verify.jar mods/org.bzdev.docsig/org/bzdev/docsig
	for i in libbzdev-base.jar libbzdev-esp.jar \
		libbzdev-math.jar libbzdev-obnaming.jar scrunner.jar ; do \
	  cp /usr/share/bzdev/$$i \
		mods/org.bzdev.docsig/org/bzdev/docsig//$$i ; \
	done
	jar --create --file docsig-web.jar \
		--main-class=org.bzdev.docsig.DocsigServer \
		-C mods/org.bzdev.docsig .
	rm -rf api.zip api

docsig-verify.jar: $(JFILES2) src/org.bzdev.docsig.verify/module-info.java
	rm -rf mods/org.bzdev.docsig.verify/
	mkdir -p mods/org.bzdev.docsig.verify/
	javac --release 11 -d mods/org.bzdev.docsig.verify -p /usr/share/bzdev \
		src/org.bzdev.docsig.verify/module-info.java $(JFILES2)
	jar --create --file docsig-verify.jar \
		--manifest=src/org.bzdev.docsig.verify/manifest.mf \
		-C mods/org.bzdev.docsig.verify .

docker: docsig-web.jar
	docker build --tag wtzbzdev/docsig:$(DOCKER_VERSION) \
		--tag wtzbzdev/docsig:latest .

docker-nocache: docsig-web.jar
	docker build --no-cache=true --tag wtzbzdev/docsig:$(DOCKER_VERSION) \
		--tag wtzbzdev/docsig:latest .

docker-release:
	docker push wtzbzdev/docsig:$(DOCKER_VERSION)
	docker push wtzbzdev/docsig:latest


test: docsig-web.jar
	java -p /usr/share/bzdev:docsig-web.jar \
		-m org.bzdev.docsig  docsig  docsig.properties

verify: docsig-verify.jar
	cat testdata.txt | scrunner --exit -p docsig-verify.jar verify.esp

verify2: docsig-verify.jar
	cat testmsgs.mbox | scrunner --exit  -p  docsig-verify.jar verify2.esp

verify3: docsig-verify.jar
	cat testmsgs.mbox | scrunner --exit  -p  docsig-verify.jar verify3.esp

start-docsig:
	mkdir -p docsig-dir
	docker run  --publish 8080:80  --name docsig --detach \
		-v `pwd`/docsig-dir:/usr/app  wtzbzdev/docsig

stop-docsig:
	docker stop docsig
	docker rm docsig

start-trivweb:
	cat $(TARGET)/request.tpl | sed -e "s/HOST/`hostname -f`/" \
		| sed -e "s/SENDTO/`git config --get docsig.sendto`/" \
		> $(TARGET)/request.html
	docker run --publish 80:80 --detach --name trivweb \
		-v `dirname $(TARGET)`:/usr/app/:ro \
		--env TARGET=/usr/app/`basename $(TARGET)` \
		wtzbzdev/trivweb ; \

stop-trivweb:
	docker stop trivweb
	docker rm trivweb
