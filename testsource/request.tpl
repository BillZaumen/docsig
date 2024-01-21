<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <style type="text/css">
    BODY {
        background-color: rgb(10,10,25);
        color: rgb(255,255,255);
        margin: 2em;
    }
    INPUT {
        background-color: rgb(10,10,64);
        color: rgb(255,255,255);
    }
  </style>
    <title>Signature Request</title>
  </head>
  <body>
    <H1>Signature Request</H1>
    <P>
      Please fill in the following information and click on the
      "Continue" button.  You will get a link to the document with
      a digitally generated signature that you can submit.
    <P>
      <form action="http://HOST/docsig/" method="post">
	Name:
	<input name="name" type="text" placeholder="Your Name" width="48">
	<br><br>
	Email:
	<input name="email" type = "text" placeholder="Your Email Address"
	       width="48"><br><br>
	ID: 1234<br><br>
	<input name="id" type="hidden" value="1234" width="48">
	<input name="transID" type="hidden" value="123456789" width="48">
	<input name="sigserver" type="hidden"
	       value="http://HOST:/docsig/">
	<input name="type" type="hidden" value="document">
	<input name="document" type="hidden"
	       value="http://HOST:8080/document.txt">
	<input name="sendto" type="hidden"
	       value="SENDTO">
	<!-- <input name="cc" type ="hidden" value="CC"> -->
	<input name="subject" type="hidden" value="Document Signature">
	<input type="submit" value="Continue"
	       style="font-size: 150%">
      </form>
    <P>
      If you are using an extension such as NoScript, you may have to
      enable scripts for this browser tab.
  </body>
</html>
