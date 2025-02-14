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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

import com.amazonaws.logging.Log;
import com.amazonaws.logging.LogFactory;

import java.io.Serializable;

/**
 * Android App Details.
 */
public class AndroidAppDetails implements Serializable {
    private static final Log LOG = LogFactory.getLog(AndroidAppDetails.class);
    private static final String UNKNOWN = "Unknown";
    private String appTitle;
    private String packageName;
    private String versionName;

    /**
     * The construct function with parameters of AndroidAppDetails.
     *
     * @param context The context of the Android.
     */
    public AndroidAppDetails(Context context) {
        Context applicationContext = context.getApplicationContext();
        try {
            PackageManager packageManager = applicationContext
                .getPackageManager();
            PackageInfo packageInfo = packageManager
                .getPackageInfo(applicationContext.getPackageName(), 0);
            ApplicationInfo appInfo = packageManager
                .getApplicationInfo(packageInfo.packageName, 0);

            appTitle = (String) packageManager.getApplicationLabel(appInfo);
            packageName = packageInfo.packageName;
            versionName = packageInfo.versionName;
        } catch (NameNotFoundException nameNotFoundException) {
            LOG.warn("Unable to get details for package " +
                applicationContext.getPackageName());
            appTitle = UNKNOWN;
            packageName = UNKNOWN;
            versionName = UNKNOWN;
        }
    }

    /**
     * Get the name of package.
     *
     * @return The name of package.
     */
    public String packageName() {
        return packageName;
    }

    /**
     * Get the name of version.
     *
     * @return The name of version.
     */
    public String versionName() {
        return versionName;
    }

    /**
     * Get the title of app.
     *
     * @return The title of app.
     */
    public String getAppTitle() {
        return appTitle;
    }

}

