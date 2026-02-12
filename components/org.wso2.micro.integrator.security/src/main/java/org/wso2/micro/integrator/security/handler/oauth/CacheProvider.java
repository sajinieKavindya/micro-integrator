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

import org.wso2.carbon.base.ServerConfiguration;

import java.util.concurrent.TimeUnit;
import javax.cache.Cache;
import javax.cache.CacheConfiguration;
import javax.cache.Caching;

public class CacheProvider {

    public static final String DEFAULT_CACHE_TIMEOUT = "Cache.DefaultCacheTimeout";
    public static final long DEFAULT_TIMEOUT = 900;
    private static CacheProvider cacheProvider;

    static {
        createParsedSignJWTCache();
        createTokenCache();
        createKeyCache();
        createInvalidTokenCache();
    }

    /**
     * Create and return SIGNED_JWT_CACHE
     */
    public static Cache createParsedSignJWTCache() {
        String tokenCacheExpiry = ""; //TODO: read Token cache expiry from the server configs -> "TokenCacheExpiry"
        if (tokenCacheExpiry != null) {
            return getCache("MI_CACHE_MANAGER", "SIGNED_JWT_CACHE",
                    Long.parseLong(tokenCacheExpiry), Long.parseLong(tokenCacheExpiry));
        } else {
            long defaultCacheTimeout = getDefaultCacheTimeout();
            return getCache("MI_CACHE_MANAGER", "SIGNED_JWT_CACHE",
                    defaultCacheTimeout, defaultCacheTimeout);
        }
    }

    /**
     * Create and return TOKEN_CACHE
     */
    public static Cache createTokenCache() {
        String tokenCacheExpiry = "";
        if (tokenCacheExpiry != null) {
            return getCache("MI_CACHE_MANAGER", "TOKEN_CACHE_NAME",
                    Long.parseLong(tokenCacheExpiry), Long.parseLong(tokenCacheExpiry));
        } else {
            long defaultCacheTimeout = getDefaultCacheTimeout();
            return getCache("MI_CACHE_MANAGER", "TOKEN_CACHE_NAME",
                    defaultCacheTimeout, defaultCacheTimeout);
        }

    }

    /**
     * Create and return KEY_CACHE_NAME
     */
    public static Cache createKeyCache() {
        String tokenCacheExpiry = "";
        if (tokenCacheExpiry != null) {
            return getCache("MI_CACHE_MANAGER", "KEY_CACHE_NAME",
                    Long.parseLong(tokenCacheExpiry), Long.parseLong(tokenCacheExpiry));
        } else {
            long defaultCacheTimeout =
                    getDefaultCacheTimeout();
            return getCache("MI_CACHE_MANAGER", "KEY_CACHE_NAME",
                    defaultCacheTimeout, defaultCacheTimeout);
        }
    }

    /**
     * Create and return GATEWAY_INVALID_TOKEN_CACHE
     */
    public static Cache createInvalidTokenCache() {
        String tokenCacheExpiry = "";
        if (tokenCacheExpiry != null) {
            return getCache("MI_CACHE_MANAGER", "INVALID_TOKEN_CACHE_NAME", Long.parseLong(tokenCacheExpiry), Long.parseLong
                    (tokenCacheExpiry));
        } else {
            long defaultCacheTimeout = getDefaultCacheTimeout();
            return getCache("MI_CACHE_MANAGER", "INVALID_TOKEN_CACHE_NAME",
                    defaultCacheTimeout, defaultCacheTimeout);
        }
    }

    /**
     * @param cacheName name of the requested cache
     * @return cache
     */
    private static Cache getCache(final String cacheName) {
        return Caching.getCacheManager("MI_CACHE_MANAGER").getCache(cacheName);
    }

    /**
     * Create the Cache object from the given parameters
     *
     * @param cacheManagerName - Name of the Cache Manager
     * @param cacheName        - Name of the Cache
     * @param modifiedExp      - Value of the MODIFIED Expiry Type
     * @param accessExp        - Value of the ACCESSED Expiry Type
     * @return - The cache object
     */
    public synchronized static Cache getCache(final String cacheManagerName, final String cacheName,
                                              final long modifiedExp, final long accessExp) {

        Iterable<Cache<?, ?>> availableCaches = Caching.getCacheManager(cacheManagerName).getCaches();
        for (Cache cache : availableCaches) {
            if (cache.getName().equalsIgnoreCase(cacheName)) {
                return Caching.getCacheManager(cacheManagerName).getCache(cacheName);
            }
        }

        return Caching.getCacheManager(
                        cacheManagerName).createCacheBuilder(cacheName).
                setExpiry(CacheConfiguration.ExpiryType.MODIFIED, new CacheConfiguration.Duration(TimeUnit.SECONDS,
                        modifiedExp)).
                setExpiry(CacheConfiguration.ExpiryType.ACCESSED, new CacheConfiguration.Duration(TimeUnit.SECONDS,
                        accessExp)).setStoreByValue(false).build();
    }

    /**
     * @return default cache timeout value
     */
    public static long getDefaultCacheTimeout() {
        if (ServerConfiguration.getInstance().getFirstProperty(DEFAULT_CACHE_TIMEOUT) != null) {
            return Long.parseLong(ServerConfiguration.getInstance().getFirstProperty(DEFAULT_CACHE_TIMEOUT)) * 60;
        }
        return DEFAULT_TIMEOUT;
    }

    /**
     *
     * @return SignedJWT ParsedCache
     */
    public static Cache getSignedJWTParseCache() {

        return getCache("SIGNED_JWT_CACHE");
    }

    /**
     * @return gateway token cache
     */
    public static Cache getTokenCache() {
        return getCache("TOKEN_CACHE_NAME");
    }

    /**
     * @return key cache
     */
    public static Cache getKeyCache() {
        return getCache("KEY_CACHE_NAME");
    }

    /**
     * @return invalid token cache
     */
    public static Cache getInvalidTokenCache() {
        return getCache("INVALID_TOKEN_CACHE_NAME");
    }

}
