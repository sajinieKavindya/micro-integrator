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

package org.wso2.micro.integrator.security.handler.oauth;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2Sender;
import org.apache.synapse.rest.AbstractHandler;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.config.mapper.ConfigParser;
import org.wso2.micro.integrator.security.handler.oauth.jwt.JWTValidationInfo;
import org.wso2.micro.integrator.security.handler.oauth.jwt.JWTValidator;

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.cache.Cache;

public class OAuthAuthenticationHandler extends AbstractHandler implements ManagedLifecycle {

    private static final Log log = LogFactory.getLog(OAuthAuthenticationHandler.class);

    private static final String DEFAULT_SECURITY_HEADER = HttpHeaders.AUTHORIZATION;
    private String authorizationHeader = DEFAULT_SECURITY_HEADER;
    private List<String> trustedIssuerList;
    private TokenRevocationChecker tokenRevocationChecker;
    private int tokenCacheTimeout;
    private String jwksEndpoint;
    private HttpClientConfiguration httpClientConfiguration;

    @Override
    public void init(SynapseEnvironment synapseEnvironment) {
        HttpClientConfiguration.Builder builder  = new HttpClientConfiguration.Builder();
        int connectionTimeout = Integer.parseInt(
                (String) ConfigParser.getParsedConfigs().get(OAuthConstants.HTTP_CLIENT_CONNECTION_TIMEOUT));
        int socketTimeout = Integer.parseInt(
                (String) ConfigParser.getParsedConfigs().get(OAuthConstants.HTTP_CLIENT_SOCKET_TIMEOUT));
        int requestTimeout = Integer.parseInt(
                (String) ConfigParser.getParsedConfigs().get(OAuthConstants.HTTP_CLIENT_REQUEST_TIMEOUT));

        builder.withConnectionParams(connectionTimeout, requestTimeout, socketTimeout);

        boolean proxyEnabled = Boolean.parseBoolean(
                (String) ConfigParser.getParsedConfigs().get(OAuthConstants.PROXY_ENABLE));

        if (proxyEnabled) {
            String proxyHost = (String) ConfigParser.getParsedConfigs().get(OAuthConstants.PROXY_HOST);
            int proxyPort = Integer.parseInt((String) ConfigParser.getParsedConfigs().get(OAuthConstants.PROXY_PORT));
            String proxyUsername = (String) ConfigParser.getParsedConfigs().get(OAuthConstants.PROXY_USERNAME);
            String proxyPassword = (String) ConfigParser.getParsedConfigs().get(OAuthConstants.PROXY_PASSWORD);
            String proxyProtocol = (String) ConfigParser.getParsedConfigs().get(OAuthConstants.PROXY_PROTOCOL);
            builder.withProxy(proxyHost, proxyPort, proxyUsername, proxyPassword, proxyProtocol);
        }

        httpClientConfiguration = builder.build();

    }

    @Override
    public void destroy() {

    }

    @Override
    public boolean handleRequest(MessageContext messageContext) {

        try {
            boolean isJWT = false;
            SignedJWTInfo signedJWTInfo = null;

            Map headers = (Map) ((Axis2MessageContext) messageContext).getAxis2MessageContext().
                    getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);

            if (headers != null) {
                String authHeader = (String) headers.get(getSecurityHeader());
                String[] elements = authHeader.split(OAuthConstants.CONSUMER_KEY_SEGMENT_DELIMITER);

                if (OAuthConstants.BEARER.equals(elements[0])) {

                    String accessToken = elements[1];

                    // TODO: I think this removal should be done at the end of the authentication logic.
                    if (OAuthUtil.isRemoveOAuthHeadersFromOutMessage()) {
                        headers.remove(getSecurityHeader());
                    }

                    //Initial guess of a JWT token using the presence of a DOT.
                    if (StringUtils.isEmpty(accessToken) || !accessToken.contains(OAuthConstants.DOT)) {
                        log.debug("The provided credential does not follow the JWT format");
                    }

                    try {
                        String[] JWTElements = accessToken.split("\\.");

                        if (JWTElements.length != 3) {
                            log.debug("The provided credential does not follow the JWT format");
                        }

                        signedJWTInfo = getSignedJwtInfo(accessToken);
                        String issuer = signedJWTInfo.getJwtClaimsSet().getIssuer();

                        if (StringUtils.isNotEmpty(issuer) && trustedIssuerList.contains(issuer)) {
                            if (log.isDebugEnabled()) {
                                log.debug("Issuer: " + issuer + "found for authenticate token "
                                        + OAuthUtil.getMaskedToken(accessToken));
                            }
                            isJWT = true;
                        } else {
                            if (log.isDebugEnabled()) {
                                log.debug("Iss claim not found for accessToken "
                                        + OAuthUtil.getMaskedToken(accessToken));
                            }
                            throw new OAuthSecurityException(OAuthConstants.API_AUTH_INVALID_CREDENTIALS, 
                                    OAuthConstants.API_AUTH_INVALID_CREDENTIALS_MESSAGE);
                        }
                    } catch (ParseException e) {
                        if (log.isDebugEnabled()) {
                            log.debug("Not a JWT token. Failed to decode the token header.", e);
                        }
                    }
                }
            }
            
            if (isJWT) {
                if (log.isDebugEnabled()) {
                    log.debug("Authentication started for JWT tokens");
                }

                JWTValidator jwtValidator = new JWTValidator(trustedIssuerList, tokenRevocationChecker, jwksEndpoint,
                        httpClientConfiguration);
                jwtValidator.authenticate(signedJWTInfo, messageContext);

            }
        } catch (OAuthSecurityException e) {
            handleAuthFailure(messageContext, e);
        }

