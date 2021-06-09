> This project is work in progress.

# Shibboleth IDP 4.1, e-mail code based 2nd factor.
This is an e-mail code second factor authentication flow for Shibboleth Identity Provider v4.1.x. 
It was derived from the [G2F Flow](https://github.com/gtkrug/shib-g2f).  

## Notes
Tested with Shibboleth Identity Provider 4.1.x and Google Chrome and Firefox

## Requirements
* [Shibboleth Identity Provider 4.1.x](http://shibboleth.net/downloads/identity-provider/latest/)
* Java 11

## Build
Build the library by executing **./gradlew clean installDist** from the project directory.  This will build 1 library 
and copy a few other dependencies into a single directory **./build/install/dps-2f-code/edit-webapp/WEB-INF/lib/**.  

There is a zipfile included in the repository that represents a build based on the date listed.

## Installation
The contents of the **./build/install/dps-2f-code/** directory in general need to be copied to a deployed IDP 4.1, and 
then added to the idp.war file by executing **[IDP_HOME]/bin/build.sh**.

## Configuration
There are several configuration files that need to be added/updated based on the examples/distribution files for this package:

* **conf/authn/authn.properties** - This needs to be updated to specify authentication via MFA.
* **conf/authn/g2f.properties** - This file needs to be in this directory (it will automatically load if it is), and it will need to be updated to speicfy a mailserver, a default email template, and default sms template. Within both templates **TOKEN** will be replaced with the generated 6-digit token.
* **conf/authn/mfa-authn-config.xml** - Update based on the build's version, which includes Javascript code to intelligently determine if a 2nd factor is required and to update the user session with data needed to execute the 2nd factor.  Depending on what attribute has the user's email address and sms address, this file may also need to be updated to properly query for it. If sending to multiple email or sms addresses, the string should be a comma delimited list of email addresses.
* **conf/attribute-resolver.xml** - This file needs to be updated based on the example, specifically to support a new attribute for resolving the user's access type.  If using a customized email address for the token to be sent to, that attribute for that address/addresses will also need to be defined.
* **conf/ip.js** - This file can be placed anywhere as the path to it is specified in attribute-resolver.xml.  It is a sample javascript for determining if a user is on an internal network or external network.  It should be customized appropriately.  Be aware that the surrounding environment (apache/tomcat/proxies/load balancers) must be very carefully configured to support the methodolgy of remote address determination used by this javascript file.  
* **views/g2f.vm** - This is the velocity template for entering the 2nd factor code.  It should be updated in terms of CSS and graphics to match the look & feel of the login page.  There are some new CSRF required fields, so this cannot be overwritten by a 3.x velocity template.

