package com.wokoworks.chain.tron.common.codes.codes;

public class SubscribeCode {
    public enum SubscribeAddressCode {
        SAVE_ERROR("保存数据失败"),
        ;
        SubscribeAddressCode(String msg) {
        }
    }

    public enum UnSubscribeAddressCode {
        SAVE_ERROR("保存数据失败"),
        ADDRESS_NOT_EXIST("地址不存在"),
        ;
        UnSubscribeAddressCode(String msg) {
        }
    }
}
