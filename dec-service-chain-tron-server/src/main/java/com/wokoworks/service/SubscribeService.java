package com.wokoworks.service;

import com.wokoworks.chain.tron.common.codes.codes.SubscribeCode;
import com.wokoworks.framework.data.ReturnValue;

import java.util.Set;

public interface SubscribeService {
    ReturnValue<Void, SubscribeCode.SubscribeAddressCode> subscribeAddress(String appNum, Set<String> addressSet);

    ReturnValue<Void, SubscribeCode.UnSubscribeAddressCode> unSubscribeAddress(String appNum, Set<String> addressSet);
}
