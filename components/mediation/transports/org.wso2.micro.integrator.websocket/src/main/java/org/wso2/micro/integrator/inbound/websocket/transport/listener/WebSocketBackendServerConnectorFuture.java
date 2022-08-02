package org.wso2.micro.integrator.inbound.websocket.transport.listener;

import org.wso2.micro.integrator.websocket.transport.WebSocketBackendConnectorListener;
import org.wso2.transport.http.netty.contract.websocket.WebSocketConnection;

public interface WebSocketBackendServerConnectorFuture {

    WebSocketBackendServerConnectorFuture setHandshakeListener(WebSocketBackendConnectorListener var1);

    void notifySuccess(WebSocketConnection var1);

    void notifyError(Throwable var1);
}
