package example;

import io.netty.buffer.ByteBuf;
// Import required crypto and netty classes
import io.netty.incubator.codec.quic.QuicTokenHandler;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SecureRandom;
import java.util.Arrays;
// import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

// dcid is up to 20 bytes
// token can be any length
// time is 8 bytes
// InetSocketAddress can be either IPv4, IPv6, or a hostname, also includes a port - probably a little to variable
// Hmac is 32 bytes
// Block size = 16
// AES((DCID + Time) + Padding)
// HMAC((DCID + Time) + Padding)
// Final result = AES + HMAC
//
// In reverse:
// Take of last 32 bytes for HMAC
// Decrypt AES
// Compare HMAC of decrypted value against HMAC from token
// Remove padding
// Remove 8 bytes of time
// Leaving the dcid which I don't really care about
public class QuicTokenEncryptionHandler implements QuicTokenHandler {

  private static final String AES_ALGORITHM = "AES";
  private static final String HMAC_ALGORITHM = "HmacSHA256";
  // AES needs a block size of 16
  private static final int BLOCK_SIZE = 16;

  // Once calculated these to remain constant
  // Such that if this in invoked later the values have not changed
  private final SecretKeySpec aesKey;
  private final SecretKeySpec hmacKey;

  // not sure if a different instance of this class is called anytime a new token is needed
  // lets assume its one time only and so the dcid value can't be saved for validation
  // private final byte[] dcid;

  private static SecretKeySpec generateSecretKey(String algorithm) {
    try {
      // Generate a random AES key
      KeyGenerator keyGenerator = KeyGenerator.getInstance(algorithm);

      // Initialize the KeyGenerator with the desired key size and a secure random source
      SecureRandom secureRandom = new SecureRandom();
      keyGenerator.init(256, secureRandom);

      // Generate the SecretKey
      SecretKey secretKey = keyGenerator.generateKey();
      // Encode the key to Base64
      // AES doesn't care
      // String encodedKey = Base64.getEncoder()
      //  .encodeToString(secretKey.getEncoded());
      return new SecretKeySpec(
        secretKey.getEncoded(),
        secretKey.getAlgorithm()
      );
    } catch (NoSuchAlgorithmException e) {
      // throw exception
    }
    return null;
  }

  public QuicTokenEncryptionHandler() {
    // Generate random keys
    System.out.println("Calling new QuicTokenEncryptionHandler");
    this.aesKey = generateSecretKey("AES");
    this.hmacKey = generateSecretKey("HmacSHA256");
  }

  private byte[] longArrayToByteArray(long[] longs) {
    // byte[longs.length()*2] a = null;
    int totalLength = 0;
    for (int i = 0; i < longs.length; i++) {
      totalLength = totalLength + longToBytes(longs[i]).length;
    }
    if (totalLength != (2 * longs.length)) {
      // This is bad
      // We should have two bytes per long
    }
    byte[] out = new byte[totalLength];
    int offset = 0;
    for (int i = 0; i < longs.length; i++) {
      byte[] tmpByteArray = longToBytes(longs[i]);
      System.arraycopy(tmpByteArray, 0, out, offset, tmpByteArray.length);
      offset = offset + tmpByteArray.length;
    }
    return out;
  }

