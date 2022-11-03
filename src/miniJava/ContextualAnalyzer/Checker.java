package miniJava.ContextualAnalyzer;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.SourcePosition;

import java.util.ArrayList;

public class Checker implements Visitor<Object, TypeDenoter> {
    ErrorReporter reporter;

    public Checker(ErrorReporter reporter) {
        this.reporter = reporter;
    }

    public void check(Package ast) {
        ast.visit(this, null);
    }

    private void updateMainMethods(ClassDecl cd, ArrayList<MethodDecl> mainMethods) {
        int count = 0;
        for (MethodDecl md : cd.methodDeclList) {
            if (md.name.equals("main") &&
                    !md.isPrivate &&
                    md.isStatic &&
                    md.type.typeKind == TypeKind.VOID &&
            md.parameterDeclList.size() == 1 &&
            md.parameterDeclList.get(0).type.toString().equals("String[]")) {
                mainMethods.add(md);
            }
        }
    }

    @Override
    public TypeDenoter visitPackage(Package prog, Object arg) {
        ArrayList<MethodDecl> mainMethods = new ArrayList<>();
        for (ClassDecl cd : prog.classDeclList) {
            updateMainMethods(cd, mainMethods);
            if (mainMethods.size() > 1) {
                reporter.reportError("Cannot have more than one main method in the program", prog.posn);
            }
        }
        if (mainMethods.size() == 0) {
            reporter.reportError("Expected a main method in the program, but did not find one", prog.posn);
            return null;
        }
        prog.mainDecl = mainMethods.get(0);
        for (ClassDecl cd : prog.classDeclList) {
            cd.visit(this, null);
        }
        return null;
    }

    @Override
    public TypeDenoter visitClassDecl(ClassDecl cd, Object arg) {
        for (FieldDecl fd : cd.fieldDeclList) {
            fd.visit(this, null);
        }
        for (MethodDecl md : cd.methodDeclList) {
            md.visit(this, null);
        }
        return cd.type;
    }

    @Override
    public TypeDenoter visitFieldDecl(FieldDecl fd, Object arg) {
        return fd.type;
    }

    @Override
    public TypeDenoter visitMethodDecl(MethodDecl md, Object arg) {
        for (ParameterDecl pd : md.parameterDeclList) {
            pd.visit(this, null);
        }
        for (Statement statement : md.statementList) {
            statement.visit(this, null);
        }
        if (md.type.typeKind != TypeKind.VOID &&
                (md.statementList.size() == 0 || !(md.statementList.get(md.statementList.size() - 1) instanceof ReturnStmt))) {
            reporter.reportError("Method \"" + md.name +
                    "\" must have a return statement as it's last statement", md.posn);
        }
        return null;
    }

    @Override
    public TypeDenoter visitParameterDecl(ParameterDecl pd, Object arg) {
        return pd.type;
    }

    @Override
    public TypeDenoter visitVarDecl(VarDecl decl, Object arg) {
        return decl.type;
    }

    @Override
    public TypeDenoter visitBaseType(BaseType type, Object arg) {
        return type;
    }

    @Override
    public TypeDenoter visitClassType(ClassType type, Object arg) {
        return type;
    }

    @Override
    public TypeDenoter visitArrayType(ArrayType type, Object arg) {
        return type;
    }

    @Override
    public TypeDenoter visitBlockStmt(BlockStmt stmt, Object arg) {
        for (Statement statement : stmt.sl) {
            statement.visit(this, null);
        }
        return null;
    }

    private boolean isObjectType(TypeDenoter type) {
        return type.typeKind == TypeKind.CLASS || type.typeKind == TypeKind.ARRAY;
    }

    // Returns whether assignment t1 = t2 allowed
    private boolean isAssignmentAllowed(TypeDenoter t1, TypeDenoter t2) {
        return t1.equals(t2) ||
                (isObjectType(t1) && t2.typeKind == TypeKind.NULL);
    }

    @Override
    public TypeDenoter visitVardeclStmt(VarDeclStmt stmt, Object arg) {
        TypeDenoter initExpType = stmt.initExp.visit(this, null);
        TypeDenoter varDeclType = stmt.varDecl.visit(this, null);
        if (!isAssignmentAllowed(varDeclType, initExpType)) {
            reporter.reportError("Required type: \"" + varDeclType + "\"; Provided: \"" +
                    initExpType + "\"", stmt.posn);
        }
        return null;
    }

