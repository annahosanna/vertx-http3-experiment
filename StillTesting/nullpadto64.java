package example;

import java.util.Arrays;

public class PaddingExample {

  public static byte[] padTo64Bytes(byte[] input) {
    if (input.length != 60) {
      throw new IllegalArgumentException("Input array must be 60 bytes");
    }

    byte[] paddedArray = new byte[64];
    System.arraycopy(input, 0, paddedArray, 0, input.length);
    // Last 4 bytes remain as 0 (null)
    return paddedArray;
  }

  public static byte[] unpadTo60Bytes(byte[] input) {
    if (input.length != 64) {
      throw new IllegalArgumentException("Input array must be 64 bytes");
    }

    return Arrays.copyOf(input, 60);
  }
}
