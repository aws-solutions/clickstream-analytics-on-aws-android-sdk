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

package software.aws.solution.clickstream.client.network;

import androidx.annotation.NonNull;

import com.amplifyframework.util.UserAgent;

import com.amazonaws.logging.Log;
import com.amazonaws.logging.LogFactory;
import software.aws.solution.clickstream.ClickstreamConfiguration;
import software.aws.solution.clickstream.client.util.StringUtil;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * send net request.
 */
public final class NetRequest {

    private static final Log LOG = LogFactory.getLog(NetRequest.class);
    private static final long HTTP_CONNECT_TIME_OUT = 10;
    private static final long HTTP_READ_TIME_OUT = 10;
    private static final long HTTP_WRITE_TIME_OUT = 10;

    /**
     * Default constructor.
     */
    private NetRequest() {
    }

    /**
     * upload batch of recorded events to server.
     *
     * @param eventJson     event json string
     * @param configuration the ClickstreamConfiguration.
     * @param bundleSequenceId the bundle sequence id.
     * @return submit result.
     */
    public static boolean uploadEvents(String eventJson, ClickstreamConfiguration configuration, int bundleSequenceId) {
        if (StringUtil.isNullOrEmpty(eventJson)) {
            return false;
        }
        try (Response response = request(eventJson, configuration, bundleSequenceId);
             ResponseBody ignored = response.body()) {
            if (response.isSuccessful()) {
                LOG.debug("submitEvents success. \n" + response);
                return true;
            } else {
                LOG.error("submitEvents fail. \n" + response);
            }
        } catch (final Exception exception) {
            LOG.error("submitEvents error: " + exception.getMessage());
        }
        return false;
    }

    /**
     * make request.
     *
     * @param eventJson     events to send.
     * @param configuration ClickstreamConfiguration
     * @return the sync okhttp Response
     * @throws IOException throw IOException.
     */
    private static Response request(@NonNull String eventJson, @NonNull ClickstreamConfiguration configuration,
                                    int bundleSequenceId)
        throws IOException {
        String appId = configuration.getAppId();
        String endpoint = configuration.getEndpoint();
        String curStr = eventJson;
        String compression = "";
        if (Boolean.TRUE.equals(configuration.isCompressEvents())) {
            LOG.debug("submitEvents isCompressEvents true");
            curStr = StringUtil.compressForGzip(eventJson);
            compression = "gzip";
        }
        if (null == curStr) {
            LOG.debug("submitEvents isCompressEvents false");
            curStr = eventJson;
        }

        RequestBody body = RequestBody.create(curStr, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder().url(endpoint).build();
        HttpUrl url = request.url().newBuilder()
            .addQueryParameter("platform", "Android")
            .addQueryParameter("appId", appId)
            .addQueryParameter("hashCode", StringUtil.getHashCode(curStr))
            .addQueryParameter("event_bundle_sequence_id", String.valueOf(bundleSequenceId))
            .addQueryParameter("upload_timestamp", String.valueOf(System.currentTimeMillis()))
            .addQueryParameter("compression", compression)
            .build();
        Request.Builder builder = request.newBuilder().url(url).post(body);
        if (!StringUtil.isNullOrEmpty(configuration.getAuthCookie())) {
            builder.addHeader("cookie", configuration.getAuthCookie());
        }
        request = builder.build();

        final OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder();
        okHttpClientBuilder.connectTimeout(HTTP_CONNECT_TIME_OUT, TimeUnit.SECONDS);
        okHttpClientBuilder.readTimeout(HTTP_READ_TIME_OUT, TimeUnit.SECONDS);
        okHttpClientBuilder.writeTimeout(HTTP_WRITE_TIME_OUT, TimeUnit.SECONDS);
        okHttpClientBuilder.callTimeout(configuration.getCallTimeOut(), TimeUnit.SECONDS);
        okHttpClientBuilder.retryOnConnectionFailure(true);
        okHttpClientBuilder.addNetworkInterceptor(UserAgentInterceptor.using(UserAgent::string));
        if (configuration.getDns() != null) {
            okHttpClientBuilder.dns(configuration.getDns());
        }

        OkHttpClient client = okHttpClientBuilder.build();
        LOG.debug(
            String.format(Locale.US, "Current %d conn and %d idle conn", client.connectionPool().connectionCount(),
                client.connectionPool().idleConnectionCount()));
        // make the sync request.
        return client.newCall(request).execute();
    }

}
