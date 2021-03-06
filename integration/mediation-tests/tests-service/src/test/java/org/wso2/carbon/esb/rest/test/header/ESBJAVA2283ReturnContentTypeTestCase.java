/*
 *Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */

package org.wso2.carbon.esb.rest.test.header;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.esb.integration.common.utils.ESBIntegrationTest;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import static org.testng.Assert.assertEquals;

public class ESBJAVA2283ReturnContentTypeTestCase extends ESBIntegrationTest {

    private Log log = LogFactory.getLog(ESBJAVA2283ReturnContentTypeTestCase.class);
    private static final int HTTP_STATUS_OK = 200;
    private static final int PORT = 8089;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {

        super.init();
    }

    @Test(groups = { "wso2.esb" }, description = "test return content type")
    public void testReturnContentType() throws Exception {

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        String contentType = "text/xml";
        server.createContext("/gettest", new MyHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
        DefaultHttpClient httpclient = new DefaultHttpClient();
        String url = "http://localhost:8480/serviceTest/test";
        HttpGet httpGet = new HttpGet(url);
        HttpResponse response = null;
        try {
            response = httpclient.execute(httpGet);
        } catch (IOException e) {
            log.error("Error Occurred while sending http get request. ", e);
        }
        log.info(response.getEntity().getContentType());
        log.info(response.getStatusLine().getStatusCode());

        assertEquals(response.getFirstHeader("Content-Type").getValue(), contentType,
                "Expected content type doesn't match");
        assertEquals(response.getStatusLine().getStatusCode(), HTTP_STATUS_OK, "response code doesn't match");

        server.stop(5);
    }

    private class MyHandler implements HttpHandler {

        public void handle(HttpExchange t) throws IOException {

            Headers h = t.getResponseHeaders();
            h.add("Content-Type", "text/xml");
            String response = "This is the test case for ESBJAVA-2283";
            t.sendResponseHeaders(HTTP_STATUS_OK, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}
