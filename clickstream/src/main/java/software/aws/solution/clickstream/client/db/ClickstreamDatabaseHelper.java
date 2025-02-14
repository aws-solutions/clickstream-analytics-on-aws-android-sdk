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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.Serializable;

/**
 * Clickstream Database Helper.
 */
public class ClickstreamDatabaseHelper extends SQLiteOpenHelper implements Serializable {
    private static final String DATABASE_NAME = "clickstream.db";
    private static final int DATABASE_VERSION = 1;

    private final int version;

    /**
     * The constructor with parameters.
     * @param context the context of Android.
     */
    public ClickstreamDatabaseHelper(final Context context) {
        this(context, DATABASE_VERSION);
    }

    /**
     * The constructor with parameters.
     * @param context The context of Android.
     * @param version The version of SDK.
     */
    public ClickstreamDatabaseHelper(final Context context, final int version) {
        super(context, DATABASE_NAME, null, version);
        this.version = version;
    }

    /**
     * Set the configuration of SQLite database.
     * @param database The instance of SQLite database.
     */
    public void onConfigure(final SQLiteDatabase database) {
        database.execSQL("PRAGMA auto_vacuum = FULL");
    }

    /**
     * Creates the database.
     *
     * @param database An SQLiteDatabase instance.
     */
    @Override
    public void onCreate(final SQLiteDatabase database) {
        EventTable.onCreate(database, version);
    }

    /**
     * Upgrades the database.
     *
     * @param database   An SQLiteDatabase instance.
     * @param oldVersion The old version of the database.
     * @param newVersion The new version of the database.
     */
    @Override
    public void onUpgrade(final SQLiteDatabase database, final int oldVersion, final int newVersion) {
        EventTable.onUpgrade(database, oldVersion, newVersion);
    }
}

