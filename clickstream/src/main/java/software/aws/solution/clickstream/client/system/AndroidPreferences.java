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

package software.aws.solution.clickstream.client.system;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.Serializable;

/**
 * Android Preferences.
 */
public class AndroidPreferences implements Serializable {

    private final SharedPreferences preferences;

    /**
     * The default construct function.
     */
    public AndroidPreferences() {
        preferences = null;
    }

    /**
     * The construct function with parameters.
     * @param context The context of Android.
     * @param preferencesKey The key of preference.
     */
    public AndroidPreferences(final Context context,
                              final String preferencesKey) {
        preferences = context.getSharedPreferences(preferencesKey,
                Context.MODE_PRIVATE);
    }

    /**
     * Get the boolean value from the preference with the key.
     * @param key The key in the preference.
     * @param optValue The default value when no key found.
     * @return The boolean value from the preference with the key.
     */
    public boolean getBoolean(String key, boolean optValue) {
        return preferences.getBoolean(key, optValue);
    }

    /**
     * Get the integer value from the preference with the key.
     * @param key The key in the preference.
     * @param optValue The default value when no key found.
     * @return The integer value from the preference with the key.
     */
    public int getInt(String key, int optValue) {
        return preferences.getInt(key, optValue);
    }

    /**
     * Get the float value from the preference with the key.
     * @param key The key in the preference.
     * @param optValue The default value when no key found.
     * @return The float value from the preference with the key.
     */
    public float getFloat(String key, float optValue) {
        return preferences.getFloat(key, optValue);
    }

    /**
     * Get the long value from the preference with the key.
     * @param key The key in the preference.
     * @param optValue The default value when no key found.
     * @return The long value from the preference with the key.
     */
    public long getLong(String key, long optValue) {
        return preferences.getLong(key, optValue);
    }

    /**
     * Get the string value from the preference with the key.
     * @param key The key in the preference.
     * @param optValue The default value when no key found.
     * @return The string value from the preference with the key.
     */
    public String getString(String key, String optValue) {
        return preferences.getString(key, optValue);
    }

    /**
     * Set the boolean value with the key.
     * @param key The key in the preference.
     * @param value The value with the key.
     */
    public void putBoolean(String key, boolean value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    /**
     * Set the integer value with the key.
     * @param key The key in the preference.
     * @param value The value with the key.
     */
    public void putInt(String key, int value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    /**
     * Set the float value with the key.
     * @param key The key in the preference.
     * @param value The value with the key.
     */
    public void putFloat(String key, float value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putFloat(key, value);
        editor.apply();
    }

    /**
     * Set the long value with the key.
     * @param key The key in the preference.
     * @param value The value with the key.
     */
    public void putLong(String key, long value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(key, value);
        editor.apply();
    }

    /**
     * Set the string value with the key.
     * @param key The key in the preference.
     * @param value The value with the key.
     */
    public void putString(String key, String value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

}

