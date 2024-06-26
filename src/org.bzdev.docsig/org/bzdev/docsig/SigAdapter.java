package org.bzdev.docsig;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.security.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.CRC32;

import org.bzdev.io.AppendableWriter;
import org.bzdev.net.*;
import org.bzdev.net.ServletAdapter.ServletException;
import org.bzdev.util.TemplateProcessor;
import org.bzdev.util.TemplateProcessor.KeyMap;

public class SigAdapter implements ServletAdapter {
    static final Charset UTF8 = Charset.forName("UTF-8");
    static String[] pemPair = SecureBasicUtilities.createPEMPair();
    static String publicKey = pemPair[1];
    
    static private SecureBasicUtilities createSBU(String pem) {
	try {
	    return new SecureBasicUtilities(pem);
	} catch (Exception e) {
	    return null;
	}
    }
    static  MessageDigest createMD() {
	try {
	    return MessageDigest.getInstance("SHA-256");
	} catch (Exception e) {
	    return null;
	}
    }

    static SecureBasicUtilities sbuPrivate = createSBU(pemPair[0]);
    static SecureBasicUtilities sbuPublic = createSBU(pemPair[1]);

    static String pd;
    static {
	MessageDigest md = createMD();
	md.update(publicKey.getBytes(UTF8));
	pd = bytesToHex(md.digest());
    }
    static final String pemDigest = pd;

    static final String CRLF = "\r\n";

    // When running in a container, localhost may refer to the
    // contain's localhost, not the system localhost, so we have
    // to modify it.
    static final String DOCSIG_LOCALHOST = System.getenv("DOCSIG_LOCALHOST");

    private static String getDocsigLocalAddr() {
	if (DOCSIG_LOCALHOST == null) return null;
	String found = null;
	try {
	    for(InetAddress addr: InetAddress.getAllByName(DOCSIG_LOCALHOST)) {
		if (!addr.isLoopbackAddress()) {
		    String name = addr.getCanonicalHostName();
		    if (name == null) {
			// add brackets because that is wht we need for HTTP
			// and HTTPS.
			if (addr instanceof Inet6Address) {
			    name =  addr.getHostAddress();
			    if (name.length() == 39) {
				name =  "[" + name + "]";
			    } else {
				name = null;
			    }
			} else {
			    name =  addr.getHostAddress();
			}
		    }
		    if (name != null) return name;
		}
	    }
	} catch (Exception e) {}
	return null;
    }


    static final String DOCSIG_LOCAL_ADDR = getDocsigLocalAddr();


