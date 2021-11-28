/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.inbound.endpoint.protocol.httpws.management;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.commons.handlers.MessagingHandler;
import org.apache.synapse.inbound.InboundProcessorParams;
import org.apache.synapse.transport.netty.api.HttpWebSocketInboundEndpointHandler;
import org.wso2.carbon.inbound.endpoint.common.AbstractInboundEndpointManager;
import org.wso2.carbon.inbound.endpoint.inboundfactory.InboundRequestProcessorFactoryImpl;
import org.wso2.carbon.inbound.endpoint.osgi.service.ServiceReferenceHolder;

import java.net.InetSocketAddress;
import java.util.List;

import static org.wso2.carbon.inbound.endpoint.common.Constants.SUPER_TENANT_DOMAIN_NAME;

public class HttpWebsocketEndpointManager extends AbstractInboundEndpointManager {

    private static final Log LOGGER = LogFactory.getLog(HttpWebsocketEndpointManager.class);

    private static HttpWebsocketEndpointManager instance = null;

    public static HttpWebsocketEndpointManager getInstance() {

        if (instance == null) {
            instance = new HttpWebsocketEndpointManager();
        }
        return instance;
    }

    @Override
    public boolean startListener(int port, String name, InboundProcessorParams inboundParameters) {

        if (!HttpWebSocketInboundEndpointHandler.isPortAvailable(port)) {
            LOGGER.error("A service is already listening on port " + port
                    + ". Please select a different port for this endpoint.");
            return false;
        }

        ConfigurationContext configurationContext = ServiceReferenceHolder.getInstance().
                getConfigurationContextService().getServerConfigContext();
        List<MessagingHandler> inboundEndpointHandlers = inboundParameters.getHandlers();

        return HttpWebSocketInboundEndpointHandler.startEndpoint(new InetSocketAddress(port), configurationContext,
                inboundEndpointHandlers, name);
    }

    @Override
    public boolean startEndpoint(int port, String name, InboundProcessorParams inboundParameters) {

        String epName = dataStore.getListeningEndpointName(port, SUPER_TENANT_DOMAIN_NAME);
        if (epName != null) {
            if (epName.equalsIgnoreCase(name)) {
                LOGGER.info(epName + " Endpoint is already started in port : " + port);
            } else {
                String msg = "Another endpoint named : " + epName + " is currently using this port: " + port;
                LOGGER.warn(msg);
                throw new SynapseException(msg);
            }
        } else {
            dataStore.registerListeningEndpoint(port, SUPER_TENANT_DOMAIN_NAME,
                    InboundRequestProcessorFactoryImpl.Protocols.httpws.toString(), name, inboundParameters);
            boolean start = startListener(port, name, inboundParameters);

            if (!start) {
                dataStore.unregisterListeningEndpoint(port, SUPER_TENANT_DOMAIN_NAME);
                return false;
            }
        }
        return true;
    }

    @Override
    public void closeEndpoint(int port) {
        dataStore.unregisterListeningEndpoint(port, SUPER_TENANT_DOMAIN_NAME);
    }
}
