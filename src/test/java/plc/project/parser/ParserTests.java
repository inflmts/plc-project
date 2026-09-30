package plc.project.parser;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import plc.project.lexer.Lexer;
import plc.project.lexer.Token;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

final class ParserTests {

    @ParameterizedTest
    @MethodSource
    void testSource(String test, Object input, Object expected) {
        test("source", input, expected);
    }

    private static Stream<Arguments> testSource() {
        return Stream.of(
            Arguments.of("Empty Statement", ";",
                new ParseException("", Optional.of(new Token(Token.Type.OPERATOR, ";")))
            ),
            Arguments.of("Single",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "stmt"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new Ast.Source(List.of(
                    new Ast.Stmt.Expression(new Ast.Expr.Variable("stmt"))
                ))
            ),
            Arguments.of("Multiple",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "first"),
                    new Token(Token.Type.OPERATOR, ";"),
                    new Token(Token.Type.IDENTIFIER, "second"),
                    new Token(Token.Type.OPERATOR, ";"),
                    new Token(Token.Type.IDENTIFIER, "third"),
                    new Token(Token.Type.OPERATOR, ";")
                ),
                new Ast.Source(List.of(
                    new Ast.Stmt.Expression(new Ast.Expr.Variable("first")),
                    new Ast.Stmt.Expression(new Ast.Expr.Variable("second")),
                    new Ast.Stmt.Expression(new Ast.Expr.Variable("third"))
                ))
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testLetStmt(String test, Object input, Object expected) {
        test("stmt", input, expected);
    }

    private static Stream<Arguments> testLetStmt() {
        return Stream.of(
            Arguments.of("Declaration", "LET name;",
                new Ast.Stmt.Let("name", Optional.empty())
            ),
            Arguments.of("Initialization", "LET name = expr;",
                new Ast.Stmt.Let("name", Optional.of(new Ast.Expr.Variable("expr")))
            ),
            Arguments.of("Confusing Declaration", "LET LET;",
                new Ast.Stmt.Let("LET", Optional.empty())
            ),
            Arguments.of("Confusing Initialization", "LET LET = expr;",
                new Ast.Stmt.Let("LET", Optional.of(new Ast.Expr.Variable("expr")))
            ),
            Arguments.of("Missing Variable", "LET;",
                new ParseException("", Optional.of(new Token(Token.Type.OPERATOR, ";")))
            ),
            Arguments.of("Missing Value", "LET name = ;",
                new ParseException("", Optional.of(new Token(Token.Type.OPERATOR, ";")))
            ),
            Arguments.of("Missing Semicolon", "LET name = expr",
                new ParseException("", Optional.empty())
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDefStmt(String test, Object input, Object expected) {
        test("stmt", input, expected);
    }

    private static Stream<Arguments> testDefStmt() {
        return Stream.of(
            Arguments.of("Def",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "DEF"),
                    new Token(Token.Type.IDENTIFIER, "name"),
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.OPERATOR, ")"),
                    new Token(Token.Type.IDENTIFIER, "DO"),
                    new Token(Token.Type.IDENTIFIER, "END")
                ),
                new Ast.Stmt.Def("name", List.of(), List.of())
            ),
            Arguments.of("Parameter",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "DEF"),
                    new Token(Token.Type.IDENTIFIER, "name"),
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.IDENTIFIER, "parameter"),
                    new Token(Token.Type.OPERATOR, ")"),
                    new Token(Token.Type.IDENTIFIER, "DO"),
                    new Token(Token.Type.IDENTIFIER, "END")
                ),
                new Ast.Stmt.Def("name", List.of("parameter"), List.of())
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testIfStmt(String test, Object input, Object expected) {
        test("stmt", input, expected);
    }

    private static Stream<Arguments> testIfStmt() {
        return Stream.of(
            Arguments.of("If",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "IF"),
                    new Token(Token.Type.IDENTIFIER, "cond"),
                    new Token(Token.Type.IDENTIFIER, "DO"),
                    new Token(Token.Type.IDENTIFIER, "then"),
                    new Token(Token.Type.OPERATOR, ";"),
                    new Token(Token.Type.IDENTIFIER, "END")
                ),
                new Ast.Stmt.If(
                    new Ast.Expr.Variable("cond"),
                    List.of(new Ast.Stmt.Expression(new Ast.Expr.Variable("then"))),
                    List.of()
                )
            ),
            Arguments.of("Else", "IF cond DO then; ELSE else; END",
                new Ast.Stmt.If(
                    new Ast.Expr.Variable("cond"),
                    List.of(new Ast.Stmt.Expression(new Ast.Expr.Variable("then"))),
                    List.of(new Ast.Stmt.Expression(new Ast.Expr.Variable("else")))
                )
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testForStmt(String test, Object input, Object expected) {
        test("stmt", input, expected);
    }

    private static Stream<Arguments> testForStmt() {
        return Stream.of(
            Arguments.of("For",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "FOR"),
                    new Token(Token.Type.IDENTIFIER, "name"),
                    new Token(Token.Type.IDENTIFIER, "IN"),
                    new Token(Token.Type.IDENTIFIER, "expr"),
                    new Token(Token.Type.IDENTIFIER, "DO"),
                    new Token(Token.Type.IDENTIFIER, "stmt"),
                    new Token(Token.Type.OPERATOR, ";"),
                    new Token(Token.Type.IDENTIFIER, "END")
                ),
                new Ast.Stmt.For(
                    "name",
                    new Ast.Expr.Variable("expr"),
                    List.of(new Ast.Stmt.Expression(new Ast.Expr.Variable("stmt")))
                )
            ),
            Arguments.of("Missing In",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "FOR"),
                    new Token(Token.Type.IDENTIFIER, "name"),
                    new Token(Token.Type.IDENTIFIER, "expr"),
                    new Token(Token.Type.IDENTIFIER, "DO"),
                    new Token(Token.Type.IDENTIFIER, "stmt"),
                    new Token(Token.Type.OPERATOR, ";"),
                    new Token(Token.Type.IDENTIFIER, "END")
                ),
                new ParseException("", Optional.of(new Token(Token.Type.IDENTIFIER, "expr")))
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testReturnStmt(String test, Object input, Object expected) {
        test("stmt", input, expected);
    }

    private static Stream<Arguments> testReturnStmt() {
        return Stream.of(
            Arguments.of("Return", "RETURN;",
                new Ast.Stmt.Return(Optional.empty())
            ),
            Arguments.of("Return Expr", "RETURN expr;",
                new Ast.Stmt.Return(Optional.of(new Ast.Expr.Variable("expr")))
            ),
            Arguments.of("Return If", "RETURN IF cond;",
                new Ast.Stmt.If(
                    new Ast.Expr.Variable("cond"),
                    List.of(new Ast.Stmt.Return(Optional.empty())),
                    List.of()
                )
            ),
            Arguments.of("Return Expr If", "RETURN expr IF cond;",
                new Ast.Stmt.If(
                    new Ast.Expr.Variable("cond"),
                    List.of(new Ast.Stmt.Return(Optional.of(new Ast.Expr.Variable("expr")))),
                    List.of()
                )
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExpressionStmt(String test, Object input, Object expected) {
        test("stmt", input, expected);
    }

    private static Stream<Arguments> testExpressionStmt() {
        return Stream.of(
            Arguments.of("Variable", "variable;",
                new Ast.Stmt.Expression(new Ast.Expr.Variable("variable"))
            ),
            Arguments.of("Else", "ELSE;",
                new Ast.Stmt.Expression(new Ast.Expr.Variable("ELSE"))
            ),
            Arguments.of("Function", "function();",
                new Ast.Stmt.Expression(new Ast.Expr.Function("function", List.of()))
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAssignmentStmt(String test, Object input, Object expected) {
        test("stmt", input, expected);
    }

    private static Stream<Arguments> testAssignmentStmt() {
        return Stream.of(
            Arguments.of("Variable", "variable = value;",
                new Ast.Stmt.Assignment(
                    new Ast.Expr.Variable("variable"),
                    new Ast.Expr.Variable("value")
                )
            ),
            Arguments.of("Property", "object.property = value;",
                new Ast.Stmt.Assignment(
                    new Ast.Expr.Property(new Ast.Expr.Variable("object"), "property"),
                    new Ast.Expr.Variable("value")
                )
            ),
            Arguments.of("Nonsensical", "function() = value;",
                new Ast.Stmt.Assignment(
                    new Ast.Expr.Function("function", List.of()),
                    new Ast.Expr.Variable("value")
                )
            ),
            Arguments.of("Missing Value", "variable = ;",
                new ParseException("", Optional.of(new Token(Token.Type.OPERATOR, ";")))
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testLiteralExpr(String test, Object input, Object expected) {
        test("expr", input, expected);
    }

    private static Stream<Arguments> testLiteralExpr() {
        return Stream.of(
            Arguments.of("Nil", "NIL",
                new Ast.Expr.Literal(null)
            ),
            Arguments.of("True", "TRUE",
                new Ast.Expr.Literal(true)
            ),
            Arguments.of("False", "FALSE",
                new Ast.Expr.Literal(false)
            ),
            Arguments.of("Integer", "1",
                new Ast.Expr.Literal(new BigInteger("1"))
            ),
            Arguments.of("Positive Integer", "+1",
                new Ast.Expr.Literal(new BigInteger("1"))
            ),
            Arguments.of("Negative Integer", "-1",
                new Ast.Expr.Literal(new BigInteger("-1"))
            ),
            Arguments.of("Integer Exponent", "1e3",
                new Ast.Expr.Literal(new BigInteger("1000"))
            ),
            Arguments.of("Integer Positive Exponent", "1e+3",
                new Ast.Expr.Literal(new BigInteger("1000"))
            ),
            Arguments.of("Integer Negative Exponent", "1000000e-3",
                new Ast.Expr.Literal(new BigInteger("1000"))
            ),
            Arguments.of("Integer Invalid Exponent", "1e-3",
                new ParseException("", Optional.of(new Token(Token.Type.INTEGER, "1e-3")))
            ),
            Arguments.of("Decimal", "1.0",
                new Ast.Expr.Literal(new BigDecimal("1.0"))
            ),
            Arguments.of("Decimal Exponent", "1.0e3",
                new Ast.Expr.Literal(new BigDecimal("1.0e3"))
            ),
            Arguments.of("Decimal Positive Exponent", "1.0e+3",
                new Ast.Expr.Literal(new BigDecimal("1.0e3"))
            ),
            Arguments.of("Decimal Negative Exponent", "1.0e-3",
                new Ast.Expr.Literal(new BigDecimal("1.0e-3"))
            ),
            Arguments.of("Character", "'c'",
                new Ast.Expr.Literal('c')
            ),
            Arguments.of("Character Backspace Escape", "'\\b'",
                new Ast.Expr.Literal('\b')
            ),
            Arguments.of("Character Newline Escape", "'\\n'",
                new Ast.Expr.Literal('\n')
            ),
            Arguments.of("Character Backslash Escape", "'\\\\'",
                new Ast.Expr.Literal('\\')
            ),
            Arguments.of("Character Single Quote Escape", "'\\''",
                new Ast.Expr.Literal('\'')
            ),
            Arguments.of("Character Double Quote", "'\"'",
                new Ast.Expr.Literal('"')
            ),
            Arguments.of("Character Double Quote Escape", "'\\\"'",
                new Ast.Expr.Literal('"')
            ),
            Arguments.of("String", "\"string\"",
                new Ast.Expr.Literal("string")
            ),
            Arguments.of("String Single Quote", "\"one'two\\'three\"",
                new Ast.Expr.Literal("one'two'three")
            ),
            Arguments.of("String Double Quote Escape", "\"one\\\"two\"",
                new Ast.Expr.Literal("one\"two")
            ),
            Arguments.of("String Newline Escape", "\"Hello,\\nWorld!\"",
                new Ast.Expr.Literal("Hello,\nWorld!")
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGroupExpr(String test, Object input, Object expected) {
        test("expr", input, expected);
    }

    private static Stream<Arguments> testGroupExpr() {
        return Stream.of(
            Arguments.of("Group", "(expr)",
                new Ast.Expr.Group(new Ast.Expr.Variable("expr"))
            ),
            Arguments.of("Nested Group", "((expr))",
                new Ast.Expr.Group(new Ast.Expr.Group(new Ast.Expr.Variable("expr")))
            ),
            Arguments.of("Group Addition", "(a + b)",
                new Ast.Expr.Group(new Ast.Expr.Binary("+",
                    new Ast.Expr.Variable("a"),
                    new Ast.Expr.Variable("b")
                ))
            ),
            Arguments.of("Missing Expression", "()",
                new ParseException("", Optional.of(new Token(Token.Type.OPERATOR, ")")))
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testBinaryExpr(String test, Object input, Object expected) {
        test("expr", input, expected);
    }

    private static Stream<Arguments> testBinaryExpr() {
        return Stream.of(
            Arguments.of("Addition", "left + right",
                new Ast.Expr.Binary("+",
                    new Ast.Expr.Variable("left"),
                    new Ast.Expr.Variable("right")
                )
            ),
            Arguments.of("Addition Whitespace", "1+2",
                new ParseException("", Optional.of(new Token(Token.Type.INTEGER, "+2")))
            ),
            Arguments.of("Subtraction Whitespace", "1-2",
                new ParseException("", Optional.of(new Token(Token.Type.INTEGER, "-2")))
            ),
            Arguments.of("Multiplication", "left * right",
                new Ast.Expr.Binary("*",
                    new Ast.Expr.Variable("left"),
                    new Ast.Expr.Variable("right")
                )
            ),
            Arguments.of("Multiple Comparison", "1 == 2 < 3",
                new Ast.Expr.Binary("<",
                    new Ast.Expr.Binary("==",
                        new Ast.Expr.Literal(BigInteger.valueOf(1)),
                        new Ast.Expr.Literal(BigInteger.valueOf(2))
                    ),
                    new Ast.Expr.Literal(BigInteger.valueOf(3))
                )
            ),
            Arguments.of("Equal Precedence", "first + second + third",
                new Ast.Expr.Binary("+",
                    new Ast.Expr.Binary("+",
                        new Ast.Expr.Variable("first"),
                        new Ast.Expr.Variable("second")
                    ),
                    new Ast.Expr.Variable("third")
                )
            ),
            Arguments.of("Lower Precedence", "first + second * third",
                new Ast.Expr.Binary("+",
                    new Ast.Expr.Variable("first"),
                    new Ast.Expr.Binary("*",
                        new Ast.Expr.Variable("second"),
                        new Ast.Expr.Variable("third")
                    )
                )
            ),
            Arguments.of("Higher Precedence", "first * second + third",
                new Ast.Expr.Binary("+",
                    new Ast.Expr.Binary("*",
                        new Ast.Expr.Variable("first"),
                        new Ast.Expr.Variable("second")
                    ),
                    new Ast.Expr.Variable("third")
                )
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testVariableExpr(String test, Object input, Object expected) {
        test("expr", input, expected);
    }

    private static Stream<Arguments> testVariableExpr() {
        return Stream.of(
            Arguments.of("Nil", "nil",
                new Ast.Expr.Variable("nil")
            ),
            Arguments.of("True", "true",
                new Ast.Expr.Variable("true")
            ),
            Arguments.of("False", "false",
                new Ast.Expr.Variable("false")
            ),
            Arguments.of("Let", "LET",
                new Ast.Expr.Variable("LET")
            ),
            Arguments.of("Variable", "variable",
                new Ast.Expr.Variable("variable")
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testPropertyExpr(String test, Object input, Object expected) {
        test("expr", input, expected);
    }

    private static Stream<Arguments> testPropertyExpr() {
        return Stream.of(
            Arguments.of("Property", "one.two",
                new Ast.Expr.Property(
                    new Ast.Expr.Variable("one"),
                    "two"
                )
            ),
            Arguments.of("Property of Property", "one.two.three",
                new Ast.Expr.Property(
                    new Ast.Expr.Property(
                        new Ast.Expr.Variable("one"),
                        "two"
                    ),
                    "three"
                )
            ),
            Arguments.of("Property of Method", "one.two().three",
                new Ast.Expr.Property(
                    new Ast.Expr.Method(
                        new Ast.Expr.Variable("one"),
                        "two",
                        List.of()
                    ),
                    "three"
                )
            ),
            Arguments.of("Missing Name", "one.",
                new ParseException("", Optional.empty())
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testFunctionExpr(String test, Object input, Object expected) {
        test("expr", input, expected);
    }

    private static Stream<Arguments> testFunctionExpr() {
        return Stream.of(
            Arguments.of("No Arguments", "function()",
                new Ast.Expr.Function("function", List.of())
            ),
            Arguments.of("One Argument", "function(one)",
                new Ast.Expr.Function("function", List.of(
                    new Ast.Expr.Variable("one")
                ))
            ),
            Arguments.of("Two Arguments", "function(one, two)",
                new Ast.Expr.Function("function", List.of(
                    new Ast.Expr.Variable("one"),
                    new Ast.Expr.Variable("two")
                ))
            ),
            Arguments.of("Comma Only", "function(,)",
                new ParseException("", Optional.of(new Token(Token.Type.OPERATOR, ",")))
            ),
            Arguments.of("Missing Comma", "function(one two)",
                new ParseException("", Optional.of(new Token(Token.Type.IDENTIFIER, "two")))
            ),
            Arguments.of("Trailing Comma", "function(one,)",
                new ParseException("", Optional.of(new Token(Token.Type.OPERATOR, ")")))
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testMethodExpr(String test, Object input, Ast.Expr.Method expected) {
        test("expr", input, expected);
    }

    private static Stream<Arguments> testMethodExpr() {
        return Stream.of(
            Arguments.of("Method",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "receiver"),
                    new Token(Token.Type.OPERATOR, "."),
                    new Token(Token.Type.IDENTIFIER, "method"),
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.OPERATOR, ")")
                ),
                new Ast.Expr.Method(
                    new Ast.Expr.Variable("receiver"),
                    "method",
                    List.of()
                )
            ),
            Arguments.of("Method of Property", "one.two.three()",
                new Ast.Expr.Method(
                    new Ast.Expr.Property(
                        new Ast.Expr.Variable("one"),
                        "two"
                    ),
                    "three",
                    List.of()
                )
            ),
            Arguments.of("Method of Method", "one.two().three()",
                new Ast.Expr.Method(
                    new Ast.Expr.Method(
                        new Ast.Expr.Variable("one"),
                        "two",
                        List.of()
                    ),
                    "three",
                    List.of()
                )
            ),
            Arguments.of("Arguments",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "receiver"),
                    new Token(Token.Type.OPERATOR, "."),
                    new Token(Token.Type.IDENTIFIER, "method"),
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.IDENTIFIER, "first"),
                    new Token(Token.Type.OPERATOR, ","),
                    new Token(Token.Type.IDENTIFIER, "second"),
                    new Token(Token.Type.OPERATOR, ","),
                    new Token(Token.Type.IDENTIFIER, "third"),
                    new Token(Token.Type.OPERATOR, ")")
                ),
                new Ast.Expr.Method(
                    new Ast.Expr.Variable("receiver"),
                    "method",
                    List.of(
                        new Ast.Expr.Variable("first"),
                        new Ast.Expr.Variable("second"),
                        new Ast.Expr.Variable("third")
                    )
                )
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testObjectExpr(String test, Object input, Ast.Expr.ObjectExpr expected) {
        test("expr", input, expected);
    }

    private static Stream<Arguments> testObjectExpr() {
        return Stream.of(
            Arguments.of("Empty", "OBJECT DO END",
                new Ast.Expr.ObjectExpr(
                    Optional.empty(),
                    List.of(),
                    List.of()
                )
            ),
            Arguments.of("Empty Named", "OBJECT END DO END",
                new Ast.Expr.ObjectExpr(
                    Optional.of("END"),
                    List.of(),
                    List.of()
                )
            ),
            Arguments.of("Field",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "OBJECT"),
                    new Token(Token.Type.IDENTIFIER, "DO"),
                    new Token(Token.Type.IDENTIFIER, "LET"),
                    new Token(Token.Type.IDENTIFIER, "field"),
                    new Token(Token.Type.OPERATOR, ";"),
                    new Token(Token.Type.IDENTIFIER, "END")
                ),
                new Ast.Expr.ObjectExpr(
                    Optional.empty(),
                    List.of(new Ast.Stmt.Let("field", Optional.empty())),
                    List.of()
                )
            ),
            Arguments.of("Method",
                List.of(
                    new Token(Token.Type.IDENTIFIER, "OBJECT"),
                    new Token(Token.Type.IDENTIFIER, "DO"),
                    new Token(Token.Type.IDENTIFIER, "DEF"),
                    new Token(Token.Type.IDENTIFIER, "method"),
                    new Token(Token.Type.OPERATOR, "("),
                    new Token(Token.Type.OPERATOR, ")"),
                    new Token(Token.Type.IDENTIFIER, "DO"),
                    new Token(Token.Type.IDENTIFIER, "END"),
                    new Token(Token.Type.IDENTIFIER, "END")
                ),
                new Ast.Expr.ObjectExpr(
                    Optional.empty(),
                    List.of(),
                    List.of(new Ast.Stmt.Def("method", List.of(), List.of()))
                )
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testProgram(String test, Object input, Object expected) {
        test("source", input, expected);
    }

    public static Stream<Arguments> testProgram() {
        return Stream.of(
            Arguments.of("Distance",
                """
                DEF distance(a, b) DO
                    LET dx = b.x - a.x;
                    LET dy = b.y - a.y;
                    LET dz = b.z - a.z;
                    RETURN sqrt(dx * dx + dy * dy + dz * dz);
                END
                print(distance(OBJECT DO LET x = 0; LET y = -2; LET z = 14e0; END,
                               OBJECT DO LET x = 4; LET y = 3; LET z = 140e-1; END));
                """,
                new Ast.Source(List.of(
                    new Ast.Stmt.Def("distance", List.of("a", "b"), List.of(
                        new Ast.Stmt.Let("dx", Optional.of(new Ast.Expr.Binary("-",
                            new Ast.Expr.Property(new Ast.Expr.Variable("b"), "x"),
                            new Ast.Expr.Property(new Ast.Expr.Variable("a"), "x")
                        ))),
                        new Ast.Stmt.Let("dy", Optional.of(new Ast.Expr.Binary("-",
                            new Ast.Expr.Property(new Ast.Expr.Variable("b"), "y"),
                            new Ast.Expr.Property(new Ast.Expr.Variable("a"), "y")
                        ))),
                        new Ast.Stmt.Let("dz", Optional.of(new Ast.Expr.Binary("-",
                            new Ast.Expr.Property(new Ast.Expr.Variable("b"), "z"),
                            new Ast.Expr.Property(new Ast.Expr.Variable("a"), "z")
                        ))),
                        new Ast.Stmt.Return(Optional.of(new Ast.Expr.Function("sqrt", List.of(
                            new Ast.Expr.Binary("+",
                                new Ast.Expr.Binary("+",
                                    new Ast.Expr.Binary("*",
                                        new Ast.Expr.Variable("dx"),
                                        new Ast.Expr.Variable("dx")
                                    ),
                                    new Ast.Expr.Binary("*",
                                        new Ast.Expr.Variable("dy"),
                                        new Ast.Expr.Variable("dy")
                                    )
                                ),
                                new Ast.Expr.Binary("*",
                                    new Ast.Expr.Variable("dz"),
                                    new Ast.Expr.Variable("dz")
                                )
                            )
                        ))))
                    )),
                    new Ast.Stmt.Expression(new Ast.Expr.Function("print", List.of(
                        new Ast.Expr.Function("distance", List.of(
                            new Ast.Expr.ObjectExpr(
                                Optional.empty(),
                                List.of(
                                    new Ast.Stmt.Let("x", Optional.of(new Ast.Expr.Literal(BigInteger.valueOf(0)))),
                                    new Ast.Stmt.Let("y", Optional.of(new Ast.Expr.Literal(BigInteger.valueOf(-2)))),
                                    new Ast.Stmt.Let("z", Optional.of(new Ast.Expr.Literal(BigInteger.valueOf(14))))
                                ),
                                List.of()
                            ),
                            new Ast.Expr.ObjectExpr(
                                Optional.empty(),
                                List.of(
                                    new Ast.Stmt.Let("x", Optional.of(new Ast.Expr.Literal(BigInteger.valueOf(4)))),
                                    new Ast.Stmt.Let("y", Optional.of(new Ast.Expr.Literal(BigInteger.valueOf(3)))),
                                    new Ast.Stmt.Let("z", Optional.of(new Ast.Expr.Literal(BigInteger.valueOf(14))))
                                ),
                                List.of()
                            )
                        ))
                    )))
                ))
            ),
            Arguments.of("Hello World",
                """
                DEF main() DO
                    print("Hello, World!");
                END
                """,
                new Ast.Source(List.of(
                    new Ast.Stmt.Def("main", List.of(), List.of(
                        new Ast.Stmt.Expression(new Ast.Expr.Function(
                            "print",
                            List.of(new Ast.Expr.Literal("Hello, World!"))
                        ))
                    ))
                ))
            )
        );
    }

    interface ParserMethod<T extends Ast> {
        T invoke(Parser parser) throws ParseException;
    }

    private static void test(String rule, Object input, Object expected) {
        var tokens = switch (input) {
            case List<?> list -> (List<Token>) list;
            case String program -> Assertions.assertDoesNotThrow(() -> new Lexer(program).lex());
            default -> throw new AssertionError(input);
        };
        Parser parser = new Parser(tokens);
        switch (expected) {
            case Ast ast -> {
                var received = Assertions.assertDoesNotThrow(() -> parser.parse(rule));
                Assertions.assertEquals(ast, received);
            }
            case ParseException e -> {
                var received = Assertions.assertThrows(ParseException.class, () -> parser.parse(rule));
                Assertions.assertEquals(e.getToken(), received.getToken());
            }
            default -> throw new AssertionError(input);
        }
    }

}
