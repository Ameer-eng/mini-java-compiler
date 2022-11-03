package miniJava.SyntacticAnalyzer;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.Token.TokenKind;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static miniJava.SyntacticAnalyzer.Token.TokenKind.*;

public class Parser {
    private Scanner scanner;
    private ErrorReporter reporter;
    private Token token;
    private SourcePosition previousTokenPosition;
    private boolean trace = true;

    HashSet<TokenKind> fieldDeclarationOrMethodDeclarationStarters = new HashSet<>(Arrays.asList(
            PUBLIC, PRIVATE, STATIC, BOOLEAN, ID, INT, VOID));
    HashSet<TokenKind> statementStarters = new HashSet<>(Arrays.asList(LCURLY, BOOLEAN, ID, INT, THIS, RETURN, IF,
            WHILE));
    HashSet<TokenKind> parameterListStarters = new HashSet<>(Arrays.asList(BOOLEAN, ID, INT));
    HashSet<TokenKind> typeStarters = new HashSet<>(Arrays.asList(BOOLEAN, ID, INT));
    HashSet<TokenKind> expressionStarters = new HashSet<>(Arrays.asList(ID, THIS, UNOP, LPAREN, NUM, TRUE, FALSE, NULL, NEW));

    private final HashMap<String, Integer> precedence = new HashMap<>();
    private static final int BOTTOM_PRECEDENCE = 7;

    public Parser(Scanner scanner, ErrorReporter reporter) {
        this.scanner = scanner;
        this.reporter = reporter;
        previousTokenPosition = new SourcePosition();
        precedence.put("||", 1);
        precedence.put("&&", 2);
        precedence.put("==", 3);
        precedence.put("!=", 3);
        precedence.put("<=", 4);
        precedence.put("<", 4);
        precedence.put(">", 4);
        precedence.put(">=", 4);
        precedence.put("+", 5);
        precedence.put("-", 5);
        precedence.put("*", 6);
        precedence.put("/", 6);
    }

    /**
     * SyntaxError is used to unwind parse stack when parse fails
     */
    class SyntaxError extends Error {
        private static final long serialVersionUID = 1L;
    }

    /**
     * start records the position of the start of a phrase.
     * This is defined to be the position of the first
     * character of the first token of the phrase.
     *
     * @param position
     */
    void start(SourcePosition position) {
        position.start = token.position.start;
    }

    /**
     * finish records the position of the end of a phrase.
     * This is defined to be the position of the last
     * character of the last token of the phrase.
     *
     * @param position
     */
    void finish(SourcePosition position) {
        position.finish = previousTokenPosition.finish;
    }

    SourcePosition sp(int startPosition) {
        return new SourcePosition(startPosition, previousTokenPosition.finish);
    }

    /**
     * parse input, catch possible parse error
     */
    public Package parse() {
        previousTokenPosition.start = 0;
        previousTokenPosition.finish = 0;
        token = scanner.scan();
        try {
            return parseProgram();
        } catch (SyntaxError e) {
            return null;
        }
    }

    // Parse the rules
    private Package parseProgram() {
        int startPosition = token.position.start;
        ClassDeclList classDeclarations = new ClassDeclList();
        while (token.kind == TokenKind.CLASS) {
            classDeclarations.add(parseClassDeclaration());
        }
        accept(TokenKind.EOT);
        return new Package(classDeclarations, sp(startPosition));
    }

    private ParameterDeclList parseParameterList() {
        ParameterDeclList parameterList = new ParameterDeclList();
        if (parameterListStarters.contains(token.kind)) {
            int startPosition = token.position.start;
            TypeDenoter parameterType = parseType();
            String parameterName = token.spelling;
            accept(ID);
            parameterList.add(new ParameterDecl(parameterType, parameterName, sp(startPosition)));
            while (token.kind == COMMA) {
                acceptIt();
                startPosition = token.position.start;
                parameterType = parseType();
                parameterName = token.spelling;
                accept(ID);
                parameterList.add(new ParameterDecl(parameterType, parameterName, sp(startPosition)));
            }
        }
        return parameterList;
    }

