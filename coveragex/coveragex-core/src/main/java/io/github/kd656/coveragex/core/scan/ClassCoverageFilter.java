package io.github.kd656.coveragex.core.scan;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Applies include/exclude policy to class files discovered during instrumentation or enrichment.
 *
 * <p>Patterns are compiled once at construction time because this filter is called for
 * every discovered class. The supported syntax intentionally mirrors the existing
 * CoverageX glob style: {@code *} matches one package segment and {@code **} crosses
 * package boundaries.</p>
 */
public final class ClassCoverageFilter {

    private static final List<String> DEFAULT_EXCLUDES = List.of(
            "java.**",
            "javax.**",
            "jdk.**",
            "sun.**",
            "com.sun.**",
            "io.github.kd656.coveragex.**"
    );

    private final List<ClassNamePattern> includes;
    private final List<ClassNamePattern> excludes;
    private final boolean explicitIncludes;
    private final boolean excludeZeroProbeClasses;

    public ClassCoverageFilter(List<String> includes, List<String> excludes) {
        this(includes, excludes, true);
    }

    public ClassCoverageFilter(List<String> includes, List<String> excludes, boolean excludeZeroProbeClasses) {
        this.explicitIncludes = includes != null && !includes.isEmpty();
        this.includes = compilePatterns(explicitIncludes ? includes : List.of("**"));

        List<String> effectiveExcludes = new ArrayList<>(excludes != null ? excludes : List.of());
        if (!explicitIncludes) {
            effectiveExcludes.addAll(DEFAULT_EXCLUDES);
        }
        this.excludes = compilePatterns(effectiveExcludes);
        this.excludeZeroProbeClasses = excludeZeroProbeClasses;
    }

    public boolean shouldInclude(String internalClassName, Path classFile, ClassOrigin origin) {
        if (internalClassName == null || internalClassName.isBlank()) {
            return false;
        }
        if (origin == ClassOrigin.TEST_OUTPUT || looksLikeTestOutput(classFile)) {
            return false;
        }

        String dottedName = internalClassName.replace('/', '.');
        if (isPackageOrModuleInfo(dottedName)) {
            return false;
        }

        if (!explicitIncludes && isAnonymousOrSyntheticName(dottedName)) {
            return false;
        }

        if (matchesAny(excludes, dottedName)) {
            return false;
        }
        return matchesAny(includes, dottedName);
    }

    public boolean excludeZeroProbeClasses() {
        return excludeZeroProbeClasses;
    }

    private static List<ClassNamePattern> compilePatterns(List<String> patterns) {
        return patterns.stream()
                .filter(pattern -> pattern != null && !pattern.isBlank())
                .map(ClassNamePattern::compile)
                .toList();
    }

    private static boolean matchesAny(List<ClassNamePattern> patterns, String className) {
        for (ClassNamePattern pattern : patterns) {
            if (pattern.matches(className)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPackageOrModuleInfo(String dottedName) {
        return dottedName.endsWith(".module-info") || dottedName.equals("module-info")
                || dottedName.endsWith(".package-info") || dottedName.equals("package-info");
    }

    private boolean looksLikeTestOutput(Path classFile) {
        if (classFile == null) {
            return false;
        }
        String normalized = classFile.toString().replace('\\', '/');
        return normalized.contains("/target/test-classes/")
                || normalized.endsWith("/target/test-classes")
                || normalized.contains("/build/classes/java/test/")
                || normalized.contains("/build/classes/kotlin/test/")
                || normalized.contains("/build/resources/test/");
    }

    private boolean isAnonymousOrSyntheticName(String dottedName) {
        return dottedName.matches(".*\\$\\d+(\\..*)?$")
                || dottedName.contains("$$")
                || dottedName.contains("$Mockito")
                || dottedName.contains("$ByteBuddy");
    }

    private record ClassNamePattern(Pattern pattern) {

        private static ClassNamePattern compile(String glob) {
            StringBuilder regex = new StringBuilder();
            for (int i = 0; i < glob.length(); i++) {
                char ch = glob.charAt(i);
                if (ch == '*') {
                    boolean doubleStar = i + 1 < glob.length() && glob.charAt(i + 1) == '*';
                    regex.append(doubleStar ? ".*" : "[^.]*");
                    if (doubleStar) {
                        i++;
                    }
                } else {
                    // Quote each literal character so '$', '+', '[' and other regex tokens stay literal.
                    regex.append(Pattern.quote(String.valueOf(ch)));
                }
            }
            return new ClassNamePattern(Pattern.compile(regex.toString()));
        }

        private boolean matches(String className) {
            return pattern.matcher(className).matches();
        }
    }
}
