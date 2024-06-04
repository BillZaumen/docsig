<!DOCTYPE html>
<HTML lang="en">
  <HEAD>
  <META charset="UTF-8">
  <META name="viewport" content="width=device-width, initial-scale=1.0">
  <STYLE type="text/css">
    BODY {
        background-color: $(bgcolor);
        color: $(color);
        margin: 2em;
    }
    A {color: $(linkColor);}
    A:link {color: $(linkColor);}
    A:visited {color: $(visitedColor);}

    BLOCKQUOTE {background-color: $(bquoteBGColor);}

    li + li { margin-top: 10px;}
  </style>
  <TITLE>DOCSIG Documentation</TITLE>
  </HEAD>
  <BODY>
  <H1>DOCSIG Documentation</H1>
    <P>
      Links:
      <UL>
	<LI><A HREF="#quickstart">Quick Start</A>.
	<LI><A HREF="#intro">Introduction</A>.
	<LI><A HREF="#forms">Forms</A>.
	<LI><A HREF="#generated">Generated Forms</A>.
	<LI><A HREF="#queries">Query strings<A>.
	<LI><A HREF="#pem">PEM Encodings</A>.
	<LI><A HREF="#config">Configuration</A>.
	<LI><A HREF="#templates">Templates</A>.
	<LI><A HREF="#startup">Startup</A>.
	<LI><A HREF="#validation">Validation and Table Generation</A>.<br>
	  The following links can be used to install JAR files and
	  to view or install API documentation:
	  <P>
	  <UL>
	    <LI><A HREF="/docsig-api/api/index.html">API for validation</A>
	    <LI><A HREF="/bzdev-api/index.html">API for the BZDev
		class library</A>.
	    <LI><A HREF="/jars/">JAR files for validation</A>.
	    <LI><A HREF="/docsig-api.zip">ZIP file for downloading the
		DOCSIG API documentation</A>
	      (when unzipped, the documentation will be in a directory
	      named <STRONG>api</STRONG>, so runzip should be run in an
	      appropriately named directory).
	  </UL>
	  <P>
	  <A HREF="#sha256">SHA-256 message digests</A> for JAR files
	  should be checked.
       <LI><A HREF="#proof">Verifying and Trusting Signatures</A>.
       <LI><A HREF="#source">Source code</A>.
       <LI><A HREF="#security">Security</A>.
       <LI><A HREF="#extensibility">Extensibility</A>.
       <LI><A HREF="/PublicKeys">Public Keys</A> that have been
	  created by this server.
      </UL>
      <H1><A ID="quickstart">Quick Start</A></H1>
    <P>
      The easiest way install and run DOCSIG is to log onto a server
      and  place the
      following file in a directory named "docsig" with the file name
      docker-compose.yml
      <BLOCKQUOTE><PRE>

version: "3"

services:
  docsig:
    image: wtzbzdev/docsig:latest
    container_name: docsig
    network_mode: "bridge"
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - acme:/etc/acme
      - certlog:/var/log/cert
      - contents:/usr/app
    restart: "unless-stopped"

volumes:
  acme:
  certlog:
  contents:

</PRE></BLOCKQUOTE>	  
      and then run the command
      <BLOCKQUOTE><PRE>

docker compose up -d

</PRE></BLOCKQUOTE>	  
    <P>
      This will start a server running HTTP. For older versions
      of Docker, "docker compose" should be "docker-compose". If the
      "-d" is absent (e.g., for testing), the docker command may not
      terminate, but useful information may be printed even if the
      program crashes.
    <P>
      To use HTTPS,
      <OL>
	<LI> Create a domain name.  For the domain example.com,
	  one might create a subdomain docsig.example.com and add
	  an 'A" or 'AAAA' record to the DNS server to associate
	  this subdomain with the server's IP address. Each domain
	  registry has its own procedure for doing this.
	<LI> Download a configuration file.
	  <UL>
	    <LI><A HREF="https://raw.githubusercontent.com/BillZaumen/docsig/main/acme.config">acme.config</A> or
	      <A HREF="https://raw.githubusercontent.com/BillZaumen/docsig/main/acme.yaml">acme.yaml</A>
	      can be used to get free certificates automatically from
	      Let's Encrypt.
	    <LI><A HREF="https://raw.githubusercontent.com/BillZaumen/docsig/main/default.config">default.config</A> or
	      <A HREF="https://raw.githubusercontent.com/BillZaumen/docsig/main/default.yaml">default.yaml</A>
	      will provide a self-signed certificate suitable for
	      testing.
	    <LI><A HREF="https://raw.githubusercontent.com/BillZaumen/docsig/main/manual.config">manual.config</A> or
	      <A HREF="https://raw.githubusercontent.com/BillZaumen/docsig/main/manual.yaml">manual.yaml</A>
	      can be used when certificates are managed manually.
	    <LI><A HREF="https://raw.githubusercontent.com/BillZaumen/docsig/main/docsig.config">docsig.config</A>
	      is the default configuration file, which will be automatically
	      installed if no configuration file is present. The file
	      <A HREF="https://raw.githubusercontent.com/BillZaumen/docsig/main/docsig.yaml">docsig.yaml</A>
	      is also available.
	  </UL>
	  (The links for these configuration files use github because
	  some browsers are now restricting downloads over HTTP.)
	<LI> Edit the downloaded configuration file. Each contains
	  instructions and typically one or two lines will have to
	  be changed.
	<LI> With docsig running, run the command
	  <BLOCKQUOTE><PRE>

docker cp CONFIGURATION_FILE docsig:/usr/app/docsig.config

</PRE></BLOCKQUOTE>
	  where CONFIGURATION_FILE is the name of the configuration
	  file that was downloaded and edited. For more complex
	  installations, the configuration file can use YAML syntax as
	  described <A HREF="#YAML">below</A>, in which case
	  CONFIGURATION_FILE must have the extension ".yaml" instead
	  of ".config" and the
	  docker copy command is 
	  <BLOCKQUOTE><PRE>

docker cp CONFIGURATION_FILE docsig:/usr/app/docsig.yaml

</PRE></BLOCKQUOTE>
	  where the configuration file uses YAML syntax.
	<LI> Run the docker-compose command
	  <BLOCKQUOTE><PRE>

docker compose restart

</PRE></BLOCKQUOTE>
	  which will restart the server and use the new configuration file.
      </OL>
    <P> To list the contents of a volume defined in the docker-compose.yml
      file, run
	  <BLOCKQUOTE><PRE>

docker volume ls

</PRE></BLOCKQUOTE> which will list various volume names whose name
	  starts with the name of the current directory and that end
	  in "acme", "certlog", and "contents". The ones ending in
	  "acme" and "certlog" contain useful files
	  when <B>certificateManager</B> is set to <B>AcmeClient</B>.
	  The command
	  <BLOCKQUOTE><PRE>

docker run --rm -v VOLUME:/data -w /data busybox CMD

