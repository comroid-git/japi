package org.comroid.api;

public final class MacAddress implements Named {
    private final byte[] bytes;

    public MacAddress(String parse) {
        this(parseBytes(parse));
    }

    public MacAddress(byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    public String getName() {
        return toString();
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (byte b : bytes) {
            str.append(Integer.toHexString(b))
                    .append(':');
        }
        return str.substring(0, str.length() - 1);
    }

    private static byte[] parseBytes(String parse) {
        String[] split = parse.split(":");
        byte[] bytes = new byte[6];
        if (split.length != bytes.length)
            throw new IllegalArgumentException("Invalid MAC-Address: " + parse);
        for (int i = 0; i < split.length; i++)
            bytes[i] = (byte) Integer.parseInt(split[i], 16);
        return bytes;
    }
}
