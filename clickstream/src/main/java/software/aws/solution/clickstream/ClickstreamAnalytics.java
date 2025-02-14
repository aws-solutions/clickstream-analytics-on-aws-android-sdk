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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.core.category.CategoryConfiguration;
import com.amplifyframework.core.category.CategoryType;

import com.amazonaws.logging.Log;
import com.amazonaws.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import software.aws.solution.clickstream.AWSClickstreamPlugin.ConfigurationKey;
import software.aws.solution.clickstream.client.AnalyticsClient;
import software.aws.solution.clickstream.client.Event.PresetEvent;
import software.aws.solution.clickstream.client.Event.ReservedAttribute;
import software.aws.solution.clickstream.client.util.ThreadUtil;

/**
 * This is the top-level customer-facing interface to The ClickstreamAnalytics.
 */
public final class ClickstreamAnalytics {
    private static final Log LOG = LogFactory.getLog(ClickstreamAnalytics.class);
    private static final String NOINSTANCEALLOWED = "No instances allowed.";

    private ClickstreamAnalytics() {
        throw new UnsupportedOperationException(NOINSTANCEALLOWED);
    }

    /**
     * Init ClickstreamAnalytics Plugin.
     *
     * @param context ApplicationContext
     * @throws AmplifyException Exception of init.
     */
    public static void init(@NonNull Context context) throws AmplifyException {
        init(context, new ClickstreamConfiguration());
    }

    /**
     * Init ClickstreamAnalytics Plugin.
     *
     * @param context       ApplicationContext
     * @param configuration ClickstreamConfiguration
     * @throws AmplifyException Exception of init.
     */
    public static void init(@NonNull Context context, ClickstreamConfiguration configuration)
        throws AmplifyException {
        if (ThreadUtil.notInMainThread()) {
            throw new AmplifyException("Clickstream SDK initialization failed", "Please initialize in the main thread");
        }
        AmplifyConfiguration configure = getAmplifyConfigurationObject(context, configuration);
        Amplify.addPlugin(new AWSClickstreamPlugin(context));
        Amplify.configure(configure, context);
    }

    /**
     * Use this method to record Event.
     *
     * @param event ClickstreamEvent to record
     */
    public static void recordEvent(@NonNull final ClickstreamEvent event) {
        Amplify.Analytics.recordEvent(event);
    }

    /**
     * Use this method to record Event.
     *
     * @param eventName the event name
     */
    public static void recordEvent(@NonNull final String eventName) {
        Amplify.Analytics.recordEvent(eventName);
    }

    /**
     * Use this method to send events immediately.
     */
    public static void flushEvents() {
        Amplify.Analytics.flushEvents();
    }

    /**
     * Add user clickstreamAttribute.
     *
     * @param clickstreamAttribute the global clickstreamAttribute.
     */
    public static void addGlobalAttributes(@NonNull ClickstreamAttribute clickstreamAttribute) {
        Amplify.Analytics.registerGlobalProperties(clickstreamAttribute.getAttributes());
    }

    /**
     * Delete global attributes.
     *
     * @param attributeName the attribute name to delete.
     */
    public static void deleteGlobalAttributes(@NonNull String... attributeName) {
        Amplify.Analytics.unregisterGlobalProperties(attributeName);
    }

    /**
     * Add user attributes.
     *
     * @param userProfile user
     */
    public static void addUserAttributes(@NonNull ClickstreamUserAttribute userProfile) {
        Amplify.Analytics.identifyUser(ReservedAttribute.USER_ID_UNSET, userProfile);
    }

    /**
     * Set user id.
     *
     * @param userId user
     */
    public static void setUserId(String userId) {
        String newUserId = userId;
        if (newUserId == null) {
            newUserId = "";
        }
        Amplify.Analytics.identifyUser(newUserId, new ClickstreamUserAttribute.Builder().build());
    }

    /**
     * Enable clickstream SDK.
     */
    public static void enable() {
        if (ThreadUtil.notInMainThread()) {
            LOG.error("Clickstream SDK enabled failed, please execute in the main thread");
            return;
        }
        Amplify.Analytics.enable();
    }

