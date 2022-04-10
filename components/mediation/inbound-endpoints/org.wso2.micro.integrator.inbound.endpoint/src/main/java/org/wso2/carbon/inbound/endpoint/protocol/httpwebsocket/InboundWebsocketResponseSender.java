/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.inbound.endpoint.protocol.httpwebsocket;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axis2.transport.MessageFormatter;
import org.apache.axis2.transport.base.BaseUtils;
import org.apache.axis2.util.JavaUtils;
import org.apache.axis2.util.MessageProcessorSelector;
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.inbound.InboundResponseSender;
import org.apache.synapse.transport.passthru.util.RelayUtils;
import org.wso2.carbon.inbound.endpoint.protocol.httpwebsocket.management.HttpWebsocketEndpointManager;
import org.wso2.carbon.inbound.endpoint.protocol.websocket.InboundWebsocketConstants;
import org.wso2.carbon.inbound.endpoint.protocol.websocket.InboundWebsocketSourceHandler;
import org.wso2.carbon.inbound.endpoint.protocol.websocket.WebsocketLogUtil;
import org.wso2.transport.http.netty.contract.websocket.WebSocketConnection;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.Objects;
import javax.xml.stream.XMLStreamException;

public class InboundWebsocketResponseSender implements InboundResponseSender {

    private static final Log LOG = LogFactory.getLog(InboundWebsocketResponseSender.class);

    WebSocketConnection webSocketConnection;
    private String tenantDomain;

    public InboundWebsocketResponseSender(String tenantDomain, WebSocketConnection webSocketConnection) {
        this.tenantDomain = tenantDomain;
        this.webSocketConnection = webSocketConnection;
    }

