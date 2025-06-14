/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.annotations.JSConstructor;
import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;

/**
 * Define a simple JavaScript File object.
 *
 * <p>This isn't intended to be any sort of definitive attempt at a standard File object for
 * JavaScript, but instead is an example of a more involved definition of a host object.
 *
 * <p>Example of use of the File object:
 *
 * <pre>
 * js&gt; defineClass("File")
 * js&gt; file = new File("myfile.txt");
 * [object File]
 * js&gt; file.writeLine("one");           &lt;i&gt;only now is file actually opened&lt;/i&gt;
 * js&gt; file.writeLine("two");
 * js&gt; file.writeLine("thr", "ee");
 * js&gt; file.close();                    &lt;i&gt;must close file before we can reopen for reading&lt;/i&gt;
 * js&gt; var a = file.readLines();        &lt;i&gt;creates and fills an array with the contents of the file&lt;/i&gt;
 * js&gt; a;
 * one,two,three
 * js&gt;
 * </pre>
 *
 * File errors or end-of-file signaled by thrown Java exceptions will be wrapped as JavaScript
 * exceptions when called from JavaScript, and may be caught within JavaScript.
 *
 * @author Norris Boyd
 */
public class File extends ScriptableObject {
    /** Serial version UID for serialization compatibility. */
    private static final long serialVersionUID = 2549960399774237828L;

    /**
     * The zero-parameter constructor.
     *
     * <p>When Context.defineClass is called with this class, it will construct File.prototype using
     * this constructor.
     */
    public File() {}

    /**
     * The Java method defining the JavaScript File constructor.
     *
     * <p>If the constructor has one or more arguments, and the first argument is not undefined, the
     * argument is converted to a string as used as the filename.
     *
     * <p>Otherwise System.in or System.out is assumed as appropriate to the use.
     *
     * @param cx the current Context for this thread
     * @param args the array of arguments passed to the constructor
     * @param ctorObj the constructor function object
     * @param inNewExpr true if this constructor is called with the 'new' operator
     * @return a new File instance configured with the specified filename or standard streams
     */
    @JSConstructor
    public static Scriptable jsConstructor(
            Context cx, Object[] args, Function ctorObj, boolean inNewExpr) {
        File result = new File();
        if (args.length == 0 || args[0] == Context.getUndefinedValue()) {
            result.name = "";
            result.file = null;
        } else {
            result.name = Context.toString(args[0]);
            result.file = new java.io.File(result.name);
        }
        return result;
    }

    /**
     * Returns the name of this JavaScript class, "File". This method is required by the
     * ScriptableObject interface.
     *
     * @return the fixed string "File"
     */
    @Override
    public String getClassName() {
        return "File";
    }

    /**
     * Get the name of the file.
     *
     * <p>Used to define the "name" property in JavaScript.
     *
     * @return the filename associated with this File object, or empty string if using standard
     *     streams
     */
    @JSGetter
    public String getName() {
        return name;
    }

    /**
     * Read the remaining lines in the file and return them in an array.
     *
     * <p>Implements a JavaScript function that reads all remaining lines from the current position
     * in the file and returns them as a JavaScript Array object.
     *
     * <p>This is a good example of creating a new array and setting elements in that array.
     *
     * @return a JavaScript Array containing all the remaining lines in the file as strings
     * @throws IOException if an error occurred while accessing the file associated with this
     *     object, or if the file cannot be read
     */
    @JSFunction
    public Object readLines() throws IOException {
        List<String> list = new ArrayList<String>();
        String s;
        while ((s = readLine()) != null) {
            list.add(s);
        }
        String[] lines = list.toArray(new String[list.size()]);
        Scriptable scope = ScriptableObject.getTopLevelScope(this);
        Context cx = Context.getCurrentContext();
        return cx.newObject(scope, "Array", lines);
    }

    /**
     * Read a single line from the file.
     *
     * <p>Implements a JavaScript function that reads the next line from the file. Line terminators
     * are not included in the returned string.
     *
     * @return the next line from the file as a string, or null if end of file is reached
     * @throws IOException if an error occurred while accessing the file associated with this
     *     object, or if the file cannot be read
     */
    @JSFunction
    public String readLine() throws IOException {
        return getReader().readLine();
    }

