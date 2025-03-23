import io.netty.buffer.ByteBuf;
import io.netty.incubator.codec.quic.QuicTokenHandler;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;

public class CustomQuicTokenHandler implements QuicTokenHandler {

    private static final String ALGORITHM = "AES";
    private final SecretKey privateKey;

    public CustomQuicTokenHandler() {
        // Generate a random 256-bit key
        byte[] keyBytes = new byte[32];
        new SecureRandom().nextBytes(keyBytes);
        this.privateKey = new SecretKeySpec(keyBytes, ALGORITHM);
    }

    @Override
    public void writeToken(ByteBuf out, ByteBuf dcid, InetSocketAddress address) {
        try {
            // Initialize cipher for encryption
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, privateKey);

            // Encrypt the output
            byte[] outBytes = new byte[out.readableBytes()];
            out.getBytes(0, outBytes);
            byte[] encrypted = cipher.doFinal(outBytes);

            // Write encrypted data back to ByteBuf
            out.clear();
            out.writeBytes(encrypted);

        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt token", e);
        }
    }

    @Override
    public int validateToken(ByteBuf token, ByteBuf expectedDcid, InetSocketAddress address) {
        try {
            // Initialize cipher for decryption
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            // Decrypt the token
            byte[] tokenBytes = new byte[token.readableBytes()];
            token.getBytes(0, tokenBytes);
            byte[] decrypted = cipher.doFinal(tokenBytes);

            // Validate decrypted content
            byte[] expectedBytes = new byte[expectedDcid.readableBytes()];
            expectedDcid.getBytes(0, expectedBytes);

            if (Arrays.equals(decrypted, expectedBytes)) {
                return decrypted.length;
            }
            return -1;

        } catch (Exception e) {
            return -1;
        }
    }
}