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

package software.aws.solution.clickstream.client.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;

import com.amazonaws.logging.Log;
import com.amazonaws.logging.LogFactory;
import software.aws.solution.clickstream.client.AnalyticsEvent;
import software.aws.solution.clickstream.client.EventRecorder;

/**
 * Clickstream Database Util.
 */
public class ClickstreamDBUtil {
    private static final Log LOG = LogFactory.getLog(EventRecorder.class);

    /**
     * ClickstreamDBBase is a basic helper for accessing the database.
     */
    private ClickstreamDBBase clickstreamDBBase;

    /**
     * Constructs a ClickstreamDBUtil with the given Context.
     *
     * @param context An instance of Context.
     */
    public ClickstreamDBUtil(final Context context) {
        if (clickstreamDBBase == null) {
            clickstreamDBBase = new ClickstreamDBBase(context);
        }
    }

    /**
     * Closes the DB Connection.
     */
    public void closeDB() {
        if (clickstreamDBBase != null) {
            clickstreamDBBase.closeDBHelper();
        }
    }

    /**
     * Saves an event into the database.
     *
     * @param event The AnalyticsEvent to be saved.
     * @return An Uri of the record inserted.
     */
    public Uri saveEvent(final AnalyticsEvent event) {
        Uri uri = null;
        try {
            uri = clickstreamDBBase.insert(clickstreamDBBase.getContentUri(), generateContentValuesFromEvent(event));
        } catch (SQLException error) {
            LOG.warn("SQLException: " + error.getMessage());
        }
        return uri;
    }

    private ContentValues generateContentValuesFromEvent(final AnalyticsEvent event) {
        ContentValues values = new ContentValues();
        String json = event.toJSONObject().toString();
        values.put(EventTable.COLUMN_JSON, json);
        values.put(EventTable.COLUMN_SIZE, json.length());
        return values;
    }

    /**
     * Queries all the events.
     *
     * @return A Cursor pointing to records in the database.
     */
    public Cursor queryAllEvents() {
        return clickstreamDBBase.query(clickstreamDBBase.getContentUri(),
            null, null, null, null, null);
    }

    /**
     * Queries all events from oldest. Does not include JSON.
     *
     * @param limit The limit of result set.
     * @return A Cursor pointing to records in the database.
     */
    public Cursor queryOldestEvents(final int limit) {
        return clickstreamDBBase.query(clickstreamDBBase.getContentUri(),
            new String[] {EventTable.COLUMN_ID, EventTable.COLUMN_SIZE},
            null, null, null,
            Integer.toString(limit));
    }

    /**
     * Deletes the event with the given eventId.
     *
     * @param eventId The eventId of the event to be deleted.
     * @return Number of rows deleted.
     */
    public int deleteEvent(final int eventId) {
        return clickstreamDBBase.delete(getEventUri(eventId), null,
            null);
    }

    /**
     * Deletes all the event where eventId is not larger than lastEventId.
     *
     * @param lastEventId The last eventId.
     * @return Number of rows deleted.
     */
    public int deleteBatchEvents(final int lastEventId) {
        return clickstreamDBBase.delete(getLastEventIdUri(lastEventId), null,
            null);
    }

    /**
     * Gets the Uri of an event.
     *
     * @param eventId The id of the event.
     * @return The Uri of the event specified by the id.
     */
    public Uri getEventUri(final int eventId) {
        return Uri.parse(clickstreamDBBase.getContentUri() + "/" + eventId);
    }

    /**
     * Gets the Uri of an event.
     *
     * @param lastEventId The id of the event which is the last.
     * @return The Uri of the event specified by the id.
     */
    public Uri getLastEventIdUri(final int lastEventId) {
        return Uri.parse(clickstreamDBBase.getContentUri() + "/last-event-id/" + lastEventId);
    }

    /**
     * Get the total event size calculate by sum of all event string's length.
     *
     * @return The total size.
     */
    public long getTotalSize() {
        return clickstreamDBBase.getTotalSize();
    }

    /**
     * Get the total event number calculate by sum of all events.
     *
     * @return The total number.
     */
    public long getTotalNumber() {
        return clickstreamDBBase.getTotalNumber();
    }
}

