package org.wso2.carbon.inbound.endpoint.protocol.http_websocket.websocketlistener;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.util.UIDGenerator;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.description.InOutAxisOperation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.commons.handlers.ConnectionId;
import org.apache.synapse.commons.handlers.HandlerResponse;
import org.apache.synapse.commons.handlers.MessageHolder;
import org.apache.synapse.commons.handlers.MessagingHandler;
import org.apache.synapse.commons.handlers.MessagingHandlerConstants;
import org.apache.synapse.commons.handlers.Protocol;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.MessageContextCreatorForAxis2;
import org.apache.synapse.inbound.InboundEndpoint;
import org.apache.synapse.inbound.InboundEndpointConstants;
import org.wso2.carbon.inbound.endpoint.osgi.service.ServiceReferenceHolder;
import org.wso2.carbon.inbound.endpoint.protocol.http_websocket.InboundHttpAndWebsocketListener;
import org.wso2.carbon.inbound.endpoint.protocol.websocket.InboundWebsocketConstants;
import org.wso2.carbon.inbound.endpoint.protocol.websocket.InboundWebsocketResponseSender;
import org.wso2.carbon.inbound.endpoint.protocol.websocket.management.WebsocketEndpointManager;
import org.wso2.carbon.inbound.endpoint.protocol.websocket.management.WebsocketSubscriberPathManager;
import org.wso2.transport.http.netty.contract.websocket.WebSocketBinaryMessage;
import org.wso2.transport.http.netty.contract.websocket.WebSocketCloseMessage;
import org.wso2.transport.http.netty.contract.websocket.WebSocketConnection;
import org.wso2.transport.http.netty.contract.websocket.WebSocketConnectorListener;
import org.wso2.transport.http.netty.contract.websocket.WebSocketControlMessage;
import org.wso2.transport.http.netty.contract.websocket.WebSocketHandshaker;
import org.wso2.transport.http.netty.contract.websocket.WebSocketTextMessage;
import org.wso2.transport.http.netty.contractimpl.websocket.message.DefaultWebSocketHandshaker;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.wso2.carbon.inbound.endpoint.common.Constants.SUPER_TENANT_DOMAIN_NAME;
import static org.wso2.carbon.inbound.endpoint.common.Constants.TENANT_DOMAIN;

/**
 * Server Connector listener for WebSocket.
 */
public class WebSocketServerListener implements WebSocketConnectorListener {

    private static final Log LOG = LogFactory.getLog(InboundHttpAndWebsocketListener.class);

    List<MessagingHandler> messagingHandlers;
    private InboundWebsocketResponseSender responseSender;

    private final String tenantDomain = SUPER_TENANT_DOMAIN_NAME;
    private int port;

    public WebSocketServerListener(List<MessagingHandler> messagingHandlers) {
        this.messagingHandlers = messagingHandlers;
    }

    @Override
    public void onHandshake(WebSocketHandshaker webSocketHandshaker) {

//        this.responseSender = new InboundWebsocketResponseSender(this);
//
//        DefaultWebSocketHandshaker handshaker = (DefaultWebSocketHandshaker)webSocketHandshaker;
//
//        executeMessagingHandlersOnHandshake();
//
//        this.port = ((InetSocketAddress) ctx.channel().localAddress()).getPort()
//        List<Map.Entry<String, String>> httpHeaders = webSocketHandshaker.getHttpCarbonRequest().getHeaders().entries();
//
//        String endpointName = WebsocketEndpointManager.getInstance().getEndpointName(port, tenantDomain);
//        if (endpointName == null) {
//            handleException("Endpoint not found for port : " + port + "" + " tenant domain : " + tenantDomain);
//        }
//
//        WebsocketSubscriberPathManager.getInstance()
//                .addChannelContext(endpointName, subscriberPath.getPath(), wrappedContext);
//        MessageContext synCtx = getSynapseMessageContext(tenantDomain, ctx);
//        InboundEndpoint endpoint = synCtx.getConfiguration().getInboundEndpoint(endpointName);
//        defaultContentType = endpoint.getParametersMap().get(InboundWebsocketConstants.INBOUND_DEFAULT_CONTENT_TYPE);
//        if (endpoint == null) {
//            log.error("Cannot find deployed inbound endpoint " + endpointName + "for process request");
//            return;
//        }
//
//        for (Map.Entry<String, String> entry : httpHeaders) {
//            synCtx.setProperty(entry.getKey(), entry.getValue());
//            ((Axis2MessageContext) synCtx).getAxis2MessageContext().setProperty(entry.getKey(), entry.getValue());
//        }
//
//        synCtx.setProperty(InboundWebsocketConstants.SOURCE_HANDSHAKE_PRESENT, new Boolean(true));
//        ((Axis2MessageContext) synCtx).getAxis2MessageContext()
//                .setProperty(InboundWebsocketConstants.SOURCE_HANDSHAKE_PRESENT, new Boolean(true));
//        synCtx.setProperty(InboundWebsocketConstants.WEBSOCKET_SOURCE_HANDSHAKE_PRESENT, new Boolean(true));
//        ((Axis2MessageContext) synCtx).getAxis2MessageContext()
//                .setProperty(InboundWebsocketConstants.WEBSOCKET_SOURCE_HANDSHAKE_PRESENT, new Boolean(true));
//        ((Axis2MessageContext) synCtx).getAxis2MessageContext()
//                .setProperty(InboundWebsocketConstants.CLIENT_ID, ctx.channel().hashCode());
//        injectForMediation(synCtx, endpoint);
    }