</PRE></BLOCKQUOTE>
	  will run CMD in the top-level directory of the specified
	  volume VOLUME.  CMD will typically be "ls" or "cat FILE"
	  where FILE is a filename listed when CMD is "ls".
    <P>
      The remainder of this document provides programming documentation
      for DOCSIG.

      <H1><A ID="intro"> Introduction</A></H1>
    <P>
      DOCSIG is a server that supports electronic signatures for
      simple documents that require a single signature, and for
      providing data associated with a document or simply a
      request. Docsig servers will respond to an HTML form submission
      by generating a web page with a link to a document (if any) that
      is to be signed, together with the document&apos;s SHA-256
      message digest. This web page will also have a 'mailto' link
      that will set up an email message that can be sent to submit the
      signature. This email message does not contain the document
      being signed, but does contain a link to the document, if
      provided, and that document's SHA-256 message digest.  There are
      also some Java classes that facilitate processing emails so that
      each individual email does not have to be handled manually. The
      server can be configured to process these emails by generating a
      CSV (Comma Separated Values) file that can be loaded into
      spreadsheets or databases.  The DOCSIG server provides links to
      the relevant JAR files, including ones needed to use a scripting
      language. Besides allowing emails to be processed in bulk, the
      Java classes check that email messages have not been modified:
      each email contains some digitally signed data that is
      <A HREF="https://www.c-sharpcorner.com/article/what-is-a-pem-file/">PEM</A>
      encoded and that is used in the verification procedure.
    <P>
      A DOCSIG server is capable of performing a small number of
      tasks. As such, it represents a simple example of the use of the
      <A HREF="https://microservices.io/">microservice architecture</A>,
      which more or less breaks a complex application into a number of
      separately deployable and loosely coupled components.

      Existing software used for document signing includes both
      <A HREF ="https://www.docusign.com">DocuSign</A> and
      <A HREF = "https://www.docuseal.co/\">DocuSeal</A> (an open
      source alternative to DocuSign). These are far more capable than
      DOCSIG, but are overkill for the simplest cases: one use case
      for DOCSIG is a small group that is required by other entities
      to have members periodically sign waivers, which end up
      being stored somewhere and are never looked at again unless
      something goes wrong. Another is to implement a sign up sheet of
      some sort where user's may have to provide various parameters.

      <H1><A ID="forms">HTML forms</A></H1>
    <P>
      To use DOCSIG, a web site has to return a
      an HTML file, or alternatively an HTML email attachment,
      containing an HTML form. DOCSIG can also be configured
      to generate a suitable HTML file containing this form. For
      example:
      <BLOCKQUOTE><PRE>

 &lt;form action="<A HREF="#HTTP">HTTP</A>://<A HREF="#DS_SERVER">DOCSIG_SERVER</A>/docsig/" method="<A HREF="#METHOD">METHOD</A>"&gt;
   Name:
   &lt;input name="name" type="text" placeholder="Your Name" width="48"&gt;
   &lt;br&gt;&lt;br&gt;
   ID: <A HREF="#myid">MY_ID</A>
   &lt;input name="id" type="hidden" value="<A HREF="#myid">MY_ID</A>"&lt;br&gt;&lt;br&gt;
   Email:
   &lt;input name="email" type="text" placeholder="Your Email Address"
          width="48"&gt;&lt;br&gt;&lt;br&gt;&lt;br&gt;
   &lt;input name="transID" type="hidden" value="<A HREF="#TID">TRANSACTION_ID</A>"&gt;
    &lt;input name="sigserver" type="hidden"
          value="<A HREF="#HTTP">HTTP</A>://<A HREF="#DS_SERVER">DOCSIG_SERVER</A>/docsig/"&gt;
   &lt;input name="type" type="hidden" value="<A HREF="#type">TYPE</A>"&gt;
   &lt;input name="document" type="hidden"
          value="<A HREF="#DURL">DOCUMENT_URL</A>"&gt;
   &lt;input name="sendto" type="hidden"
          value="<A HREF="#emr">EMAIL_RECIPIENT</A>A"&gt;
   &lt;input name="subject" type="hidden" value="<A HREF="#emsl">EMAIL_SUBJECT_LINE</A>"&gt;
   &lt;input type="submit" value="Continue"
          style="font-size: 150%"&gt;
 &lt;/form&gt;
    </PRE></BLOCKQUOTE>
    where
    <UL>
      <LI><A ID="HTTP">HTTP</A> is either http or https.
      <LI><A ID="METHOD">METHOD</A> is either POST or GET.  The value
	POST is preferred because each web page returned contains a
	different timestamp, but GET is useful if one wants to use an
	HTTP redirect to avoid having the user submit a form. With
	an HTTP redirect, the query string must contain all the name/value
	pairs that the form would provide.
      <LI><A ID="myid">MY_ID</A> is an optional ID associated with the
	person submitting a signature.  The corresponding input may
	be hidden, or the user can be required to enter it.
      <LI><A ID="TID">TRANSACTION_ID</A> is an optional transaction ID.
	The field should be hidden, but may be reproduced text.
      <LI><A ID="DS_SERVER">DOCSIG_SERVER</A> is the the host name for
	the DOCSIG server, optionally followed by a ":" and the TCP
	port number. The port number is required if a non-standard
	port is used.
      <LI><A ID="type">TYPE</A> is the type of the document (e.g,
      "document" or "waiver")
      <LI><A ID="DURL">DOCUMENT_URL</A> is the URL for the document to
      be signed.
      <LI><A ID="emr">EMAIL_RECIPIENT</A> is the email address of a
	recipient that will process and store the signature.
      <LI><A ID="emsl">EMAIL_SUBJECT_LINE</A> is the subject line for
	an email providing the signature.
    </UL>
    Additional input elements may appear in the form so that users can
    provide additional parameters.
    <P>
      When a user clicks the 'Continue' button on the form shown above,
      the DOCSIG server will respond by providing a web page that contains
      a link to the document and its SHA-256 message digest. That link
      will either produce a document matching that message digest or will
      result in a 404 (Not Found) error.  When the 'Click to Send'
      button is pressed, the user&apos;s browser should open a window for
      composing email. This email will contain some easily readable
      text indicating that a specific document is being signed, plus
      some PEM encoded data used for processing.

      <H2><A ID="generated">Generated forms</A></H2>

      To make it easier to create an HTML form, DOCSIG provides a
      "servlet adapter" for this purpose.  In this case, A YAML
      configuration file must be used, with a "contexts" list that
      includes a description of each request form. This description
      includes a prefix (the path component of a URL), the <B>className</B>
      ServletWebMap, the <B>arg</B> org.bzdev.docsig.RequestAdapter,
      the method GET, and the servlet parameters, The syntax is described
      in the documentation for
      <A HREF="/bzdev-api/org.bzdev.ejws/org/bzdev/ejws/ConfigurableWS.html">ConfigurableWS</A>.
      The <A ID="requestParameters">parameters</A> are
      <UL>
	<LI><B>bgcolor</B>: the CSS background color for the generated
          HTML page.  The default value is "rgb(10,10,25)".
	<LI><B>color</B>: the CSS foreground color for the generated HTML page
	  The default value is "rgb(255,255,255)".
	<LI><B>linkColor</B>: the CSS color to use for links.  The default
	  value is "rgb(65,225,128)".;
	<LI><B>visitedColor</B>: the CSS color to use for visited links. The
	  default value is "rgb(65,164,128)".
	<LI><B>inputBGColor</B>: the CSS background color to use for controls in
	  HTML forms. The default value is "rgb(10,10,64)".
	<LI><B>inputFGColor</B>: the CSS  foreground color to use for
	  controls in HTML forms. The default value is
	  "rgb(255,255,255)".
	<LI><B>borderColor</B>: the CSS color of the border around an IFRAME
	  showing the document. the default value is "steelblue".
	<LI><B>bquoteBGColor</B>: the background color for block-quoted text.
	  The default is "rgb(32,32,32)".
	<LI><B>type</B>:  the document type. The default value is "document".
	  Examples of values include "waiver", "contract", "agreement", and
	  "request".
	<LI><B>document</B>: The document URL. This must be an absolute URL.
	  If missing, one will almost certainly need an
	  <B>additionalFormElements</B> parameter described below.
	<LI><B>sendto</B>: the email recipient.
	<LI><B>cc</B>:  An optional email address to which to send a copy.
	<LI><B>subject</B>: the subject line to use in an email message. The
	  default value is "Document Signature"
	<LI><B>sigserver</B>: the URL for the signature server.
	  This must be an absolute URL.
	<LI><A ID="template"></A><B>template</B>: the file name for the
	  <A HREF="/bzdev-api/org.bzdev.base/org/bzdev/util/TemplateProcessor.html">template</A>
	  used to generate a signature-request page. The
	  <A HREF="https://raw.githubusercontent.com/BillZaumen/docsig/main/src/org.bzdev.docsig/org/bzdev/docsig/request.tpl">default template</A>
	  can be copied and then modified to customize the request.
	<LI><B>fillText</B>: how plain text documents are displayed.
	  If the value is the string <B>true</B>, plain
	  text will be filled and formatted to fit into an HTML IFRAME
	  with appropriate indentation.  Otherwise (the default) the text
	  is displayed as is.  For filled text,
	  <UL>
	    <LI> blank lines separate paragraphs.
	    <LI> Indented lines starting with "*" followed by a white space
	      (spaces and tabs) are formatted so that the '* is to the
	      left of the paragraph, so that the result looks like a bullet.
	    <LI> Indented lines starting with nonzero digit, followed by
	      a period, and then whitespace is treated as part of a numbered
	      list. The list will be indented, with the initial digit
	      to its left.
	  </UL>
	  This is sufficient for simple formatting appropriate for
	  short documents.  One advantage when <B>fillText</B>
	  is <B>true</B> is that plain text files will fit
	  horizontally within an IFRAME as the text will 'wrap' to
	  keep the full line width visible.  The content type provided
	  in HTTP headers for a plain text document must be
	  text/plain.
	<LI><A ID="afe"><B>additionalFormElements</B></A>: text
	  containing HTML 5 <B>FIELDSET</B>, <B>LABEL</B>, <B>P</B>,
	  <B>LEGEND</B>, and <B>INPUT</B> elements that will be
	  included in the HTML form to be submitted.  The <B>NAME</B>
	  attribute for each <B>INPUT</B> element should be a Java
	  identifier. These names must be unique after being converted to
	  lower case. They must not match any name already in use for
	  an <A HREF="#emailTemplate">email template</A>.
      </UL>
      If a template is provided, the keys the template processor will
      provide include the parameters listed above, except for
      <B>textFill</B>, <B>template</B>, and <B>additionalFormElements</B>.
      There are also two additional keys for the template:
      <UL>
	<LI><B>digest</B>. This is the SHA-256 message digest of the
	  document.
	<LI><B>documentURL</B>. This is the URL given to an IFRAME.
	  When the document is plain text, this URL will be one whose
	  scheme is "data" (in order to be able to control the text
	  and background colors for the IFRAME).
      </UL>
      For example,
      <BLOCKQUOTE><PRE>

%YAML 1.2
---
defs: [optional]
config: [not shown]
contexts:
  - prefix: /request/
    className: ServletWebMap
    arg: org.bzdev.docsig.RequestAdapter
    parameters:
      document: https://example.com/docs/document.txt
      type: document
      subject: Document Signature
      sendto: sigs@example.com
      sigserver: https://example.com/docsig/
      fillText: "true"
    propertyNames:
      - color
      - bgcolor
      - linkColor
      - visitedColor
      - inputFGColor
      - inputBGColor
    methods:
      - GET
...

</PRE></BLOCKQUOTE>
      If there are multiple request forms, one should include a
      "template" parameter as described <A HREF="#template">above</A>
      and (of course) use a different prefix for each. The object
      <B>propertyNames</B> in this example provides parameters whose
      names and values were defined for the "config" object, including
      those not explicitly listed but that have default values.
    <P>
      To <A HREF="requestQS">get the request form,/A>, use the URL
      <BLOCKQUOTE><PRE>

https://example.com/request/?<B>QUERY_STRING</B>

