/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.micro.integrator.management.apis;

import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.SynapseConfiguration;
import org.wso2.micro.integrator.management.apis.security.handler.AuthConstants;
import org.wso2.micro.integrator.management.apis.security.handler.JWTInMemoryTokenStore;
import org.wso2.micro.integrator.management.apis.security.handler.SecurityUtils;

import java.util.HashSet;
import java.util.Set;

public class LogoutResource implements MiApiResource{

    private static final Log LOG = LogFactory.getLog(LoginResource.class);

    // HTTP method types supported by the resource
    Set<String> methods;

    public LogoutResource() {
        methods = new HashSet<>();
        methods.add(Constants.HTTP_POST);
        methods.add(Constants.HTTP_GET);
    }

    @Override
    public Set<String> getMethods() {
        return methods;
    }

    @Override
    public boolean invoke(MessageContext messageContext, org.apache.axis2.context.MessageContext axis2MessageContext, SynapseConfiguration synapseConfiguration) {
        String authHeader = (String)SecurityUtils.getHeaders(axis2MessageContext).get(HTTPConstants.HEADER_AUTHORIZATION);
        String token = authHeader.substring(AuthConstants.BEARER_AUTH_HEADER_TOKEN_TYPE.length() + 1).trim();
        //Revokes token when logging out.
        if(!JWTInMemoryTokenStore.getInstance().revokeToken(token)) {
            LOG.info("Log out failed");
            handleServerError(axis2MessageContext, "Log out failed due to incorrect credentials");
            return true;
        }
        return true;
    }

    /**
     * Generate and sets error json response
     * @param axis2MessageContext msg ctx
     * @param errorDetail Error string
     */
    private void handleServerError(org.apache.axis2.context.MessageContext axis2MessageContext, String errorDetail) {
        Utils.setJsonPayLoad(axis2MessageContext, Utils.createJsonErrorObject(errorDetail));
        axis2MessageContext.setProperty(Constants.HTTP_STATUS_CODE, Constants.INTERNAL_SERVER_ERROR);
        axis2MessageContext.removeProperty(Constants.NO_ENTITY_BODY);
    }
}