    @Override
    public void sendBack(MessageContext msgContext) {
        String defaultContentType = "";
        if (msgContext != null) {
            Integer errorCode = null;
            String errorMessage = null;
            if (msgContext.getProperty("errorCode") != null) {
                errorCode = Integer.parseInt(msgContext.getProperty("errorCode").toString());
            }
            if (msgContext.getProperty("ERROR_MESSAGE") != null) {
                errorMessage = msgContext.getProperty("ERROR_MESSAGE").toString();
            }

            if (errorCode != null && errorMessage != null) {
                CloseWebSocketFrame closeWebSocketFrame = new CloseWebSocketFrame(errorCode, errorMessage);
                if (LOG.isDebugEnabled()) {
                    String customErrorMessage = "errorCode:" + errorCode + " error message: "
                            + errorMessage;
                    WebsocketLogUtil.printWebSocketFrame(LOG, closeWebSocketFrame,
                            webSocketConnection.getChannelId(), customErrorMessage, false);
                }
                terminateConnection(errorCode, errorMessage);
            }

            if (isPropertyTrue(msgContext, InboundWebsocketConstants.SOURCE_HANDSHAKE_PRESENT)) {
                return;
            } else if (isPropertyTrue(msgContext, InboundWebsocketConstants.WEBSOCKET_TARGET_HANDSHAKE_PRESENT)) {

                ChannelHandlerContext targetCtx = (ChannelHandlerContext) msgContext
                        .getProperty(InboundWebsocketConstants.WEBSOCKET_TARGET_HANDLER_CONTEXT);
                if (Objects.nonNull(targetCtx)) {
                    if (LOG.isDebugEnabled()) {
                        WebsocketLogUtil.printWebSocketFrame(LOG, new CloseWebSocketFrame(), targetCtx,
                                false);
                    }
                    addCloseListener(targetCtx);
                }
            } else if (isPropertyTrue(msgContext, InboundWebsocketConstants.WEBSOCKET_BINARY_FRAME_PRESENT)) {

                sendBinaryMessage(webSocketConnection, msgContext, defaultContentType);

            } else if (isPropertyTrue(msgContext, InboundWebsocketConstants.WEBSOCKET_TEXT_FRAME_PRESENT)) {

                sendTextMessage(webSocketConnection, msgContext, defaultContentType);

            } else {
                try {
                    Object wsCloseFrameStatusCode = msgContext
                            .getProperty(InboundWebsocketConstants.WS_CLOSE_FRAME_STATUS_CODE);
                    String wsCloseFrameReasonText = (String) (msgContext
                            .getProperty(InboundWebsocketConstants.WS_CLOSE_FRAME_REASON_TEXT));
                    int statusCode = InboundWebsocketConstants.WS_CLOSE_DEFAULT_CODE;
                    if (wsCloseFrameStatusCode != null) {
                        statusCode = (int) wsCloseFrameStatusCode;
                    } else {
                        wsCloseFrameReasonText = "Unexpected frame type";
                    }

                    if (wsCloseFrameStatusCode != null && wsCloseFrameReasonText != null) {
                        CloseWebSocketFrame closeWebSocketFrame = new CloseWebSocketFrame(statusCode,
                                wsCloseFrameReasonText);
                        if (LOG.isDebugEnabled()) {
                            WebsocketLogUtil.printWebSocketFrame(LOG, closeWebSocketFrame,
                                    webSocketConnection.getChannelId(), null, false);
                        }
                        terminateConnection(statusCode, wsCloseFrameReasonText);
                        return;
                    }
                    org.apache.axis2.context.MessageContext axis2MsgCtx =
                            ((Axis2MessageContext) msgContext).getAxis2MessageContext();
                    RelayUtils.buildMessage(axis2MsgCtx, false);
                    webSocketConnection.pushText(messageContextToText(axis2MsgCtx));

                } catch (IOException ex) {
                    LOG.error("Failed for format message to specified output format", ex);
                } catch (XMLStreamException e) {
                    LOG.error("Error while building message", e);
                }
            }
        }
    }

//    protected void handleSendBack(WebSocketConnection webSocketConnection, WebSocketFrame frame,
//                                  InboundWebsocketChannelContext ctx, int clientBroadcastLevel,
//                                  String subscriberPath, WebsocketSubscriberPathManager pathManager) {
//        if (clientBroadcastLevel == 0) {
//            ctx.writeToChannel(frame);
//        } else if (clientBroadcastLevel == 1) {
//            String endpointName = WebsocketEndpointManager.getInstance()
//                    .getEndpointName(sourceHandler.getPort(), sourceHandler.getTenantDomain());
//            pathManager.broadcastOnSubscriberPath(frame, endpointName, subscriberPath);
//        } else if (clientBroadcastLevel == 2) {
//            String endpointName = WebsocketEndpointManager.getInstance()
//                    .getEndpointName(sourceHandler.getPort(), sourceHandler.getTenantDomain());
//            pathManager.exclusiveBroadcastOnSubscriberPath(frame, endpointName, subscriberPath, ctx);
//        }
//    }

    protected String messageContextToText(org.apache.axis2.context.MessageContext msgCtx) throws IOException {
        OMOutputFormat format = BaseUtils.getOMOutputFormat(msgCtx);
        MessageFormatter messageFormatter = MessageProcessorSelector.getMessageFormatter(msgCtx);
        StringWriter sw = new StringWriter();
        OutputStream out = new WriterOutputStream(sw, format.getCharSetEncoding());
        messageFormatter.writeTo(msgCtx, format, out, true);
        out.close();
        return sw.toString();
    }

    public boolean isPropertyTrue(MessageContext msgContext, String name) {
        return this.isPropertyTrue(msgContext, name, false);
    }

    public boolean isPropertyTrue(MessageContext msgContext, String name, boolean defaultVal) {
        return JavaUtils.isTrueExplicitly(msgContext.getProperty(name), defaultVal);
    }

    private ChannelFuture sendBinaryMessage(WebSocketConnection webSocketConnection, MessageContext msgContext,
                                            String defaultContentType) {

        ByteBuffer data = prepareBinaryFrame(msgContext, defaultContentType);
        return webSocketConnection.pushBinary(data);
    }

