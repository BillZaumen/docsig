package org.bzdev.docsig.verify;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.MessageDigest;
import java.security.spec.*;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.zip.*;

import org.bzdev.io.AppendableWriter;
import org.bzdev.net.*;
import org.bzdev.util.ACMatcher;
import org.bzdev.util.ACMatcher.MatchResult;
import org.bzdev.util.TemplateProcessor;
import org.bzdev.util.TemplateProcessor.KeyMap;

/**
 * Class for parsing and verifying messages created by
 * a DOCSIG server. Methods allow both message contents
 * and mbox-formatted files containing multiple messages.
 * <P>
 * These message represent signatures applied to simple
 * documents (text files, PDF files, etc.). A DOCSIG
 * server will provide a public key used to prove that
 * a specific part of the contents of an email were
 * generated by the server.
 */
public class DocsigVerifier {

    static final Charset UTF8 = Charset.forName("UTF-8");
    static final String CRLF = "\r\n";

    private static final String PEM_START =
	"-----BEGIN DOCUMENT SIGNATURE DATA-----";

    private static ACMatcher matcher = new
	ACMatcher(true, "From ",
		  PEM_START,
		  "-----END DOCUMENT SIGNATURE DATA-----",
		  "From: ",
		  "Message-ID: ",
		  "Content-Transfer-Encoding: ",
		  "\n\r\n",
		  "\n\n");

    private static enum State {
	START,
	FOUND_FROM,
	FOUND_BEGIN,
	FOUND_END
    }

    private static ACMatcher matcher2 = new ACMatcher("signature-algorithm: ");

    static final char[] hexArray = {
	'0','1','2','3','4','5','6','7','8','9',
	'a','b','c','d','e','f'};

    static String bytesToHex(byte[] bytes) {
	char[] hexChars = new char[bytes.length * 2];
	int v;
	for ( int j = 0; j < bytes.length; j++ ) {
	    v = bytes[j] & 0xFF;
	    hexChars[j * 2] = hexArray[v >>> 4];
	    hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	}
	return new String(hexChars);
    }

    static byte[] hexToBytes(String hexString) {
	int hlen = hexString.length();
	if (hlen % 2 == 1) return null;
	int len = hlen/2;
	byte[] bytes = new byte[len];
	for (int i = 0; i < len; i++) {
	    int hind1 = i*2;
	    int hind2 = hind1+1;
	    int hind3 = hind2+1;
	    String v1 = hexString.substring(hind1, hind2);         
	    String v2 = hexString.substring(hind2, hind3);
	    int bhigh = Integer.parseInt(v1, 16);
	    int blow = Integer.parseInt(v2, 16);
	    bytes[i] = (byte)((0xff) & (bhigh << 4 | blow));
	}
	return bytes;
    }


    static private MessageDigest createMD() {
	try {
	    return MessageDigest.getInstance("SHA-256");
	} catch (Exception e) {
	    return null;
	}
    }

    /*
     * Initialize with set of preferred providers.
     */
    private static HashMap<String,String> sigpmap = new HashMap<>();
    static {
	sigpmap.put("Sha256withECDSA", "SunEC");
    }

    private static HashMap<String,String> pmap = new HashMap<>();
    static {
	pmap.put("EC", "SunEC");
    }

    static private String[] headerKeys = {
	"acceptedBy", "date", "ipaddr", "id", "transID", "email",
	"server", "sendto", "cc", "document", "type", "digest", "publicKeyID"
    };

    /**
     * Class providing results of parsing/verifying DOCSIG emails.
     */
    public static class Result {
	HeaderOps headers;
	String emailName;
	String emailAddr;
	String messageID;
	boolean status;
	String reasons;

