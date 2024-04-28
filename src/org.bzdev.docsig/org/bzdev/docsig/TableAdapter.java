package org.bzdev.docsig;
import java.io.*;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.HashMap;

import org.bzdev.docsig.verify.DocsigVerifier;
import org.bzdev.docsig.verify.DocsigVerifier.Result;
import org.bzdev.io.*;
import org.bzdev.net.*;
import org.bzdev.util.TemplateProcessor;
import org.bzdev.util.TemplateProcessor.KeyMap;

public class TableAdapter implements ServletAdapter {

    static final Charset UTF8 = Charset.forName("UTF-8");

    static Map<String,String> colmap = new HashMap<>();
    static {
	String coldata[][] = {
	    {"acceptedBy","Name"},
	    {"timestamp","UTC time"},
	    {"date","Date"},
	    {"timezone","Time Zone"},
	    {"ipaddr","IP Address"},
	    {"id","ID"},
	    {"transID","Transaction ID"},
	    {"email","Email Address"},
	    {"server","Server URL"},
	    {"sendto","Recipient"},
	    {"cc","CC"},
	    {"document","Document URL"},
	    {"type","Document Type"},
	    {"digest","Document SHA256 Digest"},
	    {"publicKeyID","Server Public=Key ID"},
	    {"signature","Digital Signature"},
	    {"from","Sender Email Address"},
	    {"message-id","Message ID"},
	    {"status","Entry Valid?"},
	    {"reasons","Reasons if not valid"}
	};
	for (String[] row: coldata) {
	    colmap.put(row[0], row[1]);
	}
    }

    String bgcolor = "rgb(10,10,25)";
    String color = "white";
    String inputBG = "rgb(10,10,64)";
    String inputColor = "white";
    String accentColor = "green";

