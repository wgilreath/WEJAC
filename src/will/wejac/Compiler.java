/*
 * @(#)Compiler.java
 *
 * Title: WEJAC - Will's Elided Java Api Compiler.
 *
 * Description: A Java compiler using the Java Compiler API with options not in javac.
 *
 * @author William F. Gilreath (wfgilreath@yahoo.com)
 * @version 2.1  6/26/19
 *
 * Copyright © 2019 All Rights Reserved.
 *
 * License: This software is subject to the terms of the GNU General Public License (GPL)  
 *     version 3.0 available at the following link: http://www.gnu.org/copyleft/gpl.html.
 *
 * You must accept the terms of the GNU General Public License (GPL) license agreement
 *     to use this software.
 *
 **/
package will.wejac;

import java.io.PrintWriter;
import java.io.Writer;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public final class Compiler {

    private static boolean briefFlag = false;  //set brief error reporting a count of error diagnostics
    private static boolean finalFlag = false;  //set final compilation with no debug information

    private static boolean echoFlag  = false;  //set echo Java compiler parameters and compiler status
    private static boolean hushFlag  = false;  //set hush compiler diagnostics except errors
    private static boolean muteFlag  = false;  //set mute all compiler diagnostics are silenced
    private static boolean timeFlag  = false;  //set to time overall time to compile a Java source file

    private final static ArrayList<String> files = new ArrayList<>(); //Javac compiler Java source files
    private final static ArrayList<String> param = new ArrayList<>(); //Javac compiler parameters implicit and explicit

    private final static Charset CHARSET = Charset.defaultCharset();
    private final static String  ENDLN   = System.getProperty("line.separator");
    private final static Locale  LOCALE  = Locale.getDefault();
    private final static Writer  SYS_ERR = new PrintWriter(System.err, true);

    private final static Iterable<String> NO_ANNOTATION_PROC = Collections.emptyList();

    /**
     * Private constructor to prevent instantiating this class except internally.
     */
    private Compiler() {
    }//end constructor

    /**
     * Diagnose compiler errors with error, position, and illustrative source code line.
     *
     * @param fileName      - name of the external file containing the Java source code.
     * @param diagnostics   - diagnostic information from compile of Java source code.
     * @param javaFileCode  - list containing the lines of Java source code from file.
     */
    private void diagnose(final String fileName,
                          final DiagnosticCollector<JavaFileObject> diagnostics,
                          final List<String> javaFileCode){

        for (Diagnostic<?> diag : diagnostics.getDiagnostics()) {

        	if(hushFlag) {
        		if(diag.getKind() != Diagnostic.Kind.ERROR)
        			continue;
        	}//end if 
        	
            final String diagnosticText = diag.toString();

            System.out.printf("Error: %s.%n",   fileName);

            if(diag.getKind() != Diagnostic.Kind.NOTE) {

                System.out.printf("Line %d ",   diag.getLineNumber());
                System.out.printf("At %d:",     diag.getColumnNumber());

                String diagText = diagnosticText.split(":")[3];
                String diagLine = diagText.split(ENDLN)[0];

                System.out.printf("%s%n", diagLine);

                String codeLine = this.getCodeLine(javaFileCode, diag.getLineNumber(), diag.getColumnNumber());
                System.out.printf("%s%n", codeLine);

            } else {
                System.out.println(diag.getMessage(LOCALE));
            }//end if

            System.out.println();

        }//end for

    }//end diagnose

    /**
     * Get the source line of Java code and format with indicator of the point of diagnostic error.
     *
     * @param srcCode - a list containing the Java source code.
     * @param lineNum - the line number within the Java source code to retrieve.
     * @param colNum  - the column position within the line for the diagnostic error.
     *      
     * @return String - the line of source code formatted to indicate point of error.
     */
    private String getCodeLine(final List<String> srcCode, final long lineNum, final long colNum) {

        try {

            String line = srcCode.get((int) lineNum - 1);

            StringBuilder codeLine = new StringBuilder(line);
            codeLine.append(ENDLN);

            for (int x = 0; x < colNum - 1; x++) {
                codeLine.append(" ");
            }//end for

            codeLine.append("^");

            return codeLine.toString();

        } catch (Exception ex) {
            System.out.printf("%s%n", ex.getMessage());
            ex.printStackTrace();
        }//end try

        return "";

    }//end getCodeLine

    /**
     * Compile single Java source code file using the Java Compiler API with compiler parameters.
     *
     * @param fileName - name of the external file containing the Java source code.
     */
    private void compileFile(final String fileName) {

        if (echoFlag) {

            System.out.printf("%nJava Compile Options: %s%n%n",
                              param.isEmpty() ? "None." : param.toString());

        }//end if

        //0 - error, 1 - mandatory warning, 2 - note, 3 - other, 4 - warning, 5 - total diagnostic
        final int[] diagnosticCounter = new int[]{0, 0, 0, 0, 0, 0};

        boolean statusFlag = false;

        long timeStart = 0, timeClose = 0;

        try {

            JavaCompiler                        comp	= ToolProvider.getSystemJavaCompiler();
            DiagnosticCollector<JavaFileObject> diag    = new DiagnosticCollector<>();
            StandardJavaFileManager             file    = comp.getStandardFileManager(diag, LOCALE, CHARSET);
            Iterable<? extends JavaFileObject>  list    = file.getJavaFileObjectsFromStrings(Collections.singletonList(fileName));
            JavaCompiler.CompilationTask        task    = comp.getTask( SYS_ERR,
                                                                        file,
                                                                        diag,
                                                                        param,
                                                                        NO_ANNOTATION_PROC,
                                                                        list);

            System.gc();

            timeStart  = System.currentTimeMillis();
            statusFlag = task.call();
            timeClose  = System.currentTimeMillis();

            if (!muteFlag) {

                if (briefFlag) {

                    for (Diagnostic<? extends JavaFileObject> diagnostic : diag.getDiagnostics()) {

                        Diagnostic.Kind kind = diagnostic.getKind();

                        switch (kind) {
                            case ERROR:
                                diagnosticCounter[0]++;
                                diagnosticCounter[5]++;
                                break;
                            case MANDATORY_WARNING:
                                diagnosticCounter[1]++;
                                diagnosticCounter[5]++;
                                break;
                            case NOTE:
                                diagnosticCounter[2]++;
                                diagnosticCounter[5]++;
                                break;
                            case OTHER:
                                diagnosticCounter[3]++;
                                diagnosticCounter[5]++;
                                break;
                            case WARNING:
                                diagnosticCounter[4]++;
                                diagnosticCounter[5]++;
                                break;
                        }//end switch

                    }//end for

                }//end if (Compiler.briefFlag)

                Path         javaFilePath = Paths.get(".", fileName);
                List<String> javaFileCode = Files.readAllLines(javaFilePath, CHARSET);

                if(!briefFlag)
                    this.diagnose(fileName, diag, javaFileCode);

                System.gc();

            }//end if (!Compiler.muteFlag)

            file.close();

        } catch (Exception ex) {

            System.out.printf("Compiler Exception: '%s' is '%s'.%n", ex.getClass().getName(), ex.getMessage());

        } finally {

            if (briefFlag) {
                if (diagnosticCounter[5] > 0){
                    System.out.printf("%3d Diagnostic messages:%n", diagnosticCounter[5]);
                    for(int x=0;x<diagnosticCounter.length-1;x++){
                        if(diagnosticCounter[x] > 0){

                            //0 - error, 1 - mandatory warning, 2 - note, 3 - other, 4 - warning, 5 - total diagnostic

                            switch(x){
                                case 0: System.out.printf("  %3d Error%n", diagnosticCounter[x]); break;
                                case 1: System.out.printf("  %3d Mandatory Warning%n", diagnosticCounter[x]); break;
                                case 2: System.out.printf("  %3d Note%n", diagnosticCounter[x]); break;
                                case 3: System.out.printf("  %3d Other%n", diagnosticCounter[x]);
                                case 4: System.out.printf("  %3d Warning%n", diagnosticCounter[x]); break;
                            }//end switch

                        }//end if

                    }//end for

                    System.out.println();

                } else {

                    System.out.println("No compiler diagnostic messages.");

                }//end if

            }//end if

            if (timeFlag) {
                System.out.printf("Time: %d-ms for: %s%n", (timeClose - timeStart), fileName);
            }//end if

            if (echoFlag) {
                System.out.printf("Compiler result for file: '%s' is: ", fileName);
                System.out.printf("%s%n", statusFlag ? "Success." : "Failure!");
                System.exit(statusFlag ? EXIT_CODE_SUCCESS : EXIT_CODE_FAILURE);
            }//end if

        }//end try

    }//end compile

    /**
     * Report a compiler error and then exit with status code of failure with a problem.
     *
     * @param text -  error message to report to the user.
     * @param args -  error message arguments to report.
     */
    private static void error(final String text, final Object... args) {

        System.out.printf("%nError! ");
        System.out.printf(text, args);
        System.out.printf("%n%n");
        System.exit(EXIT_CODE_PROBLEM);

    }//end error

    /**
     * Print compiler USEINFO and OPTIONS and then exit without invoking compiler.
     *
     */
    private static void printOptions() {

        System.out.printf("%n%s%n%s%n", USEINFO, OPTIONS);
        System.exit(EXIT_CODE_SUCCESS);

    }//end printOptions

    /**
     * Print compiler RELEASE and VERSION and then exit without invoking compiler.
     *
     */
    private static void printVersion() {

        System.out.printf("%s%n%s%n", RELEASE, VERSION);
        System.exit(EXIT_CODE_SUCCESS);

    }//end printVersion

    /**
     * Add any Javac compiler parameters to the compiler parameters passed to the Java Compiler API.
     *
     * @param args -  command line arguments for the Javac compiler passed to Java Compiler API.
     * @param idx  -  starting index position within array of command line arguments.
     * @return int -  closing index position within the array of command line arguments.
     */
    private int processJavacArguments(final String[] args, final int idx) {

        int pos;

        for (pos = idx + 1; pos < args.length; pos++) {

            if (args[pos].contains(FILE_SOURCE_EXT)) {
                break;
            } else {
                param.add(args[pos]);
            }//end if

        }//end for

        return pos - 1;

    }//end processJavacArguments

    /**
     * Process the command line arguments to set the internal parameters for compilation.
     *
     * @param args - command line arguments to set compiler parameters during compilation.
     */
    private void processCommandLineArgs(final String[] args) {

        int x;
        for (x = 0; x < args.length; x++) {

            if (args[x].contains("-")) {

                switch (args[x]) {

                    case "-time":
                        timeFlag = true;
                        break;
                    case "-echo":
                        echoFlag = true;
                        break;
                    case "-final":
                        finalFlag = true;
                        break;
                    case "-brief":
                        if (hushFlag || muteFlag)
                            error(ERROR_OPT_BRIEF);
                        briefFlag = true;
                        break;
                    case "-hush":
                        if (briefFlag || muteFlag)
                            error(ERROR_OPT_HUSH);
                        hushFlag = true;
                        break;
                    case "-mute":
                        if (briefFlag || hushFlag)
                            error(ERROR_OPT_MUTE);
                        muteFlag = true;
                        break;
                    case "-javac":
                        x = processJavacArguments(args, x);
                        break;
                    case "-help":
                        printOptions();
                        break;
                    case "-info":
                        printVersion();
                        break;
                    default:
                        error(ERROR_PARAM_WRONG, args[x]);
                        break;
                }//end switch

            } else {

                for (; x < args.length; x++) {

                    if (args[x].contains(FILE_SOURCE_EXT)) {
                        files.add(args[x]);
                    } else {

                        if (args[x].contains("-")) {
                            error(ERROR_PARAM_FILES, args[x]);
                        } else {
                            error(ERROR_FILE_EXT, args[x]);
                        }//end if

                    }//end if

                }//end for

                break;

            }//end if

        }//end for

    }//end processCommandLineArgs

    /**
     * Compile using the command line arguments of compiler parameters and Java source files.
     *
     * @param args - command line arguments passed to the WEJAC compiler.
     */
    private static void compile(final String[] args) {

        if (args.length == 0) {
            error(ERROR_NO_INPUT);
        }//end if

        final Compiler wejac = new Compiler();

        wejac.processCommandLineArgs(args);

        if (files.isEmpty()) {
            error(ERROR_NO_FILES);
        }//end if

        wejac.configureParams();

        for (String sourceFile : files) {
            wejac.compileFile(sourceFile);
        }//end for

    }//end compile

    private final static String JAVAC_FINAL = "-g:none";
    private final static String JAVAC_DEBUG = "-g";
    
    /**
     * Configure underlying Javac compiler parameters the WEJAC parameters passed as command line arguments.
     *
     */
    private void configureParams() {

        for (String sourceFile : files) {
            verifyFile(sourceFile);
        }//end for

        	param.add(finalFlag ? JAVAC_FINAL : JAVAC_DEBUG);
    }//end configureParams

    /**
     * Verify a Java source file in given path exists, is readable, and is of minimum file size to compile.
     *
     * @param filePath - file path to Java source file to compiler.
     */
    private static void verifyFile(final String filePath) {

        Path path = Paths.get(filePath);

        try {

            if (!Files.exists(path)) {
                error(ERROR_FILE_EXIST, path.toString());
            } else if (!Files.isReadable(path)) {
                error(ERROR_FILE_READ, path.toString());
            } else if (Files.size(path) < FILE_SIZE_MINIMUM) {
                error(ERROR_FILE_SMALL, path.toString());
            }//end if

        } catch (Exception ex) {
            error("Verify File Exception: '%s' is '%s'.%n", ex.getClass().getName(), ex.getMessage());
        }//end try

    }//end verifyFile

    private final static int EXIT_CODE_SUCCESS      = 0; //success - compiler success in compiling Java source file.
    private final static int EXIT_CODE_FAILURE      = 1; //failure - compiler failure in compiling Java source file.
    private final static int EXIT_CODE_PROBLEM      = 2; //problem - compiler failure with a problem for Java source file.

    private final static long   FILE_SIZE_MINIMUM   = 10;      //smallest java file size is 10-bytes
    private final static String FILE_SOURCE_EXT     = ".java"; //Java source file extension

    private final static String ERROR_NO_INPUT      = "No compiler options or files given! Use -help for options.";
    private final static String ERROR_NO_FILES      = "No source files given! Use -help for options.";

    private final static String ERROR_PARAM_FILES   = "Compiler option: '%s' must precede Java files list.";
    private final static String ERROR_PARAM_WRONG   = "Compiler option: '%s' is not recognized.";

    private final static String ERROR_FILE_EXT      = "File: '%s' does not have '.java' extension.";
    private final static String ERROR_FILE_EXIST    = "File: '%s' does not exist.";
    private final static String ERROR_FILE_READ     = "File: '%s' is not readable.";
    private final static String ERROR_FILE_SMALL    = "File: '%s' is too small.";

    private final static String ERROR_OPT_BRIEF     = "Option -brief ambiguous with option -hush and/or -mute option.";
    private final static String ERROR_OPT_HUSH      = "Option -hush ambiguous with option -brief and/or -mute option.";
    private final static String ERROR_OPT_MUTE      = "Option -mute ambiguous with -brief and/or -hush option.";

    private final static String LICENSE             = "License is GNU General Public License (GPL) version 3.0";
    private final static String VERSION             = "Version 1.3 Released July 2019";

    private final static String RELEASE             = "WEJAC - Will's Elided Java Api Compiler\n(C) Copyright 2019 William F. Gilreath. All Rights Reserved";
    private final static String USEINFO             = "Usage:  wejac (option)* [ -javac (javac-options)+ ] (java-file)+ | ( -help | -info )";

    private final static String OPTIONS =   "                                                                        \n\r" +
                                            "  WEJAC OPTIONS:                                                          \n" +
											"                                                                          \n" +
											"  Compiler Options:  [ -echo ] | [ -final ] | [ -time ]                   \n" +
											"                                                                          \n" +
											"    -echo        Print Java compiler options and success or failure.      \n" +
											"    -final       Compile final release without debug information.         \n" +
											"    -time        Print total time for success compiling of a source file. \n" +
											"                                                                          \n" +

											"  Error Reporting Option: [ -brief | -hush | -mute ]                      \n" +
											"                                                                          \n" +
											"    -brief       Print only a brief count of compiler messages.           \n" +
											"    -hush        Disable all compiler messages except errors.             \n" +
											"    -mute        Disable all compiler messages.                           \n" +
											"                                                                          \n" +

											"  Help or Version Option:  ( -help | -? ) | ( -info | -v )                \n" +
											"                                                                          \n" +
											"    -help        Print list of compiler options and exit.                     \n" +
											"    -info        Print compiler version information and exit.                 \n" +
											"                                                                          \n" +
											"  Note: All options for -javac are passed as-is to the compiler.          \n" +
											"                                                                        \n\r" ;

    /**
     * The main method is the central start method of the WEJAC compiler that invokes other compiler methods.
     *
     * @param args - command-line arguments to compiler
     */
    public static void main(final String[] args) {

        if (args.length == 0) {
            System.out.printf("%s %s%n%s%n", RELEASE, VERSION, LICENSE);
        }//end if

        compile(args);

        System.exit(EXIT_CODE_SUCCESS);

    }//end main

}//end class Compiler