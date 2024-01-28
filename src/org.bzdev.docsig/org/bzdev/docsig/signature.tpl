<!DOCTYPE html>
<html lang="en">
  <head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <style type="text/css">
    BODY {
        background-color: $(bgcolor);
        color: $(color);
        margin: 2em;
    }
    A {color: $(linkColor);}
    A:link {color:$(linkColor);}
    A:visited {color:$(visitedColor);}
  </style>
  <title>Signature Request</title>
  </head>
  <body>
    <P> This page was produced by $(sigserver) --- the following
      link will display the $(type) to be signed:
      <BLOCKQUOTE>
<A HREF="$(document)">$(document)</A>
      </BLOCKQUOTE>
	and its SHA-256 message digest, is
      <BLOCKQUOTE>
<A HREF="$(sigserver)?url=$(encDocument)&digest=$(digest)">$(digest)</A>
      </BLOCKQUOTE>
    <P>
      To sign the $(type), click the following button. This should open an
      email window with everything filled out.  If possible, please send it
      with a GPG or PGP signature and also compute the SHA-256 digest
      independently. The submission will not be valid if the
      text of the email is modified.
    <P><br>
    <A href="mailto:$(sendto)?$(query)"
       style="background-color:$(buttonBGColor);color:$(buttonFGColor); padding: 14px 25px; text-align: center; text-decoration: none;">Click To Send</A>
    <P><br>
      If your system cannot send email in this way, send an email
      to <SPAN STYLE="background-color: $(bquoteBGColor)">$(sendto)</SPAN>
      with a subject line
      "<SPAN STYLE="background-color: $(bquoteBGColor)">$(subject)</SPAN>"
      and a message  containig the following text:
    <P>
      <PRE>
+++++++++++++++++++++++
</PRE><PRE STYLE="background-color: $(bquoteBGColor)">
$(body)</PRE><PRE>
+++++++++++++++++++++++
      </PRE>
    <P>
  </body>
</html>
