package example;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicServerCodecBuilder;
import io.netty.incubator.codec.quic.QuicStreamChannel;

public class FortuneServer {

  private final int port;

  public FortuneServer(int port) {
    this.port = port;
  }

  public void start() throws Exception {
    SelfSignedCertificate cert = new SelfSignedCertificate();
    EventLoopGroup group = new NioEventLoopGroup();

    try {
      QuicServerCodecBuilder codecBuilder = new QuicServerCodecBuilder()
        .certificate(cert.certificate())
        .privateKey(cert.privateKey())
        .handler(new FortuneServerInitializer());

      ServerBootstrap b = new ServerBootstrap();
      b
        .group(group)
        .channel(codecBuilder.buildChannel())
        .childHandler(
          new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) {
              ch.pipeline().addLast(codecBuilder.build());
            }
          }
        )
        .childOption(ChannelOption.AUTO_READ, true);

      Channel ch = b.bind(port).sync().channel();
      ch.closeFuture().sync();
    } finally {
      group.shutdownGracefully();
    }
  }

  public static void main(String[] args) throws Exception {
    int port = 8080;
    new FortuneServer(port).start();
    DatabaseInitializer.initDb();
  }
}
