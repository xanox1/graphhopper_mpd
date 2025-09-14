/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper GmbH licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.application.util;

/**
 * Utility class for external endpoint testing configuration.
 * Allows tests to use the production external endpoint instead of local test server.
 */
public class ExternalEndpointTestUtils {
    
    /**
     * System property to enable external endpoint testing
     */
    public static final String EXTERNAL_ENDPOINT_PROPERTY = "test.external.endpoint";
    
    /**
     * Default external endpoint URL
     */
    public static final String DEFAULT_EXTERNAL_ENDPOINT = "https://graphhopper.xanox.org";
    
    /**
     * Check if external endpoint testing is enabled
     * @return true if external endpoint testing is enabled
     */
    public static boolean isExternalEndpointEnabled() {
        return getExternalEndpoint() != null;
    }
    
    /**
     * Get the external endpoint URL from system property
     * @return external endpoint URL or null if not set
     */
    public static String getExternalEndpoint() {
        String endpoint = System.getProperty(EXTERNAL_ENDPOINT_PROPERTY);
        if (endpoint != null && endpoint.trim().isEmpty()) {
            return null;
        }
        return endpoint;
    }
    
    /**
     * Get the external endpoint URL with fallback to default
     * @return external endpoint URL or default if property is set but empty
     */
    public static String getExternalEndpointWithDefault() {
        String endpoint = getExternalEndpoint();
        if (endpoint == null) {
            return null;
        }
        if (endpoint.trim().isEmpty()) {
            return DEFAULT_EXTERNAL_ENDPOINT;
        }
        return endpoint;
    }
    
    /**
     * Build the routing URL for external endpoint
     * @param baseUrl the base external endpoint URL
     * @return routing URL for the external endpoint
     */
    public static String buildRoutingUrl(String baseUrl) {
        if (baseUrl == null) {
            return null;
        }
        return baseUrl.endsWith("/") ? baseUrl + "route" : baseUrl + "/route";
    }
    
    /**
     * Check if endpoint is reachable (basic connectivity test)
     * @param endpoint the endpoint URL to test
     * @return true if endpoint appears to be reachable
     */
    public static boolean isEndpointReachable(String endpoint) {
        try {
            java.net.URL url = new java.net.URL(endpoint);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000); // 5 seconds
            connection.setReadTimeout(5000);
            int responseCode = connection.getResponseCode();
            return responseCode >= 200 && responseCode < 500; // Accept any response except server errors
        } catch (Exception e) {
            return false;
        }
    }
}