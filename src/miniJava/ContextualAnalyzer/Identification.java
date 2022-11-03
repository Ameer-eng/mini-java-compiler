package miniJava.ContextualAnalyzer;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;

public class Identification implements Visitor<Object, Object> {

    public IdentificationTable idTable;
    private final ErrorReporter reporter;
    private static final SourcePosition origin = new SourcePosition(0,0);
    public static final FieldDecl LENGTH_DECL = new FieldDecl(false, false, new BaseType(TypeKind.INT, origin), "length", origin);

    public Identification(ErrorReporter reporter) {
        this.reporter = reporter;
        idTable = new IdentificationTable(this, reporter);
    }

    static class IdentificationError extends Error {
        private static final long serialVersionUID = 1L;
    }

    /**
     * Report identification error and stop identification
     * @param message error message
     * @param pos position of error
     * @throws IdentificationError
     */
    protected void identificationError(String message, SourcePosition pos) throws IdentificationError {
        reporter.reportError("Identification error: " + message, pos);
        throw new IdentificationError();
    }

    public void identify(Package ast) {
            try {
                ast.visit(this, null);
            } catch (IdentificationError ignored) {

            }
    }



    @Override
    public Object visitPackage(Package prog, Object arg) {
        idTable.openScope();
        for (ClassDecl cd : prog.classDeclList) {
            idTable.enter(cd.name, cd);
        }
        for (ClassDecl cd : prog.classDeclList) {
            cd.visit(this, null);
        }
        idTable.closeScope();
        return null;
    }

    @Override
    public Object visitClassDecl(ClassDecl cd, Object arg) {
        idTable.openScope();
        idTable.thisClassDecl = cd;
        for (FieldDecl fd : cd.fieldDeclList) {
            idTable.enter(fd.name, fd);
        }
        for (MethodDecl md : cd.methodDeclList) {
            idTable.enter(md.name, md);
        }
        for (FieldDecl fd : cd.fieldDeclList) {
            fd.visit(this, null);
        }
        for (MethodDecl md : cd.methodDeclList) {
            md.visit(this, null);
        }
        idTable.closeScope();
        return null;
    }

    @Override
    public Object visitFieldDecl(FieldDecl fd, Object arg) {
        // The field declaration has already been entered into the idTable.
        fd.type.visit(this, null);
        return null;
    }

    @Override
    public Object visitMethodDecl(MethodDecl md, Object arg) {
        // The method declaration has already been entered into the idTable.
        idTable.openScope();
        idTable.currentMethodDecl = md;
        for (ParameterDecl pd : md.parameterDeclList) {
            pd.visit(this, null);
        }
        for (Statement statement : md.statementList) {
            statement.visit(this, null);
        }
        idTable.closeScope();
        return null;
    }

    @Override
    public Object visitParameterDecl(ParameterDecl pd, Object arg) {
        pd.type.visit(this, null);
        idTable.enter(pd.name, pd);
        return null;
    }

    @Override
    public Object visitVarDecl(VarDecl decl, Object arg) {
        decl.type.visit(this, null);
        idTable.enter(decl.name, decl);
        return null;
    }

    @Override
    public Object visitBaseType(BaseType type, Object arg) {
        return null;
    }

    @Override
    public Object visitClassType(ClassType type, Object arg) {
        // Check that the class name has been declared as a class.
        type.className.decl = idTable.retrieveClass(type.className.spelling);
        if (type.className.decl == null) {
            identificationError("\"" + type.className.spelling + "\" is not a valid class name", type.posn);
        }
        return null;
    }

    @Override
    public Object visitArrayType(ArrayType type, Object arg) {
        type.eltType.visit(this, null);
        return null;
    }

    @Override
    public Object visitBlockStmt(BlockStmt stmt, Object arg) {
        idTable.openScope();
        for (Statement statement : stmt.sl) {
            statement.visit(this, null);
        }
        idTable.closeScope();
        return null;
    }