    @Override
    public TypeDenoter visitAssignStmt(AssignStmt stmt, Object arg) {
        TypeDenoter valType = stmt.val.visit(this, null);
        TypeDenoter refType = stmt.ref.visit(this, null);
        if (stmt.ref.decl == Identification.LENGTH_DECL) {
            reporter.reportError("Cannot assign to length field of an array", stmt.posn);
        } else if (stmt.ref instanceof ThisRef) {
            reporter.reportError("Cannot assign to \"this\"", stmt.posn);
        } else if (stmt.ref.decl instanceof ClassDecl) {
            reporter.reportError("Cannot assign to the class \"" + stmt.ref.decl.name + "\"", stmt.posn);
        } else if (stmt.ref.decl instanceof MethodDecl) {
            reporter.reportError("Cannot assign to the method \"" + stmt.ref.decl.name + "\"", stmt.posn);
        } else if (!isAssignmentAllowed(refType, valType)) {
            reporter.reportError("Required type: \"" + refType + "\"; Provided: \"" +
                    valType + "\"", stmt.posn);
        }
        return null;
    }

    @Override
    public TypeDenoter visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
        TypeDenoter rhsType = stmt.exp.visit(this, null);
        TypeDenoter arrayType = stmt.ref.visit(this, null);
        TypeDenoter indexType = stmt.ix.visit(this, null);
        if (!indexType.typeKind.equals(TypeKind.INT)) {
            reporter.reportError("Array index must be an integer, but found a \"" + indexType +
                    "\"", stmt.ix.posn);
        }
        if (!(arrayType instanceof ArrayType)) {
            reporter.reportError("\"" + stmt.ref.decl.name + "\" is not an array type; found: \"" +
                    arrayType + "\"", stmt.ref.posn);
        } else if (!isAssignmentAllowed(((ArrayType) arrayType).eltType, rhsType)) {
            reporter.reportError("Type \"" + rhsType +
                    "\" of rhs of assignment does not match element type \"" + ((ArrayType) arrayType).eltType +
                    "\" of \"" + stmt.ref.decl.name + "\"", stmt.posn);
        }
        return null;
    }

    private boolean checkActualParameters(ExprList actualParameters, ParameterDeclList formalParameters, SourcePosition posn) {
        if (formalParameters.size() != actualParameters.size()) {
            reporter.reportError("Expected " + formalParameters +
                    " actual parameters but encountered " + actualParameters.size() + " actual parameters", posn);
            return false;
        }
        for (int i = 0; i < actualParameters.size(); i++) {
            if (!isAssignmentAllowed(formalParameters.get(i).type, actualParameters.get(i).type)) {
                reporter.reportError("Expected actual parameter " + i + " to have type \"" +
                        formalParameters.get(i).type + "\" but found type \"" + actualParameters.get(i).type +
                        "\" instead", posn);
                return false;
            }
        }
        return true;
    }

    @Override
    public TypeDenoter visitCallStmt(CallStmt stmt, Object arg) {
        // Need to check that the args to the call match the formal parameters of the method
        for (Expression expr : stmt.argList) {
            expr.visit(this, null);
        }
        MethodDecl methodDecl = (MethodDecl) stmt.methodRef.decl;
        ParameterDeclList formalParameters = methodDecl.parameterDeclList;
        ExprList actualParameters = stmt.argList;
        checkActualParameters(actualParameters, formalParameters, stmt.posn);
        return null;
    }

    @Override
    public TypeDenoter visitReturnStmt(ReturnStmt stmt, Object arg) {
        if (stmt.returnExpr != null) {
            stmt.returnExpr.visit(this, null);
            if (!isAssignmentAllowed(stmt.methodDecl.type, stmt.returnExpr.type)) {
                reporter.reportError("Expected return type \"" + stmt.methodDecl.type +
                        "\" but found type \"" + stmt.returnExpr.type + "\".", stmt.posn);
            }
        } else {
            if (stmt.methodDecl.type.typeKind != TypeKind.VOID) {
                reporter.reportError("Method \"" + stmt.methodDecl.name + "\" expects return of type \""
                        + stmt.methodDecl.type + "\" but nothing was returned.", stmt.posn);
            }
        }
        return null;
    }

    @Override
    public TypeDenoter visitIfStmt(IfStmt stmt, Object arg) {
        TypeDenoter condType = stmt.cond.visit(this, null);
        if (condType.typeKind != TypeKind.BOOLEAN) {
            reporter.reportError("Condition in if statement is of type \"" + condType +
                    "\" but must be boolean", stmt.posn);
        }
        stmt.thenStmt.visit(this, null);
        if (stmt.thenStmt instanceof VarDeclStmt) {
            reporter.reportError("A variable declaration cannot be the solitary statement in a branch of a conditional statement.", stmt.posn);
        }
        if (stmt.elseStmt != null) {
            stmt.elseStmt.visit(this, null);
            if (stmt.elseStmt instanceof VarDeclStmt) {
                reporter.reportError("A variable declaration cannot be the solitary statement in a branch of a conditional statement.", stmt.posn);
            }
        }
        return null;
    }

    @Override
    public TypeDenoter visitWhileStmt(WhileStmt stmt, Object arg) {
        TypeDenoter condType = stmt.cond.visit(this, null);
        if (condType.typeKind != TypeKind.BOOLEAN) {
            reporter.reportError("Condition in while loop is of type \"" + condType +
                    "\" but must be boolean", stmt.posn);
        }
        stmt.body.visit(this, null);
        if (stmt.body instanceof VarDeclStmt) {
            reporter.reportError("A variable declaration cannot be the solitary statement in a while loop body.", stmt.posn);
        }
        return null;
    }

    @Override
    public TypeDenoter visitUnaryExpr(UnaryExpr expr, Object arg) {
        String opSpelling = expr.operator.spelling;
        TypeDenoter operandType = expr.expr.visit(this, null);
        if (opSpelling.equals("!")) {
            if (operandType.typeKind != TypeKind.BOOLEAN) {
                reporter.reportError("Operator \"!\" cannot be applied to \"" + operandType + "\"", expr.posn);
                expr.type = new BaseType(TypeKind.ERROR, expr.posn);
            } else {
                expr.type = new BaseType(TypeKind.BOOLEAN, expr.posn);
            }
        } else if (opSpelling.equals("-")) {
            if (operandType.typeKind != TypeKind.INT) {
                reporter.reportError("Operator \"-\" cannot be applied to \"" + operandType + "\"", expr.posn);
                expr.type = new BaseType(TypeKind.ERROR, expr.posn);
            } else {
                expr.type = new BaseType(TypeKind.INT, expr.posn);
            }
        }
        return expr.type;
    }

    @Override
    public TypeDenoter visitBinaryExpr(BinaryExpr expr, Object arg) {
        TypeDenoter expr1Type = expr.left.visit(this, null);
        TypeDenoter expr2Type = expr.right.visit(this, null);
        switch (expr.operator.spelling) {
            case "<":
            case ">":
            case "<=":
            case ">=":
                if (expr1Type.typeKind != TypeKind.INT || expr2Type.typeKind != TypeKind.INT) {
                    reporter.reportError("Operator \"" + expr.operator.spelling +
                            "\" cannot be applied to \"" + expr1Type + "\", \"" + expr2Type + "\"", expr.posn);
                    expr.type = new BaseType(TypeKind.ERROR, expr.posn);
                } else {
                    expr.type = new BaseType(TypeKind.BOOLEAN, expr.posn);
                }
                break;
            case "+":
            case "-":
            case "*":
            case "/":
                if (expr1Type.typeKind != TypeKind.INT || expr2Type.typeKind != TypeKind.INT) {
                    reporter.reportError("Operator \"" + expr.operator.spelling +
                            "\" cannot be applied to \"" + expr1Type + "\", \"" + expr2Type + "\"", expr.posn);
                    expr.type = new BaseType(TypeKind.ERROR, expr.posn);
                } else {
                    expr.type = new BaseType(TypeKind.INT, expr.posn);
                }
                break;
            case "==":
            case "!=":
                if (!expr1Type.equals(expr2Type) && !(isObjectType(expr1Type) &&
                        expr2Type.typeKind == TypeKind.NULL) && !(isObjectType(expr2Type) &&
                        expr1Type.typeKind == TypeKind.NULL)) {
                    reporter.reportError("Operator \"" + expr.operator.spelling +
                            "\" cannot be applied to \"" + expr1Type + "\", \"" + expr2Type + "\"", expr.posn);
                    expr.type = new BaseType(TypeKind.ERROR, expr.posn);
                } else {
                   // boolean x =  "" == new Integer(3);
                    expr.type = new BaseType(TypeKind.BOOLEAN, expr.posn);
                }
                break;
            case "&&":
            case "||":
                if (expr1Type.typeKind != TypeKind.BOOLEAN || expr2Type.typeKind != TypeKind.BOOLEAN) {
                    reporter.reportError("Operator \"" + expr.operator.spelling +
                            "\" cannot be applied to \"" + expr1Type + "\", \"" + expr2Type + "\"", expr.posn);
                    expr.type = new BaseType(TypeKind.ERROR, expr.posn);
                } else {
                    expr.type = new BaseType(TypeKind.BOOLEAN, expr.posn);
                }
                break;
            default:
                throw new RuntimeException();
        }
        return expr.type;
    }

    @Override
    public TypeDenoter visitRefExpr(RefExpr expr, Object arg) {
        boolean error = false;
        if (expr.ref.decl instanceof MethodDecl) {
            reporter.reportError("The method \"" + expr.ref.decl.name + "\" is not an expression", expr.posn);
            expr.type = new BaseType(TypeKind.ERROR, expr.posn);
            error = true;
        } else if (expr.ref.decl instanceof ClassDecl && expr.ref instanceof IdRef &&
                ((IdRef) expr.ref).id.spelling.equals(expr.ref.decl.name)) {
            reporter.reportError("The class name \"" + expr.ref.decl.name +
                    "\" is not an expression", expr.posn);
            expr.type = new BaseType(TypeKind.ERROR, expr.posn);
            error = true;
        }
        TypeDenoter refType = expr.ref.visit(this, null);
        if (!error) {
            expr.type = refType;
        }
        return expr.type;
    }

    @Override
    public TypeDenoter visitIxExpr(IxExpr expr, Object arg) {
        TypeDenoter arrayType = expr.ref.visit(this, null);
        TypeDenoter indexType = expr.ixExpr.visit(this, null);
        boolean error = false;
        if (!indexType.typeKind.equals(TypeKind.INT)) {
            reporter.reportError("Array index must have int type but is \"" + indexType + "\"", expr.ixExpr.posn);
            expr.type = new BaseType(TypeKind.ERROR, expr.posn);
            error = true;
        }
        if (!(arrayType instanceof ArrayType)) {
            reporter.reportError("Array type expected; found: \"" + arrayType + "\"", expr.ref.posn);
            expr.type = new BaseType(TypeKind.ERROR, expr.posn);
            error = true;
        }
        if (!error) {
            expr.type = ((ArrayType) arrayType).eltType;
        }
        return expr.type;
    }

    @Override
    public TypeDenoter visitCallExpr(CallExpr expr, Object arg) {
        // Need to check that the args to the call match the formal parameters of the method
        for (Expression argument : expr.argList) {
            argument.visit(this, null);
        }
        MethodDecl methodDecl = (MethodDecl) expr.functionRef.decl;
        ParameterDeclList formalParameters = methodDecl.parameterDeclList;
        ExprList actualParameters = expr.argList;
        if (!checkActualParameters(actualParameters, formalParameters, expr.posn)) {
            expr.type = new BaseType(TypeKind.ERROR, expr.posn);
        } else {
            expr.type = methodDecl.type;
        }
        return expr.type;
    }

    @Override
    public TypeDenoter visitLiteralExpr(LiteralExpr expr, Object arg) {
        // The literal tokens are TRUE, FALSE, NULL, NUM
        expr.type = expr.lit.visit(this, null);
        return expr.type;
    }

    @Override
    public TypeDenoter visitNewObjectExpr(NewObjectExpr expr, Object arg) {
        expr.type = expr.classtype;
        return expr.type;
    }

    @Override
    public TypeDenoter visitNewArrayExpr(NewArrayExpr expr, Object arg) {
        TypeDenoter sizeType = expr.sizeExpr.visit(this, null);
        if (sizeType.typeKind != TypeKind.INT) {
            reporter.reportError("Array size must be an integer but a \"" + sizeType +
                    "\" was provided", expr.posn);
            return new BaseType(TypeKind.ERROR, expr.posn);
        }
        expr.type = new ArrayType(expr.eltType, expr.posn);
        return expr.type;
    }

    @Override
    public TypeDenoter visitThisRef(ThisRef ref, Object arg) {
        ref.type = ref.decl.type;
        return ref.type;
    }

    @Override
    public TypeDenoter visitIdRef(IdRef ref, Object arg) {
        ref.type = ref.decl.type;
        return ref.type;
    }

    @Override
    public TypeDenoter visitQRef(QualRef ref, Object arg) {
        ref.type = ref.decl.type;
        return ref.type;
    }

    @Override
    public TypeDenoter visitIdentifier(Identifier id, Object arg) {
        id.type = id.decl.type;
        return id.type;
    }

    @Override
    public TypeDenoter visitOperator(Operator op, Object arg) {
        return null;
    }

    @Override
    public TypeDenoter visitIntLiteral(IntLiteral num, Object arg) {
        num.type = new BaseType(TypeKind.INT, num.posn);
        return num.type;
    }

    @Override
    public TypeDenoter visitBooleanLiteral(BooleanLiteral bool, Object arg) {
        bool.type = new BaseType(TypeKind.BOOLEAN, bool.posn);
        return bool.type;
    }

    @Override
    public TypeDenoter visitNullLiteral(NullLiteral nullLiteral, Object arg) {
        nullLiteral.type = new BaseType(TypeKind.NULL, nullLiteral.posn);
        return nullLiteral.type;
    }
}
