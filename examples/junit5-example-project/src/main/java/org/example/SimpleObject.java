package org.example;

public class SimpleObject {

    private final String name;
    private final int counter;

    public SimpleObject(String name, int counter) {
        this.name = name;
        this.counter = counter;
    }

    public int getCounter() {
        return counter;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "SimpleObject{" +
                "name='" + name + '\'' +
                ", counter=" + counter +
                '}';
    }
}