    @Override
    public void init(Map<String,String>parameters)
	throws ServletAdapter.ServletException
    {
	if (parameters == null) {
	    // all of the parameters for this servlet adapter
	    // have default values.
	    return;
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
	s = parameters.get("accentColor");
	if (s != null) {
	    s = s.trim();
	    if (s.length() > 0) {
		accentColor = s;
	    }
	}
    }

    @Override
    public void doGet(HttpServerRequest req, HttpServerResponse res)
	throws IOException, ServletAdapter.ServletException
    {
	String url = req.getRequestURL();
	TemplateProcessor.KeyMap keymap = new  TemplateProcessor.KeyMap();
	keymap.put("bgcolor", bgcolor);
	keymap.put("color", color);
	keymap.put("inputBG", inputBG);
	keymap.put("inputColor", inputColor);
	keymap.put("accentColor", accentColor);
	keymap.put("url", url);
	TemplateProcessor tp = new TemplateProcessor(keymap);
	ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
	Writer w = new OutputStreamWriter(baos, UTF8);

	Reader r =
	    new InputStreamReader(getClass().getResourceAsStream("table.tpl"),
				  UTF8);
	tp.processTemplate(r, w);
	w.flush();
	byte[]results = baos.toByteArray();
	res.setHeader("content-type", "text/html; charset=UTF-8");
	res.sendResponseHeaders(200, results.length);
	OutputStream os = res.getOutputStream();
	os.write(results);
	os.flush();
	os.close();
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

    enum Mode {
	ALL,
	VALID,
	INVALID
    }

    @Override
    public void doPost(HttpServerRequest req, HttpServerResponse res)
	throws IOException, ServletAdapter.ServletException
    {
	Mode mode = Mode.ALL;
	boolean showHeaders = false;
	String expectedDocument = null;
	String expectedDigest = null;
	String expectedServer = null;
	
	String type = req.getMediaType();
	InputStream is = req.getDecodedInputStream();
	char[] chars = null;
	if (type.equalsIgnoreCase("multipart/form-data")) {
	    DocsigVerifier.Result[] results = null;
	    String columns[] = null;
	    String boundary = req.getFromHeader("content-type", "boundary");
	    FormDataIterator fdi = new FormDataIterator(is, boundary);
	    while (fdi.hasNext()) {
		InputStream fis = fdi.next();
		BufferedReader r =
		    new BufferedReader(new InputStreamReader(fis, UTF8));
		String name = fdi.getName();
		if (name.equals("mbox")) {
		    int estsize = req.getContentLength();
		    if (estsize < 0) estsize = (1 << 15);
		    CharArrayWriter cw = new CharArrayWriter(estsize);
		    r.transferTo(cw);
		    chars = cw.toCharArray();
		} else {
		    StringBuilder sb = new StringBuilder();
		    Writer w = new AppendableWriter(sb);
		    r.transferTo(w);
		    String value = sb.toString().trim();
		    sb.setLength(0);
		    if (name.equals("columns")) {
			columns = value.split("\\s*,\\s*");
		    } else if (name.equals("mode")) {
			if (value.equals("valid")) {
			    mode = Mode.VALID;
			} else if (value.equals("invalid")) {
			    mode = mode.INVALID;
			}
		    } else if (name.equals("showHeadings")) {
			if (value.equals("true")) {
			    showHeaders = true;
			}
		    } else if (name.equals("expectedDocument")) {
			if (value.length() > 0) {
			    expectedDocument = value;
			}
		    } else if (name.equals("expectedDigest")) {
			if (value.length() > 0) {
			    expectedDigest = value;
			}
		    } else if (name.equals("expectedServer")) {
			if (value.length() > 0) {
			    expectedServer = value;
			}
		    } else {
			fis.transferTo(OutputStream.nullOutputStream());
		    }
		}
		fis.close();
	    }
	    results = DocsigVerifier
		.decodeFromMbox(new CharArrayReader(chars),
				new PrintWriter(Writer.nullWriter()),
				expectedDocument,
				expectedDigest,
				expectedServer);

	    if (columns == null && results == null) {
		is.transferTo(OutputStream.nullOutputStream());
		is.close();
		sendSimpleResponse(res, 400, "column names and mbox missing");
		return;
	    }
	    if (columns == null) {
		is.transferTo(OutputStream.nullOutputStream());
		is.close();
		sendSimpleResponse(res, 400, "No column-names provided");
		return;
	    }
	    if (results == null) {
		is.transferTo(OutputStream.nullOutputStream());
		is.close();
		sendSimpleResponse(res, 400, "No mbox");
		return;
	    }
	    res.setHeader("content-type", "text/csv; charset=UTF-8");
	    StringBuilder sb = new StringBuilder();
	    Writer rwriter = new AppendableWriter(sb);
	    CSVWriter csvw = new CSVWriter(rwriter, columns.length);
	    if (showHeaders) {
		for (String col: columns) {
		    String heading = colmap.get(col);
		    if (heading  == null) heading = col;
		    csvw.writeField(heading);
		}
	    }
	    for (DocsigVerifier.Result result: results) {
		HeaderOps headers = result.getHeaders();
		if (mode != Mode.ALL) {
		    if (result.getStatus()) {
			if (mode == Mode.INVALID) continue;
		    } else {
			if (mode == Mode.VALID) continue;
		    }
		}
		for (String col: columns) {
		    String value = null;
		    if (col.equalsIgnoreCase("from")) {
			value = result.getEmailAddr();
		    } else if (col.equalsIgnoreCase("message-id")) {
			value = result.getMessageID();
		    } else if (col.equalsIgnoreCase("status")) {
			value = "" + result.getStatus();
		    } else if (col.equalsIgnoreCase("reasons")) {
			value = result.getReasons();
		    } else {
			value = headers.getFirst(col);
		    }
		    if (value == null) value = "";
		    csvw.writeField(value);
		}
	    }
	    csvw.flush();
	    csvw.close();
	    byte[] bytes = sb.toString().getBytes(UTF8);
	    res.sendResponseHeaders(200, bytes.length);
	    OutputStream os = res.getOutputStream();
	    os.write(bytes);
	    os.flush();
	    os.close();
	} else {
	    is.transferTo(OutputStream.nullOutputStream());
	    is.close();
	    sendSimpleResponse(res, 400, "input is not form data");
	}
    }
}
