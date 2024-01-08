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

import android.app.Application;
import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.test.core.app.ApplicationProvider;

import com.amplifyframework.core.Amplify;

import com.amazonaws.logging.Log;
import com.github.dreamhead.moco.HttpServer;
import com.github.dreamhead.moco.Runner;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import software.aws.solution.clickstream.client.AnalyticsClient;
import software.aws.solution.clickstream.client.ClickstreamConfiguration;
import software.aws.solution.clickstream.client.ClickstreamContext;
import software.aws.solution.clickstream.client.Event;
import software.aws.solution.clickstream.client.EventRecorder;
import software.aws.solution.clickstream.client.db.ClickstreamDBUtil;
import software.aws.solution.clickstream.util.CustomOkhttpDns;
import software.aws.solution.clickstream.util.ReflectUtil;

import java.util.Map;

import static com.github.dreamhead.moco.Moco.and;
import static com.github.dreamhead.moco.Moco.by;
import static com.github.dreamhead.moco.Moco.eq;
import static com.github.dreamhead.moco.Moco.header;
import static com.github.dreamhead.moco.Moco.httpServer;
import static com.github.dreamhead.moco.Moco.status;
import static com.github.dreamhead.moco.Moco.text;
import static com.github.dreamhead.moco.Moco.uri;
import static com.github.dreamhead.moco.Runner.runner;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class IntegrationTest {
    private static final String COLLECT_SUCCESS = "/collect/success";
    private static final String COLLECT_SUCCESS1 = "/collect/success1";
    private static final String COLLECT_FOR_AUTH = "/collect/auth";
    private static final String COLLECT_FAIL = "/collect/fail";
    private static final String COLLECT_HOST = "http://localhost:8082";
    private static Runner runner;
    private static Handler handler;
    private AWSClickstreamPlugin plugin;
    private ClickstreamDBUtil dbUtil;
    private AnalyticsClient analyticsClient;
    private EventRecorder eventRecorder;
    private Application application;

    /**
     * beforeClass to init environment before all test case.
     */
    @BeforeClass
    public static void beforeClass() {
        //config and start server
        final HttpServer server = httpServer(8082);
        server.request(by(uri(COLLECT_SUCCESS))).response(status(200), text("success"));
        server.request(by(uri(COLLECT_SUCCESS1))).response(status(200), text("success"));
        server.request(and(by(uri(COLLECT_FOR_AUTH)), eq(header("cookie"), "testCookie")))
            .response(status(200), text("success"));
        server.request(by(uri(COLLECT_FAIL))).response(status(403), text("fail"));
        runner = runner(server);
        runner.start();
    }

    /**
     * setup Amplify get dbUtil and init handler background environment.
     *
     * @throws Exception exception
     */
    @Before
    public void setup() throws Exception {
        Boolean isConfigured = (Boolean) ReflectUtil.invokeSuperMethod(Amplify.Analytics, "isConfigured");
        if (!isConfigured) {
            Context context = ApplicationProvider.getApplicationContext();
            application = mock(Application.class);
            plugin = new AWSClickstreamPlugin(application);
            Amplify.addPlugin(plugin);
            Amplify.configure(context);
        } else {
            plugin = (AWSClickstreamPlugin) Amplify.Analytics.getPlugin("awsClickstreamPlugin");
            application = (Application) ReflectUtil.getFiled(plugin, "context");
        }
        analyticsClient = plugin.getEscapeHatch();
        assert analyticsClient != null;
        eventRecorder = (EventRecorder) ReflectUtil.getFiled(analyticsClient, "eventRecorder");
        dbUtil = (ClickstreamDBUtil) ReflectUtil.getFiled(eventRecorder, "dbUtil");
    }

    /**
     * test record event with name use ClickstreamAnalytics api and make sure
     * the event has be auto submitted success after handler post massage.
     *
     * @throws Exception exception
     */
    @Test
    public void testRecordEventWithName() throws Exception {
        executeBackground();
        ClickstreamAnalytics.recordEvent("testRecordEventWithName");
        assertEquals(1, dbUtil.getTotalNumber());
        Thread.sleep(2500);
        assertEquals(0, dbUtil.getTotalNumber());
    }

    /**
     * test record event with invalid name.
     *
     * @throws Exception exception
     */
    @Test
    public void testRecordEventWithInvalidName() throws Exception {
        executeBackground();
        ClickstreamAnalytics.recordEvent("01TestEvent");
        try (Cursor cursor = dbUtil.queryAllEvents()) {
            cursor.moveToFirst();
            String eventString = cursor.getString(2);
            JSONObject jsonObject = new JSONObject(eventString);
            assertEquals(Event.PresetEvent.CLICKSTREAM_ERROR, jsonObject.getString("event_type"));
        }
        assertEquals(1, dbUtil.getTotalNumber());
        Thread.sleep(1500);
        assertEquals(0, dbUtil.getTotalNumber());
    }

    /**
     * test record one AnalyticsEvent use ClickstreamAnalytics api and
     * make sure the event data is valid from db.
     *
     * @throws Exception exception
     */
    @Test
    public void testRecordOneEvent() throws Exception {
        executeBackground();
        ClickstreamEvent event =
            ClickstreamEvent.builder()
                .name("PasswordReset")
                .add("Channel", "SMS")
                .add("Successful", true)
                .add("ProcessDuration", 792)
                .add("Timestamp", 169823889238L)
                .add("UserAge", 120.3)
                .build();
        ClickstreamAnalytics.recordEvent(event);
        assertEquals(1, dbUtil.getTotalNumber());

        Cursor cursor = dbUtil.queryAllEvents();
        cursor.moveToFirst();
        String eventString = cursor.getString(2);
        JSONObject jsonObject = new JSONObject(eventString);
        JSONObject attribute = jsonObject.getJSONObject("attributes");
        Assert.assertEquals("PasswordReset", jsonObject.getString("event_type"));
        Assert.assertEquals("SMS", attribute.getString("Channel"));
        Assert.assertTrue(attribute.getBoolean("Successful"));
        Assert.assertEquals(792, attribute.getInt("ProcessDuration"));
        Assert.assertEquals(120.3, attribute.getDouble("UserAge"), 0.01);
        Assert.assertEquals(169823889238L, attribute.getLong("Timestamp"));

        Thread.sleep(2500);
        assertEquals(0, dbUtil.getTotalNumber());
        cursor.close();
    }

    /**
     * test add items.
     *
     * @throws Exception exception
     */
    @Test
    public void testAddItem() throws Exception {
        ClickstreamItem item1 = ClickstreamItem.builder()
            .add(ClickstreamAnalytics.Item.ITEM_ID, 123)
            .add(ClickstreamAnalytics.Item.ITEM_NAME, "Galaxy S10")
            .add(ClickstreamAnalytics.Item.ITEM_CATEGORY, "phone")
            .add(ClickstreamAnalytics.Item.ITEM_BRAND, "Sumsang")
            .add(ClickstreamAnalytics.Item.PRICE, 4999.9)
            .add(ClickstreamAnalytics.Item.QUANTITY, 25)
            .add("is_new", false)
            .build();

        ClickstreamItem item2 = ClickstreamItem.builder()
            .add(ClickstreamAnalytics.Item.ITEM_ID, 124)
            .add(ClickstreamAnalytics.Item.ITEM_NAME, "Galaxy S20")
            .add(ClickstreamAnalytics.Item.ITEM_CATEGORY, "phone")
            .add(ClickstreamAnalytics.Item.ITEM_BRAND, "Sumsang")
            .add(ClickstreamAnalytics.Item.PRICE, 5999.9)
            .add(ClickstreamAnalytics.Item.QUANTITY, 35)
            .add("is_new", true)
            .build();

        ClickstreamEvent event = ClickstreamEvent.builder()
            .name("testItem")
            .add(ClickstreamAnalytics.Item.ITEM_ID, 123)
            .setItems(new ClickstreamItem[] {item1, item2})
            .build();
        ClickstreamAnalytics.recordEvent(event);
        assertEquals(1, dbUtil.getTotalNumber());

        try (Cursor cursor = dbUtil.queryAllEvents()) {
            cursor.moveToFirst();
            String eventString = cursor.getString(2);
            JSONObject jsonObject = new JSONObject(eventString);
            JSONObject attribute = jsonObject.getJSONObject("attributes");
            JSONArray itemArray = jsonObject.getJSONArray("items");
            Assert.assertEquals(2, itemArray.length());
            JSONObject itemObject = (JSONObject) itemArray.get(0);

            Assert.assertEquals(123, attribute.getInt(ClickstreamAnalytics.Item.ITEM_ID));
            Assert.assertEquals("Galaxy S10", itemObject.getString(ClickstreamAnalytics.Item.ITEM_NAME));
            Assert.assertEquals(25, itemObject.getInt(ClickstreamAnalytics.Item.QUANTITY));
            Assert.assertEquals(4999.9, itemObject.getDouble(ClickstreamAnalytics.Item.PRICE), 0.01);
            Assert.assertFalse(itemObject.getBoolean("is_new"));
        }
    }

    /**
     * test add global attribute.
     *
     * @throws Exception exception
     */
    @Test
    public void testAddGlobalAttribute() throws Exception {
        long timestamp = System.currentTimeMillis();
        ClickstreamAttribute globalAttribute = ClickstreamAttribute.builder()
            .add("channel", "HUAWEI")
            .add("level", 5.1)
            .add("class", 6)
            .add("timestamp", timestamp)
            .add("isOpenNotification", true)
            .build();
        ClickstreamAnalytics.addGlobalAttributes(globalAttribute);
        ClickstreamEvent event = ClickstreamEvent.builder()
            .name("PasswordReset")
            .add("Message", "SMS")
            .add("Successful", true)
            .add("ProcessDuration", 792)
            .add("UserAge", 120.3)
            .build();
        ClickstreamAnalytics.recordEvent(event);
        assertEquals(1, dbUtil.getTotalNumber());
        Cursor cursor = dbUtil.queryAllEvents();
        cursor.moveToFirst();
        String eventString = cursor.getString(2);
        JSONObject jsonObject = new JSONObject(eventString);
        JSONObject attribute = jsonObject.getJSONObject("attributes");

        Assert.assertEquals("HUAWEI", attribute.getString("channel"));
        Assert.assertEquals(5.1, attribute.getDouble("level"), 0.01);
        Assert.assertEquals(6, attribute.getInt("class"));
        Assert.assertTrue(attribute.getBoolean("isOpenNotification"));
        Assert.assertEquals(timestamp, attribute.getLong("timestamp"));

        Assert.assertEquals("SMS", attribute.getString("Message"));
        Assert.assertTrue(attribute.getBoolean("Successful"));
        Assert.assertEquals(792, attribute.getInt("ProcessDuration"));
        Assert.assertEquals(120.3, attribute.getDouble("UserAge"), 0.01);

        ClickstreamAnalytics.flushEvents();
        Thread.sleep(1000);
        assertEquals(0, dbUtil.getTotalNumber());
        cursor.close();
    }


    /**
     * test add delete global attribute.
     *
     * @throws Exception exception
     */
    @Test
    public void testDeleteGlobalAttribute() throws Exception {
        ClickstreamAttribute globalAttribute = ClickstreamAttribute.builder()
            .add("channel", "HUAWEI")
            .add("level", 5.1)
            .add("class", 6)
            .add("isOpenNotification", true)
            .build();
        ClickstreamAnalytics.addGlobalAttributes(globalAttribute);
        ClickstreamEvent event = ClickstreamEvent.builder()
            .name("PasswordReset")
            .add("Message", "SMS")
            .add("Successful", true)
            .add("ProcessDuration", 792)
            .add("Number", 20.1)
            .build();
        ClickstreamAnalytics.deleteGlobalAttributes("level");
        ClickstreamAnalytics.recordEvent(event);
        assertEquals(1, dbUtil.getTotalNumber());
        Cursor cursor = dbUtil.queryAllEvents();
        cursor.moveToFirst();
        String eventString = cursor.getString(2);
        JSONObject jsonObject = new JSONObject(eventString);
        JSONObject attribute = jsonObject.getJSONObject("attributes");

        Assert.assertEquals("HUAWEI", attribute.getString("channel"));
        Assert.assertFalse(attribute.has("level"));
        Assert.assertTrue(attribute.getBoolean("isOpenNotification"));

        ClickstreamAnalytics.flushEvents();
        Thread.sleep(1000);
        assertEquals(0, dbUtil.getTotalNumber());
        cursor.close();
    }

    /**
     * test add user attribute.
     *
     * @throws Exception exception
     */
    @Test
    public void testAddUserAttributes() throws Exception {
        long timestamp = System.currentTimeMillis();
        ClickstreamAnalytics.setUserId("13212");
        ClickstreamUserAttribute clickstreamUserAttribute = ClickstreamUserAttribute.builder()
            .add("_user_age", 21)
            .add("isFirstOpen", true)
            .add("score", 85.5)
            .add("timestamp", timestamp)
            .add("_user_name", "carl")
            .build();
        ClickstreamAnalytics.addUserAttributes(clickstreamUserAttribute);
        assertEquals(2, dbUtil.getTotalNumber());

        Cursor cursor = dbUtil.queryAllEvents();
        cursor.moveToLast();
        String eventString = cursor.getString(2);
        JSONObject jsonObject = new JSONObject(eventString);
        String eventType = jsonObject.getString("event_type");
        Assert.assertEquals(Event.PresetEvent.PROFILE_SET, eventType);
        JSONObject user = jsonObject.getJSONObject("user");
        Assert.assertEquals("13212", ((JSONObject) user.get(Event.ReservedAttribute.USER_ID)).getString("value"));
        Assert.assertEquals(21, ((JSONObject) user.get("_user_age")).getInt("value"));
        Assert.assertTrue(((JSONObject) user.get("_user_age")).has("set_timestamp"));
        Assert.assertEquals("carl", ((JSONObject) user.get("_user_name")).getString("value"));
        Assert.assertTrue(((JSONObject) user.get("_user_name")).has("set_timestamp"));
        Assert.assertEquals(timestamp, ((JSONObject) user.get("timestamp")).getLong("value"));

        ClickstreamAnalytics.flushEvents();
        Thread.sleep(1000);
        assertEquals(0, dbUtil.getTotalNumber());
        cursor.close();
    }

    /**
     * test add user id.
     *
     * @throws Exception exception
     */
    @Test
    public void testModifyUserId() throws Exception {
        ClickstreamUserAttribute clickstreamUserAttribute = ClickstreamUserAttribute.builder()
            .add("_user_age", 21)
            .add("null", true)
            .add("score", 85.5)
            .add("_user_name", "carl")
            .build();
        ClickstreamAnalytics.setUserId("13212");
        ClickstreamAnalytics.addUserAttributes(clickstreamUserAttribute);
        ClickstreamAnalytics.setUserId("12345");
        assertEquals(3, dbUtil.getTotalNumber());

        Cursor cursor = dbUtil.queryAllEvents();
        cursor.moveToLast();
        String eventString = cursor.getString(2);
        JSONObject jsonObject = new JSONObject(eventString);
        JSONObject user = jsonObject.getJSONObject("user");
        Assert.assertEquals("12345", ((JSONObject) user.get(Event.ReservedAttribute.USER_ID)).getString("value"));
        Assert.assertFalse(user.has("_user_age"));
        Assert.assertFalse(user.has("_user_name"));

        ClickstreamAnalytics.flushEvents();
        Thread.sleep(1000);
        assertEquals(0, dbUtil.getTotalNumber());
        cursor.close();
    }

    /**
     * test set user id null.
     *
     * @throws Exception exception
     */
    @Test
    public void testSetUserIdNull() throws Exception {
        ClickstreamUserAttribute clickstreamUserAttribute = ClickstreamUserAttribute.builder()
            .add("_user_age", 21)
            .add("isFirstOpen", true)
            .add("score", 85.5)
            .add("_user_name", "carl")
            .build();
        ClickstreamAnalytics.setUserId("13212");
        ClickstreamAnalytics.addUserAttributes(clickstreamUserAttribute);
        ClickstreamEvent event = ClickstreamEvent.builder()
            .name("PasswordReset")
            .add("Message", "SMS")
            .add("Successful", true)
            .add("ProcessDuration", 792)
            .add("Number", 20.1)
            .build();
        ClickstreamAnalytics.setUserId(null);
        ClickstreamAnalytics.recordEvent(event);
        assertEquals(4, dbUtil.getTotalNumber());

        Cursor cursor = dbUtil.queryAllEvents();
        cursor.moveToLast();
        String eventString = cursor.getString(2);
        JSONObject jsonObject = new JSONObject(eventString);
        JSONObject user = jsonObject.getJSONObject("user");
        Assert.assertFalse(user.has(Event.ReservedAttribute.USER_ID));
        Assert.assertFalse(user.has("_user_age"));
        Assert.assertFalse(user.has("_user_name"));

        ClickstreamAnalytics.flushEvents();
        Thread.sleep(1000);
        assertEquals(0, dbUtil.getTotalNumber());
        cursor.close();
    }

    /**
     * test flush event.
     *
     * @throws Exception exception
     */
    @Test
    public void testFlushEvent() throws Exception {
        ClickstreamEvent event =
            ClickstreamEvent.builder()
                .name("PasswordReset")
                .add("Channel", "SMS")
                .add("Successful", true)
                .add("ProcessDuration", 792)
                .add("UserAge", 120.3)
                .build();
        ClickstreamAnalytics.recordEvent(event);
        assertEquals(1, dbUtil.getTotalNumber());
        ClickstreamAnalytics.flushEvents();
        Thread.sleep(1500);
        assertEquals(0, dbUtil.getTotalNumber());
    }

    /**
     * test record one event use ClickstreamAnalytics api and
     * make sure when event submit fail the event in db has not be deleted.
     *
     * @throws Exception exception
     */
    @Test
    public void testSubmitEventFail() throws Exception {
        executeBackground();
        setRequestPathToFail();
        ClickstreamAnalytics.recordEvent("testRecordEventWithName");
        assertEquals(1, dbUtil.getTotalNumber());
        Thread.sleep(1000);
        assertEquals(1, dbUtil.getTotalNumber());
    }

    /**
     * test record multi event which need to be flush twice, and
     * make sure each flush event has been auto executed successfully.
     *
     * @throws Exception exception
     */
    @Test
    public void testRecordEventWithSubmitterTwice() throws Exception {
        executeBackground();
        ClickstreamEvent.Builder builder = ClickstreamEvent.builder()
            .name("PasswordReset")
            .add("Channel", "SMS")
            .add("Successful", true)
            .add("ProcessDuration", 792)
            .add("UserAge", 120.3);
        String longString = analyticsClient.createEvent("testEvent").toString();
        for (int i = 0; i < 80; i++) {
            builder.add("str" + i, longString);
        }
        ClickstreamEvent event = builder.build();
        for (int i = 0; i < 20; i++) {
            ClickstreamAnalytics.recordEvent(event);
        }
        assertEquals(20, dbUtil.getTotalNumber());
        Thread.sleep(2000);
        assertEquals(0, dbUtil.getTotalNumber());
    }

    /**
     * test custom clickstream config in runtime.
     *
     * @throws Exception exception
     */
    @Test
    public void testCustomConfig() throws Exception {
        ClickstreamAnalytics.getClickStreamConfiguration()
            .withAppId("23982")
            .withEndpoint(assembleEndpointUrl(COLLECT_SUCCESS1))
            .withSendEventsInterval(15000)
            .withTrackAppExceptionEvents(false)
            .withLogEvents(true)
            .withCustomDns(CustomOkhttpDns.getInstance())
            .withCompressEvents(true);

        assertEquals("23982", this.analyticsClient.getClickstreamConfiguration().getAppId());
        assertEquals(assembleEndpointUrl(COLLECT_SUCCESS1),
            this.analyticsClient.getClickstreamConfiguration().getEndpoint());
        assertEquals(CustomOkhttpDns.getInstance(), this.analyticsClient.getClickstreamConfiguration().getDns());
        assertEquals(15000, this.analyticsClient.getClickstreamConfiguration().getSendEventsInterval());
        Assert.assertFalse(this.analyticsClient.getClickstreamConfiguration().isTrackAppExceptionEvents());
        Assert.assertTrue(this.analyticsClient.getClickstreamConfiguration().isCompressEvents());
        Assert.assertTrue(this.analyticsClient.getClickstreamConfiguration().isLogEvents());

        ClickstreamAnalytics.recordEvent("testRecordEvent");
        assertEquals(1, dbUtil.getTotalNumber());
        try (Cursor cursor = dbUtil.queryAllEvents()) {
            cursor.moveToFirst();
            String eventString = cursor.getString(2);
            JSONObject jsonObject = new JSONObject(eventString);
            String appId = jsonObject.getString("app_id");
            assertEquals("23982", appId);
        }

        ClickstreamAnalytics.flushEvents();
        Thread.sleep(1500);
        assertEquals(0, dbUtil.getTotalNumber());
    }

    /**
     * test custom dns for success.
     *
     * @throws Exception exception
     */
    @Test
    public void testCustomDnsSuccess() throws Exception {
        CustomOkhttpDns dns = CustomOkhttpDns.getInstance();
        dns.setDefaultIp("127.0.0.1");

        ClickstreamAnalytics.getClickStreamConfiguration().withCustomDns(dns);

        assertEquals(dns, this.analyticsClient.getClickstreamConfiguration().getDns());

        ClickstreamAnalytics.recordEvent("testRecordEvent");
        assertEquals(1, dbUtil.getTotalNumber());
        ClickstreamAnalytics.flushEvents();
        Thread.sleep(1500);
        assertEquals(0, dbUtil.getTotalNumber());
    }

    /**
     * test custom dns for fail.
     *
     * @throws Exception exception
     */
    @Test
    public void testCustomDnsFail() throws Exception {
        CustomOkhttpDns dns = CustomOkhttpDns.getInstance();
        dns.setDefaultIp("192.168.1.10");
        ClickstreamAnalytics.getClickStreamConfiguration().withCustomDns(dns);

        assertEquals(dns, this.analyticsClient.getClickstreamConfiguration().getDns());

        ClickstreamAnalytics.recordEvent("testRecordEvent");
        assertEquals(1, dbUtil.getTotalNumber());
        setHttpRequestTimeOut(1L);
        ClickstreamAnalytics.flushEvents();
        Thread.sleep(2000);
        assertEquals(1, dbUtil.getTotalNumber());

        dns.setDefaultIp("127.0.0.1");
        setHttpRequestTimeOut(15L);
        ClickstreamAnalytics.flushEvents();
        // wait for success
        Thread.sleep(1500);
        assertEquals(0, dbUtil.getTotalNumber());
    }

    /**
     * test custom dns for resolution timeout fail.
     *
     * @throws Exception exception
     */
    @Test
    public void testCustomDnsResolutionTimeoutFail() throws Exception {
        CustomOkhttpDns dns = CustomOkhttpDns.getInstance();
        dns.setDefaultIp("127.0.0.1");
        dns.setIsResolutionTimeout(true);
        setHttpRequestTimeOut(1L);

        ClickstreamAnalytics.getClickStreamConfiguration().withCustomDns(dns);
        assertEquals(dns, this.analyticsClient.getClickstreamConfiguration().getDns());

        ClickstreamAnalytics.recordEvent("testRecordEvent");
        assertEquals(1, dbUtil.getTotalNumber());
        ClickstreamAnalytics.flushEvents();
        Thread.sleep(1500);
        assertEquals(1, dbUtil.getTotalNumber());

        dns.setIsResolutionTimeout(false);
        setHttpRequestTimeOut(15L);
        ClickstreamAnalytics.flushEvents();
        Thread.sleep(1000);
        assertEquals(0, dbUtil.getTotalNumber());
    }

    /**
     * test custom dns for unKnow host fail.
     *
     * @throws Exception exception
     */
    @Test
    public void testCustomDnsForUnKnowHostFail() throws Exception {
        CustomOkhttpDns dns = CustomOkhttpDns.getInstance();
        dns.setDefaultIp("127.0.0.1");
        dns.setIsResolutionTimeout(false);
        dns.setIsUnKnowHost(true);
        ClickstreamAnalytics.getClickStreamConfiguration().withCustomDns(dns);
        assertEquals(dns, this.analyticsClient.getClickstreamConfiguration().getDns());

        ClickstreamAnalytics.recordEvent("testRecordEvent");
        assertEquals(1, dbUtil.getTotalNumber());
        ClickstreamAnalytics.flushEvents();
        Thread.sleep(1000);
        assertEquals(1, dbUtil.getTotalNumber());

        dns.setIsUnKnowHost(false);
        ClickstreamAnalytics.flushEvents();
        Thread.sleep(1000);
        assertEquals(0, dbUtil.getTotalNumber());
    }

    /**
     * test set auth cookie success.
     *
     * @throws Exception exception
     */
    @Test
    public void testSetAuthCookieSuccess() throws Exception {
        String authCookie = "testCookie";
        ClickstreamAnalytics.getClickStreamConfiguration()
            .withAuthCookie(authCookie)
            .withEndpoint(assembleEndpointUrl(COLLECT_FOR_AUTH));

        assertEquals(authCookie, this.analyticsClient.getClickstreamConfiguration().getAuthCookie());

        ClickstreamAnalytics.recordEvent("testRecordEventForAuth");
        assertEquals(1, dbUtil.getTotalNumber());
        ClickstreamAnalytics.flushEvents();
        Thread.sleep(1000);
        assertEquals(0, dbUtil.getTotalNumber());
    }

    /**
     * test set auth cookie fail.
     *
     * @throws Exception exception
     */
    @Test
    public void testSetAuthCookieFail() throws Exception {
        String authCookie = "testCookieFail";
        ClickstreamAnalytics.getClickStreamConfiguration()
            .withAuthCookie(authCookie)
            .withEndpoint(assembleEndpointUrl(COLLECT_FOR_AUTH));

        assertEquals(authCookie, this.analyticsClient.getClickstreamConfiguration().getAuthCookie());

        ClickstreamAnalytics.recordEvent("testRecordEventFailForAuth");
        assertEquals(1, dbUtil.getTotalNumber());
        ClickstreamAnalytics.flushEvents();
        Thread.sleep(1000);
        assertEquals(1, dbUtil.getTotalNumber());

        ClickstreamAnalytics.getClickStreamConfiguration().withAuthCookie("testCookie");
        ClickstreamAnalytics.flushEvents();
        Thread.sleep(1000);
        assertEquals(0, dbUtil.getTotalNumber());
    }

    /**
     * test enable sdk not in main thread.
     *
     * @throws Exception exception
     */
    @Test
    public void testEnableAndDisableSDKNotInMainThread() throws Exception {
        Log log = mock(Log.class);
        ReflectUtil.modifyFiled(plugin, "LOG", log);
        new Thread(() -> {
            ClickstreamAnalytics.disable();
            ClickstreamAnalytics.enable();
        }).start();
        Thread.sleep(500);
        verify(log, times(0)).debug("Clickstream SDK enabled");
        verify(log, times(0)).debug("Clickstream SDK disabled");
    }

    /**
     * test enable SDK twice.
     *
     * @throws Exception exception
     */
    @Test
    public void testEnableSDKTwice() throws Exception {
        AutoEventSubmitter submitter = (AutoEventSubmitter) ReflectUtil.getFiled(plugin, "autoEventSubmitter");
        Log log = mock(Log.class);
        ReflectUtil.modifyFiled(submitter, "LOG", log);
        ClickstreamAnalytics.enable();
        ClickstreamAnalytics.enable();
        verify(log, times(0)).debug("Auto submitting start");
        ClickstreamAnalytics.recordEvent("testRecordEventWithName");
        assertEquals(1, dbUtil.getTotalNumber());
    }

    /**
     * test enable after disable.
     *
     * @throws Exception exception
     */
    @Test
    public void testEnableAfterDisable() throws Exception {
        AutoEventSubmitter submitter = (AutoEventSubmitter) ReflectUtil.getFiled(plugin, "autoEventSubmitter");
        Log log = mock(Log.class);
        ReflectUtil.modifyFiled(submitter, "LOG", log);

        ClickstreamAnalytics.disable();
        verify(log).debug("Auto submitting stop");
        assertEquals(0, dbUtil.getTotalNumber());
        ClickstreamAnalytics.recordEvent("testRecordEventWithName");
        assertEquals(0, dbUtil.getTotalNumber());
        ClickstreamAnalytics.enable();
        verify(log).debug("Auto submitting start");
        ClickstreamAnalytics.recordEvent("testRecordEventWithName");
        assertEquals(1, dbUtil.getTotalNumber());
    }

    /**
     * test disable track activity lifecycle.
     *
     * @throws Exception exception
     */
    @Test
    public void testDisableActivityLifecycle() throws Exception {
        ActivityLifecycleManager lifecycleManager =
            (ActivityLifecycleManager) ReflectUtil.getFiled(plugin, "activityLifecycleManager");

        ClickstreamAnalytics.disable();

        verify(application).unregisterActivityLifecycleCallbacks(lifecycleManager);
        ClickstreamAnalytics.enable();
    }

    /**
     * test disable sdk and will not record app lifecycle events.
     *
     * @throws Exception exception
     */
    @Test
    public void testDisableAppLifecycle() throws Exception {
        ActivityLifecycleManager lifecycleManager =
            (ActivityLifecycleManager) ReflectUtil.getFiled(plugin, "activityLifecycleManager");
        LifecycleRegistry lifecycle = new LifecycleRegistry(mock(LifecycleOwner.class));
        lifecycleManager.startLifecycleTracking(application, lifecycle);

        ClickstreamAnalytics.disable();
        lifecycleManager.stopLifecycleTracking(application, lifecycle);

        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);
        assertEquals(0, dbUtil.getTotalNumber());
        ClickstreamAnalytics.enable();
    }

    /**
     * common method to set request path.
     *
     * @throws Exception exception
     */
    private void setRequestPathToFail() throws Exception {
        ClickstreamContext context = (ClickstreamContext) ReflectUtil.getFiled(eventRecorder, "clickstreamContext");
        ClickstreamConfiguration config =
            (ClickstreamConfiguration) ReflectUtil.getFiled(context, "clickstreamConfiguration");
        ReflectUtil.modifyFiled(config, "endpoint", assembleEndpointUrl(COLLECT_FAIL));
    }

    private String assembleEndpointUrl(String path) {
        return COLLECT_HOST + path;
    }

    /**
     * mock handler.postDelayed() in 1s.
     *
     * @param handler handler
     */
    private void mockHandler(Handler handler) {
        when(handler.postDelayed(any(Runnable.class), anyLong())).thenAnswer(invocation -> {
            Thread.sleep(200);
            invocation.getArgument(0, Runnable.class).run();
            return null;
        });
    }

    /**
     * make sure the handler executed not in main thread.
     */
    private void executeBackground() {
        new Thread(() -> {
            handler = mock(Handler.class);
            mockHandler(handler);
            AutoEventSubmitter submitter = null;
            try {
                submitter = (AutoEventSubmitter) ReflectUtil.getFiled(plugin, "autoEventSubmitter");
                ReflectUtil.modifyFiled(submitter, "handler", handler);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            assert submitter != null;
            submitter.start();
        }).start();
    }

    /**
     * when each case execute finish we should stop the handler timer.
     *
     * @throws Exception exception
     */
    private void stopThreadSafely() throws Exception {
        AutoEventSubmitter submitter = (AutoEventSubmitter) ReflectUtil.getFiled(plugin, "autoEventSubmitter");
        ReflectUtil.modifyFiled(submitter, "handler", mock(Handler.class));
    }

    private void setHttpRequestTimeOut(long timeOutSecond) throws Exception {
        ReflectUtil.modifyFiled(ClickstreamAnalytics.getClickStreamConfiguration(), "callTimeOut", timeOutSecond);
    }

    /**
     * close db and stop handler executed thread.
     *
     * @throws Exception exception
     */
    @After
    public void tearDown() throws Exception {
        dbUtil.deleteBatchEvents(1000);
        dbUtil.closeDB();
        stopThreadSafely();
        ClickstreamAnalytics.getClickStreamConfiguration().withCustomDns(null);
        Map<String, Object> globalAttribute =
            (Map<String, Object>) ReflectUtil.getFiled(analyticsClient, "globalAttributes");
        ReflectUtil.modifyFiled(analyticsClient, "simpleUserAttributes", new JSONObject());
        ReflectUtil.modifyFiled(analyticsClient, "allUserAttributes", new JSONObject());
        globalAttribute.clear();
    }

    /**
     * after class stop runner.
     */
    @AfterClass
    public static void afterClass() {
        runner.stop();
    }

}
