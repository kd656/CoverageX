package com.coveragex.fixtures;

public final class ReturnPrim {

    public static int    asInt   ()  { return 42; }         // line 6
    public static long   asLong  ()  { return 42L; }        // line 7
    public static double asDouble()  { return 3.14; }       // line 8

    public static void execute() {
        asInt();
        asLong();
        asDouble();
    }
}
