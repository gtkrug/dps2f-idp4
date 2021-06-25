/*
 * Copyright 2016 Stefan Wold <ratler@stderr.eu>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.gtri.shibboleth.idp.email.authn.impl.datastores

import org.gtri.shibboleth.idp.email.authn.api.DeviceDataStore
import org.gtri.shibboleth.idp.email.authn.impl.G2fUserContext
import groovy.util.logging.Slf4j
import net.shibboleth.idp.authn.AuthnEventIds
import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.protocol.HttpContext

// For emailing
import java.util.*
import javax.mail.*
import javax.mail.internet.*

// For files
import java.nio.file.Files;
import java.nio.file.Path;

// Mailserver Stuff
// TBD


@Slf4j
class MailServerDataStore implements DeviceDataStore {
    private String Server
    private String Address
    private String EmailTemplate
    private String TextTemplate
    
    // TBD - Private Data Neccessary for Connection to SMTP Server

    /** Constructor */
    MailServerDataStore(String smtpServer, String fromAddress, String emailTemplate, String textTemplate) {
        log.debug("Mail Server constructor: ({})", smtpServer);
        try {
            Server  = smtpServer;
            Address = fromAddress;
            EmailTemplate = Files.readString(Path.of(emailTemplate));
            TextTemplate  = Files.readString(Path.of(textTemplate));
        } catch (Exception ex) {
           log.error("Failed to load email or text template from file;  Exception: {}",  ex.toString() );
        }

    }

    @Override
    def beginAuthentication(G2fUserContext g2fUserContext) {
        def username  = g2fUserContext.username
        def token     = g2fUserContext.token
        def email     = g2fUserContext.email
        def textaddr  = g2fUserContext.text

        log.debug("Send e-mail to e-mail address {} for user {} with token {}", email, username, token)
        try {
            log.debug("beginAuthentication() - Transmitting 2nd factor to user via e-mail");
            sendEmail (email, token, EmailTemplate, new String("Texas DPS Login: One Time Pin"));
        } catch (Exception e) {
            log.error("E-mail Transmission error: {}", e.toString());
            return false
        }
        if ( textaddr != null ) {
          log.debug("Send e-mail to text address {} for user {} with token {}", textaddr, username, token)
           try {
               log.debug("beginAuthentication() - Transmitting 2nd factor to user via e-mail");
               sendEmail (textaddr, token, TextTemplate, new String(""));
            } catch (Exception e) {
               log.error("E-mail Transmission error: {}", e.toString());
               return false
            }
         }

        return true
    }

    @Override
    boolean finishAuthentication(G2fUserContext g2fUserContext) {

        try {
          def username  = g2fUserContext.username
          int tokenResp = g2fUserContext.tokenResponse.toInteger()
          int token     = g2fUserContext.token.toInteger()
          log.debug("finishAuthentication() ")

          if (tokenResp == 10) {
            log.warn("No token input.  Sending a new token.")
            return false;
          }

          log.debug("Verifying token matches for user {}; token sent was {} with response {}", username, token, tokenResp)

          if ( tokenResp == token ) {
            log.debug ("Entered Valid 2nd Factor");
            return true;
          } else {
            log.warn ("Entered Invalid 2nd Factor - Requesting Again");
            return false;
         }
       } catch (Exception e) {
         log.error ("Exception when trying to validate 2nd Factor.  Possibly input was not a number.  {}", e)
         return false;
       }
    }

    private void sendEmail (String EmailAddress, int Token, String Message, String Subject) throws MessagingException {



        Properties properties = System.getProperties();

        Integer token  = new Integer(Token);
        String msgText = Message.replaceAll ("TOKEN", token.toString());

        // Setup mail server
        properties.setProperty("mail.smtp.host", Server);

        // Get the default Session object.
        Session session = Session.getDefaultInstance(properties);

        // Create a default MimeMessage object.
        MimeMessage message = new MimeMessage(session);

        // Set From: header field of the header.
        message.setFrom(new InternetAddress(Address));

        // Set To: header field of the header.
        message.setRecipients(Message.RecipientType.TO,EmailAddress);

        // Set Subject: header field
        if (! Subject.isEmpty() ) {
          message.setSubject(Subject);
        }

        // Now set the actual message
        //message.setText(msgText);
	//PB- using setConent fot http body
	message.setContent(msgText, "text/html; charset=utf-8");

        // Send message
        log.debug ("Trying to send email to ({}) with server ({}) from ({})", EmailAddress, Server, Address);

        Transport.send(message);

        log.debug ("Successfully e-mailed {} one time token to {}", EmailAddress);
   }

}