</PRE></BLOCKQUOTE>
      where <B>QUERY_STRING</B> is a query string providing the
      following parameters:
      <UL>
        <LI><B>name</B>. The corresponding value is the user's name.
        <LI><B>email</B>. The corresponding value is the user's email
           address.
        <LI><B>id</B>. The corresponding value, which is optional, is
           the user's ID (typically a number or some other code).
        <LI><B>transid</B>. The corresponding value, which is
           optional, is a transaction ID to provide in the generated
           request.
      </UL>
      For <STRONG>name</STRONG> or <STRONG>email</STRONG> are provided,
      an input control is not provided when a query string provides the
      value.
      For example,
      <BLOCKQUOTE><PRE>

https://example.com/request/?name=John+Jones&email=jjones@example.com

</PRE></BLOCKQUOTE>


      <H1><A ID="queries">URL query strings</A></H1>
      
      URLs of the form
      <BLOCKQUOTE><PRE>

<A HREF="#HTTP">HTTP</A>://<A HREF="#DS_SERVER">DOCSIG_SERVER</A>/docsig/?<A HREF="#QUERY">QUERY</A>

</PRE></BLOCKQUOTE>
    <P>
      are used to help validate signature. For these URLs, the '/' before
      the '?' are needed.
      The <A ID="QUERY">query strings</A> that are supported are the
      following:
      <UL>
	<LI><STRONG>hasKeyRequest=DIGEST</STRONG>. The value
	  <STRONG>DIGEST</STRONG> is the SHA=256 message digest of a public
	  key, represented as a sequence of hexadecimal digits. The server
	  will not return an object for this request but will instead return
	  the following status codes:
	  <UL>
	    <LI><STRONG>204</STRONG>. The corresponding public key was found
	    <LI><STRONG>404</STRONG>. The corresponding public key was not
	      found.
	    <LI><STRONG>501</STRONG>. This server does not have a public key
	      directory.
	  </UL>
	<LI><STRONG>url=URL</STRONG>&amp;<STRONG>digest=DIGEST</STRONG>.
	  The value <STRONG>URL</STRONG> is the URL of a document and the
	  value <STRONG>DIGEST</STRONG> is the SHA-256 message digest of that
	  document, represented as a sequence of hexadecimal digits. The
	  server will return the following status codes:
	  <UL>
	    <LI><STRONG>200</STRONG>. The document was found and its
	      SHA-256 digest is the one provided in the request. The
	      document will be returned.
	    <LI><STRONG>404</STRONG>. The document was not found or its
	      SHA-256 digest is not the one provided in the request. An
	      error message will be returned
	    <LI>If the document specified by the URL cannot be found or
	      if there is some other failure while fetching it, the status
	      code provided by that server is used.
	  </UL>
	<LI><STRONG>publicKeyRequest=true</STRONG>. The current public
	  key will be returned as PEM-encoded data with a header
	  indicating the type of digital signature.  The status code
	  is 200.
	<LI><STRONG>getPublicKeys=true</STRONG>. A ZIP file containing
	  all of the public keys (each as PEM-encoded data with a
	  header indicating the type of digital signature) will be
	  returned if the request can be satisfied. Otherwise an error
	  message will be returned. The status codes are
	  <UL>
	    <LI><STRONG>200</STRONG>. The request was successful.
	    <LI><STRONG>404</STRONG>. There is no public key directory
	      directory.
	  </UL>
	<LI><STRONG>getLog=true</STRONG>. The log file will be returned
	  if the request can be satisfied.  The status codes are
	  <UL>
	    <LI><STRONG>200</STRONG>. The request was successful.
	    <LI><STRONG>404</STRONG>. There is no log file.
	  </UL>
      </UL>

      <H1><A ID="pem">PEM encodings</A></H1>
    <P>
      Email generated by DOCSIG includes a PEM-encoded section.  When
      the PEM encoding is removed, the result is a GZIP-encoded file
      that contains several headers followed by a PEM-encoded public
      key provide by the DOCSIG server. Each header name is followed
      immediately by a colon and then a space. The value for the
      header is the text in the rest of the line, which is terminated
      by a carriage return followed by a line feed. The headers are
      (in order)
      <OL>
	<LI><B>acceptedBy</B>. This is the user name entered in the
	  form above.
	<LI><B>timestamp</B>. This is time at which the signature was
	  generated.
	<LI><B>date</B>. This is date at which the signature was
	  generated using the server's time zone in the form used in
	  the email message containing the signature.
	<LI><B>ipaddr</B>. This is the IP address seen by the DOCSIG
	  server when the user submitted a request.
	<LI><B>id</B>. This is the optional ID field. It may be an
	  account/membership number, driver-license number, etc.  The
	  only constraint is that it cannot contain whitespace, which
	  will be stripped off if present.
	<LI><B>transId</B>. This is the optional transaction ID field.
	  It will typically be used as a reference for some
	  transaction.  The only constraint is that it cannot contain
	  whitespace, which will be stripped off if present.
	<LI><B>email</B>. This is the user&apos;s email address, as
	  typed in.
	<LI><B>server</B>. This is value
	  <A HREF="#HTTP">HTTP</A>://<A HREF="#DS_SERVER">DOCSIG_SERVER</A>.
	<LI><B>sendto</B>. This is the value of
	  <A HREF="#emr">EMAIL_RECIPIENT</A>.
	<LI><B>cc</B>. This optional header is the email address of an
	  additional recipient.
	<LI><B>document</B>. This is the URL for the document being
	  signed.
	<LI><B>type</B>. This is the type of the document: for
	  example, "document" or "waiver".
	<LI><B>digest</B>. This is the SHA-256 message digest of the
	  document being signed (represented as a string of
	  hexadecimal digits using lower-case letters).
	<LI><B>emailTemplateURL</B>. An optional header providing the
	  URL of a template file used to generate emails. This must be
	  an absolute URL.
	<LI><B>emailTemplateDigest</B>. This header is present if an only
	  if the <B>emailTemplateURL</B> header is present. It contains
	  the SHA-256 message digest of the email template.
	<LI><B>publicKeyID</B>. This is the SHA-256 message digest of
	  the PEM-encoded public key file (represented as a string of
	  hexadecimal digits using lower-case letters).
	<LI><I>input names</I>.For each input element in addition to
	  the standard ones in the HTML form that was submitted, and
	  sorted in case-insensitive alphanumeric order, there will be
	  a header whose name matches the input element's name and whose
	  value is the value provided for that element.
	<LI><B>signature</B>. This is the digital signature of the
	  values of the headers listed above, in the order listed,
	  with each terminated by a carriage return followed by a line
	  feed, with this signature represented as a string of
	  hexadecimal digits using lower-case letters. When the
	  signature is computed all the fields signed will use a UTF-8
	  character encoding.
	<LI><B>signature-algorithm</B>. This is the signature algorithm
	  used to create the signature. Typically this will be
	  SHA256withECDSA.
      </OL>
      The final component is the PEM encoded public key itself.
    <P>
      Showing that the signature is valid requires the following steps.
      <UL>
	<LI> The signature shows that none of the headers has been modified.
	  Testing the signature is straightforward programming. Breaking
	  public key encryption is difficult: at a minimum it would
	  require the resources available to a very wealthy government.
	<LI> The SHA-256 digest (<A ID="PKD">PK_DIGEST</A>)
	  provided by the publicKeyID header and the value of the
	  "server" header can be used to fetch the public key. For
	  example,
	  <BLOCKQUOTE><PRE>

	  curl <A HREF="#HTTP">HTTP</A>://<A HREF="#DS_SERVER">DOCSIG_SERVER</A>/PublicKey/<A HREF="#PKD">PK_DIGEST</A>.pem
	  </PRE></BLOCKQUOTE> 
	  will return a copy of the signature algorithm and the public
	  key from the specified DOCSIG server and these should match
	  the ones used.  The corresponding private key is kept on the
	  DOCSIG server in memory and is never written to persistent
	  storage.
	<LI> The email addresses provided in the headers should match those
	  in the email used to submit the signature.  While in principle
	  these can be forged, that is not particularly credible in this
	  case as it would require help from those working at the companies
	  providing the email services. Amateurish attempts won&apos;t work due
	  to DIM signatures, etc., used to filter out spam.
      </UL>
    <P>
      When the path portion of a URL is /docsig/, query strings and
      POST data are supported.  The POST data is covered in the description
      of the HTML form shown above.  Otherwise a query string can contain
      <UL>
	<LI> the parameter publicKeyRequest, which (if present) should have
	  the value "true". This will return the current public key with
	  a media type of application/x-pem-file.
	<LI> the parameters url and digest. This will return the contents
	  of the resource specified by the url with the constraint that
	  the message digest for these contents must match the one specified.
	  The value returned may be a cached value, but the cache size
	  is limited.
      </UL>
      <H1><A ID="config">Configuring a DOCSIG server</A></H1>
      A DOCSIG server is configured used a configuration file that
      provides a series of name-valued pairs:
      <UL>
	<LI><A HREF="#colors">Configuring CSS colors</A> describes
	  how to set colors used by various web pages.
	<LI><A HREF="#SSL">SSL properties</A> describes properties
	  specific to SSL
	<LI><A HREF="#PARMS">Server parameters</A> describes how to
	  configure general server parameters, including whether or
	  not SSL is used.
      </UL>
    <P>
      The configuration file itself uses the same format as
      <A HREF="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Properties.html#load(java.io.Reader)"
	 TARGET="ProperityFiles">Java property files</A>.
      Basically, these are sequences of lines, each containing a name
      and a value (with some escape characters to handle special
      cases). For example,
      <BLOCKQUOTE><PRE>
	  
