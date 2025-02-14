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

package software.aws.solution.clickstream.client;

import androidx.annotation.NonNull;

import com.amazonaws.logging.Log;
import com.amazonaws.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import software.aws.solution.clickstream.ClickstreamItem;
import software.aws.solution.clickstream.client.system.AndroidAppDetails;
import software.aws.solution.clickstream.client.system.AndroidConnectivity;
import software.aws.solution.clickstream.client.system.AndroidDeviceDetails;
import software.aws.solution.clickstream.client.util.JSONBuilder;
import software.aws.solution.clickstream.client.util.JSONSerializable;

import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * An event for clickstream.
 */
public class AnalyticsEvent implements JSONSerializable {
    private static final String UNKNOWN = "UNKNOWN";
    private static final Log LOG = LogFactory.getLog(AnalyticsEvent.class);
    private static final int INDENTATION = 4;
    private String deviceId;
    private String appId;
    private final String eventId;
    private final String eventType;
    private String sdkName;
    private String sdkVersion;
    private final JSONObject attributes = new JSONObject();
    private final JSONArray eventItems = new JSONArray();
    private final JSONObject userAttributes;
    private final Long timestamp;
    private final String uniqueId;
    private Session session;
    private AndroidAppDetails appDetails;
    private AndroidDeviceDetails deviceDetails;
    private AndroidConnectivity connectivity;
    private int heightPixels;
    private int widthPixels;

    /**
     * The default constructor.
     *
     * @param eventType        The eventType of the new event.
     * @param globalAttributes A list of global attributes of the new event.
     * @param userAttributes   A list of user attributes of the new event.
     * @param timestamp        The timestamp of the new event.
     * @param uniqueId         The uniqueId of the new event.
     */
    AnalyticsEvent(final String eventType, final Map<String, Object> globalAttributes,
                   final JSONObject userAttributes, final long timestamp, final String uniqueId) {
        this(UUID.randomUUID().toString(), eventType, globalAttributes, userAttributes, timestamp, uniqueId);
    }

    private AnalyticsEvent(final String eventId, final String eventType, final Map<String, Object> globalAttributes,
                           final JSONObject userAttributes, final long timestamp, final String uniqueId) {
        this.eventId = eventId;
        this.timestamp = timestamp;
        this.uniqueId = uniqueId;
        this.eventType = eventType;
        for (final Map.Entry<String, Object> kvp : globalAttributes.entrySet()) {
            this.addGlobalAttribute(kvp.getKey(), kvp.getValue());
        }
        this.userAttributes = userAttributes;
    }

    /**
     * Setter for sdkInfo.
     *
     * @param sdkInfo The sdkInfo.
     */
    public void setSdkInfo(SDKInfo sdkInfo) {
        this.sdkName = sdkInfo.getName();
        this.sdkVersion = sdkInfo.getVersion();
    }

    /**
     * Setter for session.
     *
     * @param session The session.
     */
    public void setSession(Session session) {
        this.session = session;
    }

    /**
     * Setter for appDetails.
     *
     * @param appDetails The appDetails.
     */
    public void setAppDetails(AndroidAppDetails appDetails) {
        this.appDetails = appDetails;
    }

    /**
     * Setter for deviceDetails.
     *
     * @param deviceDetails The deviceDetails.
     */
    public void setDeviceDetails(AndroidDeviceDetails deviceDetails) {
        this.deviceDetails = deviceDetails;
    }

    /**
     * Getter for currentNumOfAttributes.
     *
     * @return currentNumOfAttributes AtomicInteger
     */
    public int getCurrentNumOfAttributes() {
        return this.attributes.length();
    }

    /**
     * Setter for connectivity.
     *
     * @param connectivity The connectivity.
     */
    public void setConnectivity(AndroidConnectivity connectivity) {
        this.connectivity = connectivity;
    }

    /**
     * Get the Android ID.
     *
     * @return The Android ID.
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * Setter for Android ID.
     *
     * @param deviceId The Android ID.
     */
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Get the App ID.
     *
     * @return The App ID.
     */
    public String getAppId() {
        return appId;
    }

