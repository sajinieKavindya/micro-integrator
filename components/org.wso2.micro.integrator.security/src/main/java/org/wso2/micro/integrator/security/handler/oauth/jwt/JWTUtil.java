/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
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

package org.wso2.micro.integrator.security.handler.oauth.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.synapse.MessageContext;
import org.apache.synapse.endpoints.ProxyConfigs;
import org.apache.synapse.endpoints.auth.AuthConstants;
import org.apache.synapse.endpoints.auth.AuthException;
import org.apache.synapse.endpoints.auth.oauth.OAuthUtils;
import org.json.JSONObject;
import org.wso2.micro.integrator.security.handler.oauth.HttpClientConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Base64;

public class JWTUtil {

    private static final Log log = LogFactory.getLog(JWTUtil.class);

    /**
     * This method used to retrieve JWKS keys from endpoint
     *
     * @param jwksEndpointUrl
     * @return
     * @throws IOException
     */
    public static String retrieveJWKSConfiguration(String jwksEndpoint, HttpClientConfiguration httpClientConfiguration,
                                                   MessageContext messageContext)
            throws IOException, AuthException {

        ProxyConfigs proxyConfigs = new ProxyConfigs();
        proxyConfigs.setProxyEnabled(httpClientConfiguration.isProxyEnabled());
        proxyConfigs.setProxyHost(httpClientConfiguration.getProxyHost());
        proxyConfigs.setProxyPort(String.valueOf(httpClientConfiguration.getProxyPort()));
        proxyConfigs.setProxyProtocol(httpClientConfiguration.getProxyProtocol());
        proxyConfigs.setProxyUsername(httpClientConfiguration.getProxyUsername());
        proxyConfigs.setProxyPassword(Arrays.toString(httpClientConfiguration.getProxyPassword()));
        try (CloseableHttpClient httpClient = OAuthUtils.getSecureClient(jwksEndpoint, messageContext,
                httpClientConfiguration.getConnectionTimeout(), httpClientConfiguration.getRequestTimeout(),
                httpClientConfiguration.getSocketTimeout(), proxyConfigs, null)) {
            HttpGet httpGet = new HttpGet(jwksEndpoint);
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                if (response.getStatusLine().getStatusCode() == 200) {
                    HttpEntity entity = response.getEntity();
                    try (InputStream content = entity.getContent()) {
                        return IOUtils.toString(content);
                    }
                } else {
                    return null;
                }
            }
        }
    }

    /**
     * Verify the JWT token signature.
     *
     * @param jwt SignedJwt Token
     * @param publicKey      public certificate
     * @return whether the signature is verified or or not
     */
    public static boolean verifyTokenSignature(SignedJWT jwt, RSAPublicKey publicKey) {

        JWSAlgorithm algorithm = jwt.getHeader().getAlgorithm();
        if ((JWSAlgorithm.RS256.equals(algorithm) || JWSAlgorithm.RS512.equals(algorithm) ||
                JWSAlgorithm.RS384.equals(algorithm)) || JWSAlgorithm.PS256.equals(algorithm)) {
            try {
                JWSVerifier jwsVerifier = new RSASSAVerifier(publicKey);
                return jwt.verify(jwsVerifier);
            } catch (JOSEException e) {
                log.error("Error while verifying JWT signature", e);
                return false;
            }
        } else {
            log.error("Public key is not a RSA");
            return false;
        }
    }
}
