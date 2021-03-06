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

package org.gtri.shibboleth.idp.email.authn.impl

import java.util.function.Function
import org.gtri.shibboleth.idp.email.authn.api.DeviceDataStore
import groovy.util.logging.Slf4j
import net.shibboleth.idp.authn.AbstractExtractionAction
import net.shibboleth.idp.authn.AuthnEventIds
import net.shibboleth.idp.authn.context.AuthenticationContext
import net.shibboleth.idp.authn.context.UsernamePasswordContext
import net.shibboleth.idp.profile.ActionSupport
import net.shibboleth.idp.session.context.navigate.CanonicalUsernameLookupStrategy
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty
import org.opensaml.profile.context.ProfileRequestContext
import org.opensaml.soap.wssecurity.Username
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.webflow.execution.Event

import javax.annotation.Nonnull
import javax.annotation.Nullable

@Slf4j
public class Initialize2ndFactor extends AbstractExtractionAction {

    @NotEmpty
    private G2fUserContext g2fUserContext

    /** Attempted username. */
    @Nullable
    @NotEmpty
    private String username

    /** G2F application ID */
    @Value('%{g2f.appId}')
    private final String appId

    /** Spring injected device data store */
    @Autowired
    @Qualifier('deviceDataStore')
    private DeviceDataStore dataStore

    /** Lookup strategy for username to match against g2f identity. */
    @Autowired
    @Qualifier('CanonicalUsernameStrategy')
    @Nonnull
    private Function<ProfileRequestContext,String> usernameLookupStrategy

    Initialize2ndFactor() {
        super()
        usernameLookupStrategy = new CanonicalUsernameLookupStrategy()
    }

    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
                                   @Nonnull final AuthenticationContext authenticationContext) {
        g2fUserContext = authenticationContext.getSubcontext(G2fUserContext.class, true)
        username = usernameLookupStrategy.apply(profileRequestContext)
        if (!username) {
            log.warn("No principal name available to cross-check G2F result")
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_CREDENTIALS)
            return false
        }
        if ( g2fUserContext.text == null ) { 
           log.debug ("Initailzie Pre-execute: g2fUserContext Initialized with email = {}", g2fUserContext.email);
        } else {
           log.debug ("Initailzie Pre-execute: g2fUserContext Initialized with email = {} and text = {}", g2fUserContext.email, g2fUserContext.text);
        }
        return true
    }

    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
                             @Nonnull final AuthenticationContext authenticationContext) {
        log.debug("${logPrefix} Entering doExecute")

        try {
            log.debug("Principal name {}", username)

            if (!g2fUserContext.initialized) {
                log.info ("User Context for {} was not marked as initialized. ", username);
                g2fUserContext.username = username
                g2fUserContext.appId = appId
                g2fUserContext.state = ""   
                g2fUserContext.initialized = true
            } else {
                log.warn ("User Context for {} already exists; perhaps a failed login: {}", username, g2fUserContext.state);
            }
            g2fUserContext.token = (int)100000 + (int)(Math.random() * 900000);  // Generates a random 6 digit number between 100,000 and 999,999
            def res = dataStore.beginAuthentication(g2fUserContext)
            if (!res) {
                def state = g2fUserContext.state
                log.debug("beginAuthentication() failed with state: {}", state)
                // Reset state
                g2fUserContext.state = ""
                ActionSupport.buildEvent(profileRequestContext, (String) state)
            }
        } catch (Exception errMail) {
            log.error("Exception when attempting to send 2nd factor: {}", errMail );
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_AUTHN_CTX)
        }
    }

}
