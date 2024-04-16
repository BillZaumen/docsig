package org.bzdev.docsig;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.security.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
import org.bzdev.net.WebEncoder;
import org.bzdev.util.TemplateProcessor;
import org.bzdev.util.TemplateProcessor.KeyMap;

/**
 * ServletAdapter for creating DOCSIG requests.
 * This adapter provides an HTML page using data provided in a
 * query string. The URL parameters provided in the query string
 * are
 * <UL>
 *   <LI><B>name</B>. The corresponsding value is the user's name.
 *      If not present, a text field asking for the value will be included.
 *   <LI><B>email</B>. The corresponding value is the user's email address.
 *      If not present, a text field asking for the value will be included.
 *   <LI><B>id</B>. The corresponding value, which is optiona, is the
 *     user's ID (typically a number or some other code).
 *   <LI><B>transid</B>. The corresponding value, which is optional, is
 *     a transaction ID to provide in the generated request.
 * </UL>
 * <P>
 * The initialization parameters are
 * <UL>
 *   <LI><B>bgcolor</B>: the CSS background color for the generated HTML page.
 *      The default value is "rgb(10,10,25)".
 *   <LI><B>color</B>: The CSS foreground color for the generated HTML page
 *      The default value is "rgb(255,255,255)".
 *   <LI><B>inputBGColor</B>: The background color to use for controls in
 *       HTML forms. The default value is "rgb(10,10,64)".
 *   <LI><B>inputFGColor</B>: The foreground color to use for controls in HTML
 *      forms. The default value is "rgb(255,255,255)".
 *   <LI><B>type</B>:  The document type. The default value is "document".
 *   <LI><B>document</B>: The document URL. This must be an absolute URL.
 *   <LI><B>sendto</B>: The email reciptient.
 *   <LI><B>cc</B>:  An optional email address to which to send a copy.
 *   <LI><B>subject</B>: The subject line to use in an email message. The
 *       default value is "Document Signature"
 *   <LI><B>sigserver</B>: The URL for the signature server.
 *      This must be an absolute URL.
 * </UL>
 * The parameters <B>document</B>, <B>sendto</B>, and <B>sigserver</B>
 * are required. There are defaults for the other parameters.
 * <P>
 * This ServletAdapter can handle HTTP GET methods, but not HTTP POST
 * methods.
 */
public class RequestAdapter implements ServletAdapter {

    static final Charset UTF8 = Charset.forName("UTF-8");

    String bgcolor = "rgb(10,10,25)";
    String color = "white";
    String inputBG = "rgb(10,10,64)";
    String inputColor = "white";
    String type = "document";

    String document = null;
    String sendto = null;
    String cc = null;
    String subject = "Document Signature";
    String sigserver = null;
    File  template = null;


