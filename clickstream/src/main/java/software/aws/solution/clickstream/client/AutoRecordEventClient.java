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

import android.app.Activity;
import androidx.annotation.NonNull;

import com.amazonaws.logging.Log;
import com.amazonaws.logging.LogFactory;
import software.aws.solution.clickstream.client.system.AndroidPreferences;
import software.aws.solution.clickstream.client.util.PreferencesUtil;
import software.aws.solution.clickstream.client.util.StringUtil;

/**
 * Client for manage and send auto record event.
 */
public class AutoRecordEventClient {
    /**
     * Logger instance for AutoRecordEventClient.
     */
    private static final Log LOG = LogFactory.getLog(AutoRecordEventClient.class);
    private static final int MIN_ENGAGEMENT_TIME = 1000;
    /**
     * The context object wraps all the essential information from the app
     * that are required.
     */
    private final ClickstreamContext clickstreamContext;

    /**
     * whether app is first open from install.
     */
    private boolean isFirstOpen;

    /**
     * whether app is first time start from process launch.
     */
    private boolean isFirstTime = true;

    /**
     * whether the app has entered the background at least once.
     */
    private boolean isAppEndCalled = false;

    /**
     * current screen is entrances.
     */
    private boolean isEntrances;

    private long startEngageTimestamp;
    private long lastEngageTime;
    private final AndroidPreferences preferences;

    /**
     * CONSTRUCTOR.
     *
     * @param clickstreamContext The {@link ClickstreamContext}.
     * @throws IllegalArgumentException When the clickstreamContext.getAnalyticsClient is null.
     */
    public AutoRecordEventClient(@NonNull final ClickstreamContext clickstreamContext) {
        if (clickstreamContext.getAnalyticsClient() == null) {
            throw new IllegalArgumentException("A valid AnalyticsClient must be provided!");
        }
        this.clickstreamContext = clickstreamContext;
        this.preferences = clickstreamContext.getSystem().getPreferences();
        this.isFirstOpen = preferences.getBoolean("isFirstOpen", true);
    }

    /**
     * record view screen event.
     *
     * @param activity the activity to record.
     */
    public void recordViewScreenAutomatically(Activity activity) {
        Boolean isTrackScreenViewEvents = clickstreamContext.getClickstreamConfiguration().isTrackScreenViewEvents();
        if (Boolean.FALSE.equals(isTrackScreenViewEvents)) {
            return;
        }

        String screenId = activity.getClass().getCanonicalName();
        String screenName = activity.getClass().getSimpleName();
        String screenUniqueId = getScreenUniqueId(activity);
        if (ScreenRefererTool.isSameScreen(screenName, screenUniqueId)) {
            return;
        }
        ScreenRefererTool.setCurrentScreenId(screenId);
        ScreenRefererTool.setCurrentScreenName(screenName);
        ScreenRefererTool.setCurrentScreenUniqueId(screenUniqueId);
        final AnalyticsEvent event =
            this.clickstreamContext.getAnalyticsClient().createEvent(Event.PresetEvent.SCREEN_VIEW);
        recordScreenViewEvent(event);
    }

    /**
     * record view screen event manually.
     *
     * @param event the screen view event to be recorded.
     */
    public void recordViewScreenManually(AnalyticsEvent event) {
        String screenName = event.getStringAttribute(Event.ReservedAttribute.SCREEN_NAME);
        if (screenName != null) {
            String screenUniqueId = event.getStringAttribute(Event.ReservedAttribute.SCREEN_UNIQUE_ID);
            if (ScreenRefererTool.isSameScreen(screenName, screenUniqueId)) {
                return;
            }
            if (ScreenRefererTool.getCurrentScreenName() != null) {
                recordUserEngagement();
            }
            ScreenRefererTool.setCurrentScreenName(screenName);
            if (screenUniqueId != null) {
                ScreenRefererTool.setCurrentScreenUniqueId(screenUniqueId);
            }
            recordScreenViewEvent(event);
        } else {
            LOG.error("record an _screen_view event without the required screen name attribute");
            final AnalyticsEvent errorEvent =
                this.clickstreamContext.getAnalyticsClient().createEvent(Event.PresetEvent.CLICKSTREAM_ERROR);
            errorEvent.addAttribute(Event.ReservedAttribute.ERROR_CODE,
                Event.ErrorCode.SCREEN_VIEW_MISSING_SCREEN_NAME);
            errorEvent.addAttribute(Event.ReservedAttribute.ERROR_MESSAGE,
                "record an _screen_view event without the required screen name attribute");
            this.clickstreamContext.getAnalyticsClient().recordEvent(errorEvent);
        }
    }

    private void recordScreenViewEvent(AnalyticsEvent event) {
        long currentTimestamp = event.getEventTimestamp();
        startEngageTimestamp = currentTimestamp;
        event.addAttribute(Event.ReservedAttribute.SCREEN_ID, ScreenRefererTool.getCurrentScreenId());
        event.addAttribute(Event.ReservedAttribute.PREVIOUS_SCREEN_NAME, ScreenRefererTool.getPreviousScreenName());
        event.addAttribute(Event.ReservedAttribute.PREVIOUS_SCREEN_ID, ScreenRefererTool.getPreviousScreenId());
        event.addAttribute(Event.ReservedAttribute.PREVIOUS_SCREEN_UNIQUE_ID,
            ScreenRefererTool.getPreviousScreenUniqueId());
        long lastScreenViewEventTimestamp = PreferencesUtil.getPreviousScreenViewTimestamp(preferences);
        if (lastScreenViewEventTimestamp > 0) {
            event.addAttribute(Event.ReservedAttribute.PREVIOUS_TIMESTAMP, lastScreenViewEventTimestamp);
        }
        event.addAttribute(Event.ReservedAttribute.ENTRANCES, isEntrances ? 1 : 0);
        if (lastEngageTime > 0) {
            event.addAttribute(Event.ReservedAttribute.ENGAGEMENT_TIMESTAMP, lastEngageTime);
        }
        this.clickstreamContext.getAnalyticsClient().recordEvent(event);
        PreferencesUtil.savePreviousScreenViewTimestamp(preferences, currentTimestamp);
        isEntrances = false;
    }

