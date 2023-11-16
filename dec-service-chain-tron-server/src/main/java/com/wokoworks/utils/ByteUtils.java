package com.wokoworks.utils;

import org.tron.trident.utils.Base58Check;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

/**
 * @Author: 飞
 * @Date: 2021/11/26 11:13
 */
public class ByteUtils {


    public static byte[] trimPrefixZero(byte[] bytes) {
        int position = 0;
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == 0x00) {
                position = i + 1;
            } else {
                break;
            }
        }
        if (position > 0) {
            return Arrays.copyOfRange(bytes, position, bytes.length);
        }
        return bytes;
    }


    public static byte[] trimSuffixZero(byte[] bytes) {
        int position = 0;
        for (int i = bytes.length; i == 0; i--) {
            if (bytes[i] == 0x00) {
                position = i + 1;
            } else {
                break;
            }
        }
        if (position > 0) {
            return Arrays.copyOfRange(bytes, 0, position);
        }
        return bytes;
    }




    public static BigDecimal fromWei(BigDecimal number, int decimal) {
        return number.divide(BigDecimal.TEN.pow(decimal), 18, RoundingMode.DOWN);
    }


    public static String topicToAddress(byte[] bytes) {
        byte[] bytes1 = Arrays.copyOfRange(bytes, 11, bytes.length);
        bytes1[0] = 65;
        return Base58Check.bytesToBase58(bytes1);
    }



    public static byte[] byteMerger(byte[] bt1, byte[] bt2){
        byte[] bt3 = new byte[bt1.length+bt2.length];
        System.arraycopy(bt1, 0, bt3, 0, bt1.length);
        System.arraycopy(bt2, 0, bt3, bt1.length, bt2.length);
        return bt3;
    }


    public static void main(String[] args) {
        String str = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,32,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,75";
        String[] split = str.split(",");
        System.out.println(split.length);
    }


}
