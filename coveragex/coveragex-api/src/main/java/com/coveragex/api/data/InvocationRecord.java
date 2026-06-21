package com.coveragex.api.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record InvocationRecord(List<String> args, int count) {
    public InvocationRecord {
        args = Collections.unmodifiableList(new ArrayList<>(args));
    }
}
