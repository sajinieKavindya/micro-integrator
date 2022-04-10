package org.wso2.sample;

import org.apache.synapse.transport.netty.listener.Testing;

public class Test implements Testing {

    @Override
    public void handshake() {

        System.out.println("Testing");
    }
}
