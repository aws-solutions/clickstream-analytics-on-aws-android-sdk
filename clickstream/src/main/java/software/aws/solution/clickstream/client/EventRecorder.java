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

import android.database.Cursor;
import android.net.Uri;
import androidx.annotation.NonNull;

import com.amazonaws.logging.Log;
import com.amazonaws.logging.LogFactory;
import software.aws.solution.clickstream.client.db.ClickstreamDBUtil;
import software.aws.solution.clickstream.client.db.EventTable;
import software.aws.solution.clickstream.client.network.NetRequest;
import software.aws.solution.clickstream.client.network.NetUtil;
import software.aws.solution.clickstream.client.util.StringUtil;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Event Recorder.
 */
public class EventRecorder {
    static final String KEY_BUNDLE_SEQUENCE_ID_PREF = "event_bundle_sequence_id";

    private static final int DEFAULT_MAX_SUBMISSIONS_ALLOWED = 3;
    private static final int MAX_EVENT_OPERATIONS = 1000;
    private static final int QUERY_OLDEST_EVENT_LIMIT = 5;
    private static final long DEFAULT_MAX_SUBMISSION_SIZE = 512 * 1024;
    private static final long DEFAULT_MAX_DB_SIZE = 50 * 1024 * 1024;
    private static final Log LOG = LogFactory.getLog(EventRecorder.class);

    private static final int JSON_COLUMN_INDEX = EventTable.ColumnIndex.JSON.getValue();
    private static final int ID_COLUMN_INDEX = EventTable.ColumnIndex.ID.getValue();
    private static final int SIZE_COLUMN_INDEX = EventTable.ColumnIndex.SIZE.getValue();

    private final ClickstreamContext clickstreamContext;
    private final ClickstreamDBUtil dbUtil;
    private final ExecutorService submissionRunnableQueue;
    private int bundleSequenceId;

    EventRecorder(final ClickstreamContext clickstreamContext, final ClickstreamDBUtil dbUtil,
                  final ExecutorService submissionRunnableQueue) {
        this.clickstreamContext = clickstreamContext;
        this.dbUtil = dbUtil;
        this.submissionRunnableQueue = submissionRunnableQueue;
        this.bundleSequenceId = clickstreamContext.getSystem().getPreferences().getInt(KEY_BUNDLE_SEQUENCE_ID_PREF, 1);
    }

