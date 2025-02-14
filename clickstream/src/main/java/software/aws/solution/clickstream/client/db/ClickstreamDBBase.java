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
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import java.io.Serializable;

/**
 * Clickstream Database Base.
 */
public class ClickstreamDBBase implements Serializable {
    private static final int EVENTS = 10;
    private static final int EVENT_ID = 20;
    private static final int EVENT_LAST_ID = 30;
    private static final String BASE_PATH = "clickstream-sdk/events";

    private final Context context;
    private final Uri contentUri;
    private final UriMatcher uriMatcher;
    private final ClickstreamDatabaseHelper databaseHelper;

    /**
     * Constructs TransferDBBase with the given Context.
     *
     * @param context A Context instance.
     */
    public ClickstreamDBBase(final Context context) {
        this.context = context;
        final String mAuthority = context.getApplicationContext().getPackageName();
        databaseHelper = new ClickstreamDatabaseHelper(this.context);
        contentUri = Uri.parse("content://" + mAuthority + "/" + BASE_PATH);
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        // The Uri of EVENTS is for all records in the Event table.
        uriMatcher.addURI(mAuthority, BASE_PATH, EVENTS);

        // The Uri of EVENT_ID is for a single event record.
        uriMatcher.addURI(mAuthority, BASE_PATH + "/#", EVENT_ID);
        // The Uri of EVENT_LAST_ID is for delete batch of event which id is equal or smaller than last event id.
        uriMatcher.addURI(mAuthority, BASE_PATH + "/last-event-id/#", EVENT_LAST_ID);
    }

    /**
     * Closes the database helper.
     */
    public void closeDBHelper() {
        databaseHelper.close();
    }

    /**
     * Gets the Uri for the table.
     *
     * @return The Uri for the table.
     */
    public Uri getContentUri() {
        return contentUri;
    }

    /**
     * Inserts a record to the table.
     *
     * @param uri    The Uri of a table.
     * @param values The values of a record.
     * @return The Uri of the inserted record.
     */
    public Uri insert(final Uri uri, final ContentValues values) {
        final int uriType = uriMatcher.match(uri);
        final SQLiteDatabase db = databaseHelper.getWritableDatabase();
        long id;
        if (uriType == EVENTS) {
            id = db.insertOrThrow(EventTable.TABLE_EVENT, null, values);
        } else {
            throw new IllegalArgumentException("Unknown URI: " + uri); //NOSONAR
        }
        return Uri.parse(BASE_PATH + "/" + id);
    }

    /**
     * Get total size of event records.
     *
     * @return Total size.
     */
    public long getTotalSize() {
        long totalSize;
        try (Cursor cursor = databaseHelper.getReadableDatabase()
            .rawQuery("SELECT SUM(" + EventTable.COLUMN_SIZE + ") FROM " + EventTable.TABLE_EVENT, null)) {
            if (!cursor.moveToNext()) {
                totalSize = 0;
            } else if (cursor.isNull(0)) {
                totalSize = 0;
            } else {
                totalSize = cursor.getLong(0);
            }
        }
        return totalSize;
    }

    /**
     * Get total number of event records.
     *
     * @return Total number.
     */
    public long getTotalNumber() {
        long totalNumber = 0;
        try (Cursor cursor = databaseHelper.getReadableDatabase()
            .rawQuery("SELECT count(*) FROM " + EventTable.TABLE_EVENT, null)) {
            if (cursor.moveToNext() && !cursor.isNull(0)) {
                totalNumber = cursor.getLong(0);
            }
        }
        return totalNumber;
    }

    /**
     * Query records from the database.
     *
     * @param uri           A Uri indicating which part of data to query.
     * @param projection    The projection of columns.
     * @param selection     The "where" clause of sql.
     * @param selectionArgs Strings in the "where" clause.
     * @param sortOrder     Sorting order of the query.
     * @param limit         Limit for query.
     * @return A Cursor pointing to records.
     */
    public Cursor query(final Uri uri, final String[] projection, final String selection, final String[] selectionArgs,
                        final String sortOrder, final String limit) {
        final SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(EventTable.TABLE_EVENT);
        final int uriType = uriMatcher.match(uri);
        switch (uriType) {
            case EVENTS:
                break;
            case EVENT_ID:
                queryBuilder.appendWhere(EventTable.COLUMN_ID + "=" + uri.getLastPathSegment());
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri); //NOSONAR
        }
        final SQLiteDatabase db = databaseHelper.getWritableDatabase();
        return queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder, limit);
    }

    /**
     * Deletes a record in the table.
     *
     * @param uri           A Uri of the specific record.
     * @param selection     The "where" clause of sql.
     * @param selectionArgs Strings in the "where" clause.
     * @return Number of rows deleted.
     */
    public int delete(final Uri uri, final String selection, final String[] selectionArgs) {
        final int uriType = uriMatcher.match(uri);
        final SQLiteDatabase db = databaseHelper.getWritableDatabase();
        int rowsDeleted;
        switch (uriType) {
            case EVENTS:
                rowsDeleted = db.delete(EventTable.TABLE_EVENT, selection, selectionArgs);
                break;
            case EVENT_ID:
                final String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = db.delete(EventTable.TABLE_EVENT, EventTable.COLUMN_ID + "=" + id, null);
                } else {
                    rowsDeleted =
                        db.delete(EventTable.TABLE_EVENT, EventTable.COLUMN_ID + "=" + id + " and " + selection,
                            selectionArgs);
                }
                break;
            case EVENT_LAST_ID:
                final String lastId = uri.getLastPathSegment();
                rowsDeleted = db.delete(EventTable.TABLE_EVENT, EventTable.COLUMN_ID + "<=" + lastId, null);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri); //NOSONAR
        }
        return rowsDeleted;
    }
}