        return false;
    }

    @Override
    public boolean handleResponse(MessageContext messageContext) {

        return false;
    }

    public static void sendFault(MessageContext messageContext, int status) {
        org.apache.axis2.context.MessageContext axis2MC = ((Axis2MessageContext) messageContext).
                getAxis2MessageContext();
        axis2MC.setProperty(NhttpConstants.HTTP_SC, status);
        Axis2Sender.sendBack(messageContext);
    }

    private String getSecurityHeader() {

        return authorizationHeader;
    }

    /**
     * To set the Authorization Header.
     *
     * @param authorizationHeader the Authorization Header of the API request.
     */
    public void setAuthorizationHeader(String authorizationHeader) {
        if (authorizationHeader != null) {
            this.authorizationHeader = authorizationHeader;
            return;
        }
        Object authorizationHeaderConfig = ConfigParser.getParsedConfigs().get(OAuthConstants.AUTHORIZATION_HEADER);
        if (authorizationHeaderConfig != null) {
            this.authorizationHeader = (String) authorizationHeaderConfig;
        } else {
            this.authorizationHeader = DEFAULT_SECURITY_HEADER;
        }
    }

    public void setTokenRevocationChecker(String checker) {
        String revocationChecker = null;
        if (checker != null) {
            revocationChecker = checker;
        } else {
            Object revocationProvideConfig = ConfigParser.getParsedConfigs().get(OAuthConstants.REVOCATION_PROVIDER);
            if (revocationProvideConfig != null) {
                revocationChecker = (String) revocationProvideConfig;
            }
        }

        if (revocationChecker == null) {
            return;
        }

        Class clazz = null;
        try {
            clazz = JWTValidator.class.getClassLoader().loadClass(revocationChecker);
            this.tokenRevocationChecker = (TokenRevocationChecker) clazz.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            //TODO: Do we need to throw an exception here?
        }
    }

    public void setTrustedIssuerList(List<String> trustedIssuerList) {

        if (trustedIssuerList != null && !trustedIssuerList.isEmpty()) {
            this.trustedIssuerList = trustedIssuerList;
        } else {
            Object trustedIssuersConfig = ConfigParser.getParsedConfigs().get(OAuthConstants.TRUSTED_ISSUERS);
            if (trustedIssuersConfig != null) {
                this.trustedIssuerList = Arrays.asList(((String)trustedIssuersConfig).split(","));
            }
        }
    }

    public void setTokenCacheTimeout(String tokenCacheTimeout) {

        if (tokenCacheTimeout != null) {
            this.tokenCacheTimeout = Integer.parseInt(tokenCacheTimeout);
        } else {
            Object tokenCacheTimeoutConfig = ConfigParser.getParsedConfigs().get(OAuthConstants.TOKEN_CACHE_TIMEOUT);
            if (tokenCacheTimeoutConfig != null) {
                this.tokenCacheTimeout = (int) tokenCacheTimeoutConfig;
            }
        }
    }

    public void setJwksEndpoint(String jwksEndpoint) {

        this.jwksEndpoint = jwksEndpoint;
    }

    /**
     * Get signed JWT info for access token
     *
     * @param accessToken Access token
     * @return SignedJWTInfo
     * @throws ParseException if an error occurs
     */
    private SignedJWTInfo getSignedJwtInfo(String accessToken) throws ParseException {

        String signature = accessToken.split("\\.")[2];
        SignedJWTInfo signedJWTInfo = null;
        Cache signedJWTParseCache = CacheProvider.getSignedJWTParseCache();
        if (signedJWTParseCache != null) {
            Object cachedEntry = signedJWTParseCache.get(signature);
            if (cachedEntry != null) {
                signedJWTInfo = (SignedJWTInfo) cachedEntry;
            }
            if (signedJWTInfo == null || !signedJWTInfo.getToken().equals(accessToken)) {
                SignedJWT signedJWT = SignedJWT.parse(accessToken);
                JWTClaimsSet jwtClaimsSet = signedJWT.getJWTClaimsSet();
                signedJWTInfo = new SignedJWTInfo(accessToken, signedJWT, jwtClaimsSet);
                signedJWTParseCache.put(signature, signedJWTInfo);
            }
        } else {
            SignedJWT signedJWT = SignedJWT.parse(accessToken);
            JWTClaimsSet jwtClaimsSet = signedJWT.getJWTClaimsSet();
            signedJWTInfo = new SignedJWTInfo(accessToken, signedJWT, jwtClaimsSet);
        }
        return signedJWTInfo;
    }

