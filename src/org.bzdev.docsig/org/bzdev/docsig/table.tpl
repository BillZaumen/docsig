<!DOCTYPE html>
<HTML lang="en">
  <HEAD>
    <META charset="UTF-8">
    <META name="viewport" content="width=device-width, initial-scale=1.0">
    <STYLE type="text/css">
      BODY {
        background-color: $(bgcolor);
        color: $(color);
        margin: 2em;
      }
      INPUT {
        background-color:$(inputBG);
        color: $(inputColor);
	accent-color: $(accentColor);
      }
      INPUT[type=file]::file-selector-button {
	  background-color: $(inputBG);
	  color: $(inputColor);
      }
      TEXTAREA {
        background-color:$(inputBG);
        color: $(inputColor);
      }
    </STYLE>
    <TITLE>Table Request</TITLE>
  </HEAD>
  <BODY>
    <H1>Table Request</H1>
    <P>
      Fill in the following form to get a table in CSV (Comma Separated Values)
      format:
      <br>
      <FORM action="$(url)" method="post" enctype="multipart/form-data">
	<br>
	MBOX File: 
	<INPUT name="mbox" type="file" placeholder="file name" width="64">
	<br><br>
	<INPUT name="showHeadings" type="checkbox" id="chbox" value="true">
	<LABEL for="chbox">Show Headings</LABEL>
	<br><br>
	Expected-Document URL: <INPUT name="expectedDocument" width="64"> <br>
	Expected-Document Digest: <INPUT name="expectedDigest" width="64"> <br>
	Expected-Server URL: <INPUT name="expectedServer" width="64"> <br>
	<br><br>Include:
	<BLOCKQUOTE>
	  <INPUT name="mode" type="radio" value="all" id="r1">
	  <LABEL for="r1">All Results</LABEL><br><br>
	  <INPUT name="mode" type="radio" value="valid" id="r2">
	  <LABEL for="r2">Valid Results</LABEL><br><br>
	  <INPUT name="mode" type="radio" value="invalid" id="r3">
	  <LABEL for="r2">Invalid Results</LABEL>
	</BLOCKQUOTE><br>
	Column Names: <br>
	<TEXTAREA name="columns" rows="10" cols="64"></TEXTAREA><br><br>
	<INPUT type="submit" value="Create Table"
	       style="font-size: 150%">
      </FORM><BR>
    <P>
      Column names are comma-separated. The columns that may be filled in
      are those whose names, are
      <UL>
	<li><STRONG>acceptedBy</STRONG>. The name of the person signing
	  the document.
	<li><STRONG>timestamp</STRONG>. The time the docuemnt signature was
	  generated.
	<li><STRONG>date</STRONG>. The date the documentsignature was
	  generated.
	<li><STRONG>timezone</STRONG>. THe timezone used for the date.
	<li><STRONG>ipaddr</STRONG>. The remote IP address the server used
	  when the document was signed.
	<li><STRONG>id</STRONG>. The ID of the person signing the document
	  (e.g., a member ID or some other number assigned to the person
	  signing the document). This field is optional.
	<li><STRONG>transID</STRONG>. A transaction ID for the document
	  signature request.  This field is optional.
	<li><STRONG>email</STRONG>. The email address of the person signing
	  the document as entered by that person.
	<li><STRONG>server</STRONG>. The URL of the DOCSIG server.
	<li><STRONG>sendto</STRONG>. The address to which the signature
	  was sent.
	<li><STRONG>cc</STRONG>. The address of an alternate recipient.
	  This field is optional.
	<li><STRONG>document</STRONG>. The URL of the document being signed.
	<li><STRONG>type</STRONG>. The type of the document (for example,
	  "document", or "waiver").
	<li><STRONG>digest</STRONG>. The SHA-256 message digest of the
	  document being signed.
	<li><STRONG>publieKeyID</STRONG>. The public-key id for the key
	  used by the server when signing a message.
	<li><STRONG>signature</STRONG>. The digital signature of the
	  fields listed above.
	<li><STRONG>from</STRONG>. The email address that appears in a
	  message&apos;s <STRONG>from</STRONG> field.  This should be the
	  same as that for <STRONG>email</STRONG> above.
	<li><STRONG>message-id</STRONG>. The message ID for the message
	  containing the signature.
	<li><STRONG>status</STRONG>. The status of the message (true for
	  valid and false if not valid).
	<li><STRONG>reasons</STRONG>. A string giving reasons when the
	  status is <STRONG>false</STRONG>.
      </UL>
      These names are case sensitive. For other names, the column will be
      left blank.  In practice, only a few of these will be used, but
      all are available.
  </BODY>
</HTML>
