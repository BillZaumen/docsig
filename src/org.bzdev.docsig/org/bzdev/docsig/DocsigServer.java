package org.bzdev.docsig;
import java.io.File;
import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.bzdev.ejws.*;
import org.bzdev.ejws.maps.*;
import org.bzdev.net.HttpMethod;
import org.bzdev.net.SSLUtilities;
import org.bzdev.util.TemplateProcessor;
import org.bzdev.util.TemplateProcessor.KeyMap;

public class DocsigServer {

    static final Charset UTF8 = Charset.forName("UTF-8");

    // Config names, used to check for misspellings in the
    // configuration file.
    static final Set<String> propertyNames =
	 Set.of("color", "bgcolor", "linkColor", "visitedColor",
		"buttonFGColor", "buggonBGColor", "bquoteBGColor",
		"ipaddr", "port", "helperPort", "backlog", "nthreads",
		"trace", "stackTrace",
		"keyStoreFile", "trustStoreFile", "sslType",
		"keyStorePassword", "keyPassword", "trustStorePassword",
		"allowLoopback", "allowSelfSigned",
		"certificateManager",
		"certName", "domain", "email", "timeOffset",
		"interval", "stopDelay");

    // required property names if a certificate manager is used.
    static final Set<String> cmPropertyNames =
	Set.of("sslType", "domain", "keyStoreFile");

