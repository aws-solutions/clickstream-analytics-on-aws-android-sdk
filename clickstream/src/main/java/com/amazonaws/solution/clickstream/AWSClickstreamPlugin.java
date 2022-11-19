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

package com.amazonaws.solution.clickstream;

import android.app.Application;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.analytics.AnalyticsEventBehavior;
import com.amplifyframework.analytics.AnalyticsException;
import com.amplifyframework.analytics.AnalyticsPlugin;
import com.amplifyframework.analytics.AnalyticsProperties;
import com.amplifyframework.analytics.UserProfile;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.logging.Logger;

import com.amazonaws.solution.clickstream.client.AnalyticsClient;
import com.amazonaws.solution.clickstream.client.ClickstreamManager;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The plugin implementation for Clickstream in Analytics category.
 */
public final class AWSClickstreamPlugin extends AnalyticsPlugin<Object> {

    private static final Logger LOG = Amplify.Logging.forNamespace("clickstream:AWSClickstreamPlugin");
    private final Application application;
    private AnalyticsClient analyticsClient;

    /**
     * Constructs a new {@link AWSClickstreamPlugin}.
     *
     * @param application Global application context
     */
    public AWSClickstreamPlugin(final Application application) {
        this.application = application;
    }

    @Override
    public void identifyUser(@NonNull String userId, @Nullable UserProfile profile) {

    }

    @Override
    public void disable() {
    }

    @Override
    public void enable() {
    }

    @Override
    public void recordEvent(@NonNull String eventName) {
    }

    @Override
    public void recordEvent(@NonNull AnalyticsEventBehavior analyticsEvent) {
    }

    @Override
    public void registerGlobalProperties(@NonNull AnalyticsProperties properties) {

    }

    @Override
    public void unregisterGlobalProperties(@NonNull String... propertyNames) {

    }

    @Override
    public void flushEvents() {
    }

    @NonNull
    @Override
    public String getPluginKey() {
        return "awsClickstreamPlugin";
    }

    @Override
    public void configure(
        JSONObject pluginConfiguration,
        @NonNull Context context
    ) throws AnalyticsException {
        if (pluginConfiguration == null) {
            throw new AnalyticsException(
                "Missing configuration for " + getPluginKey(),
                "Check amplifyconfiguration.json to make sure that there is a section for " +
                    getPluginKey() + " under the analytics category."
            );
        }

        AWSClickstreamPluginConfiguration.Builder configurationBuilder =
            AWSClickstreamPluginConfiguration.builder();

        // Read all the data from the configuration object to be used for record event
        try {
            configurationBuilder.withEndpoint(pluginConfiguration
                .getString(ConfigurationKey.ENDPOINT.getConfigurationKey()));

            if (pluginConfiguration.has(ConfigurationKey.SEND_EVENTS_SIZE.getConfigurationKey())) {
                configurationBuilder.sendEventsSize(pluginConfiguration
                    .getLong(ConfigurationKey.SEND_EVENTS_SIZE.getConfigurationKey()));
            }

            if (pluginConfiguration.has(ConfigurationKey.SEND_EVENTS_INTERVAL.getConfigurationKey())) {
                configurationBuilder.withSendEventsInterval(pluginConfiguration
                    .getLong(ConfigurationKey.SEND_EVENTS_INTERVAL.getConfigurationKey()));
            }

            if (pluginConfiguration.has(ConfigurationKey.COMPRESS_EVENTS.getConfigurationKey())) {
                configurationBuilder.withCompressEvents(
                    pluginConfiguration.getBoolean(ConfigurationKey.COMPRESS_EVENTS.getConfigurationKey()));
            }

            if (pluginConfiguration.has(ConfigurationKey.TRACK_APP_LIFECYCLE_EVENTS.getConfigurationKey())) {
                configurationBuilder.withTrackAppLifecycleEvents(pluginConfiguration
                    .getBoolean(ConfigurationKey.TRACK_APP_LIFECYCLE_EVENTS.getConfigurationKey()));
            }
        } catch (JSONException exception) {
            throw new AnalyticsException(
                "Unable to read endpoint from the amplify configuration json.", exception,
                "Make sure amplifyconfiguration.json is a valid json object in expected format. " +
                    "Please take a look at the documentation for expected format of amplifyconfiguration.json."
            );
        }

        AWSClickstreamPluginConfiguration clickstreamPluginConfiguration = configurationBuilder.build();
        ClickstreamManager clickstreamManager = ClickstreamManagerFactory.create(
            context,
            clickstreamPluginConfiguration
        );
        this.analyticsClient = clickstreamManager.getAnalyticsClient();
    }

    @Override
    public AnalyticsClient getEscapeHatch() {
        return analyticsClient;
    }

    @NonNull
    @Override
    public String getVersion() {
        return BuildConfig.VERSION_NAME;
    }

    /**
     * Clickstream configuration in amplifyconfiguration.json contains following values.
     */
    public enum ConfigurationKey {
        /**
         * the Clickstream Endpoint.
         */
        ENDPOINT("endpoint"),

        /**
         * The max number of events sent at once.
         */
        SEND_EVENTS_SIZE("sendEventsSize"),

        /**
         * Time interval after which the events are automatically submitted to server.
         */
        SEND_EVENTS_INTERVAL("sendEventsInterval"),

        /**
         * Whether to compress events.
         */
        COMPRESS_EVENTS("isCompressEvents"),

        /**
         * Whether to track app lifecycle events automatically.
         */
        TRACK_APP_LIFECYCLE_EVENTS("isTrackAppLifecycleEvents");

        /**
         * The key this property is listed under in the config JSON.
         */
        private final String configurationKey;

        /**
         * Construct the enum with the config key.
         *
         * @param configurationKey The key this property is listed under in the config JSON.
         */
        ConfigurationKey(final String configurationKey) {
            this.configurationKey = configurationKey;
        }

        /**
         * Returns the key this property is listed under in the config JSON.
         *
         * @return The key as a string
         */
        public String getConfigurationKey() {
            return configurationKey;
        }
    }
}

