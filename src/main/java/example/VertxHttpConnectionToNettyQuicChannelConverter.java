package example;
package io.netty.incubator.codec.http3.util;

import io.netty.incubator.codec.quic.QuicChannel;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.net.impl.ConnectionBase;
public class VertxHttpConnectionToNettyQuicChannelConverter {

    public static QuicChannel convert(HttpConnection connection) {
        if (!(connection instanceof HttpConnectionImpl)) {
            throw new IllegalArgumentException("Connection must be HttpConnectionImpl");
        }

        HttpConnectionImpl impl = (HttpConnectionImpl) connection;
        Channel channel = impl.channel();

        if (!(channel instanceof QuicChannel)) {
            throw new IllegalArgumentException("Underlying channel must be QuicChannel");
        }

        return (QuicChannel) channel;
    }

}
