package com.wokoworks.utils;

/**
 * @Author: 飞
 * @Date: 2021/12/14 16:51
 */
public enum FileType {


    /**/
    BASE64(-2, "base64"),
    JSON(-1, "json"),
    IMAGE(0, "图片"),
    VIDEO(1, "视频"),
    DATA_IMAGE(2, "BASE_64_IMAGE"),
    MP3(3, "mp3"),
    JIF(4, "JIF"),

    ;

    public final int value;
    public final String remark;

    FileType(int value, String remark) {
        this.value = value;
        this.remark = remark;
    }

    public static FileType valueOf(int value) {
        for (FileType chainType : values()) {
            if (chainType.value == value) {
                return chainType;
            }
        }
        return null;
    }

    public static void main(String[] args) {
        for (FileType value : FileType.values()) {
            System.out.print(value.value + ":" +value.remark + ",");
        }
    }
}
