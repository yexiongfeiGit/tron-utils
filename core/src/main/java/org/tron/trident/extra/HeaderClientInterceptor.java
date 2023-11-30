package org.tron.trident.extra;

import io.grpc.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Adding header on Grpc requests
 *
 * @author Roylic
 * 2023/7/7
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeaderClientInterceptor implements ClientInterceptor {

    private final Metadata.Key<String> NETWORK_HEADER_KEY = Metadata.Key.of("network", Metadata.ASCII_STRING_MARSHALLER);
    private final Metadata.Key<String> ACCESS_KEY_HEADER_KEY = Metadata.Key.of("accessKey", Metadata.ASCII_STRING_MARSHALLER);
    private String networkName;
    private String accessKey;

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                headers.put(ACCESS_KEY_HEADER_KEY, accessKey);
                headers.put(NETWORK_HEADER_KEY, networkName);
                super.start(responseListener, headers);
            }
        };
    }
}
