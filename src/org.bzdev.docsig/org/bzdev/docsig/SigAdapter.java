package org.bzdev.docsig;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.security.*;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
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
    
    static final int MAX_ENTRIES = 64;

    static Map<String,Entry> cache = new LinkedHashMap<String,Entry>() {
	    protected boolean removeEldestEntry(Map.Entry entry) {
		return size() > MAX_ENTRIES;
	    }
	};

    static {
	cache = Collections.synchronizedMap(cache);
    }

    String color = null;
    String bgcolor = null;
    String linkColor = null;
    String visitedColor = null;
    String buttonFGColor = null;
    String buttonBGColor = null;
    String bquoteBGColor = null;
    File publicKeyDir = null;

    @Override
    public void init(Map<String,String>parameters)
	throws ServletAdapter.ServletException
    {
	if (parameters == null) {
	    throw new ServletAdapter.ServletException("missing parameters");
	}

	color = parameters.get("color");
	bgcolor = parameters.get("bgcolor");
	linkColor = parameters.get("linkColor");
	visitedColor = parameters.get("visitedColor");
	buttonFGColor = parameters.get("buttonFGColor");
	buttonBGColor = parameters.get("buttonBGColor");
	bquoteBGColor = parameters.get("bquoteBGColor");

	String keydir = parameters.get("publicKeyDir");
	if (keydir != null) {
	    publicKeyDir = new File(keydir);
	    publicKeyDir.mkdirs();
	    if (publicKeyDir.isDirectory()) {
		byte[]contents = publicKey.getBytes(UTF8);
		MessageDigest md = createMD();
		md.update(contents);
		String fname = bytesToHex(md.digest()) + ".pem";
		try {
		    OutputStream os = new
			FileOutputStream(new File(publicKeyDir, fname));
		    os.write(contents);
		    os.flush();
		    os.close();
		} catch (Exception e) {
		    throw new ServletAdapter.ServletException
			("Cannot create PEM file", e);
		}
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
	String hasKey = req.getParameter("hasKeyRequest");
	String docurl = req.getParameter("url");
	String digest = req.getParameter("digest");
	String pemreq = req.getParameter("publicKeyRequest");
	String getreq = req.getParameter("getPublicKeys");
	docurl = (docurl == null)? null: docurl.trim();
	digest = (digest == null)? null: digest.trim().toLowerCase();
	pemreq = (pemreq == null)? null: pemreq.trim();
	getreq = (getreq == null)? null: getreq.trim().toLowerCase();
	if (docurl == null || digest == null) {
	    
	    if (getreq != null && getreq.length() != 0) {
		if (hasKey != null || docurl != null || digest != null
		    || pemreq != null) {
		    sendSimpleResponse(res, 400, "conflicting query fields");
		} else {
		    // Get a zip file for the public key files
		    if (publicKeyDir == null) {
			sendSimpleResponse(res, 404, "no public key directory");
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
		sendSimpleResponse(res, 400, "missing query fields");
	    } else if (hasKey != null && pemreq != null) {
		sendSimpleResponse(res, 400, "conflicting query fields");
		return;
		
	    } else if (hasKey != null) {
		if (publicKeyDir == null) {
		    sendSimpleResponse(res, 501, "no public key directory");
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
		URLConnection urlc = url.openConnection();
		if (urlc instanceof HttpURLConnection) {
		    HttpURLConnection hurlc = (HttpURLConnection) urlc;
		    hurlc.connect();
		    int rcode = hurlc.getResponseCode();
		    InputStream is  = hurlc.getInputStream();
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
	    URLConnection urlc = url.openConnection();
	    if (urlc instanceof HttpURLConnection) {
		HttpURLConnection hurlc = (HttpURLConnection) urlc;
		hurlc.connect();
		int rcode = hurlc.getResponseCode();
		InputStream is  = hurlc.getInputStream();
		if (rcode != 200) {
		    OutputStream nos = OutputStream.nullOutputStream();
		    is.transferTo(nos);
		    sendSimpleResponse(res, rcode,
				       "Document server did not recognize\r\n"
				       + docurl);
		    // res.sendError(rcode);
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
	String type = req.getParameter("type");
	if (type == null)
	    throw new ServletException("parameter 'type' is missing");
	type = type.strip().replaceAll("\\s","");
	String document = req.getParameter("document");
	if (document == null)
	    throw new ServletException("parameter 'document' missing");
	document = document.strip().replaceAll("\\s","");
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
	String ipaddr = req.getRemoteAddr();


	if (name == null || email == null || sigserver == null
	    || type == null || document == null
	    || sendto == null || subject == null) {
	    throw new ServletException("missing post data");
	}
	URL url = new URL(document);
	URLConnection urlc = url.openConnection();
	if (urlc instanceof HttpURLConnection) {
	    HttpURLConnection hurlc = (HttpURLConnection) urlc;
	    hurlc.connect();
	    InputStream is  = hurlc.getInputStream();
	    int rcode = hurlc.getResponseCode();
	    if (rcode != 200) {
		OutputStream nos = OutputStream.nullOutputStream();
		is.transferTo(nos);
		res.sendError(rcode);
		return;
	    }
	    String mediaType = hurlc.getContentType();
	    ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
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
	    StringBuilder sb = new StringBuilder(1024);
	    sb.append("acceptedBy: "); sb.append(name); sb.append(CRLF);
	    sb.append("date: "); sb.append(timestamp); sb.append(CRLF);
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
	    sb.append("document: "); sb.append(document);sb.append(CRLF);
	    sb.append("type: "); sb.append(type); sb.append(CRLF);
	    sb.append("digest: "); sb.append(digest); sb.append(CRLF);
	    sb.append("publicKeyID: "); sb.append(pemDigest); sb.append(CRLF);
	    try {
		Signature signer = sbuPrivate.getSigner();
		signer.update((name+CRLF).getBytes(UTF8));
		signer.update((timestamp + CRLF).getBytes(UTF8));
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
		signer.update((document+CRLF).getBytes(UTF8));
		signer.update((type + CRLF).getBytes(UTF8));
		signer.update((digest+CRLF).getBytes(UTF8));
		signer.update((pemDigest+CRLF).getBytes(UTF8));
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
	    keymap.put("document", document);
	    keymap.put("digest", digest);
	    keymap.put("PEM", sb.toString());
	    TemplateProcessor tp = new TemplateProcessor(keymap);
	    AppendableWriter aw = new AppendableWriter(body);
	    Reader r = new
		InputStreamReader(getClass().getResourceAsStream("email.tpl"),
				  UTF8);
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
	    keymap.put("mimetype", mediaType);
	    keymap.put("document", document);
	    keymap.put("sendto", sendto);
	    keymap.put("query", query);
	    // keymap.put("subject", subject);
	    keymap.put("digest", digest);
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
	} else {
	    res.sendError(404);
	}
    }
}