    // statement*
    private StatementList parseStatementList() {
        StatementList statements = new StatementList();
        while (statementStarters.contains(token.kind)) {
            statements.add(parseStatement());
        }
        return statements;
    }

    private ClassDecl parseClassDeclaration() {
        int classStartPosition = token.position.start;

        FieldDeclList fieldDeclarations = new FieldDeclList();
        MethodDeclList methodDeclarations = new MethodDeclList();
        accept(TokenKind.CLASS);
        String className = token.spelling;
        accept(ID);
        accept(TokenKind.LCURLY);
        while (fieldDeclarationOrMethodDeclarationStarters.contains(token.kind)) {
            int declarationStartPosition = token.position.start;
            boolean isPrivate = false;
            boolean isStatic = false;
            TypeDenoter type;
            String name;
            if (token.kind == TokenKind.PUBLIC || token.kind == TokenKind.PRIVATE) {
                isPrivate = token.kind == PRIVATE;
                acceptIt();
            }
            if (token.kind == TokenKind.STATIC) {
                isStatic = true;
                acceptIt();
            }
            if (token.kind == TokenKind.VOID) {
                acceptIt();
                type = new BaseType(TypeKind.VOID, sp(declarationStartPosition));

                name = token.spelling;
                accept(TokenKind.ID);

                FieldDecl methodDeclaration = new FieldDecl(isPrivate, isStatic, type, name, sp(declarationStartPosition));
                accept(TokenKind.LPAREN);
                ParameterDeclList parameters = parseParameterList();
                accept(RPAREN);
                accept(LCURLY);
                StatementList statements = parseStatementList();
                accept(RCURLY);
                methodDeclarations.add(new MethodDecl(methodDeclaration, parameters, statements, sp(declarationStartPosition)));
            } else if (typeStarters.contains(token.kind)) {
                type = parseType();

                name = token.spelling;
                accept(TokenKind.ID);
                if (token.kind == SEMICOLON) {
                    acceptIt();
                    fieldDeclarations.add(new FieldDecl(isPrivate, isStatic, type, name, sp(declarationStartPosition)));
                } else if (token.kind == LPAREN) {
                    FieldDecl methodDeclaration = new FieldDecl(isPrivate, isStatic, type, name, sp(declarationStartPosition));
                    acceptIt();
                    ParameterDeclList parameters = parseParameterList();
                    accept(RPAREN);
                    accept(LCURLY);
                    StatementList statements = parseStatementList();
                    accept(RCURLY);
                    methodDeclarations.add(new MethodDecl(methodDeclaration, parameters, statements, sp(declarationStartPosition)));
                } else {
                    parseError("Expected SEMICOLON or LPAREN but Found " + token.kind);
                }
            } else {
                parseError("Expected BOOLEAN or INT or ID or VOID but Found " + token.kind);
            }
        }
        accept(RCURLY);
        return new ClassDecl(className, fieldDeclarations, methodDeclarations, sp(classStartPosition));
    }


    private TypeDenoter parseType() {
        int startPosition = token.position.start;

        if (token.kind == BOOLEAN) {
            acceptIt();
            BaseType booleanType = new BaseType(TypeKind.BOOLEAN, sp(startPosition));
            return booleanType;
        } else if (token.kind == INT) {
            acceptIt();
            TypeDenoter intType = new BaseType(TypeKind.INT, sp(startPosition));
            if (token.kind == LBRACKET) {
                acceptIt();
                accept(RBRACKET);
                return new ArrayType(intType, sp(startPosition));
            } else {
                return intType;
            }
        } else if (token.kind == ID) {
            Identifier className = new Identifier(token);
            acceptIt();
            TypeDenoter classType = new ClassType(className, sp(startPosition));
            if (token.kind == LBRACKET) {
                acceptIt();
                accept(RBRACKET);
                return new ArrayType(classType, sp(startPosition));
            } else {
                return classType;
            }
        } else {
            parseError("Expected BOOLEAN or ID or INT but found " + token.kind);
            return null;
        }
    }

