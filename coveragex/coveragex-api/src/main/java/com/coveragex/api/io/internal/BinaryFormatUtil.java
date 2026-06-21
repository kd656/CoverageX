package com.coveragex.api.io.internal;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

final class BinaryFormatUtil {

    private BinaryFormatUtil() {}

    static void writeNullableString(DataOutputStream out, String value) throws IOException {
        out.writeBoolean(value == null);
        out.writeUTF(value == null ? "" : value);
    }

    static String readNullableString(DataInputStream in) throws IOException {
        boolean isNull = in.readBoolean();
        String value   = in.readUTF();
        return isNull ? null : value;
    }
}