    @Override
    public void onMessage(WebSocketTextMessage webSocketTextMessage) {

        LOG.info("testing testing!");
//        webSocketTextMessage.getWebSocketConnection().
    }

    @Override
    public void onMessage(WebSocketBinaryMessage webSocketBinaryMessage) {

        LOG.info("testing testing!");
    }

    @Override
    public void onMessage(WebSocketControlMessage webSocketControlMessage) {

        LOG.info("testing testing!");
    }

    @Override
    public void onMessage(WebSocketCloseMessage webSocketCloseMessage) {

        LOG.info("testing testing!");
    }

    @Override
    public void onClose(WebSocketConnection webSocketConnection) {

        LOG.info("testing testing!");
    }

    @Override
    public void onError(WebSocketConnection webSocketConnection, Throwable throwable) {

        LOG.info("testing testing!");
    }

    @Override
    public void onIdleTimeout(WebSocketControlMessage webSocketControlMessage) {

        LOG.info("testing testing!");
    }

//    private boolean executeMessagingHandlersForWebSocketFrame(org.apache.axis2.context.MessageContext axis2MsgCtx,
//                                                              ChannelHandlerContext ctx, WebSocketFrame frame) {
//        if (Objects.isNull(messagingHandlers) || messagingHandlers.isEmpty()) {
//            return true;
//        }
//        MessageHolder message = new MessageHolder(frame, Protocol.WS, new ConnectionId(ctx.channel().id().asShortText()));
//        axis2MsgCtx.setProperty(MessagingHandlerConstants.HANDLER_MESSAGE_CONTEXT, message);
//
//        for (MessagingHandler handler: messagingHandlers) {
//            HandlerResponse response = handler.handleSourceMessage(axis2MsgCtx);
//            if (Objects.isNull(response) || !response.isError()) {
//                continue;
//            }
//            if (response.isCloseConnection()) {
//                ctx.writeAndFlush(new CloseWebSocketFrame(response.getErrorCode(),
//                        "Connection closed! " + response.getErrorMessage()));
//                ctx.close();
//            } else {
//                String errorMessage = response.getErrorResponseString();
//                ctx.writeAndFlush(new TextWebSocketFrame(errorMessage));
//            }
//            return false;
//        }
//        return true;
//    }

    private boolean executeMessagingHandlersOnHandshake(org.apache.axis2.context.MessageContext axis2MsgCtx,
                                                        WebSocketHandshaker webSocketHandshaker) {

        if (Objects.isNull(messagingHandlers) || messagingHandlers.isEmpty()) {
            return true;
        }

        MessageHolder message = new MessageHolder(webSocketHandshaker.getHttpCarbonRequest(),
                Protocol.WS, new ConnectionId(webSocketHandshaker.getChannelId()));
        axis2MsgCtx.setProperty(MessagingHandlerConstants.HANDLER_MESSAGE_CONTEXT, message);

        for (MessagingHandler handler: messagingHandlers) {
            HandlerResponse response = handler.handleSourceRequest(axis2MsgCtx);
            if (Objects.nonNull(response) && response.isError()) {
                webSocketHandshaker.cancelHandshake(response.getErrorCode(), response.getErrorMessage());
                LOG.error("WebSocket handshake failed. " + response.getErrorResponseString());
                return false;
            }
        }
        return true;
    }

