package org.wso2.carbon.inbound.endpoint.protocol.grpc.util;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

import java.util.concurrent.atomic.AtomicBoolean;

public class PausingInterceptor implements ServerInterceptor {
    private final AtomicBoolean paused = new AtomicBoolean(false);

    public void pause() { paused.set(true); }
    public void resume() { paused.set(false); }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        if (paused.get()) {
            call.close(Status.UNAVAILABLE.withDescription("Server temporarily paused"), new Metadata());
            return new ServerCall.Listener<ReqT>() {}; // reject new requests
        }
        return next.startCall(call, headers);
    }
}
