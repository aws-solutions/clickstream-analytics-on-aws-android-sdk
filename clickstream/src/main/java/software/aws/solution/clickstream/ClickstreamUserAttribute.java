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
import com.amplifyframework.analytics.UserProfile;

/**
 * Clickstream UserProfile with userAttributes and userId.
 */
public class ClickstreamUserAttribute extends UserProfile {
    private static final int MAX_NAME_LENGTH = 50;
    private static final int MAX_VALUE_LENGTH = 256;
    private final AnalyticsProperties userAttributes;

    /**
     * Constructor for init the userAttribute and userId.
     *
     * @param builder An instance of the builder with the desired properties set.
     */
    protected ClickstreamUserAttribute(@NonNull Builder builder) {
        super(builder);
        this.userAttributes = builder.analyticsPropertiesBuilder.build();
    }

    /**
     * getter for userAttributes.
     *
     * @return userAttributes
     */
    public AnalyticsProperties getUserAttributes() {
        return userAttributes;
    }

    /**
     * Begins construction of an {@link ClickstreamUserAttribute} using a builder pattern.
     *
     * @return An {@link ClickstreamUserAttribute.Builder} instance
     */
    @NonNull
    public static Builder builder() {
        return new ClickstreamUserAttribute.Builder();
    }

    /**
     * Builder for the {@link ClickstreamUserAttribute} class.
     */
    public static final class Builder extends UserProfile.Builder<Builder, ClickstreamUserAttribute> {
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
        public Builder add(@NonNull @Size(min = 1L, max = MAX_NAME_LENGTH) String key,
                           @NonNull @Size(min = 1L, max = MAX_VALUE_LENGTH) String value) {
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
        public Builder add(@NonNull @Size(min = 1L, max = MAX_NAME_LENGTH) String key, @NonNull Double value) {
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
        public Builder add(@NonNull @Size(min = 1L, max = MAX_NAME_LENGTH) String key, @NonNull Boolean value) {
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
        public Builder add(@NonNull @Size(min = 1L, max = MAX_NAME_LENGTH) String key, @NonNull Integer value) {
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
        public ClickstreamUserAttribute.Builder add(@NonNull @Size(min = 1L, max = MAX_NAME_LENGTH) String key,
                                                    @NonNull Long value) {
            analyticsPropertiesBuilder.add(key, AnalyticsLongProperty.from(value));
            return this;
        }

        /**
         * Builds an instance of {@link ClickstreamUserAttribute}, using the provided values.
         *
         * @return An {@link ClickstreamUserAttribute}
         */
        @NonNull
        public ClickstreamUserAttribute build() {
            return new ClickstreamUserAttribute(this);
        }
    }
}
