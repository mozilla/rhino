/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tools.jsc;

import java.io.*;
import java.util.*;
import org.mozilla.javascript.*;
import org.mozilla.javascript.optimizer.ClassCompiler;
import org.mozilla.javascript.tools.SourceReader;
import org.mozilla.javascript.tools.ToolErrorReporter;

/**
 * @author Norris Boyd
 */
public class Main {

    /**
     * Main entry point.
     *
     * Process arguments as would a normal Java program.
     * Then set up the execution environment and begin to
     * compile scripts.
     */
    public static void main(String args[])
    {
        Main main = new Main();
        args = main.processOptions(args);
        if (args == null) {
            if (main.printHelp) {
                System.out.println(ToolErrorReporter.getMessage(
                    "msg.jsc.usage", Main.class.getName()));
                System.exit(0);
            }
            System.exit(1);
        }
        if (!main.reporter.hasReportedError()) {
            main.processSource(args);
        }
    }

    public Main()
    {
        reporter = new ToolErrorReporter(true);
        compilerEnv = new CompilerEnvirons();
        compilerEnv.setErrorReporter(reporter);
        compiler = new ClassCompiler(compilerEnv);
    }

    /**
     * Parse arguments.
     *
     */
    public String[] processOptions(String args[])
    {
        targetPackage = "";        // default to no package
        compilerEnv.setGenerateDebugInfo(false);   // default to no symbols
        for (int i=0; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("-")) {
                int tail = args.length - i;
                if (targetName != null && tail > 1) {
                    addError("msg.multiple.js.to.file", targetName);
                    return null;
                }
                String[] result = new String[tail];
                for (int j = 0; j != tail; ++j) {
                    result[j] = args[i + j];
                }
                return result;
            }
            if (arg.equals("-help") || arg.equals("-h")
                || arg.equals("--help"))
            {
                printHelp = true;
                return null;
            }

            try {
                if (arg.equals("-version") && ++i < args.length) {
                    int version = Integer.parseInt(args[i]);
                    compilerEnv.setLanguageVersion(version);
                    continue;
                }
                if ((arg.equals("-opt") || arg.equals("-O"))  &&
                    ++i < args.length)
                {
                    int optLevel = Integer.parseInt(args[i]);
                    compilerEnv.setOptimizationLevel(optLevel);
                    continue;
                }
            }
            catch (NumberFormatException e) {
                badUsage(args[i]);
                return null;
            }
            if (arg.equals("-nosource")) {
                compilerEnv.setGeneratingSource(false);
                continue;
            }
            if (arg.equals("-debug") || arg.equals("-g")) {
                compilerEnv.setGenerateDebugInfo(true);
                continue;
            }
            if (arg.equals("-main-method-class") && ++i < args.length) {
                compiler.setMainMethodClass(args[i]);
                continue;
            }
            if (arg.equals("-encoding") && ++i < args.length) {
                characterEncoding = args[i];
                continue;
            }
            if (arg.equals("-o") && ++i < args.length) {
                String name = args[i];
                int end = name.length();
                if (end == 0
                    || !Character.isJavaIdentifierStart(name.charAt(0)))
                {
                    addError("msg.invalid.classfile.name", name);
                    continue;
                }
                for (int j = 1; j < end; j++) {
                    char c = name.charAt(j);
                    if (!Character.isJavaIdentifierPart(c)) {
                        if (c == '.') {
                            // check if it is the dot in .class
                            if (j == end - 6 && name.endsWith(".class")) {
                                name = name.substring(0, j);
                                break;
                            }
                        }
                        addError("msg.invalid.classfile.name", name);
                        break;
                    }
                }
                targetName = name;
                continue;
            }
            if (arg.equals("-observe-instruction-count")) {
                compilerEnv.setGenerateObserverCount(true);
            }
            if (arg.equals("-package") && ++i < args.length) {
                String pkg = args[i];
                int end = pkg.length();
                for (int j = 0; j != end; ++j) {
                    char c = pkg.charAt(j);
                    if (Character.isJavaIdentifierStart(c)) {
                        for (++j; j != end; ++j) {
                            c = pkg.charAt(j);
                            if (!Character.isJavaIdentifierPart(c)) {
                                break;
                            }
                        }
                        if (j == end) {
                            break;
                        }
                        if (c == '.' && j != end - 1) {
                            continue;
                        }
                    }
                    addError("msg.package.name", targetPackage);
                    return null;
                }
                targetPackage = pkg;
                continue;
            }
            if (arg.equals("-extends") && ++i < args.length) {
                String targetExtends = args[i];
                Class<?> superClass;
                try {
                    superClass = Class.forName(targetExtends);
                } catch (ClassNotFoundException e) {
                    throw new Error(e.toString()); // TODO: better error
                }
                compiler.setTargetExtends(superClass);
                continue;
            }
            if (arg.equals("-implements") && ++i < args.length) {
                // TODO: allow for multiple comma-separated interfaces.
                String targetImplements = args[i];
                StringTokenizer st = new StringTokenizer(targetImplements,
                                                         ",");
                List<Class<?>> list = new ArrayList<Class<?>>();
                while (st.hasMoreTokens()) {
                    String className = st.nextToken();
                    try {
                        list.add(Class.forName(className));
                    } catch (ClassNotFoundException e) {
                        throw new Error(e.toString()); // TODO: better error
                    }
                }
                Class<?>[] implementsClasses = list.toArray(new Class<?>[list.size()]);
                compiler.setTargetImplements(implementsClasses);
                continue;
            }
            if (arg.equals("-d") && ++i < args.length) {
                destinationDir = args[i];
                continue;
            }
            badUsage(arg);
            return null;
        }
        // no file name
        p(ToolErrorReporter.getMessage("msg.no.file"));
        return null;
    }
    /**
     * Print a usage message.
     */
    private static void badUsage(String s) {
        System.err.println(ToolErrorReporter.getMessage(
            "msg.jsc.bad.usage", Main.class.getName(), s));
    }

    /**
     * Compile JavaScript source.
     *
     */
    public void processSource(String[] filenames)
    {
        for (int i = 0; i != filenames.length; ++i) {
            String filename = filenames[i];
            if (!filename.endsWith(".js")) {
                addError("msg.extension.not.js", filename);
                return;
            }
            File f = new File(filename);
            String source = readSource(f);
            if (source == null) return;

            String mainClassName = targetName;
            if (mainClassName == null) {
                String name = f.getName();
                String nojs = name.substring(0, name.length() - 3);
                mainClassName = getClassName(nojs);
            }
            if (targetPackage.length() != 0) {
                mainClassName = targetPackage+"."+mainClassName;
            }

            Object[] compiled
                = compiler.compileToClassFiles(source, filename, 1,
                                               mainClassName);
            if (compiled == null || compiled.length == 0) {
                return;
            }

            File targetTopDir = null;
            if (destinationDir != null) {
                targetTopDir = new File(destinationDir);
            } else {
                String parent = f.getParent();
                if (parent != null) {
                    targetTopDir = new File(parent);
                }
            }
            for (int j = 0; j != compiled.length; j += 2) {
                String className = (String)compiled[j];
                byte[] bytes = (byte[])compiled[j + 1];
                File outfile = getOutputFile(targetTopDir, className);
                try {
                    FileOutputStream os = new FileOutputStream(outfile);
                    try {
                        os.write(bytes);
                    } finally {
                        os.close();
                    }
                } catch (IOException ioe) {
                    addFormatedError(ioe.toString());
                }
            }
        }
    }

    private String readSource(File f)
    {
        String absPath = f.getAbsolutePath();
        if (!f.isFile()) {
            addError("msg.jsfile.not.found", absPath);
            return null;
        }
        try {
            return (String)SourceReader.readFileOrUrl(absPath, true,
                    characterEncoding);
        } catch (FileNotFoundException ex) {
            addError("msg.couldnt.open", absPath);
        } catch (IOException ioe) {
            addFormatedError(ioe.toString());
        }
        return null;
    }

    private File getOutputFile(File parentDir, String className)
    {
        String path = className.replace('.', File.separatorChar);
        path = path.concat(".class");
        File f = new File(parentDir, path);
        String dirPath = f.getParent();
        if (dirPath != null) {
            File dir = new File(dirPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
        return f;
    }

    /**
     * Verify that class file names are legal Java identifiers.  Substitute
     * illegal characters with underscores, and prepend the name with an
     * underscore if the file name does not begin with a JavaLetter.
     */

    String getClassName(String name) {
        char[] s = new char[name.length()+1];
        char c;
        int j = 0;

        if (!Character.isJavaIdentifierStart(name.charAt(0))) {
            s[j++] = '_';
        }
        for (int i=0; i < name.length(); i++, j++) {
            c = name.charAt(i);
            if ( Character.isJavaIdentifierPart(c) ) {
                s[j] = c;
            } else {
                s[j] = '_';
            }
        }
        return (new String(s)).trim();
     }

    private static void p(String s) {
        System.out.println(s);
    }

    private void addError(String messageId, String arg)
    {
        String msg;
        if (arg == null) {
            msg = ToolErrorReporter.getMessage(messageId);
        } else {
            msg = ToolErrorReporter.getMessage(messageId, arg);
        }
        addFormatedError(msg);
    }

    private void addFormatedError(String message)
    {
        reporter.error(message, null, -1, null, -1);
    }

    private boolean printHelp;
    private ToolErrorReporter reporter;
    private CompilerEnvirons compilerEnv;
    private ClassCompiler compiler;
    private String targetName;
    private String targetPackage;
    private String destinationDir;
    private String characterEncoding;
}

