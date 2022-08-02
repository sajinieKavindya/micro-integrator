package org.wso2.micro.integrator.inbound.websocket.transport.utils;

import io.netty.channel.ChannelFuture;
import org.apache.commons.logging.Log;
import org.wso2.carbon.inbound.endpoint.protocol.httpwebsocket.management.HttpWebsocketEndpointManager;
import org.wso2.transport.http.netty.contract.websocket.WebSocketConnection;

public class WebSocketUtil {

    public static void terminateConnection(WebSocketConnection webSocketConnection,
                                           WebSocketConstants.WebSocketError webSocketError, String tenantDomain) {
        terminateConnection(webSocketConnection, webSocketError.getCloseCode(), webSocketError.getCodeName(),
                tenantDomain);
    }

    public static void terminateConnection(WebSocketConnection webSocketConnection, int statusCode, String reason,
                                           String tenantDomain) {
        webSocketConnection.terminateConnection(statusCode, reason);
        String endpointName = HttpWebsocketEndpointManager.getInstance().getEndpointName(webSocketConnection.getPort(),
                tenantDomain);
//        WebsocketSubscriberPathManager.getInstance()
//                .removeChannelContext(endpointName, subscriberPath.getPath(), wrappedContext);
    }

    public static void finishConnectionClosureIfOpen(WebSocketConnection webSocketConnection, int closeCode) {

        if (webSocketConnection.isOpen()) {
            ChannelFuture finishFuture;
            if (closeCode == WebSocketConstants.STATUS_CODE_FOR_NO_STATUS_CODE_PRESENT) {
                finishFuture = webSocketConnection.finishConnectionClosure();
            } else {
                finishFuture = webSocketConnection.finishConnectionClosure(closeCode, null);
            }
            finishFuture.addListener(closeFuture -> WebSocketUtil.setListenerOpenField(connectionInfo));
        }
    }

    public static WebSocketException getWebSocketError(String msg, Throwable throwable, String errorCode) {
        WebSocketException exception;
        String message = errorCode + ": " + msg;
        if (throwable != null) {
            exception = new WebSocketException(throwable, errorCode);
        } else {
            exception = new WebSocketException(message, errorCode);
        }
        return exception;
    }

    public static String getErrorMessage(Throwable err) {
        if (err.getMessage() == null) {
            return "Unexpected error occurred";
        }
        return err.getMessage();
    }

    public static void printLog(Log log, WebSocketConstants.LogLevel logLevel, String channelId, Throwable e,
                                String message) {
        switch (logLevel) {
            case INFO:
                log.info(message);
                break;
            case WARN:
                log.warn(message);
                break;
            case DEBUG:
                if (log.isDebugEnabled()) {
                    log.debug(message + " on channel: " + channelId
                            + ", on Thread: " + Thread.currentThread().getName() + "," + Thread.currentThread().getId());
                }
                break;
            case ERROR:
                log.error(message, e);
                break;
            case FATAL:
                log.fatal(message, e);
                break;
            case TRACE:
                if (log.isTraceEnabled()) {
                    log.trace(message + " on channel: " + channelId
                            + ", on Thread: " + Thread.currentThread().getName() + "," + Thread.currentThread().getId());
                }
                break;
            default:
                // log nothing
        }
    }
}