    @Override
    public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
        idTable.undeclaredVarName = stmt.varDecl;
        stmt.initExp.visit(this, null);
        idTable.undeclaredVarName = null;
        stmt.varDecl.visit(this, null);
        return null;
    }

    @Override
    public Object visitAssignStmt(AssignStmt stmt, Object arg) {
        stmt.val.visit(this, null);
        stmt.ref.visit(this, null);
        return null;
    }

    @Override
    public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
        stmt.exp.visit(this, null);
        stmt.ref.visit(this, null);
        stmt.ix.visit(this, null);
        return null;
    }

    private void handleMethodCall(Reference methodRef) {
        // If the methodRef is an id, then retrieve the method with it's name.
        // If the methodRef is not an id, visit it and then check whether it's declaration is a method declaration.
        if (methodRef instanceof IdRef) {
            methodRef.decl = idTable.retrieveMethod(((IdRef) methodRef).id.spelling);
            if (methodRef.decl == null) {
                identificationError("\"" + ((IdRef) methodRef).id.spelling +
                        "\" is not a method existing in the current scope.", methodRef.posn);
            }
        } else {
            methodRef.visit(this, null);
            if (!(methodRef.decl instanceof MethodDecl)) {
                identificationError("\"" + methodRef.decl.name + "\" is not a method", methodRef.posn);
            }
        }
    }

    @Override
    public Object visitCallStmt(CallStmt stmt, Object arg) {
        Reference methodRef = stmt.methodRef;
        handleMethodCall(methodRef);
        for (Expression expr : stmt.argList) {
            expr.visit(this, null);
        }
        return null;
    }

    @Override
    public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
        // Save the method that this return statement refers to
        stmt.methodDecl = idTable.currentMethodDecl;
        if (stmt.returnExpr != null) {
            stmt.returnExpr.visit(this, null);
        }
        return null;
    }

    @Override
    public Object visitIfStmt(IfStmt stmt, Object arg) {
        stmt.cond.visit(this, null);
        idTable.openScope();
        stmt.thenStmt.visit(this, null);
        idTable.closeScope();
        if (stmt.elseStmt != null) {
            idTable.openScope();
            stmt.elseStmt.visit(this, null);
            idTable.closeScope();
        }
        return null;
    }

    @Override
    public Object visitWhileStmt(WhileStmt stmt, Object arg) {
        stmt.cond.visit(this, null);
        idTable.openScope();
        stmt.body.visit(this, null);
        idTable.closeScope();
        return null;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
        expr.expr.visit(this, null);
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
        expr.left.visit(this, null);
        expr.right.visit(this, null);
        return null;
    }

    @Override
    public Object visitRefExpr(RefExpr expr, Object arg) {
        expr.ref.visit(this, null);
        return null;
    }

    @Override
    public Object visitIxExpr(IxExpr expr, Object arg) {
        expr.ref.visit(this, null);
        expr.ixExpr.visit(this, null);
        return null;
    }

    @Override
    public Object visitCallExpr(CallExpr expr, Object arg) {
        handleMethodCall(expr.functionRef);
        for (Expression argument : expr.argList) {
            argument.visit(this, null);
        }
        return null;
    }

    @Override
    public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
        // Nothing needs to be done here.
        return null;
    }

    @Override
    public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
        // This visit will verify that the type being instantiated is indeed a class.
        expr.classtype.visit(this, null);
        return null;
    }

    @Override
    public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
        expr.eltType.visit(this, null);
        expr.sizeExpr.visit(this, null);
        return null;
    }

    @Override
    public Object visitThisRef(ThisRef ref, Object arg) {
        ref.decl = idTable.thisClassDecl;
        if (idTable.currentMethodDecl.isStatic) {
            identificationError("\"this\" cannot be referenced from a static context", ref.posn);
        }
        return null;
    }

    @Override
    public Object visitIdRef(IdRef ref, Object arg) {
        ref.id.visit(this, null);
        if (idTable.currentMethodDecl.isStatic && ref.id.decl instanceof MemberDecl &&
                !((MemberDecl) ref.id.decl).isStatic) {
            identificationError("Cannot access nonstatic member \"" + ref.id.spelling +
                    "\" from static context", ref.posn);
        }
        ref.decl = ref.id.decl;
        return null;
    }

    @Override
    public Object visitQRef(QualRef ref, Object arg) {
        ref.ref.visit(this, null);

        if (ref.ref.decl.type instanceof ArrayType) {
            if (ref.id.spelling.equals("length")) {
                SourcePosition origin = new SourcePosition(0,0);
                ref.id.decl = LENGTH_DECL;
                ref.decl = ref.id.decl;
                return null;
            } else {
                identificationError(ref.id.spelling + " is not a field of the Array class", ref.posn);
            }
        }

        // Check whether the declaration the head of the qualref refers to is a class type.
        if (!(ref.ref.decl.type instanceof ClassType)) {
            identificationError("\"" + ref.ref.decl.name +
                    "\" does not refer to a class or object of a class type", ref.posn);
        }
        if (ref.ref.decl instanceof MethodDecl) {
            identificationError("Cannot use method \"" + ref.ref.decl.name + "\" as head of a qualified reference", ref.posn);
        }
        ClassDecl refClassDecl = (ClassDecl) idTable.retrieveClass(((ClassType) ref.ref.decl.type).className.spelling);

        // Check whether the id is a field or method of the ref class.
        MemberDecl idDecl = null;
        for (FieldDecl fieldDecl : refClassDecl.fieldDeclList) {
            if (fieldDecl.name.equals(ref.id.spelling)) {
                idDecl = fieldDecl;
                break;
            }
        }
        for (MethodDecl methodDecl : refClassDecl.methodDeclList) {
            if (methodDecl.name.equals(ref.id.spelling)) {
                idDecl = methodDecl;
                break;
            }
        }
        if (idDecl == null) {
            identificationError("\"" + ref.id.spelling +
                    "\" is not a field or method of class \"" + refClassDecl.name + "\"", ref.id.posn);
        }
        ref.id.decl = idDecl;
        ref.decl = idDecl;
        // Check whether visibility and access of the id are valid
        if (refClassDecl != idTable.thisClassDecl && idDecl.isPrivate) {
            identificationError("Cannot access private member \"" + idDecl.name + "\" of class \"" +
                    refClassDecl.name + "\" from class \"" + idTable.thisClassDecl.name + "\".", ref.id.posn);
        }
        if (ref.ref instanceof IdRef && ((IdRef) ref.ref).id.spelling.equals(refClassDecl.name) && !idDecl.isStatic) {
            identificationError("\"" + idDecl.name + "\" is not a static member of class " +
                    refClassDecl.name, ref.id.posn);
        }
        return null;
    }

    @Override
    public Object visitIdentifier(Identifier id, Object arg) {
        id.decl = idTable.retrieve(id.spelling);
        if (id.decl == null) {
            identificationError("Cannot resolve symbol \"" + id.spelling + "\"", id.posn);
        }
        return null;
    }

    // The following methods are done.

    @Override
    public Object visitOperator(Operator op, Object arg) {
        return null;
    }

    @Override
    public Object visitIntLiteral(IntLiteral num, Object arg) {
        return null;
    }

    @Override
    public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
        return null;
    }

    @Override
    public Object visitNullLiteral(NullLiteral nullLiteral, Object arg) {
        return null;
    }
}
