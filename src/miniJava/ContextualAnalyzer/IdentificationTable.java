package miniJava.ContextualAnalyzer;

import miniJava.AbstractSyntaxTrees.ClassDecl;
import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.AbstractSyntaxTrees.VarDecl;
import miniJava.ErrorReporter;

import java.util.ArrayList;
import java.util.HashMap;

public class IdentificationTable {
    private Identification identification;
    private ArrayList<HashMap<String, Declaration>> idTable;
    public ClassDecl thisClassDecl;
    public MethodDecl currentMethodDecl;
    public ErrorReporter reporter;
    public VarDecl undeclaredVarName;

    public IdentificationTable(Identification identification, ErrorReporter reporter) {
        this.identification = identification;
        idTable = new ArrayList<>();
        this.reporter = reporter;
    }

    public void openScope() {
        idTable.add(new HashMap<>());
    }

    public void closeScope() {
        idTable.remove(idTable.size() - 1);
    }

    public void enter(String id, Declaration attribute) {
        int currentLevel = idTable.size() - 1;
        // Search for duplicate.
        for (int level = currentLevel; level >= 0; level--) {
            if (idTable.get(level).containsKey(id) && (level == currentLevel || level >= 2)) {
                attribute.duplicated = true;
                break;
            }
        }
        idTable.get(currentLevel).put(id, attribute);
        if (attribute.duplicated) {
            reporter.reportError("identifier " + attribute.name + " already declared",
                    attribute.posn);
        }
    }

    public Declaration retrieve(String id) {
        if (undeclaredVarName != null && id.equals(undeclaredVarName.name)) {
            identification.identificationError("Cannot access declared variable name \"" +
                    undeclaredVarName.name + "\" in it's own initializer expression", undeclaredVarName.posn);
        }
        for (int i = idTable.size() - 1; i >= 0; i--) {
            if (idTable.get(i).containsKey(id)) {
                return idTable.get(i).get(id);
            }
        }
        return null;
    }

    public boolean hasClass(String className) {
        return idTable.get(0).containsKey(className);
    }

    // Level 0 contains all class declarations.
    public ClassDecl retrieveClass(String className) {
        return (ClassDecl) idTable.get(0).get(className);
    }

    // Level 1 contains all member declarations of the current class.
    public MethodDecl retrieveMethod(String methodName) {
        if (idTable.size() <= 1) {
            return null;
        }
        Declaration decl = idTable.get(1).get(methodName);
        if (decl instanceof MethodDecl) {
            return (MethodDecl) decl;
        } else {
            return null;
        }
    }

    public boolean hasMethod(String methodName) {
        return retrieveMethod(methodName) != null;
    }
}