	/**
	 * Get DOCSIG headers.
	 * The header keys are
	 * <UL>
	 *   <LI><STRONG>acceptedBy</STRONG>. This is the name provided
	 *     by the individual signing the document.
	 *   <LI><STRONG>date</STRONG>. This is the date and time of day
	 *     at which the signature was generated by a DOCSIG server.
	 *   <LI><STRONG>ipaddr</STRONG>. This is the IP address of the
	 *      user or the last proxy when the signature-creation
	 *      request was sent to the server.
	 *   <LI><STRONG>id</STRONG>. This is field is optional. When
	 *      not null, it is an ID associated with the sender.
	 *   <LI><STRONG>transID</STRONG>. This is field is optional. When
	 *      not null, it is a transaction ID, typically by an entity
	 *      providing a user with a web page containing a form that
	 *      will generate a signature-creation request.
	 *   <LI><STRONG>email</STRONG>. This is the email address the user
	 *      intends to use. It should be the one the user registers with
	 *      the entity providing the document
	 *   <LI><STRONG>server</STRONG>. This is the URL for requests to a
	 *      DOCSIG server, whether sent with HTTP GET or POST methods.
	 *   <LI><STRONG>sendto</STRONG>. This is the email address of the
	 *      entity that will process signatures.
	 *   <LI><STRONG>document</STRONG>. This is the URL of the document
	 *      to sign.
	 *   <LI><STRONG>digest</STRONG>. This is a SHA-256 message digest
	 *      of the document that is to be signed.
	 *   <LI><STRONG>publicKeyID</STRONG>. This is the SHA-256
	 *      message digest of the public key used to sign the
	 *      values of these headers in the order shown above (a
	 *      string consisting of a carriage return followed by a
	 *      line feed is added to each value when these values are
	 *      signed).
	 * </UL>
	 * To obtain the value of a key, call the {@link HeaderOps} method
	 * {@link HeaderOps#getFirst(String) getFirst}. This method
	 * returns a {@link String} containing the value for the key
	 * (also a {@link String}) passed as <CODE>getFirst</CODE>'s
	 * sole argument.
	 * <P>
	 * Two additional headers are available:
	 * <UL>
	 *   <LI><STRONG>signature</STRONG>.
	 *   <LI><STRONG>signature-algorithm</STRONG>.
	 * </UL>
	 * These are used by the implementation.
	 * @return the headers
	 */
	public HeaderOps getHeaders() {return headers;}
	/**
	 * Get the name associated with an email (e.g., a name
	 * provided by the contents of the "From:" header.
	 * @return the name
	 */
	public String getEmailName() {return emailName;}
	/**
	 * Get the email address of the sender. This is obtained
	 * from the initial 'From ' header in a message.
	 * @return the address
	 */
	public String getEmailAddr() {return emailAddr;}
	/**
	 * Get the Email message ID for an email
	 * @return the message ID
	 */
	public String getMessageID() {return messageID;}
	/**
	 * Get the status for this email.
	 * 
	 * @return true if the message was verified; false otherwise
	 */
	public boolean getStatus() {return status;}

	private void setStatus(boolean status) {
	    this.status = status;
	}

	private void addToReasons(String reason) {
	    reasons = addReason(reasons, reason);
	}

	/**
	 * Get the reason for a status.
	 * If the status is <CODE>true</CODE>, the value returned will
	 * be an empty string. Otherwise the value will be a
	 * comma-separated list of keywords, any of which can be:
	 * <UL>
	 *  <LI><STRONG>PublicKeyDigest</STRONG>. The public key digest
	 *    in a message does not match that for the PEM-encoded public key.
	 *  <LI><STRONG>NotServerPublicKey</STRONG>. The public key does match
	 *    one that had been used by the DOCSIG server.
	 *  <LI><STRONG>BadServerRequest</STRONG>. Illegal request to the
	 *       DOCSIG server.
	 *  <LI><STRONG>PublicKeysNotOnServer</STRONG>. Public Keys are not
	 *       available on the DOCSIG server.
	 *  <LI><STRONG>NoServer</STRONG>. It was not possible to connect to
	 *      the DOCSIG server.
	 *  <LI><STRONG>BadSignature</STRONG>.  When validating a message,
	 *      the digital signature failed.
	 * </UL>
	 *
	 * @return the reason, represented as a comma-separated
	 *         list of keywords
	 */
	public String getReasons() {return reasons;}

