package plc.project.parser;

import com.google.common.base.Preconditions;
import plc.project.lexer.Token;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * This style of parser is called <em>recursive descent</em>. Each rule in our
 * grammar has dedicated function, and references to other rules correspond to
 * calling that function. Recursive rules are therefore supported by actual
 * recursive calls, while operator precedence is encoded via the grammar.
 *
 * <p>The parser has a similar architecture to the lexer, just with
 * {@link Token}s instead of characters. As before, {@link TokenStream#peek} and
 * {@link TokenStream#match} help with traversing the token stream. Instead of
 * emitting tokens, you will instead need to extract the literal value via
 * {@link TokenStream#get} to be added to the relevant AST.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    public Ast parse(String rule) throws ParseException {
        var ast = switch (rule) {
            case "source" -> parseSource();
            case "stmt" -> parseStmt();
            case "expr" -> parseExpr();
            default -> throw new AssertionError(rule);
        };
        if (tokens.has(0)) {
            throw new ParseException("Expected end of input.", tokens.getNext());
        }
        return ast;
    }

    private Ast.Source parseSource() throws ParseException {
        var statements = new ArrayList<Ast.Stmt>();
        while (tokens.has(0)) {
            statements.add(parseStmt());
        }
        return new Ast.Source(statements);
    }

    private Ast.Stmt parseStmt() throws ParseException {
        if (tokens.peek("LET"))
            return parseLetStmt();
        if (tokens.peek("DEF"))
            return parseDefStmt();
        if (tokens.peek("IF"))
            return parseIfStmt();
        if (tokens.peek("FOR"))
            return parseForStmt();
        if (tokens.peek("RETURN"))
            return parseReturnStmt();
        return parseExpressionOrAssignmentStmt();
    }

    private Ast.Stmt parseLetStmt() throws ParseException {
        Preconditions.checkState(tokens.match("LET"));
        if (!tokens.match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected variable name", tokens.getNext());
        }
        var name = tokens.get(-1).literal();
        Optional<Ast.Expr> value = tokens.match("=") ? Optional.of(parseExpr()) : Optional.empty();
        if (!tokens.match(";")) {
            throw new ParseException("Expected ';'", tokens.getNext());
        }
        return new Ast.Stmt.Let(name, value);
    }

    private Ast.Stmt parseDefStmt() throws ParseException {
        Preconditions.checkState(tokens.match("DEF"));
        if (!tokens.match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected function name", tokens.getNext());
        }
        var name = tokens.get(-1).literal();
        var parameters = new ArrayList<String>();
        var body = new ArrayList<Ast.Stmt>();
        if (!tokens.match("(")) {
            throw new ParseException("Expected '('", tokens.getNext());
        }
        if (tokens.match(Token.Type.IDENTIFIER)) {
            parameters.add(tokens.get(-1).literal());
            while (tokens.match(",")) {
                if (!tokens.match(Token.Type.IDENTIFIER)) {
                    throw new ParseException("Expected parameter name after ','", tokens.getNext());
                }
                parameters.add(tokens.get(-1).literal());
            }
        }
        if (!tokens.match(")", "DO")) {
            throw new ParseException("Expected ') DO'", tokens.getNext());
        }
        while (!tokens.match("END")) {
            body.add(parseStmt());
        }
        return new Ast.Stmt.Def(name, parameters, body);
    }

    private Ast.Stmt parseIfStmt() throws ParseException {
        Preconditions.checkState(tokens.match("IF"));
        var condition = parseExpr();
        var thenBody = new ArrayList<Ast.Stmt>();
        var elseBody = new ArrayList<Ast.Stmt>();
        if (!tokens.match("DO")) {
            throw new ParseException("Expected 'DO'", tokens.getNext());
        }
        while (!tokens.match("END")) {
            if (tokens.match("ELSE")) {
                while (!tokens.match("END")) {
                    elseBody.add(parseStmt());
                }
                break;
            }
            thenBody.add(parseStmt());
        }
        return new Ast.Stmt.If(condition, thenBody, elseBody);
    }

    private Ast.Stmt parseForStmt() throws ParseException {
        Preconditions.checkState(tokens.match("FOR"));
        if (!tokens.match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected loop variable name", tokens.getNext());
        }
        var name = tokens.get(-1).literal();
        if (!tokens.match("IN")) {
            throw new ParseException("Expected 'IN'", tokens.getNext());
        }
        var expr = parseExpr();
        if (!tokens.match("DO")) {
            throw new ParseException("Expected 'DO'", tokens.getNext());
        }
        var body = new ArrayList<Ast.Stmt>();
        while (!tokens.match("END")) {
            body.add(parseStmt());
        }
        return new Ast.Stmt.For(name, expr, body);
    }

    private Ast.Stmt parseReturnStmt() throws ParseException {
        Preconditions.checkState(tokens.match("RETURN"));
        Optional<Ast.Expr> value = !tokens.peek("IF") && !tokens.peek(";")
            ? Optional.of(parseExpr())
            : Optional.empty();
        Ast.Stmt statement = new Ast.Stmt.Return(value);
        if (tokens.match("IF")) {
            var condition = parseExpr();
            statement = new Ast.Stmt.If(condition, List.of(statement), List.of());
        }
        if (!tokens.match(";")) {
            throw new ParseException("Expected ';'", tokens.getNext());
        }
        return statement;
    }

    private Ast.Stmt parseExpressionOrAssignmentStmt() throws ParseException {
        var expr = parseExpr();
        Ast.Stmt statement = tokens.match("=")
            ? new Ast.Stmt.Assignment(expr, parseExpr())
            : new Ast.Stmt.Expression(expr);
        if (!tokens.match(";")) {
            throw new ParseException("Expected ';'", tokens.getNext());
        }
        return statement;
    }

    private Ast.Expr parseExpr() throws ParseException {
        return parseLogicalExpr();
    }

    private Ast.Expr parseLogicalExpr() throws ParseException {
        var expr = parseComparisonExpr();
        while (tokens.match("AND") || tokens.match("OR")) {
            var operator = tokens.get(-1).literal();
            var right = parseComparisonExpr();
            expr = new Ast.Expr.Binary(operator, expr, right);
        }
        return expr;
    }

    private Ast.Expr parseComparisonExpr() throws ParseException {
        var expr = parseAdditiveExpr();
        while (tokens.match("<") ||
               tokens.match("<=") ||
               tokens.match(">") ||
               tokens.match(">=") ||
               tokens.match("==") ||
               tokens.match("!="))
        {
            var operator = tokens.get(-1).literal();
            var right = parseAdditiveExpr();
            expr = new Ast.Expr.Binary(operator, expr, right);
        }
        return expr;
    }

    private Ast.Expr parseAdditiveExpr() throws ParseException {
        var expr = parseMultiplicativeExpr();
        while (tokens.match("+") || tokens.match("-")) {
            var operator = tokens.get(-1).literal();
            var right = parseMultiplicativeExpr();
            expr = new Ast.Expr.Binary(operator, expr, right);
        }
        return expr;
    }

    private Ast.Expr parseMultiplicativeExpr() throws ParseException {
        var expr = parseSecondaryExpr();
        while (tokens.match("*") || tokens.match("/")) {
            var operator = tokens.get(-1).literal();
            var right = parseSecondaryExpr();
            expr = new Ast.Expr.Binary(operator, expr, right);
        }
        return expr;
    }

    private Ast.Expr parseSecondaryExpr() throws ParseException {
        var expr = parsePrimaryExpr();
        while (tokens.peek(".")) {
            expr = parsePropertyOrMethod(expr);
        }
        return expr;
    }

    private Ast.Expr parsePropertyOrMethod(Ast.Expr receiver) throws ParseException {
        Preconditions.checkState(tokens.match("."));
        if (!tokens.match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected property or method name", tokens.getNext());
        }
        var name = tokens.get(-1).literal();
        if (!tokens.match("(")) {
            return new Ast.Expr.Property(receiver, name);
        }
        var arguments = new ArrayList<Ast.Expr>();
        if (!tokens.match(")")) {
            arguments.add(parseExpr());
            while (!tokens.match(")")) {
                if (!tokens.match(",")) {
                    throw new ParseException("Expected ',' or ')'", tokens.getNext());
                }
                arguments.add(parseExpr());
            }
        }
        return new Ast.Expr.Method(receiver, name, arguments);
    }

    private Ast.Expr parsePrimaryExpr() throws ParseException {
        if (tokens.peek("NIL") ||
            tokens.peek("TRUE") ||
            tokens.peek("FALSE") ||
            tokens.peek(Token.Type.INTEGER) ||
            tokens.peek(Token.Type.DECIMAL) ||
            tokens.peek(Token.Type.CHARACTER) ||
            tokens.peek(Token.Type.STRING))
            return parseLiteralExpr();
        if (tokens.peek("("))
            return parseGroupExpr();
        if (tokens.peek("OBJECT"))
            return parseObjectExpr();
        if (tokens.peek(Token.Type.IDENTIFIER))
            return parseVariableOrFunctionExpr();
        throw new ParseException("Expected primary expression", tokens.getNext());
    }

    private Ast.Expr parseLiteralExpr() throws ParseException {
        if (tokens.match("NIL")) {
            return new Ast.Expr.Literal(null);
        }
        if (tokens.match("TRUE")) {
            return new Ast.Expr.Literal(true);
        }
        if (tokens.match("FALSE")) {
            return new Ast.Expr.Literal(false);
        }
        if (tokens.match(Token.Type.INTEGER)) {
            var token = tokens.get(-1);
            var literal = token.literal();
            var decimal = new BigDecimal(literal);
            try {
                decimal = decimal.setScale(0);
            } catch (ArithmeticException e) {
                throw new ParseException("Exponent creates non-integer", Optional.of(token));
            }
            var integer = decimal.unscaledValue();
            return new Ast.Expr.Literal(integer);
        }
        if (tokens.match(Token.Type.DECIMAL)) {
            var literal = tokens.get(-1).literal();
            var decimal = new BigDecimal(literal);
            return new Ast.Expr.Literal(decimal);
        }
        if (tokens.match(Token.Type.CHARACTER)) {
            var literal = tokens.get(-1).literal();
            var string = literal.substring(1, literal.length() - 1).translateEscapes();
            Preconditions.checkState(string.length() == 1);
            return new Ast.Expr.Literal(string.charAt(0));
        }
        if (tokens.match(Token.Type.STRING)) {
            var literal = tokens.get(-1).literal();
            var string = literal.substring(1, literal.length() - 1).translateEscapes();
            return new Ast.Expr.Literal(string);
        }
        throw new IllegalStateException("Not a literal expression");
    }

    private Ast.Expr parseGroupExpr() throws ParseException {
        Preconditions.checkState(tokens.match("("));
        var expr = parseExpr();
        if (!tokens.match(")")) {
            throw new ParseException("Missing ')'", tokens.getNext());
        }
        return new Ast.Expr.Group(expr);
    }

    private Ast.Expr parseObjectExpr() throws ParseException {
        Preconditions.checkState(tokens.match("OBJECT"));
        Optional<String> name = !tokens.peek("DO") && tokens.match(Token.Type.IDENTIFIER)
            ? Optional.of(tokens.get(-1).literal())
            : Optional.empty();
        if (!tokens.match("DO")) {
            throw new ParseException("Expected 'DO'", tokens.getNext());
        }
        var fields = new ArrayList<Ast.Stmt.Let>();
        var methods = new ArrayList<Ast.Stmt.Def>();
        while (!tokens.match("END")) {
            if (tokens.peek("LET")) {
                fields.add((Ast.Stmt.Let) parseLetStmt());
            } else if (tokens.peek("DEF")) {
                methods.add((Ast.Stmt.Def) parseDefStmt());
            } else {
                throw new ParseException("Expected 'LET', 'DEF', or 'END'", tokens.getNext());
            }
        }
        return new Ast.Expr.ObjectExpr(name, fields, methods);
    }

    private Ast.Expr parseVariableOrFunctionExpr() throws ParseException {
        Preconditions.checkState(tokens.match(Token.Type.IDENTIFIER));
        var name = tokens.get(-1).literal();
        if (!tokens.match("(")) {
            return new Ast.Expr.Variable(name);
        }
        var arguments = new ArrayList<Ast.Expr>();
        if (!tokens.match(")")) {
            arguments.add(parseExpr());
            while (!tokens.match(")")) {
                if (!tokens.match(",")) {
                    throw new ParseException("Expected ',' or ')'", tokens.getNext());
                }
                arguments.add(parseExpr());
            }
        }
        return new Ast.Expr.Function(name, arguments);
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at (index + offset).
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Returns the token at (index + offset).
         */
        public Token get(int offset) {
            Preconditions.checkState(has(offset));
            return tokens.get(index + offset);
        }

        /**
         * Returns the next token, if present.
         */
        public Optional<Token> getNext() {
            return index < tokens.size() ? Optional.of(tokens.get(index)) : Optional.empty();
        }

        /**
         * Returns true if the next characters match their corresponding
         * pattern. Each pattern is either a {@link Token.Type}, matching tokens
         * of that type, or a {@link String}, matching tokens with that literal.
         * In effect, {@code new Token(Token.Type.IDENTIFIER, "literal")} is
         * matched by both {@code peek(Token.Type.IDENTIFIER)} and
         * {@code peek("literal")}.
         */
        public boolean peek(Object... patterns) {
            if (!has(patterns.length - 1)) {
                return false;
            }
            for (int offset = 0; offset < patterns.length; offset++) {
                var token = tokens.get(index + offset);
                var pattern = patterns[offset];
                Preconditions.checkState(pattern instanceof Token.Type || pattern instanceof String, pattern);
                if (!token.type().equals(pattern) && !token.literal().equals(pattern)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Equivalent to peek, but also advances the token stream.
         */
        public boolean match(Object... patterns) {
            var peek = peek(patterns);
            if (peek) {
                index += patterns.length;
            }
            return peek;
        }

    }

}
