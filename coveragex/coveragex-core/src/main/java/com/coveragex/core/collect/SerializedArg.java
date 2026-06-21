package com.coveragex.core.collect;

public record SerializedArg(boolean isNull, String value) {

    public static SerializedArg ofNull() {
        return new SerializedArg(true, "null");
    }

    public static SerializedArg of(String value) {
        return new SerializedArg(false, value);
    }
}
