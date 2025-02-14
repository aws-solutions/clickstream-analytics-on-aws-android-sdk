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

import software.aws.solution.clickstream.client.util.PreferencesUtil;
import software.aws.solution.clickstream.client.util.StringUtil;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 * A Session Object Mostly immutable, with the exception of its stop time. This
 * session object tracks whether or not it has been paused by checking the
 * status of it's stop time. A session's stop time is only set when the session
 * has been paused.
 */
public final class Session implements Serializable {
    /**
     * The session ID date format.
     */
    private static final String SESSION_ID_DATE_FORMAT = "yyyyMMdd";
    /**
     * The session ID time format.
     */
    private static final String SESSION_ID_TIME_FORMAT = "HHmmssSSS";
    /**
     * The session ID delimiter.
     */
    private static final char SESSION_ID_DELIMITER = '-';
    /**
     * The session ID pad char.
     */
    private static final char SESSION_ID_PAD_CHAR = '_';
    /**
     * The session ID unique ID length.
     */
    private static final int SESSION_ID_UNIQUE_ID_LENGTH = 8;
    private final DateFormat sessionIdTimeFormat;
    private final String sessionID;
    private final Long startTime;
    private Long pauseTime;
    private final int sessionIndex;

    private boolean isStarted;

    /**
     * CONSTRUCTOR - ACTUAL Used by SessionClient.
     *
     * @param context The context of ClickstreamContext.
     * @throws NullPointerException When the context is null.
     */
    Session(@NonNull final ClickstreamContext context, final int sessionIndex) {
        this.sessionIdTimeFormat = new SimpleDateFormat(SESSION_ID_DATE_FORMAT +
            SESSION_ID_DELIMITER + SESSION_ID_TIME_FORMAT, Locale.US);
        this.sessionIdTimeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.startTime = System.currentTimeMillis();
        this.pauseTime = null;
        this.sessionID = this.generateSessionID(context);
        this.sessionIndex = sessionIndex;
    }

    /**
     * Used by deserializer.
     *
     * @param sessionID    The session ID.
     * @param startTime    The start session timestamp.
     * @param pauseTime    The stop session timestamp.
     * @param sessionIndex the index of session.
     */
    public Session(final String sessionID, final Long startTime, final Long pauseTime, final int sessionIndex) {
        this.sessionIdTimeFormat = new SimpleDateFormat(SESSION_ID_DATE_FORMAT +
            SESSION_ID_DELIMITER + SESSION_ID_TIME_FORMAT, Locale.US);
        this.sessionIdTimeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.startTime = startTime;
        this.pauseTime = pauseTime;
        this.sessionID = sessionID;
        this.sessionIndex = sessionIndex;
    }

    /**
     * Get the session object from preferences, if exists and not expired return it.
     * otherwise create a new session.
     *
     * @param context         The {@link ClickstreamContext}.
     * @param previousSession The previous session for check whether is new session.
     * @return new Session object.
     */
    public static Session getInstance(final ClickstreamContext context, Session previousSession) {
        Session session = previousSession;
        if (session == null) {
            session = PreferencesUtil.getSession(context.getSystem().getPreferences());
        }
        if (session != null) {
            if (session.getPauseTime() == null ||
                System.currentTimeMillis() - session.getPauseTime() <
                    context.getClickstreamConfiguration().getSessionTimeoutDuration()) {
                return session;
            } else {
                return new Session(context, session.getSessionIndex() + 1);
            }
        } else {
            return new Session(context, 1);
        }
    }

    /**
     * function for get session whether is started.
     *
     * @return session whether is started.
     */
    public boolean isStarted() {
        return this.isStarted;
    }

    /**
     * function for flag the session is started.
     */
    public void start() {
        this.isStarted = true;
    }

    /**
     * Pauses the session object. Generates a stop time.
     */
    public void pause() {
        this.pauseTime = System.currentTimeMillis();
    }

    /**
     * function for is new session.
     *
     * @return is new session
     */
    public boolean isNewSession() {
        return this.pauseTime == null;
    }

    /**
     * Calculates and returns the session's duration Returns a duration of 0 if
     * session is not paused or the system clock has been tampered with.
     *
     * @return session duration in milliseconds.
     */
    public Long getSessionDuration() {
        Long time = this.pauseTime;
        if (time == null) {
            time = System.currentTimeMillis();
        }

        if (time < this.startTime) {
            return 0L;
        }
        return time - this.startTime;
    }

    /**
     * Generates Session ID by concatenating present AppKey, UniqueID, and
     * AppKey-UniqueID-yyyyMMdd-HHmmssSSS.
     *
     * @param context The {@link ClickstreamContext}.
     * @return [String] SessionID.
     */
    public String generateSessionID(final ClickstreamContext context) {
        final String uniqueId = context.getDeviceId();
        final String time = this.sessionIdTimeFormat.format(this.startTime);
        return StringUtil.trimOrPadString(uniqueId, SESSION_ID_UNIQUE_ID_LENGTH, SESSION_ID_PAD_CHAR) +
            SESSION_ID_DELIMITER + time;
    }

    /**
     * Get session ID.
     *
     * @return The string of session ID.
     */
    public String getSessionID() {
        return this.sessionID;
    }

    /**
     * Get the start session timestamp.
     *
     * @return The long value of start session timestamp.
     */
    public long getStartTime() {
        return this.startTime;
    }

    /**
     * Get the stop session timestamp.
     *
     * @return The long value of stop session timestamp.
     */
    public Long getPauseTime() {
        return this.pauseTime;
    }

    /**
     * Get session index.
     *
     * @return The value of session index.
     */
    public int getSessionIndex() {
        return this.sessionIndex;
    }
}