sslType = TLS
keyStoreFile = keystore.p12
keyStorePassword = changeit

      </PRE></BLOCKQUOTE>
      would provide a basic HTTPS configuration. <A ID="YAML"></A>
      Alternatively, a file
      using
      <A HREF="https://linuxhandbook.com/yaml-basics/">YAML syntax</A>
      can be used.  The top-level objects in a DOCSIG YAML file are
      <UL>
	<LI><B>defs</B>. The value is typically a list.  Its function
	  is to provide a series of YAML anchors, which can be
	  referenced in the other sections of the file so that creating
	  the file is less tedious.
	<LI><B>config</B>. The value is an object whose keys have the
	  names listed <A HREF="">above</A>.
	<LI><A ID="contexts"></A><B>contexts</B>.
	  The value is a list of objects, each defining a new HTML
	  context, and can be used to add additional capabilities to
	  the server.  The prefix used for a context must not be
	  <UL>
	    <LI><STRONG>/</STRONG>
	    <LI><STRONG>/docsig</STRONG>
	    <LI><STRONG>/docsig-api</STRONG>
	    <LI><STRONG>bzdev-api</STRONG>
	    <LI><STRONG>/jars</STRONG>
	    <LI><STRONG>/PublicKeys</STRONG>
	  </UL>
	  as these are used by DOCSIG directly. The configuration for
	  each context is described in the API documentation for the
	  class
	  <A HREF="/bzdev-api//org.bzdev.ejws/org/bzdev/ejws/ConfigurableWS.html">ConfigurableWS</A>.
	  If additional web pages are added to the server, the
	  docker-compose.yml may have to be modified as additional volumes
	  may be needed.
      </UL>
      For example,
      <BLOCKQUOTE><PRE>
%YAML 1.2
---	  
config:	  
  sslType: TLS
  keyStoreFile: keystore.p12
  keyStorePassword: changeit
...
      </PRE></BLOCKQUOTE>
      in which case the file name must end with the suffix
      ".yml", ".yaml", ".YML", or ".YAML".  

      <H2><A ID="colors">Configuring CSS colors</A></H2>
    <P>
      The color for web pages are set using
      the following properties, all of which use
      <A HREF="https://www.w3schools.com/css/css_colors.asp"
	 TARGET="CSSColors">CSS colors</A>
      for their values:
      <UL>
	<LI><B>color</B>. This is the color used for text. If missing,
	  a default, <B>white</B>, will be used
	<LI><B>bgcolor</B>. This is the color used for the background.  If
	  missing,  a default, <B>rgb(10,10,25)</B>, will be used.
	<LI><B>linkColor</B>. This is the color used for links.  If
	  missing,  a default, <B>rgb(65,225,128)</B>, will be used.
	<LI><B>visitedColor</B>. This is the color used for visited links.  If
	  missing,  a default, <B>rgb(65,165,128)</B>, will be used.
	<LI><B>buttonFGColor</B>. This the color to use for the text in
	  a link formatted to appear as a button.  If
	  missing,  a default, <B>white</B>, will be used.
	<LI><B>buttonBGColor</B>. This the color to use for the background
	  for a link formatted to appear as a button.  If
	  missing,  a default, <B>rgb(10,10,64)</B>, will be used.
	<LI><B>bquoteBGColor</B>. This is the color used to highlight text,
	  either in a BLOCKQUOTE element or some special cases.  If
	  missing,  a default, <B>rgb(32,32,32)</B>, will be used.
	<LI><B>inputFGColor</B>. This is the CSS color used for the foreground
	  of text-field controls in request forms generated by DOCSIG.
	  The default is <B>white</B>. This field is provided to set a
	  default value for <A HREF="#requests">request pages</A>
	  when a YAML-based configuration file is used.
	<LI><B>inputBGColor</B>. This is the CSS color used for the
	  background of text-field controls in request forms generated
	  by DOCSIG. The default is <B>rgb(10,10,64)</B>.  This field
	  is provided to set a default value for
	  <A HREF="#requests">request pages</A> when a YAML-based
	  configuration file is used.
      </UL>

      <H2><A ID="SSL">SSL properties</A></H2>
    <P>
      The SSL properties are the following:
      <UL>
	<LI><B>keyStoreFile</B>. The value is the file name of the
	  key store, which is used to hold the server&apos;s certificate.
	  Relative file names are resolved against the directory containing
	  the configuration file.
	<LI><B>keyStorePassword</B>. The value is the password used by
	    the keystore file.
	<LI><B>keyPassword</B>. The value is the password used for individual
	  entries in the keystore file.  If not provided, the default is the
	  value of <B>keyStorePassword</B>.
	<LI><B>trustStoreFile</B>. The value is the file name of the
	  trust store, which provides certificates to trust in
	  addition to those provided by Java for certificate authorities.
	  This can be used when an organization creates an internal
	  certificate authority for the organization&apos;s use.
	  Relative file names are resolved against the directory containing
	  the configuration file.
	<LI><B>trustStorePassword</B>. The value is the password used by
	    the keystore file.
	<LI><B>allowLoopback</B>. When <B>true</B>, a loopback host name
	  will be recognized as valid. The default is <B>false</B>.
	<LI><B>allowSelfSigned</B>. When <B>true</B>, self-signed
	  certificates are accepted.  The default is <B>false</B>. This
	  option is useful for testing, but should not be used otherwise.
	<LI><B>certificateManager</B>. When present, the value is either
	  a simple name or a fully-qualified class name for a certificate
	  manager. The value "default" will set up DOCSIG so that a
	  self-signed certificate is automatically generated and the
	  corresponding keystore file will be created if it is not already
	  present.  Certificates are renewed automatically. The Docker
	  container wtzbzdev/docsig includes a certificate manger  whose
	  simple name is "AcmeClient" and that will get a certificate from the
	  Let's Encrypt certificate authority. In this case, the
	  properties <B>domain</B> and <B>email</B> are required, and
	  the server's DNS server must be configured to map the domain name
	  to the server's IP address (i.e., by setting an 'A' or 'AAAA"
	  record).
	<LI><B>certMode</B>. When present, the value may be <B>NORMAL</B>,
	  <B>STAGED</B>, or <B>TEST</B>. The default is <B>NORMAL</B>.
	  The value <B>STAGED</B> is useful for initial testing when the
	  certificate manager's provider name is <B>AcmeClient</B>: the
	  Let's Encrypt server will then generate a non-functioning certificate
	  but the ACME protocol will be used to download a certificate and
	  receiving a certificate indicates that there is not a configuration
	  error.  When <B>certMode</B> is <B>TEST</B>, programs that
	  implement the ACME protocol will not actually be run. This
	  value is intended for some tests of the AcmeClient provider.
	<LI><B>certName</B>. This is a name used to tag a certificate. The
	  default is "docsig".
	<LI><B>alwaysCreate</B>. When <B>true</B>, a new certificate is
	  created whenever a certificate manager checks if the certificate
	  has expired, a behavior useful for testing. The default is
	  <B>false</B>, in which case a new certificate will be requested
	  when the old one is about to expire.
	<LI><B>domain</B>. This is the fully-qualified domain name for the
	  server.  It is used to create the distinguished name in a
	  certificate.
	<LI><B>email</B>. This is an email address used by some
	  certificate authorities to send notifications about expiring
	  certificates.
	<LI><B>timeOffset</B>. The time offset in seconds from
	  midnight, local time, at which a server should determine if
	  a certificate should be renewed.
	<LI><B>interval</B>.  The number of days between attempts to
	  renew a certificate.
	<LI><B>stopDelay</B>. The time interval in seconds from a request
	  to shutdown a server to when the server is actually shut down.
	  This is used to give transactions being processed time to complete
	  and will be used only when a new certificate is needed.
      </UL>
      When HTTPS is used on the Internet, the only properties that should
      be set are <B>keyStoreFile</B> and <B>keyStorePassword</B>.

      <H2><A ID="PARMS">Server parameters</A></H2>
    <P>
      The server parameters are the following:
      <UL>
	<LI><B>sslType</B>. When present, the values should be either
	  <B>SSL</B> or <B>TLS</B>. TLS is preferred. When absent or
	  when the value is <B>null</B>, HTTP is used instead of HTTPS.
	<LI><B>ipaddr</B>. The value is the numeric IP address on which
	  this server listens, with two special values:
	  <P>
	  <UL>
	    <LI><B>wildcard</B>. The server will use the wildcard address
	      (this is the default).
	    <LI><B>loopback</B>. The server will use the loopback
	      address. Note that within a Linux container (e.g., when using
	      Docker), the loopback address may be interpreted as the
	      loopback address inside the container, no the loopback address
	      of the system.
	  </UL>
	<LI><B>port</B>. The value is the server&apos;s TCP port.  If missing
	  the port is by default set to 80 for HTTP and 443 for HTTPS.
	<LI><B>helperPort</B>. The value is an alternate server&apos;s
	  TCP port for use with HTTP when the certificate manager (if
	  provided) does not provide an HTTP port to use.  If the
	  value is zero or the parameter is not defined, and the
	  certificate manager does not provide a port, an HTTP server
	  will not be started.  If an HTTP server is started, it will provide
	  an HTTP redirect to the HTTPS server for web pages containing
	  documentation and public keys, and for queries. For POST methods,
	  the HTTPS server has to be used directly.  If the certificate
	  manager does not provide a port, this value must be set when an
	  HTTP server is desired.
	<LI><B>backlog</B>. When present, the value is an integer providing
	  the <A HREF="https://veithen.io/2014/01/01/how-tcp-backlog-works-in-linux.html">TCP backlog</A>.
          The default value is 30.
	<LI><B>nthreads</B>.
	  The number of threads the server can use. The default is 50.
	<LI><B>trace</B>. A value of <B>true</B> indicates that the
	  execution of a request will be traced, printing out what
	  occurred on standard output.  The default is <B>false</B>.
	<LI><B>stackTrace</B>. A value of <B>true</B> indicates that
	  errors resulting from GET or POST methods will generate a
	  stack trace.  The default is <B>false</B>.
	<LI><B>timezone</B>. The value is a
	  <A HREF="./timezones.txt">time-zone ID</A>. If missing, the
	  system default will be used.  This parameter determines the
	  time zone used when a date is shown in the body of an email
	  message.
	<LI><B>emailTemplateURL</B>. This is an optional parameter.
	  When present, it is the URL that provides a template file
	  suitable for creating emails.
      </UL>

      <H1><A ID="templates">Templates</A></H1>
    <P>DOCSIG uses a
      <A HREF="/bzdev-api/org.bzdev.base/org/bzdev/util/TemplateProcessor.html">template processor</A>
      to construct emails and requests. These are configured with
      keyword-value pairs described in the sections
      <UL>
	<LI><A HREF="#emailTemplates">Email templates</A>.
	<LI><A HREF="#requestTemplates">Request templates</A>.
      </UL>
      Template processing is performed as follows:
      <UL>
	<LI> <B>$$(<I>IDENTIFIER</I>)</B> is replaced with a value
	  associated with <I>IDENTIFIER</I>.
	<LI> <B>$$(+<I>IDENTIFIER1</I>:<I>IDENTIFIER2</I>)</B> will
	  result in the text ending with, but not including,
	  <B>$$(<I>IDENTIFIER2</I>)</B> being processed if there is a
	  value associated with <I>IDENTIFIER1</I>.
	<LI> <B>$$(-<I>IDENTIFIER1</I>:<I>IDENTIFIER2</I>)</B> will
	  result in the text ending with, but not including,
	  <B>$$(<I>IDENTIFIER2</I>)</B> being processed if no
	  value associated with <I>IDENTIFIER1</I> exists.
	<LI> <B>$$(<I>IDENTIFIER1</I>:$<I>IDENTIFIER2</I>)</B> is an
	  iteration construct described in the template processor
	  API documentation, and is not used by DOCSIG
	<LI> <B>$$$$</B>. is replaced with <B>$$</B>.
	<LI> <B>$$(!<I>TEXT</I>)</B> is a comment. <I>TEXT</I> may not
	  contain a closing parenthesis.
      </UL>

      <H2><A ID="emailTemplates">Email templates</A></H2>
    <P>
      The default email template will usually be adequate for
      when <A HREF="#afe">additional form elements</A> are not
      used. When there are additional form elements, the email
      should typically display the values of those. The
      template keys will be the name of the corresponding HTML
      input element, converted to lower case.  In addition to
      these, an email template can use the following keys:
      <UL>
	<LI><B>name3</B>. The name of the email sender.
	<LI><B>type</B>. The type of document or request.
	<LI><B>date</B>. The date of submissions.
	<LI><B>timezone</B>. The timezone used for the date.
	<LI><B>document</B>. The document (if any) being signed.
	<LI><B>digest</B>. The SHA-256 digest of the document
	  represented as a series of hexadecimal digits.
	<LI><B>sigserver</B>. The URL of the server generating the
	  email.
	<LI><B>PEM</B>. While defined, this key should not be used
	  explicitly: it will be automatically added to the end of
	  the email template.
      </UL>

      <H2><A ID="requestTemplates">Request templates</A></H2>

      The default request template should be adequate for most
      purposes. If one wants to customize it (for example, to add
      an image or logo), a template should be configured.  In this
      case, the keys are
      <UL>
	<LI><B>bgcolor</B>. The default background color.
	<LI><B>color</B>. The default color for text.
	<LI><B>linkColor</B>. The color for unvisited links.
	<LI><B>visitedColor</B>. The color for visited links.
	<LI><B>inputBG</B>. The background color for input elements.
	<LI><B>inputColor</B>. The text color for input elements.
	<LI><B>borderColor</B>. The border color (e.g., to surround
	  an IFRAME).
	<LI><B>bquoteBGColor</B>. The background color for block-quoted
	  text.
	<LI><B>frameFraction</B>. The fraction of an HTML frame that an
	  IFRAME should consume (there should be at most one IFRAME in
	  a request).
	<LI><B>type</B>. The type of a document or request (e.g., "waiver",
	  "document", "request", "order", "suggestion").
	<LI><B>document</B>. The URL of a document to sign.
	<LI><B>digest</B>. The SHA-256 digest of the document, represented
	  as a series of hexadecimal digits.
	<LI><B>documentURL</B>. The URL of the document to display in an
	  IFRAME. Normally this is the same as the value of <B>document</B>,
	  but for plain text documents, a URL with a <B>data</B> schema
	  will be used instead.
	<LI><B>sendto</B>. The address to which email is to be sent.
	<LI><B>cc</B>. An optional additional address to which email should
	  be sent.
	<LI><B>subject</B>. The subject of an email.
	<LI><B>sigserver</B>. The URL for the signature server. For
	  example, <B>https://example.com/docsig/</B>.
	<LI><B>email</B>. The mail address of the sender.
	<LI><B>id</B>. An optional ID for the sender.
	<LI><B>transid</B>. An optional transaction ID.
	<LI><B>additionalFormElements</B>. An optional series of HTML
	  elements to extend the form as described
	  <A HREF="#afe">above</A>.
      </UL>

      <H1><A ID="startup">Starting the DOCSIG server</A></H1>
    <P>
      There is a <A HREF="https://hub.docker.com/r/wtzbzdev/docsig">
	DOCSIG docker image</A>, with instructions
      on how to configure it. If docker is installed, this image can
      be used to install and start a Docsig server. The remainder of
      this section describes how to install and configure a DOCSIG
      server directly.
    <P>
      If a directory DIR contains the files, or symbolic links to the
      files, one can run the command
      <BLOCKQUOTE><PRE>

	  java -p <A HREF="#dir">DIR</A> -m org.bzdev.docsig  <A HREF="#wdir">WDIR</A> <A HREF="#cfile">CFILE</A>
      </PRE></BLOCKQUOTE>
    <P>
      where
      <UL>
	<LI><A ID="dir">DIR</A> is a directory containing the file, or symbolic
	  links to the files, <B>docsig-web.jar</B>, <B>libbzdev-base.jar</B>,
	  and <B>libbzdev-ejws</B>.
	<LI><A ID="wdir">WDIR</A> is a directory for files created by
	  the server.  If this argument is missing, the server will
	  not be able to provide previously used public keys, nor this
	  introduction.
	<LI><A ID="cfile">CFILE</A> is a configuration file (the file-name
	  extension is optional). If this
	  argument is missing, defaults will be used for all
	  configuration parameters, and specifically SSL will not be
	  configured. The file-name extension, which includes the period,
	  is optional: if not present, an extension will be added. DOCSIG
	  will try ".YML", ".yml", '.YAML", '.yaml, ".CONFIG", and
	  ".config" in that order until an existing file is found. If an
	  existing file is not found, ".config" is used as an extension
	  and a basic configuration file will be created.
      </UL>
    <P>
      DOCSIG also uses the environment variable
      <STRONG>DOCSIG_LOCALHOST</STRONG>, if set, to provide a replacement
      for the host name <STRONG>localhost</STRONG>.  When DOCSIG reads a
      document specified by a URL, with Docker's default networking,
      the host name <STRONG>localhost</STRONG> refers to the container,
      not the system running the container.  On a Linux or Unix system,
      adding
      <BLOCKQUOTE><PRE>

