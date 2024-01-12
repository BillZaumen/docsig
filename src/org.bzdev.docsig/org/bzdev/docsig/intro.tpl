<!DOCTYPE html>
<html lang="en">
  <head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <style>
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
  <TITLE>Docsig Documentation</TITLE>
  </head>
  <body>
  <H1>Docsig Documentation</H1>
    <P>
      Links:
      <UL>
	<LI><A HREF="#intro">Introduction</A>.
	<LI><A HREF="#forms">Forms</A>.
	<LI><A HREF="#pem">PEM Encodings</A>.
	<LI><A HREF="#config">Configuration</A>.
	<LI><A HREF="#stgartup">Startup</A>.
	<LI><A HREF="#validation">Validation</A>.
	  The following links can be used to install JAR files and
	  API documentation:<P>
	  <UL>
	    <LI><A HREF="/api/api/index.html">API for validation</A>
	    <LI><A HREF="/jars/">JAR files for validation</A>.
	    <LI><A HREF="/api.zip">ZIP file for installing API documentation</A>.
	    <LI><A HREF="/jars.zip"> ZIP file for downloading JAR files</A>.
	  </UL><P>
	    <A HREF="#sha256">SHA-256 message digests</A> for JAR files
	    should be checked.
	<LI><A HREF="#source">Source code</A>.
      </UL>
  <H1><A ID="intro"></A> Introduction</H1>
    <P>
      Docsig is a server that supports the use of digital
      signatures for simple documents that require a single
      signature. This server will respond to a request by
      generating a web page with a link to the document that
      is to be signed, together with the document&apos;s SHA-256
      message digest. This web page will also have a 'mailto'
      link that will set up an email message that can be sent
      to submit the signature.

      <H1><A ID="forms"></A>HTML Forms</H1>
    <P>
      To use Docsig, a web site has to return a
      an HTML file containing an HTML form as shown below:
      <BLOCKQUOTE><PRE>

 &lt;form action="<A HREF="#HTTP">HTTP</A>://<A HREF="#DS_SERVER">DOCSIG_SERVER</A>/docsig/" method="post"&gt;
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
      <LI><A ID="myid">MY_ID</A> is an optional ID associated with the
	person submitting a signature.  The corresponding input may
	be hidden, or the user can be required to enter it.
      <LI><A ID="TID">TRANSACTION_ID</A> is an optional transaction ID.
	The field should be hidden, but may be reproduced text.
      <LI><A ID="DS_SERVER">DOCSIG_SERVER</A> is the the host name for
	the Docsig server, optionally followed by a ":" and the TCP
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
    <P>
      When a user clicks the 'Continue' button on the form shown above,
      the Docsig server will respond by providing a web page that contains
      a link to the document and its SHA-256 message digest. That link
      will either produce a document matching that message digest or will
      result in a 404 (Not Found) error.  When the 'Click to Send'
      button is pressed, the user&apos;s browser should open a window for
      composing email. This email will contain some easily readable
      text indicating that a specific document is being signed, plus
      some PEM encoded data used for processing.

      <H1><A ID="pem"></A>PEM encodings</H1>
      <P>
      When the PEM encoding is removed, the result is a GZIP-encoded
      file that contains several headers followed by a PEM-encoded public
      key provide by the Docsig server. Each header name is followed
      immediately by a colon and then a space. The value for the
      header is the text in the rest of the line, which is terminated
      by a carriage return followed by a line feed. The headers are
      (in order)
      <UL>
	<LI><B>acceptedBy</B>. This is the user name entered in the form above.
	<LI><B>date</B>. This is date the signature was generated.
	<LI><B>ipaddr</B>. This is the IP address seen by the Docsig server when the
	  user submitted a request.
	<LI><B>id</B>. This is the optional ID field. It may be an account/membership
	  number, driver-license number, etc.  The only constraint is that
	  it cannot contain whitespace, which will be stripped off if present.
	<LI><B>transId</B>. This is the optional transaction ID field.  It
	  will typically be used as a reference for some transaction.
	  The only constraint is that it cannot contain whitespace,
	  which will be stripped off if present.
	<LI><B>email</B>. This is the user&apos;s email address, as typed in.
	<LI><B>server</B>. This is value
	  <A HREF="#HTTP">HTTP</A>://<A HREF="#DS_SERVER">DOCSIG_SERVER</A>.
	<LI><B>sendto</B>. This is the value of
	  <A HREF="#emr">EMAIL_RECIPIENT</A>.
	<LI><B>document</B>. This
	is the URL for the document being signed.
	<LI><B>digest</B>. This is the SHA-256 message digest of the document
	  being signed (represented as a string of hexadecimal digits
	  using lower-case letters).
	<LI><B>publicKeyID</B>. This is the SHA-256 message digest of the
	  PEM-encoded public key file (represented as a string of
	  hexadecimal digits using lower-case letters).
	<LI><B>signature</B>. This is the digital signature of the values of the
	  headers listed above, in the order listed, with each
	  terminated by a carriage return followed by a line feed,
	  with this signature represented as a string of hexadecimal
	  digits using lower-case letters. When the signature is computed
	  all the fields signed will use a UTF-8 character encoding.
      </UL>
      The PEM encoded public key has an initial line with a header
      named "signature-algorithm" specifying a signature algorithm.
      The header&apos;s name is also followed by a colon and a space,
      in turn followed by the standard name for a signature algorithm.
      Typically this will be SHA256withECDSA. This is again followed
      by a carriage-return and a line feed. The final component is
      the PEM encoded public key itself.
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
	  key from the specified Docsig server and these should match
	  the ones used.  The corresponding private key is kept on the
	  Docsig server in memory and is never written to persistent
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
      <H1><A ID="config"></A>Configuring the Docsig server</H1>
      A Docsig server is configured used a configuration file that
      provides a series of name-valued pairs:
      <UL>
	<LI><A HREF="#colors">Configuring CSS colors</A> describes
	  how to set colors used by various web pages.
	<LI><A HREF="#SSL">SSL properties</A> describes properties
	  specific to SSL
	<LI><A HREF="#PARMS">Server parameters</A> describes how to
	  configure general server parameters.
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
      would provide a basic HTTPS configuration.

      <H2><A ID="colors"></A>Configuring CSS colors</H2>
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
	  missing,  a default, <B>rgb(65,225,128</B>, will be used.
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
      </UL>
      
      <H2><A ID="SSL"></A>SSL properties</H2>
    <P>
      The SSL properties are the following:
      <UL>
	<LI><B>keyStoreFile</B>. The value is the file name of the
	  key store, which is used to hold the server&apos;s certificate.
	  Relative file names are resolved against the directory containing
	  the configuration file.
	<LI><B>keyStorePassword</B>. The value is the password used by
	    the keystore file.
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
      </UL>
      When HTTPS is used on the Internet, the only properties that should
      be set are <B>keyStoreFile</B> and <B>keyStorePassword</B>.

      <H2><A ID="PARMS"></A>Server parameters</H2>
    <P>
      The server parameters are the following:
      <UL>
	<LI><B>sslType</B>. When present, the values should be either
	  <B>SSL</B> or <B>TSL</B>. TSL is preferred. When absent or
	  when the value is <B>null</B>, HTTP is used instead of HTTPS.
	<LI><B>ipaddr</B>. The value is the numeric IP address on which
	  this server listens, with two special values:
	  <P>
	  <UL>
	    <LI><B>wildcard</B>. The server will use the wildcard address.
	    <LI><B>loopback</B>. The server will use the loopback
	      address.
	  </UL>
	<LI><B>port</B>. The value is the server&apos;s TCP port.  If missing
	  the port is by default set to 8080 for HTTP and 8443 for HTTPS.
	<LI><B>backlog</B>. When present, the value is an integer providing
	  the <A HREF="https://veithen.io/2014/01/01/how-tcp-backlog-works-in-linux.html">TCP backlog</A>.
          The default value is 30.
	<LI><B>nthreads</B>.
	  The number of threads the server can use. The default is 50.
	<LI><B>trace</B>.  A value of <B>true</B> indicates that the
	  execution of a request will be traced, printing out what
	  occurred on standard output.  The default is <B>false</B>.
      </UL>

      <H1><A ID="startup"></A>Starting the Docsig server</H1>
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
	  links to the files, <B>docsig.jar</B>, <B>libbzdev-base.jar</B>,
	  and <B>libbzdev-ejws</B>.
	<LI><A ID="wdir">WDIR</A> is a directory for files created by
	  the server.  If this argument is missing, the server will
	  not be able to provide previously used public keys, nor this
	  introduction.
	<LI><A ID="cfile">CFILE</A> is a configuration file. If this
	  argument is missing, defaults will be used for all
	  configuration parameters, and specifically SSL will not be
	  configured.
      </UL>
      Alternatively, one can use Docker.

      <H1><A ID="validation"></A>Validating email</H1>

      If the email messages containing signatures are saved
      in the <STRONG>mbox</STRONG> format, these can be processed
      using Docsig's validation software.

      <H2>Installation</H2> To use the validation software, Java must
      be installed: at least Java 11 and preferably Java 17. If
      the <A HREF="http://bzdev.org/">BZDev</A> class library has been
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
      <H2>Scripting</H2>
    <P>
      If the BZDev class library is installed, one can use the
      command <CODE>scrunner</CODE> to use whatever scripting language
      a Java installation supports.  This will always include the
      scripting language ESP, provided by the BZDev class library.
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
      in CSV format (generated using a class from the BZDev library),
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

      <H2>Library Documentation</H2>
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

<H1> <A ID="source"></A>Source code</H1>
    <P>
      Source code is available on
      <A HREF="https://github.com/BillZaumen/docsig">GitHub</A>.

  </body>
</html>

<!--  LocalWords:  bgcolor linkColor visitedColor BLOCKQUOTE li px br
 -->
<!--  LocalWords:  bquoteBGColor Docsig PEM Encodings SHA mailto http
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
<!--  LocalWords:  loopback allowSelfSigned TSL nthreads WDIR CFILE
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
<!--  LocalWords:  api
 -->
