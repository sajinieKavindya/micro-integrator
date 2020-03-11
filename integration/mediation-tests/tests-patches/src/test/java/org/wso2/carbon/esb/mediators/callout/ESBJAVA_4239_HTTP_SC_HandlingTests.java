/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.esb.mediators.callout;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.esb.integration.common.utils.CarbonLogReader;
import org.wso2.esb.integration.common.utils.ESBIntegrationTest;
import org.wso2.esb.integration.common.utils.common.TestConfigurationProvider;
import org.wso2.esb.integration.common.utils.servers.SimpleSocketServer;

import java.io.File;

import static org.testng.Assert.fail;

public class ESBJAVA_4239_HTTP_SC_HandlingTests extends ESBIntegrationTest {

    private CarbonLogReader carbonLogReader;
    private SimpleSocketServer simpleSocketServer;

    @BeforeClass(alwaysRun = true)
    public void deployService() throws Exception {
        super.init();
        int port = 9045;
        String expectedResponse =
                "HTTP/1.1 404 Not Found\r\nServer: testServer\r\n" + "Content-Type: text/xml; charset=UTF-8\r\n"
                        + "Transfer-Encoding: chunked\r\n" + "\r\n" + "\"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<test></test>";
        simpleSocketServer = new SimpleSocketServer(port, expectedResponse);
        carbonLogReader = new CarbonLogReader();
        carbonLogReader.start();
    }

    @Test(groups = "wso2.esb", description = "Test whether response HTTP status code getting correctly after callout "
            + "mediator successfully execute")
    public void testHttpStatusCodeGettingSuccessfully() throws Exception {
        String endpoint = getProxyServiceURLHttp("TestCalloutHTTP_SC");
        String soapRequest =
                TestConfigurationProvider.getResourceLocation() + "artifacts" + File.separator + "ESB" + File.separator
                        + "mediatorconfig" + File.separator + "callout" + File.separator + "SOAPRequestWithHeader.xml";
        log.info("TestConfigurationProvider.getResourceLocation() ::::::::::::::: " + TestConfigurationProvider.getResourceLocation() );
        log.info("Soap Request File ::::::::::::::: " + soapRequest );
        log.info("Soap Request File Exists ::::::::::::::: " + new File(soapRequest).exists() );
        File input = new File(soapRequest);
        PostMethod post = new PostMethod(endpoint);
        RequestEntity entity = new FileRequestEntity(input, "text/xml");
        post.setRequestEntity(entity);
        post.setRequestHeader("SOAPAction", "getQuote");
        HttpClient httpClient = new HttpClient();
        boolean errorLog = false;

        try {
            simpleSocketServer.start();
            httpClient.executeMethod(post);
            errorLog = carbonLogReader.checkForLog("STATUS-Fault", DEFAULT_TIMEOUT) && carbonLogReader.
                    checkForLog("404 Error: Not Found", DEFAULT_TIMEOUT);
            carbonLogReader.stop();
        } finally {
            post.releaseConnection();
        }

        if (!errorLog) {
            fail("Response HTTP code not return successfully.");
        }
    }

}
