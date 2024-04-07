# GNU Makefile

TARGET=testsource

JFILES = $(wildcard src/org.bzdev.docsig/org/bzdev/docsig/*.java)
JFILES2 = $(wildcard src/org.bzdev.docsig.verify/org/bzdev/docsig/verify/*.java)

JAVADOC_VERSION = $(shell javadoc --version | sed -e 's/javadoc //' \
                | sed -e 's/[.].*//')

DOCKER_VERSION = 1.0

EJWS_VERSION = `dpkg-query -f '\'\$${Version}\'' -W libbzdev-ejws-java`

BZDEV_INSTALLER = $(shell git config --get docsig.installer  \
		| sed -e 's/VERSION/$(EJWS_VERSION)'/ )

EMAIL_RESOURCE = src/org.bzdev.docsig/org/bzdev/docsig/email.tpl

RESOURCES = src/org.bzdev.docsig/org/bzdev/docsig/signature.tpl \
	src/org.bzdev.docsig/org/bzdev/docsig/intro.tpl \
	$(EMAIL_RESOURCE)

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
			http://localhost/bzdev-api/ \
			file:///usr/share/doc/libbzdev-doc/api \
		-linkoffline \
			https://docs.oracle.com/en/java/javase/11/docs/api/ \
			file:///usr/share/doc/openjdk-11-doc/api \
		--module org.bzdev.docsig.verify
	(cd api/org.bzdev.docsig.verify/org/bzdev/docsig/verify ; \
	 cp DocsigVerifier.Result.html tmp; \
	 cat tmp | sed -e 's%http://localhost%%g' > \
		DocsigVerifier.Result.html; rm tmp )
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
	cp $(EMAIL_RESOURCE) \
	    mods/org.bzdev.docsig.verify/org/bzdev/docsig/verify/email.tpl
	jar --create --file docsig-verify.jar \
		--manifest=src/org.bzdev.docsig.verify/manifest.mf \
		-C mods/org.bzdev.docsig.verify .

docker: docsig-web.jar
	unzip -p $(BZDEV_INSTALLER) api.zip > bzdevapi.zip
	cp stylesheet$(JAVADOC_VERSION).css stylesheet.css
	zip bzdevapi.zip stylesheet.css
	rm stylesheet.css
	dpkg-query -f '$${Version}\n' -W libbzdev-ejws-java > \
		CURRENT_EJWS_VERSION
	docker build --tag wtzbzdev/docsig:$(DOCKER_VERSION) \
		--tag wtzbzdev/docsig:latest .
	rm bzdevapi.zip CURRENT_EJWS_VERSION

docker-nocache: docsig-web.jar
	unzip -p $(BZDEV_INSTALLER) api.zip > bzdevapi.zip
	cp stylesheet$(JAVADOC_VERSION).css stylesheet.css
	zip bzdevapi.zip stylesheet.css
	rm stylesheet.css
	docker build --no-cache=true --tag wtzbzdev/docsig:$(DOCKER_VERSION) \
		--tag wtzbzdev/docsig:latest .
	rm bzdevapi.zip

docker-release:
	docker push wtzbzdev/docsig:$(DOCKER_VERSION)
	docker push wtzbzdev/docsig:latest


test: docsig-web.jar
	java -p /usr/share/bzdev:docsig-web.jar \
		-m org.bzdev.docsig  docsig  test.config

test2: docsig-web.jar
	java -p /usr/share/bzdev:docsig-web.jar \
		-m org.bzdev.docsig  docsig  test2.yaml


test-ssl: docsig-web.jar
	java -p /usr/share/bzdev:docsig-web.jar \
		-m org.bzdev.docsig  docsig  test-ssl.config

verify: docsig-verify.jar
	cat testdata.txt | scrunner --exit -p docsig-verify.jar verify.esp

verify2: docsig-verify.jar
	cat testmsgs.mbox | scrunner --exit  -p  docsig-verify.jar verify2.esp

verify2sonic: docsig-verify.jar
	cat sonic.mbox | scrunner --exit  -p  docsig-verify.jar verify2.esp

verify2gmail: docsig-verify.jar
	cat gmail.mbox | scrunner --exit --stackTrace  -p  docsig-verify.jar verify2.esp

verify3: docsig-verify.jar
	cat testmsgs.mbox | scrunner --exit  -p  docsig-verify.jar verify3.esp

goodsigs: docsig-verify.jar
	cat testmsgs.mbox | scrunner --exit  -p  docsig-verify.jar goodsigs.esp

badsigs: docsig-verify.jar
	cat testmsgs.mbox | scrunner --exit  -p  docsig-verify.jar badsigs.esp


start-all:  start-docsig start-trivweb

start-all-local: start-docsig start-local-trivweb

start-all-local-ssl: start-docsig-ssl start-local-trivweb



stop-all: stop-docsig stop-trivweb


config-docsig:
	docker run --rm -v docsigdir:/usr/app -e newDocsigConfig=true \
		-e trace=true -e stackTrace=true wtzbzdev/docsig

start-docsig:
	docker run  --publish 80:80  --name docsig --detach \
		-e DOCSIG_LOCALHOST=`hostname` \
		-v docsigdir:/usr/app  wtzbzdev/docsig

start-docsig-tty:
	docker run  --entrypoint sh  --name docsig -it \
		-e DOCSIG_LOCALHOST=`hostname` \
		-v docsigdir:/usr/app  wtzbzdev/docsig 

start-docsig-ssl:
	docker run  --publish 443:443  --publish 80:80 \
		--name docsig --detach \
		-e DOCSIG_LOCALHOST=`hostname` \
		-v docsigdir:/usr/app  wtzbzdev/docsig

stop-docsig:
	docker stop docsig
	docker rm docsig

start-trivweb: set-request
	cat $(TARGET)/request.tpl | sed -e "s/HOST/`hostname -f`/" \
		| sed -e "s/SENDTO/`git config --get docsig.sendto`/" \
		> $(TARGET)/request.html
	docker run --publish 8080:80 --detach --name trivweb \
		-v `pwd`/$(TARGET):/usr/app/:ro \
		--env DARKMODE=true \
		--env TARGET=/usr/app/request.html \
		wtzbzdev/trivweb ;

start-local-trivweb: set-request
	cat $(TARGET)/request.tpl | sed -e "s/HOST/localhost/" \
		| sed -e "s/SENDTO/`git config --get docsig.sendto`/" \
		> $(TARGET)/request.html
	docker run --publish 8080:80 --detach --name trivweb \
		-v `pwd`/$(TARGET):/usr/app/:ro \
		--env DARKMODE=true \
		--env TARGET=/usr/app/request.html \
		wtzbzdev/trivweb ;


stop-trivweb:
	docker stop trivweb
	docker rm trivweb

set-request:
	cc=`git config --get docsig.cc` ; \
	if [ -z "$$cc" ] ; then \
		cat $(TARGET)/request.tpl | sed -e "s/HOST/localhost/" \
		| sed -e "s/SENDTO/`git config --get docsig.sendto`/" \
		> $(TARGET)/request.html ; \
	else \
		cat $(TARGET)/request.tpl | sed -e "s/HOST/localhost/" \
		| sed -e "s/SENDTO/`git config --get docsig.sendto`/" \
		| sed -e "s/CC/$$cc/" | sed -e "s/<!--//" \
		| sed -e "s/-->//" \
		> $(TARGET)/request.html ; \
	fi

set-form:
	cc=`git config --get docsig.cc` ; \
	if [ -z "$$cc" ] ; then \
		cat $(TARGET)/form.html | sed -e "s/HOST/localhost/" \
		| sed -e "s/SENDTO_EMAIL_ADDRESS/`git config --get docsig.sendto`/" \
		> form.html ; \
	else \
		cat $(TARGET)/form.html | sed -e "s/HOST/localhost/" \
		| sed -e "s/SENDTO_EMAIL_ADDRESS/`git config --get docsig.sendto`/" \
		| sed -e "s/CC/$$cc/" | sed -e "s/<!--//" \
		| sed -e "s/-->//" \
		> form.html ; \
	fi

show-installer:
	echo $(BZDEV_INSTALLER)
