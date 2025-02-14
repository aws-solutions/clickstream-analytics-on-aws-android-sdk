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

package software.aws.solution.clickstream;

import java.io.Serializable;

import okhttp3.Dns;

/**
 * Clickstream Configuration.
 */
public class ClickstreamConfiguration implements Serializable {
    private static final long DEFAULT_SEND_EVENTS_INTERVAL = 10000L;
    private static final long DEFAULT_CALL_TIME_OUT = 15000L;
    private static final long DEFAULT_SESSION_TIME_OUT = 1800000L;
    private String appId;
    private String endpoint;
    private Dns dns;
    private long sendEventsInterval;
    private long callTimeOut;
    private Boolean isCompressEvents;
    private Boolean isTrackScreenViewEvents;
    private Boolean isTrackUserEngagementEvents;
    private Boolean isTrackAppExceptionEvents;
    private Boolean isLogEvents;
    private String authCookie;
    private long sessionTimeoutDuration;
    private ClickstreamAttribute initialGlobalAttributes;

    /**
     * Create an {@link ClickstreamConfiguration} object.
     */
    public ClickstreamConfiguration() {
        // Creates a new ClickstreamConfiguration instance with default values.
    }

    static ClickstreamConfiguration getDefaultConfiguration() {
        ClickstreamConfiguration configuration = new ClickstreamConfiguration();
        configuration.sendEventsInterval = DEFAULT_SEND_EVENTS_INTERVAL;
        configuration.sessionTimeoutDuration = DEFAULT_SESSION_TIME_OUT;
        configuration.callTimeOut = DEFAULT_CALL_TIME_OUT;
        configuration.isCompressEvents = true;
        configuration.isTrackScreenViewEvents = true;
        configuration.isTrackUserEngagementEvents = true;
        configuration.isTrackAppExceptionEvents = false;
        configuration.isLogEvents = false;
        return configuration;
    }

    /**
     * The Clickstream AppId.
     *
     * @return the Clickstream AppId.
     */
    public String getAppId() {
        return this.appId;
    }

    /**
     * The Clickstream AppId.
     *
     * @param appId The Clickstream Application Id.
     * @return the current ClickstreamConfiguration instance.
     */
    @SuppressWarnings("checkstyle:hiddenfield")
    public ClickstreamConfiguration withAppId(final String appId) {
        this.appId = appId;
        return this;
    }

    /**
     * The Clickstream endpoint configured.
     *
     * @return the endpoint.
     */
    public String getEndpoint() {
        return this.endpoint;
    }

