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

package software.aws.solution.clickstream.client.util;

import com.amazonaws.logging.Log;
import com.amazonaws.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import software.aws.solution.clickstream.client.Event;
import software.aws.solution.clickstream.client.Session;
import software.aws.solution.clickstream.client.system.AndroidPreferences;

import java.util.UUID;

/**
 * Android Preferences Util.
 */
public final class PreferencesUtil {
    private static final Log LOG = LogFactory.getLog(PreferencesUtil.class);
    private static final String USER_ATTRIBUTE = "clickstream_user_attributes";
    private static final String USER_ID = "clickstream_user_id";
    private static final String USER_UNIQUE_ID = "user_unique_id";
    private static final String USER_UNIQUE_ID_MAP = "clickstream_user_unique_id";
    private static final String CURRENT_USER_UNIQUE_ID = "clickstream_current_user_unique_id";
    private static final String CURRENT_USER_FIRST_TOUCH_TIMESTAMP = "clickstream_current_user_first_touch_timestamp";
    private static final String CURRENT_SESSION = "clickstream_current_session";
    private static final String PREVIOUS_SCREEN_VIEW_TIMESTAMP = "clickstream_previous_screen_view_timestamp";

    /**
     * Default constructor.
     */
    private PreferencesUtil() {
    }

    /**
     * get the user attribute jsonObject from preferences.
     *
     * @param preferences AndroidPreferences
     * @return userAttribute JSONObject.
     */
    public static JSONObject getUserAttribute(final AndroidPreferences preferences) {
        JSONObject userAttribute = new JSONObject();
        String userAttributeString = preferences.getString(USER_ATTRIBUTE, "");
        if (StringUtil.isNullOrEmpty(userAttributeString)) {
            return userAttribute;
        }
        try {
            userAttribute = new JSONObject(userAttributeString);
        } catch (final JSONException jsonException) {
            LOG.error("Could not create Json object of userAttribute. error: " + jsonException.getMessage());
        }
        return userAttribute;
    }

    /**
     * update user attribute and save it to preferences.
     *
     * @param preferences   AndroidPreferences
     * @param userAttribute userAttribute JSONObject
     */
    public static void updateUserAttribute(final AndroidPreferences preferences, JSONObject userAttribute) {
        preferences.putString(USER_ATTRIBUTE, userAttribute.toString());
    }

    /**
     * get current userId.
     *
     * @param preferences AndroidPreferences
     * @return current userId.
     */
    public static String getCurrentUserId(final AndroidPreferences preferences) {
        return preferences.getString(USER_ID, "");
    }

    /**
     * set current userId.
     *
     * @param preferences AndroidPreferences
     * @param userId      current userId
     */
    public static void setCurrentUserId(final AndroidPreferences preferences, String userId) {
        preferences.putString(USER_ID, userId);
    }

    /**
     * get user info when switch to another user.
     *
     * @param preferences AndroidPreferences
     * @param userId      new userId
     * @return new user info contains user uniqueId and use first touch timestamp.
     */
    public static JSONObject getNewUserInfo(final AndroidPreferences preferences, String userId) {
        JSONObject userInfo = new JSONObject();
        String userUniqueIdJsonString = preferences.getString(USER_UNIQUE_ID_MAP, "{}");
        JSONObject userUniqueIdObject;
        try {
            userUniqueIdObject = new JSONObject(userUniqueIdJsonString);
            if (userUniqueIdObject.length() == 0) {
                // first new user login need to associate the userId and exist user uniqueId and save it to preferences.
                userInfo.put(USER_UNIQUE_ID, getCurrentUserUniqueId(preferences));
                userInfo.put("user_first_touch_timestamp", getCurrentUserFirstTouchTimestamp(preferences));
                userUniqueIdObject.put(userId, userInfo);
                preferences.putString(USER_UNIQUE_ID_MAP, userUniqueIdObject.toString());
            } else if (userUniqueIdObject.has(userId)) {
                // switch to old user.
                userInfo = userUniqueIdObject.getJSONObject(userId);
                setCurrentUserUniqueId(preferences, userInfo.getString(USER_UNIQUE_ID));
            } else {
                // switch to new user.
                String userUniqueId = UUID.randomUUID().toString();
                userInfo.put(USER_UNIQUE_ID, userUniqueId); //NOSONAR
                userInfo.put("user_first_touch_timestamp", System.currentTimeMillis());
                setCurrentUserUniqueId(preferences, userUniqueId);
                userUniqueIdObject.put(userId, userInfo);
                preferences.putString(USER_UNIQUE_ID_MAP, userUniqueIdObject.toString());
            }
        } catch (final JSONException jsonException) {
            LOG.error("Could not create Json object of user info. error: " + jsonException.getMessage());
        }
        return userInfo;
    }

