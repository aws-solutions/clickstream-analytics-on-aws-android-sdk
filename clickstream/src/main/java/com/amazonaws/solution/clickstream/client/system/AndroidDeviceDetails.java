/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amazonaws.solution.clickstream.client.system;

import android.os.Build;

import java.util.Locale;

/**
 * Android Device Details.
 */
public class AndroidDeviceDetails {

    private final String carrier;

    /**
     * The construct function with no parameter.
     */
    public AndroidDeviceDetails() {
        this.carrier = null;
    }

    /**
     * The construct function with parameters.
     *
     * @param carrier The name of carrier.
     */
    public AndroidDeviceDetails(String carrier) {
        this.carrier = carrier;
    }

    /**
     * Get the name of carrier.
     *
     * @return The name of carrier.
     */
    public String carrier() {
        return carrier;
    }

    /**
     * Get the version of platform.
     *
     * @return The version of platform.
     */
    public String platformVersion() {
        return Build.VERSION.RELEASE;
    }

    /**
     * Get the name of platform.
     *
     * @return The name of platform.
     */
    public String platform() {
        return "ANDROID";
    }

    /**
     * Get the name of manufacturer.
     *
     * @return The name of manufacturer.
     */
    public String manufacturer() {
        return Build.MANUFACTURER;
    }

    /**
     * Get the name of model.
     *
     * @return The name of model.
     */
    public String model() {
        return Build.MODEL;
    }

    /**
     * Get the locale.
     *
     * @return The locale.
     */
    public Locale locale() {
        return Locale.getDefault();
    }

}

