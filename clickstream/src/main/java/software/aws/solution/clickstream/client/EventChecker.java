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

import com.amazonaws.logging.Log;
import com.amazonaws.logging.LogFactory;
import org.json.JSONException;
import software.aws.solution.clickstream.ClickstreamAnalytics;
import software.aws.solution.clickstream.ClickstreamItem;
import software.aws.solution.clickstream.client.Event.ErrorCode;
import software.aws.solution.clickstream.client.Event.EventError;
import software.aws.solution.clickstream.client.Event.Limit;
import software.aws.solution.clickstream.client.util.StringUtil;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * handle the event errors.
 */
public final class EventChecker {
    private static final Log LOG = LogFactory.getLog(EventChecker.class);

    private static Set<String> itemKeySet;

    private EventChecker() {
    }

    /**
     * check the event type.
     *
     * @param eventName the event name
     * @return the EventError object
     */
    public static EventError checkEventName(String eventName) {
        EventError error = new EventError();
        error.setErrorCode(ErrorCode.NO_ERROR);
        if (Boolean.FALSE.equals(isValidName(eventName))) {
            error.setErrorCode(ErrorCode.EVENT_NAME_INVALID);
            error.setErrorMessage("Event name can only contains uppercase and lowercase letters, " +
                "underscores, number, and is not start with a number. event name: " + eventName);
        } else if (eventName.length() > Limit.MAX_LENGTH_OF_NAME) {
            error.setErrorCode(ErrorCode.EVENT_NAME_LENGTH_EXCEED);
            error.setErrorMessage("Event name is too long, the max event type length is " +
                Limit.MAX_LENGTH_OF_NAME + "characters. event name: " + eventName);
        }
        return error;
    }

    /**
     * verify the string whether only contains number, uppercase and lowercase letters, underscores,
     * and is not start with a number.
     *
     * @param name the name to verify
     * @return the name is valid.
     */
    public static Boolean isValidName(String name) {
        String pattern = "^(?!\\d)\\w+$";
        return Pattern.matches(pattern, name);
    }

    /**
     * check the attribute error.
     *
     * @param currentNumber current attribute number
     * @param name          attribute name.
     * @param value         attribute value.
     * @return the ErrorType
     */
    public static EventError checkAttribute(int currentNumber, String name, Object value) {
        EventError error = new EventError();
        error.setErrorCode(ErrorCode.NO_ERROR);
        String errorMsg = null;
        if (currentNumber >= Limit.MAX_NUM_OF_ATTRIBUTES) {
            errorMsg = "reached the max number of attributes limit ("
                + Limit.MAX_NUM_OF_ATTRIBUTES + "). and the attribute: " + name + " will not be recorded"; //NOSONAR
            error.setErrorCode(ErrorCode.ATTRIBUTE_SIZE_EXCEED);
            error.setErrorMessage(
                StringUtil.clipString("attribute name: " + name, Limit.MAX_LENGTH_OF_ERROR_VALUE, true));
        } else if (name.length() > Limit.MAX_LENGTH_OF_NAME) {
            errorMsg = "attribute : " + name + ", reached the max length of attributes name limit(" //NOSONAR
                + Limit.MAX_LENGTH_OF_NAME + "). current length is:(" + name.length() + //NOSONAR
                ") and the attribute will not be recorded"; //NOSONAR
            error.setErrorCode(ErrorCode.ATTRIBUTE_NAME_LENGTH_EXCEED);
            error.setErrorMessage(
                StringUtil.clipString("attribute name length is:(" + name.length() + ") name is:" + name,
                    Limit.MAX_LENGTH_OF_ERROR_VALUE, true));
        } else if (Boolean.FALSE.equals(isValidName(name))) {
            errorMsg = "attribute : " + name + ", was not valid, attribute name can only contains" +
                " uppercase and lowercase letters, underscores, number, and is not start with a number." + //NOSONAR
                " so the attribute will not be recorded"; //NOSONAR
            error.setErrorCode(ErrorCode.ATTRIBUTE_NAME_INVALID);
            error.setErrorMessage(StringUtil.clipString(name, Limit.MAX_LENGTH_OF_ERROR_VALUE, true));
        } else if (value instanceof String) {
            int valueLength = ((String) value).length();
            if (valueLength > Limit.MAX_LENGTH_OF_VALUE) {
                errorMsg = "attribute : " + name + ", reached the max length of attributes value limit ("
                    + Limit.MAX_LENGTH_OF_VALUE + "). current length is:(" + valueLength + //NOSONAR
                    "). and the attribute will not be recorded, attribute value:" + value; //NOSONAR
                error.setErrorCode(ErrorCode.ATTRIBUTE_VALUE_LENGTH_EXCEED);
                error.setErrorMessage(StringUtil.clipString("attribute name:" + name + ", attribute value:" + value,
                    Limit.MAX_LENGTH_OF_ERROR_VALUE, true));
            }
        }
        if (errorMsg != null) {
            LOG.warn(errorMsg);
        }
        return error;
    }

