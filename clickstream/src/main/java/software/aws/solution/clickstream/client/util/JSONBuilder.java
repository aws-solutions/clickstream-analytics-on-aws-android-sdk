/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.aws.solution.clickstream.client.util;

import androidx.annotation.NonNull;

import com.amazonaws.logging.Log;
import com.amazonaws.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The Builder for JSON.
 */
public class JSONBuilder implements JSONSerializable {
    private static final Log LOG = LogFactory.getLog(JSONBuilder.class);
    private static final int INDENTATION = 4;
    private final JSONObject json = new JSONObject();

    /**
     * The constructor of JSONBuilder with parameters.
     *
     */
    public JSONBuilder() {
        // Creates a new JSONBuilder instance with default values.
    }

    /**
     * Get the instance of JSONBuilder with key and value.
     *
     * @param key   The key.
     * @param value The value.
     * @return The instance of JSONBuilder.
     */
    public JSONBuilder withAttribute(String key, Object value) {
        final Object jsonValue = value instanceof JSONSerializable
            ? ((JSONSerializable) value).toJSONObject()
            : value;
        try {
            json.putOpt(key, jsonValue);
        } catch (final JSONException jsonException) {
            LOG.warn("error parsing json");
        }
        return this;
    }

    /**
     * Convert to JSON format.
     *
     * @return The JSON object.
     */
    @Override
    public JSONObject toJSONObject() {
        return json;
    }

    /**
     * Convert to string.
     *
     * @return The string.
     */
    @NonNull
    @Override
    public String toString() {
        try {
            return json.toString(INDENTATION);
        } catch (final JSONException jsonException) {
            return json.toString();
        }
    }
}

