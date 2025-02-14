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

import android.database.sqlite.SQLiteDatabase;

/**
 * Database Table for Event.
 */
public final class EventTable {
    /**
     * Database table name.
     */
    public static final String TABLE_EVENT = "clickstreamevent";
    /**
     * A unique id of the clickstream event.
     */
    public static final String COLUMN_ID = "event_id";
    /**
     * The JSON body of the clickstream event.
     */
    public static final String COLUMN_JSON = "event_json";
    /**
     * The size of JSON body of the clickstream event.
     */
    public static final String COLUMN_SIZE = "event_size";
    /**
     * Database creation SQL statement.
     */
    private static final String DATABASE_CREATE = "create table if not exists " + TABLE_EVENT +
        "(" + COLUMN_ID + " integer primary key autoincrement, "
        + COLUMN_SIZE + " INTEGER NOT NULL,"
        + COLUMN_JSON + " TEXT NOT NULL" + ");";

    /**
     * The default constructor.
     */
    private EventTable() {
    }

    /**
     * Creates the database.
     *
     * @param database An SQLiteDatabase instance.
     * @param version  The version of database.
     */
    public static void onCreate(final SQLiteDatabase database, final int version) {
        database.execSQL(DATABASE_CREATE);
        onUpgrade(database, 1, version);
    }

    /**
     * Upgrades the database.
     *
     * @param database   An SQLiteDatabase instance.
     * @param oldVersion The old version of the database.
     * @param newVersion The new version of the database.
     */
    public static void onUpgrade(final SQLiteDatabase database, final int oldVersion, final int newVersion) { //NOSONAR
        // do nothing
    }

    /**
     * Column Index Structure.
     */
    public enum ColumnIndex {
        /**
         * The ID of the column.
         */
        ID(0),
        /**
         * The size of the column.
         */
        SIZE(1),
        /**
         * The JSON body of the column.
         */
        JSON(2);

        private final int value;

        /**
         * The constructor.
         *
         * @param value The value.
         */
        ColumnIndex(final int value) {
            this.value = value;
        }

        /**
         * Get the value.
         *
         * @return The value.
         */
        public int getValue() {
            return value;
        }
    }
}