-e DOCSIG_LOCALHOST=`hostname`
</PRE></BLOCKQUOTE>
    <P>
      to a docker <STRONG>run</STRONG> command will replace
      <STRONG>localhost</STRONG> with the hostname of the system on which
      the container is running so that an HTTP request goes to the
      system instead of the container running DOCSIG.

      <H1><A ID="validation">Validating email and generating tables</A></H1>

      If the email messages containing signatures are saved
      in the <STRONG>mbox</STRONG> format, these can be processed
      using DOCSIG's validation software as described in the following
      sections:
      <UL>
	<LI><A HREF="#installation">Installation</A>.
	<LI><A HREF="#scripting">Scripting</A>.
	<LI><A HREF="#libdoc">Library documentation</A>.
      </UL>
      It is possible, however, to configure the DOCSIG server to
      generate tables.  In this case, a YAML configuration file must
      be used. the "contexts" list should contain the following (the
      prefix can be changed if desired):
      <BLOCKQUOTE><PRE>

  - prefix: /table/
    className: ServletWebMap
    arg: org.bzdev.docsig.TableAdapter
    parameters:
      accentColor: green
    propertyNames:
      - color
      - bgcolor
      - inputFGColor
      - inputBGColor
    methods:
      - GET
      - POST

</PRE></BLOCKQUOTE>
      Then a URL such as https://localhost/table/ will provide an HTML
      page containing a form that will download a table in CSV (Comma
      Separated Values) format describing the signatures found in a
      a mail file that uses MBOX format. The form uses the following
      <A ID="tableControls">controls</A>:
      <UL>
	<LI><STRONG>mbox</STRONG>. The name of the mbox file to upload.
	<LI><STRONG>showHeadings</STRONG>. The value <B>true</B> when
	  the table should contain headings naming each column.
	<LI><STRONG>expectedDocument</STRONG>. When provided, the value
	  is the URL of the document being signed. This value will be
	  compared to that in each email.
	<LI><STRONG>expectedDigest</STRONG>.. When provided, the value
	  is the message digest of the document being signed. This
	  value will be compared to that in each email.
	<LI><STRONG>expectedServer</STRONG>. When provided, the value
	  is the URL of for the server generating the signature. This
	  value will be compared to that in each email.
	<LI><STRONG>mode</STRONG>. The value can
	  be <STRONG>all</STRONG> to show all entries (the
	  default), <STRONG>valid</STRONG> to show only those whose
	  status is <STRONG>true</STRONG>, or <STRONG>invalid</STRONG>
	  to show only those whose status is <STRONG>false</STRONG>.
	<LI><STRONG>columns</STRONG>. A comma-separated list of
	  identifiers describing each column that will be
	  included. One can use a custom column name, although the
	  column will be left blank, but there are some reserved names
	  for specific quantities. These are
	  <A ID="columns"></A>
	  <UL>
	    <LI><STRONG>acceptedBy</STRONG>. The name of the person
	      signing the document.
	    <LI><STRONG>timestamp</STRONG>. The time the document
	      signature was generated.
	    <LI><STRONG>date</STRONG>. The date the document signature
	      was generated.
	    <LI><STRONG>timezone</STRONG>. The timezone used for the
	      date.
	    <LI><STRONG>ipaddr</STRONG>. The remote IP address the
	      server used when the document was signed.
	    <LI><STRONG>id</STRONG>. The ID of the person signing the
	      document (e.g., a member ID or some other number
	      assigned to the person signing the document). This field
	      is optional.
	    <LI><STRONG>transID</STRONG>. A transaction ID for the
	      document signature request.  This field is optional.
	    <LI><STRONG>email</STRONG>. The email address of the
	      person signing the document as entered by that person.
	    <LI><STRONG>server</STRONG>. The URL of the DOCSIG server.
	    <LI><STRONG>sendto</STRONG>. The address to which the
	      signature was sent.
	    <LI><STRONG>cc</STRONG>. The address of an alternate
	      recipient.  This field is optional.
	    <LI><STRONG>document</STRONG>. The URL of the document
	      being signed.
	    <LI><STRONG>type</STRONG>. The type of the document (for
	      example, "document", or "waiver").
	    <LI><STRONG>digest</STRONG>. The SHA-256 message digest of
	      the document being signed.
	    <LI><STRONG>publicKeyID</STRONG>. The public-key id for
	      the key used by the server when signing a message.
	    <LI><STRONG>signature</STRONG>. The digital signature of
	      the fields listed above.
	    <LI><STRONG>from</STRONG>. The email address that appears
	      in a message&apos;s <STRONG>from</STRONG> field.  This
	      should be the same as that for <STRONG>email</STRONG>
	      above.
	    <LI><STRONG>message-id</STRONG>. The message ID for the
	      message containing the signature.
	    <LI><STRONG>status</STRONG>. The status of the message
	      (true for valid and false if not valid).
	    <LI><STRONG>reasons</STRONG>. A string giving reasons when
	      the status is <STRONG>false</STRONG>.
	  </UL>
      </UL>
    <P>
      <A ID="tableCurl">To generate the table programmatically</A>, one
      should issue a POST request containing data whose media type is
      multipart/form-data and that provides the field names matching
      the control names shown above.  For <STRONG>curl</STRONG>, each
      <A HREF="#tableControls">control name</A> used has the argument
      <BLOCKQUOTE><PRE>

