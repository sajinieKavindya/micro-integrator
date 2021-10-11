package org.wso2.carbon.inbound.endpoint.protocol.httpws;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.inbound.InboundProcessorParams;
import org.apache.synapse.inbound.InboundRequestProcessor;
import org.wso2.carbon.inbound.endpoint.protocol.httpws.management.HttpWebsocketEndpointManager;

public class InboundHttpWebsocketListener implements InboundRequestProcessor {

    private static final Log LOGGER = LogFactory.getLog(InboundHttpWebsocketListener.class);

    private final String name;
    private int port;
    private final InboundProcessorParams processorParams;

    public InboundHttpWebsocketListener(InboundProcessorParams params) {
        processorParams = params;
        String portParam = params.getProperties().getProperty("port");
        try {
            port = Integer.parseInt(portParam);
        } catch (NumberFormatException e) {
            handleException("Validation failed for the port parameter " + portParam, e);
        }
        name = params.getName();
    }

    @Override
    public void init() {
        HttpWebsocketEndpointManager.getInstance().startEndpoint(port, name, processorParams);
    }

    @Override
    public void destroy() {

    }

    protected void handleException(String msg, Exception e) {
        LOGGER.error(msg, e);
        throw new SynapseException(msg, e);
    }
}
