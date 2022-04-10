package org.wso2.sample;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.util.UIDGenerator;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.builder.Builder;
import org.apache.axis2.builder.BuilderUtil;
import org.apache.axis2.builder.SOAPBuilder;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.description.InOutAxisOperation;
import org.apache.axis2.transport.TransportUtils;
import org.apache.commons.io.input.AutoCloseInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.api.rest.RestRequestHandler;
import org.apache.synapse.commons.handlers.ConnectionId;
import org.apache.synapse.commons.handlers.HandlerResponse;
import org.apache.synapse.commons.handlers.MessageInfo;
import org.apache.synapse.commons.handlers.MessagingHandler;
import org.apache.synapse.commons.handlers.MessagingHandlerConstants;
import org.apache.synapse.commons.handlers.Protocol;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.MessageContextCreatorForAxis2;
import org.apache.synapse.inbound.InboundEndpoint;
import org.apache.synapse.transport.customlogsetter.CustomLogSetter;
import org.apache.synapse.transport.netty.listener.AbstractWebSocketListener;
import org.wso2.carbon.inbound.endpoint.osgi.service.ServiceReferenceHolder;
import org.wso2.carbon.inbound.endpoint.protocol.httpwebsocket.management.HttpWebsocketEndpointManager;
import org.wso2.carbon.inbound.endpoint.protocol.websocket.InboundWebsocketConstants;
import org.wso2.carbon.inbound.endpoint.protocol.websocket.SubprotocolBuilderUtil;
import org.wso2.carbon.inbound.endpoint.protocol.websocket.WebsocketLogUtil;
import org.wso2.carbon.inbound.endpoint.protocol.websocket.management.WebsocketEndpointManager;
import org.wso2.transport.http.netty.contract.websocket.ServerHandshakeFuture;
import org.wso2.transport.http.netty.contract.websocket.WebSocketBinaryMessage;
import org.wso2.transport.http.netty.contract.websocket.WebSocketCloseMessage;
import org.wso2.transport.http.netty.contract.websocket.WebSocketConnection;
import org.wso2.transport.http.netty.contract.websocket.WebSocketConnectorListener;
import org.wso2.transport.http.netty.contract.websocket.WebSocketControlMessage;
import org.wso2.transport.http.netty.contract.websocket.WebSocketControlSignal;
import org.wso2.transport.http.netty.contract.websocket.WebSocketHandshaker;
import org.wso2.transport.http.netty.contract.websocket.WebSocketMessage;
import org.wso2.transport.http.netty.contract.websocket.WebSocketTextMessage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.wso2.carbon.inbound.endpoint.common.Constants.SUPER_TENANT_DOMAIN_NAME;
import static org.wso2.carbon.inbound.endpoint.common.Constants.TENANT_DOMAIN;

public class WebSocketListener extends AbstractWebSocketListener {

    @Override
    public void onMessage(WebSocketTextMessage webSocketTextMessage) {

    }

    @Override
    public void onMessage(WebSocketBinaryMessage webSocketBinaryMessage) {

    }

    @Override
    public void onMessage(WebSocketControlMessage webSocketControlMessage) {

    }

    @Override
    public void onMessage(WebSocketCloseMessage webSocketCloseMessage) {

    }

    @Override
    public void onClose(WebSocketConnection webSocketConnection) {

    }

    @Override
    public void onError(WebSocketConnection webSocketConnection, Throwable throwable) {

    }

    @Override
    public void onIdleTimeout(WebSocketControlMessage webSocketControlMessage) {

    }

    private static final Log LOG = LogFactory.getLog(WebSocketListener.class);

    private String defaultContentType;

    private final String tenantDomain = SUPER_TENANT_DOMAIN_NAME;
    private String requestUri;
    private static List<String> contentTypes = new ArrayList<>();
    private static List<String> subProtocols = new ArrayList<>();

    static {
        contentTypes.add("application/xml");
        contentTypes.add("application/json");
        contentTypes.add("text/xml");
        addContentTypesToSubProtocolArray(subProtocols, contentTypes);
    }

