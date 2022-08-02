package org.wso2.micro.integrator.inbound.websocket.transport.listener;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.inbound.InboundEndpoint;
import org.wso2.carbon.inbound.endpoint.protocol.httpwebsocket.management.HttpWebsocketEndpointManager;
import org.wso2.carbon.inbound.endpoint.protocol.websocket.management.WebsocketEndpointManager;
import org.wso2.micro.integrator.inbound.websocket.transport.utils.MessageUtil;
import org.wso2.micro.integrator.inbound.websocket.transport.utils.WebSocketConstants;
import org.wso2.micro.integrator.inbound.websocket.transport.utils.WebSocketUtil;
import org.wso2.transport.http.netty.contract.websocket.*;
import org.wso2.transport.http.netty.message.HttpCarbonRequest;

import java.util.ArrayList;
import java.util.List;

public class WebSocketEventDispatcher {

    private static final Log LOG = LogFactory.getLog(WebSocketEventDispatcher.class);

    private static List<String> contentTypes = new ArrayList<>();
    private static List<String> subProtocols = new ArrayList<>();

    static {
        contentTypes.add("application/xml");
        contentTypes.add("application/json");
        contentTypes.add("text/xml");
        addContentTypesToSubProtocolArray(subProtocols, contentTypes);
    }

    public static void dispatchUpgrade(WebSocketHandshaker webSocketHandshaker, MessageContext axis2Context,
                                       String tenantDomain) {
        // TODO: configure this
        int idleTimeoutInSeconds = 3;
        ServerHandshakeFuture future = webSocketHandshaker
                .handshake(getNegotiableSubProtocols(subProtocols), idleTimeoutInSeconds * 1000);
        future.setHandshakeListener(new UpgradeListener(axis2Context, tenantDomain,
                webSocketHandshaker.getHttpCarbonRequest()));

    }

    public static void dispatchOnHandshakeSuccess(WebSocketConnection webSocketConnection,
                                                  org.apache.axis2.context.MessageContext axis2Context,
                                                  String tenantDomain, HttpCarbonRequest httpCarbonRequest) {

        org.apache.synapse.MessageContext synCtx;
        try {
            synCtx = MessageUtil.getSynapseMessageContext(axis2Context, webSocketConnection, tenantDomain);
        } catch (AxisFault e) {
            WebSocketUtil.printLog(LOG, WebSocketConstants.LogLevel.ERROR, webSocketConnection.getChannelId(), e,
                    "Error occurred while creating the Synapse Message Context. Hence, closing the connection");
            WebSocketUtil.terminateConnection(webSocketConnection, WebSocketConstants.WebSocketError.SERVER_ERROR,
                    tenantDomain);
            return;
        }

        MessageUtil.populatePropertiesOnSourceHandshake(axis2Context, synCtx, httpCarbonRequest);

        MessageUtil.injectForMediation(synCtx, axis2Context, webSocketConnection, httpCarbonRequest);
        webSocketConnection.startReadingFrames();

    }

    public static void dispatchOnText(WebSocketTextMessage textMessage) {

    }

    public static void dispatchOnBinary(WebSocketBinaryMessage webSocketBinaryMessage) {

        String negotiatedSubProtocol = webSocketBinaryMessage.getWebSocketConnection().getNegotiatedSubProtocol();
        if (!continueWithSelectedSubProtocol(negotiatedSubProtocol)) {
            return;
        }

        String contentType = negotiatedSubProtocol;
        if (contentType == null && defaultContentType != null) {
            contentType = defaultContentType;
        }

        org.apache.axis2.context.MessageContext axis2MsgCtx = createAxis2MessageContext();

        if (!invokeMessagingHandlersForWebSocketFrame(axis2MsgCtx, webSocketBinaryMessage)) {
            return;
        }

        int port = webSocketBinaryMessage.getWebSocketConnection().getPort();
        String endpointName = WebsocketEndpointManager.getInstance().getEndpointName(port, tenantDomain);

        try {
            org.apache.synapse.MessageContext synCtx = getSynapseMessageContext(axis2MsgCtx);
            InboundEndpoint endpoint = synCtx.getConfiguration().getInboundEndpoint(endpointName);

            if (endpoint == null) {
                LOG.error("Cannot find deployed inbound endpoint " + endpointName + "for process request");
                return;
            }

            handleWebsocketBinaryFrame(webSocketBinaryMessage, synCtx, port, contentType);

        } catch (Exception e) {
            LOG.error("Exception occurred while injecting websocket frames to the Synapse engine", e);
        }

    }

    public static void dispatchOnPing(WebSocketControlMessage webSocketControlMessage) {
        webSocketControlMessage.getWebSocketConnection().pong(webSocketControlMessage.getByteBuffer())
                .addListener(future -> {
                    Throwable cause = future.cause();
                    if (!future.isSuccess() && cause != null) {
                        // TODO: log error message
                    }
                });
    }

    public static void dispatchOnClose(WebSocketCloseMessage webSocketCloseMessage) {
        WebSocketUtil.finishConnectionClosureIfOpen(webSocketCloseMessage.getWebSocketConnection(),
                webSocketCloseMessage.getCloseCode());
        // TODO: this is triggerred when the channel inactive method is
        //  invoked. Therefore, no need to terminate the connection as the
        //  connection is already closed. But need to notify the backend's
        //  if there are any.

    }

    public static void dispatchOnClose(WebSocketConnection webSocketConnection) {
        // TODO: this is triggerred when the channel inactive method is
        //  invoked. Therefore, no need to terminate the connection as the
        //  connection is already closed. But need to notify the backend's
        //  if there are any.

    }

    public static void dispatchOnError(WebSocketConnection webSocketConnection, Throwable throwable) {
        // TODO: just log the error
    }

    public static void dispatchOnIdleTimeout(WebSocketControlMessage webSocketControlMessage) {
        // TODO: just log the state
    }

    public static String[] getNegotiableSubProtocols(List<String> subProtocols) {
        String[] subProtocolArray = new String[subProtocols.size()];
        return subProtocols.toArray(subProtocolArray);
    }

    public static void addContentTypesToSubProtocolArray(List<String> subProtocols, List<String> contentTypes) {

        for (String contentType : contentTypes) {
            subProtocols.add("synapse(contentType='" + contentType + "')");
        }
    }

    protected static void handleException(String msg) {
        LOG.error(msg);
        throw new SynapseException(msg);
    }
}
