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
    INPUT {
        background-color:$(inputBG);
        color: $(inputColor);
    }
  </style>
    <title>Signature Request</title>
  </head>
  <body>
    <H1>Signature Request</H1>
    <P>
      Please fill in or check the following information and click on
      the "Continue" button if everything is OK.  You will get a link
      to the document with a digitally generated signature that you
      can submit.
    <P>
      <form action="https://localhost/docsig/" method="post">
	Name: $(+name:endName)$(name)
	<input name="name" type="hidden" value="$(name)">$(endName)
	$(-name:endName)
	<input name="name" type="text" placeholder="Your Name" width="48">
	$(endName)<br><br>
	Email: $(+email:endEmail)$(email)
	<input name="email" type="hidden" value="$(email)">$(endEmail)
	$(-email:endEmail)
	<input name="email" type = "text" placeholder="Your Email Address"
	       width="48">$(endEmail)<br><br>
	$(+id:endID)
	ID: $(id)<br><br>
	<input name="id" type="hidden" value="$(id)" width="48">$(endID)
	$(+transid:endTransid)
	<input name="transID" type="hidden" value="$(transid)" width="48">
	$(endTransid)
	<input name="sigserver" type="hidden"
	       value="$(sigserver)">
	<input name="type" type="hidden" value="$(type)">
	<input name="document" type="hidden"
	       value="$(document)">
	<input name="sendto" type="hidden"
	       value="$(sendto)">
	$(+cc:endCC)
	   <input name="cc" type ="hidden" value="$(cc)"> $(endCC)
	<input name="subject" type="hidden" value="$(subject)">
	<input type="submit" value="Continue"
	       style="font-size: 150%">
      </form>
    <P>
      If you are using an extension such as NoScript, you may have to
      enable scripts for this browser tab.
  </body>
</html>
