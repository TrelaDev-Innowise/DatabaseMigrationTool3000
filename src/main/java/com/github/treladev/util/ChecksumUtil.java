package com.github.treladev.util;

import java.util.zip.CRC32;

public class ChecksumUtil {

    public static long calculateChecksum(String content){
        CRC32 crc32 = new CRC32();
        crc32.update(content.getBytes());
        return crc32.getValue();
    }
    private ChecksumUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

}