//    public JWTValidationInfo validateJWTToken(SignedJWTInfo signedJWTInfo) throws APIManagementException {
//
//        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
//        JWTValidationInfo jwtValidationInfo = new JWTValidationInfo();
//        String issuer = signedJWTInfo.getJwtClaimsSet().getIssuer();
//        if (StringUtils.isNotEmpty(issuer)) {
//            List<KeyManagerDto> keyManagerDtoList = KeyManagerHolder.getKeyManagerByIssuer(tenantDomain, issuer);
//            KeyManagerDto keyManagerDto = null;
//            if (keyManagerDtoList.size() == 1) { // only one keymanager. no need to check if it can handle token
//                keyManagerDto = keyManagerDtoList.get(0);
//            } else {
//                for (KeyManagerDto kmrDto : keyManagerDtoList) {
//                    if (kmrDto.getKeyManager().canHandleToken(signedJWTInfo.getToken())) {
//                        keyManagerDto = kmrDto;
//                        break;
//                    }
//                }
//            }
//            if (keyManagerDto != null && keyManagerDto.getJwtValidator() != null) {
//                JWTValidationInfo validationInfo = keyManagerDto.getJwtValidator().validateToken(signedJWTInfo);
//                validationInfo.setKeyManager(keyManagerDto.getName());
//                return validationInfo;
//            }
//        }
//        jwtValidationInfo.setValid(false);
//        jwtValidationInfo.setValidationCode(APIConstants.KeyValidationStatus.API_AUTH_GENERAL_ERROR);
//        return jwtValidationInfo;
//    }

    private void handleAuthFailure(MessageContext messageContext, OAuthSecurityException e) {

        messageContext.setProperty(SynapseConstants.ERROR_CODE, e.getErrorCode());
        messageContext.setProperty(SynapseConstants.ERROR_MESSAGE,
                OAuthConstants.getAuthenticationFailureMessage(e.getErrorCode()));
        messageContext.setProperty(SynapseConstants.ERROR_EXCEPTION, e);

        //Setting error description which will be available to the handler
        String errorDetail = OAuthConstants.getFailureMessageDetailDescription(e.getErrorCode(), e.getMessage());
        messageContext.setProperty(SynapseConstants.ERROR_DETAIL, errorDetail);

        // By default we send a 401 response back
        org.apache.axis2.context.MessageContext axis2MC = ((Axis2MessageContext) messageContext).
                getAxis2MessageContext();

        int status;
        if (e.getErrorCode() == OAuthConstants.API_AUTH_GENERAL_ERROR ||
                e.getErrorCode() == OAuthConstants.API_AUTH_MISSING_OPEN_API_DEF) {
            status = HttpStatus.SC_INTERNAL_SERVER_ERROR;
        } else if (e.getErrorCode() == OAuthConstants.API_AUTH_INCORRECT_API_RESOURCE ||
                e.getErrorCode() == OAuthConstants.API_AUTH_FORBIDDEN ||
                e.getErrorCode() == OAuthConstants.API_OAUTH_INVALID_AUDIENCES ||
                e.getErrorCode() == OAuthConstants.INVALID_SCOPE) {
            status = HttpStatus.SC_FORBIDDEN;
        } else {
            status = HttpStatus.SC_UNAUTHORIZED;
            Map<String, String> headers =
                    (Map) axis2MC.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
            if (headers != null) {
                headers.put(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"WSO2 API Manager\""
                        + " error=\"invalid_token\""
                        + ", error_description=\"The provided token is invalid\"");
            }
        }
        sendFault(messageContext, status);
    }

}
