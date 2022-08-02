package org.wso2.micro.integrator.inbound.websocket.transport.utils;

public class WebSocketException extends RuntimeException {

    public WebSocketException(Throwable ex, String errorCode) {
        super(errorCode + ":" + WebSocketUtil.getErrorMessage(ex), ex);
    }

    public WebSocketException(String message, String errorCode) {
        super(errorCode + ":" + message);
    }
}
