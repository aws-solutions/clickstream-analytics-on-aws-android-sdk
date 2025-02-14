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
import android.provider.Settings;
import android.telephony.TelephonyManager;

import java.io.Serializable;

/**
 * Android System.
 */
public class AndroidSystem implements Serializable {
    // UUID to identify a unique shared preferences and directory the library
    // can use, will be concatenated with the package to ensure no collision.
    private final String preferencesKeySuffix = "294262d4-8dbd-4bfd-816d-0fc81b3d32b7"; // NOSONAR
    private final AndroidPreferences preferences;
    private final AndroidConnectivity connectivity;
    private final AndroidAppDetails appDetails;
    private final AndroidDeviceDetails deviceDetails;
    private final String androidId;

    /**
     * The construct function with parameters.
     *
     * @param context The context of Android.
     */
    public AndroidSystem(final Context context) {
        preferences = new AndroidPreferences(context,
            context.getApplicationContext().getPackageName() + preferencesKeySuffix);
        connectivity = new AndroidConnectivity(context);
        appDetails = new AndroidAppDetails(context);
        deviceDetails = new AndroidDeviceDetails(getCarrier(context));
        androidId = Settings.System.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    /**
     * Get the carrier.
     *
     * @param context The context of Android.
     * @return The name of carrier.
     */
    private String getCarrier(final Context context) {
        try {
            TelephonyManager telephony = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
            if (null != telephony.getNetworkOperatorName()
                && !telephony.getNetworkOperatorName().equals("")) {
                return telephony.getNetworkOperatorName();
            } else {
                return "Unknown";
            }
        } catch (Exception exception) {
            return "Unknown";
        }
    }

    /**
     * Get the Android ID.
     *
     * @return Android ID
     */
    public String getAndroidId() {
        return androidId;
    }

    /**
     * Get the preference of Android.
     *
     * @return AndroidPreferences.
     */
    public AndroidPreferences getPreferences() {
        return preferences;
    }

    /**
     * Get the connectivity of Android.
     *
     * @return AndroidConnectivity.
     */
    public AndroidConnectivity getConnectivity() {
        if (connectivity != null) {
            connectivity.isConnected();
        }
        return connectivity;
    }

    /**
     * Get the details of app.
     *
     * @return AndroidAppDetails.
     */
    public AndroidAppDetails getAppDetails() {
        return appDetails;
    }

    /**
     * Get the details of device.
     *
     * @return AndroidDeviceDetails.
     */
    public AndroidDeviceDetails getDeviceDetails() {
        return deviceDetails;
    }
}

