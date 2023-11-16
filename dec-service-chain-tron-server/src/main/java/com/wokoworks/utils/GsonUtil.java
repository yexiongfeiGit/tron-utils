package com.wokoworks.utils;

import com.google.gson.Gson;

import java.lang.reflect.Type;

public class GsonUtil {
    private static Gson gson = new Gson();

    public static <T> T fromJson(String json, Type typeOfT){
        return gson.fromJson(json, typeOfT);
    }
}
