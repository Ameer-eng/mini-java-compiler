/*
 * @(#)SourceFile.java                        2.1 2003/10/07
 *
 * Copyright (C) 1999, 2003 D.A. Watt and D.F. Brown
 * Dept. of Computing Science, University of Glasgow, Glasgow G12 8QQ Scotland
 * and School of Computer and Math Sciences, The Robert Gordon University,
 * St. Andrew Street, Aberdeen AB25 1HG, Scotland.
 * All rights reserved.
 *
 * This software is provided free for educational use only. It may
 * not be used for commercial purposes without the prior written permission
 * of the authors.
 */

package miniJava.SyntacticAnalyzer;

import java.io.IOException;
import java.io.InputStream;

public class SourceFile {

    public static final char EOL = '\n';
    public static final char EOT = '\u0000';

    java.io.File sourceFile;
    InputStream inputStream;
    int currentLine;
    char currentChar;

    public SourceFile(String filename) {
        try {
            sourceFile = new java.io.File(filename);
            inputStream = new java.io.FileInputStream(sourceFile);
            currentLine = 1;
        } catch (java.io.IOException s) {
            System.out.println("Input file " + filename + " not found");
            System.exit(1);
        }
    }

    char readChar() throws IOException {
        int c = inputStream.read();

        if (c == -1) {
            c = EOT;
        } else if (currentChar == EOL) {
            currentLine++;
        }
        currentChar = (char) c;
        return (char) c;
    }

    int getCurrentLine() {
        return currentLine;
    }
}
