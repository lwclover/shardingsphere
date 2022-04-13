/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.db.protocol.mysql.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.shardingsphere.db.protocol.CommonConstants;

/**
 * ShardingSphere-Proxy's information for MySQL.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MySQLServerInfo {
    
    /**
     * Protocol version is always 0x0A.
     */
    public static final int PROTOCOL_VERSION = 0x0A;
    
    public static final MySQLCharacterSet DEFAULT_CHARSET = MySQLCharacterSet.UTF8MB4_GENERAL_CI;
    
    private static final String DEFAULT_MYSQL_VERSION = "5.6.23";
    
    private static final String SERVER_VERSION_PATTERN = "%s-ShardingSphere-Proxy %s";
    
    private static Map<String, String> serverVersions = new ConcurrentHashMap<>();
    
    /**
     * Set server version.
     * 
     * @param schemaName schema name
     * @param serverVersion server version
     */
    public static void setServerVersion(final String schemaName, final String serverVersion) {
        serverVersions.put(schemaName, String.format(SERVER_VERSION_PATTERN, serverVersion, CommonConstants.PROXY_VERSION.get()));
    }
    
    /**
     * Get default server version.
     *
     * @return server version
     */
    public static String getDefaultServerVersion() {
        return String.format(SERVER_VERSION_PATTERN, DEFAULT_MYSQL_VERSION, CommonConstants.PROXY_VERSION.get());
    }
    
    /**
     * Get current server version by schemaName.
     * 
     * @param schemaName schema name
     * @return server version
     */
    public static String getServerVersion(final String schemaName) {
        return serverVersions.getOrDefault(schemaName, getDefaultServerVersion());
    }
}
