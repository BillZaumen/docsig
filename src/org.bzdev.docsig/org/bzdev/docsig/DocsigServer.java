package org.bzdev.docsig;
import java.io.File;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
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
import org.bzdev.util.TemplateProcessor;
import org.bzdev.util.TemplateProcessor.KeyMap;

public class DocsigServer {

    static final Charset UTF8 = Charset.forName("UTF-8");

    // Extra config names, used to check for misspellings in the
    // configuration file.
    static final Set<String> extraPropNames =
	Set.of("buttonFGColor", "buttonBGColor", "bquoteBGColor");

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
	String buttonFGColor = "white";
	String buttonBGColor = "rgb(10,10,64)";
	String bquoteBGColor = "rgb(32,32,32)";

	File cdir = new File(System.getProperty("user.dir"));
	File uadir = new File("/usr/app");
	File logFile = (cdir.equals(uadir))? new File(uadir, "docsig.log"):
	    null;
	ConfigurableWS.setTraceDefaults(defaultTrace, defaultStacktrace);

	Properties props = null;
	ConfigurableWS server = null;
	PrintStream log = null;

	if (argv.length > 1 + offset) {
	    File configFile = new File(argv[offset+1]);
	    File dir = configFile.getParentFile();

	    if (!configFile.exists()) {
		log.println("Creating " + configFile);
		log.flush();
		File template = new File("/etc/docsig/docsig.config");
		if (template.isFile() && template.canRead()) {
		    Path src = template.toPath();
		    Path dst = configFile.toPath();
		    if (dir != null) {
			dir.mkdirs();
		    }
		    Files.copy(src,dst);
		}
	    }

	    if (dir == null) dir = new File(System.getProperty("user.dir"));

	    if (configFile.canRead()) {
		server = new ConfigurableWS(extraPropNames,
					    configFile,
					    logFile);
		props = server.getProperties();
		log = server.getLog();

		buttonFGColor = props.getProperty("buttonFGColor",
						  buttonFGColor);
		buttonBGColor = props.getProperty("buttonBGColor",
						  buttonBGColor);
		bquoteBGColor = props.getProperty("bquoteBGColor",
						  bquoteBGColor);

		log.println("buttonFGColor = " + buttonFGColor);
		log.println("buttonBGColor = " + buttonBGColor);
		log.println("bquoteBGColor = " + bquoteBGColor);

	    } else {
		log = (logFile != null)?
		    new PrintStream(new FileOutputStream(logFile), true, UTF8):
		    System.out;
		log.println("cannot read config file " + configFile);
		log.flush();
		log.close();
		System.exit(1);
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
	    EmbeddedWebServer ews = server.getServer();

	    String color = server.color();
	    String bgcolor = server.bgcolor();
	    String linkColor = server.linkColor();
	    String visitedColor = server.visitedColor();

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
	    boolean trace = server.trace();
	    boolean stacktrace = server.stacktrace();
	    /*

	    log.println("trace = " + trace);
	    log.println("stacktrace = " + stacktrace);
	    */
	    if (trace) {
		ews.setTracer("/", log, false);
		ews.setTracer("/docsig/", log, stacktrace);
		ews.setTracer("/bzdev-api/", log,  false);
		ews.setTracer("/api/", log, false);
		ews.setTracer("/jars/", log, false);
	    }
	    server.start();
	    /*
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
	    */
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
