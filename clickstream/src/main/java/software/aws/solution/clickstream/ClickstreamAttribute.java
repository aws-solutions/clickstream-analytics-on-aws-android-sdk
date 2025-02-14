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

package software.aws.solution.clickstream;

import androidx.annotation.NonNull;
import androidx.annotation.Size;

import com.amplifyframework.analytics.AnalyticsBooleanProperty;
import com.amplifyframework.analytics.AnalyticsDoubleProperty;
import com.amplifyframework.analytics.AnalyticsIntegerProperty;
import com.amplifyframework.analytics.AnalyticsProperties;
import com.amplifyframework.analytics.AnalyticsStringProperty;

import software.aws.solution.clickstream.client.Event;

import java.io.Serializable;

/**
 * Clickstream attribute setter.
 */
public class ClickstreamAttribute implements Serializable {
    private final AnalyticsProperties attributes;

    /**
     * Constructor for init the userAttribute and userId.
     *
     * @param builder An instance of the builder with the desired properties set.
     */
    protected ClickstreamAttribute(@NonNull Builder builder) {
        this.attributes = builder.analyticsPropertiesBuilder.build();
    }

    /**
     * getter for userAttributes.
     *
     * @return userAttributes
     */
    public AnalyticsProperties getAttributes() {
        return attributes;
    }

    /**
     * Begins construction of an {@link ClickstreamAttribute} using a builder pattern.
     *
     * @return An {@link ClickstreamAttribute.Builder} instance
     */
    @NonNull
    public static Builder builder() {
        return new ClickstreamAttribute.Builder();
    }

    /**
     * Builder for the {@link ClickstreamAttribute} class.
     */
    public static class Builder {
        private final AnalyticsProperties.Builder analyticsPropertiesBuilder = AnalyticsProperties.builder();

        /**
         * Adds a {@link AnalyticsStringProperty} to the {@link AnalyticsProperties} under
         * construction.
         *
         * @param key   A name for the property
         * @param value A String to store in the property
         * @return Current Builder instance, for fluent method chaining
         */
        @NonNull
        public Builder add(@NonNull @Size(min = 1L, max = Event.Limit.MAX_LENGTH_OF_NAME) String key,
                           @NonNull @Size(min = 0L, max = Event.Limit.MAX_LENGTH_OF_VALUE) String value) {
            analyticsPropertiesBuilder.add(key, value);
            return this;
        }

        /**
         * Adds a {@link AnalyticsDoubleProperty} to the {@link AnalyticsProperties} under
         * construction.
         *
         * @param key   A name for the property
         * @param value A Double to store in the property
         * @return Current Builder instance, for fluent method chaining
         */
        @NonNull
        public Builder add(@NonNull @Size(min = 1L, max = Event.Limit.MAX_LENGTH_OF_NAME) String key,
                           @NonNull Double value) {
            analyticsPropertiesBuilder.add(key, value);
            return this;
        }

        /**
         * Adds a {@link AnalyticsBooleanProperty} to the {@link AnalyticsProperties} under
         * construction.
         *
         * @param key   A name for the property
         * @param value A Boolean to store in the property
         * @return Current Builder instance, for fluent method chaining
         */
        @NonNull
        public Builder add(@NonNull @Size(min = 1L, max = Event.Limit.MAX_LENGTH_OF_NAME) String key,
                           @NonNull Boolean value) {
            analyticsPropertiesBuilder.add(key, value);
            return this;
        }

        /**
         * Adds an {@link AnalyticsIntegerProperty} to the {@link AnalyticsProperties} under
         * construction.
         *
         * @param key   A name for the property
         * @param value An Integer to store in the property
         * @return Current Builder instance, for fluent method chaining
         */
        @NonNull
        public Builder add(@NonNull @Size(min = 1L, max = Event.Limit.MAX_LENGTH_OF_NAME) String key,
                           @NonNull Integer value) {
            analyticsPropertiesBuilder.add(key, value);
            return this;
        }

        /**
         * Adds an {@link AnalyticsLongProperty} to the {@link AnalyticsProperties} under
         * construction.
         *
         * @param key   A name for the property
         * @param value An Long to store in the property
         * @return Current Builder instance, for fluent method chaining
         */
        @NonNull
        public Builder add(@NonNull @Size(min = 1L, max = Event.Limit.MAX_LENGTH_OF_NAME) String key,
                           @NonNull Long value) {
            analyticsPropertiesBuilder.add(key, AnalyticsLongProperty.from(value));
            return this;
        }

        /**
         * Builds an instance of {@link ClickstreamAttribute}, using the provided values.
         *
         * @return An {@link ClickstreamAttribute}
         */
        @NonNull
        public ClickstreamAttribute build() {
            return new ClickstreamAttribute(this);
        }
    }
}
