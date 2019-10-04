/*
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.esb.mediators.store;

import java.util.concurrent.TimeUnit;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.extensions.servers.httpserver.SimpleHttpClient;
import org.wso2.esb.integration.common.utils.CarbonLogReader;
import org.wso2.esb.integration.common.utils.ESBIntegrationTest;
import static org.testng.Assert.assertFalse;

public class ESBJAVA4470StoreMediatorEmptyOMArraySerializeException extends ESBIntegrationTest {

    @BeforeClass(alwaysRun = true)
    protected void init() throws Exception {
        super.init();
    }

    @Test(groups = {"wso2.esb"}, description = "Test if Store Mediator Serialize Empty OM Array without Exception",
          enabled = false)
    public void testStoreMediatorEmptyOMArrayPropertySerialize() throws Exception {
        CarbonLogReader carbonLogReader = new CarbonLogReader();
        carbonLogReader.start();
        String url = getApiInvocationURL("SerializeProperty") + "/serializeOMArray";
        SimpleHttpClient httpClient = new SimpleHttpClient();
        httpClient.doGet(url, null);
        TimeUnit.SECONDS.sleep(10);

        boolean logFound = carbonLogReader.checkForLog("Index: 0, Size: 0", DEFAULT_TIMEOUT) &&
                carbonLogReader.checkForLog("ERROR", DEFAULT_TIMEOUT);
        assertFalse(logFound, "Exception thrown when serializing OM Array property by Store Mediator");
        carbonLogReader.stop();
    }

}
