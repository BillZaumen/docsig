
# DOCSIG&mdash;a server for signing simple documents

DOCSIG consists of a web server and some processing
software for creating signed documents.  The server
can

- provide documentation.

- generate an HTML page for submitting a signature.

- provide data useful for verifying signatures.

- provide JAR files for reading and verifying email.

The signatures are sent via email but do not directly
include the document that is being signed: instead
the email contains a link to the document, the
document's SHA-256 message digest, and some PEM-encoded
data used for verifying that the email's contents have
not been modified and were created with the designated
DOCSIG server. Email from major email services currently
add headers for DKIM, SPF, and DMARC that can show with
a high degree of reliability that an email was sent from
the designated account, so the use of email allows one
to show via an independent party that the email was almost
certainly sent by a particular individual.

For verification, a user is expected to save the emails
that have been received in MBOX format.  Software provided
with DOCSIG can then read this format, and provide outputs
in various formats, including CSV (Comma Separated Values),
which can be imported by various spreadsheet and database
software.

A typical use case for DOCSIG is one in which a small group
or organization is required by an insurance company or other
party to have waivers signed yearly, even though there have
been no history of lawsuits. Typically, a second web site
(or email) will provide an initial HTML document containing
an HTML form. When submitted to a DOCSIG server, the server
will respond with a link that contains links to the original
document, that document's SHA-256 digest, and a button that
will open the user's email application with a compose window
containing the signature to send.

There is a Docker image on Docker Hub that provides a DOCSIG server:
[wtzbzdev/docsig](https://hub.docker.com/r/wtzbzdev/docsig).

