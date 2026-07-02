package example.dto;

/**
 * Immutable user DTO. No tests in this module — coverage arrives via the service
 * module and gets routed here by the aggregator using ownership from the analyze
 * step's SemanticIndex.
 *
 * <p>Not declared as a Java {@code record} because the current
 * {@code SourceCodeAnalyzer} does not yet parse record declarations, which
 * would leave this module's {@code coveragex.map.json} empty and defeat
 * the ownership demonstration.</p>
 */
public final class UserRecord {

    private final String name;
    private final int age;

    public UserRecord(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String name() { return name; }

    public int age() { return age; }

    public boolean isAdult() {
        return age >= 18;
    }

    public String greeting() {
        return "Hello, " + name;
    }
}
