package miniJava.CodeGenerator;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.Compiler;
import miniJava.ContextualAnalyzer.Identification;
import miniJava.mJAM.Machine;

import miniJava.mJAM.Machine.*;

import java.util.ArrayList;

public class CodeGenerator implements Visitor<Object, Object> {
    Package ast;
    int stackTop;
    ArrayList<Integer> addressesToPatch;
    ArrayList<MethodDecl> methodDeclsToPatchWith;

    public CodeGenerator(Package ast) {
        this.ast = ast;
        stackTop = 0;
        addressesToPatch = new ArrayList<>();
        methodDeclsToPatchWith = new ArrayList<>();
    }

    public void generateCode() {
        Machine.initCodeGen();
        ast.visit(this, null);
        for (int i = 0; i < addressesToPatch.size(); i++) {
            Machine.patch(addressesToPatch.get(i), ((MethodDescription) methodDeclsToPatchWith.get(i).runtimeDescription).cbOffset);
        }
    }

    @Override
    public Object visitPackage(Package prog, Object arg) {
        // Initialize static fields and annotate nonstatic fields and classes
        for (ClassDecl cd : prog.classDeclList) {
            cd.runtimeDescription = new ClassDescription();
            for (FieldDecl fd : cd.fieldDeclList) {
                fd.visit(this, cd);
            }
        }
        // Generate the main method
        Machine.emit(Op.LOADL, 0);
        Machine.emit(Prim.newarr);
        int mainCallAddress = Machine.nextInstrAddr();
        Machine.emit(Op.CALL, Reg.CB, -1);
        Machine.emit(Op.HALT);

        // Now we can visit the methods
        for (ClassDecl cd : prog.classDeclList) {
            for (MethodDecl md : cd.methodDeclList) {
                md.visit(this, null);
            }
        }

        // Patch all method call instructions.
        addToPatchList(mainCallAddress, prog.mainDecl);
        return null;
    }

    @Override
    public Object visitClassDecl(ClassDecl cd, Object arg) {
        // Done already.
        return null;
    }

    @Override
    public Object visitFieldDecl(FieldDecl fd, Object arg) {
        ClassDecl cd = (ClassDecl) arg;
        fd.runtimeDescription = new VarDescription(1);
        VarDescription fdDescription = (VarDescription) fd.runtimeDescription;
        if (fd.isStatic) {
            fdDescription.offset = stackTop;
            Machine.emit(Op.PUSH, 1);
            stackTop += fd.runtimeDescription.size;
        } else {
            fdDescription.offset = cd.runtimeDescription.size;
            cd.runtimeDescription.size += fd.runtimeDescription.size;
        }
        return null;
    }

    @Override
    public Object visitMethodDecl(MethodDecl md, Object arg) {
        md.runtimeDescription = new MethodDescription(Machine.nextInstrAddr());
        for (ParameterDecl pd : md.parameterDeclList) {
            pd.visit(this, md);
        }
        int size = md.statementList.size();
        if (size == 0 || !(md.statementList.get(size - 1) instanceof ReturnStmt)) {
            md.statementList.add(new ReturnStmt(null, null));
        }
        for (Statement st : md.statementList) {
            st.visit(this, md);
        }
        return null;
    }

    @Override
    public Object visitParameterDecl(ParameterDecl pd, Object arg) {
        MethodDecl md = (MethodDecl) arg;
        MethodDescription mdDescription = (MethodDescription) md.runtimeDescription;
        pd.runtimeDescription = new VarDescription(1);
        VarDescription pdDescription = (VarDescription) pd.runtimeDescription;
        pdDescription.offset = -md.parameterDeclList.size() + mdDescription.argSize;
        mdDescription.argSize += pd.runtimeDescription.size;
        return null;
    }

    @Override
    public Object visitVarDecl(VarDecl decl, Object arg) {
        MethodDecl md = (MethodDecl) arg;
        decl.runtimeDescription = new VarDescription(1);
        VarDescription declDescription = (VarDescription) decl.runtimeDescription;
        declDescription.offset = md.runtimeDescription.size;
        md.runtimeDescription.size += declDescription.size;
        return null;
    }

    @Override
    public Object visitBaseType(BaseType type, Object arg) {
        // Done
        return null;
    }

    @Override
    public Object visitClassType(ClassType type, Object arg) {
        // Done
        return null;
    }

