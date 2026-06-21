package io.github.kd656.coveragex.fixtures;

public final class VirtualThread {

    private static int counter;

    public static void execute() throws InterruptedException {
        Thread t = Thread.startVirtualThread(() -> counter++);
        t.join();
    }
}