    /**
     * Setter for appId.
     *
     * @param appId The appId.
     */
    public void setAppId(String appId) {
        this.appId = appId;
    }

    /**
     * Get the screen height pixels.
     *
     * @return The screen height pixels.
     */
    public int getHeightPixels() {
        return heightPixels;
    }

    /**
     * Setter the screen height pixels.
     *
     * @param heightPixels The screen height pixels.
     */
    public void setHeightPixels(int heightPixels) {
        this.heightPixels = heightPixels;
    }

    /**
     * Get the screen width pixels.
     *
     * @return The screen width pixels.
     */
    public int getWidthPixels() {
        return widthPixels;
    }

    /**
     * Setter the screen width pixels.
     *
     * @param widthPixels The screen width pixels.
     */
    public void setWidthPixels(int widthPixels) {
        this.widthPixels = widthPixels;
    }

    /**
     * Returns the eventId.
     *
     * @return the eventId.
     */
    public String getEventId() {
        return this.eventId;
    }

    /**
     * add global attribute and do not check the attribute.
     *
     * @param name  The name of the attribute.
     * @param value The value of the attribute.
     */
    private void addGlobalAttribute(final String name, final Object value) {
        try {
            attributes.putOpt(name, value);
        } catch (JSONException exception) {
            LOG.error("error parsing json, error message:" + exception.getMessage()); //NOSONAR
        }
    }

    /**
     * Adds an attribute to this {@link AnalyticsEvent} with the specified key.
     * Only 500 attributes are allowed to be added to an Event. If 500
     * attribute already exist on this Event, the call will be ignored and log error.
     * If the attribute name if not valid or exceed the length limit the error will be record.
     * If the attribute value exceed the length limit the error will be record.
     * see the event limit definitions {@link Event.Limit} for detail.
     *
     * @param name  The name of the attribute.
     * @param value The value of the attribute.
     */
    public void addAttribute(final String name, final Object value) {
        if (null == name) {
            return;
        }
        if (null != value) {
            Event.EventError attributeError = EventChecker.checkAttribute(getCurrentNumOfAttributes(), name, value);
            try {
                if (attributeError.getErrorCode() > 0) {
                    if (!attributes.has(Event.ReservedAttribute.ERROR_CODE)) {
                        attributes.putOpt(Event.ReservedAttribute.ERROR_CODE, attributeError.getErrorCode());
                        attributes.putOpt(Event.ReservedAttribute.ERROR_MESSAGE, attributeError.getErrorMessage());
                    }
                } else {
                    attributes.putOpt(name, value);
                }
            } catch (JSONException exception) {
                LOG.error("error parsing json, error message:" + exception.getMessage()); //NOSONAR
            }
        } else {
            attributes.remove(name);
        }
    }

    /**
     * Adds items to this {@link AnalyticsEvent}.
     *
     * @param items The name of the attribute.
     */
    public void addItems(final ClickstreamItem[] items) {
        if (null == items || items.length == 0) {
            return;
        }
        try {
            for (ClickstreamItem item : items) {
                if (item.getAttributes().length() == 0) {
                    return;
                }
                Event.EventError attributeError =
                        EventChecker.checkItemAttribute(eventItems.length(), item);

                if (attributeError.getErrorCode() > 0 && !attributes.has(Event.ReservedAttribute.ERROR_CODE)) {
                    attributes.putOpt(Event.ReservedAttribute.ERROR_CODE, attributeError.getErrorCode());
                    attributes.putOpt(Event.ReservedAttribute.ERROR_MESSAGE, attributeError.getErrorMessage());
                }
                if (attributeError.getErrorCode() == 0) {
                    eventItems.put(item.getAttributes());
                }
            }
        } catch (JSONException exception) {
            LOG.error("error parsing json, error message:" + exception.getMessage()); //NOSONAR
        }
    }

