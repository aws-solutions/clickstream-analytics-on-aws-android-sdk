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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;

import com.amazonaws.logging.Log;
import com.amazonaws.logging.LogFactory;

import java.io.Serializable;
import java.util.Locale;

/**
 * Utility Tool for Android Connectivity.
 */
public class AndroidConnectivity implements Serializable {
    private static final Log LOG = LogFactory.getLog(AndroidConnectivity.class);
    /**
     * Check has WIFI or not.
     */
    private boolean hasWifi;
    /**
     * Check has mobile or not.
     */
    private boolean hasMobile;
    /**
     * Check is in the airplane mode or not.
     */
    private boolean inAirplaneMode;
    /**
     * The context of Android.
     */
    private final Context context;

    /**
     * The constructor of AndroidConnectivity.
     *
     * @param context The context of Android.
     */
    public AndroidConnectivity(final Context context) {
        this.context = context;
    }

    /**
     * Check the network is connected.
     *
     * @return The boolean result of connect.
     */
    public boolean isConnected() {
        determineAvailability();
        return hasWifi() || hasWAN();
    }

    /**
     * Check has the WIFI or not.
     *
     * @return The boolean value of the result.
     */
    public boolean hasWifi() {
        return this.hasWifi;
    }

    /**
     * Check has the WAN of not.
     *
     * @return The boolean value of the result.
     */
    public boolean hasWAN() {
        return this.hasMobile && !inAirplaneMode;
    }

    // this method access constants that were added in the HONEYCOMB_MR2 release
    // and is properly guarded from running on older devices.
    private void determineAvailability() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        inAirplaneMode = Settings.System.getInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) != 0;
        final NetworkInfo networkInfo = cm != null ? cm.getActiveNetworkInfo() : null;
        int networkType = 0;
        // default state
        hasWifi = false;
        // when we have connectivity manager, we assume we have some sort of
        // connectivity
        hasMobile = cm != null;
        // can we obtain network info?
        if (networkInfo != null) {
            if (networkInfo.isConnectedOrConnecting()) {
                networkType = networkInfo.getType();

                hasWifi = networkType == ConnectivityManager.TYPE_WIFI || networkType == ConnectivityManager.TYPE_WIMAX;
                hasMobile = networkType == ConnectivityManager.TYPE_MOBILE ||
                    networkType == ConnectivityManager.TYPE_MOBILE_DUN ||
                    networkType == ConnectivityManager.TYPE_MOBILE_HIPRI ||
                    networkType == ConnectivityManager.TYPE_MOBILE_MMS ||
                    networkType == ConnectivityManager.TYPE_MOBILE_SUPL;
            } else {
                // if neither connected or connecting then hasMobile defaults
                // need to be changed to false
                hasMobile = false;
            }
        }
        LOG.info(String.format(Locale.US, "Device Connectivity (%s)", getConnectivityStatus()));
    }

    private String getConnectivityStatus() {
        if (hasWifi) {
            return "On Wifi";
        } else if (hasMobile) {
            return "On Mobile";
        }
        return "No network connectivity";
    }
}

