packagee example;

import io.netty.incubator.codec.quic.QuicTokenHandler;

import java.net.InetSocketAddress;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.SocketAddress;

public class TimestampTokenHandler implements QuicTokenHandler {

    @Override
    public boolean writeToken(ByteBuf out, ByteBuf dcid, SocketAddress address) {
        long timestamp = System.currentTimeMillis();
        out.writeLong(timestamp);
        // This would cause a variable length
        //out.writeBytes(dcid);
        return true;
    }

    @Override
    public boolean validateToken(ByteBuf token, ByteBuf dcid, SocketAddress address) {
        if (token.readableBytes() < 8) {
            return false;
        }
        long timestamp = token.readLong();
        return (System.currentTimeMillis() - timestamp) < 60000; // Valid for 1 minute
    }

    @Override
    public int validateToken(ByteBuf token, InetSocketAddress address) {
        if (token.readableBytes() < 8) {
            return 0;
        }
        long timestamp = token.readLong();
        return (System.currentTimeMillis() - timestamp) < 60000 ? 1 :0; // Valid for 1 minute
    }
    
    
    @Override
    public int maxTokenLength() {
        return 8;
    }


	@Override
	   public boolean writeToken(ByteBuf out, ByteBuf dcid, InetSocketAddress address) {
        long timestamp = System.currentTimeMillis();
        out.writeLong(timestamp);
        // This would cause a variable length
        //out.writeBytes(dcid);
        return true;
	}


}