  @Override
  public boolean writeToken(
    ByteBuf out,
    ByteBuf dcid,
    InetSocketAddress address
  ) {
    System.out.println("Calling writeToken");
    try {
      // Could concatinate the dcid with the current time. Then take the hmac of that. Then encrypt it and append the hmac
      // On the validation side, trime of the hmac, then decrypt it, take the hmac and confirm they are equal
      // The split off the time and compare it to the current time
      // If greater than a minute then session is to old
      // If hmac does not match then fail
      // This leaves the dcid, but I do not think I need that for anything
      // Encrypt DCID with AES
      Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, aesKey);

      SecureRandom sr = new SecureRandom();
      // I'm sure there is very nice way to use a LongStream to pull out 16 byte
      byte[] salt = longArrayToByteArray(sr.longs(8).toArray());
      byte[] byteDcid = dcid.array();
      long timestamp = System.currentTimeMillis();
      byte[] byteTimestamp = longToBytes(timestamp);
      // Add longBytes
      int offset = 0;
      byte[] byteArrayForConcat = new byte[salt.length +
      byteDcid.length +
      byteTimestamp.length];
      System.arraycopy(
        byteDcid,
        0,
        byteArrayForConcat,
        offset,
        byteDcid.length
      );
      offset = offset + byteDcid.length;
      System.arraycopy(
        byteTimestamp,
        0,
        byteArrayForConcat,
        offset,
        byteTimestamp.length
      );
      offset = offset + byteTimestamp.length;
      System.arraycopy(salt, 0, byteArrayForConcat, offset, salt.length);
      byte[] paddedBytes = addPKCS7Padding(byteArrayForConcat);
      // Encrypt variable length dcid + time + salt
      byte[] encryptedDcid = cipher.doFinal(paddedBytes);

      // Calculate HMAC
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(hmacKey);
      byte[] hmac = mac.doFinal(paddedBytes);

      // Combine encrypted DCID and HMAC
      byte[] token = new byte[encryptedDcid.length + hmac.length];
      System.arraycopy(encryptedDcid, 0, token, 0, encryptedDcid.length);
      System.arraycopy(hmac, 0, token, encryptedDcid.length, hmac.length);
      out.readBytes(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public int validateToken(ByteBuf token, InetSocketAddress address) {
    System.out.println("Calling validateToken");
    try {
      // byte[] tokenAsBytes = token.array();
      // out.writeBytes(byte[])
      // Split token into encrypted DCID and HMAC parts
      int hmacLength = 32; // SHA256 produces 32 byte HMAC
      byte[] receivedHmac = Arrays.copyOfRange(
        token.array(),
        token.array().length - hmacLength,
        token.array().length
      );

      byte[] receivedAes = Arrays.copyOfRange(
        token.array(),
        0,
        token.array().length - hmacLength
      );

      Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, aesKey);
      byte[] decryptedAes = cipher.doFinal(receivedAes);

      // Calculate time
      byte[] unpadded = removePKCS7Padding(decryptedAes);
      byte[] longBytes = Arrays.copyOfRange(
        unpadded,
        unpadded.length - 8,
        unpadded.length
      );
      long timestamp = bytesToLong(longBytes);
      long timeDifference = System.currentTimeMillis() - timestamp;
      // Calculate token experation
      if (timeDifference > 60000) {
        // Byte offset is not right fix it later
        // return -1;
      }

      // Validate HMAC
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(hmacKey);

      byte[] calculatedHmac = mac.doFinal(decryptedAes);

      if (!Arrays.equals(receivedHmac, calculatedHmac)) {
        return -1;
      }

      return 1;
    } catch (Exception e) {
      return -1;
    }
  }

  @Override
  public int maxTokenLength() {
    System.out.println("Calling maxTokenLength");
    // If you did not have a max then there could be a hash attack
    // AES encrypts in 16 byte blocks to calculate pad.
    // Random salt is 16 bytes
    // Time is 2 bytes
    // dcid max length is 20 bytes
    // hmac is 32 bytes
    // + pad to multiple of 16
    // So max length is 16 + 2 + 20 + 32  = 70 + pad = 80
    return 80;
  }

  private static byte[] addPKCS7Padding(byte[] data) {
    // Padding + length needs to be multiple of 16
    // Must have more than 0 bytes of padding
    int paddingLength = BLOCK_SIZE - (data.length % BLOCK_SIZE);

    if (((data.length + paddingLength) % BLOCK_SIZE) != 0) {
      throw new IllegalArgumentException("Invalid PKCS7 padding");
    }

    byte[] paddedData = Arrays.copyOf(data, data.length + paddingLength);
    for (int i = data.length; i < paddedData.length; i++) {
      paddedData[i] = (byte) paddingLength;
    }
    return paddedData;
  }

  private static byte[] removePKCS7Padding(byte[] paddedData) {
    // Calculate value of last byte
    int paddingLength = paddedData[paddedData.length - 1] & 0xFF;

    // Last byte must be at least 1. You cannot have a padding byte with 0 length
    if (paddingLength < 1 || paddingLength > BLOCK_SIZE) {
      throw new IllegalArgumentException("Invalid PKCS7 padding");
    }

    int newLength = paddedData.length - paddingLength;
    return Arrays.copyOf(paddedData, newLength);
  }

  private static byte[] longToBytes(long x) {
    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
    buffer.putLong(x);
    return buffer.array();
  }

  private static long bytesToLong(byte[] bytes) {
    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
    buffer.put(bytes);
    buffer.flip(); //need flip
    return buffer.getLong();
  }
}
