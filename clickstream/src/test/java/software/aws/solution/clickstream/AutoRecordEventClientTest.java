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

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.test.core.app.ApplicationProvider;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import software.aws.solution.clickstream.client.AnalyticsEvent;
import software.aws.solution.clickstream.client.AutoRecordEventClient;
import software.aws.solution.clickstream.client.ClickstreamContext;
import software.aws.solution.clickstream.client.ClickstreamManager;
import software.aws.solution.clickstream.client.Event;
import software.aws.solution.clickstream.client.EventRecorder;
import software.aws.solution.clickstream.client.ScreenRefererTool;
import software.aws.solution.clickstream.client.db.ClickstreamDBUtil;
import software.aws.solution.clickstream.client.util.StringUtil;
import software.aws.solution.clickstream.util.ReflectUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static software.aws.solution.clickstream.client.Event.ReservedAttribute;

/**
 * Test the AutoRecordEventClient.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
public class AutoRecordEventClientTest {
    private ClickstreamDBUtil dbUtil;
    private Application.ActivityLifecycleCallbacks callbacks;
    private ClickstreamContext clickstreamContext;
    private AutoRecordEventClient client;
    private LifecycleRegistry lifecycle;
    private LifecycleOwner lifecycleOwner;
    private ClickstreamManager clickstreamManager;

    /**
     * prepare AutoRecordEventClient and context.
     */
    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        dbUtil = new ClickstreamDBUtil(context);
        ClickstreamConfiguration configuration = ClickstreamConfiguration.getDefaultConfiguration();
        configuration.withAppId("demo-app")
            .withEndpoint("http://example.com/collect")
            .withSendEventsInterval(10000)
            .withTrackScreenViewEvents(true)
            .withTrackUserEngagementEvents(true);
        clickstreamManager = new ClickstreamManager(context, configuration);
        client = clickstreamManager.getAutoRecordEventClient();
        clickstreamContext = clickstreamManager.getClickstreamContext();
        callbacks = new ActivityLifecycleManager(clickstreamManager);

        ActivityLifecycleManager lifecycleManager = new ActivityLifecycleManager(clickstreamManager);
        lifecycleOwner = mock(LifecycleOwner.class);
        lifecycle = new LifecycleRegistry(lifecycleOwner);
        lifecycleManager.startLifecycleTracking(ApplicationProvider.getApplicationContext(), lifecycle);
    }

    /**
     * test events after SDK initialization.
     *
     * @throws Exception exception.
     */
    @Test
    public void testSDKInitializationEvents() throws Exception {
        try (Cursor cursor = dbUtil.queryAllEvents()) {
            List<String> eventList = new ArrayList<>();
            while (cursor.moveToNext()) {
                String eventString = cursor.getString(2);
                JSONObject jsonObject = new JSONObject(eventString);
                String eventName = jsonObject.getString("event_type");
                eventList.add(eventName);
            }
            assertEquals(3, eventList.size());
            assertEquals(Event.PresetEvent.FIRST_OPEN, eventList.get(0));
            assertEquals(Event.PresetEvent.APP_START, eventList.get(1));
            assertEquals(Event.PresetEvent.SESSION_START, eventList.get(2));
        }
    }

    /**
     * test SDK initialize with stored session.
     *
     * @throws Exception exception.
     */
    @Test
    public void testSDKInitializeWithStoredSession() throws Exception {
        ReflectUtil.invokeMethod(clickstreamManager, "handleSessionStart");
        try (Cursor cursor = dbUtil.queryAllEvents()) {
            List<String> eventList = new ArrayList<>();
            while (cursor.moveToNext()) {
                String eventString = cursor.getString(2);
                JSONObject jsonObject = new JSONObject(eventString);
                String eventName = jsonObject.getString("event_type");
                eventList.add(eventName);
            }
            assertEquals(3, eventList.size());
            assertEquals(Event.PresetEvent.FIRST_OPEN, eventList.get(0));
            assertEquals(Event.PresetEvent.APP_START, eventList.get(1));
            assertEquals(Event.PresetEvent.SESSION_START, eventList.get(2));
        }
    }

    /**
     * test SDK lazy initialization.
     *
     * @throws Exception exception.
     */
    @Test
    public void testLazyInitialization() throws Exception {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);
        try (Cursor cursor = dbUtil.queryAllEvents()) {
            List<String> eventList = new ArrayList<>();
            while (cursor.moveToNext()) {
                String eventString = cursor.getString(2);
                JSONObject jsonObject = new JSONObject(eventString);
                String eventName = jsonObject.getString("event_type");
                eventList.add(eventName);
            }
            assertEquals(3, eventList.size());
            assertEquals(Event.PresetEvent.FIRST_OPEN, eventList.get(0));
            assertEquals(Event.PresetEvent.APP_START, eventList.get(1));
            assertEquals(Event.PresetEvent.SESSION_START, eventList.get(2));
        }
    }


    /**
     * test record user engagement event after view screen more than 1 second.
     *
     * @throws Exception exception.
     */
    @Test
    public void testUserEngagementSuccess() throws Exception {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);
        Thread.sleep(1100);
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        try (Cursor cursor = dbUtil.queryAllEvents()) {
            List<String> eventList = new ArrayList<>();
            while (cursor.moveToNext()) {
                String eventString = cursor.getString(2);
                JSONObject jsonObject = new JSONObject(eventString);
                String eventName = jsonObject.getString("event_type");
                eventList.add(eventName);
                if (eventName.equals(Event.PresetEvent.USER_ENGAGEMENT)) {
                    JSONObject attributes = jsonObject.getJSONObject("attributes");
                    assertTrue(attributes.has(ReservedAttribute.ENGAGEMENT_TIMESTAMP));
                    assertFalse(attributes.has(ReservedAttribute.SCREEN_NAME));
                    assertFalse(attributes.has(ReservedAttribute.SCREEN_ID));
                    assertFalse(attributes.has(ReservedAttribute.SCREEN_UNIQUE_ID));
                    assertTrue(attributes.getLong(ReservedAttribute.ENGAGEMENT_TIMESTAMP) > 1000);
                }
            }
            assertEquals(Event.PresetEvent.FIRST_OPEN, eventList.get(0));
            assertEquals(Event.PresetEvent.APP_START, eventList.get(1));
            assertEquals(Event.PresetEvent.SESSION_START, eventList.get(2));
            assertEquals(Event.PresetEvent.USER_ENGAGEMENT, eventList.get(3));
            assertEquals(Event.PresetEvent.APP_END, eventList.get(4));
        }
    }


    /**
     * test first open screen view and session start events will have the same sessionId.
     *
     * @throws Exception exception.
     */
    @Test
    public void testEventsHaveSameSessionId() throws Exception {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);
        Activity activity = mock(Activity.class);
        Bundle bundle = mock(Bundle.class);
        callbacks.onActivityCreated(activity, bundle);
        callbacks.onActivityStarted(activity);
        callbacks.onActivityResumed(activity);
        assertNotNull(lifecycleOwner);
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        try (Cursor cursor = dbUtil.queryAllEvents()) {
            List<String> eventList = new ArrayList<>();
            Set<String> sessionIdSet = new HashSet<>();
            while (cursor.moveToNext()) {
                String eventString = cursor.getString(2);
                JSONObject jsonObject = new JSONObject(eventString);
                String eventName = jsonObject.getString("event_type");
                eventList.add(eventName);
                sessionIdSet.add(jsonObject.getJSONObject("attributes").getString("_session_id"));
            }
            assertEquals(Event.PresetEvent.FIRST_OPEN, eventList.get(0));
            assertEquals(Event.PresetEvent.APP_START, eventList.get(1));
            assertEquals(Event.PresetEvent.SESSION_START, eventList.get(2));
            assertEquals(Event.PresetEvent.SCREEN_VIEW, eventList.get(3));
            assertEquals(Event.PresetEvent.APP_END, eventList.get(4));
            assertEquals(1, sessionIdSet.size());
        }
    }

    /**
     * test record user engagement event after view screen less than 1 second.
     *
     * @throws Exception exception.
     */
    @Test
    public void testUserEngagementFail() throws Exception {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        try (Cursor cursor = dbUtil.queryAllEvents();) {
            List<String> eventList = new ArrayList<>();
            while (cursor.moveToNext()) {
                String eventString = cursor.getString(2);
                JSONObject jsonObject = new JSONObject(eventString);
                eventList.add(jsonObject.getString("event_type"));
            }
            assertFalse(eventList.contains(Event.PresetEvent.USER_ENGAGEMENT));
        }
    }

    /**
     * test record user engagement event when configure is disabled.
     *
     * @throws Exception exception.
     */
    @Test
    public void testCloseUserEngagementEvent() throws Exception {
        clickstreamContext.getClickstreamConfiguration().withTrackUserEngagementEvents(false);
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);
        Thread.sleep(1100);
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        try (Cursor cursor = dbUtil.queryAllEvents();) {
            List<String> eventList = new ArrayList<>();
            while (cursor.moveToNext()) {
                String eventString = cursor.getString(2);
                JSONObject jsonObject = new JSONObject(eventString);
                eventList.add(jsonObject.getString("event_type"));
            }
            assertFalse(eventList.contains(Event.PresetEvent.USER_ENGAGEMENT));
        }
    }

    /**
     * test view only one screen and record an screen view event.
     *
     * @throws Exception exception.
     */
    @Test
    public void testViewOneScreenEvent() throws Exception {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);
        Activity activity = mock(Activity.class);
        Bundle bundle = mock(Bundle.class);
        callbacks.onActivityCreated(activity, bundle);
        callbacks.onActivityStarted(activity);
        callbacks.onActivityResumed(activity);
        try (Cursor cursor = dbUtil.queryAllEvents()) {
            List<String> eventList = new ArrayList<>();
            while (cursor.moveToNext()) {
                String eventString = cursor.getString(2);
                JSONObject jsonObject = new JSONObject(eventString);
                String eventName = jsonObject.getString("event_type");
                eventList.add(eventName);
                if (eventName.equals(Event.PresetEvent.SCREEN_VIEW)) {
                    JSONObject attributes = jsonObject.getJSONObject("attributes");
                    assertNotNull(attributes.getString(ReservedAttribute.SCREEN_NAME));
                    assertNotNull(attributes.getString(ReservedAttribute.SCREEN_ID));
                    Assert.assertFalse(attributes.has(ReservedAttribute.PREVIOUS_SCREEN_NAME));
                    Assert.assertFalse(attributes.has(ReservedAttribute.PREVIOUS_SCREEN_ID));
                    Assert.assertEquals(1, attributes.getInt(ReservedAttribute.ENTRANCES));
                    Assert.assertFalse(attributes.has(ReservedAttribute.ENGAGEMENT_TIMESTAMP));
                    Assert.assertFalse(attributes.has(ReservedAttribute.PREVIOUS_TIMESTAMP));
                }
            }
            assertTrue(eventList.contains(Event.PresetEvent.SCREEN_VIEW));
        }
    }

    /**
     * test screen view event with screen unique id.
     *
     * @throws Exception exception.
     */
    @Test
    public void testScreenViewWithUniqueId() throws Exception {
        Activity activity = mock(Activity.class);
        Bundle bundle = mock(Bundle.class);
        callbacks.onActivityCreated(activity, bundle);
        callbacks.onActivityStarted(activity);
        callbacks.onActivityResumed(activity);
        try (Cursor cursor = dbUtil.queryAllEvents()) {
            cursor.moveToLast();
            String eventString = cursor.getString(2);
            JSONObject jsonObject = new JSONObject(eventString);
            String eventName = jsonObject.getString("event_type");
            assertEquals(eventName, Event.PresetEvent.SCREEN_VIEW);
            JSONObject attributes = jsonObject.getJSONObject("attributes");
            String screenUniqueId = attributes.getString(ReservedAttribute.SCREEN_UNIQUE_ID);
            assertNotNull(screenUniqueId);
            assertEquals(screenUniqueId, String.valueOf(activity.hashCode()));
        }
    }

    /**
     * test close screen view events in configuration.
     *
     * @throws Exception exception.
     */
    @Test
    public void testCloseScreenViewEventsRecord() throws Exception {
        clickstreamContext.getClickstreamConfiguration().withTrackScreenViewEvents(false);
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);
        Activity activity = mock(Activity.class);
        Bundle bundle = mock(Bundle.class);
        callbacks.onActivityCreated(activity, bundle);
        callbacks.onActivityStarted(activity);
        callbacks.onActivityResumed(activity);
        try (Cursor cursor = dbUtil.queryAllEvents()) {
            List<String> eventList = new ArrayList<>();
            while (cursor.moveToNext()) {
                String eventString = cursor.getString(2);
                JSONObject jsonObject = new JSONObject(eventString);
                String eventName = jsonObject.getString("event_type");
                eventList.add(eventName);
            }
            assertFalse(eventList.contains(Event.PresetEvent.SCREEN_VIEW));
        }
    }

    /**
     * test view two different screen and record last screen view event with
     * previous screen information.
     *
     * @throws Exception exception.
     */
    @Test
    public void testViewTwoScreenEvent() throws Exception {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);
        Activity activity1 = mock(ActivityA.class);
        Activity activity2 = mock(ActivityB.class);
        Bundle bundle = mock(Bundle.class);
        callbacks.onActivityCreated(activity1, bundle);
        callbacks.onActivityStarted(activity1);
        callbacks.onActivityResumed(activity1);
        callbacks.onActivityPaused(activity1);

        callbacks.onActivityCreated(activity2, bundle);
        callbacks.onActivityStarted(activity2);
        callbacks.onActivityResumed(activity2);

        try (Cursor cursor = dbUtil.queryAllEvents()) {
            cursor.moveToLast();
            String eventString = cursor.getString(2);
            JSONObject jsonObject = new JSONObject(eventString);
            String eventName = jsonObject.getString("event_type");
            assertEquals(Event.PresetEvent.SCREEN_VIEW, eventName);
            JSONObject attributes = jsonObject.getJSONObject("attributes");
            assertNotNull(attributes.getString(ReservedAttribute.SCREEN_NAME));
            assertNotNull(attributes.getString(ReservedAttribute.SCREEN_ID));
            assertNotNull(attributes.getString(ReservedAttribute.PREVIOUS_SCREEN_NAME));
            assertNotNull(attributes.getString(ReservedAttribute.PREVIOUS_SCREEN_ID));
            assertEquals(activity2.getClass().getSimpleName(), attributes.getString(ReservedAttribute.SCREEN_NAME));
            assertEquals(activity2.getClass().getCanonicalName(), attributes.getString(ReservedAttribute.SCREEN_ID));
            assertEquals(activity1.getClass().getSimpleName(),
                attributes.getString(ReservedAttribute.PREVIOUS_SCREEN_NAME));
            assertEquals(activity1.getClass().getCanonicalName(),
                attributes.getString(ReservedAttribute.PREVIOUS_SCREEN_ID));
            assertEquals(0, attributes.getInt(ReservedAttribute.ENTRANCES));
            Assert.assertTrue(attributes.has(ReservedAttribute.ENGAGEMENT_TIMESTAMP));
            Assert.assertTrue(attributes.getLong(ReservedAttribute.ENGAGEMENT_TIMESTAMP) >= 0);
        }
    }

    /**
     * test view two same screen and record one screen view event.
     *
     * @throws Exception exception.
     */
    @Test
    public void testViewTwoSameScreenEvent() throws Exception {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);
        Activity activity1 = mock(ActivityA.class);
        Bundle bundle = mock(Bundle.class);
        callbacks.onActivityCreated(activity1, bundle);
        callbacks.onActivityStarted(activity1);
        callbacks.onActivityResumed(activity1);
        callbacks.onActivityPaused(activity1);

        callbacks.onActivityCreated(activity1, bundle);
        callbacks.onActivityStarted(activity1);
        callbacks.onActivityResumed(activity1);

        try (Cursor cursor = dbUtil.queryAllEvents()) {
            List<String> eventList = new ArrayList<>();
            while (cursor.moveToNext()) {
                String eventString = cursor.getString(2);
                JSONObject jsonObject = new JSONObject(eventString);
                String eventName = jsonObject.getString("event_type");
                if (eventName.equals(Event.PresetEvent.SCREEN_VIEW)) {
                    eventList.add(eventName);
                }
            }
            assertEquals(1, eventList.size());
        }
    }

    /**
     * test view same screen twice and only record the last screen view engagement_time_msec.
     *
     * @throws Exception exception.
     */
    @Test
    public void testSameScreenViewAndRecordLastEngagementTime() throws Exception {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);
        Activity activityA = mock(ActivityA.class);
        Activity activityB = mock(ActivityB.class);
        Bundle bundle = mock(Bundle.class);
        callbacks.onActivityCreated(activityA, bundle);
        callbacks.onActivityStarted(activityA);
        callbacks.onActivityResumed(activityA);
        Thread.sleep(200);
        callbacks.onActivityPaused(activityA);
        Thread.sleep(50);
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);
        callbacks.onActivityCreated(activityA, bundle);
        callbacks.onActivityStarted(activityA);
        callbacks.onActivityResumed(activityA);
        Thread.sleep(200);

        callbacks.onActivityCreated(activityB, bundle);
        callbacks.onActivityStarted(activityB);
        callbacks.onActivityResumed(activityB);

        try (Cursor cursor = dbUtil.queryAllEvents()) {
            cursor.moveToLast();
            String eventString = cursor.getString(2);
            JSONObject jsonObject = new JSONObject(eventString);
            String eventName = jsonObject.getString("event_type");
            assertEquals(Event.PresetEvent.SCREEN_VIEW, eventName);
            JSONObject attributes = jsonObject.getJSONObject("attributes");
            assertEquals(activityB.getClass().getSimpleName(), attributes.getString(ReservedAttribute.SCREEN_NAME));
            assertEquals(activityB.getClass().getCanonicalName(), attributes.getString(ReservedAttribute.SCREEN_ID));
            assertTrue(attributes.getString(ReservedAttribute.SCREEN_UNIQUE_ID).contains(activityB.hashCode() + ""));
            assertTrue(attributes.getLong(ReservedAttribute.ENGAGEMENT_TIMESTAMP) < 400);
        }
    }

    /**
     * test view to different screen with _user_engagement event.
     *
     * @throws Exception exception.
     */
    @Test
    public void testViewTwoScreenWithUserEngagementEvent() throws Exception {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);
        Activity activityA = mock(ActivityA.class);
        Activity activityB = mock(ActivityB.class);
        Bundle bundle = mock(Bundle.class);
        callbacks.onActivityCreated(activityA, bundle);
        callbacks.onActivityStarted(activityA);
        callbacks.onActivityResumed(activityA);
        Thread.sleep(1100);
        callbacks.onActivityPaused(activityA);

        callbacks.onActivityCreated(activityB, bundle);
        callbacks.onActivityStarted(activityB);
        callbacks.onActivityResumed(activityB);

        try (Cursor cursor = dbUtil.queryAllEvents()) {
            cursor.moveToLast();
            String eventString = cursor.getString(2);
            JSONObject jsonObject = new JSONObject(eventString);
            String eventName = jsonObject.getString("event_type");
            assertEquals(Event.PresetEvent.SCREEN_VIEW, eventName);
            JSONObject attributes = jsonObject.getJSONObject("attributes");
            long engagementTime = attributes.getLong(ReservedAttribute.ENGAGEMENT_TIMESTAMP);

            cursor.moveToPrevious();
            String eventString1 = cursor.getString(2);
            JSONObject jsonObject1 = new JSONObject(eventString1);
            String eventName1 = jsonObject1.getString("event_type");
            assertEquals(Event.PresetEvent.USER_ENGAGEMENT, eventName1);
            JSONObject attributes1 = jsonObject1.getJSONObject("attributes");
            assertEquals(engagementTime, attributes1.getLong(ReservedAttribute.ENGAGEMENT_TIMESTAMP));
        }
    }

    /**
     * test previous timestamp is setting correct in screen events.
     *
     * @throws Exception exception.
     */
    @Test
    public void testPreviousTimestampInTwoScreenViewEvent() throws Exception {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);
        Activity activityA = mock(ActivityA.class);
        Activity activityB = mock(ActivityB.class);
        Bundle bundle = mock(Bundle.class);
        callbacks.onActivityCreated(activityA, bundle);
        callbacks.onActivityStarted(activityA);
        callbacks.onActivityResumed(activityA);
        callbacks.onActivityPaused(activityA);

        callbacks.onActivityCreated(activityB, bundle);
        callbacks.onActivityStarted(activityB);
        callbacks.onActivityResumed(activityB);

        try (Cursor cursor = dbUtil.queryAllEvents()) {
            cursor.moveToLast();
            String eventString = cursor.getString(2);
            JSONObject jsonObject = new JSONObject(eventString);
            String eventName = jsonObject.getString("event_type");
            assertEquals(Event.PresetEvent.SCREEN_VIEW, eventName);
            JSONObject attributes = jsonObject.getJSONObject("attributes");
            long previousTimestamp = attributes.getLong(ReservedAttribute.PREVIOUS_TIMESTAMP);

            cursor.moveToPrevious();
            String previousEventString = cursor.getString(2);
            JSONObject previousJsonObject = new JSONObject(previousEventString);
            assertEquals(previousTimestamp, previousJsonObject.getLong("timestamp"));
        }
    }

    /**
     * test app warm start without engagement_mesc attribute.
     *
     * @throws Exception exception.
     */
    @Test
    public void testAppWarmStartWithoutEngagementTime() throws Exception {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);
        Activity activityA = mock(ActivityA.class);

        Bundle bundle = mock(Bundle.class);
        callbacks.onActivityCreated(activityA, bundle);
        callbacks.onActivityStarted(activityA);
        callbacks.onActivityResumed(activityA);
        callbacks.onActivityPaused(activityA);
        callbacks.onActivityDestroyed(activityA);

        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);

        Activity activityA1 = mock(ActivityA.class);
        callbacks.onActivityCreated(activityA1, bundle);
        callbacks.onActivityStarted(activityA1);
        callbacks.onActivityResumed(activityA1);

        try (Cursor cursor = dbUtil.queryAllEvents()) {
            cursor.moveToLast();
            String eventString = cursor.getString(2);
            JSONObject jsonObject = new JSONObject(eventString);
            String eventName = jsonObject.getString("event_type");
            assertEquals(Event.PresetEvent.SCREEN_VIEW, eventName);
            JSONObject attributes = jsonObject.getJSONObject("attributes");
            Assert.assertFalse(attributes.has(ReservedAttribute.ENGAGEMENT_TIMESTAMP));
        }
    }

    /**
     * test record screen view event manually without screen name.
     *
     * @throws Exception exception
     */
    @Test
    public void testRecordScreenViewManuallyWithoutScreenName() throws Exception {
        final AnalyticsEvent event = clickstreamContext.getAnalyticsClient().createEvent(Event.PresetEvent.SCREEN_VIEW);
        client.recordViewScreenManually(event);
        try (Cursor cursor = dbUtil.queryAllEvents()) {
            cursor.moveToLast();
            String eventString = cursor.getString(2);
            JSONObject jsonObject = new JSONObject(eventString);
            String eventName = jsonObject.getString("event_type");
            assertEquals(Event.PresetEvent.CLICKSTREAM_ERROR, eventName);
            JSONObject attributes = jsonObject.getJSONObject("attributes");
            Assert.assertEquals(Event.ErrorCode.SCREEN_VIEW_MISSING_SCREEN_NAME,
                attributes.getInt(ReservedAttribute.ERROR_CODE));
        }
    }

    /**
     * test record screen view event manually without screen unique id and
     * will add the current Activity's screen unique id.
     *
     * @throws Exception exception
     */
    @Test
    public void testRecordScreenViewManuallyWithoutScreenUniqueId() throws Exception {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);
        Activity activityA = mock(ActivityA.class);
        Fragment fragmentA = mock(FragmentA.class);
        Bundle bundle = mock(Bundle.class);
        callbacks.onActivityCreated(activityA, bundle);
        callbacks.onActivityStarted(activityA);
        callbacks.onActivityResumed(activityA);
        final AnalyticsEvent event = clickstreamContext.getAnalyticsClient().createEvent(Event.PresetEvent.SCREEN_VIEW);
        event.addAttribute(ClickstreamAnalytics.Attr.SCREEN_NAME, fragmentA.getClass().getSimpleName());
        client.recordViewScreenManually(event);
        try (Cursor cursor = dbUtil.queryAllEvents()) {
            cursor.moveToLast();
            String eventString = cursor.getString(2);
            JSONObject jsonObject = new JSONObject(eventString);
            String eventName = jsonObject.getString("event_type");
            assertEquals(Event.PresetEvent.SCREEN_VIEW, eventName);
            JSONObject attributes = jsonObject.getJSONObject("attributes");
            Assert.assertEquals(fragmentA.getClass().getSimpleName(),
                attributes.getString(ReservedAttribute.SCREEN_NAME));
            Assert.assertEquals(String.valueOf(activityA.hashCode()),
                attributes.getString(ReservedAttribute.SCREEN_UNIQUE_ID));
        }
    }


    /**
     * test record screen view event manually between two activity screen view.
     *
     * @throws Exception exception
     */
    @Test
    public void testRecordCustomScreenViewBetweenActivityResume() throws Exception {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);
        Activity activityA = mock(ActivityA.class);
        Activity activityB = mock(ActivityB.class);
        Bundle bundle = mock(Bundle.class);
        // Record activityA screen view
        callbacks.onActivityCreated(activityA, bundle);
        callbacks.onActivityStarted(activityA);
        callbacks.onActivityResumed(activityA);

        // Record custom Fragment screen view
        Fragment fragmentA = mock(FragmentA.class);
        String uniqueIDOfFragmentA = String.valueOf(fragmentA.hashCode());
        final AnalyticsEvent event = clickstreamContext.getAnalyticsClient().createEvent(Event.PresetEvent.SCREEN_VIEW);
        event.addAttribute(ClickstreamAnalytics.Attr.SCREEN_NAME, fragmentA.getClass().getSimpleName());
        event.addAttribute(ClickstreamAnalytics.Attr.SCREEN_UNIQUE_ID, uniqueIDOfFragmentA);
        client.recordViewScreenManually(event);

        // Record ActivityB Screen View
        callbacks.onActivityPaused(activityA);
        callbacks.onActivityCreated(activityB, bundle);
        callbacks.onActivityStarted(activityB);
        callbacks.onActivityResumed(activityB);
        try (Cursor cursor = dbUtil.queryAllEvents()) {
            cursor.moveToLast();
            // assert that ActivityB's screen view event previews screen is FragmentA
            String eventString = cursor.getString(2);
            JSONObject jsonObject = new JSONObject(eventString);
            String eventName = jsonObject.getString("event_type");
            assertEquals(Event.PresetEvent.SCREEN_VIEW, eventName);
            JSONObject attributes = jsonObject.getJSONObject("attributes");
            Assert.assertEquals(activityB.getClass().getSimpleName(),
                attributes.getString(ReservedAttribute.SCREEN_NAME));
            Assert.assertEquals(fragmentA.getClass().getSimpleName(),
                attributes.getString(ReservedAttribute.PREVIOUS_SCREEN_NAME));
            Assert.assertEquals(uniqueIDOfFragmentA,
                attributes.getString(ReservedAttribute.PREVIOUS_SCREEN_UNIQUE_ID));

            // assert that FragmentA's attribute contains ActivityA's screen attributes
            cursor.moveToPrevious();
            String eventString2 = cursor.getString(2);
            JSONObject jsonObject2 = new JSONObject(eventString2);
            String eventName2 = jsonObject2.getString("event_type");
            assertEquals(Event.PresetEvent.SCREEN_VIEW, eventName2);
            JSONObject attributes2 = jsonObject2.getJSONObject("attributes");
            Assert.assertEquals(fragmentA.getClass().getSimpleName(),
                attributes2.getString(ReservedAttribute.SCREEN_NAME));
            Assert.assertEquals(uniqueIDOfFragmentA,
                attributes2.getString(ReservedAttribute.SCREEN_UNIQUE_ID));
            Assert.assertEquals(activityA.getClass().getSimpleName(),
                attributes2.getString(ReservedAttribute.PREVIOUS_SCREEN_NAME));
            Assert.assertEquals(String.valueOf(activityA.hashCode()),
                attributes2.getString(ReservedAttribute.PREVIOUS_SCREEN_UNIQUE_ID));
        }
    }

    /**
     * test record two screen view event manually when automatic tracking is disabled.
     *
     * @throws Exception exception
     */
    @Test
    public void testRecordTwoScreenViewWhenAutoTrackIsDisabled() throws Exception {
        clickstreamContext.getClickstreamConfiguration().withTrackScreenViewEvents(false);
        Fragment fragmentA = mock(FragmentA.class);
        String uniqueIDOfFragmentA = String.valueOf(fragmentA.hashCode());
        Fragment fragmentB = mock(FragmentB.class);
        String uniqueIDOfFragmentB = String.valueOf(fragmentB.hashCode());
        final AnalyticsEvent eventA =
            clickstreamContext.getAnalyticsClient().createEvent(Event.PresetEvent.SCREEN_VIEW);
        eventA.addAttribute(ClickstreamAnalytics.Attr.SCREEN_NAME, fragmentA.getClass().getSimpleName());
        eventA.addAttribute(ClickstreamAnalytics.Attr.SCREEN_UNIQUE_ID, uniqueIDOfFragmentA);
        client.recordViewScreenManually(eventA);
        Thread.sleep(1100);
        final AnalyticsEvent eventB =
            clickstreamContext.getAnalyticsClient().createEvent(Event.PresetEvent.SCREEN_VIEW);
        eventB.addAttribute(ClickstreamAnalytics.Attr.SCREEN_NAME, fragmentB.getClass().getSimpleName());
        eventB.addAttribute(ClickstreamAnalytics.Attr.SCREEN_UNIQUE_ID, uniqueIDOfFragmentB);
        client.recordViewScreenManually(eventB);
        try (Cursor cursor = dbUtil.queryAllEvents()) {
            // assert last screen view
            cursor.moveToLast();
            String eventString = cursor.getString(2);
            JSONObject jsonObject = new JSONObject(eventString);
            String eventName = jsonObject.getString("event_type");
            assertEquals(Event.PresetEvent.SCREEN_VIEW, eventName);
            JSONObject attributes = jsonObject.getJSONObject("attributes");
            Assert.assertEquals(fragmentB.getClass().getSimpleName(),
                attributes.getString(ReservedAttribute.SCREEN_NAME));
            Assert.assertEquals(uniqueIDOfFragmentB, attributes.getString(ReservedAttribute.SCREEN_UNIQUE_ID));
            Assert.assertEquals(fragmentA.getClass().getSimpleName(),
                attributes.getString(ReservedAttribute.PREVIOUS_SCREEN_NAME));
            Assert.assertEquals(uniqueIDOfFragmentA, attributes.getString(ReservedAttribute.PREVIOUS_SCREEN_UNIQUE_ID));
            // assert user engagement of fragmentA
            cursor.moveToPrevious();
            String eventString1 = cursor.getString(2);
            JSONObject jsonObject1 = new JSONObject(eventString1);
            String eventName1 = jsonObject1.getString("event_type");
            assertEquals(Event.PresetEvent.USER_ENGAGEMENT, eventName1);
            JSONObject attributes1 = jsonObject1.getJSONObject("attributes");
            Assert.assertTrue(attributes1.getLong(ReservedAttribute.ENGAGEMENT_TIMESTAMP) > 1100);
            Assert.assertEquals(fragmentA.getClass().getSimpleName(),
                attributes1.getString(ReservedAttribute.SCREEN_NAME));
            Assert.assertEquals(uniqueIDOfFragmentA, attributes1.getString(ReservedAttribute.SCREEN_UNIQUE_ID));
            // assert screen view of fragmentA
            cursor.moveToPrevious();
            String eventString2 = cursor.getString(2);
            JSONObject jsonObject2 = new JSONObject(eventString2);
            String eventName2 = jsonObject2.getString("event_type");
            assertEquals(Event.PresetEvent.SCREEN_VIEW, eventName2);
            JSONObject attributes2 = jsonObject2.getJSONObject("attributes");
            Assert.assertEquals(fragmentA.getClass().getSimpleName(),
                attributes2.getString(ReservedAttribute.SCREEN_NAME));
            Assert.assertEquals(uniqueIDOfFragmentA, attributes2.getString(ReservedAttribute.SCREEN_UNIQUE_ID));
            Assert.assertFalse(attributes2.has(ReservedAttribute.PREVIOUS_SCREEN_NAME));
            Assert.assertFalse(attributes2.has(ReservedAttribute.PREVIOUS_SCREEN_UNIQUE_ID));
        }
    }

    /**
     * test record two same screen view event manually and will not record the last screen view event.
     *
     * @throws Exception exception
     */
    @Test
    public void testRecordTwoSameScreenViewManually() throws Exception {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);
        Fragment fragmentA = mock(FragmentA.class);
        final AnalyticsEvent event1 =
            clickstreamContext.getAnalyticsClient().createEvent(Event.PresetEvent.SCREEN_VIEW);
        event1.addAttribute(ClickstreamAnalytics.Attr.SCREEN_NAME, fragmentA.getClass().getSimpleName());
        event1.addAttribute(ClickstreamAnalytics.Attr.SCREEN_UNIQUE_ID, fragmentA.hashCode());
        client.recordViewScreenManually(event1);

        final AnalyticsEvent event2 =
            clickstreamContext.getAnalyticsClient().createEvent(Event.PresetEvent.SCREEN_VIEW);
        event2.addAttribute(ClickstreamAnalytics.Attr.SCREEN_NAME, fragmentA.getClass().getSimpleName());
        event2.addAttribute(ClickstreamAnalytics.Attr.SCREEN_UNIQUE_ID, fragmentA.hashCode());
        client.recordViewScreenManually(event2);
        try (Cursor cursor = dbUtil.queryAllEvents()) {
            cursor.moveToLast();
            String eventString = cursor.getString(2);
            JSONObject jsonObject = new JSONObject(eventString);
            String eventName = jsonObject.getString("event_type");
            assertEquals(Event.PresetEvent.SCREEN_VIEW, eventName);
            JSONObject attributes = jsonObject.getJSONObject("attributes");
            Assert.assertEquals(fragmentA.getClass().getSimpleName(),
                attributes.getString(ReservedAttribute.SCREEN_NAME));

            cursor.moveToPrevious();
            String eventString2 = cursor.getString(2);
            JSONObject jsonObject2 = new JSONObject(eventString2);
            String eventName2 = jsonObject2.getString("event_type");
            assertNotEquals(Event.PresetEvent.SCREEN_VIEW, eventName2);
        }
    }

    /**
     * test app version not update.
     *
     * @throws Exception exception
     */
    @Test
    public void testAppVersionForNotUpdate() throws Exception {
        ReflectUtil.modifyField(clickstreamContext.getSystem().getAppDetails(), "versionName", "1.0");
        ReflectUtil.invokeMethod(client, "checkAppVersionUpdate");
        ReflectUtil.invokeMethod(client, "checkAppVersionUpdate");

        String previousAppVersion = clickstreamContext.getSystem().getPreferences().getString("appVersion", "");
        assertNotNull(previousAppVersion);

        try (Cursor cursor = dbUtil.queryAllEvents()) {
            List<String> eventList = new ArrayList<>();
            while (cursor.moveToNext()) {
                String eventString = cursor.getString(2);
                JSONObject jsonObject = new JSONObject(eventString);
                String eventName = jsonObject.getString("event_type");
                eventList.add(eventName);
            }
            assertFalse(eventList.contains(Event.PresetEvent.APP_UPDATE));
        }
    }

    /**
     * test app version update.
     *
     * @throws Exception exception
     */
    @Test
    public void testAppVersionForUpdate() throws Exception {
        ReflectUtil.modifyField(clickstreamContext.getSystem().getAppDetails(), "versionName", "1.0");
        ReflectUtil.invokeMethod(client, "checkAppVersionUpdate");

        String previousAppVersion = clickstreamContext.getSystem().getPreferences().getString("appVersion", "");
        assertNotNull(previousAppVersion);

        ReflectUtil.modifyField(clickstreamContext.getSystem().getAppDetails(), "versionName", "2.0");
        ReflectUtil.invokeMethod(client, "checkAppVersionUpdate");

        try (Cursor cursor = dbUtil.queryAllEvents()) {
            List<String> eventList = new ArrayList<>();
            while (cursor.moveToNext()) {
                String eventString = cursor.getString(2);
                JSONObject jsonObject = new JSONObject(eventString);
                String eventName = jsonObject.getString("event_type");
                eventList.add(eventName);
                if (eventName.equals(Event.PresetEvent.APP_UPDATE)) {
                    JSONObject attributes = jsonObject.getJSONObject("attributes");
                    assertEquals("1.0", attributes.getString(ReservedAttribute.PREVIOUS_APP_VERSION));
                    assertEquals("2.0", jsonObject.getString("app_version"));
                }
            }
            assertTrue(eventList.contains(Event.PresetEvent.APP_UPDATE));
        }
    }

    /**
     * test OS version not update.
     *
     * @throws Exception exception
     */
    @Test
    public void testOSVersionForNotUpdate() throws Exception {
        String previousOSVersion = clickstreamContext.getSystem().getPreferences().getString("osVersion", "");
        assertNotNull(previousOSVersion);

        try (Cursor cursor = dbUtil.queryAllEvents()) {
            List<String> eventList = new ArrayList<>();
            while (cursor.moveToNext()) {
                String eventString = cursor.getString(2);
                JSONObject jsonObject = new JSONObject(eventString);
                String eventName = jsonObject.getString("event_type");
                eventList.add(eventName);
            }
            assertFalse(eventList.contains(Event.PresetEvent.OS_UPDATE));
        }
    }

    /**
     * test OS version update.
     *
     * @throws Exception exception
     */
    @Test
    public void testOSVersionForUpdate() throws Exception {
        ReflectUtil.invokeMethod(client, "checkOSVersionUpdate");
        String previousOSVersion = clickstreamContext.getSystem().getPreferences().getString("osVersion", "");
        assertFalse(StringUtil.isNullOrEmpty(previousOSVersion));

        ReflectionHelpers.setStaticField(Build.VERSION.class, "RELEASE", "10");
        ReflectUtil.invokeMethod(client, "checkOSVersionUpdate");
        Thread.sleep(1000);
        try (Cursor cursor = dbUtil.queryAllEvents()) {
            List<String> eventList = new ArrayList<>();
            while (cursor.moveToNext()) {
                String eventString = cursor.getString(2);
                JSONObject jsonObject = new JSONObject(eventString);
                String eventName = jsonObject.getString("event_type");
                eventList.add(eventName);

                if (eventName.equals(Event.PresetEvent.OS_UPDATE)) {
                    JSONObject attributes = jsonObject.getJSONObject("attributes");
                    assertEquals("9", attributes.getString(ReservedAttribute.PREVIOUS_OS_VERSION));
                    assertEquals("10", jsonObject.getString("os_version"));
                }
            }
            assertTrue(eventList.contains(Event.PresetEvent.OS_UPDATE));
        }
    }

    /**
     * test handleFirstOpen method.
     *
     * @throws Exception exception.
     */
    @Test
    public void testHandleFirstOpen() throws Exception {
        client.handleAppStart();
        assertEquals(3, dbUtil.getTotalNumber());
        Cursor cursor = dbUtil.queryAllEvents();
        cursor.moveToFirst();
        String eventString = cursor.getString(2);
        JSONObject jsonObject = new JSONObject(eventString);
        String eventType = jsonObject.getString("event_type");
        assertEquals(Event.PresetEvent.FIRST_OPEN, eventType);
        JSONObject attributes = jsonObject.getJSONObject("attributes");
        assertNotNull(attributes.getString("_session_id"));
        assertNotNull(attributes.getString("_session_start_timestamp"));
        assertNotNull(attributes.getString("_session_duration"));
        assertNotNull(attributes.getString("_session_number"));
        cursor.close();
    }

    /**
     * test execute handleFirstOpen method multi times.
     *
     * @throws Exception exception.
     */
    @Test
    public void testHandleFirstOpenMultiTimes() throws Exception {
        client.handleAppStart();
        client.handleAppStart();
        client.handleAppStart();
        assertEquals(3, dbUtil.getTotalNumber());
        Cursor cursor = dbUtil.queryAllEvents();
        cursor.moveToFirst();
        String eventString = cursor.getString(2);
        JSONObject jsonObject = new JSONObject(eventString);
        String eventType = jsonObject.getString("event_type");
        assertEquals(Event.PresetEvent.FIRST_OPEN, eventType);
        cursor.close();
    }

    /**
     * test execute handleAppStart.
     *
     * @throws Exception exception.
     */
    @Test
    public void testHandleAppStart() throws Exception {
        Activity activity1 = mock(Activity.class);
        Bundle bundle = mock(Bundle.class);
        callbacks.onActivityCreated(activity1, bundle);
        callbacks.onActivityStarted(activity1);
        callbacks.onActivityResumed(activity1);
        client.handleAppEnd();
        client.handleAppStart();
        assertEquals(6, dbUtil.getTotalNumber());
        try (Cursor cursor = dbUtil.queryAllEvents()) {
            List<JSONObject> eventList = new ArrayList<>();
            while (cursor.moveToNext()) {
                String eventString = cursor.getString(2);
                JSONObject jsonObject = new JSONObject(eventString);
                eventList.add(jsonObject);
            }
            assertEquals(Event.PresetEvent.FIRST_OPEN, eventList.get(0).getString("event_type"));
            assertEquals(Event.PresetEvent.APP_START, eventList.get(1).getString("event_type"));
            JSONObject appStart1 = eventList.get(1).getJSONObject("attributes");
            assertTrue(appStart1.getBoolean(Event.ReservedAttribute.IS_FIRST_TIME));
            assertFalse(appStart1.has(ReservedAttribute.SCREEN_NAME));
            assertFalse(appStart1.has(Event.ReservedAttribute.SCREEN_ID));

            assertEquals(Event.PresetEvent.SESSION_START, eventList.get(2).getString("event_type"));
            assertEquals(Event.PresetEvent.SCREEN_VIEW, eventList.get(3).getString("event_type"));
            assertEquals(Event.PresetEvent.APP_END, eventList.get(4).getString("event_type"));
            assertEquals(Event.PresetEvent.APP_START, eventList.get(5).getString("event_type"));
            JSONObject appStart2 = eventList.get(5).getJSONObject("attributes");
            assertFalse(appStart2.getBoolean(Event.ReservedAttribute.IS_FIRST_TIME));
            assertTrue(appStart2.has(ReservedAttribute.SCREEN_NAME));
            assertTrue(appStart2.has(ReservedAttribute.SCREEN_UNIQUE_ID));
            assertEquals(activity1.getClass().getSimpleName(), appStart2.getString(ReservedAttribute.SCREEN_NAME));
            assertEquals(String.valueOf(activity1.hashCode()), appStart2.getString(ReservedAttribute.SCREEN_UNIQUE_ID));
        }
    }

    /**
     * test app end event.
     *
     * @throws Exception exception.
     */
    @Test
    public void testAppEnd() throws Exception {
        Activity activity = mock(Activity.class);
        Bundle bundle = mock(Bundle.class);
        callbacks.onActivityCreated(activity, bundle);
        callbacks.onActivityStarted(activity);
        callbacks.onActivityResumed(activity);
        client.handleAppEnd();
        try (Cursor cursor = dbUtil.queryAllEvents()) {
            cursor.moveToLast();
            String eventString = cursor.getString(2);
            JSONObject jsonObject = new JSONObject(eventString);
            String eventType = jsonObject.getString("event_type");
            JSONObject attributes = jsonObject.getJSONObject("attributes");
            assertEquals(Event.PresetEvent.APP_END, eventType);
            assertTrue(attributes.has(ReservedAttribute.SCREEN_NAME));
            assertTrue(attributes.has(ReservedAttribute.SCREEN_UNIQUE_ID));
        }
    }

    /**
     * test case for app move to background with flush event.
     *
     * @throws Exception exception.
     */
    @Test
    public void testBackgroundRequest() throws Exception {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);
        Thread.sleep(1100);
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        EventRecorder eventRecorder =
            (EventRecorder) ReflectUtil.getField(clickstreamContext.getAnalyticsClient(), "eventRecorder");
        ExecutorService executorService =
            (ExecutorService) ReflectUtil.getField(eventRecorder, "submissionRunnableQueue");
        assertEquals(1, ((ThreadPoolExecutor) executorService).getActiveCount());
    }


    /**
     * test hide page and reopen page after session timeout and will record page view event.
     *
     * @throws Exception exception.
     */
    @Test
    public void testSessionTimeoutAfterReopenTheApp() throws Exception {
        clickstreamContext.getClickstreamConfiguration().withSessionTimeoutDuration(0);
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);
        Activity activityA = mock(ActivityA.class);
        Bundle bundle = mock(Bundle.class);
        // Record activityA screen view
        callbacks.onActivityCreated(activityA, bundle);
        callbacks.onActivityStarted(activityA);
        callbacks.onActivityResumed(activityA);
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        Thread.sleep(100);
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);
        try (Cursor cursor = dbUtil.queryAllEvents()) {
            cursor.moveToLast();
            String eventString = cursor.getString(2);
            JSONObject jsonObject = new JSONObject(eventString);
            String eventType = jsonObject.getString("event_type");
            JSONObject attributes = jsonObject.getJSONObject("attributes");
            assertEquals(Event.PresetEvent.SCREEN_VIEW, eventType);
            assertTrue(attributes.has(ReservedAttribute.SCREEN_NAME));
            assertTrue(attributes.has(ReservedAttribute.SCREEN_UNIQUE_ID));
            assertFalse(attributes.has(ReservedAttribute.PREVIOUS_SCREEN_NAME));
            assertFalse(attributes.has(ReservedAttribute.PREVIOUS_SCREEN_UNIQUE_ID));
            assertEquals(1, attributes.getInt(ReservedAttribute.ENTRANCES));

            cursor.moveToPrevious();
            String eventString2 = cursor.getString(2);
            JSONObject jsonObject2 = new JSONObject(eventString2);
            String eventName2 = jsonObject2.getString("event_type");
            assertEquals(Event.PresetEvent.SESSION_START, eventName2);

            // assert that the second app start event will have the same session id
            cursor.moveToPrevious();
            String eventString3 = cursor.getString(2);
            JSONObject jsonObject3 = new JSONObject(eventString3);
            String eventName3 = jsonObject3.getString("event_type");
            assertEquals(Event.PresetEvent.APP_START, eventName3);
            assertEquals(jsonObject3.getJSONObject("attributes").getString("_session_id"),
                jsonObject2.getJSONObject("attributes").getString("_session_id"));
        }
    }

    /**
     * test init autoRecordEventClient with null analyticsClient.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInitAutoRecordEventClientWithNullAnalyticsClient() {
        ClickstreamContext context = mock(ClickstreamContext.class);
        new AutoRecordEventClient(context);
    }

    /**
     * close db.
     */
    @After
    public void tearDown() {
        ScreenRefererTool.clear();
        dbUtil.closeDB();
    }

    static class ActivityA extends Activity {
    }

    static class ActivityB extends Activity {
    }

    static class FragmentA extends Fragment {
    }

    static class FragmentB extends Fragment {
    }
}