    private ExprList parseArgumentList() {
        ExprList expressions = new ExprList();
        if (expressionStarters.contains(token.kind) || token.spelling.equals("-")) {
            expressions.add(parseExpression());
            while (token.kind == COMMA) {
                acceptIt();
                expressions.add(parseExpression());
            }
        }
        return expressions;
    }

    // (id | this) (. id)*
    private Reference parseReference() {
        int startPosition = token.position.start;
        Reference reference = null;
        if (token.kind == ID) {
            Identifier id = new Identifier(token);
            acceptIt();
            reference = new IdRef(id, sp(startPosition));
        } else if (token.kind == THIS) {
            acceptIt();
            reference = new ThisRef(sp(startPosition));
        } else {
            parseError("Expected ID or THIS but found " + token.kind);
        }
        while (token.kind == DOT) {
            acceptIt();
            Identifier id = new Identifier(token);
            accept(ID);
            reference = new QualRef(reference, id, sp(startPosition));
        }
        return reference;
    }

    private Statement parseStatementAfterReference(Reference ref, int startPosition) {
        switch (token.kind) {
            // assign statement
            case EQUALS:
                acceptIt();
                Expression expr = parseExpression();
                accept(SEMICOLON);
                return new AssignStmt(ref, expr, sp(startPosition));

            case LBRACKET:
                acceptIt();
                Expression indexExpr = parseExpression();
                accept(RBRACKET);
                accept(EQUALS);
                Expression rhsExpr = parseExpression();
                accept(SEMICOLON);
                return new IxAssignStmt(ref, indexExpr, rhsExpr, sp(startPosition));

            case LPAREN:
                acceptIt();
                ExprList argumentList = parseArgumentList();
                accept(RPAREN);
                accept(SEMICOLON);
                return new CallStmt(ref, argumentList, sp(startPosition));

            default:
                parseError("Expected EQUALS or LBRACKET or LPAREN but found " + token.kind);
                return null;
        }
    }

