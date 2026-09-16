package plc.project.lexer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

public final class LexerTests {

    @ParameterizedTest
    @MethodSource
    void testWhitespace(String test, String input, boolean equals) {
        test(input, List.of(), equals);
    }

    public static Stream<Arguments> testWhitespace() {
        return Stream.of(
            Arguments.of("Tab", "\t", true),
            Arguments.of("Backspace", "\b", true),
            Arguments.of("Carriage Return", "\r", true),
            Arguments.of("Form Feed", "\f", false),

            Arguments.of("Space", " ", true),
            Arguments.of("Newline", "\n", true),
            Arguments.of("Multiple", "    \n    ", true)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testComment(String test, String input, boolean equals) {
        test(input, List.of(), equals);
    }

    public static Stream<Arguments> testComment() {
        return Stream.of(
            Arguments.of("Multiple", "//1\n//2\r\n", true),
            Arguments.of("Spaces", "// one two", true),

            Arguments.of("Empty", "//", true),
            Arguments.of("Text", "//comment", true)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testIdentifier(String test, String input, boolean equals) {
        test(input, List.of(new Token(Token.Type.IDENTIFIER, input)), equals);
    }

    public static Stream<Arguments> testIdentifier() {
        return Stream.of(
            Arguments.of("Single Lowercase", "x", true),
            Arguments.of("Single Uppercase", "X", true),
            Arguments.of("Single Underscore", "_", true),
            Arguments.of("Single Hyphen", "-", false),
            Arguments.of("Leading Lowercase", "one", true),
            Arguments.of("Leading Uppercase", "One", true),
            Arguments.of("Leading Underscore", "_one", true),
            Arguments.of("Middle Underscore", "one_two", true),
            Arguments.of("Middle Double Underscore", "one__two", true),
            Arguments.of("Middle Hyphen", "one-two", true),
            Arguments.of("Middle Double Hyphen", "one--two", true),

            Arguments.of("Alphabetic", "getName", true),
            Arguments.of("Alphanumeric", "thelegend27", true),
            Arguments.of("Leading Hyphen", "-five", false),
            Arguments.of("Leading Digit", "1fish2fish", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testInteger(String test, String input, boolean equals) {
        test(input, List.of(new Token(Token.Type.INTEGER, input)), equals);
    }

    public static Stream<Arguments> testInteger() {
        return Stream.of(
            Arguments.of("Exponent One Digit", "1e5", true),
            Arguments.of("Exponent Positive One Digit", "1e+5", true),
            Arguments.of("Exponent Positive Two Digits", "1e+10", true),
            Arguments.of("Exponent Negative One Digit", "1e-5", true),
            Arguments.of("Exponent Negative Two Digits", "1e-10", true),
            Arguments.of("Positive One Digit", "+6", true),
            Arguments.of("Positive Two Digits", "+67", true),
            Arguments.of("Positive With Exponent", "+67e10", true),
            Arguments.of("Positive With Exponent Positive", "+67e+10", true),
            Arguments.of("Positive With Exponent Negative", "+67e-10", true),
            Arguments.of("Negative One Digit", "-6", true),
            Arguments.of("Negative Two Digits", "-67", true),
            Arguments.of("Negative With Exponent", "-67e10", true),
            Arguments.of("Negative With Exponent Positive", "-67e+10", true),
            Arguments.of("Negative With Exponent Negative", "-67e-10", true),

            Arguments.of("Single Digit", "1", true),
            Arguments.of("Multiple Digits", "123", true),
            Arguments.of("Exponent", "1e10", true),
            Arguments.of("Missing Exponent Digits", "1e", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDecimal(String test, String input, boolean equals) {
        test(input, List.of(new Token(Token.Type.DECIMAL, input)), equals);
    }

    public static Stream<Arguments> testDecimal() {
        return Stream.of(
            Arguments.of("Exponent One Digit", "1.0e5", true),
            Arguments.of("Exponent Positive One Digit", "1.0e+5", true),
            Arguments.of("Exponent Positive Two Digits", "1.0e+10", true),
            Arguments.of("Exponent Negative One Digit", "1.0e-5", true),
            Arguments.of("Exponent Negative Two Digits", "1.0e-10", true),
            Arguments.of("Positive One Digit", "+6.0", true),
            Arguments.of("Positive Two Digits", "+6.07", true),
            Arguments.of("Positive With Exponent", "+6.07e10", true),
            Arguments.of("Positive With Exponent Positive", "+6.07e+10", true),
            Arguments.of("Positive With Exponent Negative", "+6.07e-10", true),
            Arguments.of("Negative One Digit", "-6.0", true),
            Arguments.of("Negative Two Digits", "-6.07", true),
            Arguments.of("Negative With Exponent", "-6.07e10", true),
            Arguments.of("Negative With Exponent Positive", "-6.07e+10", true),
            Arguments.of("Negative With Exponent Negative", "-6.07e-10", true),

            Arguments.of("Exponent Decimal", "1.0e1.0", false),
            Arguments.of("Missing Decimal Part Exponent", "1.e10", false),
            Arguments.of("Missing Exponent", "1.0e", false),

            Arguments.of("Decimal", "1.0", true),
            Arguments.of("Multiple Digits", "123.456", true),
            Arguments.of("Exponent", "1.0e10", true),
            Arguments.of("Trailing Decimal", "1.", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testCharacter(String test, String input, boolean equals) {
        test(input, List.of(new Token(Token.Type.CHARACTER, input)), equals);
    }

    public static Stream<Arguments> testCharacter() {
        return Stream.of(
            Arguments.of("Digit", "'5'", true),
            Arguments.of("Backslash", "'\\\\'", true),
            Arguments.of("Single Quote", "'\\''", true),
            Arguments.of("Double Quote", "'\"'", true),
            Arguments.of("Double Quote Escape", "'\\\"'", true),
            Arguments.of("Backspace", "'\b'", true),
            Arguments.of("Backspace Escape", "'\b'", true),
            Arguments.of("Tab", "'\t'", true),
            Arguments.of("Tab Escape", "'\\t'", true),

            Arguments.of("Alphabetic", "\'c\'", true),
            Arguments.of("Newline Escape", "\'\\n\'", true),
            Arguments.of("Unterminated", "\'u", false),
            Arguments.of("Multiple", "\'abc\'", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testString(String test, String input, boolean equals) {
        test(input, List.of(new Token(Token.Type.STRING, input)), equals);
    }

    public static Stream<Arguments> testString() {
        return Stream.of(
            Arguments.of("Carriage Return Newline", "\"\\r\\n\"", true),
            Arguments.of("Not Comment", "\"//\"", true),

            Arguments.of("Empty", "\"\"", true),
            Arguments.of("Alphabetic", "\"string\"", true),
            Arguments.of("Newline Escape", "\"Hello,\\nWorld\"", true),
            Arguments.of("Invalid Escape", "\"invalid\\escape\"", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testOperator(String test, String input, boolean equals) {
        test(input, List.of(new Token(Token.Type.OPERATOR, input)), equals);
    }

    public static Stream<Arguments> testOperator() {
        return Stream.of(
            Arguments.of("Plus Sign", "+", true),
            Arguments.of("Minus Sign", "-", true),
            Arguments.of("Exclamation", "!", true),
            Arguments.of("Slash", "/", true),
            Arguments.of("Backslash", "\\", true),
            Arguments.of("Equal", "=", true),
            Arguments.of("Double Equal", "==", true),
            Arguments.of("Not Equal", "!=", true),
            Arguments.of("Greater Equal", ">=", true),
            Arguments.of("Slash Equal", "/=", false),

            Arguments.of("Character", "(", true),
            Arguments.of("Comparison", "<=", true),
            Arguments.of("Whitespace", " ", false),
            Arguments.of("Double Quote", "\"", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testInteraction(String test, String input, List<Token> expected) {
        test(input, expected, true);
    }

    public static Stream<Arguments> testInteraction() {
        return Stream.of(
            Arguments.of("Integer Fractional Exponent", "1e1.0", List.of(
                new Token(Token.Type.INTEGER, "1e1"),
                new Token(Token.Type.OPERATOR, "."),
                new Token(Token.Type.INTEGER, "0")
            )),
            Arguments.of("Integer Uppercase Exponent", "1E10", List.of(
                new Token(Token.Type.INTEGER, "1"),
                new Token(Token.Type.IDENTIFIER, "E10")
            )),
            Arguments.of("Middle Comment", "first\n//test\nsecond", List.of(
                new Token(Token.Type.IDENTIFIER, "first"),
                new Token(Token.Type.IDENTIFIER, "second")
            )),
            Arguments.of("Operator Soup", "!'='0.!!=/ /", List.of(
                new Token(Token.Type.OPERATOR, "!"),
                new Token(Token.Type.CHARACTER, "'='"),
                new Token(Token.Type.INTEGER, "0"),
                new Token(Token.Type.OPERATOR, "."),
                new Token(Token.Type.OPERATOR, "!"),
                new Token(Token.Type.OPERATOR, "!="),
                new Token(Token.Type.OPERATOR, "/"),
                new Token(Token.Type.OPERATOR, "/")
            )),
            Arguments.of("Not Subtraction", "1-2", List.of(
                new Token(Token.Type.INTEGER, "1"),
                new Token(Token.Type.INTEGER, "-2")
            )),
            Arguments.of("Double Double Quotes", "\"\"doublestring\"\"", List.of(
                new Token(Token.Type.STRING, "\"\""),
                new Token(Token.Type.IDENTIFIER, "doublestring"),
                new Token(Token.Type.STRING, "\"\"")
            )),

            Arguments.of("Whitespace", "first second", List.of(
                new Token(Token.Type.IDENTIFIER, "first"),
                new Token(Token.Type.IDENTIFIER, "second")
            )),
            Arguments.of("Identifier Leading Hyphen", "-five", List.of(
                new Token(Token.Type.OPERATOR, "-"),
                new Token(Token.Type.IDENTIFIER, "five")
            )),
            Arguments.of("Identifier Leading Digit", "1fish2fish", List.of(
                new Token(Token.Type.INTEGER, "1"),
                new Token(Token.Type.IDENTIFIER, "fish2fish")
            )),
            Arguments.of("Integer Missing Exponent Digits", "1e", List.of(
                new Token(Token.Type.INTEGER, "1"),
                new Token(Token.Type.IDENTIFIER, "e")
            )),
            Arguments.of("Decimal Missing Decimal Digits", "1.", List.of(
                new Token(Token.Type.INTEGER, "1"),
                new Token(Token.Type.OPERATOR, ".")
            )),
            Arguments.of("Operator Multiple Operators", "<=>", List.of(
                new Token(Token.Type.OPERATOR, "<="),
                new Token(Token.Type.OPERATOR, ">")
            ))
        );
    }

    @ParameterizedTest
    @MethodSource
    void testException(String test, String input, int index) {
        var e = Assertions.assertThrows(LexException.class, () -> new Lexer(input).lex());
        Assertions.assertEquals(index, e.getIndex());
    }

    public static Stream<Arguments> testException() {
        return Stream.of(
            Arguments.of("Character Empty", "''", 1),
            Arguments.of("Character Empty Unterminated", "'", 1),
            Arguments.of("Character Single Quote Unterminated", "'\\'", 3),
            Arguments.of("Character Newline", "'\n'", 1),
            Arguments.of("Character Carriage Return", "'\r'", 1),
            Arguments.of("Character Invalid Escape", "'\\e'", 2),
            Arguments.of("String Unterminated", "\"one", 4),
            Arguments.of("String Unterminated Trailing Backslash", "\"one\\", 5),
            Arguments.of("String Middle Newline", "\"one\ntwo\"", 4),
            Arguments.of("String Middle Carriage Return", "\"one\rtwo\"", 4),

            Arguments.of("Character Unterminated", "\'u", 2),
            Arguments.of("Character Multiple", "\'abc\'", 2),
            Arguments.of("String Invalid Escape", "\"invalid\\escape\"", 9)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testProgram(String test, String input, List<Token> expected) {
        test(input, expected, true);
    }

    public static Stream<Arguments> testProgram() {
        return Stream.of(
            Arguments.of("Variable", "LET x = 5;", List.of(
                new Token(Token.Type.IDENTIFIER, "LET"),
                new Token(Token.Type.IDENTIFIER, "x"),
                new Token(Token.Type.OPERATOR, "="),
                new Token(Token.Type.INTEGER, "5"),
                new Token(Token.Type.OPERATOR, ";")
            )),
            Arguments.of("Print Function", "print(\"Hello, World!\");", List.of(
                new Token(Token.Type.IDENTIFIER, "print"),
                new Token(Token.Type.OPERATOR, "("),
                new Token(Token.Type.STRING, "\"Hello, World!\""),
                new Token(Token.Type.OPERATOR, ")"),
                new Token(Token.Type.OPERATOR, ";")
            ))
        );
    }

    private static void test(String input, List<Token> expected, boolean equals) {
        if (equals) {
            //Expect the result to exactly match expected.
            var tokens = Assertions.assertDoesNotThrow(() -> new Lexer(input).lex());
            Assertions.assertEquals(expected, tokens);
        } else {
            //Expect either an exception or different result, used for tests
            //verifying input is not lexed as a specific token type.
            try {
                Assertions.assertNotEquals(expected, new Lexer(input).lex());
            } catch (LexException ignored) {}
        }
    }

}
