package example;

import java.util.Arrays;

// This should check weather the byte array is an even multiple of 16
// If it is a multiple of 16 then add 16 more bytes of padding

public class PKCS7Padding {

  private static final int BLOCK_SIZE = 16;

  public static byte[] addPadding(byte[] data) {
    // Padding + length needs to be multiple of 16
    // Must have more than 0 bytes of padding
    int paddingLength = BLOCK_SIZE - (data.length % BLOCK_SIZE);
    if ((data.length + paddingLength) % BLOCK_SIZE) != 0 {
      throw new IllegalArgumentException("Invalid PKCS7 padding");
    }

    byte[] paddedData = Arrays.copyOf(data, data.length + paddingLength);
    for (int i = data.length; i < paddedData.length; i++) {
      paddedData[i] = (byte) paddingLength;
    }
    return paddedData;
  }

  public static byte[] removePadding(byte[] paddedData) {
    // Calculate value of last byte
    int paddingLength = paddedData[paddedData.length - 1] & 0xFF;

    // Last byte must be at least 1. You can have a padding byte with 0 length
    if (paddingLength < 1 || paddingLength > BLOCK_SIZE) {
      throw new IllegalArgumentException("Invalid PKCS7 padding");
    }

    int newLength = paddedData.length - paddingLength;
    return Arrays.copyOf(paddedData, newLength);
  }

  public static void main(String[] args) {
    byte[] data = "This is a test".getBytes();
    System.out.println("Original data: " + Arrays.toString(data));

    byte[] paddedData = addPadding(data);
    System.out.println("Padded data: " + Arrays.toString(paddedData));

    byte[] unpaddedData = removePadding(paddedData);
    System.out.println("Unpadded data: " + Arrays.toString(unpaddedData));
    System.out.println("Unpadded data String: " + new String(unpaddedData));
  }
}
