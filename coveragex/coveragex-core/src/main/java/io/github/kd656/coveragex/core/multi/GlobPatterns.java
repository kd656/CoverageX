package io.github.kd656.coveragex.core.multi;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Build-neutral glob matching for module/subproject exclusion patterns, where
 * {@code *} matches any run of characters and every other character is literal.
 * Shared by the Maven and Gradle exclusion paths so the semantics stay identical.
 */
public final class GlobPatterns {

    private GlobPatterns() {}

    /** Compiles a glob into a {@link Pattern} that matches the whole input. */
    public static Pattern compile(String glob) {
        return Pattern.compile(toRegex(glob));
    }

    /** True if {@code value} matches any non-blank glob in {@code globs}. */
    public static boolean matchesAny(String value, List<String> globs) {
        if (globs == null) {
            return false;
        }
        for (String glob : globs) {
            if (glob == null || glob.isBlank()) {
                continue;
            }
            if (compile(glob).matcher(value).matches()) {
                return true;
            }
        }
        return false;
    }

    static String toRegex(String glob) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            if (c == '*') {
                sb.append(".*");
            } else {
                sb.append(Pattern.quote(String.valueOf(c)));
            }
        }
        return sb.toString();
    }
}
