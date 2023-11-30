package com.wokoworks.utils;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Lark Notification Utils
 *
 * @author Roylic
 * 2022/10/10
 */
@Slf4j
public class LarkNotificationUtils {


    private static final String WARN_TEMPLATE = "{\"msg_type\":\"interactive\",\"card\":{\"config\":{\"wide_screen_mode\":true,\"enable_forward\":true},\"elements\":[{\"tag\":\"hr\"},{\"tag\":\"div\",\"text\":{\"content\":\"**%s**\",\"tag\":\"lark_md\"}},{\"tag\":\"hr\"},{\"tag\":\"div\",\"fields\":[{\"is_short\":false,\"text\":{\"tag\":\"lark_md\",\"content\":\"**Server:** %s\"}},{\"is_short\":false,\"text\":{\"tag\":\"lark_md\",\"content\":\"**Description:** %s\"}},{\"is_short\":false,\"text\":{\"tag\":\"lark_md\",\"content\":\"**Date:** %s\"}}]}],\"header\":{\"template\":\"%s\",\"title\":{\"content\":\"%s\",\"tag\":\"plain_text\"}}}}";

    private static final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(10, TimeUnit.SECONDS)
            .build();


    /**
     * Lark robot notification calling
     *
     * @param larkRobotUrl robot url
     * @param noteTitle    note title
     * @param warnTitle    warn title
     * @param server       server name
     * @param error        error details
     * @param warningLevel warning level enum
     */
    public static void callingLarkApi(String larkRobotUrl, String noteTitle, String warnTitle, String server,
                                      String error, WarningLevel warningLevel,String date) {

        String formattedStr = String.format(WARN_TEMPLATE, warnTitle, server,error,date, warningLevel.color, noteTitle);
        Request requestPost = new Request.Builder()
                .url(larkRobotUrl)
                .post(RequestBody.create(MediaType.parse("application/json"), formattedStr))
                .build();

        log.debug("[WarningHandler] sending notification to Lark with error content:{}", error);
        Call call = okHttpClient.newCall(requestPost);
        try {
            call.execute();
        } catch (IOException e) {
            log.error("[WarningHandler] error on calling Lark warning api, with exception causing",
                    e.getCause());
        }
    }

    /**
     * Lark robot notification calling
     *
     * @param larkRobotUrl robot url
     * @param noteTitle    note title
     * @param warnTitle    warn title
     * @param server       server name
     * @param error        error details
     * @param warningLevel warning level enum
     */
    public static void callingLarkApi(String larkRobotUrl, String noteTitle, String warnTitle, String server,
                                      String error, WarningLevel warningLevel) {

        String formattedStr = String.format(WARN_TEMPLATE, warnTitle, server, error, warningLevel.color, noteTitle);
        Request requestPost = new Request.Builder()
                .url(larkRobotUrl)
                .post(RequestBody.create(MediaType.parse("application/json"), formattedStr))
                .build();

        log.debug("[WarningHandler] sending notification to Lark with error content:{}", error);
        Call call = okHttpClient.newCall(requestPost);
        try {
            call.execute();
        } catch (IOException e) {
            log.error("[WarningHandler] error on calling Lark warning api, with exception causing",
                    e.getCause());
        }
    }

    /**
     * Warning level-color enum
     */
    public enum WarningLevel {

        ERROR("red"),
        WARN("yellow"),
        DEBUG("gray"),

        NONE("default");

        public String color;

        WarningLevel(String color) {
            this.color = color;
        }

        public static WarningLevel findByName(String name) {
            Optional<WarningLevel> any = Arrays.stream(WarningLevel.values()).filter(curEnum -> curEnum.name().equalsIgnoreCase(name)).findAny();
            return any.orElse(NONE);
        }
    }

}