    private Statement parseStatement() {
        int startPosition = token.position.start;
        switch (token.kind) {
            case LCURLY:
                acceptIt();
                StatementList statements = parseStatementList();
                accept(RCURLY);
                return new BlockStmt(statements, sp(startPosition));

            case RETURN:
                acceptIt();
                Expression expression = null;
                if (expressionStarters.contains(token.kind) || token.spelling.equals("-")) {
                    expression = parseExpression();
                }
                accept(SEMICOLON);
                return new ReturnStmt(expression, sp(startPosition));

            case IF:
                acceptIt();
                accept(LPAREN);
                Expression ifExpression = parseExpression();
                accept(RPAREN);
                Statement thenStatement = parseStatement();
                if (token.kind == ELSE) {
                    acceptIt();
                    Statement elseStatement = parseStatement();
                    return new IfStmt(ifExpression, thenStatement, elseStatement, sp(startPosition));
                }
                return new IfStmt(ifExpression, thenStatement, sp(startPosition));

            case WHILE:
                acceptIt();
                accept(LPAREN);
                Expression condition = parseExpression();
                accept(RPAREN);
                Statement body = parseStatement();
                return new WhileStmt(condition, body, sp(startPosition));

            // Type id = Expression;
            // Where Type starts with INT or BOOLEAN, as opposed to id.
            case BOOLEAN:
            case INT: {
                TypeDenoter varType = parseType();
                String varName = token.spelling;
                accept(ID);
                VarDecl varDeclaration = new VarDecl(varType, varName, sp(startPosition));
                accept(EQUALS);
                Expression expr = parseExpression();
                accept(SEMICOLON);
                return new VarDeclStmt(varDeclaration, expr, sp(startPosition));
            }

            // Reference ...
            case THIS:
                Reference ref = parseReference();
                return parseStatementAfterReference(ref, startPosition);

            // Handle start with id.
            case ID:
                Token idToken = token;
                acceptIt();
                switch (token.kind) {
                    // (Type === id) id = Expression;
                    case ID: {
                        TypeDenoter varType = new ClassType(new Identifier(idToken), sp(startPosition));
                        String varName = token.spelling;
                        acceptIt();
                        VarDecl varDeclaration = new VarDecl(varType, varName, sp(startPosition));
                        accept(EQUALS);
                        Expression expr = parseExpression();
                        accept(SEMICOLON);
                        return new VarDeclStmt(varDeclaration, expr, sp(startPosition));
                    }

                    case LBRACKET:
                        acceptIt();
                        if (expressionStarters.contains(token.kind) || token.spelling.equals("-")) {
                            // (Reference === id) [ Expression ] = Expression ;
                            IdRef idRef = new IdRef(new Identifier(idToken), idToken.position);
                            Expression indexExpr = parseExpression();
                            accept(RBRACKET);
                            accept(EQUALS);
                            Expression assignExpr = parseExpression();
                            accept(SEMICOLON);
                            return new IxAssignStmt(idRef, indexExpr, assignExpr, sp(startPosition));
                        } else {
                            // (Type === id [ ]) id = Expression ;
                            ClassType classType = new ClassType(new Identifier(idToken), idToken.position);
                            accept(RBRACKET);
                            ArrayType arrayType = new ArrayType(classType, sp(startPosition));
                            String varName = token.spelling;
                            accept(ID);
                            VarDecl varDeclaration = new VarDecl(arrayType, varName, sp(startPosition));
                            accept(EQUALS);
                            Expression expr = parseExpression();
                            accept(SEMICOLON);
                            return new VarDeclStmt(varDeclaration, expr, sp(startPosition));
                        }

                    // (Reference === id (. id)* afterReference
                    case DOT: {
                        Reference reference = new IdRef(new Identifier(idToken), idToken.position);
                        while (token.kind == DOT) {
                            acceptIt();
                            Identifier id = new Identifier(token);
                            accept(ID);
                            reference = new QualRef(reference, id, sp(startPosition));
                        }
                        return parseStatementAfterReference(reference, startPosition);
                    }

                    // (Reference === id) afterReference
                    case EQUALS:
                    case LPAREN:
                        Reference reference = new IdRef(new Identifier(idToken), idToken.position);
                        return parseStatementAfterReference(reference, startPosition);
                }
                break;

            default:
                parseError(statementStarters, token.kind);
        }
        return null;
    }

    private Expression parseExpression() {
        return parseExpression(1);
    }

    // Parse E_p = E_(p + 1) (op_p E_(p + 1))*
    private Expression parseExpression(int p) {
        if (p == BOTTOM_PRECEDENCE) {
            return parseBottomExpression();
        }
        int startPosition = token.position.start;
        Expression expr = parseExpression(p + 1);
        while (token.kind == BINOP && precedence.get(token.spelling) == p) {
            Operator op = new Operator(token);
            acceptIt();
            Expression expr2 = parseExpression(p + 1);
            expr = new BinaryExpr(op, expr, expr2, sp(startPosition));
        }
        return expr;
    }