    /**
     * Constructs a new EventRecorder specifying the client to use.
     *
     * @param clickstreamContext The ClickstreamContext.
     * @return The instance of the ClickstreamContext.
     */
    public static EventRecorder newInstance(final ClickstreamContext clickstreamContext) {
        final ExecutorService submissionRunnableQueue =
            new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(MAX_EVENT_OPERATIONS),
                new ThreadPoolExecutor.DiscardPolicy());
        return new EventRecorder(clickstreamContext,
            new ClickstreamDBUtil(clickstreamContext.getApplicationContext().getApplicationContext()),
            submissionRunnableQueue);
    }

    /**
     * Records an {@link AnalyticsEvent}.
     *
     * @param event the analytics event
     * @return Uri the event uri.
     */
    public Uri recordEvent(@NonNull final AnalyticsEvent event) {
        final Uri uri = this.dbUtil.saveEvent(event);
        if (uri != null) {
            if (clickstreamContext.getClickstreamConfiguration() != null &&
                clickstreamContext.getClickstreamConfiguration().isLogEvents()) {
                LOG.info("save event: " + event.getEventType() + " success, event json:");
                LOG.info(event.toString());
            }
            while (this.dbUtil.getTotalSize() > DEFAULT_MAX_DB_SIZE) {
                try (Cursor cursor = this.dbUtil.queryOldestEvents(QUERY_OLDEST_EVENT_LIMIT)) {
                    while (this.dbUtil.getTotalSize() > DEFAULT_MAX_DB_SIZE && cursor.moveToNext()) {
                        this.dbUtil.deleteEvent(cursor.getInt(EventTable.ColumnIndex.ID.getValue()));
                    }
                }
            }
        } else {
            LOG.error(String.format("Error to save event with EventType: %s", event.getEventType()));
            sendEventImmediately(event);
        }
        return uri;
    }

    /**
     * Submit the events.
     */
    public void submitEvents() {
        if (NetUtil.isNetworkAvailable(clickstreamContext.getApplicationContext())) {
            submissionRunnableQueue.execute(this::processEvents);
        } else {
            LOG.warn("Device is offline, skipping submitting events to Clickstream server");
        }
    }

    /**
     * Process the events.
     */
    int processEvents() {
        final long start = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
        int totalEventNumber = 0;
        try (Cursor cursor = dbUtil.queryAllEvents()) {
            if (!cursor.moveToFirst()) {
                // if the cursor is empty there is nothing to do.
                return totalEventNumber;
            }
            LOG.debug("Start flushing events");
            int submissions = 0;
            do {
                final String[] event = this.getBatchOfEvents(cursor);
                int lastId = Integer.parseInt(event[1]);
                // upload events to server
                boolean result = NetRequest.uploadEvents(event[0], clickstreamContext.getClickstreamConfiguration(),
                    bundleSequenceId);
                bundleSequenceId += 1;
                clickstreamContext.getSystem().getPreferences().putInt(KEY_BUNDLE_SEQUENCE_ID_PREF, bundleSequenceId);
                if (!result) {
                    // if fail to upload event then end the process.
                    break;
                }
                // delete all uploaded event by last event id.
                try {
                    int deleteSize = dbUtil.deleteBatchEvents(lastId);
                    submissions++;
                    totalEventNumber += deleteSize;
                    LOG.debug("Send event number: " + deleteSize);
                } catch (final IllegalArgumentException exc) {
                    LOG.error(
                        String.format(Locale.US, "Failed to delete last event: %d with %s", lastId, exc.getMessage()));
                }
                // if the submissions time
                if (submissions >= DEFAULT_MAX_SUBMISSIONS_ALLOWED) {
                    LOG.debug("Reached maxSubmissions: " + DEFAULT_MAX_SUBMISSIONS_ALLOWED);
                    break;
                }
            } while (cursor.moveToNext());
            LOG.debug(String.format(Locale.US, "Time of attemptDelivery: %d",
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime()) - start));
        } catch (Exception exception) {
            LOG.error("Failed to send event", exception);
        }
        LOG.info(String.format(Locale.US, "Submitted %s events", totalEventNumber));
        return totalEventNumber;
    }

    /**
     * Reads events of maximum of KEY_MAX_SUBMISSION_SIZE size.
     * The default max request size is DEFAULT_MAX_SUBMISSION_SIZE.
     *
     * @param cursor the cursor to the database to read events from.
     * @return an String array of the events json and lastEventId.
     */
    String[] getBatchOfEvents(final Cursor cursor) {
        long currentRequestSize = 0;
        String lastEventId = null;
        final StringBuilder eventBuilder = new StringBuilder();
        eventBuilder.append("[");
        int eventNumber = 0;
        String suffix = ",";
        do {
            int size = cursor.getInt(SIZE_COLUMN_INDEX);
            String eventJson = cursor.getString(JSON_COLUMN_INDEX);
            if (!StringUtil.isNullOrEmpty(eventJson)) {
                currentRequestSize += size;
                eventNumber++;
                if (currentRequestSize > DEFAULT_MAX_SUBMISSION_SIZE ||
                    eventNumber > Event.Limit.MAX_EVENT_NUMBER_OF_BATCH) {
                    if (eventBuilder.length() > 2) {
                        int length = eventBuilder.length();
                        eventBuilder.replace(length - 1, length, "]");
                        lastEventId = String.valueOf(cursor.getInt(ID_COLUMN_INDEX) - 1);
                        cursor.moveToPrevious();
                        break;
                    }
                }
                if (cursor.isLast()) {
                    lastEventId = String.valueOf(cursor.getInt(ID_COLUMN_INDEX));
                    suffix = "]";
                }
                eventBuilder.append(eventJson);
                eventBuilder.append(suffix);
            }
        } while (cursor.moveToNext());

        return new String[] {eventBuilder.toString(), lastEventId};
    }

    /**
     * Method for send event immediately when event saved fail.
     * @param event AnalyticsEvent
     */
    public void sendEventImmediately(AnalyticsEvent event) {
        Runnable task = () -> {
            NetRequest.uploadEvents("[" + event.toJSONObject().toString() + "]",
                clickstreamContext.getClickstreamConfiguration(),
                bundleSequenceId);
            bundleSequenceId += 1;
            clickstreamContext.getSystem().getPreferences()
                .putInt(KEY_BUNDLE_SEQUENCE_ID_PREF, bundleSequenceId);
        };
        Executors.newSingleThreadExecutor().execute(task);
    }
}

