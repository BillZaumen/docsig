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
    INPUT {
        background-color:$(inputBG);
        color: $(inputColor);
    }
  </style>
    <SCRIPT>
      function doResize() {
          var height = window.innerHeight;
          if (height < 300) height = 300;
          var contentHeight = height / 2;
          var frame = document.getElementById("frame");
          frame.height = contentHeight;
      }
      window.addEventListener("resize", doResize);
    </SCRIPT>
    <title>Signature Request</title>
  </head>
  <body onload="doResize()">
    <H1>Signature Request</H1>
    <P> To sign the following <A HREF="$(document)">$(type)</A>
      (you may have to scroll the $(type) to see all of it)
    <P>
      <IFRAME ID="frame" SRC="$(documentURL)" scrolling="auto"
	      width="95%"
	      style="border:3px solid $(borderColor);background-color:$(bquoteBGColor)">
	Visit <A HREF="$(document)">$(document)</A> to read the $(type).
    </IFRAME>
    <P>
      (SHA-256: $(digest))
    <P>
      please fill in or check the following information and click on
      the "Continue" button if everything is OK.  You will get a link
      to the $(type) with a digitally generated signature that you
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
