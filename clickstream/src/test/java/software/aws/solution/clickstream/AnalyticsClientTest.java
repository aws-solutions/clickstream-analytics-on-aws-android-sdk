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
import androidx.test.core.app.ApplicationProvider;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import software.aws.solution.clickstream.client.AnalyticsClient;
import software.aws.solution.clickstream.client.AnalyticsEvent;
import software.aws.solution.clickstream.client.ClickstreamManager;
import software.aws.solution.clickstream.client.Event;
import software.aws.solution.clickstream.client.Event.ErrorCode;
import software.aws.solution.clickstream.util.ReflectUtil;

import java.util.Map;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class AnalyticsClientTest {

    private AnalyticsClient analyticsClient;
    private Map<String, Object> globalAttributes;
    private JSONObject allUserAttributes;
    private String exceedLengthName = "abcdefghijabcdefghijabcdefghijabcdefghijabcdefghij";
    private final String invalidName = "1_goods_expose";
    private String exceedLengthValue = "";

    private AnalyticsClient mockAnalyticsClient;
    private ArgumentCaptor<AnalyticsEvent> analyticsEventCaptor;

    private String errorCodeKey = Event.ReservedAttribute.ERROR_CODE;
    private String errorMessageKey = Event.ReservedAttribute.ERROR_MESSAGE;

    /**
     * prepare eventRecorder and context.
     *
     * @throws Exception exception.
     */
    @Before
    public void setup() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        ClickstreamConfiguration configuration = ClickstreamConfiguration.getDefaultConfiguration()
            .withAppId("demo-app")
            .withEndpoint("http://example.com/collect")
            .withSendEventsInterval(10000);
        ClickstreamManager clickstreamManager = new ClickstreamManager(context, configuration);
        analyticsClient = clickstreamManager.getAnalyticsClient();

        globalAttributes = (Map<String, Object>) ReflectUtil.getField(analyticsClient, "globalAttributes");
        allUserAttributes = (JSONObject) ReflectUtil.getField(analyticsClient, "allUserAttributes");

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 21; i++) {
            sb.append(exceedLengthName);
        }
        exceedLengthValue = sb.toString();
        exceedLengthName = exceedLengthName + "1";
        mockAnalyticsClient = Mockito.spy(analyticsClient);
        analyticsEventCaptor = ArgumentCaptor.forClass(AnalyticsEvent.class);
    }

    /**
     * test create event when event name exceed the max length.
     *
     * @throws JSONException the json exception
     */
    @Test
    public void testCreateEventWithNameLengthError() throws JSONException {
        AnalyticsEvent event = mockAnalyticsClient.createEvent(exceedLengthName);
        Assert.assertNull(event);

        verify(mockAnalyticsClient).recordEvent(analyticsEventCaptor.capture());
        AnalyticsEvent errorEvent = analyticsEventCaptor.getValue();
        Assert.assertEquals(Event.PresetEvent.CLICKSTREAM_ERROR, errorEvent.getEventType());
        Assert.assertEquals(ErrorCode.EVENT_NAME_LENGTH_EXCEED,
            errorEvent.getAttributes().get(errorCodeKey));
    }

    /**
     * test create event when event name was invalid.
     *
     * @throws JSONException the json exception
     */
    @Test
    public void testCreateEventWithInvalidNameError() throws JSONException {
        AnalyticsEvent event = mockAnalyticsClient.createEvent(invalidName);
        Assert.assertNull(event);

        verify(mockAnalyticsClient).recordEvent(analyticsEventCaptor.capture());
        AnalyticsEvent errorEvent = analyticsEventCaptor.getValue();
        Assert.assertEquals(Event.PresetEvent.CLICKSTREAM_ERROR, errorEvent.getEventType());
        Assert.assertEquals(ErrorCode.EVENT_NAME_INVALID,
            errorEvent.getAttributes().get(errorCodeKey));
    }


    /**
     * test add global attribute when success.
     */
    @Test
    public void testAddGlobalAttributeWhenSuccess() {
        analyticsClient.addGlobalAttribute("channel", "HUAWEI");
        Assert.assertTrue(globalAttributes.containsKey("channel"));
        Assert.assertEquals("HUAWEI", Objects.requireNonNull(globalAttributes.get("channel")));
    }

    /**
     * test add global attribute when error.
     *
     * @throws JSONException the json exception
     */
    @Test
    public void testAddGlobalAttributeWhenError() throws JSONException {
        mockAnalyticsClient.addGlobalAttribute(exceedLengthName, "value");
        verify(mockAnalyticsClient).recordEvent(analyticsEventCaptor.capture());
        AnalyticsEvent errorEvent = analyticsEventCaptor.getValue();
        Assert.assertEquals(Event.PresetEvent.CLICKSTREAM_ERROR, errorEvent.getEventType());
        Assert.assertEquals(ErrorCode.ATTRIBUTE_NAME_LENGTH_EXCEED,
            errorEvent.getAttributes().get(errorCodeKey));

        Mockito.reset(mockAnalyticsClient);
        mockAnalyticsClient.addGlobalAttribute(invalidName, "value");
        verify(mockAnalyticsClient).recordEvent(analyticsEventCaptor.capture());
        AnalyticsEvent errorEvent1 = analyticsEventCaptor.getValue();
        Assert.assertEquals(Event.PresetEvent.CLICKSTREAM_ERROR, errorEvent1.getEventType());
        Assert.assertEquals(ErrorCode.ATTRIBUTE_NAME_INVALID,
            errorEvent1.getAttributes().get(errorCodeKey));

        Mockito.reset(mockAnalyticsClient);
        mockAnalyticsClient.addGlobalAttribute("name01", exceedLengthValue);
        verify(mockAnalyticsClient).recordEvent(analyticsEventCaptor.capture());
        AnalyticsEvent errorEvent2 = analyticsEventCaptor.getValue();
        Assert.assertEquals(Event.PresetEvent.CLICKSTREAM_ERROR, errorEvent2.getEventType());
        Assert.assertEquals(ErrorCode.ATTRIBUTE_VALUE_LENGTH_EXCEED,
            errorEvent2.getAttributes().get(errorCodeKey));
    }

    /**
     * test delete global attribute.
     */
    @Test
    public void deleteGlobalAttribute() {
        analyticsClient.addGlobalAttribute("name01", "value");
        analyticsClient.addGlobalAttribute("name02", "value1");
        analyticsClient.deleteGlobalAttribute("name01");
        Assert.assertTrue(globalAttributes.containsKey("name02"));
        Assert.assertFalse(globalAttributes.containsKey("name01"));
    }

    /**
     * test add global attribute for same name multi times, the value of the global attribute ame will
     * covered by the last value.
     */
    @Test
    public void testAddGlobalAttributeSameNameMultiTimes() {
        for (int i = 0; i < 501; i++) {
            mockAnalyticsClient.addGlobalAttribute("name", "value" + i);
        }
        Assert.assertFalse(globalAttributes.containsKey("_error_attribute_size_exceed"));
        verify(mockAnalyticsClient, never()).createEvent(anyString());
        Assert.assertEquals(1, globalAttributes.size());
        Assert.assertEquals("value500", Objects.requireNonNull(globalAttributes.get("name")).toString());
    }

    /**
     * test add user attribute when success.
     *
     * @throws JSONException exception
     */
    @Test
    public void testAddUserAttributeWhenSuccess() throws JSONException {
        analyticsClient.addUserAttribute("_user_age", 18);
        Assert.assertTrue(allUserAttributes.has("_user_age"));
        Assert.assertEquals(18, ((JSONObject) allUserAttributes.get("_user_age")).get("value"));
        Assert.assertTrue(System.currentTimeMillis() -
            (Long) (((JSONObject) allUserAttributes.get("_user_age")).get("set_timestamp")) < 1000);
    }

    /**
     * test add user attribute when error.
     *
     * @throws JSONException the json exception
     */
    @Test
    public void testAddUserAttributeWhenError() throws JSONException {
        mockAnalyticsClient.addUserAttribute(exceedLengthName, "value");
        verify(mockAnalyticsClient).recordEvent(analyticsEventCaptor.capture());
        AnalyticsEvent event = analyticsEventCaptor.getValue();
        Assert.assertEquals(Event.PresetEvent.CLICKSTREAM_ERROR, event.getEventType());
        Assert.assertEquals(ErrorCode.USER_ATTRIBUTE_NAME_LENGTH_EXCEED,
            event.getAttributes().get(errorCodeKey));

        Mockito.reset(mockAnalyticsClient);
        mockAnalyticsClient.addUserAttribute(invalidName, "value");
        verify(mockAnalyticsClient).recordEvent(analyticsEventCaptor.capture());
        AnalyticsEvent event1 = analyticsEventCaptor.getValue();
        Assert.assertEquals(Event.PresetEvent.CLICKSTREAM_ERROR, event1.getEventType());
        Assert.assertEquals(ErrorCode.USER_ATTRIBUTE_NAME_INVALID,
            event1.getAttributes().get(errorCodeKey));

        Mockito.reset(mockAnalyticsClient);
        mockAnalyticsClient.addUserAttribute("name01", exceedLengthValue);
        verify(mockAnalyticsClient).recordEvent(analyticsEventCaptor.capture());
        AnalyticsEvent event2 = analyticsEventCaptor.getValue();
        Assert.assertEquals(Event.PresetEvent.CLICKSTREAM_ERROR, event2.getEventType());
        Assert.assertEquals(ErrorCode.USER_ATTRIBUTE_VALUE_LENGTH_EXCEED,
            event2.getAttributes().get(errorCodeKey));
    }

    /**
     * test add user attribute when exceed max number limit.
     *
     * @throws JSONException the json exception
     */
    @Test
    public void testAddUserAttributeWhenExceedNumberLimit() throws JSONException {
        for (int i = 0; i < 100; i++) {
            mockAnalyticsClient.addUserAttribute("name" + i, "value" + i);
        }
        verify(mockAnalyticsClient).recordEvent(analyticsEventCaptor.capture());
        AnalyticsEvent event = analyticsEventCaptor.getValue();
        Assert.assertEquals(ErrorCode.USER_ATTRIBUTE_SIZE_EXCEED, event.getAttributes().get(errorCodeKey));
        Assert.assertEquals("attribute name: name99", event.getAttributes().get(errorMessageKey));
    }

    /**
     * test add user attribute for same name multi times, the value of the user attribute ame will
     * covered by the last value.
     *
     * @throws JSONException exception
     */
    @Test
    public void testAddUserAttributeSameNameMultiTimes() throws JSONException {
        for (int i = 0; i < 101; i++) {
            mockAnalyticsClient.addUserAttribute("name", "value" + i);
        }
        verify(mockAnalyticsClient, never()).createEvent(anyString());
        Assert.assertEquals(2, allUserAttributes.length());
        Assert.assertEquals("value100", ((JSONObject) allUserAttributes.get("name")).get("value"));
    }

    /**
     * test add global attribute for null value and verify the global attribute is deleted.
     */
    @Test
    public void testAddGlobalAttributeForNullValue() {
        analyticsClient.addGlobalAttribute("Channel", "HUAWEI");
        Assert.assertTrue(globalAttributes.containsKey("Channel"));
        analyticsClient.addGlobalAttribute("Channel", null);
        Assert.assertFalse(globalAttributes.containsKey("Channel"));
    }

    /**
     * test add user attribute for null value and verify the user attribute is deleted.
     */
    @Test
    public void testAddUserAttributeForNullValue() {
        analyticsClient.addUserAttribute("UserAge", 20);
        Assert.assertTrue(allUserAttributes.has("UserAge"));
        analyticsClient.addUserAttribute("UserAge", null);
        Assert.assertFalse(allUserAttributes.has("UserAge"));
    }

    /**
     * test add global attribute for null value when reached max length,
     * and verify the global attribute is deleted.
     */
    @Test
    public void testAddGlobalAttributeForNullValueWhenReachedMaxLength() {
        for (int i = 0; i < 500; i++) {
            analyticsClient.addGlobalAttribute("name" + i, "value" + i);
        }
        Assert.assertTrue(globalAttributes.containsKey("name0"));
        analyticsClient.addGlobalAttribute("name0", null);
        Assert.assertFalse(globalAttributes.containsKey("name0"));
        Assert.assertEquals(499, globalAttributes.size());
    }

    /**
     * test add user attribute for null value when reached max length,
     * and verify the user attribute is deleted.
     */
    @Test
    public void testAddUserAttributeForNullValueWhenReachedMaxLength() {
        for (int i = 0; i < 100; i++) {
            analyticsClient.addUserAttribute("name" + i, "value" + i);
        }
        Assert.assertTrue(allUserAttributes.has("name0"));
        analyticsClient.addUserAttribute("name0", null);
        Assert.assertFalse(allUserAttributes.has("name0"));
        Assert.assertEquals(99, allUserAttributes.length());
    }

    /**
     * test delete and non-existing global attribute.
     */
    @Test
    public void deleteNonExistingGlobalAttribute() {
        for (int i = 0; i < 500; i++) {
            analyticsClient.addGlobalAttribute("name" + i, "value" + i);
        }
        analyticsClient.addGlobalAttribute("name1000", null);
        Assert.assertEquals(500, globalAttributes.size());
    }

    /**
     * test delete and non-existing user attribute.
     */
    @Test
    public void deleteNonExistingUserAttribute() {
        for (int i = 0; i < 100; i++) {
            analyticsClient.addUserAttribute("name" + i, "value" + i);
        }
        analyticsClient.addUserAttribute("name1000", null);
        Assert.assertEquals(100, allUserAttributes.length());
    }

    /**
     * test get user attribute from storage when next sdk init.
     *
     * @throws Exception exception
     */
    @Test
    public void testUserAttributeForStorage() throws Exception {
        analyticsClient.addUserAttribute("user_name", "carl");
        analyticsClient.addUserAttribute("_user_id", "10837409");
        analyticsClient.addUserAttribute("user_age", 21);
        analyticsClient.addUserAttribute("isNew", true);
        analyticsClient.addUserAttribute("score", 85.5);
        analyticsClient.updateUserAttribute();
        Context context = ApplicationProvider.getApplicationContext();

        ClickstreamManager clickstreamManager = new ClickstreamManager(context,
            ClickstreamConfiguration.getDefaultConfiguration()
                .withAppId("demo-app")
                .withEndpoint("http://example.com/collect")
        );
        JSONObject userAttributesFromStorage =
            (JSONObject) ReflectUtil.getField(clickstreamManager.getAnalyticsClient(), "allUserAttributes");
        Assert.assertEquals(6, userAttributesFromStorage.length());
        Assert.assertEquals("carl", ((JSONObject) userAttributesFromStorage.get("user_name")).getString("value"));
        Assert.assertEquals("10837409", ((JSONObject) userAttributesFromStorage.get("_user_id")).getString("value"));
        Assert.assertEquals(21, ((JSONObject) userAttributesFromStorage.get("user_age")).getInt("value"));
        Assert.assertTrue(((JSONObject) userAttributesFromStorage.get("isNew")).getBoolean("value"));
        Assert.assertEquals(85.5, ((JSONObject) userAttributesFromStorage.get("score")).getDouble("value"), 0.01);
        long userFirstTouchTimestamp =
            ((JSONObject) userAttributesFromStorage.get(Event.ReservedAttribute.USER_FIRST_TOUCH_TIMESTAMP)).getLong(
                "value");
        Assert.assertTrue(userFirstTouchTimestamp - System.currentTimeMillis() < 500);
    }

    /**
     * test initial value in AnalyticsClient.
     *
     * @throws Exception exception
     */
    @Test
    public void testInitialValueInAnalyticsClient() throws Exception {
        String userId = (String) ReflectUtil.getField(analyticsClient, "userId");
        String userUniqueId = (String) ReflectUtil.getField(analyticsClient, "userUniqueId");
        Assert.assertEquals("", userId);
        Assert.assertNotNull(userUniqueId);

        allUserAttributes = (JSONObject) ReflectUtil.getField(analyticsClient, "allUserAttributes");
        Assert.assertTrue(allUserAttributes.has(Event.ReservedAttribute.USER_FIRST_TOUCH_TIMESTAMP));
    }

    /**
     * test update same userId twice.
     *
     * @throws Exception exception
     */
    @Test
    public void testUpdateSameUserIdTwice() throws Exception {
        String userIdForA = "aaa";
        String userUniqueId = (String) ReflectUtil.getField(analyticsClient, "userUniqueId");
        analyticsClient.updateUserId(userIdForA);
        analyticsClient.addUserAttribute("user_age", 12);
        analyticsClient.updateUserId(userIdForA);
        allUserAttributes = (JSONObject) ReflectUtil.getField(analyticsClient, "allUserAttributes");
        Assert.assertTrue(allUserAttributes.has("user_age"));
        Assert.assertEquals(userUniqueId, ReflectUtil.getField(analyticsClient, "userUniqueId"));
    }

    /**
     * test update different userId.
     *
     * @throws Exception exception
     */
    @Test
    public void testUpdateDifferentUserId() throws Exception {
        String userIdForA = "aaa";
        String userIdForB = "bbb";
        String userUniqueId = (String) ReflectUtil.getField(analyticsClient, "userUniqueId");
        analyticsClient.updateUserId(userIdForA);
        analyticsClient.addUserAttribute("user_age", 12);
        analyticsClient.updateUserId(userIdForB);
        allUserAttributes = (JSONObject) ReflectUtil.getField(analyticsClient, "allUserAttributes");
        Assert.assertFalse(allUserAttributes.has("user_age"));
        Assert.assertNotEquals(userUniqueId, ReflectUtil.getField(analyticsClient, "userUniqueId"));
    }

    /**
     * test change to origin user.
     *
     * @throws Exception exception
     */
    @Test
    public void testChangeToOriginUserId() throws Exception {
        String userIdForA = "aaa";
        String userIdForB = "bbb";
        String userUniqueId = (String) ReflectUtil.getField(analyticsClient, "userUniqueId");
        analyticsClient.updateUserId(userIdForA);
        analyticsClient.updateUserId(userIdForB);
        String userUniqueIdForB = (String) ReflectUtil.getField(analyticsClient, "userUniqueId");
        analyticsClient.updateUserId(userIdForA);
        Assert.assertEquals(userUniqueId, ReflectUtil.getField(analyticsClient, "userUniqueId"));
        analyticsClient.updateUserId(userIdForB);
        Assert.assertEquals(userUniqueIdForB, ReflectUtil.getField(analyticsClient, "userUniqueId"));
    }


    /**
     * test create event without custom user attributes.
     *
     * @throws JSONException the json exception
     */
    @Test
    public void testCreateEventWithoutCustomUserAttributes() throws JSONException {
        analyticsClient.updateUserId("123");
        analyticsClient.addUserAttribute("userName", "carl");
        analyticsClient.addUserAttribute("userAge", 22);

        AnalyticsEvent testEvent = analyticsClient.createEvent("testEvent");
        JSONObject user = testEvent.toJSONObject().getJSONObject("user");
        Assert.assertTrue(user.has(Event.ReservedAttribute.USER_ID));
        Assert.assertTrue(user.has(Event.ReservedAttribute.USER_FIRST_TOUCH_TIMESTAMP));
        Assert.assertFalse(user.has("userName"));
        Assert.assertFalse(user.has("userAge"));
        JSONObject userIdObject = user.getJSONObject(Event.ReservedAttribute.USER_ID);
        Assert.assertEquals("123", userIdObject.getString("value"));
    }

    /**
     * tearDown.
     */
    @After
    public void tearDown() {
    }

}