	Result(HeaderOps headers, String emailName, String emailAddr,
	       String messageID, boolean status, String reasons)
	{
	    this.headers = headers;
	    this.emailName = emailName;
	    this.emailAddr = emailAddr;
	    this.messageID = messageID;
	    this.status = status;
	    this.reasons = reasons;
	}
    }

    private static void addReason(StringBuilder sb, String reason) {
	if (sb.length() > 0) {
	    sb.append(", ");
	}
	sb.append(reason);
    }

    private static String addReason(String s, String reason) {
	if (s.length() > 0) {
	    s = s + ", ";
	}
	s = s + reason;
	return s;
    }

    private static Result decode(String emailName, String emailAddr,
				 String messageID,
				 String s, PrintWriter ew)
	throws IOException
    {
	boolean status = true;
	StringBuilder rsb = new StringBuilder();

	PemDecoder.Result result = PemDecoder.decode(s);
	byte[] bytes = result.getBytes();
	InputStream is = new ByteArrayInputStream(bytes);
	is = new GZIPInputStream(is, bytes.length);
	Reader r = new InputStreamReader(is, UTF8);
	StringBuilder sb = new StringBuilder(2048);
	Writer w = new AppendableWriter(sb);
	r.transferTo(w);
	w.flush(); w.close();
	String firstDecoded = sb.toString();
	// System.out.println("**************");
	// System.out.print(sb.toString());
	// System.out.println("**************");
	String publicKeyPEM = null;
	for (MatchResult mr2: matcher2.iterableOver(firstDecoded)) {
	    int ind = mr2.getStart();
	    if (ind > 2 && firstDecoded.charAt(ind-1) == '\n'
		&& firstDecoded.charAt(ind-2) == '\r') {
		publicKeyPEM = firstDecoded.substring(ind);
		break;
	    }
	}
	MessageDigest md = createMD();
	md.update(publicKeyPEM.getBytes(UTF8));
	String pd = bytesToHex(md.digest());

	result = PemDecoder.decode(firstDecoded);
	HeaderOps headers = result.getHeaders();
	if (!pd.equals(headers.getFirst("publicKeyID"))) {
	    // bad input.
	    if (ew != null) {
		ew.println("For messageID: " + messageID + ",");
		ew.println("    bad PEM");
	    }
	    status = false;
	    addReason(rsb, "PublicKeyDigest");
	}
	try {
	    URL url = new URL(headers.getFirst("server")
			      + "?hasKeyRequest=" + pd);
	    URLConnection urlc = url.openConnection();
	    if (urlc instanceof HttpURLConnection) {
		urlc.connect();
		HttpURLConnection hurlc = (HttpURLConnection) urlc;
		switch (hurlc.getResponseCode()) {
		case 204:
		    // Found the key, but 204 because no coneent returned
		    break;
		case 404:
		    if (ew != null) {
			ew.println("For messageID: " + messageID + ",");
			ew.println("    public key not recognized by server");
		    }
		    status = false;
		    addReason(rsb,"NotServerPublicKey");
		    break;
		case 422:
		    if (ew != null) {
			ew.println("For messageID: " + messageID + ",");
			ew.println("    bad request to server");
		    }
		    status = false;
		    addReason(rsb, "BadServerRequest");
		    break;
		case 501:
		    if (ew != null) {
			ew.println("For messageID: " + messageID + ",");
			ew.println("    Public keys not available at server");
		    }
		    status = false;
		    addReason(rsb,"PublicKeysNotOnServer");
		    break;
		}
	    }
	} catch (IOException e) {
	    if (ew != null) {
		ew.println("For messageID: " + messageID + ",");
		ew.println("    could not contact server: "
			   +  e.getMessage());
		addReason(rsb, "NoServer");
	    }
	    status = false;
	}
	// System.out.println();
				       
					   
	try {
	    SecureBasicUtilities sbutils =
		new SecureBasicUtilities(firstDecoded);
	    Signature verifier = sbutils.getVerifier();
	    for (String name: headerKeys) {
		String value = headers.getFirst(name);
		if (value != null) {
		    verifier.update((value + CRLF).getBytes(UTF8));
		}
	    }
	    String signatureStr = headers.getFirst("signature");
	    if (signatureStr != null) {
		byte[] signature = hexToBytes(signatureStr);
		if (!verifier.verify(signature)) {
		    if (ew != null) {
			ew.println("For messageID: " + messageID + ",");
			ew.println("    bad signature");
		    }
		    status  = false;
		    addReason(rsb, "BadSignature");
		} /* else {
		   System.out.println("signature OK");
		}
		  */
	    }
	} catch (GeneralSecurityException e) {
	    if (ew != null) {
		ew.println("For messageID: " + messageID + ",");
		ew.println("    signature-processing error");
	    }
	    status  = false;
	    addReason(rsb, "ProcessingError");
	}
	/*
	System.out.println("emailName = " + emailName
			   + ", emailAddr = " + emailAddr);
	*/
	return new Result(headers, emailName, emailAddr, messageID,
			  status, rsb.toString());
    }

