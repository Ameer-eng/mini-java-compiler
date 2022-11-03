package miniJava;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.CodeGenerator.CodeGenerator;
import miniJava.ContextualAnalyzer.Checker;
import miniJava.ContextualAnalyzer.Identification;
import miniJava.SyntacticAnalyzer.*;
import miniJava.mJAM.Disassembler;
import miniJava.mJAM.Interpreter;
import miniJava.mJAM.ObjectFile;

public class Compiler {
    private static ClassDecl getSystemClassDecl() {
        FieldDeclList fieldDecls = new FieldDeclList();
        SourcePosition pos = new SourcePosition();
        ClassType PrintStreamType = new ClassType(new Identifier(new Token(Token.TokenKind.CLASS, "_PrintStream", pos)), pos);
        FieldDecl outField = new FieldDecl(false, true, PrintStreamType, "out", pos);
        fieldDecls.add(outField);
        return new ClassDecl("System", fieldDecls, new MethodDeclList(), pos);
    }

    public static MethodDecl PRINTLN_DECL;

    private static ClassDecl getPrintstreamClassDecl() {
        MethodDeclList methodDecls = new MethodDeclList();
        SourcePosition pos = new SourcePosition();
        FieldDecl printlnField = new FieldDecl(false, false, new BaseType(TypeKind.VOID, pos),
                "println", pos);
        ParameterDeclList params = new ParameterDeclList();
        params.add(new ParameterDecl(new BaseType(TypeKind.INT, pos), "n", pos));
        MethodDecl printlnDecl = new MethodDecl(printlnField, params, new StatementList(), pos);
        methodDecls.add(printlnDecl);
        PRINTLN_DECL = printlnDecl;
        return new ClassDecl("_PrintStream", new FieldDeclList(), methodDecls, pos);
    }

    private static ClassDecl getStringClassDecl() {
        return new ClassDecl("String", new FieldDeclList(), new MethodDeclList(), new SourcePosition());
    }

    public static boolean debug = true;

    public static void main(String[] args) {
        System.out.println("Syntactic analysis ... ");
        SourceFile sourceFile = new SourceFile(args[0]);

        ErrorReporter reporter = new ErrorReporter();
        Scanner scanner = new Scanner(sourceFile, reporter);
        Parser parser = new Parser(scanner, reporter);
        Identification identification = new Identification(reporter);
        Checker typeChecker = new Checker(reporter);

        Package ast = parser.parse();
        System.out.print("Syntactic analysis complete:  ");
        if (!reporter.hasErrors()) {
            System.out.println("Identification ...");
            ClassDecl systemDecl = getSystemClassDecl();
            ClassDecl printstreamDecl = getPrintstreamClassDecl();
            ClassDecl stringDecl = getStringClassDecl();
            ast.classDeclList.add(systemDecl);
            ast.classDeclList.add(printstreamDecl);
            ast.classDeclList.add(stringDecl);
            identification.identify(ast);
        }
        if (!reporter.hasErrors()) {
            System.out.println("Type checking ...");
            typeChecker.check(ast);
        }
        if (!reporter.hasErrors()) {
            System.out.println("Code generation ...");
            CodeGenerator codeGenerator = new CodeGenerator(ast);
            codeGenerator.generateCode();
            String outputFileName = args[0].substring(0, args[0].indexOf('.')) + ".mJAM";
            ObjectFile objF = new ObjectFile(outputFileName);
            if (objF.write()) {
                System.out.println("Codegen failed");
            } else {
                System.out.println("Codegen succeeded");
            }

            if (debug) {
                // create asm file corresponding to object code using disassembler
                String asmCodeFileName = outputFileName.replace(".mJAM", ".asm");
                System.out.print("Writing assembly file " + asmCodeFileName + " ... ");
                Disassembler d = new Disassembler(outputFileName);
                if (d.disassemble()) {
                    System.out.println("FAILED!");
                    return;
                } else
                    System.out.println("SUCCEEDED");

                /*
                 * run code using debugger
                 *
                 */
                System.out.println("Running code in debugger ... ");
                Interpreter.debug(outputFileName, asmCodeFileName);

                System.out.println("*** mJAM execution completed");
            }
        }

        if (reporter.hasErrors()) {
            System.out.println("Invalid miniJava program");
            // return code for invalid input
            System.exit(4);
        } else {
            System.out.println("valid miniJava program");
            //ASTDisplay.showPosition = true;
            //(new ASTDisplay()).showTree(ast);
            // return code for valid input
            System.exit(0);
        }
    }
}