    /**
     * get current user uniqueId.
     *
     * @param preferences AndroidPreferences
     * @return current user uniqueId.
     */
    public static String getCurrentUserUniqueId(final AndroidPreferences preferences) {
        String userUniqueId = preferences.getString(CURRENT_USER_UNIQUE_ID, "");
        if (StringUtil.isNullOrEmpty(userUniqueId)) {
            userUniqueId = UUID.randomUUID().toString();
            setCurrentUserUniqueId(preferences, userUniqueId);
            saveUserFirstTouchTimestamp(preferences);
        }
        return userUniqueId;
    }

    /**
     * save user first touch timestamp.
     *
     * @param preferences AndroidPreferences
     */
    private static void saveUserFirstTouchTimestamp(final AndroidPreferences preferences) {
        long firstTouchTimestamp = System.currentTimeMillis();
        preferences.putLong(CURRENT_USER_FIRST_TOUCH_TIMESTAMP, firstTouchTimestamp);
        try {
            JSONObject userAttribute = new JSONObject();
            JSONObject attribute = new JSONObject();
            attribute.put("value", firstTouchTimestamp);
            attribute.put("set_timestamp", firstTouchTimestamp);
            userAttribute.putOpt(Event.ReservedAttribute.USER_FIRST_TOUCH_TIMESTAMP, attribute);
            updateUserAttribute(preferences, userAttribute);
        } catch (JSONException exception) {
            LOG.error("Could not create Json object of user first touch timestamp. error: " + exception.getMessage());
        }
    }

    /**
     * get current user first touch timestamp.
     *
     * @param preferences AndroidPreferences
     * @return current user first touch timestamp.
     */
    public static long getCurrentUserFirstTouchTimestamp(final AndroidPreferences preferences) {
        return preferences.getLong(CURRENT_USER_FIRST_TOUCH_TIMESTAMP, 0);
    }

    /**
     * save current user uniqueId.
     *
     * @param preferences  AndroidPreferences
     * @param userUniqueId current user uniqueId
     */
    public static void setCurrentUserUniqueId(final AndroidPreferences preferences, String userUniqueId) {
        preferences.putString(CURRENT_USER_UNIQUE_ID, userUniqueId);
    }

    /**
     * save session as json string to preferences.
     *
     * @param preferences AndroidPreferences
     * @param session     session
     */
    public static void saveSession(final AndroidPreferences preferences, Session session) {
        JSONObject sessionObject = new JSONObject();
        try {
            sessionObject.put("sessionID", session.getSessionID());
            sessionObject.put("startTime", session.getStartTime());
            sessionObject.put("pauseTime", session.getPauseTime());
            sessionObject.put("sessionIndex", session.getSessionIndex());
            preferences.putString(CURRENT_SESSION, sessionObject.toString());
        } catch (JSONException exception) {
            LOG.error("Could not create Json object of session. error: " + exception.getMessage());
        }
    }

    /**
     * get session from preferences.
     *
     * @param preferences AndroidPreferences
     * @return stored session in preferences
     */
    public static Session getSession(final AndroidPreferences preferences) {
        String sessionJsonString = preferences.getString(CURRENT_SESSION, "");
        Session session = null;
        if (!StringUtil.isNullOrEmpty(sessionJsonString)) {
            try {
                JSONObject sessionObject = new JSONObject(sessionJsonString);
                session = new Session(
                    sessionObject.getString("sessionID"),
                    sessionObject.getLong("startTime"),
                    sessionObject.getLong("pauseTime"),
                    sessionObject.getInt("sessionIndex")
                );
            } catch (JSONException exception) {
                LOG.error("Could not create Json object of session. error: " + exception.getMessage());
            }
        }
        return session;
    }

    /**
     * get last screen view start timestamp.
     *
     * @param preferences AndroidPreferences
     * @return last screen view start timestamp
     */
    public static long getPreviousScreenViewTimestamp(final AndroidPreferences preferences) {
        return preferences.getLong(PREVIOUS_SCREEN_VIEW_TIMESTAMP, 0);
    }

    /**
     * set last screen view start timestamp.
     *
     * @param preferences AndroidPreferences
     * @param timestamp   the last screen view start time stamp
     */
    public static void savePreviousScreenViewTimestamp(final AndroidPreferences preferences, long timestamp) {
        preferences.putLong(PREVIOUS_SCREEN_VIEW_TIMESTAMP, timestamp);
    }

}
