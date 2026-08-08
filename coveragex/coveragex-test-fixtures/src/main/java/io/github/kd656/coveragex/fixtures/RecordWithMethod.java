package io.github.kd656.coveragex.fixtures;

public record RecordWithMethod(int age) {

    public boolean adult() {
        return age >= 18;
    }

    public static void execute() {
        new RecordWithMethod(20).adult();
        new RecordWithMethod(10).adult();
    }
}
