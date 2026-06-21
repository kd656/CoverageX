package io.github.kd656.coveragex.core.fixtures.exceptions;

import java.io.StringReader;
import java.io.IOException;

public class TryWithResources {
    public String read(String content) throws IOException {
        try (StringReader reader = new StringReader(content)) {
            int first = reader.read();
            return first == -1 ? "empty" : "has-content";
        }
    }
}
