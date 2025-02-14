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

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.amplifyframework.analytics.AnalyticsEventBehavior;
import com.amplifyframework.analytics.AnalyticsException;
import com.amplifyframework.analytics.AnalyticsPlugin;
import com.amplifyframework.analytics.AnalyticsProperties;
import com.amplifyframework.analytics.AnalyticsPropertyBehavior;
import com.amplifyframework.analytics.UserProfile;

import com.amazonaws.logging.Log;
import com.amazonaws.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import software.aws.solution.clickstream.client.AnalyticsClient;
import software.aws.solution.clickstream.client.AnalyticsEvent;
import software.aws.solution.clickstream.client.ClickstreamManager;
import software.aws.solution.clickstream.client.Event;

import java.util.Map;
import java.util.Objects;

/**
 * The plugin implementation for Clickstream in Analytics category.
 */
public final class AWSClickstreamPlugin extends AnalyticsPlugin<Object> {
    static final String PLUGIN_KEY = "awsClickstreamPlugin";
    private static final Log LOG = LogFactory.getLog(AWSClickstreamPlugin.class);
    private final Context context;
    private AnalyticsClient analyticsClient;
    private AutoEventSubmitter autoEventSubmitter;
    private ActivityLifecycleManager activityLifecycleManager;
    private ClickstreamManager clickstreamManager;
    private boolean isEnable = true;

    /**
     * Constructs a new {@link AWSClickstreamPlugin}.
     *
     * @param context ApplicationContext
     */
    public AWSClickstreamPlugin(final Context context) {
        this.context = context;
    }

    @Override
    public void identifyUser(@NonNull String userId, @Nullable UserProfile profile) {
        if (userId.equals(Event.ReservedAttribute.USER_ID_UNSET)) {
            for (Map.Entry<String, AnalyticsPropertyBehavior<?>> entry :
                ((ClickstreamUserAttribute) Objects.requireNonNull(profile)).getUserAttributes()) {
                AnalyticsPropertyBehavior<?> property = entry.getValue();
                analyticsClient.addUserAttribute(entry.getKey(), property.getValue());
            }
        } else {
            analyticsClient.updateUserId(userId);
        }
        analyticsClient.updateUserAttribute();
        recordEvent(Event.PresetEvent.PROFILE_SET);
    }

    @Override
    public synchronized void disable() {
        if (isEnable) {
            autoEventSubmitter.stop();
            activityLifecycleManager.stopLifecycleTracking(context, ProcessLifecycleOwner.get().getLifecycle());
            clickstreamManager.disableTrackAppException();
            isEnable = false;
            LOG.info("Clickstream SDK disabled");
        }
    }

    @Override
    public synchronized void enable() {
        if (!isEnable) {
            autoEventSubmitter.start();
            activityLifecycleManager.startLifecycleTracking(context, ProcessLifecycleOwner.get().getLifecycle());
            clickstreamManager.enableTrackAppException();
            isEnable = true;
            LOG.info("Clickstream SDK enabled");
        }
    }

    @Override
    public void recordEvent(@NonNull String eventName) {
        final AnalyticsEvent event = analyticsClient.createEvent(eventName);
        if (event != null) {
            recordAnalyticsEvent(event);
        }
    }

    @Override
    public void recordEvent(@NonNull AnalyticsEventBehavior analyticsEvent) {
        ClickstreamEvent event = (ClickstreamEvent) analyticsEvent;
        final AnalyticsEvent clickstreamEvent =
            analyticsClient.createEvent(event.getName());

        if (clickstreamEvent != null) {
            for (Map.Entry<String, AnalyticsPropertyBehavior<?>> entry : analyticsEvent.getProperties()) {
                AnalyticsPropertyBehavior<?> property = entry.getValue();
                clickstreamEvent.addAttribute(entry.getKey(), property.getValue());
            }
            clickstreamEvent.addItems(event.getItems());
            recordAnalyticsEvent(clickstreamEvent);
        }
    }

    private void recordAnalyticsEvent(AnalyticsEvent event) {
        if (event.getEventType().equals(Event.PresetEvent.SCREEN_VIEW)) {
            activityLifecycleManager.onScreenViewManually(event);
        } else {
            analyticsClient.recordEvent(event);
        }
    }

    @Override
    public void registerGlobalProperties(@NonNull AnalyticsProperties properties) {
        for (Map.Entry<String, AnalyticsPropertyBehavior<?>> entry : properties) {
            AnalyticsPropertyBehavior<?> property = entry.getValue();
            analyticsClient.addGlobalAttribute(entry.getKey(), property.getValue());
        }
    }

    @Override
    public void unregisterGlobalProperties(@NonNull String... propertyNames) {
        for (String name : propertyNames) {
            analyticsClient.deleteGlobalAttribute(name);
        }
    }

    @Override
    public void flushEvents() {
        analyticsClient.submitEvents();
    }

