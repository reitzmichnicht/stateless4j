package com.github.oxo42.stateless4j.helpers;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class InputStreamHelper {

    public static byte[] read(InputStream stream) {
        try {
            ByteArrayOutputStream res = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len = stream.read(buffer);
            while (len > 0) {
                res.write(buffer, 0, len);
                len = stream.read(buffer);
            }
            return res.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String readAsString(InputStream stream) {
        return new String(read(stream), StandardCharsets.UTF_8);
    }

}
