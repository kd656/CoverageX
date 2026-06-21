package org.example;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class TestClassTest {

    @Nested
    @DisplayName("Constructor + getName()")
    class ConstructorAndGetName {

        @Test
        void shouldCreateTestClassAndReturnName() {
            TestClass testClass = new TestClass("test");

            assertEquals("test", testClass.getName());
        }

        @Test
        void shouldAllowEmptyName() {
            TestClass testClass = new TestClass("");

            assertEquals("", testClass.getName());
        }

        @Test
        void shouldAllowNullNameAndReturnNullFromGetName() {
            TestClass testClass = new TestClass(null);

            assertNull(testClass.getName());
        }
    }

    @Nested
    @DisplayName("someLogic2(String arg, String arg2)")
    class SomeLogic2 {

        @Test
        void nameShouldBeBlank() {
            TestClass testClass = new TestClass("someLogic");

            assertEquals("test", testClass.someLogic2("", "test"));
        }
    }

    @Nested
    @DisplayName("someLogic3(SimpleObject object)")
    class SomeLogic3 {

        @Test
        void objectNotNull() {
            TestClass testClass = new TestClass("someLogic");

            assertTrue(testClass.someLogic3(new SimpleObject("simpleObjectName", 999999)));
        }
    }

    @Nested
    @DisplayName("someLogic(String arg)")
    class SomeLogic {

        @Test
        void shouldReturnThisNameWhenEqualsIgnoreCase() {
            TestClass testClass = new TestClass("someLogic");

            assertThrows(IllegalArgumentException.class, () -> testClass.someLogic(testClass.getName()));
        }

        @Test
        void nameShouldBeBlank() {
            TestClass testClass = new TestClass("someLogic");

            assertEquals("blank", testClass.someLogic(""));
        }

//        @Test
//        void nameShouldBeNull() {
//            TestClass testClass = new TestClass("someLogic");
//
//            assertEquals("null", testClass.someLogic(null));
//        }

        @Test
        void nameShouldBeDifferent() {
            TestClass testClass = new TestClass("someLogic");

            assertEquals("new name", testClass.someLogic("test"));
        }
//
//        @Test
//        void nameShouldBeDot() {
//            TestClass testClass = new TestClass("someLogic");
//
//            assertEquals("dot", testClass.someLogic(".test"));
//        }
    }

    @Nested
    @DisplayName("getName(String arg)")
    class GetNameWithArgument {

        @Test
        void shouldReturnThisNameWhenEqualsIgnoreCase() {
            TestClass testClass = new TestClass("TeSt");

            assertEquals("TeSt", testClass.getName("test"));
            assertEquals("TeSt", testClass.getName("TEST"));
            assertEquals("TeSt", testClass.getName("tEsT"));
        }

        @Test
        void shouldReturnArgumentWhenNotEqualsIgnoreCase() {
            TestClass testClass = new TestClass("test");

            String arg = "test2";
            assertEquals(arg, testClass.getName(arg));
        }

        @Test
        void shouldReturnNullWhenArgumentIsNullAndNameIsNotNull() {
            TestClass testClass = new TestClass("test");

            assertNull(testClass.getName(null));
        }

        @Test
        void shouldThrowNpeWhenThisNameIsNull() {
            TestClass testClass = new TestClass(null);

            assertThrows(NullPointerException.class, () -> testClass.getName("test"));
        }
    }

    @Nested
    @DisplayName("Primitives - simple return value methods")
    class PrimitiveMethods {

        @ParameterizedTest(name = "getInt({0}) returns {0}")
        @ValueSource(ints = {0, 1, -1, Integer.MIN_VALUE, Integer.MAX_VALUE, 42})
        void shouldReturnSameInt(int value) {
            TestClass testClass = new TestClass("test");

            assertEquals(value, testClass.getInt(value));
        }

        @ParameterizedTest(name = "getShort({0}) returns {0}")
        @MethodSource("shorts")
        void shouldReturnSameShort(short value) {
            TestClass testClass = new TestClass("test");

            assertEquals(value, testClass.getShort(value));
        }

        static Stream<Short> shorts() {
            return Stream.of((short) 0, (short) 1, (short) -1, Short.MIN_VALUE, Short.MAX_VALUE, (short) 123);
        }

        @ParameterizedTest(name = "getByte({0}) returns {0}")
        @MethodSource("bytes")
        void shouldReturnSameByte(byte value) {
            TestClass testClass = new TestClass("test");

            assertEquals(value, testClass.getByte(value));
        }

        static Stream<Byte> bytes() {
            return Stream.of((byte) 0, (byte) 1, (byte) -1, Byte.MIN_VALUE, Byte.MAX_VALUE, (byte) 100);
        }

        @ParameterizedTest(name = "getLong({0}) returns {0}")
        @ValueSource(longs = {0L, 1L, -1L, Long.MIN_VALUE, Long.MAX_VALUE, 42L})
        void shouldReturnSameLong(long value) {
            TestClass testClass = new TestClass("test");

            assertEquals(value, testClass.getLong(value));
        }

        @ParameterizedTest(name = "getBoolean({0}) returns {0}")
        @ValueSource(booleans = {true, false})
        void shouldReturnSameBoolean(boolean value) {
            TestClass testClass = new TestClass("test");

            assertEquals(value, testClass.getBoolean(value));
        }

        @ParameterizedTest(name = "getFloat({0}) returns {0}")
        @MethodSource("floats")
        void shouldReturnSameFloat(float value) {
            TestClass testClass = new TestClass("test");

            assertEquals(value, testClass.getFloat(value), 0.0f);
        }

        static Stream<Float> floats() {
            return Stream.of(
                    0.0f, -0.0f, 1.0f, -1.0f,
                    Float.MIN_VALUE, Float.MAX_VALUE,
                    Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
                    42.5f
            );
        }

        @ParameterizedTest(name = "getDouble({0}) returns {0}")
        @MethodSource("doubles")
        void shouldReturnSameDouble(double value) {
            TestClass testClass = new TestClass("test");

            if (Double.isNaN(value)) {
                assertTrue(Double.isNaN(testClass.getDouble(value)));
            } else {
                assertEquals(value, testClass.getDouble(value), 0.0d);
            }
        }

        static Stream<Double> doubles() {
            return Stream.of(
                    0.0d, -0.0d, 1.0d, -1.0d,
                    Double.MIN_VALUE, Double.MAX_VALUE,
                    Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                    42.5d
            );
        }
    }
}