    /**
     * check the user attribute error.
     *
     * @param currentNumber current user attribute number.
     * @param name          attribute name.
     * @param value         attribute value.
     * @return the ErrorType
     */
    public static EventError checkUserAttribute(int currentNumber, String name, Object value) {
        EventError error = new EventError();
        error.setErrorCode(ErrorCode.NO_ERROR);
        String errorMsg = null;
        if (currentNumber >= Limit.MAX_NUM_OF_USER_ATTRIBUTES) {
            errorMsg = "reached the max number of user attributes limit ("
                + Limit.MAX_NUM_OF_USER_ATTRIBUTES + "). and the user attribute: "
                    + name + " will not be recorded"; //NOSONAR
            error.setErrorCode(ErrorCode.USER_ATTRIBUTE_SIZE_EXCEED);
            error.setErrorMessage(
                StringUtil.clipString("attribute name: " + name, Limit.MAX_LENGTH_OF_ERROR_VALUE, true));
        } else if (name.length() > Limit.MAX_LENGTH_OF_NAME) {
            errorMsg = "user attribute : " + name + ", reached the max length of attributes name limit(" //NOSONAR
                + Limit.MAX_LENGTH_OF_NAME + "). current length is:(" + name.length() + //NOSONAR
                ") and the attribute will not be recorded"; //NOSONAR
            error.setErrorCode(ErrorCode.USER_ATTRIBUTE_NAME_LENGTH_EXCEED);
            error.setErrorMessage(
                StringUtil.clipString("user attribute name length is:(" + name.length() + ") name is:" + name,
                    Limit.MAX_LENGTH_OF_ERROR_VALUE, true));
        } else if (Boolean.FALSE.equals(isValidName(name))) {
            errorMsg = "user attribute : " + name + ", was not valid, user attribute name can only contains" + //NOSONAR
                " uppercase and lowercase letters, underscores, number, and is not start with a number." +
                " so the attribute will not be recorded"; //NOSONAR
            error.setErrorCode(ErrorCode.USER_ATTRIBUTE_NAME_INVALID);
            error.setErrorMessage(StringUtil.clipString(name, Limit.MAX_LENGTH_OF_ERROR_VALUE, true));
        } else if (value instanceof String) {
            int valueLength = ((String) value).length();
            if (valueLength > Limit.MAX_LENGTH_OF_USER_VALUE) {
                errorMsg = "user attribute : " + name + ", reached the max length of attributes value limit (" //NOSONAR
                    + Limit.MAX_LENGTH_OF_USER_VALUE + "). current length is:(" + valueLength + //NOSONAR
                    "). and the attribute will not be recorded, attribute value:" + value; //NOSONAR
                error.setErrorCode(ErrorCode.USER_ATTRIBUTE_VALUE_LENGTH_EXCEED);
                error.setErrorMessage(
                    StringUtil.clipString("user attribute name:" + name + ", attribute value:" + value,
                        Limit.MAX_LENGTH_OF_ERROR_VALUE, true));
            }
        }
        if (errorMsg != null) {
            LOG.warn(errorMsg);
        }
        return error;
    }

    /**
     * check the user attribute error.
     *
     * @param currentNumber current item number.
     * @param item          Clickstream item.
     * @return the ErrorType
     */
    public static EventError checkItemAttribute(int currentNumber, ClickstreamItem item) {
        if (itemKeySet == null) {
            initItemKeySet();
        }

        EventError itemCountError = checkItemCount(currentNumber, item);
        if (itemCountError != null) {
            return itemCountError;
        }

        return validateItemAttributes(item, itemKeySet);
    }

    private static EventError checkItemCount(int currentNumber, ClickstreamItem item) {
        if (currentNumber >= Event.Limit.MAX_NUM_OF_ITEMS) {
            String itemKey = item.getAttributes().toString();
            String errorMsg = "reached the max number of items limit" + Event.Limit.MAX_NUM_OF_ITEMS +
                    ". and the item: " + itemKey + " will not be recorded"; //NOSONAR
            LOG.warn(errorMsg);
            return new EventError(ErrorCode.ITEM_SIZE_EXCEED,
                    StringUtil.clipString(errorMsg, Limit.MAX_LENGTH_OF_ERROR_VALUE, true));
        }
        return null;
    }