    /**
     * add internal attribute for not check attribute error, for example not check attribute value length
     * can allow exception stack completely record.
     *
     * @param name  The name of the attribute.
     * @param value The value of the attribute.
     */
    protected void addInternalAttribute(final String name, final Object value) {
        if (null != value) {
            try {
                attributes.putOpt(name, value);
            } catch (JSONException exception) {
                LOG.error("error parsing json, error message:" + exception.getMessage()); //NOSONAR
            }
        } else {
            attributes.remove(name);
        }
    }

    /**
     * Determines if this {@link AnalyticsEvent} contains a specific attribute.
     *
     * @param attributeName The name of the attribute.
     * @return true if this {@link AnalyticsEvent} has an attribute with the
     * specified name, false otherwise.
     */
    public boolean hasAttribute(final String attributeName) {
        if (attributeName == null) {
            return false;
        }
        return attributes.has(attributeName);
    }

    /**
     * Returns the name/type of this {@link AnalyticsEvent}.
     *
     * @return the name/type of this {@link AnalyticsEvent}.
     */
    public String getEventType() {
        return this.eventType;
    }

    /**
     * Returns the String value of the attribute with the specified name.
     *
     * @param name The name of the attribute to return.
     * @return The attribute with the specified name, or null if attribute does
     * not exist.
     */
    public String getStringAttribute(final String name) {
        if (name == null) {
            return null;
        }
        try {
            return attributes.getString(name);
        } catch (JSONException exception) {
            LOG.warn("error to get attribute: " + name);
            return null;
        }
    }

    /**
     * Get the event timestamp.
     *
     * @return The timestamp of the event.
     */
    public Long getEventTimestamp() {
        return timestamp;
    }

    /**
     * Get the unique ID to check the sequence of the event.
     *
     * @return The unique ID.
     */
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * Get the name of the SDK.
     *
     * @return The SDK name.
     */
    public String getSdkName() {
        return sdkName;
    }

    /**
     * Get the version of the SDK.
     *
     * @return The SDK version.
     */
    public String getSdkVersion() {
        return sdkVersion;
    }

    /**
     * getAttributes.
     *
     * @return the attributes JSONObject.
     */
    public JSONObject getAttributes() {
        return attributes;
    }

    /**
     * Get event items.
     *
     * @return the JSONArray of items.
     */
    public JSONArray getItems() {
        return eventItems;
    }

    /**
     * Returns the App specific information.
     *
     * @return the App specific information.
     */
    public AndroidAppDetails getAppDetails() {
        return appDetails;
    }

