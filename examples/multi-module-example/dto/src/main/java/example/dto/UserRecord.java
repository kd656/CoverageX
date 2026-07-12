package example.dto;

/**
 * Immutable user DTO. No tests in this module; coverage arrives via the service
 * module and gets routed here by the aggregator using ownership from the analyze
 * step's SemanticIndex.
 */
public record UserRecord(String name, int age) {

    public boolean isAdult() {
        return age >= 18;
    }

    public String greeting() {
        return "Hello, " + name;
    }
}
