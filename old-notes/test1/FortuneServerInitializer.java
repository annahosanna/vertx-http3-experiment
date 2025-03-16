package example;

import io.netty.channel.ChannelInitializer;
import io.netty.incubator.codec.quic.QuicStreamChannel;

public class FortuneServerInitializer
  extends ChannelInitializer<QuicStreamChannel> {

  @Override
  protected void initChannel(QuicStreamChannel ch) {
    ch.pipeline().addLast(new FortuneHandler());
  }
}