    @Override
    public void onHandshake(WebSocketHandshaker webSocketHandshaker) {

        org.apache.axis2.context.MessageContext axis2Context =  createAxis2MessageContext();

        if (!invokeMessagingHandlersForHandshakeRequest(axis2Context, webSocketHandshaker)) {
            return;
        }

        int idleTimeoutInSeconds = 3;
        ServerHandshakeFuture future = webSocketHandshaker
                .handshake(getNegotiableSubProtocols(subProtocols), idleTimeoutInSeconds * 1000);
//        future.setHandshakeListener(new UpgradeListener(axis2Context, tenantDomain,
//                webSocketHandshaker.getHttpCarbonRequest()));
        defaultContentType = "";
        requestUri = webSocketHandshaker.getTarget();

    }
//
//    @Override
//    public void onMessage(WebSocketTextMessage webSocketTextMessage) {
//
//        if (LOG.isDebugEnabled()) {
////            WebsocketLogUtil.printWebSocketFrame(LOG, webSocketTextMessage, webSocketTextMessage.getWebSocketConnection().getChannelId(), null, true);
//        }
//
//    }
//
//    @Override
//    public void onMessage(WebSocketBinaryMessage webSocketBinaryMessage) {
//
//        if (LOG.isDebugEnabled()) {
//            WebsocketLogUtil.printWebSocketFrame(LOG, webSocketBinaryMessage,
//                    webSocketBinaryMessage.getWebSocketConnection().getChannelId(), null, true);
//        }
//
//        String negotiatedSubProtocol = webSocketBinaryMessage.getWebSocketConnection().getNegotiatedSubProtocol();
//        if (!continueWithSelectedSubProtocol(negotiatedSubProtocol)) {
//            return;
//        }
//
//        String contentType = negotiatedSubProtocol;
//        if (contentType == null && defaultContentType != null) {
//            contentType = defaultContentType;
//        }
//
//        org.apache.axis2.context.MessageContext axis2MsgCtx =  createAxis2MessageContext();
//
//        if (!invokeMessagingHandlersForWebSocketFrame(axis2MsgCtx, webSocketBinaryMessage)) {
//            return;
//        }
//
//        int port = webSocketBinaryMessage.getWebSocketConnection().getPort();
//        String endpointName = WebsocketEndpointManager.getInstance().getEndpointName(port, tenantDomain);
//
//        try {
//            MessageContext synCtx = getSynapseMessageContext(axis2MsgCtx);
//            InboundEndpoint endpoint = synCtx.getConfiguration().getInboundEndpoint(endpointName);
//
//            if (endpoint == null) {
//                LOG.error("Cannot find deployed inbound endpoint " + endpointName + "for process request");
//                return;
//            }
//
//            handleWebsocketBinaryFrame(webSocketBinaryMessage, synCtx, port, contentType);
//
//        } catch (Exception e) {
//            LOG.error("Exception occurred while injecting websocket frames to the Synapse engine", e);
//        }
//    }
//
//    @Override
//    public void onMessage(WebSocketControlMessage webSocketControlMessage) {
//        if (webSocketControlMessage.getControlSignal() == WebSocketControlSignal.PING) {
//            webSocketControlMessage.getWebSocketConnection().pong(webSocketControlMessage.getByteBuffer());
//        } else if (webSocketControlMessage.getControlSignal() == WebSocketControlSignal.PONG) {
//            //
//        }
//
//    }
//
//    @Override
//    public void onMessage(WebSocketCloseMessage webSocketCloseMessage) {
//        terminateConnection(webSocketCloseMessage.getWebSocketConnection());
//        if (LOG.isDebugEnabled()) {
//            WebsocketLogUtil.printSpecificLog(LOG, webSocketCloseMessage.getWebSocketConnection().getChannelId(),
//                    "Websocket channel is terminated successfully.");
//        }
//    }
//
//    @Override
//    public void onClose(WebSocketConnection webSocketConnection) {
//
//    }
//
//    @Override
//    public void onError(WebSocketConnection webSocketConnection, Throwable throwable) {
//
//    }
//
//    @Override
//    public void onIdleTimeout(WebSocketControlMessage webSocketControlMessage) {
//
//    }
//
    private boolean invokeMessagingHandlersForHandshakeRequest(org.apache.axis2.context.MessageContext axis2MsgCtx,
                                                               WebSocketHandshaker webSocketHandshaker) {
//        if (Objects.isNull(messagingHandlers) || messagingHandlers.isEmpty()) {
//            return true;
//        }
//        MessageInfo message = new MessageInfo(webSocketHandshaker.getHttpCarbonRequest(), Protocol.WS,
//                new ConnectionId(webSocketHandshaker.getChannelId()));
//        axis2MsgCtx.setProperty(MessagingHandlerConstants.HANDLER_MESSAGE_CONTEXT, message);
//
//        for (MessagingHandler handler: messagingHandlers) {
//            HandlerResponse response = handler.handleRequest(axis2MsgCtx);
//            if (Objects.nonNull(response) && response.isError()) {
//                LOG.error("Handle WebSocket handshake request failed at {handler name}. "
//                        + response.getErrorResponseString());
//                webSocketHandshaker.cancelHandshake(response.getErrorCode(), response.getErrorMessage());
//                return false;
//            }
//        }
        return true;
    }