-F <I>NAME</I>=<I>VALUE</I>

</PRE></BLOCKQUOTE>
      where <I>NAME</I> is the name of a control and <I>VALUE</I> is
      the corresponding value. For <B>mbox</B>, the value must start with
      the character <B>@</B>, which tells <B>curl</B> that one is providing
      the name of the file to upload. For example
      <BLOCKQUOTE><PRE>

curl -F mbox=@inbox.mbox -F columns=acceptedBy,email,status \
     -F showHeadings=true -k https://localhost/table/

</PRE></BLOCKQUOTE>
      The <STRONG>-k</STRONG> option is usually not needed: it turns off some
      security checks so that (in the example) a self-signed certificate
      can be used.
    <P>
      If a CSV table is not appropriate, or additional filtering is
      needed, DOCSIG also provides jar files for the validation
      software. These are described below.

      <H2><A ID="installation">Installation</A></H2>
    <P>
      To use the validation software directly, Java must be installed:
      at least Java 11 and preferably Java 17. If the
      <A HREF="http://bzdev.org/">BZDev</A> class library has been
      installed, one should download a single file:
      <A HREF="/jars/docsig-web.jar">docsig-web.jar</A>. Otherwise,
      one should download
      <UL>
$(jars:endJars)	<LI><A HREF="/jars/$(name).jar">$(name).jar</A>
$(endJars)      </UL>
      and place all of these in the same directory.
      <A ID="sha256">The SHA-256 message digests</A> for these JAR files are:
      <UL>
$(jars:endJars)	<LI>$(name):<BR>$(md)
$(endJars)      </UL>
    <P>
      The program <CODE>shasum</CODE> (or similar programs) can be used
      to verify that the correct JAR file was downloaded. For example,
      <BLOCKQUOTE><PRE>

shasum -b -a 256 docsig-web.jar
      </PRE></BLOCKQUOTE>
      <H2><A ID="scripting">Scripting</A></H2>
    <P>
      If the BZDev class library is installed, one can use the
      command <CODE>scrunner</CODE> to use whatever scripting language
      a Java installation supports.  This will always include the
      scripting language
      <A HREF="/bzdev-api/org.bzdev.base/org/bzdev/util/doc-files/esp.html">ESP</A>,
      provided by the BZDev class library.
      For example,
      <BLOCKQUOTE><PRE>

cat MESSAGES.mbox | scrunner --exit -p docsig-verify.jar SCRIPT.esp
      </PRE></BLOCKQUOTE>
      where
      <UL>
	<LI><STRONG>MESSAGES</STRONG>.mbox is a file containing emails
	(in mbox format).
	<LI><STRONG>SCRIPT</STRONG>.esp is the file containing a script
      </UL>
      The argument <STRONG>-p</STRONG> tells scrunner to load an additional
      codebase (in this case, a JAR file), and <STRONG>--exit</STRONG>
      forces scrunner to exit as soon as scripts have been processed
      (otherwise scrunner may wait for the Java event-dispatch thread
      to detect that there are no more events to process).
    <P>
      If <CODE>scrunner</CODE> is not available, place all of the JAR
      files in a single directory (e.g., lib), and run the
      command
      <BLOCKQUOTE><PRE>

cat MESSAGES.mbox | java -p lib -m org.bzdev.scrunner \
      --add-modules org.bzdev.docsig.verify --exit  SCRIPT.esp
      </PRE></BLOCKQUOTE>
      The <CODE>--add-modules</CODE> option is needed in this case
      because docsig-verify.jar  was not not explicitly added to the
      module path (scrunner treats jar files on the module path as
      a special case).
    <P>
      The following ESP script will print out a description of a
      signature and some email headers:
      <BLOCKQUOTE><PRE>

import (org.bzdev.docsig.verify.DocsigVerifier);
import (org.bzdev.docsig.verify.DocsigVerifier.Result);
import (org.bzdev.net.HeaderOps);

var in = global.getReader();
var out = global.getWriter();
var err = global.getErrorWriter();

var results = decodeFromMbox(in, err);

var list = ["acceptedBy", "date", "ipaddr", "id",
            "transID", "email", "server", "sendto",
            "document", "digest", "publicKeyID"];

results.forEach(function(result) {
    var headers = result.getHeaders();
    out.println("**** status = " + result.getStatus()
                + ", sent from " + result.getEmailAddr()
                + " by " + result.getEmailName());
    out.println("**** reasons for failure = " + result.getReasons());
    out.println("**** message ID = " + result.getMessageID());
    list.forEach(function (name) {
        out.println(name + ": " + headers.getFirst(name));
    });
});

</PRE></BLOCKQUOTE>
    <P>
      While the ESP code above is easily read by anyone familiar with
      ECMAScript, Java, Python, etc., ESP has a few quirks:
      <UL>
	<LI> by design, it is a minimalist language.  It does not directly
	  implement iteration, instead using the Java streams packages.
	<LI> the import statements must list all classes used as argument
	  or returned by method or function calls. Functions are either
	  user defined, static methods from Java classes.
      </UL>
      The ESP implementation is optimized for a specific use case in
      which a script is used as an input to programs such as simulations,
      where a scripting language is used to configure various objects
      after which the scripting language gets out of the way.

    <P>
      The following script will print selected fields from signatures
      in CSV format (generated using the
  <A HREF="/bzdev-api/org.bzdev.base/org/bzdev/io/CSVWriter.html">CSVWriter</A>
      class from the BZDev class library),
      one line per email:
      <BLOCKQUOTE><PRE>

import (org.bzdev.docsig.verify.DocsigVerifier);
import (org.bzdev.docsig.verify.DocsigVerifier.Result);
import (org.bzdev.io.CSVWriter);
import (org.bzdev.net.HeaderOps);

var in = global.getReader();
var out = global.getWriter();
var err = global.getErrorWriter();

var results = decodeFromMbox(in, err);

var list = ["acceptedBy", "email", "date", "document"];

var csvw = new CSVWriter(out, list.size() + 1);

results.forEach(function(result) {
    var headers = result.getHeaders();
    list.forEach(function (name) {
        csvw.writeField(headers.getFirst(name));
    });
    csvw.writeField("" + result.getStatus());
});
csvw.close();