    @Override
    public Object visitArrayType(ArrayType type, Object arg) {
        // Done
        return null;
    }

    @Override
    public Object visitBlockStmt(BlockStmt stmt, Object arg) {
        MethodDecl md = (MethodDecl) arg;
        // Save original offset from LB
        int originalSize = md.runtimeDescription.size;

        for (Statement st : stmt.sl) {
            st.visit(this, md);
        }
        // Remove variables in the block statement from the frame
        if (md.runtimeDescription.size - originalSize != 0) {
            Machine.emit(Op.POP, md.runtimeDescription.size - originalSize);
            md.runtimeDescription.size = originalSize;
        }
        return null;
    }

    @Override
    public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
        MethodDecl md = (MethodDecl) arg;
        stmt.varDecl.visit(this, md);
        stmt.initExp.visit(this, md);
        return null;
    }

    @Override
    public Object visitAssignStmt(AssignStmt stmt, Object arg) {
        MethodDecl md = (MethodDecl) arg;
        VarDescription varDesc = (VarDescription) stmt.ref.decl.runtimeDescription;

        if (stmt.ref instanceof IdRef) {
            stmt.val.visit(this, md);
            if (stmt.ref.decl instanceof FieldDecl) {
                FieldDecl fd = (FieldDecl) stmt.ref.decl;
                Machine.emit(Op.STORE, fd.isStatic ? Reg.SB : Reg.OB, varDesc.offset);
            } else if (stmt.ref.decl instanceof LocalDecl) {
                Machine.emit(Op.STORE, Reg.LB, varDesc.offset);
            }
        } else if (stmt.ref instanceof QualRef) {
            // stmt.ref is a variable that is a QualRef, hence it is a field variable.
            FieldDecl fd = (FieldDecl) stmt.ref.decl;
            if (fd.isStatic) {
                stmt.val.visit(this, md);
                Machine.emit(Op.STORE, Reg.SB, varDesc.offset);
            } else {
                QualRef lhsRef = (QualRef) stmt.ref;
                lhsRef.ref.visit(this, md);
                Machine.emit(Op.LOADL, ((VarDescription) lhsRef.id.decl.runtimeDescription).offset);
                stmt.val.visit(this, md);
                Machine.emit(Prim.fieldupd);
            }
        }
        return null;
    }

    @Override
    public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
        // Put address of array, element index, new value on stack
        stmt.ref.visit(this, null);
        stmt.ix.visit(this, null);
        stmt.exp.visit(this, null);
        Machine.emit(Prim.arrayupd);
        return null;
    }

    @Override
    public Object visitCallStmt(CallStmt stmt, Object arg) {
        MethodDecl md = (MethodDecl) arg;
        for (Expression argument : stmt.argList) {
            argument.visit(this, null);
        }
        MethodDecl calledMethod = (MethodDecl) stmt.methodRef.decl;
        if (calledMethod == Compiler.PRINTLN_DECL) {
            Machine.emit(Prim.putintnl);
        } else {
            // If the method is an instance method, we need to push the address of the object calling it onto the stack.
            if (!calledMethod.isStatic) {
                if (stmt.methodRef instanceof QualRef) {
                    ((QualRef) stmt.methodRef).ref.visit(this, null);
                } else {
                    // Implicit this.
                    Machine.emit(Op.LOADA, Reg.OB, 0);
                }
            }
            int methodCallAddress = Machine.nextInstrAddr();
            Machine.emit(calledMethod.isStatic ? Op.CALL : Op.CALLI, Reg.CB, -1);
            addToPatchList(methodCallAddress, calledMethod);
            if (calledMethod.type.typeKind != TypeKind.VOID) {
                Machine.emit(Op.POP, 1);
            }
        }
        return null;
    }

    @Override
    public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
        MethodDecl md = (MethodDecl) arg;
        if (stmt.returnExpr != null) {
            stmt.returnExpr.visit(this, null);
            Machine.emit(Op.RETURN, 1, 0, md.parameterDeclList.size());
        } else {
            Machine.emit(Op.RETURN, 0, 0, md.parameterDeclList.size());
        }
        return null;
    }

    @Override
    public Object visitIfStmt(IfStmt stmt, Object arg) {
        MethodDecl md = (MethodDecl) arg;
        if (stmt.elseStmt != null) {
            /* Java code:
            if (E)
               C_1
            else
               C_2
            */

            /* MJAM code:
            eval E
            JUMPIF(0) else
            execute C_1
            JUMP end
            else: execute C_2
            end:
            */
            stmt.cond.visit(this, md);

            int jumpElseInstrAddress = Machine.nextInstrAddr();
            Machine.emit(Op.JUMPIF, Machine.falseRep, Reg.CB, -1);

            stmt.thenStmt.visit(this, md);

            int jumpEndInstrAddress = Machine.nextInstrAddr();
            Machine.emit(Op.JUMP, Reg.CB, -1);

            int elseAddress = Machine.nextInstrAddr();
            stmt.elseStmt.visit(this, md);

            int endAddress = Machine.nextInstrAddr();

            Machine.patch(jumpElseInstrAddress, elseAddress);
            Machine.patch(jumpEndInstrAddress, endAddress);
        } else {
            stmt.cond.visit(this, md);

            int jumpEndInstrAddress = Machine.nextInstrAddr();
            Machine.emit(Op.JUMPIF, Machine.falseRep, Reg.CB, -1);

            stmt.thenStmt.visit(this, md);

            int endAddress = Machine.nextInstrAddr();

            Machine.patch(jumpEndInstrAddress, endAddress);
        }
        return null;
    }

    @Override
    public Object visitWhileStmt(WhileStmt stmt, Object arg) {
        MethodDecl md = (MethodDecl) arg;
        /* Java code:
        while (cond)
            stmt
         */

        /* MJAM code:
        JUMP test
        body: execute stmt
        test: eval cond
        JUMPIF(1) body
         */

        int jumpTestAddress = Machine.nextInstrAddr();
        Machine.emit(Op.JUMP, Reg.CB, -1);

        int bodyAddress = Machine.nextInstrAddr();
        stmt.body.visit(this, md);

        int testAddress = Machine.nextInstrAddr();
        stmt.cond.visit(this, md);

        Machine.emit(Op.JUMPIF, Machine.trueRep, Reg.CB, bodyAddress);

        Machine.patch(jumpTestAddress, testAddress);
        return null;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
        expr.expr.visit(this, null);

        switch (expr.operator.spelling) {
            case "!":
                Machine.emit(Prim.not);
                break;

            case "-":
                Machine.emit(Prim.neg);
                break;

            default:
                throw new RuntimeException("Unrecognized unary operator");

        }
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
        expr.left.visit(this, null);

        // Short circuit for logical operators.
        if (expr.operator.spelling.equals("&&")) {
            /* To implement short circuit for x && y:
             JUMPIF(false) SC
             evaluate y
             JUMP NSC
             SC: LOADL false
             NSC:
             */
            int toPatchSC = Machine.nextInstrAddr();
            Machine.emit(Op.JUMPIF, Machine.falseRep, Reg.CB, -1);

            expr.right.visit(this, null);

            int toPatchNSC = Machine.nextInstrAddr();
            Machine.emit(Op.JUMP, Reg.CB, -1);

            int scAddress = Machine.nextInstrAddr();
            Machine.emit(Op.LOADL, Machine.falseRep);

            Machine.patch(toPatchSC, scAddress);
            Machine.patch(toPatchNSC, Machine.nextInstrAddr());
            return null;
        } else if (expr.operator.spelling.equals("||")) {
            int toPatchSC = Machine.nextInstrAddr();
            Machine.emit(Op.JUMPIF, Machine.trueRep, Reg.CB, -1);

            expr.right.visit(this, null);

            int toPatchNSC = Machine.nextInstrAddr();
            Machine.emit(Op.JUMP, Reg.CB, -1);

            int scAddress = Machine.nextInstrAddr();
            Machine.emit(Op.LOADL, Machine.trueRep);

            Machine.patch(toPatchSC, scAddress);
            Machine.patch(toPatchNSC, Machine.nextInstrAddr());
            return null;
        }

        expr.right.visit(this, null);

        switch (expr.operator.spelling) {
            case "<":
                Machine.emit(Prim.lt);
                break;
            case ">":
                Machine.emit(Prim.gt);
                break;
            case "<=":
                Machine.emit(Prim.le);
                break;
            case ">=":
                Machine.emit(Prim.ge);
                break;
            case "+":
                Machine.emit(Prim.add);
                break;
            case "-":
                Machine.emit(Prim.sub);
                break;
            case "*":
                Machine.emit(Prim.mult);
                break;
            case "/":
                Machine.emit(Prim.div);
                break;
            case "==":
                Machine.emit(Prim.eq);
                break;
            case "!=":
                Machine.emit(Prim.ne);
                break;
            default:
                throw new RuntimeException();
        }
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
        Machine.emit(Prim.arrayref);
        return null;
    }

    private void addToPatchList(int address, MethodDecl md) {
        addressesToPatch.add(address);
        methodDeclsToPatchWith.add(md);
    }

    @Override
    public Object visitCallExpr(CallExpr expr, Object arg) {
        // Push each argument onto the stack
        for (Expression argument : expr.argList) {
            argument.visit(this, null);
        }

        MethodDecl md = (MethodDecl) expr.functionRef.decl;
        // If the method is an instance method, we need to push the address of the object calling it onto the stack.
        if (!md.isStatic) {
            if (expr.functionRef instanceof QualRef) {
                ((QualRef) expr.functionRef).ref.visit(this, null);
            } else {
                Machine.emit(Op.LOADA, Reg.OB, 0);
            }
        }

        int methodCallAddress = Machine.nextInstrAddr();
        Machine.emit(md.isStatic ? Op.CALL : Op.CALLI, Reg.CB, -1);
        addToPatchList(methodCallAddress, md);
        return null;
    }

    @Override
    public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
        expr.lit.visit(this, null);
        return null;
    }

    @Override
    public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
        int classSize = expr.classtype.className.decl.runtimeDescription.size;
        Machine.emit(Op.LOADL, -1);
        Machine.emit(Op.LOADL, classSize);
        Machine.emit(Prim.newobj);
        return null;
    }

    @Override
    public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
        expr.sizeExpr.visit(this, null);
        Machine.emit(Prim.newarr);
        return null;
    }

    @Override
    public Object visitThisRef(ThisRef ref, Object arg) {
        Machine.emit(Op.LOADA, Reg.OB, 0);
        return null;
    }

    @Override
    public Object visitIdRef(IdRef ref, Object arg) {
        if (ref.decl instanceof FieldDecl) {
            FieldDecl fd = (FieldDecl) ref.decl;
            Machine.emit(Op.LOAD, fd.isStatic ? Reg.SB : Reg.OB, ((VarDescription) fd.runtimeDescription).offset);
        } else if (ref.decl instanceof LocalDecl) {
            LocalDecl ld = (LocalDecl) ref.decl;
            Machine.emit(Op.LOAD, Reg.LB, ((VarDescription) ld.runtimeDescription).offset);
        }
        return null;
    }

    @Override
    public Object visitQRef(QualRef ref, Object arg) {
        if (ref.ref.decl instanceof ClassDecl) {
            // Access static field of a class or this.
            if (ref.id.decl instanceof FieldDecl && !ref.id.spelling.equals("out")) {
                FieldDecl fd = (FieldDecl) ref.id.decl;
                Machine.emit(Op.LOAD, fd.isStatic ? Reg.SB : Reg.OB, ((VarDescription) fd.runtimeDescription).offset);
            }
        } else {
            ref.ref.visit(this, null);
            MemberDecl memberDecl = (MemberDecl) ref.id.decl;
            if (memberDecl != Identification.LENGTH_DECL) {
                Machine.emit(Op.LOADL, ((VarDescription) memberDecl.runtimeDescription).offset);
                Machine.emit(Prim.fieldref);
            } else {
                Machine.emit(Prim.arraylen);
            }
        }
        return null;
    }

    @Override
    public Object visitIdentifier(Identifier id, Object arg) {
        // Done
        return null;
    }

    @Override
    public Object visitOperator(Operator op, Object arg) {
        // Done
        return null;
    }

    @Override
    public Object visitIntLiteral(IntLiteral num, Object arg) {
        Machine.emit(Op.LOADL, Integer.parseInt(num.spelling));
        return null;
    }

    @Override
    public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
        if (bool.spelling.equals("true")) {
            Machine.emit(Op.LOADL, Machine.trueRep);
        } else {
            Machine.emit(Op.LOADL, Machine.falseRep);
        }
        return null;
    }

    @Override
    public Object visitNullLiteral(NullLiteral nullLiteral, Object arg) {
        Machine.emit(Op.LOADL, Machine.nullRep);
        return null;
    }
}