    /**
     * get the screen unique id for activity.
     * the unique id calculated by getting the activity hash code.
     *
     * @param activity the activity for holding the screen
     * @return the unique of the activity
     */
    public String getScreenUniqueId(Activity activity) {
        return String.valueOf(activity.hashCode());
    }

    /**
     * record user engagement event.
     */
    public void recordUserEngagement() {
        if (startEngageTimestamp == 0) {
            return;
        }
        lastEngageTime = System.currentTimeMillis() - startEngageTimestamp;
        Boolean isTrackUserEngagementEvents = clickstreamContext
                .getClickstreamConfiguration().isTrackUserEngagementEvents();
        if (Boolean.TRUE.equals(isTrackUserEngagementEvents) && lastEngageTime > MIN_ENGAGEMENT_TIME) {
            final AnalyticsEvent event =
                this.clickstreamContext.getAnalyticsClient().createEvent(Event.PresetEvent.USER_ENGAGEMENT);
            event.addAttribute(Event.ReservedAttribute.ENGAGEMENT_TIMESTAMP, lastEngageTime);
            this.clickstreamContext.getAnalyticsClient().recordEvent(event);
        }
    }

    /**
     * the method for flush events when app move to background.
     */
    public void flushEvents() {
        LOG.debug("App moves to background and start to flush events");
        this.clickstreamContext.getAnalyticsClient().submitEvents();
    }

    /**
     * update start engage timestamp.
     */
    public void updateStartEngageTimestamp() {
        startEngageTimestamp = System.currentTimeMillis();
    }

    /**
     * reset last engagement time when app warm start.
     */
    public void resetLastEngageTime() {
        lastEngageTime = 0;
    }

    /**
     * check and record _app_update event.
     */
    private void checkAppVersionUpdate() {
        String previousAppVersion = preferences.getString("appVersion", ""); //NOSONAR
        if (!StringUtil.isNullOrEmpty(previousAppVersion)) {
            String currentVersion = clickstreamContext.getSystem().getAppDetails().versionName();
            if (!currentVersion.equals(previousAppVersion)) {
                final AnalyticsEvent event =
                    this.clickstreamContext.getAnalyticsClient().createEvent(Event.PresetEvent.APP_UPDATE);
                event.addAttribute(Event.ReservedAttribute.PREVIOUS_APP_VERSION, previousAppVersion);
                this.clickstreamContext.getAnalyticsClient().recordEvent(event);
                preferences.putString("appVersion", currentVersion); //NOSONAR
            }
        } else {
            preferences.putString("appVersion", clickstreamContext.getSystem().getAppDetails().versionName()); //NOSONAR
        }
    }

    /**
     * check and record _os_update event.
     */
    private void checkOSVersionUpdate() {
        String previousOSVersion = preferences.getString("osVersion", ""); //NOSONAR
        if (!StringUtil.isNullOrEmpty(previousOSVersion)) {
            String currentOSVersion = clickstreamContext.getSystem().getDeviceDetails().platformVersion();
            if (!currentOSVersion.equals(previousOSVersion)) {
                final AnalyticsEvent event =
                    this.clickstreamContext.getAnalyticsClient().createEvent(Event.PresetEvent.OS_UPDATE);
                event.addAttribute(Event.ReservedAttribute.PREVIOUS_OS_VERSION, previousOSVersion);
                this.clickstreamContext.getAnalyticsClient().recordEvent(event);
                preferences.putString("osVersion", currentOSVersion); //NOSONAR
            }
        } else {
            preferences.putString(
                    "osVersion",
                    clickstreamContext.getSystem().getDeviceDetails().platformVersion()
            ); //NOSONAR
        }
    }

    /**
     * handle the app start event.
     */
    public void handleAppStart() {
        if (isFirstTime) {
            checkAppVersionUpdate();
            checkOSVersionUpdate();
        }
        if (isFirstOpen) {
            final AnalyticsEvent event =
                this.clickstreamContext.getAnalyticsClient().createEvent(Event.PresetEvent.FIRST_OPEN);
            this.clickstreamContext.getAnalyticsClient().recordEvent(event);
            preferences.putBoolean("isFirstOpen", false);
            isFirstOpen = false;
        }
        if (isFirstTime || isAppEndCalled) {
            final AnalyticsEvent event =
                this.clickstreamContext.getAnalyticsClient().createEvent(Event.PresetEvent.APP_START);
            event.addAttribute(Event.ReservedAttribute.IS_FIRST_TIME, isFirstTime);
            this.clickstreamContext.getAnalyticsClient().recordEvent(event);
        }
        isFirstTime = false;
    }

    /**
     * handle session start events.
     */
    public void handleSessionStart() {
        final AnalyticsEvent event =
            this.clickstreamContext.getAnalyticsClient().createEvent(Event.PresetEvent.SESSION_START);
        this.clickstreamContext.getAnalyticsClient().recordEvent(event);
    }

    /**
     * handle the app end event.
     */
    public void handleAppEnd() {
        final AnalyticsEvent event =
            this.clickstreamContext.getAnalyticsClient().createEvent(Event.PresetEvent.APP_END);
        this.clickstreamContext.getAnalyticsClient().recordEvent(event);
        isAppEndCalled = true;
    }

    /**
     * setter for isEntrances.
     */
    public void setIsEntrances() {
        isEntrances = true;
    }
}

