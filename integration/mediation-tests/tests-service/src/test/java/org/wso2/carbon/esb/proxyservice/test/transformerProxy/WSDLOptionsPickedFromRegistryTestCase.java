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
package org.wso2.carbon.esb.proxyservice.test.transformerProxy;

import org.apache.axiom.om.OMElement;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceExceptionException;
import org.wso2.esb.integration.common.clients.registry.ResourceAdminServiceClient;
import org.wso2.esb.integration.common.utils.ESBIntegrationTest;

import java.net.URL;
import java.rmi.RemoteException;
import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPathExpressionException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class WSDLOptionsPickedFromRegistryTestCase extends ESBIntegrationTest {

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
    }

    @Test(groups = "wso2.esb", description = "- Transformer proxy" + "- Publish WSDL Options - Picked from registry")
    public void testTransformerProxy() throws Exception {

        OMElement response = axis2Client
                .sendCustomQuoteRequest(getProxyServiceURLHttp("wsdlOptionsFromRegTransformerProxy"), null, "WSO2");

        assertNotNull(response, "Fault response message null");

        assertNotNull(response.getQName().getLocalPart(), "Fault response null localpart");
        assertEquals(response.getQName().getLocalPart(), "CheckPriceResponse", "Fault localpart mismatched");

        assertNotNull(response.getFirstElement().getQName().getLocalPart(), " Fault response null localpart");
        assertEquals(response.getFirstElement().getQName().getLocalPart(), "Code", "Fault localpart mismatched");
        assertEquals(response.getFirstElement().getText(), "WSO2", "Fault value mismatched");

        assertNotNull(response.getFirstChildWithName(new QName("http://services.samples/xsd", "Price")),
                "Fault response null localpart");

    }

}
