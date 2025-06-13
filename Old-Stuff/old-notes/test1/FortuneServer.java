package example;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.unix.ServerDomainSocketChannel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
// import io.netty.channel.SingleThreadEventLoop;
import io.netty.channel.DefaultEventLoop;
// import io.netty.channel.socket.nio.NioDatagramChannel;
// import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.util.
SelfSignedCertificate;
// import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicServerCodecBuilder;
import io.netty.incubator.codec.quic.*;
// import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.channel.*;
// import io.netty.handler.ssl.*;
// import io.netty.incubator.codec.http3.*;
// import java.security.*;
// import java.sql.*;
/// import java.util.concurrent.*;
// import javax.net.ssl.*;
// resourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED)
public class FortuneServer {

  private final int port;

  public FortuneServer(int port) {
    this.port = port;
  }

  public void start() throws Exception {
    // SelfSignedCertificate cert = new SelfSignedCertificate();
    // SingleThreadEventLoop group;
	  // in the netty-all jar which must be in class path
    EventLoopGroup group = new DefaultEventLoop();

    try {
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        QuicSslContext sslContext = QuicSslContextBuilder.forServer(ssc.privateKey(), null, ssc.certificate())
            .applicationProtocols("h3")
            .build();

      QuicServerCodecBuilder codecBuilder = new QuicServerCodecBuilder()
    		  .sslContext(sslContext)
    		  .handler(new FortuneServerInitializer());
   
// ServerChannel types: SctpServerChannel, ServerDomainSocketChannel, ServerSocketChannel, UdtServerChannel
      // EpollServerDomainSocketChannel, KQueueServerDomainSocketChannel
      ServerBootstrap b = new ServerBootstrap();
      b
        .group(group)
        .channel(ServerDomainSocketChannel.class)
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