    /**
     * Read a single character from the file.
     *
     * <p>Implements a JavaScript function that reads the next character from the file.
     *
     * @return the next character from the file as a single-character string, or null if end of file
     *     is reached
     * @throws IOException if an error occurred while accessing the file associated with this
     *     object, or if the file cannot be read
     */
    @JSFunction
    public String readChar() throws IOException {
        int i = getReader().read();
        if (i == -1) return null;
        char[] charArray = {(char) i};
        return new String(charArray);
    }

    /**
     * Write strings to the file.
     *
     * <p>Implements a JavaScript function that takes a variable number of arguments, converts each
     * argument to a string, and writes those strings to the file without adding line terminators.
     *
     * @param cx the current Context for this thread
     * @param thisObj the JavaScript {@code this} object (should be a File instance)
     * @param args the array of arguments to be converted to strings and written
     * @param funObj the function object of the invoked JavaScript function
     * @throws IOException if an error occurred while accessing the file associated with this
     *     object, or if the file cannot be written to
     */
    @JSFunction
    public static void write(Context cx, Scriptable thisObj, Object[] args, Function funObj)
            throws IOException {
        write0(thisObj, args, false);
    }

    /**
     * Write strings followed by a newline to the file.
     *
     * <p>Implements a JavaScript function that takes a variable number of arguments, converts each
     * argument to a string, writes those strings to the file, and then writes a platform-specific
     * line separator.
     *
     * @param cx the current Context for this thread
     * @param thisObj the JavaScript {@code this} object (should be a File instance)
     * @param args the array of arguments to be converted to strings and written
     * @param funObj the function object of the invoked JavaScript function
     * @throws IOException if an error occurred while accessing the file associated with this
     *     object, or if the file cannot be written to
     */
    @JSFunction
    public static void writeLine(Context cx, Scriptable thisObj, Object[] args, Function funObj)
            throws IOException {
        write0(thisObj, args, true);
    }

    /**
     * Get the current line number of the reader.
     *
     * <p>Used to define the "lineNumber" property in JavaScript. The line number starts at 0 and
     * increments with each line read.
     *
     * @return the current line number (0-based) of the file reader
     * @throws FileNotFoundException if the file cannot be found or opened for reading
     */
    @JSGetter
    public int getLineNumber() throws FileNotFoundException {
        return getReader().getLineNumber();
    }

