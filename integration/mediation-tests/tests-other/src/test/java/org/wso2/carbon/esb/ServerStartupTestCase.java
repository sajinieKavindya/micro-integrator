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
package org.wso2.carbon.esb;

import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.esb.integration.common.utils.CarbonLogReader;
import org.wso2.esb.integration.common.utils.ESBIntegrationTest;

public class ServerStartupTestCase extends ESBIntegrationTest {

    private CarbonLogReader carbonLogReader;

    @BeforeSuite(alwaysRun = true)
    public void initialize() {
        carbonLogReader = new CarbonLogReader();
    }

    @Test(groups = { "wso2.esb" }, description = "verify server startup errors", enabled = false)
    @SetEnvironment(executionEnvironments = { ExecutionEnvironment.STANDALONE })
    public void testVerifyLogs() throws InterruptedException {
        carbonLogReader.start();
        boolean status = carbonLogReader.checkForLog("ERROR", DEFAULT_TIMEOUT);
        carbonLogReader.stop();
        Assert.assertFalse(status, "Server started with errors.");
    }

}
