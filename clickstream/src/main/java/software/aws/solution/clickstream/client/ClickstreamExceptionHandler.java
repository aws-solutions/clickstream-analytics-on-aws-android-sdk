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

import androidx.annotation.NonNull;

import com.amazonaws.logging.Log;
import com.amazonaws.logging.LogFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Exception handler for record app exception event.
 */
public final class ClickstreamExceptionHandler implements Thread.UncaughtExceptionHandler {
    private static final Log LOG = LogFactory.getLog(ClickstreamExceptionHandler.class);
    private static ClickstreamExceptionHandler handlerInstance;
    private static final int SLEEP_TIMEOUT_MS = 500;
    private Thread.UncaughtExceptionHandler defaultExceptionHandler;
    private final ClickstreamContext clickstreamContext;

    private ClickstreamExceptionHandler(ClickstreamContext context) {
        this.clickstreamContext = context;
    }

    /**
     * start listening the exception events.
     */
    public void startTrackException() {
        defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    /**
     * stop listening the exception events.
     */
    public void stopTackException() {
        Thread.setDefaultUncaughtExceptionHandler(null);
    }

    /**
     * init static method for ClickstreamExceptionHandler.
     *
     * @param context the clickstream context for initial the ClickstreamExceptionHandler
     * @return ClickstreamExceptionHandler the instance.
     */
    public static synchronized ClickstreamExceptionHandler init(ClickstreamContext context) {
        if (handlerInstance == null) {
            handlerInstance = new ClickstreamExceptionHandler(context);
        }
        return handlerInstance;
    }

    /**
     * fetch uncaught exception and record crash event.
     *
     * @param thread    the thread
     * @param throwable the exception
     */
    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
        try {
            Boolean isTrackAppExceptionEvents = clickstreamContext
                    .getClickstreamConfiguration().isTrackAppExceptionEvents();
            if (Boolean.TRUE.equals(isTrackAppExceptionEvents)) {
                trackExceptionEvent(throwable);
            }

            this.clickstreamContext.getAnalyticsClient().submitEvents();

            sleepWithInterruptHandling();

            if (defaultExceptionHandler != null) {
                defaultExceptionHandler.uncaughtException(thread, throwable);
            } else {
                killProcessAndExit();
            }
        } catch (Exception exception) {
            LOG.error("uncaughtException:", exception);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Tracks application exceptions by creating and recording an analytics event with exception details.
     *
     * @param throwable The throwable object containing exception details to be tracked
     */
    private void trackExceptionEvent(Throwable throwable) {
        String exceptionMessage = "";
        String exceptionStack = "";
        try {
            if (throwable.getMessage() != null) {
                exceptionMessage = throwable.getMessage();
            }
            final Writer writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            throwable.printStackTrace(printWriter);
            Throwable cause = throwable.getCause();
            while (cause != null) {
                cause.printStackTrace(printWriter);
                cause = cause.getCause();
            }

            printWriter.close();
            exceptionStack = writer.toString();
        } catch (Exception exception) {
            LOG.error("exception for get exception stack:", exception);
        }

        final AnalyticsEvent event =
                this.clickstreamContext.getAnalyticsClient().createEvent(Event.PresetEvent.APP_EXCEPTION);
        event.addInternalAttribute("exception_message", exceptionMessage);
        event.addInternalAttribute("exception_stack", exceptionStack);

        this.clickstreamContext.getAnalyticsClient().recordEvent(event);
    }

    /**
     * Pauses the current thread execution for a specified timeout period.
     * If interrupted while sleeping, logs the error and preserves the interrupt status.
     *
     * @throws RuntimeException if any other unexpected error occurs during sleep
     */
    private void sleepWithInterruptHandling() {
        try {
            Thread.sleep(SLEEP_TIMEOUT_MS);
        } catch (InterruptedException exception) {
            LOG.error("interrupted exception for sleep:", exception);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * exit app.
     */
    private void killProcessAndExit() {
        try {
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        } catch (Exception exception) {
            LOG.error("exit app exception:", exception);
        }
    }
}
