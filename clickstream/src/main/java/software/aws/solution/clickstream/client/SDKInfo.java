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

import java.io.Serializable;

/**
 * Entity for SDKInfo.
 */
public class SDKInfo implements Serializable {
    private final String name;
    private final String version;

    /**
     * The constructor of SDKInfo.
     *
     * @param name    The name of SDKInfo.
     * @param version The version of SDKInfo.
     */
    public SDKInfo(final String name, final String version) {
        this.name = name;
        this.version = version;
    }

    /**
     * Get the name of SDK.
     *
     * @return The name of SDK.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the version of SDK.
     *
     * @return The version of SDK.
     */
    public String getVersion() {
        return version;
    }
}

