package miniJava;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ErrorReporter {
    private int numErrors;

    ErrorReporter() {
        numErrors = 0;
    }

    public boolean hasErrors() {
        return numErrors > 0;
    }

    public void reportError(String message, SourcePosition pos) {
        System.out.println("*** line " + pos.start + ": " + message);
        numErrors++;
    }
}
