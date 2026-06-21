package com.coveragex.api.context;

public final class StandardContextKeys {

    public static final ContextKey<String> TEST_CLASS =
        ContextKey.of("test.class", String.class);

    public static final ContextKey<String> TEST_METHOD =
        ContextKey.of("test.method", String.class);

    public static final ContextKey<String> TEST_DISPLAY_NAME =
        ContextKey.of("test.display-name", String.class);

    private StandardContextKeys() {}
}