    /**
     * get the user attribute error.
     *
     * @param item          Clickstream item.
     * @param itemKeySet    set of predefined valid item keys.
     * @return the ErrorType
     */
    public static EventError validateItemAttributes(ClickstreamItem item, Set<String> itemKeySet) {
        int customKeyNumber = 0;
        Iterator<String> keys = item.getAttributes().keys();
        EventError error = new EventError();
        error.setErrorCode(ErrorCode.NO_ERROR);
        String errorMsg = null;
        while (keys.hasNext()) {
            String key = keys.next();
            String valueStr = "";
            try {
                valueStr = item.getAttributes().get(key).toString();
            } catch (JSONException exception) {
                LOG.warn("error getting item value for key: " + key + ", error message: " + exception.getMessage());
            }
            if (!itemKeySet.contains(key)) {
                customKeyNumber += 1;
                if (customKeyNumber > Limit.MAX_NUM_OF_CUSTOM_ITEM_ATTRIBUTE) {
                    errorMsg = "reached the max number of custom item attributes limit ("
                            + Limit.MAX_NUM_OF_CUSTOM_ITEM_ATTRIBUTE + "). and the custom item attribute: " + key +
                            " will not be recorded"; //NOSONAR
                    error.setErrorCode(ErrorCode.ITEM_CUSTOM_ATTRIBUTE_SIZE_EXCEED);
                    error.setErrorMessage(
                            StringUtil.clipString("item attribute key: " //NOSONAR
                                    + key, Limit.MAX_LENGTH_OF_ERROR_VALUE, true));
                } else if (key.length() > Limit.MAX_LENGTH_OF_NAME) {
                    errorMsg = "item attribute key: " + key //NOSONAR
                            + ", reached the max length of item attributes key limit("
                            + Limit.MAX_LENGTH_OF_NAME + "). current length is:(" + key.length() + //NOSONAR
                            ") and the item attribute will not be recorded"; //NOSONAR
                    error.setErrorCode(ErrorCode.ITEM_CUSTOM_ATTRIBUTE_KEY_LENGTH_EXCEED);
                    error.setErrorMessage(
                            StringUtil.clipString("item attribute key length is:(" + key.length() + ") key is:" + key,
                                    Limit.MAX_LENGTH_OF_ERROR_VALUE, true));
                } else if (Boolean.FALSE.equals(isValidName(key))) {
                    errorMsg = "item attribute key: " //NOSONAR
                            + key
                            + ", was not valid, item attribute key can only contains"
                            + " uppercase and lowercase letters, underscores, number, and is not start with a number."
                            + " so the item attribute will not be recorded"; //NOSONAR
                    error.setErrorCode(ErrorCode.ITEM_CUSTOM_ATTRIBUTE_KEY_INVALID);
                    error.setErrorMessage(StringUtil.clipString(key, Limit.MAX_LENGTH_OF_ERROR_VALUE, true));
                }
            }
            if (error.getErrorCode() == ErrorCode.NO_ERROR && valueStr.length() > Limit.MAX_LENGTH_OF_ITEM_VALUE) {
                errorMsg =
                        "item attribute : " + key + ", reached the max length of item attribute value limit (" +
                                Limit.MAX_LENGTH_OF_ITEM_VALUE + "). current length is: (" + valueStr.length() +
                                "). and the item attribute will not be recorded, attribute value: " + //NOSONAR
                                valueStr;
                String errorString =
                        "item attribute name: " + key + ", item attribute value: " + valueStr;
                error.setErrorCode(ErrorCode.ITEM_ATTRIBUTE_VALUE_LENGTH_EXCEED);
                error.setErrorMessage(StringUtil.clipString(errorString,
                        Limit.MAX_LENGTH_OF_ERROR_VALUE, true));
            }
            if (error.getErrorCode() > 0) {
                LOG.warn(errorMsg);
                return error;
            }
        }
        return error;
    }

    private static void initItemKeySet() {
        itemKeySet = new HashSet<>();
        itemKeySet.add(ClickstreamAnalytics.Item.ITEM_ID);
        itemKeySet.add(ClickstreamAnalytics.Item.ITEM_NAME);
        itemKeySet.add(ClickstreamAnalytics.Item.LOCATION_ID);
        itemKeySet.add(ClickstreamAnalytics.Item.ITEM_BRAND);
        itemKeySet.add(ClickstreamAnalytics.Item.CURRENCY);
        itemKeySet.add(ClickstreamAnalytics.Item.PRICE);
        itemKeySet.add(ClickstreamAnalytics.Item.QUANTITY);
        itemKeySet.add(ClickstreamAnalytics.Item.CREATIVE_NAME);
        itemKeySet.add(ClickstreamAnalytics.Item.CREATIVE_SLOT);
        itemKeySet.add(ClickstreamAnalytics.Item.ITEM_CATEGORY);
        itemKeySet.add(ClickstreamAnalytics.Item.ITEM_CATEGORY2);
        itemKeySet.add(ClickstreamAnalytics.Item.ITEM_CATEGORY3);
        itemKeySet.add(ClickstreamAnalytics.Item.ITEM_CATEGORY4);
        itemKeySet.add(ClickstreamAnalytics.Item.ITEM_CATEGORY5);
    }

}
