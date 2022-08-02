package org.wso2.micro.integrator.inbound.websocket.transport.utils;

public class WebSocketConstants {

    public static final String WEBSOCKET_SOURCE_HANDSHAKE_PRESENT = "websocket.source.handshake.present";

    public static final String CLIENT_ID = "clientId";

    public static final String WS = "ws";
    public static final String WSS = "wss";
    public static final String HTTPWS = "httpws";
    public static final String HTTPSWSS = "httpswss";

    /**
     * Specifies the error code for webSocket module.
     */
    public enum ErrorCode {
        ConnectionClosureError("ConnectionClosureError"),
        InvalidHandshakeError("InvalidHandshakeError"),
        PayloadTooLargeError("PayloadTooLargeError"),
        ProtocolError("ProtocolError"),
        ConnectionError("ConnectionError"),
        InvalidContinuationFrameError("InvalidContinuationFrameError"),
        HandshakeTimedOut("HandshakeTimedOut"),
        ReadTimedOutError("ReadTimedOutError"),
        SslError("SslError"),
        AuthzError("AuthzError"),
        AuthnError("AuthnError"),
        Error("Error");

        private String errorCode;

        ErrorCode(String errorCode) {
            this.errorCode = errorCode;
        }

        public String errorCode() {
            return errorCode;
        }

    }

    public enum LogLevel {
        FATAL,
        ERROR,
        WARN,
        INFO,
        DEBUG,
        TRACE;
    }

    public enum WebSocketError {

        CLOSE_NORMAL(1000, "CLOSE_NORMAL"), // normal socket shut down
        CLOSE_GOING_AWAY(1001, "CLOSE_GOING_AWAY"), // browser tab closing
        CLOSE_PROTOCOL_ERROR(1002, "CLOSE_PROTOCOL_ERROR"), // endpoint received a malformed frame
        CLOSE_UNSUPPORTED(1003, "CLOSE_UNSUPPORTED"), // endpoint received unsupported frame
        CLOSED_NO_STATUS(1005, "CLOSED_NO_STATUS"), // expected close status, received none
        CLOSE_ABNORMAL(1006, "CLOSE_ABNORMAL"), // no close code frame has been received
        UNSUPPORTED_PAYLOAD(1007, "Unsupported payload"), // endpoint received an inconsistent message
        POLICY_VIOLATION(1008, "Policy violation"), // generic code used for situations other than 1003 and 1009
        CLOSE_TOO_LARGE(1009, "CLOSE_TOO_LARGE"), // endpoint won’t process large frame
        MANDATORY_EXTENSION(1010, "Mandatory extension"), // client wanted an extension which server did not negotiate
        SERVER_ERROR(1011, "Server error"), // internal server error while operating
        SERVER_RESTART(1012, "Service restart"), // service is restarting
        TRY_AGAIN_LATER(1013, "Try again later"), // temporary server condition forced blocking client’s request
        BAD_GATEWAY(1014, "Bad gateway"), // server acting as gateway received an invalid response
        TLS_HANDSHAKE_FAIL(1015, "TLS handshake fail"); //transport Layer Security handshake failure

        private final int closeCode;
        private final String codeName;

        WebSocketError(int closeCode, String codeName) {
            this.closeCode = closeCode;
            this.codeName = codeName;
        }

        public int getCloseCode() {
            return closeCode;
        }

        public String getCodeName() {
            return codeName;
        }
    }
}
