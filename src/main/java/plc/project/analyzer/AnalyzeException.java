package plc.project.analyzer;

import plc.project.parser.Ast;

/**
 * IMPORTANT: This is an API file and should not be modified by your submission.
 */
public final class AnalyzeException extends Exception {

    private final Ast ast;

    public AnalyzeException(String message, Ast ast) {
        super(message + "\n - @ ast " + ast);
        this.ast = ast;
    }

    public Ast getAst() {
        return ast;
    }

}
