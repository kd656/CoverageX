package org.example;

public class TestClass {

    private final String name;

    public TestClass(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getName(String name) {
        if (this.name.equalsIgnoreCase(name)) {
            return this.name;
        }

        return name;
    }

    public String someLogic2(String name, String name2) {
        return name.isBlank() ? name2 : name;
    }

    public boolean someLogic3(SimpleObject simpleObject) {
        return simpleObject != null;
    }

    public String someLogic(String name) {
        if (this.name.equalsIgnoreCase(name)) {
            throw new IllegalArgumentException("Some stuff");
        }

        if (name == null || name.isBlank()) {
            switch (name) {
                case null -> {
                    return "null";
                }
                default -> {
                    return "blank";
                }
            }
        }

        if (name.startsWith(".")) {
            return "dot";
        }

        return "new name";
    }

    public int getInt(int num) {
        return num;
    }

    public short getShort(short num) {
        return num;
    }

    public byte getByte(byte num) {
        return num;
    }

    public long getLong(long num) {
        return num;
    }

    public boolean getBoolean(boolean flag) {
        return flag;
    }

    public float getFloat(float num) {
        return num;
    }

    public double getDouble(double num) {
        return num;
    }
}