    @Override
    public void init(Map<String,String>parameters)
	throws ServletAdapter.ServletException
    {
	if (parameters == null) {
	    throw new ServletAdapter.ServletException("missing parameters");
	}
	String s = parameters.get("bgcolor");
	if (s != null) {
	    s = s.trim();
	    if (s.length() > 0) {
		bgcolor = s;
	    }
	}
	s = parameters.get("color");
	if (s != null) {
	    s = s.trim();
	    if (s.length() > 0) {
		color = s;
	    }
	}
	s = parameters.get("inputBGColor");
	if (s != null) {
	    s = s.trim();
	    if (s.length() > 0) {
		inputBG = s;
	    }
	}
	s = parameters.get("inputFGColor");
	if (s != null) {
	    s = s.trim();
	    if (s.length() > 0) {
		inputColor = s;
	    }
	}
	s = parameters.get("type");
	if (s != null) {
	    s = s.trim();
	    if (s.length() > 0) {
		type = s;
	    }
	}
	s = parameters.get("subject");
	if (s != null) {
	    s = s.trim();
	    if (s.length() > 0) {
		subject = s;
	    }
	}
	String key = "document";
	s = parameters.get(key);
	if (s == null) {
	    throw new ServletAdapter.ServletException("missing parameter: "
						      + key);
	} else {
	    s = s.trim();
	    if (s.length() == 0) {
		throw new ServletAdapter.ServletException("missing parameter: "
							  + key);
	    }
	    document = s;
	}
	key = "sendto";
	s = parameters.get(key);
	if (s == null) {
	    throw new ServletAdapter.ServletException("missing parameter: "
						      + key);
	} else {
	    s = s.trim();
	    if (s.length() == 0) {
		throw new ServletAdapter.ServletException("missing parameter: "
							  + key);
	    }
	    sendto = s;
	}
	key = "cc";
	s = parameters.get(key);
	if (s != null) {
	    s = s.trim();
	    if (s.length() != 0) {
		cc = s;
	    }
	}
	key = "sigserver";
	s = parameters.get(key);
	if (s == null) {
	    throw new ServletAdapter.ServletException("missing parameter: "
						      + key);
	} else {
	    s = s.trim();
	    if (s.length() == 0) {
		throw new ServletAdapter.ServletException("missing parameter: "
							  + key);
	    }
	    sigserver = s;
	}
	key = "template";
	s = parameters.get(key).trim();
	if (s != null) {
	    template = new File(s);
	    if (!(template.isFile() && template.canRead())) {
		String msg = "File \"" + s  +"\" not readable or not an "
		    + "ordinary file";
		throw new ServletAdapter.ServletException(msg);
	    }
	}
	
    }

    @Override
    public void doGet(HttpServerRequest req, HttpServerResponse res)
	throws IOException, ServletAdapter.ServletException
    {
	String name = req.getParameter("name");
	if (name != null) name = name.trim();
	if (name == null || name.length() == 0) {
	    name = null;
	}
	String email = req.getParameter("email");
	if (email != null) email = email.trim();
	if (email == null || email.length() == 0) {
	    email = null;
	}
	String id = req.getParameter("id");
	if (id != null) id = id.trim();
	if (id != null && id.length() == 0) {
	    id = null;
	}
	String transid = req.getParameter("transid");
	if (transid != null) transid = id.trim();
	if (transid != null && transid.length() == 0) {
	    id = null;
	}

	TemplateProcessor.KeyMap keymap = new  TemplateProcessor.KeyMap();
	keymap.put("bgcolor", bgcolor);
	keymap.put("color", color);
	keymap.put("inputBG", inputBG);
	keymap.put("inputColor", inputColor);
	keymap.put("type", WebEncoder.htmlEncode(type));
	keymap.put("document", WebEncoder.htmlEncode(document));
	keymap.put("sendto", WebEncoder.htmlEncode(sendto));
	if (cc != null) {
	    keymap.put("cc", WebEncoder.htmlEncode(cc));
	}
	keymap.put("subject", WebEncoder.htmlEncode(subject));
	keymap.put("sigserver", WebEncoder.htmlEncode(sigserver));

	if (name != null) {
	    keymap.put("name", WebEncoder.htmlEncode(name));
	}
	if (email != null) {
	    keymap.put("email", WebEncoder.htmlEncode(email));
	}
	if (id != null) {
	    keymap.put("id", WebEncoder.htmlEncode(id));
	}
	if (transid != null) {
	    keymap.put("transid", WebEncoder.htmlEncode(transid));
	}

	TemplateProcessor tp = new TemplateProcessor(keymap);
	ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
	Writer w = new OutputStreamWriter(baos, UTF8);

	Reader r = (template == null)?
	    new InputStreamReader(getClass().getResourceAsStream("request.tpl"),
				  UTF8):
	    new FileReader(template, UTF8);;
	tp.processTemplate(r, w);
	w.flush();
	byte[]results = baos.toByteArray();
	res.setHeader("content-type", "text/html");
	res.sendResponseHeaders(200, results.length);
	OutputStream os = res.getOutputStream();
	os.write(results);
	os.flush();
	os.close();
    }
}
