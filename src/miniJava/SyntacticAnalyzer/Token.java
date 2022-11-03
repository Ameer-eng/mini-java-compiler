package miniJava.SyntacticAnalyzer;


public class Token {
    public enum TokenKind {
        EOT, CLASS, ID, VOID, PUBLIC, PRIVATE, STATIC, INT, BOOLEAN, THIS, DOT, LBRACKET, RBRACKET, SEMICOLON,
        LPAREN, RPAREN, LCURLY, RCURLY, RETURN, IF, WHILE, COMMA, UNOP, NUM, NEW, ELSE,
        EQUALS, TRUE, FALSE, BINOP, ERROR, NULL
    }

    public TokenKind kind;
    public String spelling;
    public SourcePosition position;

    public Token(TokenKind kind, String spelling, SourcePosition position) {
        this.kind = kind;
        this.spelling = spelling;
        this.position = position;
    }
}