    /**
     * Disable clickstream SDK.
     */
    public static void disable() {
        if (ThreadUtil.notInMainThread()) {
            LOG.error("Clickstream SDK disabled failed, please execute in the main thread");
            return;
        }
        Amplify.Analytics.disable();
    }

    /**
     * Get clickstream configuration please config it after SDK initialize.
     *
     * @return ClickstreamConfiguration configurationF
     */
    public static ClickstreamConfiguration getClickStreamConfiguration() {
        AnalyticsClient client =
            ((AWSClickstreamPlugin) Amplify.Analytics.getPlugin(AWSClickstreamPlugin.PLUGIN_KEY)).getEscapeHatch();
        assert client != null;
        return client.getClickstreamConfiguration();
    }

    private static AmplifyConfiguration getAmplifyConfigurationObject(Context context,
                                                                      ClickstreamConfiguration configuration)
        throws AmplifyException {
        AmplifyConfiguration amplifyConfiguration;
        JSONObject configureObject;
        try {
            amplifyConfiguration = AmplifyConfiguration.fromConfigFile(context);
            CategoryConfiguration categoryConfiguration = amplifyConfiguration.forCategoryType(CategoryType.ANALYTICS);
            configureObject = categoryConfiguration.getPluginConfig(AWSClickstreamPlugin.PLUGIN_KEY);
        } catch (AmplifyException exception) {
            LOG.info("Clickstream SDK can not find the amplifyconfiguration.json file, " +
                "The SDK will initialize using the configuration you set");
            JSONObject amplifyObject = new JSONObject();
            JSONObject analyticsObject = new JSONObject();
            JSONObject pluginsObject = new JSONObject();
            configureObject = new JSONObject();
            try {
                pluginsObject.put(AWSClickstreamPlugin.PLUGIN_KEY, configureObject);
                analyticsObject.put("plugins", pluginsObject);
                amplifyObject.put("analytics", analyticsObject);
            } catch (JSONException jsonException) {
                throw new AmplifyException("JSONException", "JSONException occurred while constructing a JSON object");
            }
            amplifyConfiguration = AmplifyConfiguration.fromJson(amplifyObject);
        }
        try {
            if (configuration.getAppId() != null) {
                configureObject.put(ConfigurationKey.APP_ID, configuration.getAppId());
            }
            if (configuration.getEndpoint() != null) {
                configureObject.put(ConfigurationKey.ENDPOINT, configuration.getEndpoint());
            }
            if (configuration.isLogEvents() != null) {
                configureObject.put(ConfigurationKey.IS_LOG_EVENTS, configuration.isLogEvents());
            }
            if (configuration.isCompressEvents() != null) {
                configureObject.put(ConfigurationKey.IS_COMPRESS_EVENTS, configuration.isCompressEvents());
            }
            if (configuration.isTrackAppExceptionEvents() != null) {
                configureObject.put(ConfigurationKey.IS_TRACK_APP_EXCEPTION_EVENTS,
                    configuration.isTrackAppExceptionEvents());
            }
            if (configuration.isTrackScreenViewEvents() != null) {
                configureObject.put(ConfigurationKey.IS_TRACK_SCREEN_VIEW_EVENTS,
                    configuration.isTrackScreenViewEvents());
            }
            if (configuration.isTrackUserEngagementEvents() != null) {
                configureObject.put(ConfigurationKey.IS_TRACK_USER_ENGAGEMENT_EVENTS,
                    configuration.isTrackUserEngagementEvents());
            }
            if (configuration.getSessionTimeoutDuration() > 0) {
                configureObject.put(ConfigurationKey.SESSION_TIMEOUT_DURATION,
                    configuration.getSessionTimeoutDuration());
            }
            if (configuration.getSendEventsInterval() > 0) {
                configureObject.put(ConfigurationKey.SEND_EVENTS_INTERVAL, configuration.getSendEventsInterval());
            }
            if (configuration.getAuthCookie() != null) {
                configureObject.put(ConfigurationKey.AUTH_COOKIE, configuration.getAuthCookie());
            }
            if (configuration.getInitialGlobalAttributes() != null) {
                configureObject.put(ConfigurationKey.GLOBAL_ATTRIBUTES, configuration.getInitialGlobalAttributes());
            }
        } catch (Exception exception) {
            LOG.error("Parse JSON exception, you may need to check your initial configuration");
        }
        return amplifyConfiguration;
    }