    private boolean invokeMessagingHandlersForWebSocketFrame(org.apache.axis2.context.MessageContext axis2MsgCtx,
                                                             WebSocketMessage webSocketMessage) {
//        if (Objects.isNull(messagingHandlers) || messagingHandlers.isEmpty()) {
//            return true;
//        }
//        MessageInfo message = new MessageInfo(webSocketMessage, Protocol.WS,
//                new ConnectionId(webSocketMessage.getWebSocketConnection().getChannelId()));
//        axis2MsgCtx.setProperty(MessagingHandlerConstants.HANDLER_MESSAGE_CONTEXT, message);
//
//        for (MessagingHandler handler: messagingHandlers) {
//            HandlerResponse response = handler.handleRequest(axis2MsgCtx);
//            if (Objects.isNull(response) || !response.isError()) {
//                continue;
//            }
//            if (response.isCloseConnection()) {
//                webSocketMessage.getWebSocketConnection().terminateConnection(response.getErrorCode(),
//                        response.getErrorMessage());
//            } else {
//                webSocketMessage.getWebSocketConnection().pushText(response.getErrorResponseString());
//            }
//            return false;
//        }
        return true;
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

    public MessageContext getSynapseMessageContext(org.apache.axis2.context.MessageContext axis2MsgCtx) throws AxisFault {
        MessageContext synCtx = createSynapseMessageContext(axis2MsgCtx);
        synCtx.setProperty(SynapseConstants.IS_INBOUND, true);
        ((Axis2MessageContext) synCtx).getAxis2MessageContext().setProperty(SynapseConstants.IS_INBOUND, true);
//        synCtx.setProperty(InboundEndpointConstants.INBOUND_ENDPOINT_RESPONSE_WORKER, responseSender);
//        ((Axis2MessageContext) synCtx).getAxis2MessageContext()
//                .setProperty(InboundEndpointConstants.INBOUND_ENDPOINT_RESPONSE_WORKER, responseSender);
//        synCtx.setProperty(InboundWebsocketConstants.WEBSOCKET_SOURCE_HANDLER_CONTEXT,
//                wrappedContext.getChannelHandlerContext());
//        ((Axis2MessageContext) synCtx).getAxis2MessageContext()
//                .setProperty(InboundWebsocketConstants.WEBSOCKET_SOURCE_HANDLER_CONTEXT,
//                        wrappedContext.getChannelHandlerContext());
//        if (outflowDispatchSequence != null) {
//            synCtx.setProperty(InboundWebsocketConstants.WEBSOCKET_OUTFLOW_DISPATCH_SEQUENCE, outflowDispatchSequence);
//            ((Axis2MessageContext) synCtx).getAxis2MessageContext()
//                    .setProperty(InboundWebsocketConstants.WEBSOCKET_OUTFLOW_DISPATCH_SEQUENCE,
//                            outflowDispatchSequence);
//        }
//        if (outflowErrorSequence != null) {
//            synCtx.setProperty(InboundWebsocketConstants.WEBSOCKET_OUTFLOW_DISPATCH_FAULT_SEQUENCE,
//                    outflowErrorSequence);
//            ((Axis2MessageContext) synCtx).getAxis2MessageContext()
//                    .setProperty(InboundWebsocketConstants.WEBSOCKET_OUTFLOW_DISPATCH_FAULT_SEQUENCE,
//                            outflowErrorSequence);
//        }
//        synCtx.setProperty(InboundWebsocketConstants.WEBSOCKET_SUBSCRIBER_PATH, subscriberPath.toString());
        return synCtx;
    }

    private static MessageContext createSynapseMessageContext(org.apache.axis2.context.MessageContext axis2MsgCtx) throws AxisFault {
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

    protected void handleWebsocketBinaryFrame(WebSocketBinaryMessage webSocketBinaryMessage, MessageContext synCtx,
                                              int port, String contentType) throws AxisFault {
        String endpointName = HttpWebsocketEndpointManager.getInstance().getEndpointName(port, tenantDomain);

        InboundEndpoint endpoint = synCtx.getConfiguration().getInboundEndpoint(endpointName);

        if (endpoint == null) {
            LOG.error("Cannot find deployed inbound endpoint " + endpointName + "for process request");
            return;
        }

        org.apache.axis2.context.MessageContext axis2MsgCtx = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
        synCtx.setProperty(InboundWebsocketConstants.WEBSOCKET_BINARY_FRAME_PRESENT, true);
        axis2MsgCtx.setProperty(InboundWebsocketConstants.WEBSOCKET_BINARY_FRAME_PRESENT, true);
        synCtx.setProperty(InboundWebsocketConstants.WEBSOCKET_BINARY_MESSAGE, webSocketBinaryMessage.getByteBuffer());
        axis2MsgCtx.setProperty(InboundWebsocketConstants.WEBSOCKET_BINARY_MESSAGE, webSocketBinaryMessage.getByteBuffer());

        Builder builder = BuilderUtil.getBuilderFromSelector(contentType, axis2MsgCtx);
        if (builder != null) {
            if (InboundWebsocketConstants.BINARY_BUILDER_IMPLEMENTATION
                    .equals(builder.getClass().getName())) {
                synCtx.setProperty(InboundWebsocketConstants.WEBSOCKET_BINARY_FRAME_PRESENT, true);
            } else {
                synCtx.setProperty(InboundWebsocketConstants.WEBSOCKET_BINARY_FRAME_PRESENT, false);
            }
            InputStream in = new AutoCloseInputStream(
                    new ByteBufInputStream(Unpooled.wrappedBuffer(webSocketBinaryMessage.getByteBuffer())));

            OMElement documentElement = builder.processDocument(in, contentType, axis2MsgCtx);
            synCtx.setEnvelope(TransportUtils.createSOAPEnvelope(documentElement));
        }
        injectForMediation(synCtx, endpoint, webSocketBinaryMessage.getWebSocketConnection());

    }

    public static String[] getNegotiableSubProtocols(List<String> subProtocols) {
        String[] subProtocolArray = new String[subProtocols.size()];
        return subProtocols.toArray(subProtocolArray);
    }

    public static void addContentTypesToSubProtocolArray(List<String> subProtocols, List<String> contentTypes) {

        for (String contentType: contentTypes) {
            subProtocols.add("synapse(contentType='" + contentType + "')");
        }
    }
//
//    private void terminateConnection(WebSocketConnection webSocketConnection) {
//        webSocketConnection.terminateConnection();
//        String endpointName = HttpWebsocketEndpointManager.getInstance().getEndpointName(webSocketConnection.getPort(),
//                tenantDomain);
////        WebsocketSubscriberPathManager.getInstance()
////                .removeChannelContext(endpointName, subscriberPath.getPath(), wrappedContext);
//    }
//
//    private boolean continueWithSelectedSubProtocol(String negotiatedSubProtocol) {
//
//        return Objects.nonNull(negotiatedSubProtocol) && negotiatedSubProtocol
//                .contains(InboundWebsocketConstants.SYNAPSE_SUBPROTOCOL_PREFIX);
//    }
//
    private void injectForMediation(MessageContext synCtx, InboundEndpoint endpoint,
                                    WebSocketConnection webSocketConnection) {
//        SequenceMediator faultSequence = getFaultSequence(synCtx, endpoint);
//        MediatorFaultHandler mediatorFaultHandler = new MediatorFaultHandler(faultSequence);
//        synCtx.pushFaultHandler(mediatorFaultHandler);
//        if (log.isDebugEnabled()) {
//            log.debug("injecting message to sequence : " + endpoint.getInjectingSeq());
//        }
        synCtx.setProperty("inbound.endpoint.name", endpoint.getName());
//        synCtx.setProperty(ApiConstants.API_CALLER, endpoint.getName());

//        boolean isProcessed;
        org.apache.axis2.context.MessageContext axis2Context = ((Axis2MessageContext)synCtx).getAxis2MessageContext();
        axis2Context.setIncomingTransportName(getScheme(webSocketConnection));
        axis2Context.setProperty(Constants.Configuration.TRANSPORT_IN_URL, requestUri);
//            isProcessed = inboundApiHandler.process(synCtx);
        new RestRequestHandler().process(synCtx);

//        if (!isProcessed) {
//            SequenceMediator injectingSequence = null;
//            if (endpoint.getInjectingSeq() != null) {
//                injectingSequence = (SequenceMediator) synCtx.getSequence(endpoint.getInjectingSeq());
//            }
//            if (injectingSequence == null) {
//                injectingSequence = (SequenceMediator) synCtx.getMainSequence();
//            }
//            if (dispatchToCustomSequence) {
//                String context = (subscriberPath.getPath()).substring(1);
//                context = context.replace('/', '-');
//                if (synCtx.getConfiguration().getDefinedSequences().containsKey(context))
//                    injectingSequence = (SequenceMediator) synCtx.getSequence(context);
//            }
//            synCtx.getEnvironment().injectMessage(synCtx, injectingSequence);
//        }
    }

    private String getScheme(WebSocketConnection webSocketConnection) {
        if (webSocketConnection.isSecure()) {
            return InboundWebsocketConstants.WSS;
        }
        return InboundWebsocketConstants.WS;
    }

    protected void handleWebsocketPassThroughTextFrame(WebSocketTextMessage webSocketTextMessage, MessageContext synCtx,
                                                       InboundEndpoint endpoint) throws AxisFault {

        org.apache.axis2.context.MessageContext axis2MsgCtx = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
        String negotiatedSubProtocol = webSocketTextMessage.getWebSocketConnection().getNegotiatedSubProtocol();

        if ((negotiatedSubProtocol == null) || !negotiatedSubProtocol
                .contains(InboundWebsocketConstants.SYNAPSE_SUBPROTOCOL_PREFIX)) {
            String contentType = negotiatedSubProtocol;
            if (contentType == null && defaultContentType != null) {
                contentType = defaultContentType;
            }
            synCtx.setProperty(InboundWebsocketConstants.WEBSOCKET_TEXT_FRAME_PRESENT, Boolean.TRUE);
            axis2MsgCtx.setProperty(InboundWebsocketConstants.WEBSOCKET_TEXT_FRAME_PRESENT, Boolean.TRUE);
            synCtx.setProperty(InboundWebsocketConstants.WEBSOCKET_TEXT_MESSAGE, webSocketTextMessage.getText());
            axis2MsgCtx.setProperty(InboundWebsocketConstants.WEBSOCKET_TEXT_MESSAGE, webSocketTextMessage.getText());

            Builder builder = BuilderUtil.getBuilderFromSelector(contentType, axis2MsgCtx);
            if (builder != null) {
                if (InboundWebsocketConstants.TEXT_BUILDER_IMPLEMENTATION.equals(builder.getClass().getName())) {
                    synCtx.setProperty(InboundWebsocketConstants.WEBSOCKET_TEXT_FRAME_PRESENT, true);
                } else {
                    synCtx.setProperty(InboundWebsocketConstants.WEBSOCKET_TEXT_FRAME_PRESENT, false);
                }
                InputStream in = new AutoCloseInputStream(
                        new ByteArrayInputStream(webSocketTextMessage.getText().getBytes()));
                OMElement documentElement = builder.processDocument(in, contentType, axis2MsgCtx);
                synCtx.setEnvelope(TransportUtils.createSOAPEnvelope(documentElement));
            }
        } else {
            CustomLogSetter.getInstance().setLogAppender(endpoint.getArtifactContainerName());

            String message = webSocketTextMessage.getText();
            String contentType = SubprotocolBuilderUtil.syanapeSubprotocolToContentType(
                    SubprotocolBuilderUtil.extractSynapseSubprotocol(negotiatedSubProtocol));

            Builder builder = null;
            if (contentType == null) {
                LOG.debug("No content type specified. Using SOAP builder.");
                builder = new SOAPBuilder();
            } else {
                int index = contentType.indexOf(';');
                String type = index > 0 ? contentType.substring(0, index) : contentType;
                try {
                    builder = BuilderUtil.getBuilderFromSelector(type, axis2MsgCtx);
                } catch (AxisFault axisFault) {
                    LOG.error("Error while creating message builder :: " + axisFault.getMessage());
                }
                if (builder == null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("No message builder found for type '" + type + "'. Falling back to SOAP.");
                    }
                    builder = new SOAPBuilder();
                }
            }
            OMElement documentElement;
            InputStream in = new AutoCloseInputStream(new ByteArrayInputStream(message.getBytes()));
            documentElement = builder.processDocument(in, contentType, axis2MsgCtx);
            synCtx.setEnvelope(TransportUtils.createSOAPEnvelope(documentElement));
        }
        injectForMediation(synCtx, endpoint, webSocketTextMessage.getWebSocketConnection());
    }
}
