package plc.project.evaluator;

import plc.project.parser.Ast;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Evaluator implements Ast.Visitor<RuntimeValue, EvaluateException> {

    private Scope scope;

    public Evaluator(Scope scope) {
        this.scope = scope;
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public RuntimeValue visit(Ast.Source ast) throws EvaluateException {
        RuntimeValue value = new RuntimeValue.Primitive(null);
        for (var stmt : ast.statements()) {
            try {
                value = visit(stmt);
            } catch (Return e) {
                throw new EvaluateException("Cannot use RETURN outside function", Optional.of(e.getAst()));
            }
        }
        return value;
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.Let ast) throws EvaluateException {
        return defineVariable(scope, ast);
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.Def ast) throws EvaluateException {
        return defineFunction(scope, scope, ast);
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.If ast) throws EvaluateException {
        RuntimeValue condition = visit(ast.condition());
        if (!(condition instanceof RuntimeValue.Primitive(Boolean conditionBoolean))) {
            throw new EvaluateException("IF condition must be a boolean", Optional.of(ast.condition()));
        }
        Scope parent = scope;
        scope = new Scope(parent);
        RuntimeValue value = new RuntimeValue.Primitive(null);
        try {
            if (conditionBoolean) {
                for (var stmt : ast.thenBody()) {
                    value = visit(stmt);
                }
            } else {
                for (var stmt : ast.elseBody()) {
                    value = visit(stmt);
                }
            }
        } finally {
            scope = parent;
        }
        return value;
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.For ast) throws EvaluateException {
        var expression = visit(ast.expression());
        if (!(expression instanceof RuntimeValue.Primitive(Iterable iterable))) {
            throw new EvaluateException("FOR expression is not iterable", Optional.of(ast.expression()));
        }
        Scope parent = scope;
        try {
            for (var iterationValue : iterable) {
                Scope iterationScope = new Scope(parent);
                iterationScope.define(ast.name(), (RuntimeValue) iterationValue);
                scope = new Scope(iterationScope);
                for (var stmt : ast.body()) {
                    visit(stmt);
                }
            }
        } finally {
            scope = parent;
        }
        return new RuntimeValue.Primitive(null);
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.Return ast) throws EvaluateException {
        RuntimeValue value;
        if (ast.value().isPresent()) {
            value = visit(ast.value().get());
        } else {
            value = new RuntimeValue.Primitive(null);
        }
        throw new Return(value, ast);
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.Expression ast) throws EvaluateException {
        return visit(ast.expression());
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.Assignment ast) throws EvaluateException {
        if (ast.expression() instanceof Ast.Expr.Variable variable) {
            if (scope.resolve(variable.name()).isEmpty()) {
                throw new EvaluateException("Variable '" + variable.name() + "' does not exist", Optional.of(variable));
            }
            var value = visit(ast.value());
            scope.assign(variable.name(), value);
            return value;
        } else if (ast.expression() instanceof Ast.Expr.Property property) {
            var receiver = visit(property.receiver());
            if (!(receiver instanceof RuntimeValue.ObjectValue object)) {
                throw new EvaluateException("Receiver is not an object", Optional.of(property.receiver()));
            }
            resolveProperty(object, property.name(), property);
            var value = visit(ast.value());
            // Assignments always affect the object's scope (the prototype object is never modified).
            if (object.scope().current(property.name()).isPresent()) {
                object.scope().assign(property.name(), value);
            } else {
                object.scope().define(property.name(), value);
            }
            return value;
        } else {
            throw new EvaluateException("Invalid assignment target", Optional.of(ast.expression()));
        }
    }

    @Override
    public RuntimeValue visit(Ast.Expr.Literal ast) throws EvaluateException {
        return new RuntimeValue.Primitive(ast.value());
    }

    @Override
    public RuntimeValue visit(Ast.Expr.Group ast) throws EvaluateException {
        return visit(ast.expression());
    }

    @Override
    public RuntimeValue visit(Ast.Expr.Binary ast) throws EvaluateException {
        switch (ast.operator()) {
            case "+" -> {
                var left = visit(ast.left());
                var right = visit(ast.right());
                if (left instanceof RuntimeValue.Primitive(String leftString)) {
                    return new RuntimeValue.Primitive(leftString + right.print());
                } else if (right instanceof RuntimeValue.Primitive(String rightString)) {
                    return new RuntimeValue.Primitive(left.print() + rightString);
                } else if (left instanceof RuntimeValue.Primitive(BigInteger leftInteger)) {
                    if (!(right instanceof RuntimeValue.Primitive(BigInteger rightInteger))) {
                        throw new EvaluateException("Cannot add integer and non-integer", Optional.of(ast.right()));
                    }
                    return new RuntimeValue.Primitive(leftInteger.add(rightInteger));
                } else if (left instanceof RuntimeValue.Primitive(BigDecimal leftDecimal)) {
                    if (!(right instanceof RuntimeValue.Primitive(BigDecimal rightDecimal))) {
                        throw new EvaluateException("Cannot add decimal and non-decimal", Optional.of(ast.right()));
                    }
                    return new RuntimeValue.Primitive(leftDecimal.add(rightDecimal));
                } else {
                    throw new EvaluateException("Cannot add non-numeric types", Optional.of(ast.left()));
                }
            }
            case "-" -> {
                var left = visit(ast.left());
                if (left instanceof RuntimeValue.Primitive(BigInteger leftInteger)) {
                    var right = visit(ast.right());
                    if (!(right instanceof RuntimeValue.Primitive(BigInteger rightInteger))) {
                        throw new EvaluateException("Cannot subtract integer with non-integer", Optional.of(ast.right()));
                    }
                    return new RuntimeValue.Primitive(leftInteger.subtract(rightInteger));
                } else if (left instanceof RuntimeValue.Primitive(BigDecimal leftDecimal)) {
                    var right = visit(ast.right());
                    if (!(right instanceof RuntimeValue.Primitive(BigDecimal rightDecimal))) {
                        throw new EvaluateException("Cannot subtract decimal with non-decimal", Optional.of(ast.right()));
                    }
                    return new RuntimeValue.Primitive(leftDecimal.subtract(rightDecimal));
                } else {
                    throw new EvaluateException("Cannot subtract non-numeric types", Optional.of(ast.left()));
                }
            }
            case "*" -> {
                var left = visit(ast.left());
                if (left instanceof RuntimeValue.Primitive(BigInteger leftInteger)) {
                    var right = visit(ast.right());
                    if (!(right instanceof RuntimeValue.Primitive(BigInteger rightInteger))) {
                        throw new EvaluateException("Cannot multiply integer with non-integer", Optional.of(ast.right()));
                    }
                    return new RuntimeValue.Primitive(leftInteger.multiply(rightInteger));
                } else if (left instanceof RuntimeValue.Primitive(BigDecimal leftDecimal)) {
                    var right = visit(ast.right());
                    if (!(right instanceof RuntimeValue.Primitive(BigDecimal rightDecimal))) {
                        throw new EvaluateException("Cannot multiply decimal with non-decimal", Optional.of(ast.right()));
                    }
                    return new RuntimeValue.Primitive(leftDecimal.multiply(rightDecimal));
                } else {
                    throw new EvaluateException("Cannot multiply non-numeric types", Optional.of(ast.left()));
                }
            }
            case "/" -> {
                var left = visit(ast.left());
                if (left instanceof RuntimeValue.Primitive(BigInteger leftInteger)) {
                    var right = visit(ast.right());
                    if (!(right instanceof RuntimeValue.Primitive(BigInteger rightInteger))) {
                        throw new EvaluateException("Cannot divide integer with non-integer", Optional.of(ast.right()));
                    }
                    try {
                        return new RuntimeValue.Primitive(leftInteger.divide(rightInteger));
                    } catch (ArithmeticException e) {
                        throw new EvaluateException("Cannot divide by zero", Optional.of(ast.right()));
                    }
                } else if (left instanceof RuntimeValue.Primitive(BigDecimal leftDecimal)) {
                    var right = visit(ast.right());
                    if (!(right instanceof RuntimeValue.Primitive(BigDecimal rightDecimal))) {
                        throw new EvaluateException("Cannot divide decimal with non-decimal", Optional.of(ast.right()));
                    }
                    try {
                        return new RuntimeValue.Primitive(leftDecimal.divide(rightDecimal, RoundingMode.HALF_EVEN));
                    } catch (ArithmeticException e) {
                        throw new EvaluateException("Cannot divide by zero", Optional.of(ast.right()));
                    }
                } else {
                    throw new EvaluateException("Cannot divide non-numeric types", Optional.of(ast.left()));
                }
            }
            case "==" -> {
                var left = visit(ast.left());
                var right = visit(ast.right());
                return new RuntimeValue.Primitive(left.equals(right));
            }
            case "!=" -> {
                var left = visit(ast.left());
                var right = visit(ast.right());
                return new RuntimeValue.Primitive(!left.equals(right));
            }
            case "<" -> {
                return new RuntimeValue.Primitive(compare(ast) < 0);
            }
            case "<=" -> {
                return new RuntimeValue.Primitive(compare(ast) <= 0);
            }
            case ">" -> {
                return new RuntimeValue.Primitive(compare(ast) > 0);
            }
            case ">=" -> {
                return new RuntimeValue.Primitive(compare(ast) >= 0);
            }
            case "AND" -> {
                var left = visit(ast.left());
                if (!(left instanceof RuntimeValue.Primitive(Boolean leftBoolean))) {
                    throw new EvaluateException("AND operand must be a boolean", Optional.of(ast.left()));
                }
                if (!leftBoolean) {
                    return left;
                }
                var right = visit(ast.right());
                if (!(right instanceof RuntimeValue.Primitive(Boolean rightBoolean))) {
                    throw new EvaluateException("AND operand must be a boolean", Optional.of(ast.right()));
                }
                return right;
            }
            case "OR" -> {
                var left = visit(ast.left());
                if (!(left instanceof RuntimeValue.Primitive(Boolean leftBoolean))) {
                    throw new EvaluateException("AND operand must be a boolean", Optional.of(ast.left()));
                }
                if (leftBoolean) {
                    return left;
                }
                var right = visit(ast.right());
                if (!(right instanceof RuntimeValue.Primitive(Boolean rightBoolean))) {
                    throw new EvaluateException("AND operand must be a boolean", Optional.of(ast.right()));
                }
                return right;
            }
        }
        throw new IllegalArgumentException("Unsupported binary operator '" + ast.operator() + "'");
    }

    @Override
    public RuntimeValue visit(Ast.Expr.Variable ast) throws EvaluateException {
        return scope.resolve(ast.name()).orElseThrow(() -> {
            return new EvaluateException("Variable '" + ast.name() + "' not defined", Optional.of(ast));
        });
    }

    @Override
    public RuntimeValue visit(Ast.Expr.Property ast) throws EvaluateException {
        var receiver = visit(ast.receiver());
        if (!(receiver instanceof RuntimeValue.ObjectValue object)) {
            throw new EvaluateException("Receiver is not an object", Optional.of(ast.receiver()));
        }
        return resolveProperty(object, ast.name(), ast);
    }

    @Override
    public RuntimeValue visit(Ast.Expr.Function ast) throws EvaluateException {
        var value = scope.resolve(ast.name()).orElseThrow(() -> {
            return new EvaluateException("Function '" + ast.name() + "' does not exist", Optional.of(ast));
        });
        if (!(value instanceof RuntimeValue.Function function)) {
            throw new EvaluateException("'" + ast.name() + "' is not a function", Optional.of(ast));
        }
        var arguments = new ArrayList<RuntimeValue>(ast.arguments().size());
        for (var argumentAst : ast.arguments()) {
            arguments.add(visit(argumentAst));
        }
        return function.definition().invoke(arguments);
    }

    @Override
    public RuntimeValue visit(Ast.Expr.Method ast) throws EvaluateException {
        var receiver = visit(ast.receiver());
        if (!(receiver instanceof RuntimeValue.ObjectValue object)) {
            throw new EvaluateException("Receiver is not an object", Optional.of(ast.receiver()));
        }
        var value = resolveProperty(object, ast.name(), ast);
        if (!(value instanceof RuntimeValue.Function function)) {
            throw new EvaluateException("'" + ast.name() + "' is not a function", Optional.of(ast));
        }
        var arguments = new ArrayList<RuntimeValue>(ast.arguments().size() + 1);
        arguments.add(receiver);
        for (var argumentAst : ast.arguments()) {
            arguments.add(visit(argumentAst));
        }
        return function.definition().invoke(arguments);
    }

    @Override
    public RuntimeValue visit(Ast.Expr.ObjectExpr ast) throws EvaluateException {
        var objectScope = new Scope(null);
        for (var stmt : ast.fields()) {
            defineVariable(objectScope, stmt);
        }
        for (var stmt : ast.methods()) {
            var parameters = new ArrayList<String>(stmt.parameters().size() + 1);
            parameters.add("this");
            parameters.addAll(stmt.parameters());
            defineFunction(objectScope, scope, new Ast.Stmt.Def(stmt.name(), parameters, stmt.body()));
        }
        return new RuntimeValue.ObjectValue(ast.name(), objectScope);
    }

    private int compare(Ast.Expr.Binary ast) throws EvaluateException {
        var left = visit(ast.left());
        if (!(left instanceof RuntimeValue.Primitive(Comparable leftObject))) {
            throw new EvaluateException("Value is not comparable", Optional.of(ast.left()));
        }
        var right = visit(ast.right());
        if (!(right instanceof RuntimeValue.Primitive(Comparable rightObject))) {
            throw new EvaluateException("Value is not comparable", Optional.of(ast.right()));
        }
        if (leftObject.getClass() != rightObject.getClass()) {
            throw new EvaluateException("Cannot compare objects of different types", Optional.of(ast.right()));
        }
        return leftObject.compareTo(rightObject);
    }

    private RuntimeValue defineVariable(Scope targetScope, Ast.Stmt.Let ast) throws EvaluateException {
        if (targetScope.current(ast.name()).isPresent()) {
            throw new EvaluateException("Variable '" + ast.name() + "' already defined", Optional.of(ast));
        }
        RuntimeValue value;
        if (ast.value().isPresent()) {
            value = visit(ast.value().get());
        } else {
            value = new RuntimeValue.Primitive(null);
        }
        targetScope.define(ast.name(), value);
        return value;
    }

    private RuntimeValue defineFunction(Scope targetScope, Scope closureScope, Ast.Stmt.Def ast) throws EvaluateException {
        if (targetScope.current(ast.name()).isPresent()) {
            throw new EvaluateException("Variable '" + ast.name() + "' already defined", Optional.of(ast));
        }
        final var parameters = ast.parameters();
        for (int i = 0; i < parameters.size(); ++i) {
            var parameter = parameters.get(i);
            for (int j = 0; j < i; ++j) {
                if (parameters.get(j) == parameter) {
                    throw new EvaluateException("Duplicate parameter '" + parameter + "'", Optional.of(ast));
                }
            }
        }
        var value = new RuntimeValue.Function(ast.name(), arguments -> {
            if (arguments.size() != parameters.size()) {
                throw new EvaluateException("Incorrect number of parameters", Optional.empty());
            }
            Scope callerScope = scope;
            Scope argumentScope = new Scope(closureScope);
            for (int i = 0; i < parameters.size(); ++i) {
                argumentScope.define(parameters.get(i), arguments.get(i));
            }
            scope = new Scope(argumentScope);
            try {
                for (var stmt : ast.body()) {
                    visit(stmt);
                }
            } catch (Return e) {
                return e.getValue();
            } finally {
                scope = callerScope;
            }
            return new RuntimeValue.Primitive(null);
        });
        targetScope.define(ast.name(), value);
        return value;
    }

    private RuntimeValue resolveProperty(RuntimeValue.ObjectValue object, String name, Ast ast) throws EvaluateException {
        var value = object.scope().current(name);
        if (value.isPresent()) {
            return value.get();
        }
        var prototype = object.scope().current("prototype").orElseThrow(() -> {
            return new EvaluateException("No such property '" + name + "'", Optional.of(ast));
        });
        if (!(prototype instanceof RuntimeValue.ObjectValue prototypeObject)) {
            throw new EvaluateException("Prototype is not an object", Optional.of(ast));
        }
        return resolveProperty(prototypeObject, name, ast);
    }

    public final class Return extends RuntimeException {

        private final RuntimeValue value;
        private final Ast ast;

        public Return(RuntimeValue value, Ast ast) {
            this.value = value;
            this.ast = ast;
        }

        public RuntimeValue getValue() {
            return value;
        }

        public Ast getAst() {
            return ast;
        }

    }

    public static class EnvironmentHooks {

        public static RuntimeValue range(List<RuntimeValue> arguments) throws EvaluateException {
            if (arguments.size() != 2) {
                throw new EvaluateException("range() expects 2 arguments, got " + arguments.size(), Optional.empty());
            }
            var left = arguments.get(0);
            var right = arguments.get(1);
            if (!(left instanceof RuntimeValue.Primitive(BigInteger leftInteger))) {
                throw new EvaluateException("range(): start is not an integer", Optional.empty());
            }
            if (!(right instanceof RuntimeValue.Primitive(BigInteger rightInteger))) {
                throw new EvaluateException("range(): end is not an integer", Optional.empty());
            }
            if (leftInteger.compareTo(rightInteger) > 0) {
                throw new EvaluateException("range(): start cannot be greater than end", Optional.empty());
            }
            var list = new ArrayList<RuntimeValue>();
            for (var i = leftInteger; !i.equals(rightInteger); i = i.add(BigInteger.ONE)) {
                list.add(new RuntimeValue.Primitive(i));
            }
            return new RuntimeValue.Primitive(list);
        }

        public static RuntimeValue isinstance(List<RuntimeValue> arguments) throws EvaluateException {
            if (arguments.size() != 2) {
                throw new EvaluateException("isinstance() expects 2 arguments, got " + arguments.size(), Optional.empty());
            }
            if (!(arguments.get(1) instanceof RuntimeValue.ObjectValue prototype)) {
                throw new EvaluateException("isinstance(): prototype is not an object", Optional.empty());
            }
            var value = arguments.get(0);
            while (value instanceof RuntimeValue.ObjectValue object) {
                if (object == prototype) {
                    return new RuntimeValue.Primitive(true);
                }
                var objectPrototype = object.scope().current("prototype");
                if (objectPrototype.isEmpty()) {
                    break;
                }
                value = objectPrototype.get();
            }
            return new RuntimeValue.Primitive(false);
        }

    }

}