    private static String pqEncode(String s) {
	StringBuilder sb = new StringBuilder();
	s.chars().forEachOrdered((ch) -> {
		if (ch == 20) {
		    sb.append(" ");
		} if (ch == 61 /*=*/) {
		    sb.append("=3D");
		} else if (ch >= 32 && ch <= 60) {
		    sb.append((char)ch);
		} else if (ch >= 62 && ch <= 126) {
		    sb.append((char)ch);
		} else if (ch < 256) {
		    String hex = Integer.toHexString(ch);
		    if (hex.length() == 1) {
			hex = "0" + hex;
		    }
		    hex = hex.toLowerCase();
		    sb.append("%" + hex);
		} else {
		    int i1 = ch & 0xff;
		    int i2 = ch & 0xff00;
		    String hex = Integer.toHexString(i2);
		    if (hex.length() == 1) {
			hex = "0" + hex;
		    }
		    sb.append("%" + hex);
		    hex = Integer.toHexString(i1);
		    if (hex.length() == 1) {
			hex = "0" + hex;
		    }
		    sb.append("%" + hex);
		}
	    });
	return sb.toString();
    }

    /**
     * Decode multiple messages stored using mbox format, also
     * providing a {@link PrintWriter} for error messages.
     * @param r a {@link java.io.Reader} used to read an mbox file
     * @param ew a {@link java.io.PrintWriter} used to record error messages
     * @return an array of {@link Result} describing the status of
     *         a signature request and the values of various headers
     * @throws IOException if an IO error occurred
     */
    public static Result[] decodeFromMbox(Reader r, PrintWriter ew)
	throws IOException
    {
	if (ew == null) {
	    ew = new PrintWriter(new OutputStreamWriter(System.err));
	}
	StringBuilder sb = new StringBuilder(4096);
	AppendableWriter w = new AppendableWriter(sb);
	r.transferTo(w);
	w.flush(); w.close();
	State state = State.START;
	String buffer = sb.toString();
	int ind1 = 0;
	int ind2 = 0;
	int msgStart = -1;
	int msgEnd = -1;
	ArrayList<Result> list = new ArrayList<>(256);
	String fromName = null;
	String fromEmail = null;
	String messageID = null;
	boolean quotedPrintable = false;
	for (MatchResult mr: matcher.iterableOver(buffer)) {
	    int index = mr.getIndex();
	    switch(state) {
	    case START:
		if (index == 0) {
		    int sind = mr.getStart();
		    if (sind > 0) {
			if (buffer.charAt(sind-1) != '\n') continue;
		    }

		    state = State.FOUND_FROM;
		    fromName = null;
		    messageID = null;
		    msgStart = -1;
		    msgEnd = -1;
		    int ind = buffer.indexOf("\n", mr.getStart());
		    String s = buffer.substring(mr.getStart()+5, ind)
			.trim();
		    ind = s.indexOf(' ');
		    if (ind == -1) {
			fromEmail = s;
		    } else {
			fromEmail = s.substring(0, ind);
		    }
		}
		break;
	    case FOUND_FROM:
		if (index == 1) {
		    int sind = mr.getStart();
		    if (sind > 0) {
			if (buffer.charAt(sind-1) != '\n') continue;
		    }
		    state = State.FOUND_BEGIN;
		    ind1 = mr.getStart();
		    msgEnd = mr.getEnd() + 1;
		    if (buffer.charAt(msgEnd) == '\r') {
			msgEnd++;
		    }
		} else if (index == 0) {
		    int sind = mr.getStart();
		    if (sind > 0) {
			if (buffer.charAt(sind-1) != '\n') continue;
		    }
		    state = State.FOUND_FROM;
		    fromName = null;
		    messageID = null;
		    int ind = buffer.indexOf("\n", mr.getStart());
		    String s = buffer.substring(mr.getStart()+5, ind)
			.trim();
		    ind = s.indexOf(' ');
		    if (ind == -1) {
			fromEmail = s;
		    } else {
			fromEmail = s.substring(0, ind);
		    }
		} else if (index == 3) {
		    int sind = mr.getStart();
		    if (sind > 0) {
			if (buffer.charAt(sind-1) != '\n') continue;
		    }
		    // found a "From: " header so just record
		    // info for cross checking.
		    int ind = buffer.indexOf("\n", mr.getStart());
		    if (ind != -1) {
			String s = buffer.substring(mr.getStart()+5, ind);
			int obind = s.indexOf('<');
			int cbind = s.indexOf('>');

			if (obind != -1 && cbind != -1) {
			    fromEmail = s.substring(obind+1, cbind).trim();
			    fromName = s.substring(0, obind).trim();
			} else {
			    fromEmail = s.trim();
			}
			// System.out.println("fromName = " + fromName);
		    }
		} else  if (index == 4) {
		    int sind = mr.getStart();
		    if (sind > 0) {
			if (buffer.charAt(sind-1) != '\n') continue;
		    }
		    // found a "Message-ID: " header so record its value
		    int ind = buffer.indexOf("\n", mr.getStart());
		    if (ind != -1  && messageID == null) {
			String s = buffer.substring(mr.getStart()+11, ind);
			int obind = s.indexOf('<');
			int cbind = s.indexOf('>');
			if (obind != -1 && cbind != -1) {
			    messageID = s.substring(obind+1, cbind);
			}
		    }
		} else if (index == 5) {
		    int sind = mr.getStart();
		    if (sind > 0) {
			if (buffer.charAt(sind-1) != '\n') continue;
		    }
		    // found a "Message-ID: " header so record its value
		    int ind = buffer.indexOf("\n", mr.getStart());
		    if (ind != -1) {
			String s = buffer.substring(mr.getEnd(), ind)
			    .trim().toLowerCase(Locale.US);
			quotedPrintable = s.equals("quoted-printable");
		    }
		} else if (msgStart == -1 && (index == 6 || index == 7)) {
		    msgStart = mr.getEnd();
		}
		break;
	    case FOUND_BEGIN:
		if (index == 2) {
		    state = State.FOUND_END;
		    ind2 = mr.getEnd();
		    char ch = buffer.charAt(ind2);
		    if (ch == '\r') ind2++;
		    if (ch == '\n') ind2++;
		    String s = buffer.substring(ind1, ind2);
		    // System.out.println("s.length() = " + s.length());
		    // System.out.println(s);
		    if (quotedPrintable) {
			s = s.replace("=\r\n", "");
			s = s.replace("=3D", "=");
		    }
		    Result result = decode(fromName, fromEmail, messageID,
					   s, ew);
		    if (msgStart == -1) {
			// should never happen, but just in case ...
			result.setStatus(false);
			result.addToReasons("NoMessageBody");
		    } else {
			s = buffer.substring(msgStart, msgEnd);
			if (quotedPrintable) {
			    s = s.replace("=\r\n","");
			    // regularize for a test.
			    s = s.replace("\r\n", "\n");
			}
			s = s.replace("\r\n", "\n").replaceAll("\\s+", " ")
			    .trim();
			int textEnd = ind2;
			HeaderOps hdrs = result.getHeaders();
			String nm = hdrs.getFirst("acceptedBy");
			String md = hdrs.getFirst("digest");
			String document = hdrs.getFirst("document"); 
			String type = hdrs.getFirst("type");
			if (quotedPrintable) {
			    nm = pqEncode(nm);
			    type = pqEncode(type);
			    document = pqEncode(document);
			}
			KeyMap keymap = new KeyMap();
			keymap.put("name", nm);
			keymap.put("type", type);
			keymap.put("document", document);
			keymap.put("digest", md);
			keymap.put("PEM", PEM_START);
			TemplateProcessor tp = new TemplateProcessor(keymap);
			r = new
			    InputStreamReader(DocsigVerifier.class
					      .getResourceAsStream("email.tpl"),
					      UTF8);
			StringBuilder msg = new StringBuilder(1024);
			AppendableWriter aw = new AppendableWriter(msg);
			tp.processTemplate(r, aw);
			aw.flush(); r.close(); aw.close();
			
			String desired = msg.toString().replace("\r\n","\n")
			    .replaceAll("\\s+", " ").trim();
			if (!s.equals(desired)) {
			    result.setStatus(false);
			    result.addToReasons("ModifiedEmail");
			}
		    }
		    list.add(result);
		} else {
		    // something is wrong, so reset
		    state = State.START;
		}
		break;
	    case FOUND_END:
		if (index == 0) {
		    int sind = mr.getStart();
		    if (sind > 0) {
			if (buffer.charAt(sind-1) != '\n') continue;
		    }
		    state = State.FOUND_FROM;
		    fromName = null;
		    messageID = null;
		    int ind = buffer.indexOf("\n", mr.getStart());
		    String s = buffer.substring(mr.getStart()+5, ind)
			.trim();
		    ind = s.indexOf(' ');
		    if (ind == -1) {
			fromEmail = s;
		    } else {
			fromEmail = s.substring(0, ind);
		    }
		}
	    }
	}
	Result[] array = new Result[list.size()];
	return list.toArray(array);
    }

