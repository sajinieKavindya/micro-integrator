package org.wso2.micro.integrator.inbound.websocket.transport.utils;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.description.InOutAxisOperation;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.api.rest.RestRequestHandler;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.MessageContextCreatorForAxis2;
import org.apache.synapse.inbound.InboundEndpoint;
import org.apache.synapse.inbound.InboundEndpointConstants;
import org.wso2.carbon.inbound.endpoint.protocol.websocket.InboundWebsocketConstants;
import org.wso2.micro.integrator.inbound.websocket.transport.sender.InboundWebSocketResponseSender;
import org.wso2.transport.http.netty.contract.websocket.WebSocketConnection;
import org.wso2.transport.http.netty.message.HttpCarbonRequest;

import java.util.List;
import java.util.Map;

import static org.wso2.carbon.inbound.endpoint.common.Constants.SUPER_TENANT_DOMAIN_NAME;
import static org.wso2.carbon.inbound.endpoint.common.Constants.TENANT_DOMAIN;

public class MessageUtil {

    public static org.apache.synapse.MessageContext getSynapseMessageContext(
            org.apache.axis2.context.MessageContext axis2MsgCtx, WebSocketConnection webSocketConnection,
            String tenantDomain) throws AxisFault {

        org.apache.synapse.MessageContext synCtx = createSynapseMessageContext(axis2MsgCtx);

        synCtx.setProperty(SynapseConstants.IS_INBOUND, true);
        axis2MsgCtx.setProperty(SynapseConstants.IS_INBOUND, true);

        InboundWebSocketResponseSender responseSender = new InboundWebSocketResponseSender(tenantDomain,
                webSocketConnection);
        synCtx.setProperty(InboundEndpointConstants.INBOUND_ENDPOINT_RESPONSE_WORKER, responseSender);
        axis2MsgCtx.setProperty(InboundEndpointConstants.INBOUND_ENDPOINT_RESPONSE_WORKER, responseSender);

        synCtx.setProperty(InboundWebsocketConstants.WEBSOCKET_SOURCE_CHANNEL_IDENTIFIER,
                webSocketConnection.getChannelId());
        axis2MsgCtx.setProperty(InboundWebsocketConstants.WEBSOCKET_SOURCE_CHANNEL_IDENTIFIER,
                        webSocketConnection.getChannelId());

//        synCtx.setProperty(InboundWebsocketConstants.WEBSOCKET_SOURCE_HANDLER_CONTEXT,
//                wrappedContext.getChannelHandlerContext());
//        axis2MsgCtx.setProperty(InboundWebsocketConstants.WEBSOCKET_SOURCE_HANDLER_CONTEXT,
//                        wrappedContext.getChannelHandlerContext());
//        if (outflowDispatchSequence != null) {
//            synCtx.setProperty(InboundWebsocketConstants.WEBSOCKET_OUTFLOW_DISPATCH_SEQUENCE, outflowDispatchSequence);
//            axis2MsgCtx.setProperty(InboundWebsocketConstants.WEBSOCKET_OUTFLOW_DISPATCH_SEQUENCE,
//                            outflowDispatchSequence);
//        }
//        if (outflowErrorSequence != null) {
//            synCtx.setProperty(InboundWebsocketConstants.WEBSOCKET_OUTFLOW_DISPATCH_FAULT_SEQUENCE,
//                    outflowErrorSequence);
//            axis2MsgCtx.setProperty(InboundWebsocketConstants.WEBSOCKET_OUTFLOW_DISPATCH_FAULT_SEQUENCE,
//                            outflowErrorSequence);
//        }
//        synCtx.setProperty(InboundWebsocketConstants.WEBSOCKET_SUBSCRIBER_PATH, subscriberPath.toString());
        return synCtx;
    }

    public static org.apache.synapse.MessageContext createSynapseMessageContext(org.apache.axis2.context.MessageContext axis2MsgCtx)
            throws AxisFault {
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

    public static void populatePropertiesOnSourceHandshake(org.apache.axis2.context.MessageContext axis2Context,
                                                           org.apache.synapse.MessageContext synCtx,
                                                           HttpCarbonRequest httpCarbonRequest) {

        List<Map.Entry<String, String>> httpHeaders = httpCarbonRequest.getHeaders().entries();

        for (Map.Entry<String, String> entry : httpHeaders) {
            synCtx.setProperty(entry.getKey(), entry.getValue());
            axis2Context.setProperty(entry.getKey(), entry.getValue());
        }

        synCtx.setProperty(WebSocketConstants.WEBSOCKET_SOURCE_HANDSHAKE_PRESENT, Boolean.TRUE);
        axis2Context.setProperty(WebSocketConstants.WEBSOCKET_SOURCE_HANDSHAKE_PRESENT, Boolean.TRUE);
    }

    public static void injectForMediation(org.apache.synapse.MessageContext synCtx,
                                          org.apache.axis2.context.MessageContext axis2Context,
                                          WebSocketConnection webSocketConnection,
                                          HttpCarbonRequest httpCarbonRequest) {
//        SequenceMediator faultSequence = getFaultSequence(synCtx, endpoint);
//        MediatorFaultHandler mediatorFaultHandler = new MediatorFaultHandler(faultSequence);
//        synCtx.pushFaultHandler(mediatorFaultHandler);
//        if (log.isDebugEnabled()) {
//            log.debug("injecting message to sequence : " + endpoint.getInjectingSeq());
//        }
//        synCtx.setProperty("inbound.endpoint.name", endpoint.getName());
//        synCtx.setProperty(ApiConstants.API_CALLER, endpoint.getName());

//        boolean isProcessed;
//            org.apache.axis2.context.MessageContext msgCtx = ((Axis2MessageContext)synCtx).getAxis2MessageContext();
        axis2Context.setIncomingTransportName(getScheme(webSocketConnection));
        axis2Context.setProperty(Constants.Configuration.TRANSPORT_IN_URL, httpCarbonRequest.getRequestUrl());
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

    public static String getScheme(WebSocketConnection webSocketConnection) {
        if (webSocketConnection.isSecure()) {
            return WebSocketConstants.HTTPSWSS;
        }
        return WebSocketConstants.HTTPWS;
    }
}