</PRE></BLOCKQUOTE>
    <P>
      Either script can be coded almost as easily using
      Java directly.

      <H2><A ID="libdoc">Library documentation</A></H2>
    <P>
      The examples above use classes from the
      <A HREF="http://bzdev.org/">BZDev class library</A>.
      Installation instructs are available
      <A HREF="https://billzaumen.github.io/bzdev/">on GitHub</A>.
      There are a number of Debian packages including ones for
      documentation.
    <P>
      For systems other than Linux, there is a list of
      <A HREF="https://billzaumen.github.io/bzdev/installers.html">installers</A>
      including one listed as <STRONG>bzdev(libbzdev-*)</STRONG>, which
      is a link to the current JAR file.  If that installer is
      downloaded, you can run the command
      <BLOCKQUOTE><PRE>

unzip <A HREF="#JARFILE">JARFILE</A> api.zip

</PRE></BLOCKQUOTE>
      where <A ID="JARFILE">JARFILE</A> is the name of the file that
      was downloaded.  This will extract just the file api.zip, which
      contains API documentation.

      <H1><A ID="proof">Verifying and trusting signatures</A></H1>
    <P>
      For a digital signature to be trusted, it is necessary to show that
      the document being signed was the one provided to an individual
      and that individual intended to sign it.
      <UL>
	<LI> Intent is shown by the use of a multi-stage process, ending
	  with the user explicitly sending an email message from the
	  user's account.
	<LI> The document being signed is referenced by a both a URL
	  (which gives its location) and by a SHA-256 message digest,
	  which is almost impossible to forge. SHA-255 is widely used for
	  digital signatures and is an industry standard, vetted by the
	  U.S. government.
	<LI> All or nearly all email providers provide digital signatures
	  on portions of email messages as a mechanism for eliminating
	  SPAM, which frequently forges the sender email address.  These
	  signatures are widely used by mainstream news organizations to
	  verify that some public official actually sent a particular
	  email.
      </UL>
      In addition, DOCSIG uses public-key encryption for its own
      digital signatures. These signatures are used to show that
      data used to generate emails have not been modified. Since
      the data can reconstruct email contents, one can also verify
      that the email contents have not been modified by a user. In
      other words, an organization has assurances that a signature
      sent by a user was the one actually generated.
    <P>
      In more detail,
      <UL>
	<LI>The DOCSIG server can be trusted to behave as described in
	  this document: he source code for the server is publicly
	  available and is short enough that it can easily checked
	  manually.
	<LI> The DOCSIG server will provide a digitally signed set of
	  values that include (in order)
	  <UL>
	    <LI> the signer's name.
	    <LI> a timestamp (time/date) for when the server provided
	      these values.
	    <LI> the date at which the server provide these values.
	    <LI> the timezone used in determining the date.
	    <LI> the remote IP address provided to the server.
	    <LI> if provided, the signer's ID.
	    <LI> if provided, the request's transaction ID.
	    <LI> the signer's email address.
	    <LI> a URL for the DOCSIG server itself (so one can verify
	      that the server is actually a known  DOCSIG server).
	    <LI> the intended recipient of the email.
	    <LI> if present, a "cc" field for the email.
	    <LI> The URL of the document being signed.
	    <LI> the SHA-256 message digest of the document to be signed,
	      with the digest computed by the DOCSIG server.
	    <LI> the ID for the key used to sign these values.
	  </UL>
	  While the public key is provided directly, one can also look
	  up the public key from the server to cross check it. These
	  values, plus some text, will be sent via email.
	<LI> To show who sent the document, one can use the FROM field
	  in that user's email message, which must match the one in
	  the digitally signed set of values. Email service providers
	  generally add headers containing digital signatures to show
	  that a message was sent from a specific account. Email
	  security protocols currently in use for this purpose include
	  <A HREF="https://www.csoonline.com/article/567357/3-email-security-protocols-that-help-prevent-address-spoofing-how-to-use-them.html">
	    DKIM, SPF, and DMARC</A>.
	  This sort of validation is used by
	  <A HREF="https://www.propublica.org/nerds/authenticating-email-using-dkim-and-arc-or-how-we-analyzed-the-kasowitz-emails">news organizations"</A>
	  when reporting about a public official's emails.
	  While it is theoretically possible to forge these
	  signatures, that would require help from someone with
	  administrative privileges such as an employee granted such
	  privileges by an email provider.
	<LI>The private keys that a DOCSIG server uses to sign messages
	  are kept in the server's memory and specifically never stored
	  permanently on a disk or other storage device, making forging
	  a DOCSIG server's signatures nearly impossible.
	<LI>Besides indicating when a signature was made, the timestamp,
	  together with the URL of the DOCSIG server, makes it possible to
	  show that a signature was not somehow added after the fact: any
	  attempt would require using a different server.
	<LI> GPG-signed (or PGP-signed) email for a fraction of the
	  signatures provides some additional proof that nothing is
	  being manipulated.  This is useful because it is a
	  completely independent way to verify who the sender is.
      </UL>

      <H1> <A ID="source">Source code</A></H1>
    <P>
      Source code is available on
      <A HREF="https://github.com/BillZaumen/docsig">GitHub</A>.
    <P>
      <H1><A ID="security">Security</A></H1>
    <P>
      DOCSIG's security is based on the following:
      <UL>
	<LI> DOCSIG is written in Java. This ostensibly eliminates
	  security exploits based on buffer overflow issues, but
	  of course is dependent on the JVM (Java virtual machine)
	  functioning as advertised.
	<LI> DOCSIG does not write anything except logging data and the
	  public keys it generates to the file system. The log does
	  not contain any information about a user, even an IP
	  address, and is provided merely for debugging server issues
	  (e.g., errors in the configuration file).
	<LI> DOCSIG does not use a database, so SQL-injection attacks
	  are not possible.  Similarly a DOCSIG server does not provide
	  any scripting capabilities.
	<LI> DOCSIG will typically be run in a Docker container, which
	  isolates DOCSIG server from the system on which it is running.
	  The standard DOCSIG Docker image was created using jlink to
	  create a stripped-down JRE (Java Runtime Environment). DOCSIG
	  itself uses the Java modules
	  <UL>
	    <LI> java.base,
	    <LI> java.desktop,
	    <LI> java.xml,
	    <LI> jdk.httpserver,
	    <LI> jdk.crypto.ec,
	  </UL>
	  and the Java library PJAC (used to obtained certificates
	  from Lets Encrypt) requires several additional modules:
	  <UL>
	    <LI> java.logging.
	    <LI> java.management.
	    <LI> java.naming.
	    <LI> java.sql.
	    <LI> jdk.crypto.cryptoki.
	    <LI> jdk.localedata.
	  </UL>
	  By using a stripped-down JRE, the "attack surface" is smaller,
	  and the JRE will load significantly faster and will use less
	  memory.
      </UL>
      <H1><A ID="extensibility">Extensibility</A></H1>
    <P>
      When a YAML configuration is used, the server can be extended
      by adding new entries in the YAML files <B>contexts</B> list.
      As stated above, the prefixes (the initial part of a URL's path)
      that are reserved are
      <UL>
	<LI><STRONG>/</STRONG>
	<LI><STRONG>/docsig</STRONG>
	<LI><STRONG>/docsig-api</STRONG>
	<LI><STRONG>bzdev-api</STRONG>
	<LI><STRONG>/jars</STRONG>
	<LI><STRONG>/PublicKeys</STRONG>
      </UL>
      as the DOCSIG software creates these automatically.
      Each element in the list will contain an object providing key/value
      pairs with the following keys:
      <UL>
	<LI><STRONG>prefix</STRONG>.  This is the initial portion of a
	  path. If it does not end in a '/', a '/' will be added.
	<LI><STRONG>className</STRONG>.
	  This is the class name of a subclass of
	  WebMap.
	  Supported values are
	  <UL>
	    <LI><STRONG>DirWebMap</STRONG>. When this is used, the value
	      of <STRONG>arg</STRONG> is the name of a directory.  For a
	      Docker image, the directory is one in an image's container.
	    <LI><STRONG>RedirectWebMap</STRONG>. When this is used, the value
	      of <STRONG>arg</STRONG> is a URL to which requests should be
	      redirected. The remainder of a request URL's path (the part after
	      the prefix) will be appended to the URL provided by this
	      argument.
	    <LI><STRONG>ZipWebMap</STRONG>. When this is used, the value
	      of <STRONG>arg</STRONG> is a ZIP file. For a Docker image,
	      the file name is the one used in the image's container.
	    <LI><STRONG>ResourceWebMap</STRONG>. When this is used, the value
	      of <STRONG>arg</STRONG> is the initial portion of a Java
	      resource name. This is not useful unless additional classes
	      are placed on DOCSIG&apos;s class path. The
	      <A HREF="/bzdev-api/org.bzdev.ejws/org/bzdev/ejws/maps/ResourceWebMap.html">ResourceWebMap documentation</A>
	      contains the relevant API documentation for this class.
	    <LI><STRONG>ServletWebMap</STRONG>. When this is used, the value
	      of <STRONG>arg</STRONG> is the name of a subclass of
	      <A HREF="/bzdev-api/org.bzdev.base/org/bzdev/net/ServletAdapter.html">ServletAdapter</A>. The values built into DOCSIG are
	      <UL>
		<LI><STRONG><A HREF="#requestAdapter">org.bzdev.docsig.RequestAdapter</A></STRONG>.
		  This servlet adapter generates signature requests: a
		  web page containing an HTML form that will also allow
		  one to view the document being signed.
		<LI><STRONG><A HREF="#sigAdapter">org.bzdev.docsig.SigAdapter</A></STRONG>. This servlet
		  adapter generates the email that the user will send to
		  sign a document.
		<LI><STRONG><A HREF="#tableAdapter">org.bzdev.docsig.TableAdapter</A></STRONG>. This
		  servlet adapter generates CSV (Comma Separated Values)
		  files from a file containing email in mbox format. For
		  an HTTP GET request, the servlet provides a web page
		  with a form to fill out. For an HTTP POST request,
		  this servlet adapter will generate the CSV file.
	      </UL>
	      The parameters for these are described below.
	  </UL>
	<LI><STRONG>arg</STRONG>. The argument used to figure a WebMap
	  are listed above.
	<LI><STRONG>useHTTP</STRONG>. When the value is <STRONG>true</STRONG>,
	  the context will be used with HTTP but not HTTPS.
	<LI><STRONG>welcome</STRONG>. The argument is list of 'welcome'
	  pages, providing a path name relative to the prefix.
	<LI><STRONG>parameters</STRONG>. The value is an object whose
	  property names and corresponding values are used as parameter
	  names and values, passed to a servlet adapter when it is
	  initialized.
	<LI><STRONG>propertyNames</STRONG>. The value is a list of
	  property names provided in the files "config" section.
	  These names and their values are then added as parameters.
	<LI><STRONG>methods</STRONG>. The value is a list of
	  the methods (DELETE, GET, HEAD, OPTIONS, POST, PUT, TRACE)
	  that are supported.
	<LI><STRONG>nowebxml</STRONG>. If the value is <STRONG>true</STRONG>,
	  any web.xml file present will be ignored.
	<LI><STRONG>displayDir</STRONG>. If the value
	  is <STRONG>true</STRONG>, directories should be displayed if
	  possible. Otherwise directories are not displayed.
	<LI><STRONG>hideWebInf</STRONG>. If the value
	  is <STRONG>true</STRONG>, a WEB-INF directory will not be
	  displayed.
	<LI> For the web maps that are listed above and that provide
	  directories, several colors can be defined for use with
	  when those directories are displayed. These are
	  <UL>
	    <LI><STRONG>color</STRONG>. The CSS color for text.
	    <LI><STRONG>bgcolor</STRONG>. The CSS color for the background
	    <LI><STRONG>linkColor</STRONG>. The CSS color for links.
	    <LI><STRONG>visitedColor</STRONG>. The CSS color for visited
	      links.
	  </UL>
	  There are defaults for each color, so only those that are
	  changed from its default has to be included.  These colors
	  do not apply when the class name is RedirectWebMap or
	  ServletWebMap.
	<LI><STRONG>trace</STRONG>. This should
	  be <STRONG>true</STRONG> to turn on tracing for this
	  prefix. The default is <STRONG>false</STRONG>.
	<LI><STRONG>stacktrace</STRONG>. This should
	  be <STRONG>true</STRONG> to turn if tracing should include a
	  stack trace. The default is <STRONG>false</STRONG>.
      </UL>
    <P>
      The following contains a description of the standard servlet
      adapters
      <H2><A ID="requestAdapter">RequestAdapter</A></H2>
      <P>
      The parameters are listed <A HREF="#requestParameters">above</A>.
      <P>
	Request adapters will provide an HTML form, possibly with fields
	for the user to fill out. This form will also include a
	button to submit it. The query string is described
	<A HREF="#requestQS">above</A>.  With the default template,
	the only data requested by the form will be the user's name
	and email address.
	

      <H2><A ID="sigAdapter">SigAdapter</A></H2>

      <P>
	Signature adapters generate the email messages that must
	be sent to submit a signature.
	The parameters are
	<UL>
	  <LI><STRONG>bgcolor</STRONG>. The background color.
	  <LI><STRONG>color</STRONG>. The foreground color.
	  <LI><STRONG>linkColor</STRONG>. The color for links.
	  <LI><STRONG>visitedColor</STRONG>. The color for visited links.
	  <LI><STRONG>buttonBGColor</STRONG>. The background color for the
	    'send' button.
	  <LI><STRONG>buttonFGColor</STRONG>. The foreground color for the
	    'send' button.
	  <LI><STRONG>bquoteBGColor</STRONG>. The background color for
	    block-quoted text.
	  <LI><STRONG>timezone</STRONG>. The time zone to  use when a date
	    is printed.
	  <LI><STRONG>logFile</STRONG>. The name of a log file.  This must
	    be different for each instance of this class.
	  <LI><STRONG>publicKeyDir</STRONG>. The directory containing public
	    the keys that were generated. This should be the same for
	    all signature adapters.
	  <LI><B>emailTemplateURL</B>. This is an optional parameter.
	    When present, it is the URL that provides a template file
	    suitable for creating emails.  If the parameter
	    <A HREF="#afe">additionalFormElements</A> is set, the
	    <B>emailTemplateURL</B> option can provide a template that
	    allows the values of the additional elements to be displayed
	    in an email.
	  <LI><B>nthreads</B>. This value should be at least the number
	    of threads available. It is used by an instance of
	    <B>SigAdapter</B> to set a cache size.
	  <LI><B>numberOfDocuments</B>. This value should be the number
	    of distinct documents that an instance of <B>SigAdapter</B>
	    will handle simultaneously.  It is used to set a cache size.
	</UL>
      <P>
	The parameters sent in an HTTP POST request for a SigAdapter
	are described <A HREF="#forms">above</A>. The query string for
	HTTP GET requests are also described <A HREF="#queries">above</A>.

      <H2><A ID="tableAdapter">TableAdapter</A></H2>
      <P>
	Table adapters are used to create CSV files that can be
	imported into spreadsheets or data bases.
	The parameters are
	<UL>
	  <LI><STRONG>bgcolor</STRONG>. The background color.
	  <LI><STRONG>color</STRONG>. The foreground color.
	  <LI><STRONG>inputBGColor</STRONG>. The background color for a
	    form&apos;s input field.
	  <LI><STRONG>inputFGColor</STRONG>. The foreground color for a
	    form&apos;s input field.
	  <LI><STRONG>accentColor</STRONG>. This color is used in
	    selected checkboxes and selected radio buttons.
	</UL>
      <P>
	For an HTTP POST, the parameters that should be sent have the
	same name and values as the controls listed
	<A HREF="#tableControls">above</A>. The request can be
	made <A HREF="#tableCurl">programmatically</A>, which is useful
	for scripting.
  </body>
