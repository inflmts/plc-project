package plc.project.analyzer;

import plc.project.parser.Ast;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class Analyzer implements Ast.Visitor<Type, AnalyzeException> {

    private Context context;

    public Analyzer(Scope scope) {
        this.context = new Context(scope, Optional.empty(), false, Set.of());
    }

    public Context getContext() {
        return context;
    }

    @Override
    public Type visit(Ast.Source ast) throws AnalyzeException {
        Type type = Type.NIL;
        for (var statement : ast.statements()) {
            type = visit(statement);
        }
        return type;
    }

    @Override
    public Type visit(Ast.Stmt.Let ast) throws AnalyzeException {
        var type = declareVariable(context.scope(), ast);
        if (ast.value().isEmpty()) {
            context.uninitialized().add(ast.name());
        }
        return type;
    }

    @Override
    public Type visit(Ast.Stmt.Def ast) throws AnalyzeException {
        return declareFunction(context.scope(), ast, Optional.empty());
    }

    @Override
    public Type visit(Ast.Stmt.If ast) throws AnalyzeException {
        var condition = visit(ast.condition());
        if (!condition.isSubtypeOf(Type.BOOLEAN)) {
            throw new AnalyzeException("IF condition is " + condition + ", expected Boolean", ast.condition());
        }
        var original = context;
        var thenContext = new Context(context);
        var elseContext = new Context(context);
        try {
            context = thenContext;
            for (var stmt : ast.thenBody()) {
                visit(stmt);
            }
            context = elseContext;
            for (var stmt : ast.elseBody()) {
                visit(stmt);
            }
        } finally {
            context = original;
        }
        context.merge(List.of(thenContext, elseContext));
        return Type.DYNAMIC;
    }

    @Override
    public Type visit(Ast.Stmt.For ast) throws AnalyzeException {
        var expression = visit(ast.expression());
        if (!expression.isSubtypeOf(Type.ITERABLE)) {
            throw new AnalyzeException("FOR expression is " + expression + ", expected Iterable", ast.expression());
        }
        var original = context;
        var iterationContext = new Context(context);
        iterationContext.scope().declare(ast.name(), Type.INTEGER);
        try {
            context = iterationContext;
            for (var stmt : ast.body()) {
                visit(stmt);
            }
        } finally {
            context = original;
        }
        context.merge(List.of(iterationContext));
        return Type.NIL;
    }

    @Override
    public Type visit(Ast.Stmt.Return ast) throws AnalyzeException {
        if (context.function().isEmpty()) {
            throw new AnalyzeException("RETURN outside function", ast);
        }
        var function = context.function().get();
        var type = ast.value().isPresent() ? visit(ast.value().get()) : Type.NIL;
        if (!type.isSubtypeOf(function.returns())) {
            throw new AnalyzeException("Cannot return " + type + " in function returning " + function.returns(), ast);
        }
        context.returns(true);
        return Type.NIL; // XXX
    }

    @Override
    public Type visit(Ast.Stmt.Expression ast) throws AnalyzeException {
        return visit(ast.expression());
    }

    @Override
    public Type visit(Ast.Stmt.Assignment ast) throws AnalyzeException {
        if (ast.expression() instanceof Ast.Expr.Variable variable) {
            var target = resolveVariable(variable.name(), variable);
            var value = visit(ast.value());
            if (!value.isSubtypeOf(target)) {
                throw new AnalyzeException("Cannot assign " + value + " to variable of type " + target, variable);
            }
            context.uninitialized().remove(variable.name());
            return target;
        } else if (ast.expression() instanceof Ast.Expr.Property property) {
            var target = resolveProperty(visit(property.receiver()), property.name(), property.receiver(), property);
            var value = visit(ast.value());
            if (!value.isSubtypeOf(target)) {
                throw new AnalyzeException("Cannot assign " + value + " to property of type " + target, property);
            }
            return target;
        } else {
            throw new AnalyzeException("Invalid assignment target", ast.expression());
        }
    }

    @Override
    public Type visit(Ast.Expr.Literal ast) throws AnalyzeException {
        return switch (ast.value()) {
            case null -> Type.NIL;
            case Boolean _ -> Type.BOOLEAN;
            case BigInteger _ -> Type.INTEGER;
            case BigDecimal _ -> Type.DECIMAL;
            case Character _ -> Type.CHARACTER;
            case String _ -> Type.STRING;
            default -> throw new AssertionError(ast.value().getClass());
        };
    }

    @Override
    public Type visit(Ast.Expr.Group ast) throws AnalyzeException {
        return visit(ast.expression());
    }

    @Override
    public Type visit(Ast.Expr.Binary ast) throws AnalyzeException { // XXX
        switch (ast.operator()) {
            case "+" -> {
                var left = visit(ast.left());
                var right = visit(ast.right());
                if (left.equals(Type.DYNAMIC)) {
                    if (right.isSubtypeOf(Type.STRING) || right.isSubtypeOf(Type.INTEGER) || right.isSubtypeOf(Type.DECIMAL)) {
                        return right;
                    }
                } else if (left.isSubtypeOf(Type.STRING) || right.isSubtypeOf(Type.STRING)) {
                    return Type.STRING;
                } else if (left.isSubtypeOf(Type.INTEGER)) {
                    if (right.isSubtypeOf(Type.INTEGER)) {
                        return Type.INTEGER;
                    }
                } else if (left.isSubtypeOf(Type.DECIMAL)) {
                    if (right.isSubtypeOf(Type.DECIMAL)) {
                        return Type.DECIMAL;
                    }
                    return Type.DECIMAL;
                } else {
                    throw new AnalyzeException("Cannot add " + left + " and " + right, ast.left());
                }
                throw new AnalyzeException("Cannot add " + left + " and " + right, ast.right());
            }
            case "-", "*", "/" -> {
                var left = visit(ast.left());
                var right = visit(ast.right());
                if (left.equals(Type.DYNAMIC)) {
                    if (right.isSubtypeOf(Type.INTEGER) || right.isSubtypeOf(Type.DECIMAL)) {
                        return right;
                    }
                } else if (left.isSubtypeOf(Type.INTEGER)) {
                    if (right.isSubtypeOf(Type.INTEGER)) {
                        return Type.INTEGER;
                    }
                } else if (left.isSubtypeOf(Type.DECIMAL)) {
                    if (right.isSubtypeOf(Type.DECIMAL)) {
                        return Type.DECIMAL;
                    }
                } else {
                    throw new AnalyzeException(ast.operator() + " operands incompatible: " + left + ", " + right, ast.left());
                }
                throw new AnalyzeException(ast.operator() + " operands incompatible: " + left + ", " + right, ast.right());
            }
            case "==", "!=" -> {
                var left = visit(ast.left());
                var right = visit(ast.right());
                if (!left.isSubtypeOf(right) && !right.isSubtypeOf(left)) {
                    throw new AnalyzeException(ast.operator() + " operands incompatible: " + left + ", " + right, ast.right());
                }
                return Type.BOOLEAN;
            }
            case "<", "<=", ">", ">=" -> {
                var left = visit(ast.left());
                if (!left.isSubtypeOf(Type.COMPARABLE)) {
                    throw new AnalyzeException(left + " is not comparable", ast.left());
                }
                var right = visit(ast.right());
                if (!left.isSubtypeOf(right) || !right.isSubtypeOf(left)) {
                    throw new AnalyzeException(ast.operator() + " operands incompatible: " + left + ", " + right, ast.right());
                }
                return Type.BOOLEAN;
            }
            case "AND", "OR" -> {
                var left = visit(ast.left());
                if (!left.isSubtypeOf(Type.BOOLEAN)) {
                    throw new AnalyzeException(ast.operator() + " operand must be Boolean, got " + left, ast.left());
                }
                var right = visit(ast.right());
                if (!right.isSubtypeOf(Type.BOOLEAN)) {
                    throw new AnalyzeException(ast.operator() + " operand must be Boolean, got " + right, ast.right());
                }
                return Type.BOOLEAN;
            }
        }
        throw new IllegalArgumentException("Unsupported binary operator '" + ast.operator() + "'");
    }

    @Override
    public Type visit(Ast.Expr.Variable ast) throws AnalyzeException {
        var type = resolveVariable(ast.name(), ast);
        if (context.uninitialized().contains(ast.name())) {
            throw new AnalyzeException("Variable " + ast.name() + " is uninitialized", ast);
        }
        return type;
    }

    @Override
    public Type visit(Ast.Expr.Property ast) throws AnalyzeException {
        return resolveProperty(visit(ast.receiver()), ast.name(), ast.receiver(), ast);
    }

    @Override
    public Type visit(Ast.Expr.Function ast) throws AnalyzeException {
        var type = resolveVariable(ast.name(), ast);
        return call(type, ast.arguments(), ast);
    }

    @Override
    public Type visit(Ast.Expr.Method ast) throws AnalyzeException {
        var type = resolveProperty(visit(ast.receiver()), ast.name(), ast.receiver(), ast);
        return call(type, ast.arguments(), ast);
    }

    @Override
    public Type visit(Ast.Expr.ObjectExpr ast) throws AnalyzeException {
        var type = new Type.ObjectType(ast.name(), new Scope(null));
        for (var stmt : ast.fields()) {
            declareVariable(type.scope(), stmt);
        }
        for (var stmt : ast.methods()) {
            declareFunction(type.scope(), stmt, Optional.of(type));
        }
        return type;
    }

    private Type declareVariable(Scope targetScope, Ast.Stmt.Let ast) throws AnalyzeException {
        if (targetScope.current(ast.name()).isPresent()) {
            throw new AnalyzeException("Variable " + ast.name() + " already defined", ast);
        }
        Type type;
        if (ast.type().isPresent()) {
            type = resolveType(ast.type().get(), ast);
            if (ast.value().isPresent()) {
                var value = visit(ast.value().get());
                if (!value.isSubtypeOf(type)) {
                    throw new AnalyzeException("Cannot assign " + value + " to " + type, ast.value().get());
                }
            }
        } else if (ast.value().isPresent()) {
            type = visit(ast.value().get());
        } else {
            type = Type.DYNAMIC;
        }
        targetScope.declare(ast.name(), type);
        return type;
    }

    private Type declareFunction(Scope targetScope, Ast.Stmt.Def ast, Optional<Type> receiver) throws AnalyzeException {
        if (targetScope.current(ast.name()).isPresent()) {
            throw new AnalyzeException("Variable " + ast.name() + " already defined", ast);
        }
        var parameters = ast.parameters();
        for (int i = 0; i < parameters.size(); ++i) {
            var parameter = parameters.get(i);
            if (receiver.isPresent() && parameter.equals("this")) {
                throw new AnalyzeException("this is not a valid parameter name in methods", ast);
            }
            for (int j = 0; j < i; ++j) {
                if (parameters.get(j).equals(parameter)) {
                    throw new AnalyzeException("Duplicate parameter " + parameter, ast);
                }
            }
        }
        var parameterTypes = new ArrayList<Type>(ast.parameterTypes().size());
        for (var name : ast.parameterTypes()) {
            parameterTypes.add(resolveTypeOr(name, Type.DYNAMIC, ast));
        }
        var returnType = resolveTypeOr(ast.returnType(), Type.DYNAMIC, ast);
        var type = new Type.Function(parameterTypes, returnType);
        targetScope.declare(ast.name(), type);

        var original = context;
        var parameterScope = new Scope(original.scope());
        receiver.ifPresent(r -> parameterScope.declare("this", r));
        for (int i = 0; i < parameters.size(); ++i) {
            parameterScope.declare(parameters.get(i), parameterTypes.get(i));
        }
        context = new Context(new Scope(parameterScope), Optional.of(type), false, Set.of());
        try {
            for (var stmt : ast.body()) {
                visit(stmt);
            }
            if (!returnType.isSubtypeOf(Type.NIL) && !context.returns()) {
                throw new AnalyzeException("Function with return type " + returnType + " does not return", ast);
            }
        } finally {
            context = original;
        }
        return type;
    }

    private Type resolveVariable(String name, Ast ast) throws AnalyzeException {
        return context.scope().resolve(name).orElseThrow(
            () -> new AnalyzeException("Variable " + name + " does not exist", ast)
        );
    }

    private Type resolveProperty(Type receiver, String name, Ast receiverAst, Ast ast) throws AnalyzeException {
        if (receiver.equals(Type.DYNAMIC)) {
            return Type.DYNAMIC;
        }
        if (!(receiver instanceof Type.ObjectType receiverObject)) {
            throw new AnalyzeException("Cannot access property of " + receiver, receiverAst);
        }
        var type = receiverObject.scope().current(name);
        if (type.isPresent()) {
            return type.get();
        }
        var prototype = receiverObject.scope().current("prototype").orElseThrow(
            () -> new AnalyzeException("No such property '" + name + "'", ast)
        );
        return resolveProperty(prototype, name, receiverAst, ast);
    }

    private Type resolveType(String name, Ast ast) throws AnalyzeException {
        Type type = Environment.TYPES.get(name);
        if (type == null) {
            throw new AnalyzeException("Invalid type " + name, ast);
        }
        return type;
    }

    private Type resolveTypeOr(Optional<String> name, Type fallback, Ast ast) throws AnalyzeException {
        return name.isPresent() ? resolveType(name.get(), ast) : fallback;
    }

    private Type call(Type type, List<Ast.Expr> arguments, Ast ast) throws AnalyzeException {
        if (type.equals(Type.DYNAMIC)) {
            return Type.DYNAMIC;
        }
        if (!(type instanceof Type.Function function)) {
            throw new AnalyzeException("Cannot call " + type, ast);
        }
        var parameters = function.parameters();
        if (arguments.size() != parameters.size()) {
            throw new AnalyzeException("Incorrect number of arguments, expected " + parameters.size() + ", got " + arguments.size(), ast);
        }
        for (int i = 0; i < arguments.size(); ++i) {
            var argument = visit(arguments.get(i));
            var parameter = parameters.get(i);
            if (!argument.isSubtypeOf(parameter)) {
                throw new AnalyzeException("Cannot assign " + argument + " to parameter of type " + parameter, arguments.get(i));
            }
        }
        return function.returns();
    }

    public static final class ContextHooks {

        public static boolean mergeReturns(List<Context> children) {
            return children.stream().allMatch(Context::returns);
        }

        public static Set<String> mergeUninitialized(List<Context> children) {
            return children.stream().map(Context::uninitialized).collect(HashSet::new, HashSet::addAll, HashSet::addAll);
        }

    }

    public static final class EnvironmentHooks {

        public static Type rangeType() {
            return new Type.Function(List.of(Type.INTEGER, Type.INTEGER), Type.ITERABLE);
        }

        public static Type isinstanceType() {
            // The type system does not have a way to represent "any object"
            return new Type.Function(List.of(Type.ANY, Type.ANY), Type.BOOLEAN);
        }

    }

    public static final class TypeHooks {

        public static boolean isSubtypeOf(Type subtype, Type supertype) {
            if (supertype.equals(subtype)) {
                return true;
            }
            if (supertype.equals(Type.ANY)) {
                return true;
            }
            if (supertype.equals(Type.DYNAMIC) || subtype.equals(Type.DYNAMIC)) {
                return true;
            }
            if (supertype.equals(Type.EQUATABLE) && (
                subtype.equals(Type.NIL) ||
                subtype.isSubtypeOf(Type.COMPARABLE) ||
                subtype.equals(Type.ITERABLE) ||
                subtype instanceof Type.ObjectType)) {
                return true;
            }
            if (supertype.equals(Type.COMPARABLE) && (
                subtype.equals(Type.BOOLEAN) ||
                subtype.equals(Type.INTEGER) ||
                subtype.equals(Type.DECIMAL) ||
                subtype.equals(Type.CHARACTER) ||
                subtype.equals(Type.STRING))) {
                return true;
            }
            if (supertype instanceof Type.ObjectType superobject && subtype instanceof Type.ObjectType subobject) {
                var prototype = subobject.scope().current("prototype");
                if (prototype.isPresent()) {
                    return prototype.get().isSubtypeOf(superobject);
                }
            }
            return false;
        }

    }

}
