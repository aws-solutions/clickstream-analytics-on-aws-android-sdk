/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.aws.solution.clickstream;

import androidx.annotation.NonNull;
import androidx.annotation.Size;

import com.amazonaws.logging.Log;
import com.amazonaws.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import software.aws.solution.clickstream.client.Event;

/**
 * ClickstreamItem for item record.
 */
public final class ClickstreamItem {
    private static final Log LOG = LogFactory.getLog(ClickstreamItem.class);
    private final JSONObject attributes;

    /**
     * Constructor for init the ClickstreamItem.
     *
     * @param attributes An instance of the builder with the desired attributes set.
     */
    private ClickstreamItem(@NonNull JSONObject attributes) {
        this.attributes = attributes;
    }

    /**
     * the getter for attributes.
     *
     * @return the attributes json object.
     */
    public JSONObject getAttributes() {
        return attributes;
    }


    /**
     * Begins construction of an {@link ClickstreamItem} using a builder pattern.
     *
     * @return An {@link ClickstreamItem.Builder} instance
     */
    @NonNull
    public static Builder builder() {
        return new ClickstreamItem.Builder();
    }

    /**
     * Builder for the {@link ClickstreamItem} class.
     */
    public static class Builder {
        private final JSONObject jsonObjectBuilder = new JSONObject();

        /**
         * constructor for Builder.
         *
         * @param key   A name for the property
         * @param value A String to store in the property
         * @return Current Builder instance, for fluent method chaining
         */
        @NonNull
        public ClickstreamItem.Builder add(@NonNull @Size(min = 1L, max = Event.Limit.MAX_LENGTH_OF_NAME) String key,
                                           @NonNull @Size(min = 0L, max = Event.Limit.MAX_LENGTH_OF_ITEM_VALUE)
                                           String value) {
            setAttribute(key, value);
            return this;
        }

        /**
         * Adds double value.
         *
         * @param key   A name for the property
         * @param value A Double to store in the property
         * @return Current Builder instance, for fluent method chaining
         */
        @NonNull
        public ClickstreamItem.Builder add(@NonNull @Size(min = 1L, max = Event.Limit.MAX_LENGTH_OF_NAME) String key,
                                           @NonNull Double value) {
            setAttribute(key, value);
            return this;
        }

        /**
         * Adds boolean value.
         *
         * @param key   A name for the property
         * @param value A Boolean to store in the property
         * @return Current Builder instance, for fluent method chaining
         */
        @NonNull
        public ClickstreamItem.Builder add(@NonNull @Size(min = 1L, max = Event.Limit.MAX_LENGTH_OF_NAME) String key,
                                           @NonNull Boolean value) {
            setAttribute(key, value);
            return this;
        }

        /**
         * Adds int value.
         *
         * @param key   A name for the property
         * @param value An Integer to store in the property
         * @return Current Builder instance, for fluent method chaining
         */
        @NonNull
        public ClickstreamItem.Builder add(@NonNull @Size(min = 1L, max = Event.Limit.MAX_LENGTH_OF_NAME) String key,
                                           @NonNull Integer value) {
            setAttribute(key, value);
            return this;
        }

        /**
         * Adds long value.
         *
         * @param key   A name for the property
         * @param value An Long to store in the property
         * @return Current Builder instance, for fluent method chaining
         */
        @NonNull
        public ClickstreamItem.Builder add(@NonNull @Size(min = 1L, max = Event.Limit.MAX_LENGTH_OF_NAME) String key,
                                           @NonNull Long value) {
            setAttribute(key, value);
            return this;
        }

        private void setAttribute(String key, Object value) {
            try {
                jsonObjectBuilder.putOpt(key, value);
            } catch (JSONException exception) {
                LOG.warn("error parsing json, error message:" + exception.getMessage());
            }
        }

        /**
         * Builds an instance of {@link ClickstreamItem}, using the provided values.
         *
         * @return An {@link ClickstreamItem}
         */
        @NonNull
        public ClickstreamItem build() {
            return new ClickstreamItem(jsonObjectBuilder);
        }
    }
}
