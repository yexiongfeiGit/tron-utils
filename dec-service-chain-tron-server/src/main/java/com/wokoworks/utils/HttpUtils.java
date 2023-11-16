package com.wokoworks.utils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * @Author: 飞
 * @Date: 2021/12/2 16:54
 */
@Slf4j
public class HttpUtils {




    public static FileVo get(String url) {

        OkHttpClient client = new OkHttpClient().newBuilder().readTimeout(30, TimeUnit.SECONDS ).build();
        Request request = new Request.Builder()
                .url(url)
                .build();
        try {
            Response responses = client.newCall(request).execute();
            String contentType = responses.headers().get("Content-Type");
            return new FileVo(contentType, responses.body().string());
        } catch (IOException e) {
            log.warn("<==url:{}" , url);
            log.warn(e.getMessage(), e);
            return null;
        }
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class FileVo {
        private String contentType;
        private String content;
    }



    public static void main(String[] args) {
        FileVo fileVo = get("https://ipfs.io/ipfs/bafybeiezeds576kygarlq672cnjtimbsrspx5b3tr3gct2lhqud6abjgiu");
        System.out.println(fileVo.content);
        System.out.println(fileVo.contentType);
    }

}
