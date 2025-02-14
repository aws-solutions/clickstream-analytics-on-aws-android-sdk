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

import android.util.Base64;

import com.amazonaws.logging.Log;
import com.amazonaws.logging.LogFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPOutputStream;

/**
 * String utility methods.
 */
public final class StringUtil {
    private static final Log LOG = LogFactory.getLog(StringUtil.class);
    private static final int HASH_CODE_BYTE_LENGTH = 4;
    private static final int HASH_CODE_PREFIX = 0xFF;

    /**
     * Default constructor.
     */
    private StringUtil() {
    }

    /**
     * Compress the string using the gzip.
     *
     * @param ungzipStr The raw string.
     * @return The string using the gzip to compress and encoding with base64.
     */
    public static String compressForGzip(String ungzipStr) {
        byte[] encode = null;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(ungzipStr.getBytes());
            gzip.close(); //NOSONAR
            encode = baos.toByteArray();
            baos.flush();
        } catch (UnsupportedEncodingException uee) {
            LOG.error("UnsupportedEncodingException occur when compressForGzip.");
        } catch (IOException ioe) {
            LOG.error("IOException occur when compressForGzip.");
        }
        if (encode != null) {
            return Base64.encodeToString(encode, Base64.NO_WRAP);
        } else {
            LOG.error("compressForGzip fail.");
            return null;
        }
    }

    /**
     * Determines if a string is null or zero-length.
     *
     * @param string a string.
     * @return true if the argument is null or zero-length, otherwise false.
     */
    public static boolean isNullOrEmpty(final String string) {
        return string == null || string.length() == 0;
    }

    /**
     * Determines if a string is blank.
     *
     * @param string a string.
     * @return true if the argument is blank, otherwise false.
     */
    public static boolean isBlank(final String string) {
        return string == null || string.trim().length() == 0;
    }

    /**
     * Reduces the input string to the number of chars, or its length if the
     * number of chars exceeds the input string's length.
     *
     * @param input          The string to clip.
     * @param numChars       the number of leading chars to keep (all others will be
     *                       removed).
     * @param appendEllipses The boolean value of append.
     * @return the clipped string.
     */
    public static String clipString(final String input, final int numChars, final boolean appendEllipses) {
        int end = Math.min(numChars, input.length());
        String output = input.substring(0, end);
        if (appendEllipses) {
            output = (output.length() < input.length()) ? output + "..." : output;
        }
        return output;
    }

    /**
     * Trims string to its last X characters. If string is too short, is padded
     * at the front with given char.
     *
     * @param str string to trim.
     * @param len length of desired string. (must be positive).
     * @param pad character to pad with.
     * @return The string after pad or trim.
     */
    public static String trimOrPadString(String str, int len, final char pad) {
        String curStr = str;
        int curLen = len;
        if (curLen < 0) {
            curLen = 0;
        }
        if (curStr == null) {
            curStr = "";
        }

        StringBuilder s = new StringBuilder();
        if (curStr.length() > curLen - 1) {
            s.append(curStr.substring(curStr.length() - curLen));
        } else {
            for (int i = 0; i < curLen - curStr.length(); i++) {
                s.append(pad);
            }
            s.append(curStr);
        }

        return s.toString();
    }

    /**
     * method for get event hash code.
     *
     * @param str event json string
     * @return the first 8 sha256 character of the event json
     */
    public static String getHashCode(String str) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
            digest.update(str.getBytes());
            return bytesToHexString(digest.digest());
        } catch (NoSuchAlgorithmException error) {
            LOG.error("Failed to get sha256 for str:" + str);
        }
        return "";
    }

    private static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < HASH_CODE_BYTE_LENGTH; i++) {
            String hex = Integer.toHexString(HASH_CODE_PREFIX & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }
}