    /**
     * Decode the contents of a DOCSIG message obtained from a {@link Reader}.
     * @param r a {@link Reader} that provides the message
     * @return a {@link Result} describing the status of
     *         a signature request and the values of various headers
     * @throws IOException if an IO error occurred
     */
    public static Result decodeFrom(Reader r)
	throws IOException
    {
	return decodeFrom(r, null);
    }

    /**
     * Decode the contents of a DOCSIG message obtained from a {@link Reader},
     * also providing a {@link PrintWriter} for error messages.
     * @param r a {@link java.io.Reader} used to read an mbox file
     * @param ew a {@link java.io.PrintWriter} used to record error messages
     * @return a {@link Result} describing the status of
     *         a signature request and the values of various headers
     * @throws IOException if an IO error occurred
     */
    public static Result decodeFrom(Reader r, PrintWriter ew)
	throws IOException
    {
	StringBuilder sb = new StringBuilder(4096);
	AppendableWriter w = new AppendableWriter(sb);
	r.transferTo(w);
	w.flush(); w.close();
	String s = sb.toString();
	return decodeFrom(s, ew);
    }

    /**
     * Decode the contents of a DOCSIG message obtained from a {@link String}.
     * @param s the string containing the contents of a DOCSIG message
     * @return a {@link Result} describing the status of
     *         a signature request and the values of various headers
     * @throws IOException if an IO error occurred
     */
    public static Result decodeFrom(String s)
	throws IOException
    {
	return decodeFrom(s, null);
    }

    /**
     * Decode the contents of a DOCSIG message obtained from a {@link String},
     * also providing a {@link PrintWriter} for error messages.
     * @param s the string containing the contents of a DOCSIG message
     * @param ew a {@link PrintWriter} used to record error messages
     * @return a {@link Result} describing the status of
     *         a signature request and the values of various headers
     * @throws IOException if an IO error occurred
     */
    public static Result decodeFrom(String s, PrintWriter ew)
	throws IOException
    {
	return decode(null, null, null, s, ew);
    }
}

//  LocalWords:  DOCSIG mbox UTF SHA Sha withECDSA SunEC acceptedBy
//  LocalWords:  ipaddr transID sendto publicKeyID  HeaderOps
//  LocalWords:  getFirst messageID PEM hasKeyRequest emailName ew
//  LocalWords:  emailAddr PrintWriter IOException fromName
