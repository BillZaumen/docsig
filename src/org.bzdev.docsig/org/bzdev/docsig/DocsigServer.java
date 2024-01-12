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
import org.bzdev.ejws.*;
import org.bzdev.ejws.maps.*;
import org.bzdev.net.HttpMethod;
import org.bzdev.net.SSLUtilities;
import org.bzdev.util.TemplateProcessor;
import org.bzdev.util.TemplateProcessor.KeyMap;

public class DocsigServer {

    static final Charset UTF8 = Charset.forName("UTF-8");

    public static void main(String argv[]) throws Exception {
	if (argv.length == 0)
	    throw new IllegalStateException("no arguments");

        System.setProperty("java.awt.headless", "true");
	/*

        String s = System.getenv("PORT");
        int port = (s == null)? 80: Integer.parseInt(s);
        s = System.getenv("BACKLOG");
        int backlog = (s == null)? 30: Integer.parseInt(s);
        s = System.getenv("NTHREADS");
        int nthreads = (s == null)? 50: Integer.parseInt(s);
        s = System.getenv("TRACE");
        boolean trace = (s == null)? false: Boolean.parseBoolean(s);
        s = System.getProperty("IPADDR");
        InetAddress addr = (s == null || s.equals("wildcard"))? null:
            s.equalsIgnoreCase("loopback")? InetAddress.getLoopbackAddress():
            InetAddress.getByName(s);
	*/

	// CSS colors.
	String color = "white";
	String bgcolor = "rgb(10,10,25)";
	String linkColor = "rgb(65,225,128)";
	String visitedColor = "rgb(65,164,128)";
	String buttonFGColor = "white";
	String buttonBGColor = "rgb(10,10,64)";
	String bquoteBGColor = "rgb(32,32,32)";

	int port = 80;
	int backlog = 30;
	int nthreads = 50;
	boolean trace = false;
	InetAddress addr = null;

	EmbeddedWebServer.SSLSetup sslSetup = null;
	String sslType = null;
	File keyStoreFile = null;
	char[] keyStorePW = null;
	File trustStoreFile = null;
	char[] trustStorePW = null;
	boolean loopback = false;
	boolean selfsigned = false;

	if (argv.length > 1) {
	    File propFile = new File(argv[1]);
	    File dir = propFile.getParentFile();
	    if (dir == null) dir = new File(System.getProperty("user.dir"));
	    if (propFile.canRead()) {
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

		String s = props.getProperty("ipaddr");
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

		s = props.getProperty("keyStoreFile");
		if (s != null) {
		    Path path = Path.of(s);
		    if (path.isAbsolute()) {
			keyStoreFile = path.toFile();
		    } else {
			keyStoreFile = dir.toPath().resolve(path).toFile();
		    }
		    if (!keyStoreFile.canRead()) keyStoreFile = null;
		}
		s = props.getProperty("trustStoreFile");
		if (s != null) {
		    Path path = Path.of(s);
		    if (path.isAbsolute()) {
			trustStoreFile = path.toFile();
		    } else {
			trustStoreFile = dir.toPath().resolve(path).toFile();
		    }
		    if (!trustStoreFile.canRead()) trustStoreFile = null;
		}
		
		s = props.getProperty("sslType");
		if (s == null || !(s.trim().length() == 0)
		    || s.trim().equals("null")) {
		    keyStoreFile = null;
		} else {
		    sslType = s.trim().toUpperCase();
		}
		s = props.getProperty("port");
		if (s == null) {
		    port = (sslType == null)? 80: 443;
		} else {
		    port = Integer.parseInt(s);
		}
		s = props.getProperty("allowLoopback");
		if (s != null && s.trim().equalsIgnoreCase("true")) {
		    loopback = true;
		}
		s = props.getProperty("allowSelfSigned");
		if (s != null && s.trim().equalsIgnoreCase("true")) {
		    selfsigned = true;
		}

		s = props.getProperty("keyStorePassword");
		keyStorePW = (s== null || keyStoreFile == null)? null:
		    s.toCharArray();

		s = props.getProperty("trustStorePassword");
		trustStorePW = (s== null || trustStoreFile == null)? null:
		    s.toCharArray();
	    }
	}

	EmbeddedWebServer.SSLSetup sslsetup = null;
	if (sslType != null) {
	    if (loopback) {
		SSLUtilities.allowLoopbackHostname();
	    }
	    if (selfsigned) {
		SSLUtilities.installTrustManager(sslType,
						 trustStoreFile, trustStorePW,
						 (cert) -> {return true;});
	    } else if (trustStoreFile != null) {
		SSLUtilities.installTrustManager(sslType,
						 trustStoreFile, trustStorePW,
						 (cert) -> {return false;});
	    }
	    sslSetup = new EmbeddedWebServer.SSLSetup(sslType);
	    if (keyStoreFile != null && keyStorePW != null) {
		sslsetup.keystore(new FileInputStream(keyStoreFile))
		    .keystorePassword(keyStorePW);
	    }
	    /*
	     * We do not need a trust store because we don't requesat
	     * client authentication.
	    if (trustStoreFile != null && trustStorePW != null) {
		sslsetup.truststore(new FileInputStream(trustStoreFile))
		    .truststorePassword(trustStorePW);
	    }
	    */
	}
	keyStorePW = null;
	trustStorePW = null;

        EmbeddedWebServer ews = new
            EmbeddedWebServer(addr, port, backlog, nthreads, sslSetup);

	// ews.setRootColors(color, bgcolor, linkColor, visitedColor);

	Map<String,String> parameters = new HashMap<>();

	if (argv.length > 0) {
	    String fs = System.getProperty("file.separator");
	    fs = argv[0].endsWith(fs)? "": fs;
	    parameters.put("publicKeyDir", argv[0] + fs + "PublicKeys");
	}

	parameters.put("color", color);
	parameters.put("bgcolor", bgcolor);
	parameters.put("linkColor", linkColor);
	parameters.put("visitedColor", visitedColor);
	parameters.put("buttonFGColor", buttonFGColor);
	parameters.put("buttonBGColor", buttonBGColor);
	parameters.put("bquoteBGColor", bquoteBGColor);

	ews.add("/docsig", ServletWebMap.class,
		new ServletWebMap.Config(new SigAdapter(),
					 parameters, true,
					 HttpMethod.GET,
					 HttpMethod.POST,
					 HttpMethod.HEAD,
					 HttpMethod.OPTIONS,
					 HttpMethod.TRACE),
		null, true, false, true);

	if (argv.length > 0) {
	    File f = new File(argv[0]);
	    f.mkdirs();

	    TemplateProcessor.KeyMap keymap = new TemplateProcessor.KeyMap();
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
		    System.out.println("could not open " + nm);
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

	    target = new File(f, "api.zip");
	    InputStream zis = DocsigServer.class
		.getResourceAsStream("api.zip");
	    FileOutputStream zos = new FileOutputStream(target);
	    zis.transferTo(zos);
	    zos.flush(); zos.close();

	    ews.add("/api/", ZipWebMap.class,
		    target, null, true, true, false);
	    
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

        if (trace) {
            ews.setTracer("/", System.out, true);
            ews.setTracer("/docsig/", System.out, true);
        }
        ews.start();
    }
}
