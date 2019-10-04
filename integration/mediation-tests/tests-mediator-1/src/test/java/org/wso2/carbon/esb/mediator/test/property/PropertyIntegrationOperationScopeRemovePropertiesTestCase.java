/*
 *Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.esb.mediator.test.property;

import org.apache.axiom.om.OMElement;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.esb.integration.common.utils.CarbonLogReader;
import org.wso2.esb.integration.common.utils.ESBIntegrationTest;
import static org.testng.Assert.assertTrue;

/**
 * This test case tests whether the removing of properties
 * in the operation scope is working fine.
 */

public class PropertyIntegrationOperationScopeRemovePropertiesTestCase extends ESBIntegrationTest {

    private CarbonLogReader carbonLogReader;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
        carbonLogReader = new CarbonLogReader();
        carbonLogReader.start();
    }

    @Test(groups = "wso2.esb", description = "Remove action as \"value\" and type Integer (operation scope)",
          enabled = false)
    public void testIntVal() throws Exception {
        carbonLogReader.clearLogs();
        OMElement response = axis2Client
                .sendSimpleStockQuoteRequest(getProxyServiceURLHttp("propertyIntOperationRemoveTestProxy"), null,
                        "Random Symbol");
        assertTrue(response.toString().contains("Property Set and Removed"), "Proxy Invocation Failed!");
        assertTrue(isMatchFound("symbol = 123"), "Integer Property Not Either Set or Removed in the Axis2 scope!!");
    }

    @Test(groups = "wso2.esb", description = "Remove action as \"value\" and type String (operation scope)",
          enabled = false)
    public void testStringVal() throws Exception {
        carbonLogReader.clearLogs();
        OMElement response = axis2Client
                .sendSimpleStockQuoteRequest(getProxyServiceURLHttp("propertyStringOperationRemoveTestProxy"), null,
                        "Random Symbol");
        assertTrue(response.toString().contains("Property Set and Removed"), "Proxy Invocation Failed!");
        assertTrue(isMatchFound("symbol = WSO2 Lanka"),
                "String Property Not Either Set or Removed in the Axis2 scope!!");
    }

    @Test(groups = "wso2.esb", description = "Remove action as \"value\" and type Float (operation scope)",
          enabled = false)
    public void testFloatVal() throws Exception {
        carbonLogReader.clearLogs();
        OMElement response = axis2Client
                .sendSimpleStockQuoteRequest(getProxyServiceURLHttp("propertyFloatOperationRemoveTestProxy"), null,
                        "Random Symbol");
        assertTrue(response.toString().contains("Property Set and Removed"), "Proxy Invocation Failed!");
        assertTrue(isMatchFound("symbol = 123.123"), "Float Property Not Either Set or Removed in the Axis2 scope!!");
    }

    @Test(groups = "wso2.esb", description = "Remove action as \"value\" and type Long (operation scope)",
          enabled = false)
    public void testLongVal() throws Exception {
        carbonLogReader.clearLogs();
        OMElement response = axis2Client
                .sendSimpleStockQuoteRequest(getProxyServiceURLHttp("propertyLongOperationRemoveTestProxy"), null,
                        "Random Symbol");
        assertTrue(response.toString().contains("Property Set and Removed"), "Proxy Invocation Failed!");
        assertTrue(isMatchFound("symbol = 123123123"), "Long Property Not Either Set or Removed in the Axis2 scope!!");
    }

    @Test(groups = "wso2.esb", description = "Remove action as \"value\" and type Short (operation scope)",
          enabled = false)
    public void testShortVal() throws Exception {
        carbonLogReader.clearLogs();
        OMElement response = axis2Client
                .sendSimpleStockQuoteRequest(getProxyServiceURLHttp("propertyShortOperationRemoveTestProxy"), null,
                        "Random Symbol");
        assertTrue(response.toString().contains("Property Set and Removed"), "Proxy Invocation Failed!");
        assertTrue(isMatchFound("symbol = 12"), "Short Property Not Either Set or Removed in the Axis2 scope!!");
    }

    @Test(groups = "wso2.esb", description = "Remove action as \"value\" and type OM (operation scope)",
          enabled = false)
    public void testOMVal() throws Exception {
        carbonLogReader.clearLogs();
        OMElement response = axis2Client
                .sendSimpleStockQuoteRequest(getProxyServiceURLHttp("propertyOMOperationRemoveTestProxy"), null,
                        "Random Symbol");
        assertTrue(response.toString().contains("Property Set and Removed"), "Proxy Invocation Failed!");
        assertTrue(isMatchFound("symbol = OMMMMM"), "OM Property Not Either Set or Removed in the Axis2 scope!!");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        carbonLogReader.stop();
    }

    /**
     * The method that checks whether the particular
     * match string is available in the sysytem logs
     */
    private boolean isMatchFound(String matchStr) throws InterruptedException {
        boolean isSet = carbonLogReader.checkForLog(matchStr, DEFAULT_TIMEOUT) &&
                carbonLogReader.checkForLog("symbol = null", DEFAULT_TIMEOUT);
        return isSet;
    }
}
