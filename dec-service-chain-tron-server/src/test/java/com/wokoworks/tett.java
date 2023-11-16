package com.wokoworks;

import org.bouncycastle.util.encoders.Hex;
import org.tron.trident.abi.datatypes.Array;
import org.tron.trident.utils.Base58Check;

import java.util.Arrays;

/**
 * @Author: 飞
 * @Date: 2021/11/3 10:46
 */
public class tett {

//    10,2,-115,3,34,8,27,85,-83,-38,2,57,-6,79,64,-104,-100,-16,-118,-50,47,90,-16,1,8,31,18,-21,1,10,49,116,121,112,101,46,103,111,111,103,108,101,97,112,105,115,46,99,111,109,47,112,114,111,116,111,99,111,108,46,84,114,105,103,103,101,114,83,109,97,114,116,67,111,110,116,114,97,99,116,18,-75,1,10,21,65,9,-8,14,121,127,-26,-55,-95,109,-66,86,37,-37,-5,90,-126,100,-16,-17,-67,18,21,65,101,-93,-54,63,-20,-9,-17,-38,-7,26,-62,-109,125,-93,27,55,23,112,-28,82,34,-124,1,97,-119,-47,7,0,0,0,0,0,0,0,0,0,0,0,0,-102,5,91,44,-18,-40,-20,-70,-59,-59,69,-62,-75,26,-45,-35,57,117,74,-62,0,0,0,0,0,0,0,0,0,0,0,0,-20,31,83,-121,32,125,-60,-111,85,-70,-82,93,-93,-13,-18,73,-118,-3,-85,-33,112,120,47,116,114,97,110,115,102,101,114,47,99,104,97,110,110,101,108,45,48,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,76,83,-20,-36,24,-90,0,0,112,-117,-87,-37,-7,-51,47,-112,1,-128,-62,-41,47,
//            ---------------------------
//            ---------------2------------
//            97,-119,-47,7,0,0,0,0,0,0,0,0,0,0,0,0,-102,5,91,44,-18,-40,-20,-70,-59,-59,69,-62,-75,26,-45,-35,57,117,74,-62,0,0,0,0,0,0,0,0,0,0,0,0,-20,31,83,-121,32,125,-60,-111,85,-70,-82,93,-93,-13,-18,73,-118,-3,-85,-33,112,120,47,116,114,97,110,115,102,101,114,47,99,104,97,110,110,101,108,45,48,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,76,83,-20,-36,24,-90,0,0,
//            ----------------2-----------


    public static void main(String[] args) {

        String contract = "TQ1bUkdPkgxtJL2KYRo5MGPPUvufm6tmTW";
        byte[] contractBytes = Base58Check.base58ToBytes(contract);
        System.out.println();
        for (byte b : contractBytes) {
            System.out.print(b + ",");
        }
        System.out.println();

        String contract2 = "TKEdTmLSocskqBUFL2fBHSUixhZbo9RG55";
        byte[] contractBytes2 = Base58Check.base58ToBytes(contract2);
        System.out.println();
        for (byte b : contractBytes2) {
            System.out.print(b + ",");
        }
        System.out.println();



        String hash = "dbfc0e5ed8341359e7901206b54ebcf0fde0cc5e18280e58c35c88063031ca1c";
        byte[] hashBytes = Hex.decodeStrict(hash);
        for (byte hashByte : hashBytes) {
            System.out.print(hashByte+ ",");
        }
        System.out.println();


        String fromAddress = "TAsvF4D4g5szmUsr5joSbB3NZV132NnNQJ";

        byte[] fromAddressBytes = Base58Check.base58ToBytes(fromAddress);
        System.out.println();
        for (byte b : fromAddressBytes) {
            System.out.print(b + ",");
        }
        System.out.println();


        String toAddress = "TKEdTmLSocskqBUFL2fBHSUixhZbo9RG55";
        byte[] toAddressBytes = Base58Check.base58ToBytes(toAddress);
        System.out.println();
        for (byte b : toAddressBytes) {
            System.out.print(b + ",");
        }
        System.out.println();





        byte[] by = new byte[]{10,2,-115,3,34,8,27,85,-83,-38,2,57,-6,79,64,-104,-100,-16,-118,-50,47,90,-16,1,8,31,18,-21,1,10,49,116,121,112,101,46,103,111,111,103,108,101,97,112,105,115,46,99,111,109,47,112,114,111,116,111,99,111,108,46,84,114,105,103,103,101,114,83,109,97,114,116,67,111,110,116,114,97,99,116,18,-75,1,10,21,65,9,-8,14,121,127,-26,-55,-95,109,-66,86,37,-37,-5,90,-126,100,-16,-17,-67,18,21,65,101,-93,-54,63,-20,-9,-17,-38,-7,26,-62,-109,125,-93,27,55,23,112,-28,82,34,-124,1,97,-119,-47,7,0,0,0,0,0,0,0,0,0,0,0,0,-102,5,91,44,-18,-40,-20,-70,-59,-59,69,-62,-75,26,-45,-35,57,117,74,-62,0,0,0,0,0,0,0,0,0,0,0,0,-20,31,83,-121,32,125,-60,-111,85,-70,-82,93,-93,-13,-18,73,-118,-3,-85,-33,112,120,47,116,114,97,110,115,102,101,114,47,99,104,97,110,110,101,108,45,48,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,76,83,-20,-36,24,-90,0,0,112,-117,-87,-37,-7,-51,47,-112,1,-128,-62,-41,47};
        for (int i = 0; i < by.length; i++) {
            if (by[i] == -102) {
                System.out.println(i);
            }
        }

        byte[] bytes = Arrays.copyOfRange(by, 85, 106);
        System.out.println(Base58Check.bytesToBase58(bytes));
        System.out.println("TAsvF4D4g5szmUsr5joSbB3NZV132NnNQJ");

        byte[] bytes2 = Arrays.copyOfRange(by, 108, 129);

        System.out.println(Base58Check.bytesToBase58(bytes2));


        byte[] bytes3 = Arrays.copyOfRange(by, 147, 168);
        bytes3[0] = 65;

        System.out.println(Base58Check.bytesToBase58(bytes3));










    }



}