    /**
     * Item attributes.
     */
    public static final class Item {
        /**
         * key to item id.
         */
        public static final String ITEM_ID = "id";
        /**
         * key to item name.
         */
        public static final String ITEM_NAME = "name";
        /**
         * key to item location id.
         */
        public static final String LOCATION_ID = "location_id";
        /**
         * key to item brand.
         */
        public static final String ITEM_BRAND = "brand";
        /**
         * key to item currency.
         */
        public static final String CURRENCY = "currency";
        /**
         * key to item price.
         */
        public static final String PRICE = "price";
        /**
         * key to item quantity.
         */
        public static final String QUANTITY = "quantity";
        /**
         * key to item creative name.
         */
        public static final String CREATIVE_NAME = "creative_name";
        /**
         * key to item creative slot.
         */
        public static final String CREATIVE_SLOT = "creative_slot";
        /**
         * key to item category.
         */
        public static final String ITEM_CATEGORY = "item_category";
        /**
         * key to item category2.
         */
        public static final String ITEM_CATEGORY2 = "item_category2";
        /**
         * key to item category3.
         */
        public static final String ITEM_CATEGORY3 = "item_category3";
        /**
         * key to item category4.
         */
        public static final String ITEM_CATEGORY4 = "item_category4";
        /**
         * key to item category5.
         */
        public static final String ITEM_CATEGORY5 = "item_category5";

        private Item() {
            throw new IllegalStateException(NOINSTANCEALLOWED);
        }
    }

    /**
     * Preset Event.
     */
    public static final class Event {

        /**
         * screen view.
         */
        public static final String SCREEN_VIEW = PresetEvent.SCREEN_VIEW;

        private Event() {
            throw new IllegalStateException(NOINSTANCEALLOWED);
        }
    }

    /**
     * Preset Attributes.
     */
    public static final class Attr {

        /**
         * screen name.
         */
        public static final String SCREEN_NAME = ReservedAttribute.SCREEN_NAME;

        /**
         * screen unique id.
         */
        public static final String SCREEN_UNIQUE_ID = ReservedAttribute.SCREEN_UNIQUE_ID;

        /**
         * traffic source source.
         */
        public static final String TRAFFIC_SOURCE_SOURCE = "_traffic_source_source";

        /**
         * traffic source medium.
         */
        public static final String TRAFFIC_SOURCE_MEDIUM = "_traffic_source_medium";

        /**
         * traffic source campaign.
         */
        public static final String TRAFFIC_SOURCE_CAMPAIGN = "_traffic_source_campaign";

        /**
         * traffic source campaign id.
         */
        public static final String TRAFFIC_SOURCE_CAMPAIGN_ID = "_traffic_source_campaign_id";

        /**
         * traffic source term.
         */
        public static final String TRAFFIC_SOURCE_TERM = "_traffic_source_term";

        /**
         * traffic source content.
         */
        public static final String TRAFFIC_SOURCE_CONTENT = "_traffic_source_content";

        /**
         * traffic source clid.
         */
        public static final String TRAFFIC_SOURCE_CLID = "_traffic_source_clid";

        /**
         * traffic source clid platform.
         */
        public static final String TRAFFIC_SOURCE_CLID_PLATFORM = "_traffic_source_clid_platform";

        /**
         * app install channel.
         */
        public static final String APP_INSTALL_CHANNEL = "_app_install_channel";

        /**
         * event value.
         */
        public static final String VALUE = "_value";

        /**
         * event currency.
         */
        public static final String CURRENCY = "_currency";

        private Attr() {
            throw new IllegalStateException(NOINSTANCEALLOWED);
        }
    }
}