</HTML>

<!--  LocalWords:  bgcolor linkColor visitedColor BLOCKQUOTE li px br
 -->
<!--  LocalWords:  bquoteBGColor DOCSIG PEM Encodings SHA mailto http
 -->
<!--  LocalWords:  docsig transID sigserver sendto https TCP GZIP UTF
 -->
<!--  LocalWords:  encodings acceptedBy ipaddr whitespace transId pem
 -->
<!--  LocalWords:  publicKeyID withECDSA PublicKey publicKeyRequest
 -->
<!--  LocalWords:  url SSL sslType TLS keyStoreFile keystore changeit
 -->
<!--  LocalWords:  keyStorePassword rgb buttonFGColor buttonBGColor
 -->
<!--  LocalWords:  trustStoreFile trustStorePassword allowLoopback md
 -->
<!--  LocalWords:  loopback allowSelfSigned TLS nthreads WDIR CFILE
 -->
<!--  LocalWords:  libbzdev ejws mbox Docsig's BZDev endJars shasum
 -->
<!--  LocalWords:  scrunner codebase getReader getWriter forEach CSV
 -->
<!--  LocalWords:  getErrorWriter decodeFromMbox getHeaders println
 -->
<!--  LocalWords:  getStatus getEmailAddr getEmailName getMessageID
 -->
<!--  LocalWords:  getFirst csvw CSVWriter writeField bzdev JARFILE
 -->
<!--  LocalWords:  api hasKeyRequest getPublicKeys stackTrace getLog
 -->
<!--  LocalWords:  newDocsigConfig DOCSIG's getReasons GPG LOCALHOST
 -->
<!--  LocalWords:  localhost Docker's hostname DKIM runzip deployable
 -->
<!--  LocalWords:  microservice DocuSign DocuSeal DMARC keyPassword
 -->
<!--  LocalWords:  certificateManager wtzbzdev ejwscert certbot yml
 -->
<!--  LocalWords:  certName timeOffset stopDelay helperPort AAAA DNS
 -->
<!--  LocalWords:  config github cp AcmeClient certMode YAML yaml JVM
 -->
<!--  LocalWords:  ConfigurableWS PublicKeys jlink JRE Runtime xml
 -->
<!--  LocalWords:  jdk httpserver PJAC sql localedata certlog busybox
 -->
<!--  LocalWords:  CMD alwaysCreate servlet className ServletWebMap
 -->
<!--  LocalWords:  arg inputBGColor inputFGColor borderColor IFRAME
 -->
<!--  LocalWords:  fillText textFill documentURL defs propertyNames
 -->
<!--  LocalWords:  transid Extensibility steelblue accentColor URL's
 -->
<!--  LocalWords:  showHeadings expectedDocument expectedDigest
 -->
<!--  LocalWords:  expectedServer programmatically WebMap DirWebMap
 -->
<!--  LocalWords:  RedirectWebMap ZipWebMap ResourceWebMap useHTTP
 -->
<!--  LocalWords:  ServletAdapter nowebxml displayDir hideWebInf
 -->
<!--  LocalWords:  stacktrace RequestAdapter SigAdapter logFile
 -->
<!--  LocalWords:  publicKeyDir TableAdapter checkboxes FIELDSET
 -->
<!--  LocalWords:  additionalFormElements emailTemplateURL unvisited
 -->
<!--  LocalWords:  emailTemplateDigest inputBG inputColor
 -->
<!--  LocalWords:  frameFraction numberOfDocuments
 -->
