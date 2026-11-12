package plc.project.analyzer;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * IMPORTANT: This is an API file and should not be modified by your submission.
 */
public final class Context {

    private final Scope scope;
    private final Optional<Type.Function> function;
    private boolean returns;
    private Set<String> uninitialized;

    public Context(Scope scope, Optional<Type.Function> function, boolean returns, Set<String> uninitialized) {
        this.scope = scope;
        this.function = function;
        this.returns = returns;
        this.uninitialized = new HashSet<>(uninitialized);
    }

    public Context(Context parent) {
        this(new Scope(parent.scope), parent.function, parent.returns, parent.uninitialized);
    }

    public Scope scope() { return scope; }
    public Optional<Type.Function> function() { return function; }
    public boolean returns() { return returns; }
    public void returns(boolean returns) { this.returns = returns; }
    public /*mutable*/ Set<String> uninitialized() { return uninitialized; }

    public void merge(List<Context> children) {
        this.returns = Analyzer.ContextHooks.mergeReturns(children);
        this.uninitialized.retainAll(Analyzer.ContextHooks.mergeUninitialized(children));
    }

}
