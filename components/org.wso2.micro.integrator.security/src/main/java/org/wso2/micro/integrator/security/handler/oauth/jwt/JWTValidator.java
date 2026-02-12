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
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.util.DateUtils;
import org.apache.axis2.Constants;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.api.API;
import org.apache.synapse.api.Resource;
import org.apache.synapse.api.dispatch.RESTDispatcher;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.endpoints.auth.AuthException;
import org.apache.synapse.rest.RESTConstants;
import org.apache.synapse.rest.RESTUtils;
import org.wso2.micro.integrator.security.handler.oauth.CacheProvider;
import org.wso2.micro.integrator.security.handler.oauth.HttpClientConfiguration;
import org.wso2.micro.integrator.security.handler.oauth.OAuthConstants;
import org.wso2.micro.integrator.security.handler.oauth.OAuthSecurityException;
import org.wso2.micro.integrator.security.handler.oauth.OAuthUtil;
import org.wso2.micro.integrator.security.handler.oauth.SignedJWTInfo;
import org.wso2.micro.integrator.security.handler.oauth.TokenRevocationChecker;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JWTValidator {

    private static final Log log = LogFactory.getLog(JWTValidator.class);

    private JWKSet jwkSet;
    private List<String> trustedIssuerList;
    private TokenRevocationChecker tokenRevocationChecker;
    private String jwksEndpoint;
    private HttpClientConfiguration httpClientConfiguration;

    public JWTValidator(List<String> trustedIssuerList, TokenRevocationChecker tokenRevocationChecker,
                        String jwksEndpoint, HttpClientConfiguration httpClientConfiguration) {

        this.trustedIssuerList = trustedIssuerList;
        this.tokenRevocationChecker = tokenRevocationChecker;
        this.jwksEndpoint = jwksEndpoint;
        this.httpClientConfiguration = httpClientConfiguration;
    }

    public String getApiElectedResource(String httpMethod, MessageContext messageContext)
            throws OAuthSecurityException {

        API selectedApi = (API) messageContext.getProperty(RESTConstants.PROCESSED_API);
        Resource selectedResource = null;
        String resourceString;

        if (selectedApi != null) {
            Resource[] selectedAPIResources = selectedApi.getResources();

            List<Resource> acceptableResourcesList = new LinkedList<>();

            for (Resource resource : selectedAPIResources) {
                //If the requesting method is OPTIONS or if the Resource contains the requesting method
                if (RESTConstants.METHOD_OPTIONS.equals(httpMethod) &&
                        (resource.getMethods() != null && Arrays.asList(resource.getMethods()).contains(httpMethod))) {
                    acceptableResourcesList.add(0, resource);
                } else if (RESTConstants.METHOD_OPTIONS.equals(httpMethod) ||
                        (resource.getMethods() != null && Arrays.asList(resource.getMethods()).contains(httpMethod))) {
                    acceptableResourcesList.add(resource);
                }
            }

            Set<Resource> acceptableResources = new LinkedHashSet<>(acceptableResourcesList);

            if (acceptableResources.size() > 0) {
                for (RESTDispatcher dispatcher : RESTUtils.getDispatchers()) {
                    Resource resource = dispatcher.findResource(messageContext, acceptableResources);
                    if (resource != null && Arrays.asList(resource.getMethods()).contains(httpMethod)) {
                        selectedResource = resource;
                        if (selectedResource.getDispatcherHelper()
                                .getString() != null && !selectedResource.getDispatcherHelper().getString()
                                .contains("/*")) {
                            break;
                        }
                    }
                }
            }
        }

        if (selectedResource == null) {
            //No matching resource found.
            String msg = "Could not find matching resource for "
                    + messageContext.getProperty(RESTConstants.REST_FULL_REQUEST_PATH);;
            log.error(msg);
            throw new OAuthSecurityException(msg);
        }

        resourceString = selectedResource.getDispatcherHelper().getString();
        messageContext.setProperty(RESTConstants.SELECTED_RESOURCE, resourceString);
        return resourceString;
    }

    public void authenticate(SignedJWTInfo signedJWTInfo, MessageContext synCtx)
            throws OAuthSecurityException {

        org.apache.axis2.context.MessageContext axis2MsgContext =
                ((Axis2MessageContext) synCtx).getAxis2MessageContext();
        String httpMethod = (String) axis2MsgContext.getProperty(Constants.Configuration.HTTP_METHOD);
        String matchingResource = getApiElectedResource(httpMethod, synCtx);
        String jwtTokenIdentifier = getJWTTokenIdentifier(signedJWTInfo);
        String jwtHeader = signedJWTInfo.getSignedJWT().getHeader().toString();

        //TODO: handle cnf validation

        if (StringUtils.isNotEmpty(jwtTokenIdentifier) && tokenRevocationChecker != null) {
            if (tokenRevocationChecker.isRevoked(jwtTokenIdentifier)) {
                if (log.isDebugEnabled()) {
                    log.debug("Token retrieved from the revoked jwt token map. Token: "
                            + OAuthUtil.getMaskedToken(jwtHeader));
                }
                log.error("Invalid JWT token. " + OAuthUtil.getMaskedToken(jwtHeader));
                throw new OAuthSecurityException(OAuthConstants.API_AUTH_INVALID_CREDENTIALS,
                        "Invalid JWT token");
            }
        }

        JWTValidationInfo jwtValidationInfo = getJwtValidationInfo(signedJWTInfo, jwtTokenIdentifier, synCtx);

        if (jwtValidationInfo != null) {
            if (jwtValidationInfo.isValid()) {
                // Validate scopes
                JWTClaimsSet jwtClaimsSet = signedJWTInfo.getJwtClaimsSet();
                validateScopes(matchingResource, httpMethod, synCtx, jwtClaimsSet);
                synCtx.setProperty(OAuthConstants.SCOPES, jwtValidationInfo.getScopes().toString());
                synCtx.setProperty(OAuthConstants.JWT_CLAIMS, jwtValidationInfo.getClaims());

                if (log.isDebugEnabled()) {
                    log.debug("JWT authentication successful.");
                }

            } else {
                throw new OAuthSecurityException(jwtValidationInfo.getValidationCode(),
                        OAuthConstants.getAuthenticationFailureMessage(jwtValidationInfo.getValidationCode()));
            }
        } else {
            throw new OAuthSecurityException(OAuthConstants.API_AUTH_GENERAL_ERROR,
                    OAuthConstants.API_AUTH_GENERAL_ERROR_MESSAGE);
        }
    }

    private String getJWTTokenIdentifier(SignedJWTInfo signedJWTInfo) {

        JWTClaimsSet jwtClaimsSet = signedJWTInfo.getJwtClaimsSet();
        String jti = jwtClaimsSet.getJWTID();
        if (org.apache.commons.lang.StringUtils.isNotEmpty(jti)) {
            return jti;
        }
        return signedJWTInfo.getSignedJWT().getSignature().toString();
    }

    private JWTValidationInfo getJwtValidationInfo(SignedJWTInfo signedJWTInfo, String jti,
                                                   MessageContext messageContext) throws OAuthSecurityException {

        String jwtHeader = signedJWTInfo.getSignedJWT().getHeader().toString();
        JWTValidationInfo jwtValidationInfo = null;

        String cacheToken = (String) CacheProvider.getTokenCache().get(jti);
        if (SignedJWTInfo.ValidationStatus.VALID.equals(signedJWTInfo.getValidationStatus()) && cacheToken != null) {
            if (CacheProvider.getKeyCache().get(jti) != null) {
                JWTValidationInfo tempJWTValidationInfo = (JWTValidationInfo) CacheProvider.getKeyCache().get(jti);
                checkTokenExpiration(jti, tempJWTValidationInfo);
                jwtValidationInfo = tempJWTValidationInfo;
            }
        } else if (CacheProvider.getInvalidTokenCache().get(jti) != null) {
            if (log.isDebugEnabled()) {
                log.debug("Token retrieved from the invalid token cache. Token: " + OAuthUtil
                        .getMaskedToken(jwtHeader));
            }
            log.error("Invalid JWT token. " + OAuthUtil.getMaskedToken(jwtHeader));

            jwtValidationInfo = new JWTValidationInfo();
            jwtValidationInfo.setValidationCode(OAuthConstants.API_AUTH_INVALID_CREDENTIALS);
            jwtValidationInfo.setValid(false);
        }

        if (jwtValidationInfo == null) {
            jwtValidationInfo = validateToken(signedJWTInfo, messageContext);
            signedJWTInfo.setValidationStatus(jwtValidationInfo.isValid() ?
                    SignedJWTInfo.ValidationStatus.VALID : SignedJWTInfo.ValidationStatus.INVALID);

            if (jwtValidationInfo.isValid()) {
                CacheProvider.getTokenCache().put(jti, Boolean.TRUE);
                CacheProvider.getKeyCache().put(jti, jwtValidationInfo);
            } else {
                CacheProvider.getInvalidTokenCache().put(jti, Boolean.TRUE);
            }
        }
        return jwtValidationInfo;
    }

    /**
     * Check whether the jwt token is expired or not.
     *
     * @param tokenIdentifier The token Identifier of JWT.
     * @param payload        The payload of the JWT token
     * @return
     */
    private JWTValidationInfo checkTokenExpiration(String tokenIdentifier, JWTValidationInfo payload) {

        long timestampSkew = getTimeStampSkewInSeconds();

        Date now = new Date();
        Date exp = new Date(payload.getExpiryTime());
        if (!DateUtils.isAfter(exp, now, timestampSkew)) {
            CacheProvider.getTokenCache().remove(tokenIdentifier);
            CacheProvider.getKeyCache().remove(tokenIdentifier);
            CacheProvider.getInvalidTokenCache().put(tokenIdentifier, Boolean.TRUE);
            payload.setValid(false);
            payload.setValidationCode(OAuthConstants.API_AUTH_INVALID_CREDENTIALS);
            payload.setExpired(true);
            return payload;
        }
        return payload;
    }

    protected boolean validateTokenExpiry(JWTClaimsSet jwtClaimsSet) {

        long timestampSkew = getTimeStampSkewInSeconds();
        Date now = new Date();
        Date exp = jwtClaimsSet.getExpirationTime();
        return exp == null || DateUtils.isAfter(exp, now, timestampSkew);
    }

//    public JWTValidationInfo validateJWTToken(SignedJWTInfo signedJWTInfo, MessageContext messageContext) {
//
//        JWTValidationInfo jwtValidationInfo = new JWTValidationInfo();
//        String issuer = signedJWTInfo.getJwtClaimsSet().getIssuer();
//        if (StringUtils.isNotEmpty(issuer)) {
//
//            if (keyManagerDto != null && keyManagerDto.getJwtValidator() != null) {
//                JWTValidationInfo validationInfo = validateToken(signedJWTInfo, messageContext);
//                validationInfo.setKeyManager(keyManagerDto.getName());
//                return validationInfo;
//            }
//        }
//        jwtValidationInfo.setValid(false);
//        jwtValidationInfo.setValidationCode(APIConstants.KeyValidationStatus.API_AUTH_GENERAL_ERROR);
//        return jwtValidationInfo;
//    }

    public JWTValidationInfo validateToken(SignedJWTInfo signedJWTInfo, MessageContext messageContext)
            throws OAuthSecurityException {

        JWTValidationInfo jwtValidationInfo = new JWTValidationInfo();
        boolean state;
        try {
            state = validateSignature(signedJWTInfo.getSignedJWT(), messageContext);
            if (state) {
                JWTClaimsSet jwtClaimsSet = signedJWTInfo.getJwtClaimsSet();
                state = validateTokenExpiry(jwtClaimsSet);
                if (state) {
                    jwtValidationInfo.setScopes(getTransformedScopes(jwtClaimsSet));
                    createJWTValidationInfoFromJWT(jwtValidationInfo, jwtClaimsSet);
                    jwtValidationInfo.setRawPayload(signedJWTInfo.getToken());
                    return jwtValidationInfo;
                } else {
                    jwtValidationInfo.setValid(false);
                    jwtValidationInfo.setValidationCode(OAuthConstants.API_AUTH_INVALID_CREDENTIALS);
                    return jwtValidationInfo;
                }
            } else {
                jwtValidationInfo.setValid(false);
                jwtValidationInfo.setValidationCode(OAuthConstants.API_AUTH_INVALID_CREDENTIALS);
                return jwtValidationInfo;
            }
        } catch (ParseException e) {
            throw new OAuthSecurityException("Error while parsing JWT", e);
        }
    }

    protected boolean validateSignature(SignedJWT signedJWT, MessageContext messageContext)
            throws OAuthSecurityException {

        try {
            String keyID = signedJWT.getHeader().getKeyID();
            if (StringUtils.isEmpty(keyID)) {
                return false;
            }
            if (jwksEndpoint != null) {
                URL jwksEndpointUrl = new URL(jwksEndpoint);
                // Check JWKSet Available in Cache
                if (jwkSet == null) {
                    jwkSet = retrieveJWKSet(jwksEndpoint, messageContext);
                }
                if (jwkSet.getKeyByKeyId(keyID) == null) {
                    jwkSet = retrieveJWKSet(jwksEndpoint, messageContext);
                }
                if (jwkSet.getKeyByKeyId(keyID) instanceof RSAKey) {
                    RSAKey keyByKeyId = (RSAKey) jwkSet.getKeyByKeyId(keyID);
                    RSAPublicKey rsaPublicKey = keyByKeyId.toRSAPublicKey();
                    if (rsaPublicKey != null) {
                        return JWTUtil.verifyTokenSignature(signedJWT, rsaPublicKey);
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Key Algorithm not supported");
                    }
                    return false; // return false to produce 401 unauthenticated response
                }
            }
            return false;
        } catch (ParseException e) {
            log.error("Error while parsing JWKS information", e);
            throw new OAuthSecurityException("Error while parsing JWT", e);
        } catch (JOSEException e) {
            log.error("Error while verifying token signature", e);
            throw new OAuthSecurityException("Error while parsing JWT", e);
        } catch (IOException | AuthException e) {
            log.error("Error while connecting to JWKS endpoint", e);
            throw new OAuthSecurityException("Error while parsing JWT", e);
        } catch (OAuthSecurityException e) {
            log.error("Error while retrieving JWKS information", e);
            throw new OAuthSecurityException(e.getMessage(), e);
        }
    }

    private JWKSet retrieveJWKSet(String jwksEndpoint, MessageContext messageContext)
            throws IOException, ParseException, OAuthSecurityException, AuthException {

        String jwksInfo = JWTUtil.retrieveJWKSConfiguration(jwksEndpoint, httpClientConfiguration, messageContext);
        if (jwksInfo != null) {
            jwkSet = JWKSet.parse(jwksInfo);
        } else {
            throw new OAuthSecurityException("Invalid JWKS endpoint.");
        }
        return jwkSet;
    }

    protected long getTimeStampSkewInSeconds() {

        return OAuthConstants.DEFAULT_TIMESTAMP_SKEW_IN_SECONDS;
    }

    private void createJWTValidationInfoFromJWT(JWTValidationInfo jwtValidationInfo,
                                                JWTClaimsSet jwtClaimsSet)
            throws ParseException {

        jwtValidationInfo.setIssuer(jwtClaimsSet.getIssuer());
        jwtValidationInfo.setValid(true);
        jwtValidationInfo.setClaims(new HashMap<>(jwtClaimsSet.getClaims()));
        if (jwtClaimsSet.getExpirationTime() != null){
            jwtValidationInfo.setExpiryTime(jwtClaimsSet.getExpirationTime().getTime());
        }
        if (jwtClaimsSet.getIssueTime() != null){
            jwtValidationInfo.setIssuedTime(jwtClaimsSet.getIssueTime().getTime());
        }
        jwtValidationInfo.setUser(jwtClaimsSet.getSubject());
        jwtValidationInfo.setJti(jwtClaimsSet.getJWTID());
    }

    /**
     * Validate scopes bound to the resource of the API being invoked against the scopes specified
     * in the JWT token payload.
     *
     * @param matchingResource         Accessed API resource
     * @param httpMethod               API resource's HTTP method
     * @param synCtx                   MessageContext
     * @throws OAuthSecurityException in case of scope validation failure
     */
    private boolean validateScopes(String matchingResource, String httpMethod, MessageContext synCtx,
                                   JWTClaimsSet jwtClaimsSet) throws OAuthSecurityException {

        // Format the lookup key
        String lookupKey = httpMethod + ":" + matchingResource;

        // Get the required scopes from our pre-processed map
        Map<String, List<String>> resourceScopeMap =
                (Map<String, List<String>>) synCtx.getProperty("RESOURCE_SCOPE_MAP");
        List<String> requiredScopes = resourceScopeMap.get(lookupKey);

        if (requiredScopes == null || requiredScopes.isEmpty()) {
            return true; // No scopes required = Open Access
        }

        List<String> tokenScopesClaims = Collections.emptyList();

        try {
            String scopeClaim = JWTConstants.SCOPE;
            if (jwtClaimsSet.getClaim(scopeClaim) instanceof String) {
                tokenScopesClaims = Arrays.asList(jwtClaimsSet.getStringClaim(scopeClaim)
                        .split(JWTConstants.SCOPE_DELIMITER));
            } else if (jwtClaimsSet.getClaim(scopeClaim) instanceof List) {
                tokenScopesClaims = jwtClaimsSet.getStringListClaim(scopeClaim);
            }
        } catch (ParseException e) {
            throw new OAuthSecurityException("Error while parsing JWT claims", e);
        }

        // Intersection Check (Does the user have ANY of the required scopes?)
        for (String required : requiredScopes) {
            if (tokenScopesClaims.contains(required)) {
                return true;
            }
        }
        return false;
    }

    public List<String> getTransformedScopes(JWTClaimsSet jwtClaimsSet) throws OAuthSecurityException {

        try {
            String scopeClaim = JWTConstants.SCOPE;
            if (jwtClaimsSet.getClaim(scopeClaim) instanceof String) {
                return Arrays.asList(jwtClaimsSet.getStringClaim(scopeClaim)
                        .split(JWTConstants.SCOPE_DELIMITER));
            } else if (jwtClaimsSet.getClaim(scopeClaim) instanceof List) {
                return jwtClaimsSet.getStringListClaim(scopeClaim);
            }
        } catch (ParseException e) {
            throw new OAuthSecurityException("Error while parsing JWT claims", e);
        }
        return List.of(JWTConstants.OAUTH2_DEFAULT_SCOPE);
    }
}
