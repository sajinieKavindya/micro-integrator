package org.wso2.carbon.inbound.endpoint.protocol.httpws.management;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.transport.TransportListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.inbound.InboundProcessorParams;
import org.apache.synapse.transport.netty.listener.Axis2HttpTransportListener;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.apache.synapse.transport.passthru.api.PassThroughInboundEndpointHandler;
import org.wso2.carbon.inbound.endpoint.common.AbstractInboundEndpointManager;
import org.wso2.carbon.inbound.endpoint.osgi.service.ServiceReferenceHolder;
import org.wso2.carbon.inbound.endpoint.protocol.websocket.InboundWebsocketConstants;

import java.util.concurrent.ConcurrentHashMap;

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
        TransportListener httpTransportListener = new Axis2HttpTransportListener();
        ConfigurationContext configurationContext = ServiceReferenceHolder.getInstance().
                getConfigurationContextService().getServerConfigContext();
        TransportInDescription transportInDescription = new TransportInDescription("http");
        try {
            Parameter parameter = new Parameter();
            parameter.setName("port");
            parameter.setValue(port);
            transportInDescription.addParameter(parameter);

            Parameter parameter2 = new Parameter();
            parameter2.setName(NhttpConstants.HTTP_GET_PROCESSOR);
            parameter2.setValue(inboundParameters.getProperties().getProperty(NhttpConstants.HTTP_GET_PROCESSOR));
            transportInDescription.addParameter(parameter2);

            httpTransportListener.init(configurationContext, transportInDescription);
        } catch (AxisFault e) {
            LOGGER.error("Couldn't initialize the " + transportInDescription.getName() + "transport listener", e);
        }
        return false;
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
            dataStore.registerListeningEndpoint(port, SUPER_TENANT_DOMAIN_NAME, InboundWebsocketConstants.WS, name,
                    inboundParameters);
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
