package plc.project.analyzer;

import plc.project.parser.Ast;

import java.math.BigDecimal;
import java.math.BigInteger;
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
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    @Override
    public Type visit(Ast.Stmt.Def ast) throws AnalyzeException {
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    @Override
    public Type visit(Ast.Stmt.If ast) throws AnalyzeException {
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    @Override
    public Type visit(Ast.Stmt.For ast) throws AnalyzeException {
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    @Override
    public Type visit(Ast.Stmt.Return ast) throws AnalyzeException {
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    @Override
    public Type visit(Ast.Stmt.Expression ast) throws AnalyzeException {
        return visit(ast.expression());
    }

    @Override
    public Type visit(Ast.Stmt.Assignment ast) throws AnalyzeException {
        throw new UnsupportedOperationException("TODO"); //TODO
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
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    @Override
    public Type visit(Ast.Expr.Binary ast) throws AnalyzeException {
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    @Override
    public Type visit(Ast.Expr.Variable ast) throws AnalyzeException {
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    @Override
    public Type visit(Ast.Expr.Property ast) throws AnalyzeException {
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    @Override
    public Type visit(Ast.Expr.Function ast) throws AnalyzeException {
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    @Override
    public Type visit(Ast.Expr.Method ast) throws AnalyzeException {
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    @Override
    public Type visit(Ast.Expr.ObjectExpr ast) throws AnalyzeException {
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    public static final class ContextHooks {

        public static boolean mergeReturns(List<Context> children) {
            return false; //TODO
        }

        public static Set<String> mergeUninitialized(List<Context> children) {
            return Set.of(); //TODO
        }

    }

    public static final class EnvironmentHooks {

        public static Type rangeType() {
            return Type.DYNAMIC; //TODO
        }

        public static Type isinstanceType() {
            return Type.DYNAMIC; //TODO
        }

    }

    public static final class TypeHooks {

        public static boolean isSubtypeOf(Type subtype, Type supertype) {
            return true; //TODO
        }

    }

}