    static String bytesToHex(byte[] bytes) {
	final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9',
				 'a','b','c','d','e','f'};
	char[] hexChars = new char[bytes.length * 2];
	int v;
	for ( int j = 0; j < bytes.length; j++ ) {
	    v = bytes[j] & 0xFF;
	    hexChars[j * 2] = hexArray[v >>> 4];
	    hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	}
	return new String(hexChars);
    }

    static class Entry {
	String url;
	String mediaType;
	byte[] contents;
    }
    
    private int MAX_ENTRIES = 32;
    void setMAX_ENTRIES(int max) {
	MAX_ENTRIES = max;
    }

    private Map<String,Entry> cache = Collections
	.synchronizedMap(new LinkedHashMap<String,Entry>(MAX_ENTRIES) {
		protected boolean removeEldestEntry(Map.Entry entry) {
		    return size() > MAX_ENTRIES;
		}
	    });

    String color = null;
    String bgcolor = null;
    String linkColor = null;
    String visitedColor = null;
    String buttonFGColor = null;
    String buttonBGColor = null;
    String bquoteBGColor = null;
    File publicKeyDir = null;
    File logFile = null;
    String timezone = null;
    ZoneId zoneID = null;
    URL emailTemplateURL = null;

    static File defaultLogFile = null;

    /**
     * Set the log file.
     */
    public static void setDefaultLogFile(File logFile) {
	defaultLogFile = logFile;
    }

    static File defaultPublicKeyDir = null;

    /**
     * Set the default public key directory.
     */
    public static void setDefaultPublicKeyDir(File dir) throws IOException {
	if (dir != null) {
	    dir.mkdirs();
	    defaultPublicKeyDir = dir;
	    if (!(dir.isDirectory() && dir.canRead() && dir.canWrite())) {
		throw new IOException("File " + dir
				      + " not a readable"
				      + " and writable directory");
	    }
	} else {
	    throw new NullPointerException("argument is null, not a directory");
	}
    }

    @Override
    public void init(Map<String,String>parameters)
	throws ServletAdapter.ServletException
    {
	if (parameters == null) {
	    throw new ServletAdapter.ServletException("missing parameters");
	}

	if (DOCSIG_LOCALHOST != null && DOCSIG_LOCAL_ADDR == null) {
	    throw new ServletAdapter.ServletException
		("DOCSIG_LOCALHOST = " + DOCSIG_LOCALHOST
		 + " but no suitable IP address found");
	}
	// System.out.println("DOCSIG_LOCAL_ADDR = " + DOCSIG_LOCAL_ADDR);

	color = parameters.get("color");
	bgcolor = parameters.get("bgcolor");
	linkColor = parameters.get("linkColor");
	visitedColor = parameters.get("visitedColor");
	buttonFGColor = parameters.get("buttonFGColor");
	buttonBGColor = parameters.get("buttonBGColor");
	bquoteBGColor = parameters.get("bquoteBGColor");

	if (color == null || color.trim().length() == 0) {
	    color = "white";
	}
	if (bgcolor == null || bgcolor.trim().length() == 0) {
	    bgcolor = "rgb(10,10,25)";
	}
	if (linkColor == null || linkColor.trim().length() == 0) {
	    linkColor = "rgb(65,225,128)";
	}
	if (visitedColor == null || visitedColor.trim().length() == 0) {
	    visitedColor = "rgb(65,164,128)";
	}
	if (buttonFGColor == null || buttonFGColor.trim().length() == 0) {
	    buttonFGColor = "white";
	}
	if (buttonBGColor == null || buttonBGColor.trim().length() == 0) {
	    buttonBGColor = "rgb(10,10,64)";
	}
	if (bquoteBGColor == null || bquoteBGColor.trim().length() == 0) {
	    bquoteBGColor = "rgv(32,32,32)";
	}

	timezone = parameters.get("timezone");
	if (timezone != null) {
	    // DocsigServer already checked that the time zone is a
	    // one recognized by our JRE.
	    zoneID = ZoneId.of(timezone);
	} else {
	    timezone = ZoneId.systemDefault().getId();
	}

	String emailurl = parameters.get("emailTemplateURL");
	try {
	    emailTemplateURL = (emailurl == null)? null:
		new URL(emailurl);
	} catch (MalformedURLException eurl) {
	    String msg = "Malformed URL: " + emailurl;
	    throw new ServletAdapter.ServletException(msg, eurl);
	}

	String logFileName = parameters.get("logFile");
	logFile = (logFileName != null)? new File(logFileName): defaultLogFile;

	String keydir = parameters.get("publicKeyDir");
	if (keydir != null) {
	    publicKeyDir = new File(keydir);
	    publicKeyDir.mkdirs();
	} else {
	    publicKeyDir = defaultPublicKeyDir;
	}
	if (publicKeyDir != null) {
	    byte[]contents = publicKey.getBytes(UTF8);
	    MessageDigest md = createMD();
	    md.update(contents);
	    String fname = bytesToHex(md.digest()) + ".pem";
	    try {
		File keyf = new File(publicKeyDir, fname);
		if (!keyf.exists()) {
		    OutputStream os = new FileOutputStream(keyf);
		    os.write(contents);
		    os.flush();
		    os.close();
		}
	    } catch (Exception e) {
		throw new ServletAdapter.ServletException
		    ("Cannot create PEM file", e);
	    }
	} else {
		throw new ServletAdapter.ServletException
		    ("No public-key directory");
	}
	String nthreads = parameters.get("nthreads");
	if (nthreads != null) {
	    try {
		int nt = Integer.parseInt(nthreads);
		if (nt <= 0) {
		    throw new Exception("Not positive");
		}
		setMAX_CACHED(nt);
	    } catch (Exception e) {
		    throw new ServletAdapter.ServletException
			("Illegal number of threads: " + nthreads, e);
	    }
	}
	String ndocs = parameters.get("numberOfDocuments");
	if (ndocs != null) {
	    try {
		int nd = Integer.parseInt(ndocs);
		if (nd <= 0) {
		    throw new Exception("Not positive");
		}
		setMAX_ENTRIES(nd);
	    } catch (Exception e) {
		    throw new ServletAdapter.ServletException
			("Illegal number of documents: " + ndocs, e);
	    }
	    
	}
	
    }

    static void sendSimpleResponse(HttpServerResponse res, int code,
				   String msg)
	throws IOException, ServletAdapter.ServletException
    {
	byte[] bytes = msg.getBytes(UTF8);
	res.setHeader("content-type", "text/plain; charset=UTF-8");
	res.sendResponseHeaders(code, bytes.length);
	OutputStream os = res.getOutputStream();
	os.write(bytes);
	os.flush();
	os.close();
    }

    @Override
    public void doGet(HttpServerRequest req, HttpServerResponse res)
	throws IOException, ServletAdapter.ServletException
    {
	if (req.getParameter("sendto") != null) {
	    // Generally one should use a POST method to create the
	    // respose document because the response includes a timestamp.
	    // DOCSERVER allows a GET method as well so a server can
	    // generate an HTTP redirect so that a user does not have
	    // to submit a form.
	    res.setHeader("cache-control", "no-cache");
	    doPost(req, res);
	    return;
	}

	String hasKey = req.getParameter("hasKeyRequest");
	String docurl = req.getParameter("url");
	String digest = req.getParameter("digest");
	String pemreq = req.getParameter("publicKeyRequest");
	String getreq = req.getParameter("getPublicKeys");
	String getlog = req.getParameter("getLog");
	docurl = (docurl == null)? null: docurl.trim();
	digest = (digest == null)? null: digest.trim().toLowerCase();
	pemreq = (pemreq == null)? null: pemreq.trim();
	getreq = (getreq == null)? null: getreq.trim().toLowerCase();
	getlog = (getlog == null)? null: getlog.trim().toLowerCase();
	if (getreq != null && !getreq.equals("true")) getreq = null;
	if (getlog != null && !getlog.equals("true")) getlog = null;

	if (docurl == null || digest == null) {
	    if (getlog != null) {
		if (hasKey != null || docurl != null || digest != null
		    || pemreq != null || getreq != null) {
		    sendSimpleResponse(res, 400,
				       "conflicting query fields\r\n");
		    return;
		} else if (logFile == null) {
		    sendSimpleResponse(res, 404, "no log file\r\n");
		    return;
		}
		FileReader r = new FileReader(logFile, UTF8);
		StringWriter w = new StringWriter(4096);
		r.transferTo(w);
		res.setHeader("cache-control", "no-cache");
		sendSimpleResponse(res, 200, w.toString());
		return;
	    } else if (getreq != null) {
		if (hasKey != null || docurl != null || digest != null
		    || pemreq != null) {
		    sendSimpleResponse(res, 400,
				       "conflicting query-string fields\r\n");
		    return;
		} else {
		    // Get a zip file for the public key files
		    if (publicKeyDir == null) {
			sendSimpleResponse(res, 404,
					   "no public key directory\r\n");
			return;
		    }
		    ByteArrayOutputStream bos = new ByteArrayOutputStream(4096);
		    GZIPOutputStream gzos = new GZIPOutputStream(bos);
		    ZipOutputStream zos = new ZipOutputStream(gzos, UTF8);
		    zos.setMethod(ZipOutputStream.STORED);
		    for (File f: publicKeyDir.listFiles()) {
			FileInputStream in = new FileInputStream(f);
			ByteArrayOutputStream out =
			    new ByteArrayOutputStream(1024);
			in.transferTo(out);
			out.close();
			ZipEntry ze = new ZipEntry(f.getName());
			CRC32 crc = new CRC32();
			byte[] bytes = out.toByteArray();
			crc.update(bytes);
			ze.setSize(bytes.length);
			ze.setCompressedSize(bytes.length);
			ze.setCrc(crc.getValue());
			zos.putNextEntry(ze);
			zos.write(bytes, 0, bytes.length);
			zos.flush();
		    }
		    zos.close();
		    gzos.close();
		    int len = bos.size();
		    res.setHeader("content-encoding", "gzip");
		    res.setHeader("content-type", "application/zip");
		    res.sendResponseHeaders(200, bos.size());
		    OutputStream os = res.getOutputStream();
		    bos.writeTo(os);
		    return;
		}
	    } else if (hasKey == null && pemreq == null) {
		sendSimpleResponse(res, 400, "missing query-string fields\r\n");
		return;
	    } else if (hasKey != null && pemreq != null) {
		sendSimpleResponse(res, 400,
				   "conflicting query-string fields\r\n");
		return;
		
	    } else if (hasKey != null) {
		if (publicKeyDir == null) {
		    sendSimpleResponse(res, 501, "no public key directory\r\n");
		    return;
		} else {
		    File testf = new File(publicKeyDir, hasKey + ".pem");
		    boolean status = false;
		    for (File f: publicKeyDir.listFiles()) {
			if (testf.equals(f)) {
			    status = true;
			    break;
			}
		    }
		    if (status) {
			res.sendResponseHeaders(204, -1);
			return;
		    } else {
			res.sendResponseHeaders(404, -1);
			return;
		    }
		}
	    } else {
		byte[] results = publicKey.getBytes(UTF8);
		res.setHeader("content-type", "application/x-pem-file");
		res.sendResponseHeaders(200, results.length);
		OutputStream os = res.getOutputStream();
		os.write(results);
		os.flush();
		os.close();
		return;
	    }
	} else if (getlog != null || getreq != null || pemreq != null
		   || hasKey != null) {
	    sendSimpleResponse(res, 400, "conflicting query-string fields\r\n");
	    return;
	}
	Entry entry = cache.get(digest);
	if (entry != null) {
	    if (entry.url.equals(docurl)) {
		res.setHeader("content-type", entry.mediaType);
		res.sendResponseHeaders(200, entry.contents.length);
		OutputStream os = res.getOutputStream();
		os.write(entry.contents);
		os.flush();
		os.close();
		cache.put(digest, entry);
		return;
	    } else {
		URL url = new URL(docurl);
		String host = url.getHost();
		if (DOCSIG_LOCALHOST != null && host.equals("localhost")) {
		    url = new URL(url.getProtocol(),
				  DOCSIG_LOCAL_ADDR,
				  url.getPort(),
				  url.getFile());
		}
		URLConnection urlc = url.openConnection();
		if (urlc instanceof HttpURLConnection) {
		    HttpURLConnection hurlc = (HttpURLConnection) urlc;
		    InputStream is = null;
		    try {
			hurlc.connect();
			int rcode = hurlc.getResponseCode();
			is  = hurlc.getInputStream();
			if (rcode != 200) {
			    OutputStream nos = OutputStream.nullOutputStream();
			    is.transferTo(nos);
			    sendSimpleResponse(res, rcode,
					       "Could not load \r\n"
					       + docurl + "\r\n"
					       + " --- status was " + rcode
					       + "\r\n");
			    // res.sendError(rcode);
			    return;
			}
		    } catch (IOException eio) {
			if (DOCSIG_LOCALHOST != null
			    && host.equals("localhost")) {
			    sendSimpleResponse(res, 404,
					      "could not load " + docurl
					      +"\r\n (localhost -> "
					      + DOCSIG_LOCALHOST + ")"
					      + "\r\n error [1]: "
					      + eio.getMessage());
			} else {
			    sendSimpleResponse(res, 404,
					       "could not load " + docurl
					       +"\r\n error [1]: "
					       + eio.getMessage());
			}
			return;
		    }
		    String mediaType = hurlc.getContentType();
		    ByteArrayOutputStream baos = new
			ByteArrayOutputStream(8192);
		    is.transferTo(baos);
		    byte[] document = baos.toByteArray();
		    MessageDigest md = createMD();
		    md.update(document);
		    String ndigest = bytesToHex(md.digest());
		    if (digest.equals(ndigest)) {
			res.setHeader("content-type", entry.mediaType);
			res.sendResponseHeaders(200, entry.contents.length);
			OutputStream os = res.getOutputStream();
			os.write(entry.contents);
			os.flush();
			os.close();
			cache.put(digest, entry);
			return;
		    } else {
			// res.sendError(404);
			sendSimpleResponse(res, 404,
					       "Could not find document\r\n"
					       + docurl + "\r\n"
					       +"with SHA-256 digest"
					       + digest + "\r\n");
			return;
		    }
		} else {
		    throw new ServletAdapter.ServletException("not HTTP(S)");
		}
	    }
	} else {
	    URL url = new URL(docurl);
	    String host = url.getHost();
	    if (DOCSIG_LOCALHOST != null && host.equals("localhost")) {
		url = new URL(url.getProtocol(),
			      DOCSIG_LOCAL_ADDR,
			      url.getPort(),
			      url.getFile());
	    }

	    URLConnection urlc = url.openConnection();
	    if (urlc instanceof HttpURLConnection) {
		HttpURLConnection hurlc = (HttpURLConnection) urlc;
		InputStream is = null;
		try {
		    hurlc.connect();
		    int rcode = hurlc.getResponseCode();
		    is  = hurlc.getInputStream();
		    if (rcode != 200) {
			OutputStream nos = OutputStream.nullOutputStream();
			is.transferTo(nos);
			sendSimpleResponse(res, rcode,
					   "Document server did not"
					   + " recognize\r\n"
					   + docurl);
			// res.sendError(rcode);
			return;
		    }
		} catch (IOException eio) {
		    if (DOCSIG_LOCALHOST != null
			&& host.equals("localhost")) {
			sendSimpleResponse(res, 404,
					  "could not load " + docurl
					  +"\r\n (localhost -> "
					  + DOCSIG_LOCALHOST + ")"
					  + "\r\n error [2]: "
					  + eio.getMessage());
		    } else {
			sendSimpleResponse(res, 404,
					   "could not load " + docurl
					   +"\r\n error [2]: "
					   + eio.getMessage());
		    }
		    return;
		}
		String mediaType = hurlc.getContentType();
		ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
		is.transferTo(baos);
		byte[] document = baos.toByteArray();
		MessageDigest md = createMD();
		md.update(document);
		String ndigest = bytesToHex(md.digest());
		entry = new Entry();
		entry.url = docurl;
		entry.mediaType = mediaType;
		entry.contents = document;
		if (ndigest.equals(digest)) {
		    res.setHeader("content-type", entry.mediaType);
		    res.sendResponseHeaders(200, entry.contents.length);
		    OutputStream os = res.getOutputStream();
		    os.write(entry.contents);
		    os.flush();
		    os.close();
		    cache.put(digest, entry);
		    return;
		} else {
		    res.sendError(404);
		    return;
		}
	    } else {
		throw new ServletAdapter.ServletException("not HTTP(S)");
	    }
	}
    }

    private int MAX_CACHED = 50; // default number of threads
    void setMAX_CACHED (int max) {
	MAX_CACHED = max;
    }

    private  Map<URL,String> ucache = Collections
	.synchronizedMap(new LinkedHashMap<URL,String>() {
		protected boolean removeEldestEntry(Map.Entry entry) {
		    return size() > MAX_CACHED;
		}
	    });

    private Reader urlReader(URL url, boolean pem)
	throws ServletAdapter.ServletException
    {
	try {
	    URLConnection urlc = url.openConnection();
	    InputStream is = urlc.getInputStream();
	    ByteArrayOutputStream boas = new ByteArrayOutputStream(4096);
	    is.transferTo(boas);
	    if (pem) {
		String pemString="\n$(PEM)\n";
		boas.writeBytes(pemString.getBytes(UTF8));
	    }
	    byte[] bytes = boas.toByteArray();
	    MessageDigest md = createMD();
	    md.update(bytes);
	    String digest = bytesToHex(md.digest());
	    is = new ByteArrayInputStream(bytes);
	    ucache.put(url, digest);
	    return new InputStreamReader(is, UTF8);
	} catch (IOException e) {
	    String msg = "Cannot read the content of URL " + url.toString();
	    throw new ServletAdapter.ServletException(msg, e);
	}
    }

    static Set<String> reservedParms = Set
	.of("name", "email", "id", "transID", "sigserver", "type",
	    "document", "sendto", "cc", "subject");

    static Set<String> reservedHdrs =
	Set.of("acceptedBy", "timestamp", "date", "timezone", "ipaddr",
	       "id", "transID", "email", "server", "sendto", "cc",
	       "document", "type", "digest",
	       "emailTemplateURL", "emailTemplateDigest", "publicKeyID",
	       "signature");

    static Comparator<String> nameComparator = new Comparator<String>() {
	    public int compare(String s1, String s2) {
		if (s1 == null) s1 = "";
		if (s2 == null) s2 = "";
		return s1.compareToIgnoreCase(s2);
	    }
	};


    @Override
    public void doPost(HttpServerRequest req, HttpServerResponse res)
	throws IOException, ServletAdapter.ServletException
    {
	String name = req.getParameter("name");
	if (name == null) throw new ServletException
			      ("parameter 'name' missing");
	name = name.strip().replaceAll("\\s+"," ");
	String email = req.getParameter("email");
	if (email == null)
	    throw new ServletException("parameter 'email' missing");
	email = email.strip().replaceAll("\\s","");
	String id = req.getParameter("id");
	if (id != null) {
	    id = id.strip().replaceAll("\\s","");
	}
	String transID  =req.getParameter("transID");
	if (transID != null) {
	    transID = transID.strip().replaceAll("\\s","");
	}
	String sigserver = req.getParameter("sigserver");
	if (sigserver == null)
	    throw new ServletException("parameter 'sigserver' is missing");
	sigserver = sigserver.strip().replaceAll("\\s","");
	if (!sigserver.endsWith("/")) {
	    // This implementation requires a "/" before the query string.
	    sigserver = sigserver + "/";
	}
	String type = req.getParameter("type");
	type = type.strip().replaceAll("\\s","");
	String document = req.getParameter("document");
	if (document != null) {
	    if (type == null)
		throw new ServletException("parameter 'type' is missing");
	    document = document.strip().replaceAll("\\s","");
	}
	String sendto = req.getParameter("sendto");
	if (sendto == null) {
	    throw new ServletException("parameter 'sendto' is missing");
	}
	String cc = req.getParameter("cc");
	if (cc != null) {
	    cc = cc.strip().replaceAll("\\s","");
	}
	sendto = sendto.strip().replaceAll("\\s","");
	String subject = req.getParameter("subject");
	if (subject == null)
	    throw new ServletAdapter
		.ServletException("parameter 'subject' is missing");
	subject = subject.strip().replaceAll("\\s+"," ");
	String digest = null;
	String timestamp = Instant.now().toString();
	String date = (zoneID == null? LocalDate.now(): LocalDate.now(zoneID))
	    .toString();
	String ipaddr = req.getRemoteAddr();


	if (name == null || email == null || sigserver == null
	    || sendto == null || subject == null) {
	    throw new ServletException("missing post data");
	}

	ByteArrayOutputStream baos;
	String mediaType = null;
	URL url = (document == null)? null: new URL(document);
	if (url != null) {
	    String host = url.getHost();
	    if (DOCSIG_LOCALHOST != null && host.equals("localhost")) {
		url = new URL(url.getProtocol(),
			      DOCSIG_LOCAL_ADDR,
			      url.getPort(),
			      url.getFile());
	    }
	    URLConnection urlc = url.openConnection();
	    if (urlc instanceof HttpURLConnection) {
		HttpURLConnection hurlc = (HttpURLConnection) urlc;
		InputStream is = null;
		try {
		    hurlc.connect();
		    is  = hurlc.getInputStream();
		    int rcode = hurlc.getResponseCode();
		    if (rcode != 200) {
			OutputStream nos = OutputStream.nullOutputStream();
			is.transferTo(nos);
			res.sendError(rcode);
			return;
		    }
		} catch (IOException eio) {
		    if (DOCSIG_LOCALHOST != null && host.equals("localhost")) {
			sendSimpleResponse(res, 404,
					   "could not load " + document
					   + "\r\n (localhost -> "
					   + DOCSIG_LOCALHOST + ")"
					   + "\r\n error [3]: "
					   + eio.getMessage());
		    } else {
			sendSimpleResponse(res, 404,
					   "could not load " + document
					   +"\r\n error [3]: "
					   + eio.getMessage());
		    }
		    return;
		}
		if (document != null) {
		    mediaType = hurlc.getContentType();
		    baos = new ByteArrayOutputStream(8192);
		    is.transferTo(baos);
		    byte[] contents = baos.toByteArray();
		    MessageDigest md = createMD();
		    md.update(contents);
		    digest = bytesToHex(md.digest());
		    Entry entry = new Entry();
		    entry.url = document;
		    entry.mediaType = mediaType;
		    entry.contents = contents;
		    cache.put(digest, entry);
		}
	    } else {
		res.sendError(404);
	    }
	}
	Reader r = (emailTemplateURL == null)?
	    new InputStreamReader(getClass()
				  .getResourceAsStream("email.tpl"),
				  UTF8):
	    urlReader(emailTemplateURL, true);
	String udigest = (emailTemplateURL == null)? null:
		ucache.get(emailTemplateURL);
	StringBuilder sb = new StringBuilder(1024);
	sb.append("acceptedBy: "); sb.append(name); sb.append(CRLF);
	sb.append("timestamp: "); sb.append(timestamp); sb.append(CRLF);
	sb.append("date: "); sb.append(date); sb.append(CRLF);
	sb.append("timezone: "); sb.append(timezone); sb.append(CRLF);
	sb.append("ipaddr: "); sb.append(ipaddr); sb.append(CRLF);
	if (id != null) {
	    sb.append("id: "); sb.append(id); sb.append(CRLF);
	}
	if (transID != null) {
	    sb.append("transID: "); sb.append(transID); sb.append(CRLF);
	}
	sb.append("email: "); sb.append(email); sb.append(CRLF);
	sb.append("server: "); sb.append(sigserver); sb.append(CRLF);
	sb.append("sendto: "); sb.append(sendto); sb.append(CRLF);
	if (cc != null) {
	    sb.append("cc: "); sb.append(cc); sb.append(CRLF);
	}
	if (document != null) {
	    sb.append("document: "); sb.append(document);sb.append(CRLF);
	}
	sb.append("type: "); sb.append(type); sb.append(CRLF);
	if (digest != null) {
	    sb.append("digest: "); sb.append(digest); sb.append(CRLF);
	}
	if (emailTemplateURL != null) {
	    sb.append("emailTemplateURL: ");
	    sb.append(emailTemplateURL.toString());
	    sb.append(CRLF);
	    sb.append("emailTemplateDigest: " + udigest);
	    sb.append(CRLF);
	}
	sb.append("publicKeyID: "); sb.append(pemDigest); sb.append(CRLF);
	TreeSet<String> nameSet = new TreeSet<>(nameComparator);
	for (Enumeration<String> names = req.getParameterNames();
	     names.hasMoreElements();) {
	    String nm = names.nextElement();
	    if (reservedParms.contains(nm)) continue;
	    if (reservedHdrs.contains(nm)) continue;
	    nameSet.add(nm);
	    // System.out.println("... adding " +nm + " to nameSet");
	}
	for (String key: nameSet) {
	    sb.append(key + ": " + req.getParameter(key).trim());
	    sb.append(CRLF);
	}

	/*
	  String signing[] = {
	  name,
	  timestamp,
	  date,
	  timezone,
	  ipaddr,
	  ((id == null)? "<null>": id),
	  ((transID == null)? "<null>": transID),
	  email,
	  sigserver,
	  sendto,
	  ((cc == null)? "<null>": cc),
	  document,
	  type,
	  digest,
	  ((emailTemplateURL == null)? "<null>":
	  emailTemplateURL.toString()),
	  ((emailTemplateURL == null)? "<null>":
	  udigest),
	  pemDigest
	  };
	  System.out.println("signed ...");
	  for (String sstr: signing) {
	  System.out.println("... " + sstr);
	  }
	*/
	try {
	    Signature signer = sbuPrivate.getSigner();
	    signer.update((name+CRLF).getBytes(UTF8));
	    signer.update((timestamp + CRLF).getBytes(UTF8));
	    signer.update((date + CRLF).getBytes(UTF8));
	    signer.update((timezone + CRLF).getBytes(UTF8));
	    signer.update((ipaddr + CRLF).getBytes(UTF8));
	    if (id != null) {
		signer.update((id+CRLF).getBytes(UTF8));
	    }
	    if (transID != null) {
		signer.update((transID+CRLF).getBytes(UTF8));
	    }
	    signer.update((email+CRLF).getBytes(UTF8));
	    signer.update((sigserver+CRLF).getBytes(UTF8));
	    signer.update((sendto+CRLF).getBytes(UTF8));
	    if (cc != null) {
		signer.update((cc+CRLF).getBytes(UTF8));
	    }
	    if (document != null) {
		signer.update((document+CRLF).getBytes(UTF8));
	    }
	    signer.update((type + CRLF).getBytes(UTF8));
	    if (digest != null) {
		signer.update((digest+CRLF).getBytes(UTF8));
	    }
	    if (emailTemplateURL != null) {
		signer.update((emailTemplateURL.toString() + CRLF)
			      .getBytes(UTF8));
		signer.update((udigest + CRLF).getBytes(UTF8));
	    }
	    signer.update((pemDigest+CRLF).getBytes(UTF8));
	    for (String key: nameSet) {
		signer.update((req.getParameter(key).trim() + CRLF)
			      .getBytes(UTF8));
	    }
	    sb.append("signature: ");
	    sb.append(bytesToHex(signer.sign()) + CRLF);
	} catch (Exception e) {
	    throw new ServletAdapter.ServletException("signer error", e);
	}
	sb.append(CRLF);
	sb.append(publicKey);
	baos = new ByteArrayOutputStream(2048);
	GZIPOutputStream os = new GZIPOutputStream(baos);
	os.write(sb.toString().getBytes(UTF8));
	os.finish();
	os.close();
	sb = new StringBuilder(2048);
	PemEncoder pemEncoder = new PemEncoder(sb);
	pemEncoder.encode("DOCUMENT SIGNATURE DATA", baos.toByteArray());
	StringBuilder body = new StringBuilder(1024);
	TemplateProcessor.KeyMap keymap = new TemplateProcessor.KeyMap();
	keymap.put("name", name);
	keymap.put("type", type);
	keymap.put("date", date);
	keymap.put("timezone", timezone);
	if (document != null) {
	    keymap.put("document", document);
	}
	if (digest != null) {
	    keymap.put("digest", digest);
	}
	keymap.put("sigserver", sigserver);
	keymap.put("PEM", sb.toString());
	for (String key: nameSet) {
	    keymap.put(key.toLowerCase(), req.getParameter(key).trim());
	}
	TemplateProcessor tp = new TemplateProcessor(keymap);
	AppendableWriter aw = new AppendableWriter(body);
	tp.processTemplate(r, aw);
	aw.flush(); r.close(); aw.close();
	String bodystr = body.toString();
	String encodedBody = URLEncoder.encode(bodystr, UTF8);
	HashMap<String,String> queryMap = new HashMap<>();
	queryMap.put("subject", subject);
	if (cc != null) {
	    queryMap.put("cc", cc);
	}
	queryMap.put("body", bodystr);
	String query = WebEncoder.formEncode(queryMap, false, UTF8)
	    .replaceAll("[+]", "%20");

	keymap = new TemplateProcessor.KeyMap();
	keymap.put("color", color);
	keymap.put("bgcolor", bgcolor);
	keymap.put("linkColor", linkColor);
	keymap.put("visitedColor", visitedColor);
	keymap.put("buttonFGColor", buttonFGColor);
	keymap.put("buttonBGColor", buttonBGColor);
	keymap.put("bquoteBGColor", bquoteBGColor);
	keymap.put("name", name);
	keymap.put("email", email);
	keymap.put("sigserver", sigserver);
	keymap.put("type", type);
	if (mediaType != null) {
	    keymap.put("mimetype", mediaType);
	}
	if (document != null) {
	    keymap.put("document", document);
	    keymap.put("encDocument", URLEncoder.encode(document, UTF8));
	}
	keymap.put("sendto", sendto);
	keymap.put("query", query);
	// keymap.put("subject", subject);
	if (digest != null) {
	    keymap.put("digest", digest);
	}
	keymap.put("subject", subject);
	keymap.put("body", bodystr);
	//  keymap.put("encodedBody", encodedBody);
	tp = new TemplateProcessor(keymap);
	baos = new ByteArrayOutputStream(2048);
	Writer w = new OutputStreamWriter(baos, UTF8);

	r = new InputStreamReader(getClass()
				  .getResourceAsStream("signature.tpl"),
				  UTF8);
	tp.processTemplate(r, w);
	w.flush();
	byte[] results = baos.toByteArray();
	res.setHeader("content-type", "text/html");
	res.sendResponseHeaders(200, results.length);
	OutputStream os2 = res.getOutputStream();
	os2.write(results);
	os2.flush();
	os2.close();
    }
}