    public MessageContext getSynapseMessageContext() throws AxisFault {
        MessageContext synCtx = createSynapseMessageContext();
//        synCtx.setProperty(SynapseConstants.IS_INBOUND, true);
//        ((Axis2MessageContext) synCtx).getAxis2MessageContext().setProperty(SynapseConstants.IS_INBOUND, true);
//        synCtx.setProperty(InboundEndpointConstants.INBOUND_ENDPOINT_RESPONSE_WORKER, responseSender);
//        ((Axis2MessageContext) synCtx).getAxis2MessageContext()
//                .setProperty(InboundEndpointConstants.INBOUND_ENDPOINT_RESPONSE_WORKER, responseSender);
//        synCtx.setProperty(InboundWebsocketConstants.WEBSOCKET_SOURCE_HANDLER_CONTEXT, wrappedContext.getChannelHandlerContext());
//        ((Axis2MessageContext) synCtx).getAxis2MessageContext()
//                .setProperty(InboundWebsocketConstants.WEBSOCKET_SOURCE_HANDLER_CONTEXT, wrappedContext.getChannelHandlerContext());
//        synCtx.setProperty(InboundWebsocketConstants.WEBSOCKET_SOURCE_CHANNEL_IDENTIFIER,
//                wrappedContext.getChannelIdentifier());
//        ((Axis2MessageContext) synCtx).getAxis2MessageContext()
//                .setProperty(InboundWebsocketConstants.WEBSOCKET_SOURCE_CHANNEL_IDENTIFIER,
//                        wrappedContext.getChannelIdentifier());
//        if (outflowDispatchSequence != null) {
//            synCtx.setProperty(InboundWebsocketConstants.WEBSOCKET_OUTFLOW_DISPATCH_SEQUENCE, outflowDispatchSequence);
//            ((Axis2MessageContext) synCtx).getAxis2MessageContext()
//                    .setProperty(InboundWebsocketConstants.WEBSOCKET_OUTFLOW_DISPATCH_SEQUENCE, outflowDispatchSequence);
//        }
//        if (outflowErrorSequence != null) {
//            synCtx.setProperty(InboundWebsocketConstants.WEBSOCKET_OUTFLOW_DISPATCH_FAULT_SEQUENCE, outflowErrorSequence);
//            ((Axis2MessageContext) synCtx).getAxis2MessageContext()
//                    .setProperty(InboundWebsocketConstants.WEBSOCKET_OUTFLOW_DISPATCH_FAULT_SEQUENCE, outflowErrorSequence);
//        }
//        synCtx.setProperty(InboundWebsocketConstants.WEBSOCKET_SUBSCRIBER_PATH, subscriberPath.toString());
        return synCtx;
    }

    private static org.apache.synapse.MessageContext createSynapseMessageContext() throws AxisFault {
        org.apache.axis2.context.MessageContext axis2MsgCtx = createAxis2MessageContext();
        ServiceContext svcCtx = new ServiceContext();
        OperationContext opCtx = new OperationContext(new InOutAxisOperation(), svcCtx);
        axis2MsgCtx.setServiceContext(svcCtx);
        axis2MsgCtx.setOperationContext(opCtx);

        axis2MsgCtx.setProperty(TENANT_DOMAIN, SUPER_TENANT_DOMAIN_NAME);

        SOAPFactory fac = OMAbstractFactory.getSOAP11Factory();
        SOAPEnvelope envelope = fac.getDefaultEnvelope();
        axis2MsgCtx.setEnvelope(envelope);
        return MessageContextCreatorForAxis2.getSynapseMessageContext(axis2MsgCtx);
    }

    private static org.apache.axis2.context.MessageContext createAxis2MessageContext() {
        org.apache.axis2.context.MessageContext axis2MsgCtx = new org.apache.axis2.context.MessageContext();
        axis2MsgCtx.setMessageID(UIDGenerator.generateURNString());
        axis2MsgCtx.setConfigurationContext(
                ServiceReferenceHolder.getInstance().getConfigurationContextService().getServerConfigContext());
        axis2MsgCtx.setProperty(org.apache.axis2.context.MessageContext.CLIENT_API_NON_BLOCKING, Boolean.TRUE);
        axis2MsgCtx.setServerSide(true);

        return axis2MsgCtx;
    }
}