    public static void main(String argv[]) throws Exception {
	boolean defaultTrace = false;
	boolean defaultStacktrace = false;
	int offset = 0;
	if (argv.length > 0) {
	    if (argv[0].equals("--stackTrace")) {
		offset++;
		defaultTrace = true;
		defaultStacktrace = true;
	    } else if (argv[0].equals("--trace")) {
		offset++;
		defaultTrace = true;
	    }
	}
	if (argv.length == offset) {
	    throw new IllegalStateException("no arguments");
	}

        System.setProperty("java.awt.headless", "true");

	// CSS colors.
	String color = "white";
	String bgcolor = "rgb(10,10,25)";
	String linkColor = "rgb(65,225,128)";
	String visitedColor = "rgb(65,164,128)";
	String buttonFGColor = "white";
	String buttonBGColor = "rgb(10,10,64)";
	String bquoteBGColor = "rgb(32,32,32)";

	int port = 80;
	int helperPort = 0;
	boolean setHelperPort = false;
	int backlog = 30;
	int nthreads = 50;
	boolean trace = defaultTrace;
	boolean stacktrace = defaultStacktrace;
	InetAddress addr = null;

	EmbeddedWebServer.SSLSetup sslSetup = null;
	CertManager cm = null;
	String sslType = null;
	File keyStoreFile = null;
	char[] keyStorePW = null;
	char[] keyPW = null;
	File trustStoreFile = null;
	char[] trustStorePW = null;
	boolean loopback = false;
	boolean selfsigned = false;
	String certName = "docsig";
	String domain = null;
	String email = null;
	int timeOffset = 0;
	int interval = 90;
	int stopDelay = 5;

	File cdir = new File(System.getProperty("user.dir"));
	File uadir = new File("/usr/app");
	File logFile = (cdir.equals(uadir))? new File(uadir, "docsig.log"):
	    null;

	PrintStream log = (logFile != null)?
	    new PrintStream(new FileOutputStream(logFile), true, UTF8):
	    System.out;


	// System.out.println("argv.length = " + argv.length);
	// System.out.println("offset = " + offset);
	// System.out.flush();
	if (argv.length > 1 + offset) {
	    File propFile = new File(argv[offset+1]);
	    String newconfig = System.getenv("newDocsigConfig");
	    if (newconfig != null && newconfig.equals("true")) {
		File  cf = new File("argv[offset+1]");
		PrintWriter w = new PrintWriter(propFile, UTF8);
		for (String name: propertyNames) {
		    String value = System.getenv(name);
		    if (value != null) {
			w.println(name + " = " + value);
		    }
		}
		w.flush();
		w.close();
		System.exit(0);
	    }

	    log.println("Config file  = " + propFile);
	    File dir = propFile.getParentFile();
	    if (dir == null) dir = new File(System.getProperty("user.dir"));
	    if (propFile.canRead()) {
		// System.out.println("propFile is readable");
		Reader r = new FileReader(propFile, UTF8);
		Properties props = new Properties();
		props.load(r);
		r.close();
	        color = props.getProperty("color", color);
		bgcolor = props.getProperty("bgcolor", bgcolor);
		linkColor = props.getProperty("linkColor", linkColor);
		visitedColor = props.getProperty("visitedColor", visitedColor);
		buttonFGColor = props.getProperty("buttonFGColor",
						  buttonFGColor);
		buttonBGColor = props.getProperty("buttonBGColor",
						  buttonBGColor);
		bquoteBGColor = props.getProperty("bquoteBGColor",
						  bquoteBGColor);

		log.println("color = " + color);
		log.println("bgcolor = " + bgcolor);
		log.println("linkColor = " + linkColor);
		log.println("visitedColor = " + visitedColor);
		log.println("buttonFGColor = " + buttonFGColor);
		log.println("buttonBGColor = " + buttonBGColor);
		log.println("bquoteBGColor = " + bquoteBGColor);

		String s = props.getProperty("ipaddr");
		log.println("ipaddr = " + s);
		if (s != null) s = s.trim();
		if (s == null || s.equalsIgnoreCase("wildcard")) {
		    addr = null;
		} else if (s.equalsIgnoreCase("loopback")) {
		    addr = InetAddress.getLoopbackAddress();
		} else {
		    addr = InetAddress.getByName(s);
		}
		s = props.getProperty("backlog");
		backlog = (s == null)? backlog: Integer.parseInt(s);
		s = props.getProperty("nthreads");
		nthreads = (s == null)? nthreads: Integer.parseInt(s);
		s = props.getProperty("trace");
		trace = (s == null)? trace: Boolean.parseBoolean(s);
		s = props.getProperty("stackTrace");
		stacktrace = (s == null)? stacktrace: Boolean.parseBoolean(s);

		log.println("backlog = " + backlog);
		log.println("nthreads = " + nthreads);
		log.println("trace = " + trace);
		log.println("stackTrace = " + stacktrace);

		// System.out.println("trace = " + trace);
		// System.out.println("stacktrace = " + stacktrace);
		// System.out.flush();

		s = props.getProperty("certificateManager");
		if (s != null) {
		    log.println("certificateManager = " + s);
		    cm = CertManager.newInstance(s.strip());
		    if (cm == null) {
			log.println("... certificatManager not recognized");
		    }
		}

		s = props.getProperty("keyStoreFile");
		log.println("keyStoreFile = " + s);
		if (s != null) {
		    Path path = Path.of(s);
		    if (path.isAbsolute()) {
			keyStoreFile = path.toFile();
		    } else {
			keyStoreFile = dir.toPath().resolve(path).toFile();
		    }
		    if (cm == null) {
			if (keyStoreFile.isDirectory()) {
			    keyStoreFile = null;
			    log.println("... keyStoreFile is a directory");
			} else if (!keyStoreFile.canRead()) {
			    keyStoreFile = null;
			    log.println("... keyStoreFile not readable");
			}
		    }
		}
		s = props.getProperty("trustStoreFile");
		log.println("trustStoreFile = " + s);
		if (s != null) {
		    Path path = Path.of(s);
		    if (path.isAbsolute()) {
			trustStoreFile = path.toFile();
		    } else {
			trustStoreFile = dir.toPath().resolve(path).toFile();
		    }
		    if (trustStoreFile.isDirectory()) {
			log.println("... trustStoreFile is a directory");
			log.println("... trustStoreFile ignored");
			trustStoreFile = null;
		    } else if (!trustStoreFile.canRead()) {
			log.println("... trustStoreFile not readable");
			log.println("... trustStoreFile ignored");
			trustStoreFile = null;
		    }
		}
		
		s = props.getProperty("sslType");
		if (s == null || (s.trim().length() == 0)
		    || s.trim().equals("null")) {
		    keyStoreFile = null;
		} else {
		    sslType = s.trim().toUpperCase();
		}
		log.println("sslType = " + sslType);

		s = props.getProperty("port");
		if (s == null) {
		    port = (sslType == null)? 80: 443;
		} else {
		    port = Integer.parseInt(s);
		}
		log.println("port = " + port);

		s = props.getProperty("helperPort");
		if (s == null) {
		    helperPort = (sslType == null)? 0: 80;
		} else {
		    helperPort = Integer.parseInt(s);
		    setHelperPort = true;
		}
		log.println("helperPort = " + helperPort);

		s = props.getProperty("allowLoopback");
		if (s != null && s.trim().equalsIgnoreCase("true")) {
		    loopback = true;
		}
		log.println("allowLoopback = " + loopback);
		s = props.getProperty("allowSelfSigned");
		if (s != null && s.trim().equalsIgnoreCase("true")) {
		    selfsigned = true;
		}
		log.println("allowSelfSigned = " + selfsigned);

		s = props.getProperty("keyStorePassword");
		keyStorePW = (s== null || keyStoreFile == null)? null:
		    s.toCharArray();
		log.println("keyStorePassword = "
			    + ((keyStorePW == null)? "null": "<redacted>"));

		s = props.getProperty("keyPassword");
		keyPW = (s == null)? keyStorePW: s.toCharArray();
		log.println("keyPassword = "
			    + ((keyPW == null)? "null": "<redacted>"));

		s = props.getProperty("trustStorePassword");
		trustStorePW = (s== null || trustStoreFile == null)? null:
		    s.toCharArray();
		log.println("trustStorePassword = "
			    + ((trustStorePW == null)? "null": "<redacted>"));

		certName = props.getProperty("certName", "docsig");
		domain = props.getProperty("domain");
		email = props.getProperty("email");
		s = props.getProperty("timeOffset");
		timeOffset = (s == null)? 0: Integer.parseInt(s);
		s = props.getProperty("interval");
		interval = (s == null)? 90: Integer.parseInt(s);
		s = props.getProperty("stopDelay");
		stopDelay = (s == null)? 5: Integer.parseInt(s);


		for (String key: props.stringPropertyNames()) {
		    if (!propertyNames.contains(key)) {
			log.println("Warning: config property \"" + key
				    + "\" not recognized.");
		    }
		}
		if (cm != null && domain != null) {
		    for (String nm: cmPropertyNames) {
			String value = props.getProperty(nm);
			if (value == null) {
			    log.println("Warning: " + nm + " missing");
			}
		    }
		}
	    } else {
		log.println("cannot read config file " + propFile);
		// System.out.println("propFile is not readable");
		// System.out.flush();
	    }
	}
	String DOCSIG_LOCALHOST = System.getenv("DOCSIG_LOCALHOST");
	if (DOCSIG_LOCALHOST != null) {
	    try {
		InetAddress iaddr = InetAddress.getByName(DOCSIG_LOCALHOST);
		log.println("DOCSIG_LOCALHOST (" + DOCSIG_LOCALHOST +") => "
			    + iaddr.getHostAddress());
	    } catch (Exception e) {
		log.println("DOCSIG_LOCALHOST (" + DOCSIG_LOCALHOST
			    + ") address not found");
	    }
	}
		    
	try {
	    boolean needHelperStart = false;
	    EmbeddedWebServer helper = null;
	    if (sslType != null) {
		if (loopback) {
		    SSLUtilities.allowLoopbackHostname();
		}
		if (selfsigned) {
		    SSLUtilities.installTrustManager(sslType,
						     trustStoreFile,
						     trustStorePW,
						     (cert) -> {return true;});
		} else if (trustStoreFile != null) {
		    SSLUtilities.installTrustManager(sslType,
						     trustStoreFile,
						     trustStorePW,
						     (cert) -> {return false;});
		}
		if (cm != null && domain != null) {
		    cm.setProtocol(sslType)
			.setInterval(interval)
			.setStopDelay(stopDelay)
			.setTimeOffset(timeOffset)
			.setCertName(certName)
			.setDomain(domain)
			.setKeystoreFile(keyStoreFile)
		        .setKeystorePW(keyStorePW)
			.setKeyPW(keyPW);

		    if (email != null) cm.setEmail(email);
		    int hport = cm.helperPort();
		    int hhport = (hport == 0)? helperPort: hport;
		    if (helperPort != 0 && setHelperPort
			&& hhport != helperPort) {
			log.println("Warning: helperPort ignored - "
				    + "conflicts with certificate manager");
		    }
		    helper = (hhport == 0)? null: new EmbeddedWebServer(hhport);
		    if (helper != null) {
			helper.add("/", RedirectWebMap.class,
				   "https://" + domain + ":" + port +"/",
				   null, true, false, true);
			if (hport != 0) {
			    cm.setHelper(helper);
			} else {
			    needHelperStart = (hhport != 0);
			}
		    }
		    // Don't need a trust store because no client
		    // authentication.
		} else {
		    if (domain == null) {
			log.println("warning: no domain but certificate "
				    + "manager specified");
			log.println("ignoring certificate manager");
		    }
		    cm = null;
		}
		sslSetup = (cm == null)?
		    new EmbeddedWebServer.SSLSetup(sslType): null;
		if (sslSetup != null && helperPort != 0) {
		    if (helperPort != port) {
			helper = new EmbeddedWebServer(helperPort);
			if (domain == null) domain = "localhost";
			helper.add("/", RedirectWebMap.class,
				   "https://" + domain + ":" + port +"/",
				   null, true, false, true);
			needHelperStart = true;
		    } else {
			log.println("Warning: HTTP and HTTPS servers using "
				    + "the same port");
			log.println("... will not start HTTP server");
		    }
		}
		if (cm == null && keyStoreFile != null && keyStorePW != null) {
		    // https://blog.syone.com/how-to-build-a-java-keystore-alias-with-a-complete-certificate-chain
		    // indicates how to set up a keystore so it contains
		    // the full certificate chain.
		    // Also see
		    // https://serverfault.com/questions/483465/import-of-pem-certificate-chain-and-key-to-java-keystore

		    sslSetup.keystore(new FileInputStream(keyStoreFile))
			.keystorePassword(keyStorePW);
		}
		/*
		 * We do not need a trust store because we don't requesat
		 * client authentication.
		 */
	    } else {
		// if sslType is null, we are using HTTP so any
		// certificate manager should be ignored.
		cm = null;
	    }
	    keyStorePW = null;
	    trustStorePW = null;

	    EmbeddedWebServer ews = (cm == null)?
		new EmbeddedWebServer(addr, port, backlog, nthreads, sslSetup):
		new EmbeddedWebServer(addr, port, backlog, nthreads, cm);

	    ews.setRootColors(color, bgcolor, linkColor, visitedColor);

	    Map<String,String> parameters = new HashMap<>();

	    if (argv.length > offset) {
		String fs = System.getProperty("file.separator");
		fs = argv[offset].endsWith(fs)? "": fs;
		parameters.put("publicKeyDir", argv[offset]
			       + fs + "PublicKeys");
	    }

	    parameters.put("color", color);
	    parameters.put("bgcolor", bgcolor);
	    parameters.put("linkColor", linkColor);
	    parameters.put("visitedColor", visitedColor);
	    parameters.put("buttonFGColor", buttonFGColor);
	    parameters.put("buttonBGColor", buttonBGColor);
	    parameters.put("bquoteBGColor", bquoteBGColor);
	    if (logFile != null) {
		String logPath = logFile.getCanonicalPath();
		parameters.put("logFile", logPath);
		log.println("logFile = " + logPath);
	    }

	    ews.add("/docsig", ServletWebMap.class,
		    new ServletWebMap.Config(new SigAdapter(),
					     parameters, true,
					     HttpMethod.GET,
					     HttpMethod.POST,
					     HttpMethod.HEAD,
					     HttpMethod.OPTIONS,
					     HttpMethod.TRACE),
		    null, true, false, true);

	    if (argv.length > offset) {
		File f = new File(argv[offset]);
		f.mkdirs();

		TemplateProcessor.KeyMap keymap =
		    new TemplateProcessor.KeyMap();
		keymap.put("color", color);
		keymap.put("bgcolor", bgcolor);
		keymap.put("linkColor", linkColor);
		keymap.put("visitedColor", visitedColor);
		keymap.put("bquoteBGColor", bquoteBGColor);

		String[] resourceNames = {
		    "docsig-verify",
		    "libbzdev-base",
		    "libbzdev-esp",
		    "libbzdev-math",
		    "libbzdev-obnaming",
		    "scrunner",
		};
	    
		TemplateProcessor.KeyMapList kmlist =
		    new TemplateProcessor.KeyMapList();
		Properties jarprops = new Properties();
		for (String name: resourceNames) {
		    String nm = name + ".jar";
		    InputStream ris = DocsigServer.class
			.getResourceAsStream(nm);
		    if (ris == null) {
			// System.out.println("could not open " + nm);
		    }
		    ByteArrayOutputStream ros = new ByteArrayOutputStream(4096);
		    ris.transferTo(ros);
		    byte[] array = ros.toByteArray();
		    jarprops.put(nm, array);
		    MessageDigest md = SigAdapter.createMD();
		    md.update(array);
		
		    TemplateProcessor.KeyMap rkeymap =
			new TemplateProcessor.KeyMap();
		    rkeymap.put("name", nm);
		    rkeymap.put("md", SigAdapter.bytesToHex(md.digest()));
		    kmlist.addLast(rkeymap);
		}
		keymap.put("jars", kmlist);

		TemplateProcessor tp = new TemplateProcessor(keymap);
		ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
		Writer w = new OutputStreamWriter(baos, UTF8);
					      
		Reader r = new
		    InputStreamReader(DocsigServer.class
				      .getResourceAsStream("intro.tpl"),
				      UTF8);
		tp.processTemplate(r, w);
		w.flush();
		/*
		  InputStream is = DocsigServer.class
		  .getResourceAsStream("intro.html");
		*/
		File target = new File(f, "intro.html");
		OutputStream os = new FileOutputStream(target);
		baos.writeTo(os);
		// is.transferTo(os);
		r.close();
		os.flush();
		os.close();
		ews.add("/", DirWebMap.class,
			new DirWebMap.Config(f, color, bgcolor,
					     linkColor, visitedColor),
			null, true, true, false);

		ews.getWebMap("/").addWelcome("intro.html");

		target = new File(f, "docsig-api.zip");
		InputStream zis = DocsigServer.class
		    .getResourceAsStream("api.zip");
		FileOutputStream zos = new FileOutputStream(target);
		zis.transferTo(zos);
		zos.flush(); zos.close();

		ews.add("/docsig-api/", ZipWebMap.class,
			new ZipWebMap.Config(target, color, bgcolor,
					     linkColor, visitedColor),
			null, true, true, false);
	    
		// See if this fixes a firefox problem with
		// text fields in the 'search' box.
		// WebMap wm = ews.getWebMap("/api/");
		// wmap.addMapping("js", "application/x-javascript");

		// Add BZDEV API documentation. It may be in a directory
		// or a ZIP file.
		File bzdevapi = new File ("/usr/share/doc/libbzdev-doc/api");
		if (bzdevapi.isDirectory()) {
		    ews.add("/bzdev-api/", DirWebMap.class,
			    new DirWebMap.Config(bzdevapi, color, bgcolor,
					     linkColor, visitedColor),
			    null, true, true, false);
		} else {
		    bzdevapi = new File("/usr/share/doc/libbzdev-doc/api.zip");
		    if (bzdevapi.canRead()) {
			ews.add("/bzdev-api/", ZipWebMap.class,
				new ZipWebMap.Config(bzdevapi, color, bgcolor,
						     linkColor, visitedColor),
				null, true, true, false);
		    }
		}
		// wm = ews.getWebMap("/bzdev-api/");
		// wm.addMapping("js", "application/x-javascript");

		for (String name: resourceNames) {
		    String nm = name + ".jar";
		    zis = DocsigServer.class.getResourceAsStream(nm);
		    ByteArrayOutputStream bos = new ByteArrayOutputStream(4096);
		    zis.transferTo(bos);
		    jarprops.put(nm, bos.toByteArray());
		}
		ews.add("/jars/", PropertiesWebMap.class,
			new PropertiesWebMap.Config(jarprops, color, bgcolor,
						    linkColor, visitedColor),
			null, true, true, false);

		WebMap wmap = ews.getWebMap("/");
		wmap.addMapping("pem", "application/x-pem-file");
	    }
	    log.println("trace = " + trace);
	    log.println("stacktrace = " + stacktrace);
	    if (trace) {
		ews.setTracer("/", log, false);
		ews.setTracer("/docsig/", log, stacktrace);
		ews.setTracer("/bzdev-api/", log,  false);
		ews.setTracer("/api/", log, false);
		ews.setTracer("/jars/", log, false);
	    }
	    ews.start();
	    log.println("ews started on port " +ews.getPort()
			+ " running " + (ews.usesHTTPS()? "HTTPS": "HTTP"));
	    if (needHelperStart) {
		// In thius case, the cerfiicate manager does not
		// use the helper and start it, so we'll start the
		// helper manually as it will map http requests
		// to https requests.
		helper.start();
		log.println("helper started directly on port "
			    + helper.getPort());
	    }
	} catch (Exception ex) {
	    log.println("Exception terminating server was thrown:");
	    ex.printStackTrace(log);
	    if (ex.getCause() != null) {
		Throwable t = ex;
		while ((t = t.getCause()) != null) {
		    log.println("----");
		    t.printStackTrace(log);
		}
		log.println("-------------");
		log.flush();
		System.exit(1);
	    }
	    log.flush();
	}
    }
}
