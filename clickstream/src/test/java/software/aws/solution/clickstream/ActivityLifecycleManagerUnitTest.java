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
import android.os.Bundle;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.test.core.app.ApplicationProvider;

import com.amazonaws.logging.Log;
import com.amazonaws.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import software.aws.solution.clickstream.client.AutoRecordEventClient;
import software.aws.solution.clickstream.client.ClickstreamManager;
import software.aws.solution.clickstream.client.ScreenRefererTool;
import software.aws.solution.clickstream.client.SessionClient;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link ActivityLifecycleManager}.
 */
@RunWith(RobolectricTestRunner.class)
public final class ActivityLifecycleManagerUnitTest {
    private SessionClient sessionClient;
    private AutoRecordEventClient autoRecordEventClient;
    private Application.ActivityLifecycleCallbacks callbacks;
    private Log log;
    private LifecycleRegistry lifecycle;
    private LifecycleOwner lifecycleOwner;
    private ActivityLifecycleManager lifecycleManager;

    /**
     * Setup dependencies and object under test.
     *
     * @throws Exception exception
     */
    @Before
    public void setup() throws Exception {
        this.sessionClient = mock(SessionClient.class);
        ClickstreamManager clickstreamManager = mock(ClickstreamManager.class);
        this.autoRecordEventClient = mock(AutoRecordEventClient.class);
        when(clickstreamManager.getSessionClient()).thenReturn(sessionClient);
        when(clickstreamManager.getAutoRecordEventClient()).thenReturn(autoRecordEventClient);
        lifecycleManager = new ActivityLifecycleManager(clickstreamManager);
        this.callbacks = lifecycleManager;

        log = LogFactory.getLog(Application.ActivityLifecycleCallbacks.class);
        log.setLevel(LogFactory.Level.DEBUG);

        lifecycleOwner = mock(LifecycleOwner.class);
        lifecycle = new LifecycleRegistry(lifecycleOwner);
        lifecycleManager.startLifecycleTracking(ApplicationProvider.getApplicationContext(), lifecycle);
        ScreenRefererTool.clear();
    }

    /**
     * When the app is opened, Application open will be logged.
     */
    @Test
    public void testWhenAppOpened() {
        // Given: the launcher activity instance and bundle class instance.
        Activity activity = mock(Activity.class);
        Bundle bundle = mock(Bundle.class);

        // When: the app is opened main activity goes through the following lifecycle states.
        callbacks.onActivityCreated(activity, bundle);
        callbacks.onActivityStarted(activity);
        callbacks.onActivityResumed(activity);
    }

    /**
     * When the app is started, user interacts with the app and close the first activity.
     */
    @Test
    public void testWhenAppEnterAndExit() {
        // Given: the launcher activity and the app is opened.
        Activity activity = mock(Activity.class);
        Bundle bundle = mock(Bundle.class);
        // Activity is put in resume state when the app is opened.
        callbacks.onActivityCreated(activity, bundle);
        callbacks.onActivityStarted(activity);
        callbacks.onActivityResumed(activity);

        // When: home button is pressed, app goes to the background.
        // Activity stopped when home button is pressed and app goes to background.
        callbacks.onActivityPaused(activity);
        callbacks.onActivitySaveInstanceState(activity, bundle);
        callbacks.onActivityStopped(activity);
        callbacks.onActivityDestroyed(activity);
    }

    /**
     * test invalid context.
     */
    @Test
    public void testContextIsInValid() {
        ByteArrayOutputStream logContent = new ByteArrayOutputStream();
        PrintStream oldSystemOut = System.out;
        System.setOut(new PrintStream(logContent));

        Activity activity = mock(Activity.class);
        lifecycleManager.startLifecycleTracking(activity, lifecycle);

        System.setOut(oldSystemOut);
        assertTrue(
                logContent.toString().contains(
                        "The context is not ApplicationContext, so lifecycle events are not automatically recorded"));
    }

    /**
     * test onAppForegrounded event.
     */
    @Test
    public void testOnAppForegrounded() {
        assertNotNull(lifecycleOwner);
        when(sessionClient.initialSession()).thenReturn(true);
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);
        verify(autoRecordEventClient).updateStartEngageTimestamp();
        verify(autoRecordEventClient).handleAppStart();
        verify(sessionClient).initialSession();
        verify(autoRecordEventClient).setIsEntrances();
    }

    /**
     * test onAppBackgrounded event.
     */
    @Test
    public void testOnAppBackgrounded() {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        verify(sessionClient).storeSession();
        verify(autoRecordEventClient).recordUserEngagement();
    }

    /**
     * test screen view event.
     */
    @Test
    public void testScreenView() {
        Activity activity = mock(Activity.class);
        Bundle bundle = mock(Bundle.class);
        callbacks.onActivityCreated(activity, bundle);
        callbacks.onActivityStarted(activity);
        callbacks.onActivityResumed(activity);
        verify(autoRecordEventClient).recordViewScreenAutomatically(activity);
    }
}
