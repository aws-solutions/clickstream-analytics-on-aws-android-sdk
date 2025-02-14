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
import android.database.Cursor;
import android.net.Uri;
import androidx.test.core.app.ApplicationProvider;

import com.amazonaws.logging.Log;
import com.amazonaws.logging.LogFactory;
import com.github.dreamhead.moco.HttpServer;
import com.github.dreamhead.moco.Runner;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import software.aws.solution.clickstream.client.AnalyticsClient;
import software.aws.solution.clickstream.client.AnalyticsEvent;
import software.aws.solution.clickstream.client.ClickstreamContext;
import software.aws.solution.clickstream.client.ClickstreamManager;
import software.aws.solution.clickstream.client.Event;
import software.aws.solution.clickstream.client.EventRecorder;
import software.aws.solution.clickstream.client.db.ClickstreamDBUtil;
import software.aws.solution.clickstream.client.network.NetRequest;
import software.aws.solution.clickstream.client.util.StringUtil;
import software.aws.solution.clickstream.util.ReflectUtil;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.github.dreamhead.moco.Moco.and;
import static com.github.dreamhead.moco.Moco.by;
import static com.github.dreamhead.moco.Moco.eq;
import static com.github.dreamhead.moco.Moco.exist;
import static com.github.dreamhead.moco.Moco.httpServer;
import static com.github.dreamhead.moco.Moco.query;
import static com.github.dreamhead.moco.Moco.status;
import static com.github.dreamhead.moco.Moco.text;
import static com.github.dreamhead.moco.Moco.uri;
import static com.github.dreamhead.moco.Runner.runner;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class EventRecorderTest {
    private static final String COLLECT_SUCCESS = "/collect/success";
    private static final String COLLECT_FAIL = "/collect/fail";
    private static final String COLLECT_FOR_VERIFY_HASH_CODE = "/collect/hashcode";
    private static final String COLLECT_FOR_VERIFY_UPLOAD_TIMESTAMP = "/collect/timestamp";
    private static Runner runner;
    private static String jsonString;
    private static HttpServer server;
    private ClickstreamDBUtil dbUtil;
    private EventRecorder eventRecorder;
    private ClickstreamContext clickstreamContext;
    private AnalyticsEvent event;
    private ExecutorService executorService;
    private Log log;

    /**
     * beforeClass to init environment before all test case.
     */
    @BeforeClass
    public static void beforeClass() {
        //config and start server
        server = httpServer(8082);
        server.request(by(uri(COLLECT_SUCCESS))).response(status(200), text("success"));
        server.request(by(uri(COLLECT_FAIL))).response(status(403), text("fail"));
        runner = runner(server);
        runner.start();
        StringBuilder sb = new StringBuilder();
        String str = "abcdeabcde";
        for (int i = 0; i < 100; i++) {
            sb.append(str);
        }
        jsonString = sb.toString();
    }

    /**
     * prepare eventRecorder and context.
     *
     * @throws Exception exception.
     */
    @Before
    public void setup() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        dbUtil = new ClickstreamDBUtil(context);

        ClickstreamConfiguration configuration = ClickstreamConfiguration.getDefaultConfiguration()
            .withAppId("demo-app")
            .withEndpoint("http://example.com/collect")
            .withSendEventsInterval(10000);
        ClickstreamManager clickstreamManager = new ClickstreamManager(context, configuration);
        AnalyticsClient analyticsClient = clickstreamManager.getAnalyticsClient();
        event = analyticsClient.createEvent("testEvent");
        clickstreamContext = clickstreamManager.getClickstreamContext();

        executorService = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(1000),
            new ThreadPoolExecutor.DiscardPolicy());
        // Using reflection to get the method
        eventRecorder =
            (EventRecorder) ReflectUtil.newInstance(EventRecorder.class, clickstreamContext, dbUtil, executorService);

        log = LogFactory.getLog(EventRecorder.class);
        log.setLevel(LogFactory.Level.DEBUG);

        assertEquals(3, dbUtil.getTotalNumber());
        dbUtil.deleteBatchEvents(3);
    }

    /**
     * test insert single event.
     */
    @Test
    public void testRecordEvent() {
        final Uri uri = eventRecorder.recordEvent(event);
        final int idInserted = Integer.parseInt(uri.getLastPathSegment());
        assertNotEquals(0, idInserted);
        final Cursor c = dbUtil.queryAllEvents();
        assertNotNull(c);
        assertEquals(1, c.getCount());
        c.close();
    }

    /**
     * test record event reached max db size.
     *
     * @throws Exception exception.
     */
    @Test
    public void testRecordEventForReachedMaxDbSize() throws Exception {
        setRequestPath(COLLECT_SUCCESS);
        for (int i = 0; i < 200; i++) {
            event.addAttribute("test_json_" + i, jsonString);
        }
        for (int i = 0; i < 260; i++) {
            eventRecorder.recordEvent(event);
        }
        assertTrue(dbUtil.getTotalSize() < 50 * 1024 * 1024L);
        assertEquals(256, dbUtil.getTotalNumber());
    }

    /**
     * test insert single event when exceed attribute number limit.
     *
     * @throws JSONException the json exception
     */
    @Test
    public void testRecordEventExceedAttributeNumberLimit() throws JSONException {
        for (int i = 0; i < 501; i++) {
            event.addAttribute("name" + i, "value" + i);
        }
        assertEquals("value499", event.getStringAttribute("name499"));
        assertFalse(event.hasAttribute("name500"));
        assertEquals(Event.ErrorCode.ATTRIBUTE_SIZE_EXCEED,
            event.getAttributes().get(Event.ReservedAttribute.ERROR_CODE));
        assertEquals("attribute name: name500", event.getStringAttribute(Event.ReservedAttribute.ERROR_MESSAGE));
    }

    /**
     * test insert single event when add same attribute name multi times, the value of the attribute name
     * will covered by the last value.
     */
    @Test
    public void testRecordEventAddSameAttributeMultiTimes() {
        for (int i = 0; i < 501; i++) {
            event.addAttribute("name", "value" + i);
        }
        assertEquals("value500", event.getStringAttribute("name"));
        Assert.assertEquals(1, event.getCurrentNumOfAttributes());
        assertFalse(event.hasAttribute(Event.ReservedAttribute.ERROR_CODE));
    }

    /**
     * test getBatchOfEvents for one event.
     *
     * @throws Exception exception.
     */
    @Test
    public void testGetBatchOfEventsForOneEvent() throws Exception {
        eventRecorder.recordEvent(event);
        Cursor cursor = dbUtil.queryAllEvents();
        String[] result = getBatchOfEvents(cursor);
        assertEquals(result.length, 2);
        assertTrue(result[0].contains(event.getEventId()));
        assertEquals("4", result[1]);
        cursor.close();
    }

    /**
     * test getBatchOfEvents for one event reached limit size.
     *
     * @throws Exception exception.
     */
    @Test
    public void testGetBatchOfEventsForOneEventReachedLimitSize() throws Exception {
        String str = "abcdefghij";
        StringBuilder nameBuilder = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            nameBuilder.append(str);
        }
        String name = nameBuilder.toString();
        for (int i = 0; i < 500; i++) {
            event.addAttribute(name + i, jsonString + i);
        }
        eventRecorder.recordEvent(event);
        Assert.assertTrue(event.toString().length() > 512 * 1024);
        Cursor cursor = dbUtil.queryAllEvents();
        String[] result = getBatchOfEvents(cursor);
        assertEquals(result.length, 2);
        assertNotNull(result[0]);
        assertTrue(result[0].length() > 512 * 1024);
        assertEquals("4", result[1]);
        cursor.close();
    }

    /**
     * test getBatchOfEvents when not reached default limited size.
     *
     * @throws Exception exception.
     */
    @Test
    public void testGetBatchOfEventsNotReachedDefaultLimitSize() throws Exception {
        for (int i = 0; i < 20; i++) {
            eventRecorder.recordEvent(event);
        }
        Cursor cursor = dbUtil.queryAllEvents();
        String[] result = getBatchOfEvents(cursor);
        assertEquals(2, result.length);
        assertEquals(new JSONArray(result[0]).length(), 20);
        assertEquals("23", result[1]);
        cursor.close();
    }

    /**
     * test getBatchOfEvents when reached default limited event number 100.
     *
     * @throws Exception exception.
     */
    @Test
    public void testGetBatchOfEventsReachedLimitNumber() throws Exception {
        for (int i = 0; i < 110; i++) {
            eventRecorder.recordEvent(event);
        }
        Cursor cursor = dbUtil.queryAllEvents();
        String[] result = getBatchOfEvents(cursor);
        assertEquals(2, result.length);
        assertEquals(new JSONArray(result[0]).length(), 100);
        assertEquals("103", result[1]);
        cursor.close();
    }

    /**
     * test getBatchOfEvents when reached default limited size.
     *
     * @throws Exception exception.
     */
    @Test
    public void testGetBatchOfEventsReachedDefaultLimitSize() throws Exception {
        for (int i = 0; i < 20; i++) {
            event.addAttribute("test_json_" + i, jsonString);
        }
        for (int i = 0; i < 30; i++) {
            eventRecorder.recordEvent(event);
        }
        Cursor cursor = dbUtil.queryAllEvents();
        String[] result = getBatchOfEvents(cursor);
        assertEquals(2, result.length);
        int length = new JSONArray(result[0]).length();
        assertTrue(length < 30);
        assertEquals(length, Integer.parseInt(result[1]) - 3);
        cursor.close();
    }

    /**
     * test process None event.
     *
     * @throws Exception exception.
     */
    @Test
    public void testProcessNoneEvent() throws Exception {
        setRequestPath(COLLECT_SUCCESS);
        assertEquals(0, dbUtil.getTotalNumber());
        int totalEventNumber = (int) ReflectUtil.invokeMethod(eventRecorder, "processEvents");
        assertEquals(0, totalEventNumber);
    }

    /**
     * test process event when success.
     *
     * @throws Exception exception.
     */
    @Test
    public void testProcessOneEventSuccess() throws Exception {
        setRequestPath(COLLECT_SUCCESS);
        eventRecorder.recordEvent(event);
        assertEquals(1, dbUtil.getTotalNumber());
        ReflectUtil.invokeMethod(eventRecorder, "processEvents");
        assertEquals(0, dbUtil.getTotalNumber());
    }

    /**
     * test process event when request fail.
     *
     * @throws Exception exception.
     */
    @Test
    public void testProcessOneEventFail() throws Exception {
        setRequestPath(COLLECT_FAIL);
        eventRecorder.recordEvent(event);
        assertEquals(1, dbUtil.getTotalNumber());
        ReflectUtil.invokeMethod(eventRecorder, "processEvents");
        assertEquals(1, dbUtil.getTotalNumber());
    }

    /**
     * test processEvent() for send multi event success with one request.
     *
     * @throws Exception exception.
     */
    @Test
    public void testProcessMultiEventSuccess() throws Exception {
        setRequestPath(COLLECT_SUCCESS);
        for (int i = 0; i < 20; i++) {
            eventRecorder.recordEvent(event);
        }
        assertEquals(20, dbUtil.getTotalNumber());
        ReflectUtil.invokeMethod(eventRecorder, "processEvents");
        assertEquals(0, dbUtil.getTotalNumber());
    }

    /**
     * test processEvent() for send multi event fail with one request.
     *
     * @throws Exception exception.
     */
    @Test
    public void testProcessMultiEventFail() throws Exception {
        setRequestPath(COLLECT_FAIL);
        for (int i = 0; i < 20; i++) {
            eventRecorder.recordEvent(event);
        }
        assertEquals(20, dbUtil.getTotalNumber());
        ReflectUtil.invokeMethod(eventRecorder, "processEvents");
        assertEquals(20, dbUtil.getTotalNumber());
    }


    /**
     * test processEvent() for more than one request and not reached default maxSubmissions.
     *
     * @throws Exception exception.
     */
    @Test
    public void testProcessEventNearReachMaxSubmissions() throws Exception {
        setRequestPath(COLLECT_SUCCESS);

        ByteArrayOutputStream logContent = new ByteArrayOutputStream();
        PrintStream oldSystemOut = System.out;
        System.setOut(new PrintStream(logContent));

        for (int i = 0; i < 40; i++) {
            event.addAttribute("test_json_" + i, jsonString);
        }
        for (int i = 0; i < 20; i++) {
            eventRecorder.recordEvent(event);
        }
        assertEquals(20, dbUtil.getTotalNumber());
        int eventNumber = (int) ReflectUtil.invokeMethod(eventRecorder, "processEvents");
        assertEquals(20, eventNumber);

        System.setOut(oldSystemOut);
        System.out.println(">>>>>>>\n" + logContent.toString() + "<<<<<<<\n");
        assertTrue(logContent.toString().contains("Send event number: 12"));
        assertTrue(
                "Debug log contains \"Send event number: 9\"",
                logContent.toString().contains("Send event number: 8"));
        assertEquals(0, dbUtil.getTotalNumber());

    }

    /**
     * test processEvent() for reached default maxSubmissions and not submit all event.
     *
     * @throws Exception exception.
     */
    @Test
    public void testProcessEventReachedMaxSubmissions() throws Exception {
        setRequestPath(COLLECT_SUCCESS);

        ByteArrayOutputStream logContent = new ByteArrayOutputStream();
        PrintStream oldSystemOut = System.out;
        System.setOut(new PrintStream(logContent));

        for (int i = 0; i < 40; i++) {
            event.addAttribute("test_json_" + i, jsonString);
        }
        for (int i = 0; i < 40; i++) {
            eventRecorder.recordEvent(event);
        }
        assertEquals(40, dbUtil.getTotalNumber());
        int eventNumber = (int) ReflectUtil.invokeMethod(eventRecorder, "processEvents");
        assertEquals(36, eventNumber);

        System.setOut(oldSystemOut);
        assertTrue(logContent.toString().contains("Send event number: 12"));
        assertTrue(logContent.toString().contains("Reached maxSubmissions: 3"));

        assertEquals(4, dbUtil.getTotalNumber());
    }

    /**
     * test submitEvents() for submit all event once.
     *
     * @throws Exception exception.
     */
    @Test
    public void testSubmitAllEventForOneRequest() throws Exception {
        setRequestPath(COLLECT_SUCCESS);
        for (int i = 0; i < 20; i++) {
            eventRecorder.recordEvent(event);
        }
        assertEquals(20, dbUtil.getTotalNumber());
        eventRecorder.submitEvents();
        assertTrue(((ThreadPoolExecutor) executorService).getActiveCount() < 2);
        Thread.sleep(2000);
        assertEquals(0, dbUtil.getTotalNumber());
    }

    /**
     * test submitEvents() for submit part of event.
     *
     * @throws Exception exception.
     */
    @Test
    public void testSubmitPartOfEventForMultiRequest() throws Exception {
        setRequestPath(COLLECT_SUCCESS);

        ByteArrayOutputStream logContent = new ByteArrayOutputStream();
        PrintStream oldSystemOut = System.out;
        System.setOut(new PrintStream(logContent));

        for (int i = 0; i < 40; i++) {
            event.addAttribute("test_json_" + i, jsonString);
        }
        for (int i = 0; i < 40; i++) {
            eventRecorder.recordEvent(event);
        }
        assertEquals(40, dbUtil.getTotalNumber());
        eventRecorder.submitEvents();
        assertEquals(1, ((ThreadPoolExecutor) executorService).getTaskCount());

        Thread.sleep(2000);

        System.setOut(oldSystemOut);
        assertTrue(logContent.toString().contains("Send event number: 12"));
        assertTrue(logContent.toString().contains("Reached maxSubmissions: 3"));

        assertEquals(4, dbUtil.getTotalNumber());
    }

    /**
     * test submitEvents() for submit all event twice.
     *
     * @throws Exception exception.
     */
    @Test
    public void testSubmitAllEventForMultiRequest() throws Exception {
        setRequestPath(COLLECT_SUCCESS);

        ByteArrayOutputStream logContent = new ByteArrayOutputStream();
        PrintStream oldSystemOut = System.out;
        System.setOut(new PrintStream(logContent));

        for (int i = 0; i < 40; i++) {
            event.addAttribute("test_json_" + i, jsonString);
        }
        for (int i = 0; i < 40; i++) {
            eventRecorder.recordEvent(event);
        }
        assertEquals(40, dbUtil.getTotalNumber());
        eventRecorder.submitEvents();
        eventRecorder.submitEvents();
        assertEquals(2, ((ThreadPoolExecutor) executorService).getTaskCount());
        assertTrue(((ThreadPoolExecutor) executorService).getActiveCount() < 2);
        Thread.sleep(2000);

        System.setOut(oldSystemOut);
        assertTrue(logContent.toString().contains("Send event number: 12"));
        assertTrue(logContent.toString().contains("Reached maxSubmissions: 3"));
        assertTrue(logContent.toString().contains("Send event number: 4"));

        assertEquals(0, dbUtil.getTotalNumber());
    }


    /**
     * test to submitEvents() for three times，and each times with multi request. the last times db is null.
     *
     * @throws Exception exception.
     */
    @Test
    public void testTimerThreeTimesSubmitAllEventForMultiRequest() throws Exception {
        setRequestPath(COLLECT_SUCCESS);

        ByteArrayOutputStream logContent = new ByteArrayOutputStream();
        PrintStream oldSystemOut = System.out;
        System.setOut(new PrintStream(logContent));

        for (int i = 0; i < 40; i++) {
            event.addAttribute("test_json_" + i, jsonString);
        }
        for (int i = 0; i < 40; i++) {
            eventRecorder.recordEvent(event);
        }
        assertEquals(40, dbUtil.getTotalNumber());
        for (int i = 0; i < 3; i++) {
            Thread.sleep(100);
            eventRecorder.submitEvents();
        }
        Thread.sleep(2000);

        System.setOut(oldSystemOut);
        assertTrue(logContent.toString().contains("Send event number: 12"));
        assertTrue(logContent.toString().contains("Reached maxSubmissions: 3"));
        assertTrue(logContent.toString().contains("Send event number: 4"));

        assertEquals(0, dbUtil.getTotalNumber());
    }

    /**
     * test to submitEvents() reached the thread pool LinkedBlockingQueue capacity.
     *
     * @throws Exception exception.
     */
    @Test
    public void testSubmitAllEventForReachTheQueueLimit() throws Exception {
        setRequestPath(COLLECT_SUCCESS);
        for (int i = 0; i < 40; i++) {
            event.addAttribute("test_json_" + i, jsonString);
        }
        for (int i = 0; i < 120; i++) {
            eventRecorder.recordEvent(event);
        }
        assertEquals(120, dbUtil.getTotalNumber());
        for (int i = 0; i < 1100; i++) {
            eventRecorder.submitEvents();
        }
        assertTrue(((ThreadPoolExecutor) executorService).getActiveCount() < 2);
        assertEquals(1001, ((ThreadPoolExecutor) executorService).getTaskCount());
    }

    /**
     * test record event with request parameter hash code.
     *
     * @throws Exception exception.
     */
    @Test
    public void testRecordEventRequestWithHashCode() throws Exception {
        clickstreamContext.getClickstreamConfiguration().withCompressEvents(false);
        setRequestPath(COLLECT_FOR_VERIFY_HASH_CODE);
        String eventJson = "[" + event.toJSONObject().toString() + "]";
        String eventHashCode = StringUtil.getHashCode(eventJson);
        server.request(and(by(uri(COLLECT_FOR_VERIFY_HASH_CODE)), eq(query("hashCode"), eventHashCode)))
            .response(status(200), text("success"));
        boolean requestResult = NetRequest.uploadEvents(eventJson, clickstreamContext.getClickstreamConfiguration(), 1);
        assertTrue(requestResult);
    }

    /**
     * test record event with request parameter upload timestamp.
     *
     * @throws Exception exception.
     */
    @Test
    public void testRecordEventRequestUploadTimestamp() throws Exception {
        setRequestPath(COLLECT_FOR_VERIFY_UPLOAD_TIMESTAMP);
        server.request(
                and(by(uri(COLLECT_FOR_VERIFY_UPLOAD_TIMESTAMP)), exist(query("upload_timestamp"))))
            .response(status(200), text("success"));
        boolean requestResult = NetRequest.uploadEvents("[]", clickstreamContext.getClickstreamConfiguration(), 1);
        assertTrue(requestResult);
    }

    /**
     * test save event failed and send event immediately.
     *
     * @throws Exception exception.
     */
    @Test
    public void testSaveEventFailedAndWillSendEventImmediately() throws Exception {
        ByteArrayOutputStream logContent = new ByteArrayOutputStream();
        PrintStream oldSystemOut = System.out;
        System.setOut(new PrintStream(logContent));

        dbUtil = mock(ClickstreamDBUtil.class);
        ReflectUtil.modifyField(eventRecorder, "dbUtil", dbUtil);
        when(dbUtil.saveEvent(event)).thenReturn(null);
        eventRecorder.recordEvent(event);

        System.setOut(oldSystemOut);
        assertTrue(logContent.toString().contains("Error to save event with EventType: testEvent"));

        Thread.sleep(1500);

        int bundleSequenceId = (int) ReflectUtil.getField(eventRecorder, "bundleSequenceId");
        assertTrue(bundleSequenceId > 1);
    }

    /**
     * common method to set request path.
     *
     * @param path request path for mock.
     * @throws Exception exception
     */
    private void setRequestPath(String path) throws Exception {
        ClickstreamContext context = (ClickstreamContext) ReflectUtil.getField(eventRecorder, "clickstreamContext");
        ClickstreamConfiguration config =
            (ClickstreamConfiguration) ReflectUtil.getField(context, "clickstreamConfiguration");
        ReflectUtil.modifyField(config, "endpoint", "http://localhost:8082" + path);
    }

    /**
     * getBatchOfEvents util.
     *
     * @param cursor db Cursor.
     * @return getBatchOfEvents() result.
     * @throws Exception exception.
     */
    private String[] getBatchOfEvents(Cursor cursor) throws Exception {
        cursor.moveToFirst();
        Method method = EventRecorder.class.getDeclaredMethod("getBatchOfEvents", Cursor.class);
        return (String[]) ReflectUtil.invokeMethod(eventRecorder, method, cursor);
    }

    /**
     * close db and finish executorService.
     */
    @After
    public void tearDown() {
        dbUtil.closeDB();
        executorService.shutdownNow();
    }

    /**
     * after class stop runner.
     */
    @AfterClass
    public static void afterClass() {
        runner.stop();
    }
}
