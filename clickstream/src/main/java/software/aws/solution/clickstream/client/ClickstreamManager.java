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

import android.content.Context;
import androidx.annotation.NonNull;

import com.amplifyframework.analytics.AnalyticsPropertyBehavior;

import com.amazonaws.AmazonClientException;
import com.amazonaws.logging.Log;
import com.amazonaws.logging.LogFactory;
import software.aws.solution.clickstream.AWSClickstreamPlugin;
import software.aws.solution.clickstream.BuildConfig;
import software.aws.solution.clickstream.ClickstreamConfiguration;

import java.util.Locale;
import java.util.Map;

/**
 * Clickstream Manager.
 */
public class ClickstreamManager {
    // This value is decided by the Clickstream
    private static final String SDK_NAME = "aws-solution-clickstream-sdk";
    private static final SDKInfo SDK_INFO = new SDKInfo(SDK_NAME, BuildConfig.VERSION_NAME);
    private static final Log LOG = LogFactory.getLog(AWSClickstreamPlugin.class);

    private final ClickstreamContext clickstreamContext;
    private final AnalyticsClient analyticsClient;
    private final SessionClient sessionClient;
    private final AutoRecordEventClient autoRecordEventClient;
    private ClickstreamExceptionHandler exceptionHandler;

    /**
     * Constructor.
     *
     * @param config     {@link Context} object.
     * @param appContext {@link ClickstreamConfiguration} object.
     * @throws AmazonClientException When RuntimeException occur.
     */
    public ClickstreamManager(@NonNull Context appContext, @NonNull final ClickstreamConfiguration config) {
        try {
            this.clickstreamContext = new ClickstreamContext(appContext, SDK_INFO, config);
            this.analyticsClient = new AnalyticsClient(this.clickstreamContext);
            this.clickstreamContext.setAnalyticsClient(this.analyticsClient);
            this.sessionClient = new SessionClient(this.clickstreamContext);
            this.autoRecordEventClient = new AutoRecordEventClient(this.clickstreamContext);
            this.clickstreamContext.setSessionClient(this.sessionClient);
            Boolean isTrackAppExceptionEvents = config.isTrackAppExceptionEvents();
            if (Boolean.TRUE.equals(isTrackAppExceptionEvents)) {
                exceptionHandler = ClickstreamExceptionHandler.init(this.clickstreamContext);
                enableTrackAppException();
            }
            setInitialGlobalAttributes(this.analyticsClient, config);
            LOG.debug(String.format(Locale.US,
                "Clickstream SDK(%s) initialization successfully completed", BuildConfig.VERSION_NAME));
            this.autoRecordEventClient.handleAppStart();
            handleSessionStart();
        } catch (final RuntimeException runtimeException) {
            LOG.error(String.format(Locale.US,
                "Cannot initialize Clickstream SDK %s", runtimeException.getMessage()));
            throw new AmazonClientException(runtimeException.getLocalizedMessage());
        }
    }

    private void setInitialGlobalAttributes(AnalyticsClient analyticsClient, ClickstreamConfiguration config) {
        if (config.getInitialGlobalAttributes() != null) {
            for (Map.Entry<String, AnalyticsPropertyBehavior<?>> entry : config.getInitialGlobalAttributes()
                .getAttributes()) {
                AnalyticsPropertyBehavior<?> property = entry.getValue();
                analyticsClient.addGlobalAttribute(entry.getKey(), property.getValue());
            }
            config.withInitialGlobalAttributes(null);
        }
    }

    /**
     * handle session start after SDK initialize.
     */
    private void handleSessionStart() {
        boolean isNewSession = sessionClient.initialSession();
        if (isNewSession) {
            autoRecordEventClient.handleSessionStart();
            autoRecordEventClient.setIsEntrances();
            sessionClient.startSession();
        }
    }

    /**
     * Enable track app exception.
     */
    public void enableTrackAppException() {
        if (exceptionHandler != null) {
            exceptionHandler.startTrackException();
        }
    }

    /**
     * Disable track app exception.
     */
    public void disableTrackAppException() {
        if (exceptionHandler != null) {
            exceptionHandler.stopTackException();
        }
    }

    /**
     * Get the Clickstream context.
     *
     * @return The Clickstream context.
     */
    public ClickstreamContext getClickstreamContext() {
        return clickstreamContext;
    }

    /**
     * The {@link AnalyticsClient} is the primary class used to create, store, and
     * submit events from your application.
     *
     * @return an {@link AnalyticsClient}
     */
    public AnalyticsClient getAnalyticsClient() {
        return analyticsClient;
    }

    /**
     * The {@link SessionClient} is the primary class used to create, store session from your application.
     *
     * @return a {@link SessionClient}
     */
    public SessionClient getSessionClient() {
        return sessionClient;
    }

    /**
     * The {@link AutoRecordEventClient} is aim to auto record lifecycle relates event.
     *
     * @return a {@link AutoRecordEventClient}
     */
    public AutoRecordEventClient getAutoRecordEventClient() {
        return autoRecordEventClient;
    }
}