    private Expression parseBottomExpression() {
        int startPosition = token.position.start;
        switch (token.kind) {
            // Reference ...
            case ID:
            case THIS:
                Reference reference = parseReference();
                switch (token.kind) {
                    case LBRACKET:
                        acceptIt();
                        Expression expr = parseExpression();
                        accept(RBRACKET);
                        return new IxExpr(reference, expr, sp(startPosition));

                    case LPAREN:
                        acceptIt();
                        ExprList argumentList = parseArgumentList();
                        accept(RPAREN);
                        return new CallExpr(reference, argumentList, sp(startPosition));

                    default:
                        return new RefExpr(reference, sp(startPosition));
                }

            // unop ...
            case UNOP: {
                Operator unop = new Operator(token);
                acceptIt();
                Expression expr = parseBottomExpression();
                return new UnaryExpr(unop, expr, sp(startPosition));
            }

            // Handle unary minus, which is scanned as a binop, but should be a unop.
            case BINOP:
                if (token.spelling.equals("-")) {
                    token.kind = UNOP;
                    Operator unop = new Operator(token);
                    acceptIt();
                    Expression expr = parseBottomExpression();
                    return new UnaryExpr(unop, expr, sp(startPosition));
                } else {
                    parseError(expressionStarters, BINOP);
                }
                break;

            // ( Expression )
            case LPAREN: {
                acceptIt();
                Expression expr = parseExpression();
                accept(RPAREN);
                return expr;
            }

            case NUM:
            case TRUE:
            case FALSE:
            case NULL:
                Terminal literal;
                if (token.kind == NUM) {
                    literal = new IntLiteral(token);
                } else if (token.kind == NULL) {
                    literal = new NullLiteral(token);
                } else {
                    literal = new BooleanLiteral(token);
                }
                acceptIt();
                return new LiteralExpr(literal, sp(startPosition));

            case NEW:
                acceptIt();
                switch (token.kind) {
                    case INT: {
                        BaseType intType = new BaseType(TypeKind.INT, token.position);
                        acceptIt();
                        accept(LBRACKET);
                        Expression expr = parseExpression();
                        accept(RBRACKET);
                        return new NewArrayExpr(intType, expr, sp(startPosition));
                    }

                    case ID:
                        ClassType classType = new ClassType(new Identifier(token), token.position);
                        acceptIt();
                        switch (token.kind) {
                            case LPAREN:
                                acceptIt();
                                accept(RPAREN);
                                return new NewObjectExpr(classType, sp(startPosition));

                            case LBRACKET:
                                acceptIt();
                                Expression expr = parseExpression();
                                accept(RBRACKET);
                                return new NewArrayExpr(classType, expr, sp(startPosition));

                            default:
                                parseError("Expected LPAREN or LBRACKET but found " + token.kind);
                        }
                        break;

                    default:
                        parseError("Expected INT or ID but found " + token.kind);

                }
                break;

            default:
                parseError(expressionStarters, token.kind);
        }
        return null;
    }

    /**
     * accept current token and advance to next token
     */
    private void acceptIt() throws SyntaxError {
        accept(token.kind);
    }

    /**
     * verify that current token in input matches expected token and advance to next token
     *
     * @param expectedTokenKind
     * @throws SyntaxError if match fails
     */
    private void accept(TokenKind expectedTokenKind) throws SyntaxError {
        if (token.kind == expectedTokenKind) {
            if (trace) {
                pTrace();
            }
            previousTokenPosition = token.position;
            token = scanner.scan();
        } else {
            parseError("expecting '" + expectedTokenKind +
                    "' but found '" + token.kind + "'");
        }
    }

    /**
     * report parse error and unwind call stack to start of parse
     *
     * @param starters expected starter for the rule
     * @param found    found starter for the rule
     * @throws SyntaxError
     */
    private void parseError(HashSet<TokenKind> starters, TokenKind found) throws SyntaxError {
        StringBuilder errorMsg = new StringBuilder("Invalid Term - expecting ");
        for (TokenKind starter : starters) {
            errorMsg.append(starter.toString() + " or ");
        }
        errorMsg.append("but found " + found.toString());
        parseError(errorMsg.toString());
    }

    /**
     * report parse error and unwind call stack to start of parse
     *
     * @param e string with error detail
     * @throws SyntaxError
     */
    private void parseError(String e) throws SyntaxError {
        reporter.reportError("Parse error: " + e, token.position);
        throw new SyntaxError();
    }

    // show parse stack whenever terminal is  accepted
    private void pTrace() {
        StackTraceElement[] stl = Thread.currentThread().getStackTrace();
        for (int i = stl.length - 1; i > 0; i--) {
            if (stl[i].toString().contains("parse"))
                System.out.println(stl[i]);
        }
        System.out.println("accepting: " + token.kind + " (\"" + token.spelling + "\")");
        System.out.println();
    }
}