    private ByteBuffer prepareBinaryFrame(MessageContext msgContext, String defaultContentType) {

        ByteBuffer webSocketBinaryMessage = (ByteBuffer) msgContext
                .getProperty(InboundWebsocketConstants.WEBSOCKET_BINARY_MESSAGE);
        org.apache.axis2.context.MessageContext axis2MsgCtx =
                ((Axis2MessageContext) msgContext).getAxis2MessageContext();

        if (axis2MsgCtx.isPropertyTrue(InboundWebsocketConstants.IS_TCP_TRANSPORT)) {
            try {
                RelayUtils.buildMessage(axis2MsgCtx, false);
                if (defaultContentType != null && defaultContentType
                        .startsWith(InboundWebsocketConstants.BINARY)) {
                    MessageFormatter messageFormatter = BaseUtils.getMessageFormatter(axis2MsgCtx);
                    OMOutputFormat format = BaseUtils.getOMOutputFormat(axis2MsgCtx);
                    byte[] message = messageFormatter.getBytes(axis2MsgCtx, format);
                    return ByteBuffer.wrap(message);
//                    frame = new BinaryWebSocketFrame(Unpooled.copiedBuffer(message));
                }
            } catch (XMLStreamException ex) {
                LOG.error("Error while building message", ex);
            } catch (IOException ex) {
                LOG.error("Failed for format message to specified output format", ex);
            }
        }
        return webSocketBinaryMessage;
    }

    private ChannelFuture sendTextMessage(WebSocketConnection webSocketConnection, MessageContext msgContext,
                                            String defaultContentType) {

        String data = createTextFrame(msgContext, defaultContentType);
        return webSocketConnection.pushText(data);
    }

    private String createTextFrame(MessageContext msgContext, String defaultContentType) {

        TextWebSocketFrame frame = (TextWebSocketFrame) msgContext
                .getProperty(InboundWebsocketConstants.WEBSOCKET_TEXT_MESSAGE);
        org.apache.axis2.context.MessageContext axis2MsgCtx =
                ((Axis2MessageContext) msgContext).getAxis2MessageContext();
        if (axis2MsgCtx.isPropertyTrue(InboundWebsocketConstants.IS_TCP_TRANSPORT)) {
            try {
                RelayUtils.buildMessage(axis2MsgCtx, false);
                if (defaultContentType != null && defaultContentType
                        .startsWith(InboundWebsocketConstants.TEXT)) {

                    String backendMessageType = (String) axis2MsgCtx
                            .getProperty(InboundWebsocketConstants.BACKEND_MESSAGE_TYPE);
                    axis2MsgCtx.setProperty(InboundWebsocketConstants.MESSAGE_TYPE, backendMessageType);
                    return messageContextToText(axis2MsgCtx);
                }
            } catch (XMLStreamException ex) {
                LOG.error("Error while building message", ex);
            } catch (IOException ex) {
                LOG.error("Failed for format message to specified output format", ex);
            }
        }
        return frame.text();
    }

    private void terminateConnection(int statusCode, String reason) {
        webSocketConnection.terminateConnection(statusCode, reason);
        String endpointName = HttpWebsocketEndpointManager.getInstance().getEndpointName(webSocketConnection.getPort(),
                tenantDomain);
//        WebsocketSubscriberPathManager.getInstance()
//                .removeChannelContext(endpointName, subscriberPath.getPath(), wrappedContext);
    }

//    private void sendBackToClient(WebSocketConnection webSocketConnection, WebSocketFrame frame) {
////        InboundWebsocketChannelContext ctx = sourceHandler.getChannelHandlerContext();
//        int clientBroadcastLevel = sourceHandler.getClientBroadcastLevel();
//        String subscriberPath = sourceHandler.getSubscriberPath();
//        WebsocketSubscriberPathManager pathManager = WebsocketSubscriberPathManager.getInstance();
//        if (log.isDebugEnabled()) {
//            WebsocketLogUtil.printWebSocketFrame( log, frame, webSocketConnection.getChannelId(), null, false);
//        }
//        handleSendBack(webSocketConnection, frame, ctx, clientBroadcastLevel, subscriberPath, pathManager);
//    }

    public void addCloseListener(final ChannelHandlerContext targetCtx) {
        webSocketConnection.addConnectionCloseListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    if (targetCtx.channel().isActive()) {
                        targetCtx.channel().write(new CloseWebSocketFrame()).addListener(ChannelFutureListener.CLOSE);
                    }
                }
            }
        });
    }

}