    /**
     * The endpoint for Clickstream.
     *
     * @param endpoint The endpoint.
     * @return the current ClickstreamConfiguration instance.
     */
    public ClickstreamConfiguration withEndpoint(final String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    /**
     * For get the Clickstream Okhttp3 dns.
     *
     * @return the dns.
     */
    public Dns getDns() {
        return this.dns;
    }

    /**
     * The Custom Okhttp3 dns for Clickstream.
     *
     * @param dns The custom dns.
     * @return the current ClickstreamConfiguration instance.
     */
    public ClickstreamConfiguration withCustomDns(final Dns dns) {
        this.dns = dns;
        return this;
    }

    /**
     * The interval of events sent at once.
     *
     * @return submit events interval.
     */
    public long getSendEventsInterval() {
        return this.sendEventsInterval;
    }

    /**
     * The interval of events sent at once.
     *
     * @param sendEventsInterval Submit events interval.
     * @return the current ClickstreamConfiguration instance.
     */
    public ClickstreamConfiguration withSendEventsInterval(final long sendEventsInterval) {
        this.sendEventsInterval = sendEventsInterval;
        return this;
    }

    /**
     * The time out of entire http call.
     *
     * @return callTimeOut.
     */
    public Long getCallTimeOut() {
        return this.callTimeOut;
    }

    /**
     * Is compress events.
     *
     * @return Is compress events.
     */
    public Boolean isCompressEvents() {
        return this.isCompressEvents;
    }

    /**
     * Is compress events.
     *
     * @param compressEvents Is compress events.
     * @return the current ClickstreamConfiguration instance.
     */
    public ClickstreamConfiguration withCompressEvents(final boolean compressEvents) {
        this.isCompressEvents = compressEvents;
        return this;
    }

    /**
     * Is track app screen view events.
     *
     * @return Is track appLifecycle events.
     */
    public Boolean isTrackScreenViewEvents() {
        return this.isTrackScreenViewEvents;
    }

    /**
     * Is track user engagement events.
     *
     * @return Is track user engagement events.
     */
    public Boolean isTrackUserEngagementEvents() {
        return this.isTrackUserEngagementEvents;
    }

    /**
     * Is track app screen view events.
     *
     * @param isTrackScreenViewEvents Is track screen view events.
     * @return the current ClickstreamConfiguration instance.
     */
    public ClickstreamConfiguration withTrackScreenViewEvents(final boolean isTrackScreenViewEvents) {
        this.isTrackScreenViewEvents = isTrackScreenViewEvents;
        return this;
    }

    /**
     * Is track user engagement events.
     *
     * @param isTrackUserEngagementEvents Is track user engagement events.
     * @return the current ClickstreamConfiguration instance.
     */
    public ClickstreamConfiguration withTrackUserEngagementEvents(final boolean isTrackUserEngagementEvents) {
        this.isTrackUserEngagementEvents = isTrackUserEngagementEvents;
        return this;
    }

    /**
     * Is track app exception events.
     *
     * @return Is track app exception events.
     */
    public Boolean isTrackAppExceptionEvents() {
        return this.isTrackAppExceptionEvents;
    }

    /**
     * Is track app exception events.
     *
     * @param isTrackAppExceptionEvents Is track app exception events.
     * @return the current ClickstreamConfiguration instance.
     */
    public ClickstreamConfiguration withTrackAppExceptionEvents(final boolean isTrackAppExceptionEvents) {
        this.isTrackAppExceptionEvents = isTrackAppExceptionEvents;
        return this;
    }

    /**
     * Is log events.
     *
     * @return Is log events json when record event.
     */
    public Boolean isLogEvents() {
        return this.isLogEvents;
    }

    /**
     * Is log events json when record event, set true for debug mode.
     *
     * @param isLogEvents Is log events json.
     * @return the current ClickstreamConfiguration instance.
     */
    public ClickstreamConfiguration withLogEvents(final boolean isLogEvents) {
        this.isLogEvents = isLogEvents;
        return this;
    }

    /**
     * Get The Clickstream authCookie.
     *
     * @return the authCookie.
     */
    public String getAuthCookie() {
        return this.authCookie;
    }

    /**
     * Set the auth cookie for Clickstream.
     *
     * @param authCookie The authCookie.
     * @return the current ClickstreamConfiguration instance.
     */
    public ClickstreamConfiguration withAuthCookie(final String authCookie) {
        this.authCookie = authCookie;
        return this;
    }

    /**
     * The interval of events session timeout duration.
     *
     * @return session timeout duration.
     */
    public long getSessionTimeoutDuration() {
        return this.sessionTimeoutDuration;
    }

    /**
     * The interval of events session timeout duration.
     *
     * @param sessionTimeoutDuration the duration of session timeout.
     * @return the current ClickstreamConfiguration instance.
     */
    public ClickstreamConfiguration withSessionTimeoutDuration(final long sessionTimeoutDuration) {
        this.sessionTimeoutDuration = sessionTimeoutDuration;
        return this;
    }

    /**
     * Set the global attribute when initialize the SDK.
     *
     * @param attribute global attributes.
     * @return the current ClickstreamConfiguration instance.
     */
    public ClickstreamConfiguration withInitialGlobalAttributes(ClickstreamAttribute attribute) {
        this.initialGlobalAttributes = attribute;
        return this;
    }

    /**
     * Get the global attribute.
     *
     * @return the global attribute instance.
     */
    public ClickstreamAttribute getInitialGlobalAttributes() {
        return this.initialGlobalAttributes;
    }
}

