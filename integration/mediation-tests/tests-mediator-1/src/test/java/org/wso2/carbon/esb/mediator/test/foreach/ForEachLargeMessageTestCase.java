/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.esb.mediator.test.foreach;

import org.apache.axiom.om.OMElement;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.esb.integration.common.utils.CarbonLogReader;
import org.wso2.esb.integration.common.utils.ESBIntegrationTest;

/**
 * Tests sending different number of large messages through foreach mediator
 */

public class ForEachLargeMessageTestCase extends ESBIntegrationTest {

    private CarbonLogReader carbonLogReader;

    @BeforeClass
    public void setEnvironment() throws Exception {
        init();
        carbonLogReader = new CarbonLogReader();
        carbonLogReader.start();
    }

    @Test(groups = "wso2.esb",
            description = "Tests large message in small number 5", enabled = false)
    public void testSmallNumbers() throws Exception {
        carbonLogReader.clearLogs();
        OMElement response;
        for (int i = 0; i < 5; i++) {
            response = axis2Client
                    .sendCustomQuoteRequest(getProxyServiceURLHttp("foreachLargeMessageTestProxy"), null, "IBM");
            Assert.assertNotNull(response);
            Assert.assertTrue(response.toString().contains("IBM"), "Incorrect symbol in response");
        }
        if (carbonLogReader.checkForLog("foreach = in", DEFAULT_TIMEOUT)) {
            if (!carbonLogReader.checkForLog("IBM", DEFAULT_TIMEOUT)) {
                Assert.fail("Incorrect message entered ForEach scope. Could not find symbol IBM ..");
            }
        }
        Assert.assertTrue(carbonLogReader.checkForLog("foreach = in", DEFAULT_TIMEOUT, 5),
                          "Count of messages entered ForEach scope is incorrect");
    }

    @Test(groups = "wso2.esb",
            description = "Tests large message in large number 10", enabled = false)
    public void testLargeNumbers() throws Exception {
        carbonLogReader.clearLogs();
        OMElement response;
        for (int i = 0; i < 10; i++) {
            response = axis2Client
                    .sendCustomQuoteRequest(getProxyServiceURLHttp("foreachLargeMessageTestProxy"), null, "SUN");
            Assert.assertNotNull(response);
            Assert.assertTrue(response.toString().contains("SUN"), "Incorrect symbol in response");
        }
        if (carbonLogReader.checkForLog("foreach = in", DEFAULT_TIMEOUT)) {
            if (!carbonLogReader.checkForLog("SUN", DEFAULT_TIMEOUT)) {
                Assert.fail("Incorrect message entered ForEach scope. Could not find symbol SUN ..");
            }
        }
        Assert.assertTrue(carbonLogReader.checkForLog("foreach = in", DEFAULT_TIMEOUT, 10),
                          "Count of messages entered ForEach scope is incorrect");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        carbonLogReader.stop();
    }

}
