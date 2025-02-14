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

package software.aws.solution.clickstream.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import software.aws.solution.clickstream.AnalyticsEventTest;
import software.aws.solution.clickstream.client.AnalyticsEvent;
import software.aws.solution.clickstream.client.db.ClickstreamDBBase;
import software.aws.solution.clickstream.client.db.ClickstreamDBUtil;
import software.aws.solution.clickstream.util.ReflectUtil;

import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 26)
public class DBUtilTest {

    private ClickstreamDBUtil dbUtil;
    private AnalyticsEvent analyticsEvent;

    /**
     * prepare dbUtil and context.
     */
    @Before
    public void setup() {
        dbUtil = new ClickstreamDBUtil(ApplicationProvider.getApplicationContext());
        analyticsEvent = AnalyticsEventTest.getAnalyticsClient().createEvent("testEvent");
        dbUtil.deleteBatchEvents(3);
    }

    /**
     * test insert single event.
     */
    @Test
    public void testInsertSingleEvent() {
        Uri uri = dbUtil.saveEvent(analyticsEvent);
        int idInserted = Integer.parseInt(Objects.requireNonNull(uri.getLastPathSegment()));
        assertNotEquals(idInserted, 0);
    }

    /**
     * test insert single event failed.
     *
     * @throws Exception exception.
     */
    @Test
    public void testInsertSingleEventFailed() throws Exception {
        ClickstreamDBBase clickstreamDBBase = mock(ClickstreamDBBase.class);
        ReflectUtil.modifyField(dbUtil, "clickstreamDBBase", clickstreamDBBase);
        doThrow(new SQLException("Mocked SQLException")).when(clickstreamDBBase).insert(any(Uri.class), any(
            ContentValues.class));
        Uri uri = dbUtil.saveEvent(analyticsEvent);
        assertNull(uri);
    }

    /**
     * test query all.
     */
    @Test
    public void testQueryAll() {
        Uri uri1 = dbUtil.saveEvent(analyticsEvent);
        Uri uri2 = dbUtil.saveEvent(analyticsEvent);
        int idInserted1 = Integer.parseInt(Objects.requireNonNull(uri1.getLastPathSegment()));
        int idInserted2 = Integer.parseInt(Objects.requireNonNull(uri2.getLastPathSegment()));
        assertNotEquals(idInserted1, 0);
        assertNotEquals(idInserted2, 0);
        Cursor c = dbUtil.queryAllEvents();
        assertNotNull(c);
        assertEquals(c.getCount(), 2);
        c.close();
    }


    /**
     * test delete two event.
     */
    @Test
    public void testDelete() {
        Uri uri1 = dbUtil.saveEvent(analyticsEvent);
        Uri uri2 = dbUtil.saveEvent(analyticsEvent);
        int idInserted1 = Integer.parseInt(Objects.requireNonNull(uri1.getLastPathSegment()));
        int idInserted2 = Integer.parseInt(Objects.requireNonNull(uri2.getLastPathSegment()));
        assertNotEquals(idInserted1, 0);
        assertNotEquals(idInserted2, 0);
        Cursor c = dbUtil.queryAllEvents();
        assertNotNull(c);
        assertEquals(c.getCount(), 2);
        c.close();

        int delete1 = dbUtil.deleteEvent(idInserted1);
        assertEquals(delete1, 1);
        Cursor c1 = dbUtil.queryAllEvents();
        assertNotNull(c1);
        assertEquals(c1.getCount(), 1);
        c1.close();

        int delete2 = dbUtil.deleteEvent(idInserted2);
        assertEquals(delete2, 1);
        Cursor c2 = dbUtil.queryAllEvents();
        assertNotNull(c2);
        assertEquals(c2.getCount(), 0);
        c2.close();
    }

    /**
     * test get total size.
     */
    @Test
    public void testGetTotalDbSize() {
        int eventLength1 = analyticsEvent.toJSONObject().toString().length();
        Uri uri1 = dbUtil.saveEvent(analyticsEvent);
        int eventLength2 = analyticsEvent.toJSONObject().toString().length();
        Uri uri2 = dbUtil.saveEvent(analyticsEvent);
        int idInserted1 = Integer.parseInt(Objects.requireNonNull(uri1.getLastPathSegment()));
        int idInserted2 = Integer.parseInt(Objects.requireNonNull(uri2.getLastPathSegment()));
        assertNotEquals(idInserted1, 0);
        assertNotEquals(idInserted2, 0);
        Cursor c = dbUtil.queryAllEvents();
        assertNotNull(c);
        assertEquals(c.getCount(), 2);
        c.close();
        Assert.assertTrue(dbUtil.getTotalSize() - (eventLength1 + eventLength2) < 10);
    }

    /**
     * test get total number.
     */
    @Test
    public void testGetTotalDbNumber() {
        Uri uri1 = dbUtil.saveEvent(analyticsEvent);
        Uri uri2 = dbUtil.saveEvent(analyticsEvent);
        int idInserted1 = Integer.parseInt(Objects.requireNonNull(uri1.getLastPathSegment()));
        int idInserted2 = Integer.parseInt(Objects.requireNonNull(uri2.getLastPathSegment()));
        assertNotEquals(idInserted1, 0);
        assertNotEquals(idInserted2, 0);
        assertEquals(dbUtil.getTotalNumber(), 2);
    }

    /**
     * close db.
     */
    @After
    public void tearDown() {
        dbUtil.closeDB();
    }
}
