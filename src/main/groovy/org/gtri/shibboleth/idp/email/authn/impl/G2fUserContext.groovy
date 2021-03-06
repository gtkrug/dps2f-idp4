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

import groovy.util.logging.Slf4j
import org.opensaml.messaging.context.BaseContext

@Slf4j
class G2fUserContext extends BaseContext {
    def appId
    def username
    def email
    def text
    def key
    def state
    def tokenResponse
    def token
    def initialized = false

    def getAppId() {
        return appId
    }

    def getUsername() {
        return username
    }

    def setUsername(String username) {
        this.username = username;
    }

    def getEmail() {
        return email
    }

    def setEmail(String userEmail) {
        email = userEmail;
    }

    def getTextAddress() {
        return text
    }

    def setTextAddress(String textAddr) {
        text = textAddr
    }


    /**
     * Get current error and display it to the user.
     *
     * @return an error description
     */

    def getErrorMessage() {
        log.debug ("g2fUserContext.state = {} username = {} email = {} text = {}", state, username, email, text)
        return state
    }
}
