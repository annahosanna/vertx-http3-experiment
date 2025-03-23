package example;

import io.netty.buffer.ByteBuf;
import io.netty.incubator.codec.quic.QuicTokenHandler;
import java.net.InetSocketAddress;

public class TimestampTokenHandler implements QuicTokenHandler {

  @Override
  public int validateToken(ByteBuf token, InetSocketAddress address) {
    // Validate the token
    // Return the offset
    if (token.readableBytes() < 8) {
      return 0;
    }
    long timestamp = token.readLong();
    return (System.currentTimeMillis() - timestamp) < 60000 ? 1 : -1; // Valid for 1 minute
  }

  @Override
  public int maxTokenLength() {
    return 8;
  }

  @Override
  public boolean writeToken(
    ByteBuf out,
    ByteBuf dcid,
    InetSocketAddress address
  ) {
    // Generate a new token based on connection id and address
    // Return false if no token should be generated (and thus no validation)
    // This should use cryptography.
    // Maybe generate a token encrypted with a private key
    long timestamp = System.currentTimeMillis();
    out.writeLong(timestamp);
    // This would cause a variable length
    //out.writeBytes(dcid);
    return true;
  }
}
