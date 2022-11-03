package miniJava.SyntacticAnalyzer;

import java.io.IOException;
import java.io.InputStream;

import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.Token.TokenKind;

import static miniJava.SyntacticAnalyzer.Token.TokenKind.*;

public class Scanner {
    private SourceFile sourceFile;
    private ErrorReporter reporter;

    private char currentChar;
    private StringBuilder currentSpelling;


    // true when end of text is found.
    private boolean eot = false;
    public static final char EOT = '\u0000';

    // True when end of line is found.
    private boolean eol = false;

    public Scanner(SourceFile source, ErrorReporter reporter) {
        this.sourceFile = source;
        this.reporter = reporter;

        // initialize scanner state
        readChar();
    }

    private void scanComment() {
        switch (currentChar) {
            case '/':
                skipIt();
                while (!eol && !eot) {
                    skipIt();
                }
                if (eol) {
                    skipIt();
                }
                break;

            case '*':
                skipIt();
                char prevChar = 0;
                while (!(prevChar == '*' && currentChar == '/') && !eot) {
                    prevChar = currentChar;
                    skipIt();
                }
                if (prevChar == '*' && currentChar == '/') {
                    skipIt();
                } else {
                    scanError("Expected '*/' to end comment but found end of text");
                }
                break;

            default:
        }
    }

    /**
     * skip whitespace and scan next token
     */
    public Token scan() {
        currentSpelling = new StringBuilder();
        SourcePosition pos = new SourcePosition();
        while (!eot && (isSeparator(currentChar) || currentChar == '/')) {
            if (isSeparator(currentChar)) {
                skipIt();
                continue;
            }

            skipIt();
            switch (currentChar) {
                case '/':
                case '*':
                    scanComment();
                    if (reporter.hasErrors()) {
                        pos.start = sourceFile.getCurrentLine();
                        pos.finish = sourceFile.getCurrentLine();
                        return new Token(ERROR, currentSpelling.toString(), pos);
                    }
                    break;

                default:
                    pos.start = sourceFile.getCurrentLine();
                    pos.finish = sourceFile.getCurrentLine();
                    return new Token(BINOP, "/", pos);
            }
        }

        // start of a token: collect spelling and identify token kind
        pos.start = sourceFile.getCurrentLine();
        TokenKind kind = scanToken();
        pos.finish = sourceFile.getCurrentLine();
        String spelling = currentSpelling.toString();

        // return new token
        return new Token(kind, spelling, pos);
    }

    /**
     * determine token kind
     */
    public TokenKind scanToken() {
        if (eot) {
            return TokenKind.EOT;
        }

        switch (currentChar) {
            case 'a':
            case 'b':
            case 'c':
            case 'd':
            case 'e':
            case 'f':
            case 'g':
            case 'h':
            case 'i':
            case 'j':
            case 'k':
            case 'l':
            case 'm':
            case 'n':
            case 'o':
            case 'p':
            case 'q':
            case 'r':
            case 's':
            case 't':
            case 'u':
            case 'v':
            case 'w':
            case 'x':
            case 'y':
            case 'z':
            case 'A':
            case 'B':
            case 'C':
            case 'D':
            case 'E':
            case 'F':
            case 'G':
            case 'H':
            case 'I':
            case 'J':
            case 'K':
            case 'L':
            case 'M':
            case 'N':
            case 'O':
            case 'P':
            case 'Q':
            case 'R':
            case 'S':
            case 'T':
            case 'U':
            case 'V':
            case 'W':
            case 'X':
            case 'Y':
            case 'Z':
                takeIt();
                while (isLetter(currentChar) || isDigit(currentChar) || currentChar == '_') {
                    takeIt();
                }
                switch (currentSpelling.toString()) {
                    case "class":
                        return TokenKind.CLASS;
                    case "void":
                        return TokenKind.VOID;
                    case "public":
                        return TokenKind.PUBLIC;
                    case "private":
                        return TokenKind.PRIVATE;
                    case "static":
                        return TokenKind.STATIC;
                    case "int":
                        return TokenKind.INT;
                    case "boolean":
                        return TokenKind.BOOLEAN;
                    case "this":
                        return TokenKind.THIS;
                    case "return":
                        return TokenKind.RETURN;
                    case "if":
                        return TokenKind.IF;
                    case "while":
                        return TokenKind.WHILE;
                    case "new":
                        return TokenKind.NEW;
                    case "else":
                        return TokenKind.ELSE;
                    case "true":
                        return TokenKind.TRUE;
                    case "false":
                        return TokenKind.FALSE;
                    case "null":
                        return TokenKind.NULL;
                    default:
                        return TokenKind.ID;
                }

            case '[':
                takeIt();
                return LBRACKET;

            case ']':
                takeIt();
                return RBRACKET;

            case ',':
                takeIt();
                return COMMA;

            case '.':
                takeIt();
                return DOT;

            case '{':
                takeIt();
                return LCURLY;

            case '}':
                takeIt();
                return RCURLY;

            case ';':
                takeIt();
                return SEMICOLON;

            case '(':
                takeIt();
                return LPAREN;

            case ')':
                takeIt();
                return RPAREN;

            case '=':
                takeIt();
                if (currentChar == '=') {
                    takeIt();
                    return BINOP;
                } else {
                    return EQUALS;
                }

            case '>':
            case '<':
                takeIt();
                if (currentChar == '=') {
                    takeIt();
                }
                return BINOP;

            case '!':
                takeIt();
                if (currentChar == '=') {
                    takeIt();
                    return BINOP;
                } else {
                    return UNOP;
                }

            case '&':
                takeIt();
                if (currentChar == '&') {
                    takeIt();
                    return BINOP;
                } else {
                    scanError("Expected '&' after '&' but found " + currentChar);
                    return ERROR;
                }

            case '|':
                takeIt();
                if (currentChar == '|') {
                    takeIt();
                    return BINOP;
                } else {
                    scanError("Expected '&' after '&' but found " + currentChar);
                    return ERROR;
                }

            case '+':
            case '-':
            case '*':
                takeIt();
                return BINOP;

            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                while (isDigit(currentChar))
                    takeIt();
                return NUM;

            default:
                scanError("Unrecognized character '" + currentChar + "' in input");
                return TokenKind.ERROR;
        }
    }

    private void takeIt() {
        currentSpelling.append(currentChar);
        nextChar();
    }

    private void skipIt() {
        nextChar();
    }

    private boolean isSeparator(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    private boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private boolean isDigit(char c) {
        return (c >= '0') && (c <= '9');
    }

    private void scanError(String m) {
        reporter.reportError("Scan Error:  " + m,
                new SourcePosition(sourceFile.getCurrentLine(), sourceFile.getCurrentLine()));
    }

    /**
     * advance to next char in inputstream
     * detect end of file or end of line as end of input
     */
    private void nextChar() {
        if (!eot) {
            readChar();
        }
    }

    private void readChar() {
        try {
            currentChar = sourceFile.readChar();
            eot = currentChar == EOT;
            eol = currentChar == '\n';
        } catch (IOException e) {
            scanError("I/O Exception!");
            eot = true;
        }

        /*
        try {
            int c = inputStream.read();
            if (c == -1) {
                eot = true;
            } else {
                currentChar = (char) c;
                eol = c == '\n';
            }
        } catch (IOException e) {
            scanError("I/O Exception!");
            eot = true;
        }
        */
    }
}