    /**
     * Close the file streams.
     *
     * <p>Implements a JavaScript function that closes any open reader or writer associated with
     * this file. The file may be reopened later for reading or writing. It is important to close
     * files to free system resources.
     *
     * @throws IOException if an error occurred while closing the file streams
     */
    @JSFunction
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
            reader = null;
        } else if (writer != null) {
            writer.close();
            writer = null;
        }
    }

    /**
     * Finalizer method called during garbage collection.
     *
     * <p>Automatically closes the file when this object is collected by the garbage collector. This
     * provides a safety net to ensure files are closed even if the JavaScript code doesn't
     * explicitly call close().
     */
    @SuppressWarnings("deprecation")
    @Override
    protected void finalize() {
        try {
            close();
        } catch (IOException e) {
            // Ignore exceptions during finalization
        }
    }

    /**
     * Get the Java LineNumberReader object wrapped for JavaScript access.
     *
     * <p>This method is exposed to JavaScript with the name "getReader". It returns the underlying
     * Java reader object wrapped in a way that allows JavaScript code to directly access Java
     * reader methods.
     *
     * @return the Java LineNumberReader wrapped as a JavaScript object, or null if no reader is
     *     currently open
     */
    @JSFunction("getReader")
    public Object getJSReader() {
        if (reader == null) return null;
        // Here we use javaToJS() to "wrap" the LineNumberReader object
        // in a Scriptable object so that it can be manipulated by
        // JavaScript.
        Scriptable parent = ScriptableObject.getTopLevelScope(this);
        return Context.javaToJS(reader, parent);
    }

    /**
     * Get the Java BufferedWriter object wrapped for JavaScript access.
     *
     * <p>Similar to {@link #getJSReader()}, this method returns the underlying Java writer object
     * wrapped for JavaScript access, allowing direct manipulation of the writer from JavaScript
     * code.
     *
     * @return the Java BufferedWriter wrapped as a JavaScript object, or null if no writer is
     *     currently open
     * @see #getJSReader()
     */
    @JSFunction
    public Object getWriter() {
        if (writer == null) return null;
        Scriptable parent = ScriptableObject.getTopLevelScope(this);
        return Context.javaToJS(writer, parent);
    }

    /**
     * Get the LineNumberReader for this file, creating it if necessary.
     *
     * <p>This method ensures that we're not already writing to this file before allowing read
     * access. If no reader exists, it creates one using either the specified file or System.in if
     * no file was specified.
     *
     * @return the LineNumberReader for reading from this file or System.in
     * @throws FileNotFoundException if the specified file cannot be found or opened
     */
    private LineNumberReader getReader() throws FileNotFoundException {
        if (writer != null) {
            throw Context.reportRuntimeError("already writing file \"" + name + "\"");
        }
        if (reader == null)
            reader =
                    new LineNumberReader(
                            file == null ? new InputStreamReader(System.in) : new FileReader(file));
        return reader;
    }

    /**
     * Perform the common functionality for write and writeLine methods.
     *
     * <p>Since the two functions differ only in whether they write a newline character, this method
     * contains the shared implementation. It handles the creation of the writer if necessary and
     * writes all arguments as strings to the file.
     *
     * @param thisObj the File object to write to
     * @param args the arguments to convert to strings and write
     * @param eol true to append a newline after writing all arguments, false otherwise
     * @throws IOException if an error occurs while writing to the file
     */
    private static void write0(Scriptable thisObj, Object[] args, boolean eol) throws IOException {
        File thisFile = checkInstance(thisObj);
        if (thisFile.reader != null) {
            throw Context.reportRuntimeError("already reading file \"" + thisFile.name + "\"");
        }
        if (thisFile.writer == null)
            thisFile.writer =
                    new BufferedWriter(
                            thisFile.file == null
                                    ? new OutputStreamWriter(System.out)
                                    : new FileWriter(thisFile.file));
        for (int i = 0; i < args.length; i++) {
            String s = Context.toString(args[i]);
            thisFile.writer.write(s, 0, s.length());
        }
        if (eol) thisFile.writer.newLine();
    }

    /**
     * Perform instanceof check and return the downcasted File object.
     *
     * <p>This is necessary since methods may reside in the File.prototype object and scripts can
     * dynamically alter prototype chains. This method ensures type safety by verifying that the
     * object is actually a File instance before casting.
     *
     * <p>For example, the following would be caught by this check:
     *
     * <pre>
     * js> defineClass("File");
     * js> o = {};
     * [object Object]
     * js> o.__proto__ = File.prototype;
     * [object File]
     * js> o.write("hi");
     * js: called on incompatible object
     * </pre>
     *
     * <p>The runtime will take care of such checks when non-static Java methods are defined as
     * JavaScript functions.
     *
     * @param obj the object to check and cast
     * @return the object cast to File type
     * @throws RuntimeException if the object is null or not an instance of File
     */
    private static File checkInstance(Scriptable obj) {
        if (obj == null || !(obj instanceof File)) {
            throw Context.reportRuntimeError("called on incompatible object");
        }
        return (File) obj;
    }

    /**
     * The filename associated with this File object. May be empty string if using standard
     * input/output streams.
     */
    private String name;

    /**
     * The underlying Java File object for file system operations. May be null if using standard
     * input/output streams instead of a file.
     */
    private java.io.File file;

    /**
     * The reader used for reading from the file or standard input. Null when not currently reading
     * or when writing mode is active.
     */
    private LineNumberReader reader;

    /**
     * The writer used for writing to the file or standard output. Null when not currently writing
     * or when reading mode is active.
     */
    private BufferedWriter writer;
}