    /**
     * Get the zone offset.
     *
     * @return The zone offset.
     */
    public int getZoneOffset() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.ZONE_OFFSET);
    }

    /**
     * Convert from event JSON format to string.
     *
     * @return The string of event JSON format.
     */
    @NonNull
    @Override
    public String toString() {
        final JSONObject json = toJSONObject();
        try {
            return json.toString(INDENTATION);
        } catch (final JSONException jsonException) {
            return json.toString();
        }
    }

    private String getOrDefault(Supplier<String> supplier) {
        try {
            return Optional.ofNullable(supplier.get()).orElse(UNKNOWN);
        } catch (Exception exception) {
            return UNKNOWN;
        }
    }

    /**
     * Convert event to JSON format.
     *
     * @return The JSON object.
     */
    @Override
    public JSONObject toJSONObject() {
        final Locale locale = this.deviceDetails.locale();

        final String localeString = getOrDefault(locale::toString);
        final String displayCountryString = getOrDefault(locale::getDisplayCountry);
        final String countryString = getOrDefault(locale::getCountry);
        final String languageString = getOrDefault(locale::getLanguage);
        final String carrierString = getOrDefault(() -> this.deviceDetails.carrier());

        final JSONBuilder builder = new JSONBuilder();

        // ****************************************************
        // ==================System Attributes=================
        // ****************************************************
        //builder.withAttribute("account_id", getAccountId()); // login ID
        // The non-login ID, it be changed after uninstalling then reinstalling the app.
        // https://developer.android.com/training/articles/user-data-ids
        builder.withAttribute("unique_id", getUniqueId());
        builder.withAttribute("event_type", getEventType()); // event_name AKA event_type, NOT NULL when type = track
        builder.withAttribute("event_id", getEventId());
        builder.withAttribute("app_id", getAppId());
        // Unix time is the number of seconds since January 1st, 1970.
        // https://nickb.dev/blog/designing-a-rest-api-unix-time-vs-iso-8601/
        builder.withAttribute("timestamp", getEventTimestamp());
        // ****************************************************
        // ==============Device Details Attributes=============
        // ****************************************************
        // The user's device ID, iOS take the user's IDFV or UUID,
        // Android takes androidID
        builder.withAttribute("device_id", getDeviceId());
        builder.withAttribute("platform", this.deviceDetails.platform());
        builder.withAttribute("os_version", this.deviceDetails.platformVersion());
        builder.withAttribute("make", this.deviceDetails.manufacturer());
        builder.withAttribute("brand", this.deviceDetails.brand());
        builder.withAttribute("model", this.deviceDetails.model());
        builder.withAttribute("locale", localeString);
        builder.withAttribute("carrier", carrierString);
        if (this.connectivity != null) {
            builder.withAttribute("network_type", getNetworkType());
        } else {
            builder.withAttribute("network_type", UNKNOWN);
        }

        builder.withAttribute("screen_height", getHeightPixels());
        builder.withAttribute("screen_width", getWidthPixels());
        builder.withAttribute("zone_offset", getZoneOffset());
        builder.withAttribute("system_language", languageString);

        // ****************************************************
        // ==============Geo Details Attributes=============
        // ****************************************************
        builder.withAttribute("country", displayCountryString);
        builder.withAttribute("country_code", countryString);

        // ****************************************************
        // ==============Session Attributes=============
        // ****************************************************
        if (session != null) {
            try {
                attributes.put("_session_id", session.getSessionID());
                attributes.put("_session_start_timestamp", session.getStartTime());
                attributes.put("_session_duration", session.getSessionDuration().longValue());
                attributes.put("_session_number", session.getSessionIndex());
            } catch (final JSONException jsonException) {
                LOG.error("Error serializing session information " + jsonException.getMessage());
            }
        }

        String screenName = ScreenRefererTool.getCurrentScreenName();
        String screenUniqueId = ScreenRefererTool.getCurrentScreenUniqueId();
        if (screenName != null) {
            try {
                attributes.put(Event.ReservedAttribute.SCREEN_NAME, screenName);
                attributes.put(Event.ReservedAttribute.SCREEN_UNIQUE_ID, screenUniqueId);
            } catch (final JSONException jsonException) {
                LOG.error("Error serializing session information " + jsonException.getMessage());
            }
        }

        // ****************************************************
        // ====SDK Details Attributes -- Prefix with 'sdk_'====
        // ****************************************************
        builder.withAttribute("sdk_version", this.sdkVersion);
        builder.withAttribute("sdk_name", this.sdkName);

        // ****************************************************
        // Application Details Attributes -- Prefix with 'app_'
        // ****************************************************
        builder.withAttribute("app_version", this.appDetails.versionName());
        builder.withAttribute("app_package_name", this.appDetails.packageName());
        builder.withAttribute("app_title", this.appDetails.getAppTitle());
        builder.withAttribute("items", this.eventItems);
        builder.withAttribute("user", this.userAttributes);
        builder.withAttribute("attributes", this.attributes);
        return builder.toJSONObject();
    }

    /**
     * Gets the current network connection type of the device.
     *
     * @return A String representing the network type:
     *         "Mobile" for WAN connections,
     *         "WIFI" for WiFi connections,
     *         or "UNKNOWN" if no recognized connection is available
     */
    public String getNetworkType() {
        if (this.connectivity.hasWAN()) {
            return "Mobile";
        } else if (this.connectivity.hasWifi()) {
            return "WIFI";
        } else {
            return UNKNOWN;
        }
    }
}
