/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.micro.integrator.crypto.impl;

import org.testng.annotations.Test;
import org.wso2.carbon.crypto.api.InternalCryptoProvider;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class DefaultCryptoServiceTest {

    @Test
    public void testProviderSelection() throws Exception {

        DefaultCryptoService defaultCryptoService = new DefaultCryptoService();

        InternalCryptoProvider mockCryptoProvider = new SimpleCryptoProvider();
        defaultCryptoService.registerInternalCryptoProvider(mockCryptoProvider);

        assertTrue(defaultCryptoService.areInternalCryptoProvidersAvailable());

        assertEquals(defaultCryptoService.getMostSuitableInternalProvider(), mockCryptoProvider);

        defaultCryptoService.unregisterAllInternalCryptoProviders();

        assertFalse(defaultCryptoService.areInternalCryptoProvidersAvailable());
    }
}
