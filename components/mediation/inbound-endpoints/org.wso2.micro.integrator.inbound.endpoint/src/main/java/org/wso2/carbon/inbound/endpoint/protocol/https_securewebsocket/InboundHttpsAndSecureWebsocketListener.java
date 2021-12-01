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
package org.wso2.carbon.inbound.endpoint.protocol.https_securewebsocket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.inbound.InboundProcessorParams;
import org.wso2.carbon.inbound.endpoint.protocol.http_websocket.InboundHttpAndWebsocketListener;
import org.wso2.carbon.inbound.endpoint.protocol.http_websocket.management.HttpWebsocketEndpointManager;

public class InboundHttpsAndSecureWebsocketListener extends InboundHttpAndWebsocketListener {

    private static final Log LOGGER = LogFactory.getLog(InboundHttpsAndSecureWebsocketListener.class);

    public InboundHttpsAndSecureWebsocketListener(InboundProcessorParams params) {

        super(params);
    }

    @Override
    public void init() {
        if (HttpWebsocketEndpointManager.isPortOccupied(port)) {
            LOGGER.warn("Port " + port + "used by inbound endpoint " + name + " is already used by another "
                    + "application. Hence, undeploying the inbound endpoint");
            throw new SynapseException("Port " + port + " used by inbound endpoint " + name + " is already used by "
                    + "another application.");
        }
        HttpWebsocketEndpointManager.getInstance().startSSLEndpoint(port, name, processorParams);
    }
}
