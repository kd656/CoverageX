package io.github.kd656.coveragex.fixtures;

public final class PatternSealedSwitch {

    public sealed interface Animal permits Dog, Cat {}
    public record Dog(String name) implements Animal {}
    public record Cat(String name) implements Animal {}

    public static String sound(Animal a) {
        return switch (a) {
            case Dog d -> "woof";
            case Cat c -> "meow";
        };
    }

    public static void execute() {
        sound(new Dog("rex"));
        sound(new Cat("whiskers"));
    }
}
