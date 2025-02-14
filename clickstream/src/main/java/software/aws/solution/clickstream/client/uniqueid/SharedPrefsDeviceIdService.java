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

package software.aws.solution.clickstream.client.uniqueid;

import android.provider.Settings;

import com.amazonaws.logging.Log;
import com.amazonaws.logging.LogFactory;
import software.aws.solution.clickstream.client.ClickstreamContext;
import software.aws.solution.clickstream.client.system.AndroidPreferences;
import software.aws.solution.clickstream.client.util.StringUtil;

import java.util.UUID;

/**
 * Shared Prefs Unique ID Services.
 */
public class SharedPrefsDeviceIdService {

    /**
     * The device ID key.
     */
    protected static final String DEVICE_ID_KEY = "DeviceId";
    private static final Log LOG = LogFactory.getLog(SharedPrefsDeviceIdService.class);

    /**
     * Uses Shared prefs to recall and store the unique ID.
     */
    public SharedPrefsDeviceIdService() {
        // Creates a new SharedPrefsDeviceIdService instance with default values.
    }

    /**
     * Get the Id based on the passed in clickstreamContext.
     *
     * @param context The Analytics clickstreamContext to use when looking up the id.
     * @return the Id of Analytics clickstreamContext.
     */
    public String getDeviceId(ClickstreamContext context) {
        if (context == null || context.getSystem() == null
            || context.getSystem().getPreferences() == null) {
            LOG.debug("Unable to generate unique id, clickstreamContext has not been fully initialized.");
            return "";
        }

        String uniqueId = getIdFromPreferences(context.getSystem().getPreferences());
        if (uniqueId == null || uniqueId.length() == 0) {
            // an id doesn't exist for this clickstreamContext, create set it from Android ID
            uniqueId = Settings.System.getString(context.getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);
            // if Android ID is not valid, replace it for UUID.
            if (StringUtil.isNullOrEmpty(uniqueId)) {
                uniqueId = UUID.randomUUID().toString();
            }
            storeUniqueId(context.getSystem().getPreferences(), uniqueId);
        }

        return uniqueId;
    }

    private String getIdFromPreferences(AndroidPreferences preferences) {
        return preferences.getString(DEVICE_ID_KEY, null);
    }

    private void storeUniqueId(AndroidPreferences preferences,
                               String uniqueId) {
        try {
            preferences.putString(DEVICE_ID_KEY, uniqueId);
        } catch (Exception exception) {
            // Do not log ex due to potentially sensitive information
            LOG.error(
                "Exception when trying to store the unique id into the Preferences.");
        }
    }
}