    @NonNull
    @Override
    public String getPluginKey() {
        return PLUGIN_KEY;
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
        ClickstreamConfiguration configuration = ClickstreamConfiguration.getDefaultConfiguration();
        // Read all the data from the configuration object to be used for record event
        try {
            configuration.withAppId(pluginConfiguration.getString(ConfigurationKey.APP_ID));
            configuration.withEndpoint(pluginConfiguration
                .getString(ConfigurationKey.ENDPOINT));

            if (pluginConfiguration.has(ConfigurationKey.SEND_EVENTS_INTERVAL)) {
                configuration.withSendEventsInterval(pluginConfiguration
                    .getLong(ConfigurationKey.SEND_EVENTS_INTERVAL));
            }
            if (pluginConfiguration.has(ConfigurationKey.IS_COMPRESS_EVENTS)) {
                configuration.withCompressEvents(
                    pluginConfiguration.getBoolean(ConfigurationKey.IS_COMPRESS_EVENTS));
            }
            if (pluginConfiguration.has(ConfigurationKey.IS_TRACK_APP_EXCEPTION_EVENTS)) {
                configuration.withTrackAppExceptionEvents(pluginConfiguration
                    .getBoolean(ConfigurationKey.IS_TRACK_APP_EXCEPTION_EVENTS));
            }
            if (pluginConfiguration.has(ConfigurationKey.IS_LOG_EVENTS)) {
                configuration.withLogEvents(pluginConfiguration.getBoolean(ConfigurationKey.IS_LOG_EVENTS));
            }
            if (pluginConfiguration.has(ConfigurationKey.IS_TRACK_SCREEN_VIEW_EVENTS)) {
                configuration.withTrackScreenViewEvents(
                    pluginConfiguration.getBoolean(ConfigurationKey.IS_TRACK_SCREEN_VIEW_EVENTS));
            }
            if (pluginConfiguration.has(ConfigurationKey.IS_TRACK_USER_ENGAGEMENT_EVENTS)) {
                configuration.withTrackUserEngagementEvents(
                    pluginConfiguration.getBoolean(ConfigurationKey.IS_TRACK_USER_ENGAGEMENT_EVENTS));
            }
            if (pluginConfiguration.has(ConfigurationKey.SESSION_TIMEOUT_DURATION)) {
                configuration.withSessionTimeoutDuration(
                    pluginConfiguration.getLong(ConfigurationKey.SESSION_TIMEOUT_DURATION));
            }
            if (pluginConfiguration.has(ConfigurationKey.AUTH_COOKIE)) {
                configuration.withAuthCookie(pluginConfiguration.getString(ConfigurationKey.AUTH_COOKIE));
            }
            if (pluginConfiguration.has(ConfigurationKey.GLOBAL_ATTRIBUTES)) {
                configuration.withInitialGlobalAttributes(
                    (ClickstreamAttribute) pluginConfiguration.get(ConfigurationKey.GLOBAL_ATTRIBUTES));
            }
        } catch (JSONException exception) {
            throw new AnalyticsException(
                "Unable to read appId or endpoint from the amplify configuration json.", exception,
                "Make sure amplifyconfiguration.json is a valid json object in expected format. " +
                    "Please take a look at the documentation for expected format of amplifyconfiguration.json."
            );
        }
        clickstreamManager = new ClickstreamManager(context, configuration);
        this.analyticsClient = clickstreamManager.getAnalyticsClient();

        autoEventSubmitter = new AutoEventSubmitter(configuration.getSendEventsInterval());
        autoEventSubmitter.start();

        activityLifecycleManager = new ActivityLifecycleManager(clickstreamManager);
        activityLifecycleManager.startLifecycleTracking(this.context, ProcessLifecycleOwner.get().getLifecycle());
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
     * The Clickstream configuration keys.
     */
    static final class ConfigurationKey {
        static final String APP_ID = "appId";
        static final String ENDPOINT = "endpoint";
        static final String SEND_EVENTS_INTERVAL = "autoFlushEventsInterval";
        static final String IS_COMPRESS_EVENTS = "isCompressEvents";
        static final String IS_LOG_EVENTS = "isLogEvents";
        static final String AUTH_COOKIE = "authCookie";
        static final String SESSION_TIMEOUT_DURATION = "sessionTimeoutDuration";
        static final String IS_TRACK_APP_EXCEPTION_EVENTS = "isTrackAppExceptionEvents";
        static final String IS_TRACK_SCREEN_VIEW_EVENTS = "isTrackScreenViewEvents";
        static final String IS_TRACK_USER_ENGAGEMENT_EVENTS = "isTrackUserEngagementEvents";
        static final String GLOBAL_ATTRIBUTES = "globalAttributes";

        private ConfigurationKey() {
            throw new IllegalStateException("Utility class");
        }
    }
}

