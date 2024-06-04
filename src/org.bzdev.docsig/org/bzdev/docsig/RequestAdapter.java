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
import java.util.Set;
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
 *   <LI><B>linkColor</B>: The color to use for links.  The default
 *      value is "rgb(65,225,128)".;
 *   <LI><B>visitedColor</B>:The color to use for visited links. The
 *      default value is "rgb(65,164,128)".
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
    String linkColor = "rgb(65,225,128)";
    String visitedColor = "rgb(65,164,128)";
    String inputBG = "rgb(10,10,64)";
    String inputColor = "white";
    String bquoteBGColor = "rgb(32,32,32)";
    String frameFraction = "0.5";

    String type = "document";

    String document = null;
    String documentURL = null;
    String digest = null;
    String sendto = null;
    String cc = null;
    String subject = "Document Signature";
    String sigserver = null;
    File  template = null;
    boolean fillText = false;

    String borderColor = "steelblue";
    String additionalFormElements = null;


    Set<String> inputTypes = Set
	.of("hidden", "text", "tel", "url", "email",
	    "password", "dateTime", "date", "time",
	    "datetime-local", "number", "range", "color",
	    "checkbox", "radio");

    Set<String> inputAttr = Set
	.of("type", "checked", "value", "valueAsDate", "valueAsNumber",
	    "height", "width", "step", "size", "max", "min", "inputmode",
	    "maxlength", "multiple", "placeholder", "readonly", "label",
	    "heading", "name", "id");

    Set<String> noValAttr = Set
	.of("checked", "multiple", "readonly", "required");

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
	s = parameters.get("linkColor");
	if (s != null) {
	    s = s.trim();
	    if (s.length() > 0) {
		linkColor = s;
	    }
	}
	s = parameters.get("visitedColor");
	if (s != null) {
	    s = s.trim();
	    if (s.length() > 0) {
		visitedColor = s;
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
	s = parameters.get("borderColor");
	if (s != null) {
	    s = s.trim();
	    if (s.length() > 0) {
		borderColor = s;
	    }
	}

	s = parameters.get("bquoteBGColor");
	if (s != null) {
	    s = s.trim();
	    if (s.length() > 0) {
		bquoteBGColor = s;
	    }
	}

	String key = "document";
	s = parameters.get(key);
	if (s != null) {
	    s = s.trim();
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
	s = parameters.get(key);
	if (s != null) {
	    s = s.trim();
	    template = new File(s);
	    if (!(template.isFile() && template.canRead())) {
		String msg = "File \"" + s  +"\" not readable or not an "
		    + "ordinary file";
		throw new ServletAdapter.ServletException(msg);
	    }
	}
	
	key  = "fillText";
	s = parameters.get(key);
	if (s != null) {
	    s = s.trim();
	    fillText = Boolean.parseBoolean(s);
	}

	key = "additionalFormElements";
	s = parameters.get(key);
	if (s != null) {
	    s.trim();
	    additionalFormElements = s;
	}
    }

    static  MessageDigest createMD() {
	try {
	    return MessageDigest.getInstance("SHA-256");
	} catch (Exception e) {
	    return null;
	}
    }

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

    @Override
    public void doGet(HttpServerRequest req, HttpServerResponse res)
	throws IOException, ServletAdapter.ServletException
    {

	if (document != null && documentURL == null) {
	    try {
		URL url = new URL(document);
		URLConnection urlc = url.openConnection();
		String mtype = urlc.getContentType();
		int ind = mtype.indexOf(";");
		if (ind != -1) {
		    mtype = mtype.substring(0, ind).trim();
		}
		InputStream is = urlc.getInputStream();
		ByteArrayOutputStream os1 = new ByteArrayOutputStream(8192);
		is.transferTo(os1);
		byte[] bytes = os1.toByteArray();
		MessageDigest md = createMD();
		md.update(bytes);
		digest = bytesToHex(md.digest());
		if (mtype.equalsIgnoreCase("text/plain")) {
		    is = new ByteArrayInputStream(bytes);
		    Reader r = new InputStreamReader(is, UTF8);
		    StringBuffer sb = new StringBuffer();
		    AppendableWriter aw = new AppendableWriter(sb);
		    PrintWriter w = new PrintWriter(aw);
		    String hdr =
			"<!DOCTYPE html><html lang=\"en\"><head>"
			+ "<meta charset=\"UTF-8\">"
			+ "<meta name=\"viewport\" "
			+ "content=\"width=device-width, "
			+ "initial-scale=1.0\">\r\n"
			+ "<style type=\"text/css\">\r\n"
			+ "BODY {\r\n"
			+ "   background-color: " + bquoteBGColor + ";\r\n"
			+ "   color: " + color + ";\r\n"
			+ "   margin: 2em;\r\n"
			+ "}\r\n"
			+ "</style>\r\n"
			+ "</head><body>";
			r.transferTo(w);
		    w.flush();
		    String data;
		    if (fillText) {
			String[] paragraphs = sb.toString()
			    .split("(\\s*\\n){2,}");
			sb.setLength(0);
			for (String paragraph: paragraphs) {
			    paragraph = paragraph.replaceAll("^\\h+", " ");
			    int len = paragraph.length();
			    if (paragraph.startsWith(" ") && len > 2) {
				String shift = null;
				String bullet = null;
				char ch1 = paragraph.charAt(1);
				char ch2 = paragraph.charAt(2);
				char ch3 = (len > 3)? paragraph.charAt(3): '\0';
				if (ch2 == ' ' || ch2 == '\t' || ch2 == '.') {
				    switch(ch1) {
				    case '*':
					if (ch2 == ' ' || ch2 == '\t') {
					    paragraph = paragraph.substring(2);
					    bullet = "&bull;";
					    shift = "-0.9em";
					}
					break;
				    case '1':
				    case '2':
				    case '3':
				    case '4':
				    case '5':
				    case '6':
				    case '7':
				    case '8':
				    case '9':
					if (ch2 == '.' && (ch3 == ' '
							   || ch3 == '\t')) {
					    paragraph = paragraph.substring(1);
					    shift = "-1.25em";
					}
				    }
				}
				if (shift != null) {
				    sb.append("<P style=\"margin-left: 2em;"
					      + "text-indent:" + shift +";"
					      +"\">");
				    if (bullet != null) {
					sb.append(bullet);
				    }
				} else {
				    sb.append("<P style=\"margin-left: 2em\">");
				}
			    } else {
				sb.append("<P>");
			    }
			    paragraph = WebEncoder.htmlEncode(paragraph);
			    sb.append(paragraph);
			}
			data = hdr + sb.toString() + "<body></html>";
		    } else {
			data = hdr + "<pre>"
			    + WebEncoder.htmlEncode(sb.toString())
			    +"</pre></body></html>";
		    }
		    // We need %20 instead of '+' for the data URL to work.
		    data = URLEncoder.encode(data).replace("+", "%20");
		    // System.out.println("data = " + data);
		    documentURL = "data:text/html," + data;
		} else {
		    documentURL = document;
		}
	    } catch (Exception e) {
		documentURL = null;
		String msg = "cannot handle " + document;
		throw new ServletAdapter.ServletException(msg, e);
	    }
	}

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
	keymap.put("linkColor", linkColor);
	keymap.put("visitedColor", visitedColor);
	keymap.put("inputBG", inputBG);
	keymap.put("inputColor", inputColor);
	keymap.put("borderColor", borderColor);
	keymap.put("bquoteBGColor", bquoteBGColor);
	keymap.put("frameFraction", frameFraction);
	keymap.put("digest", digest);

	keymap.put("type", WebEncoder.htmlEncode(type));
	if (documentURL != null) {
	    keymap.put("documentURL", documentURL);
	}
	if (document != null) {
	    keymap.put("document", WebEncoder.htmlEncode(document));
	}
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
	if (additionalFormElements != null) {
	    keymap.put("additionalFormElements", additionalFormElements);
	}

	TemplateProcessor tp = new TemplateProcessor(keymap);
	ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
	Writer w = new OutputStreamWriter(baos, UTF8);

	Reader r = (template == null)?
	    new InputStreamReader(getClass().getResourceAsStream("request.tpl"),
				  UTF8):
	    new FileReader(template, UTF8);
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
