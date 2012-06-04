/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.javascript.tools.debugger;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.EventQueue;
import java.awt.ActiveEvent;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.MenuComponent;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventListener;
import java.util.EventObject;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.io.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import java.lang.reflect.Method;

import org.mozilla.javascript.Kit;
import org.mozilla.javascript.SecurityUtilities;

import org.mozilla.javascript.tools.shell.ConsoleTextArea;

import org.mozilla.javascript.tools.debugger.treetable.JTreeTable;
import org.mozilla.javascript.tools.debugger.treetable.TreeTableModel;
import org.mozilla.javascript.tools.debugger.treetable.TreeTableModelAdapter;

/**
 * GUI for the Rhino debugger.
 */
public class SwingGui extends JFrame implements GuiCallback {

    /**
     * Serializable magic number.
     */
    private static final long serialVersionUID = -8217029773456711621L;

    /**
     * The debugger.
     */
    Dim dim;

    /**
     * The action to run when the 'Exit' menu item is chosen or the
     * frame is closed.
     */
    private Runnable exitAction;

    /**
     * The {@link JDesktopPane} that holds the script windows.
     */
    private JDesktopPane desk;

    /**
     * The {@link JPanel} that shows information about the context.
     */
    private ContextWindow context;

    /**
     * The menu bar.
     */
    private Menubar menubar;

    /**
     * The tool bar.
     */
    private JToolBar toolBar;

    /**
     * The console that displays I/O from the script.
     */
    private JSInternalConsole console;

    /**
     * The {@link JSplitPane} that separates {@link #desk} from
     * {@link org.mozilla.javascript.Context}.
     */
    private JSplitPane split1;

    /**
     * The status bar.
     */
    private JLabel statusBar;

    /**
     * Hash table of internal frame names to the internal frames themselves.
     */
    private final Map<String,JFrame> toplevels =
        Collections.synchronizedMap(new HashMap<String,JFrame>());

    /**
     * Hash table of script URLs to their internal frames.
     */
    private final Map<String,FileWindow> fileWindows =
        Collections.synchronizedMap(new HashMap<String,FileWindow>());


    /**
     * The {@link FileWindow} that last had the focus.
     */
    private FileWindow currentWindow;

    /**
     * File choose dialog for loading a script.
     */
    JFileChooser dlg;

    /**
     * The AWT EventQueue.  Used for manually pumping AWT events from
     * {@link #dispatchNextGuiEvent()}.
     */
    private EventQueue awtEventQueue;

    /**
     * Creates a new SwingGui.
     */
    public SwingGui(Dim dim, String title) {
        super(title);
        this.dim = dim;
        init();
        dim.setGuiCallback(this);
    }

    /**
     * Returns the Menubar of this debugger frame.
     */
    public Menubar getMenubar() {
        return menubar;
    }

    /**
     * Sets the {@link Runnable} that will be run when the "Exit" menu
     * item is chosen.
     */
    public void setExitAction(Runnable r) {
        exitAction = r;
    }

    /**
     * Returns the debugger console component.
     */
    public JSInternalConsole getConsole() {
        return console;
    }

    /**
     * Sets the visibility of the debugger GUI.
     */
    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        if (b) {
            // this needs to be done after the window is visible
            console.consoleTextArea.requestFocus();
            context.split.setDividerLocation(0.5);
            try {
                console.setMaximum(true);
                console.setSelected(true);
                console.show();
                console.consoleTextArea.requestFocus();
            } catch (Exception exc) {
            }
        }
    }

    /**
     * Records a new internal frame.
     */
    void addTopLevel(String key, JFrame frame) {
        if (frame != this) {
            toplevels.put(key, frame);
        }
    }

    /**
     * Constructs the debugger GUI.
     */
    private void init() {
        menubar = new Menubar(this);
        setJMenuBar(menubar);
        toolBar = new JToolBar();
        JButton button;
        JButton breakButton, goButton, stepIntoButton,
            stepOverButton, stepOutButton;
        String [] toolTips = {"Break (Pause)",
                              "Go (F5)",
                              "Step Into (F11)",
                              "Step Over (F7)",
                              "Step Out (F8)"};
        int count = 0;
        button = breakButton = new JButton("Break");
        button.setToolTipText("Break");
        button.setActionCommand("Break");
        button.addActionListener(menubar);
        button.setEnabled(true);
        button.setToolTipText(toolTips[count++]);

        button = goButton = new JButton("Go");
        button.setToolTipText("Go");
        button.setActionCommand("Go");
        button.addActionListener(menubar);
        button.setEnabled(false);
        button.setToolTipText(toolTips[count++]);

        button = stepIntoButton = new JButton("Step Into");
        button.setToolTipText("Step Into");
        button.setActionCommand("Step Into");
        button.addActionListener(menubar);
        button.setEnabled(false);
        button.setToolTipText(toolTips[count++]);

        button = stepOverButton = new JButton("Step Over");
        button.setToolTipText("Step Over");
        button.setActionCommand("Step Over");
        button.setEnabled(false);
        button.addActionListener(menubar);
        button.setToolTipText(toolTips[count++]);

        button = stepOutButton = new JButton("Step Out");
        button.setToolTipText("Step Out");
        button.setActionCommand("Step Out");
        button.setEnabled(false);
        button.addActionListener(menubar);
        button.setToolTipText(toolTips[count++]);

        Dimension dim = stepOverButton.getPreferredSize();
        breakButton.setPreferredSize(dim);
        breakButton.setMinimumSize(dim);
        breakButton.setMaximumSize(dim);
        breakButton.setSize(dim);
        goButton.setPreferredSize(dim);
        goButton.setMinimumSize(dim);
        goButton.setMaximumSize(dim);
        stepIntoButton.setPreferredSize(dim);
        stepIntoButton.setMinimumSize(dim);
        stepIntoButton.setMaximumSize(dim);
        stepOverButton.setPreferredSize(dim);
        stepOverButton.setMinimumSize(dim);
        stepOverButton.setMaximumSize(dim);
        stepOutButton.setPreferredSize(dim);
        stepOutButton.setMinimumSize(dim);
        stepOutButton.setMaximumSize(dim);
        toolBar.add(breakButton);
        toolBar.add(goButton);
        toolBar.add(stepIntoButton);
        toolBar.add(stepOverButton);
        toolBar.add(stepOutButton);

        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());
        getContentPane().add(toolBar, BorderLayout.NORTH);
        getContentPane().add(contentPane, BorderLayout.CENTER);
        desk = new JDesktopPane();
        desk.setPreferredSize(new Dimension(600, 300));
        desk.setMinimumSize(new Dimension(150, 50));
        desk.add(console = new JSInternalConsole("JavaScript Console"));
        context = new ContextWindow(this);
        context.setPreferredSize(new Dimension(600, 120));
        context.setMinimumSize(new Dimension(50, 50));

        split1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, desk,
                                          context);
        split1.setOneTouchExpandable(true);
        SwingGui.setResizeWeight(split1, 0.66);
        contentPane.add(split1, BorderLayout.CENTER);
        statusBar = new JLabel();
        statusBar.setText("Thread: ");
        contentPane.add(statusBar, BorderLayout.SOUTH);
        dlg = new JFileChooser();

        javax.swing.filechooser.FileFilter filter =
            new javax.swing.filechooser.FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        if (f.isDirectory()) {
                            return true;
                        }
                        String n = f.getName();
                        int i = n.lastIndexOf('.');
                        if (i > 0 && i < n.length() -1) {
                            String ext = n.substring(i + 1).toLowerCase();
                            if (ext.equals("js")) {
                                return true;
                            }
                        }
                        return false;
                    }

                    @Override
                    public String getDescription() {
                        return "JavaScript Files (*.js)";
                    }
                };
        dlg.addChoosableFileFilter(filter);
        addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    exit();
                }
            });
    }

    /**
     * Runs the {@link #exitAction}.
     */
    private void exit() {
        if (exitAction != null) {
            SwingUtilities.invokeLater(exitAction);
        }
        dim.setReturnValue(Dim.EXIT);
    }

    /**
     * Returns the {@link FileWindow} for the given URL.
     */
    FileWindow getFileWindow(String url) {
        if (url == null || url.equals("<stdin>")) {
            return null;
        }
        return fileWindows.get(url);
    }

    /**
     * Returns a short version of the given URL.
     */
    static String getShortName(String url) {
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash < 0) {
            lastSlash = url.lastIndexOf('\\');
        }
        String shortName = url;
        if (lastSlash >= 0 && lastSlash + 1 < url.length()) {
            shortName = url.substring(lastSlash + 1);
        }
        return shortName;
    }

    /**
     * Closes the given {@link FileWindow}.
     */
    void removeWindow(FileWindow w) {
        fileWindows.remove(w.getUrl());
        JMenu windowMenu = getWindowMenu();
        int count = windowMenu.getItemCount();
        JMenuItem lastItem = windowMenu.getItem(count -1);
        String name = getShortName(w.getUrl());
        for (int i = 5; i < count; i++) {
            JMenuItem item = windowMenu.getItem(i);
            if (item == null) continue; // separator
            String text = item.getText();
            //1 D:\foo.js
            //2 D:\bar.js
            int pos = text.indexOf(' ');
            if (text.substring(pos + 1).equals(name)) {
                windowMenu.remove(item);
                // Cascade    [0]
                // Tile       [1]
                // -------    [2]
                // Console    [3]
                // -------    [4]
                if (count == 6) {
                    // remove the final separator
                    windowMenu.remove(4);
                } else {
                    int j = i - 4;
                    for (;i < count -1; i++) {
                        JMenuItem thisItem = windowMenu.getItem(i);
                        if (thisItem != null) {
                            //1 D:\foo.js
                            //2 D:\bar.js
                            text = thisItem.getText();
                            if (text.equals("More Windows...")) {
                                break;
                            } else {
                                pos = text.indexOf(' ');
                                thisItem.setText((char)('0' + j) + " " +
                                                 text.substring(pos + 1));
                                thisItem.setMnemonic('0' + j);
                                j++;
                            }
                        }
                    }
                    if (count - 6 == 0 && lastItem != item) {
                        if (lastItem.getText().equals("More Windows...")) {
                            windowMenu.remove(lastItem);
                        }
                    }
                }
                break;
            }
        }
        windowMenu.revalidate();
    }

    /**
     * Shows the line at which execution in the given stack frame just stopped.
     */
    void showStopLine(Dim.StackFrame frame) {
        String sourceName = frame.getUrl();
        if (sourceName == null || sourceName.equals("<stdin>")) {
            if (console.isVisible()) {
                console.show();
            }
        } else {
            showFileWindow(sourceName, -1);
            int lineNumber = frame.getLineNumber();
            FileWindow w = getFileWindow(sourceName);
            if (w != null) {
                setFilePosition(w, lineNumber);
            }
        }
    }

    /**
     * Shows a {@link FileWindow} for the given source, creating it
     * if it doesn't exist yet. if <code>lineNumber</code> is greater
     * than -1, it indicates the line number to select and display.
     * @param sourceUrl the source URL
     * @param lineNumber the line number to select, or -1
     */
    protected void showFileWindow(String sourceUrl, int lineNumber) {
        FileWindow w = getFileWindow(sourceUrl);
        if (w == null) {
            Dim.SourceInfo si = dim.sourceInfo(sourceUrl);
            createFileWindow(si, -1);
            w = getFileWindow(sourceUrl);
        }
        if (lineNumber > -1) {
            int start = w.getPosition(lineNumber-1);
            int end = w.getPosition(lineNumber)-1;
            w.textArea.select(start);
            w.textArea.setCaretPosition(start);
            w.textArea.moveCaretPosition(end);
        }
        try {
            if (w.isIcon()) {
                w.setIcon(false);
            }
            w.setVisible(true);
            w.moveToFront();
            w.setSelected(true);
            requestFocus();
            w.requestFocus();
            w.textArea.requestFocus();
        } catch (Exception exc) {
        }
    }

    /**
     * Creates and shows a new {@link FileWindow} for the given source.
     */
    protected void createFileWindow(Dim.SourceInfo sourceInfo, int line) {
        boolean activate = true;

        String url = sourceInfo.url();
        FileWindow w = new FileWindow(this, sourceInfo);
        fileWindows.put(url, w);
        if (line != -1) {
            if (currentWindow != null) {
                currentWindow.setPosition(-1);
            }
            try {
                w.setPosition(w.textArea.getLineStartOffset(line-1));
            } catch (BadLocationException exc) {
                try {
                    w.setPosition(w.textArea.getLineStartOffset(0));
                } catch (BadLocationException ee) {
                    w.setPosition(-1);
                }
            }
        }
        desk.add(w);
        if (line != -1) {
            currentWindow = w;
        }
        menubar.addFile(url);
        w.setVisible(true);

        if (activate) {
            try {
                w.setMaximum(true);
                w.setSelected(true);
                w.moveToFront();
            } catch (Exception exc) {
            }
        }
    }

    /**
     * Update the source text for <code>sourceInfo</code>. This returns true
     * if a {@link FileWindow} for the given source exists and could be updated.
     * Otherwise, this does nothing and returns false.
     * @param sourceInfo the source info
     * @return true if a {@link FileWindow} for the given source exists
     *              and could be updated, false otherwise.
     */
    protected boolean updateFileWindow(Dim.SourceInfo sourceInfo) {
        String fileName = sourceInfo.url();
        FileWindow w = getFileWindow(fileName);
        if (w != null) {
            w.updateText(sourceInfo);
            w.show();
            return true;
        }
        return false;
    }

    /**
     * Moves the current position in the given {@link FileWindow} to the
     * given line.
     */
    private void setFilePosition(FileWindow w, int line) {
        boolean activate = true;
        JTextArea ta = w.textArea;
        try {
            if (line == -1) {
                w.setPosition(-1);
                if (currentWindow == w) {
                    currentWindow = null;
                }
            } else {
                int loc = ta.getLineStartOffset(line-1);
                if (currentWindow != null && currentWindow != w) {
                    currentWindow.setPosition(-1);
                }
                w.setPosition(loc);
                currentWindow = w;
            }
        } catch (BadLocationException exc) {
            // fix me
        }
        if (activate) {
            if (w.isIcon()) {
                desk.getDesktopManager().deiconifyFrame(w);
            }
            desk.getDesktopManager().activateFrame(w);
            try {
                w.show();
                w.toFront();  // required for correct frame layering (JDK 1.4.1)
                w.setSelected(true);
            } catch (Exception exc) {
            }
        }
    }

    /**
     * Handles script interruption.
     */
    void enterInterruptImpl(Dim.StackFrame lastFrame,
                            String threadTitle, String alertMessage) {
        statusBar.setText("Thread: " + threadTitle);

        showStopLine(lastFrame);

        if (alertMessage != null) {
            MessageDialogWrapper.showMessageDialog(this,
                                                   alertMessage,
                                                   "Exception in Script",
                                                   JOptionPane.ERROR_MESSAGE);
        }

        updateEnabled(true);

        Dim.ContextData contextData = lastFrame.contextData();

        JComboBox ctx = context.context;
        List<String> toolTips = context.toolTips;
        context.disableUpdate();
        int frameCount = contextData.frameCount();
        ctx.removeAllItems();
        // workaround for JDK 1.4 bug that caches selected value even after
        // removeAllItems() is called
        ctx.setSelectedItem(null);
        toolTips.clear();
        for (int i = 0; i < frameCount; i++) {
            Dim.StackFrame frame = contextData.getFrame(i);
            String url = frame.getUrl();
            int lineNumber = frame.getLineNumber();
            String shortName = url;
            if (url.length() > 20) {
                shortName = "..." + url.substring(url.length() - 17);
            }
            String location = "\"" + shortName + "\", line " + lineNumber;
            ctx.insertItemAt(location, i);
            location = "\"" + url + "\", line " + lineNumber;
            toolTips.add(location);
        }
        context.enableUpdate();
        ctx.setSelectedIndex(0);
        ctx.setMinimumSize(new Dimension(50, ctx.getMinimumSize().height));
    }

    /**
     * Returns the 'Window' menu.
     */
    private JMenu getWindowMenu() {
        return menubar.getMenu(3);
    }

    /**
     * Displays a {@link JFileChooser} and returns the selected filename.
     */
    private String chooseFile(String title) {
        dlg.setDialogTitle(title);
        File CWD = null;
        String dir = SecurityUtilities.getSystemProperty("user.dir");
        if (dir != null) {
            CWD = new File(dir);
        }
        if (CWD != null) {
            dlg.setCurrentDirectory(CWD);
        }
        int returnVal = dlg.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                String result = dlg.getSelectedFile().getCanonicalPath();
                CWD = dlg.getSelectedFile().getParentFile();
                Properties props = System.getProperties();
                props.put("user.dir", CWD.getPath());
                System.setProperties(props);
                return result;
            } catch (IOException ignored) {
            } catch (SecurityException ignored) {
            }
        }
        return null;
    }

    /**
     * Returns the current selected internal frame.
     */
    private JInternalFrame getSelectedFrame() {
       JInternalFrame[] frames = desk.getAllFrames();
       for (int i = 0; i < frames.length; i++) {
           if (frames[i].isShowing()) {
               return frames[i];
           }
       }
       return frames[frames.length - 1];
    }

    /**
     * Enables or disables the menu and tool bars with respect to the
     * state of script execution.
     */
    private void updateEnabled(boolean interrupted) {
        ((Menubar)getJMenuBar()).updateEnabled(interrupted);
        for (int ci = 0, cc = toolBar.getComponentCount(); ci < cc; ci++) {
            boolean enableButton;
            if (ci == 0) {
                // Break
                enableButton = !interrupted;
            } else {
                enableButton = interrupted;
            }
            toolBar.getComponent(ci).setEnabled(enableButton);
        }
        if (interrupted) {
            toolBar.setEnabled(true);
            // raise the debugger window
            int state = getExtendedState();
            if (state == Frame.ICONIFIED) {
                setExtendedState(Frame.NORMAL);
            }
            toFront();
            context.setEnabled(true);
        } else {
            if (currentWindow != null) currentWindow.setPosition(-1);
            context.setEnabled(false);
        }
    }

    /**
     * Calls {@link JSplitPane#setResizeWeight} via reflection.
     * For compatibility, since JDK &lt; 1.3 does not have this method.
     */
    static void setResizeWeight(JSplitPane pane, double weight) {
        try {
            Method m = JSplitPane.class.getMethod("setResizeWeight",
                                                  new Class[]{double.class});
            m.invoke(pane, new Object[]{new Double(weight)});
        } catch (NoSuchMethodException exc) {
        } catch (IllegalAccessException exc) {
        } catch (java.lang.reflect.InvocationTargetException exc) {
        }
    }

    /**
     * Reads the file with the given name and returns its contents as a String.
     */
    private String readFile(String fileName) {
        String text;
        try {
            Reader r = new FileReader(fileName);
            try {
                text = Kit.readReader(r);
            } finally {
                r.close();
            }
        } catch (IOException ex) {
            MessageDialogWrapper.showMessageDialog(this,
                                                   ex.getMessage(),
                                                   "Error reading "+fileName,
                                                   JOptionPane.ERROR_MESSAGE);
            text = null;
        }
        return text;
    }

    // GuiCallback

    /**
     * Called when the source text for a script has been updated.
     */
    public void updateSourceText(Dim.SourceInfo sourceInfo) {
        RunProxy proxy = new RunProxy(this, RunProxy.UPDATE_SOURCE_TEXT);
        proxy.sourceInfo = sourceInfo;
        SwingUtilities.invokeLater(proxy);
    }

    /**
     * Called when the interrupt loop has been entered.
     */
    public void enterInterrupt(Dim.StackFrame lastFrame,
                               String threadTitle,
                               String alertMessage) {
        if (SwingUtilities.isEventDispatchThread()) {
            enterInterruptImpl(lastFrame, threadTitle, alertMessage);
        } else {
            RunProxy proxy = new RunProxy(this, RunProxy.ENTER_INTERRUPT);
            proxy.lastFrame = lastFrame;
            proxy.threadTitle = threadTitle;
            proxy.alertMessage = alertMessage;
            SwingUtilities.invokeLater(proxy);
        }
    }

    /**
     * Returns whether the current thread is the GUI event thread.
     */
    public boolean isGuiEventThread() {
        return SwingUtilities.isEventDispatchThread();
    }

    /**
     * Processes the next GUI event.
     */
    public void dispatchNextGuiEvent() throws InterruptedException {
        EventQueue queue = awtEventQueue;
        if (queue == null) {
            queue = Toolkit.getDefaultToolkit().getSystemEventQueue();
            awtEventQueue = queue;
        }
        AWTEvent event = queue.getNextEvent();
        if (event instanceof ActiveEvent) {
            ((ActiveEvent)event).dispatch();
        } else {
            Object source = event.getSource();
            if (source instanceof Component) {
                Component comp = (Component)source;
                comp.dispatchEvent(event);
            } else if (source instanceof MenuComponent) {
                ((MenuComponent)source).dispatchEvent(event);
            }
        }
    }

    // ActionListener

    /**
     * Performs an action from the menu or toolbar.
     */
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        int returnValue = -1;
        if (cmd.equals("Cut") || cmd.equals("Copy") || cmd.equals("Paste")) {
            JInternalFrame f = getSelectedFrame();
            if (f != null && f instanceof ActionListener) {
                ((ActionListener)f).actionPerformed(e);
            }
        } else if (cmd.equals("Step Over")) {
            returnValue = Dim.STEP_OVER;
        } else if (cmd.equals("Step Into")) {
            returnValue = Dim.STEP_INTO;
        } else if (cmd.equals("Step Out")) {
            returnValue = Dim.STEP_OUT;
        } else if (cmd.equals("Go")) {
            returnValue = Dim.GO;
        } else if (cmd.equals("Break")) {
            dim.setBreak();
        } else if (cmd.equals("Exit")) {
            exit();
        } else if (cmd.equals("Open")) {
            String fileName = chooseFile("Select a file to compile");
            if (fileName != null) {
                String text = readFile(fileName);
                if (text != null) {
                    RunProxy proxy = new RunProxy(this, RunProxy.OPEN_FILE);
                    proxy.fileName = fileName;
                    proxy.text = text;
                    new Thread(proxy).start();
                }
            }
        } else if (cmd.equals("Load")) {
            String fileName = chooseFile("Select a file to execute");
            if (fileName != null) {
                String text = readFile(fileName);
                if (text != null) {
                    RunProxy proxy = new RunProxy(this, RunProxy.LOAD_FILE);
                    proxy.fileName = fileName;
                    proxy.text = text;
                    new Thread(proxy).start();
                }
            }
        } else if (cmd.equals("More Windows...")) {
            MoreWindows dlg = new MoreWindows(this, fileWindows,
                                              "Window", "Files");
            dlg.showDialog(this);
        } else if (cmd.equals("Console")) {
            if (console.isIcon()) {
                desk.getDesktopManager().deiconifyFrame(console);
            }
            console.show();
            desk.getDesktopManager().activateFrame(console);
            console.consoleTextArea.requestFocus();
        } else if (cmd.equals("Cut")) {
        } else if (cmd.equals("Copy")) {
        } else if (cmd.equals("Paste")) {
        } else if (cmd.equals("Go to function...")) {
            FindFunction dlg = new FindFunction(this, "Go to function",
                                                "Function");
            dlg.showDialog(this);
        } else if (cmd.equals("Tile")) {
            JInternalFrame[] frames = desk.getAllFrames();
            int count = frames.length;
            int rows, cols;
            rows = cols = (int)Math.sqrt(count);
            if (rows*cols < count) {
                cols++;
                if (rows * cols < count) {
                    rows++;
                }
            }
            Dimension size = desk.getSize();
            int w = size.width/cols;
            int h = size.height/rows;
            int x = 0;
            int y = 0;
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    int index = (i*cols) + j;
                    if (index >= frames.length) {
                        break;
                    }
                    JInternalFrame f = frames[index];
                    try {
                        f.setIcon(false);
                        f.setMaximum(false);
                    } catch (Exception exc) {
                    }
                    desk.getDesktopManager().setBoundsForFrame(f, x, y,
                                                               w, h);
                    x += w;
                }
                y += h;
                x = 0;
            }
        } else if (cmd.equals("Cascade")) {
            JInternalFrame[] frames = desk.getAllFrames();
            int count = frames.length;
            int x, y, w, h;
            x = y = 0;
            h = desk.getHeight();
            int d = h / count;
            if (d > 30) d = 30;
            for (int i = count -1; i >= 0; i--, x += d, y += d) {
                JInternalFrame f = frames[i];
                try {
                    f.setIcon(false);
                    f.setMaximum(false);
                } catch (Exception exc) {
                }
                Dimension dimen = f.getPreferredSize();
                w = dimen.width;
                h = dimen.height;
                desk.getDesktopManager().setBoundsForFrame(f, x, y, w, h);
            }
        } else {
            Object obj = getFileWindow(cmd);
            if (obj != null) {
                FileWindow w = (FileWindow)obj;
                try {
                    if (w.isIcon()) {
                        w.setIcon(false);
                    }
                    w.setVisible(true);
                    w.moveToFront();
                    w.setSelected(true);
                } catch (Exception exc) {
                }
            }
        }
        if (returnValue != -1) {
            updateEnabled(false);
            dim.setReturnValue(returnValue);
        }
    }
}

/**
 * Helper class for showing a message dialog.
 */
class MessageDialogWrapper {

    /**
     * Shows a message dialog, wrapping the <code>msg</code> at 60
     * columns.
     */
    public static void showMessageDialog(Component parent, String msg,
                                         String title, int flags) {
        if (msg.length() > 60) {
            StringBuffer buf = new StringBuffer();
            int len = msg.length();
            int j = 0;
            int i;
            for (i = 0; i < len; i++, j++) {
                char c = msg.charAt(i);
                buf.append(c);
                if (Character.isWhitespace(c)) {
                    int k;
                    for (k = i + 1; k < len; k++) {
                        if (Character.isWhitespace(msg.charAt(k))) {
                            break;
                        }
                    }
                    if (k < len) {
                        int nextWordLen = k - i;
                        if (j + nextWordLen > 60) {
                            buf.append('\n');
                            j = 0;
                        }
                    }
                }
            }
            msg = buf.toString();
        }
        JOptionPane.showMessageDialog(parent, msg, title, flags);
    }
}

/**
 * Extension of JTextArea for script evaluation input.
 */
class EvalTextArea
    extends JTextArea
    implements KeyListener, DocumentListener {

    /**
     * Serializable magic number.
     */
    private static final long serialVersionUID = -3918033649601064194L;

    /**
     * The debugger GUI.
     */
    private SwingGui debugGui;

    /**
     * History of expressions that have been evaluated
     */
    private List<String> history;

    /**
     * Index of the selected history item.
     */
    private int historyIndex = -1;

    /**
     * Position in the display where output should go.
     */
    private int outputMark;

    /**
     * Creates a new EvalTextArea.
     */
    public EvalTextArea(SwingGui debugGui) {
        this.debugGui = debugGui;
        history = Collections.synchronizedList(new ArrayList<String>());
        Document doc = getDocument();
        doc.addDocumentListener(this);
        addKeyListener(this);
        setLineWrap(true);
        setFont(new Font("Monospaced", 0, 12));
        append("% ");
        outputMark = doc.getLength();
    }

    /**
     * Selects a subrange of the text.
     */
    @Override
    public void select(int start, int end) {
        //requestFocus();
        super.select(start, end);
    }

    /**
     * Called when Enter is pressed.
     */
    private synchronized void returnPressed() {
        Document doc = getDocument();
        int len = doc.getLength();
        Segment segment = new Segment();
        try {
            doc.getText(outputMark, len - outputMark, segment);
        } catch (javax.swing.text.BadLocationException ignored) {
            ignored.printStackTrace();
        }
        String text = segment.toString();
        if (debugGui.dim.stringIsCompilableUnit(text)) {
            if (text.trim().length() > 0) {
               history.add(text);
               historyIndex = history.size();
            }
            append("\n");
            String result = debugGui.dim.eval(text);
            if (result.length() > 0) {
                append(result);
                append("\n");
            }
            append("% ");
            outputMark = doc.getLength();
        } else {
            append("\n");
        }
    }

    /**
     * Writes output into the text area.
     */
    public synchronized void write(String str) {
        insert(str, outputMark);
        int len = str.length();
        outputMark += len;
        select(outputMark, outputMark);
    }

    // KeyListener

    /**
     * Called when a key is pressed.
     */
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_BACK_SPACE || code == KeyEvent.VK_LEFT) {
            if (outputMark == getCaretPosition()) {
                e.consume();
            }
        } else if (code == KeyEvent.VK_HOME) {
           int caretPos = getCaretPosition();
           if (caretPos == outputMark) {
               e.consume();
           } else if (caretPos > outputMark) {
               if (!e.isControlDown()) {
                   if (e.isShiftDown()) {
                       moveCaretPosition(outputMark);
                   } else {
                       setCaretPosition(outputMark);
                   }
                   e.consume();
               }
           }
        } else if (code == KeyEvent.VK_ENTER) {
            returnPressed();
            e.consume();
        } else if (code == KeyEvent.VK_UP) {
            historyIndex--;
            if (historyIndex >= 0) {
                if (historyIndex >= history.size()) {
                    historyIndex = history.size() -1;
                }
                if (historyIndex >= 0) {
                    String str = history.get(historyIndex);
                    int len = getDocument().getLength();
                    replaceRange(str, outputMark, len);
                    int caretPos = outputMark + str.length();
                    select(caretPos, caretPos);
                } else {
                    historyIndex++;
                }
            } else {
                historyIndex++;
            }
            e.consume();
        } else if (code == KeyEvent.VK_DOWN) {
            int caretPos = outputMark;
            if (history.size() > 0) {
                historyIndex++;
                if (historyIndex < 0) {historyIndex = 0;}
                int len = getDocument().getLength();
                if (historyIndex < history.size()) {
                    String str = history.get(historyIndex);
                    replaceRange(str, outputMark, len);
                    caretPos = outputMark + str.length();
                } else {
                    historyIndex = history.size();
                    replaceRange("", outputMark, len);
                }
            }
            select(caretPos, caretPos);
            e.consume();
        }
    }

    /**
     * Called when a key is typed.
     */
    public void keyTyped(KeyEvent e) {
        int keyChar = e.getKeyChar();
        if (keyChar == 0x8 /* KeyEvent.VK_BACK_SPACE */) {
            if (outputMark == getCaretPosition()) {
                e.consume();
            }
        } else if (getCaretPosition() < outputMark) {
            setCaretPosition(outputMark);
        }
    }

    /**
     * Called when a key is released.
     */
    public synchronized void keyReleased(KeyEvent e) {
    }

    // DocumentListener

    /**
     * Called when text was inserted into the text area.
     */
    public synchronized void insertUpdate(DocumentEvent e) {
        int len = e.getLength();
        int off = e.getOffset();
        if (outputMark > off) {
            outputMark += len;
        }
    }

    /**
     * Called when text was removed from the text area.
     */
    public synchronized void removeUpdate(DocumentEvent e) {
        int len = e.getLength();
        int off = e.getOffset();
        if (outputMark > off) {
            if (outputMark >= off + len) {
                outputMark -= len;
            } else {
                outputMark = off;
            }
        }
    }

    /**
     * Attempts to clean up the damage done by {@link #updateUI()}.
     */
    public synchronized void postUpdateUI() {
        //requestFocus();
        setCaret(getCaret());
        select(outputMark, outputMark);
    }

    /**
     * Called when text has changed in the text area.
     */
    public synchronized void changedUpdate(DocumentEvent e) {
    }
}

/**
 * An internal frame for evaluating script.
 */
class EvalWindow extends JInternalFrame implements ActionListener {

    /**
     * Serializable magic number.
     */
    private static final long serialVersionUID = -2860585845212160176L;

    /**
     * The text area into which expressions can be typed.
     */
    private EvalTextArea evalTextArea;

    /**
     * Creates a new EvalWindow.
     */
    public EvalWindow(String name, SwingGui debugGui) {
        super(name, true, false, true, true);
        evalTextArea = new EvalTextArea(debugGui);
        evalTextArea.setRows(24);
        evalTextArea.setColumns(80);
        JScrollPane scroller = new JScrollPane(evalTextArea);
        setContentPane(scroller);
        //scroller.setPreferredSize(new Dimension(600, 400));
        pack();
        setVisible(true);
    }

    /**
     * Sets whether the text area is enabled.
     */
    @Override
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        evalTextArea.setEnabled(b);
    }

    // ActionListener

    /**
     * Performs an action on the text area.
     */
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (cmd.equals("Cut")) {
            evalTextArea.cut();
        } else if (cmd.equals("Copy")) {
            evalTextArea.copy();
        } else if (cmd.equals("Paste")) {
            evalTextArea.paste();
        }
    }
}

/**
 * Internal frame for the console.
 */
class JSInternalConsole extends JInternalFrame implements ActionListener {

    /**
     * Serializable magic number.
     */
    private static final long serialVersionUID = -5523468828771087292L;

    /**
     * Creates a new JSInternalConsole.
     */
    public JSInternalConsole(String name) {
        super(name, true, false, true, true);
        consoleTextArea = new ConsoleTextArea(null);
        consoleTextArea.setRows(24);
        consoleTextArea.setColumns(80);
        JScrollPane scroller = new JScrollPane(consoleTextArea);
        setContentPane(scroller);
        pack();
        addInternalFrameListener(new InternalFrameAdapter() {
                @Override
                public void internalFrameActivated(InternalFrameEvent e) {
                    // hack
                    if (consoleTextArea.hasFocus()) {
                        consoleTextArea.getCaret().setVisible(false);
                        consoleTextArea.getCaret().setVisible(true);
                    }
                }
            });
    }

    /**
     * The console text area.
     */
    ConsoleTextArea consoleTextArea;

    /**
     * Returns the input stream of the console text area.
     */
    public InputStream getIn() {
        return consoleTextArea.getIn();
    }

    /**
     * Returns the output stream of the console text area.
     */
    public PrintStream getOut() {
        return consoleTextArea.getOut();
    }

    /**
     * Returns the error stream of the console text area.
     */
    public PrintStream getErr() {
        return consoleTextArea.getErr();
    }

    // ActionListener

    /**
     * Performs an action on the text area.
     */
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (cmd.equals("Cut")) {
            consoleTextArea.cut();
        } else if (cmd.equals("Copy")) {
            consoleTextArea.copy();
        } else if (cmd.equals("Paste")) {
            consoleTextArea.paste();
        }
    }
}

/**
 * Popup menu class for right-clicking on {@link FileTextArea}s.
 */
class FilePopupMenu extends JPopupMenu {

    /**
     * Serializable magic number.
     */
    private static final long serialVersionUID = 3589525009546013565L;

    /**
     * The popup x position.
     */
    int x;

    /**
     * The popup y position.
     */
    int y;

    /**
     * Creates a new FilePopupMenu.
     */
    public FilePopupMenu(FileTextArea w) {
        JMenuItem item;
        add(item = new JMenuItem("Set Breakpoint"));
        item.addActionListener(w);
        add(item = new JMenuItem("Clear Breakpoint"));
        item.addActionListener(w);
        add(item = new JMenuItem("Run"));
        item.addActionListener(w);
    }

    /**
     * Displays the menu at the given coordinates.
     */
    public void show(JComponent comp, int x, int y) {
        this.x = x;
        this.y = y;
        super.show(comp, x, y);
    }
}

/**
 * Text area to display script source.
 */
class FileTextArea
    extends JTextArea
    implements ActionListener, PopupMenuListener, KeyListener, MouseListener {

    /**
     * Serializable magic number.
     */
    private static final long serialVersionUID = -25032065448563720L;

    /**
     * The owning {@link FileWindow}.
     */
    private FileWindow w;

    /**
     * The popup menu.
     */
    private FilePopupMenu popup;

    /**
     * Creates a new FileTextArea.
     */
    public FileTextArea(FileWindow w) {
        this.w = w;
        popup = new FilePopupMenu(this);
        popup.addPopupMenuListener(this);
        addMouseListener(this);
        addKeyListener(this);
        setFont(new Font("Monospaced", 0, 12));
    }

    /**
     * Moves the selection to the given offset.
     */
    public void select(int pos) {
        if (pos >= 0) {
            try {
                int line = getLineOfOffset(pos);
                Rectangle rect = modelToView(pos);
                if (rect == null) {
                    select(pos, pos);
                } else {
                    try {
                        Rectangle nrect =
                            modelToView(getLineStartOffset(line + 1));
                        if (nrect != null) {
                            rect = nrect;
                        }
                    } catch (Exception exc) {
                    }
                    JViewport vp = (JViewport)getParent();
                    Rectangle viewRect = vp.getViewRect();
                    if (viewRect.y + viewRect.height > rect.y) {
                        // need to scroll up
                        select(pos, pos);
                    } else {
                        // need to scroll down
                        rect.y += (viewRect.height - rect.height)/2;
                        scrollRectToVisible(rect);
                        select(pos, pos);
                    }
                }
            } catch (BadLocationException exc) {
                select(pos, pos);
                //exc.printStackTrace();
            }
        }
    }

    /**
     * Checks if the popup menu should be shown.
     */
    private void checkPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
            popup.show(this, e.getX(), e.getY());
        }
    }

    // MouseListener

    /**
     * Called when a mouse button is pressed.
     */
    public void mousePressed(MouseEvent e) {
        checkPopup(e);
    }

    /**
     * Called when the mouse is clicked.
     */
    public void mouseClicked(MouseEvent e) {
        checkPopup(e);
        requestFocus();
        getCaret().setVisible(true);
    }

    /**
     * Called when the mouse enters the component.
     */
    public void mouseEntered(MouseEvent e) {
    }

    /**
     * Called when the mouse exits the component.
     */
    public void mouseExited(MouseEvent e) {
    }

    /**
     * Called when a mouse button is released.
     */
    public void mouseReleased(MouseEvent e) {
        checkPopup(e);
    }

    // PopupMenuListener

    /**
     * Called before the popup menu will become visible.
     */
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
    }

    /**
     * Called before the popup menu will become invisible.
     */
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
    }

    /**
     * Called when the popup menu is cancelled.
     */
    public void popupMenuCanceled(PopupMenuEvent e) {
    }

    // ActionListener

    /**
     * Performs an action.
     */
    public void actionPerformed(ActionEvent e) {
        int pos = viewToModel(new Point(popup.x, popup.y));
        popup.setVisible(false);
        String cmd = e.getActionCommand();
        int line = -1;
        try {
            line = getLineOfOffset(pos);
        } catch (Exception exc) {
        }
        if (cmd.equals("Set Breakpoint")) {
            w.setBreakPoint(line + 1);
        } else if (cmd.equals("Clear Breakpoint")) {
            w.clearBreakPoint(line + 1);
        } else if (cmd.equals("Run")) {
            w.load();
        }
    }

    // KeyListener

    /**
     * Called when a key is pressed.
     */
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
        case KeyEvent.VK_BACK_SPACE:
        case KeyEvent.VK_ENTER:
        case KeyEvent.VK_DELETE:
        case KeyEvent.VK_TAB:
            e.consume();
            break;
        }
    }

    /**
     * Called when a key is typed.
     */
    public void keyTyped(KeyEvent e) {
        e.consume();
    }

    /**
     * Called when a key is released.
     */
    public void keyReleased(KeyEvent e) {
        e.consume();
    }
}

/**
 * Dialog to list the available windows.
 */
class MoreWindows extends JDialog implements ActionListener {

    /**
     * Serializable magic number.
     */
    private static final long serialVersionUID = 5177066296457377546L;

    /**
     * Last selected value.
     */
    private String value;

    /**
     * The list component.
     */
    private JList list;

    /**
     * Our parent frame.
     */
    private SwingGui swingGui;

    /**
     * The "Select" button.
     */
    private JButton setButton;

    /**
     * The "Cancel" button.
     */
    private JButton cancelButton;

    /**
     * Creates a new MoreWindows.
     */
    MoreWindows(SwingGui frame, Map<String,FileWindow> fileWindows, String title,
                String labelText) {
        super(frame, title, true);
        this.swingGui = frame;
        //buttons
        cancelButton = new JButton("Cancel");
        setButton = new JButton("Select");
        cancelButton.addActionListener(this);
        setButton.addActionListener(this);
        getRootPane().setDefaultButton(setButton);

        //dim part of the dialog
        list = new JList(new DefaultListModel());
        DefaultListModel model = (DefaultListModel)list.getModel();
        model.clear();
        //model.fireIntervalRemoved(model, 0, size);
        for (String data: fileWindows.keySet()) {
            model.addElement(data);
        }
        list.setSelectedIndex(0);
        //model.fireIntervalAdded(model, 0, data.length);
        setButton.setEnabled(true);
        list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        list.addMouseListener(new MouseHandler());
        JScrollPane listScroller = new JScrollPane(list);
        listScroller.setPreferredSize(new Dimension(320, 240));
        //XXX: Must do the following, too, or else the scroller thinks
        //XXX: it's taller than it is:
        listScroller.setMinimumSize(new Dimension(250, 80));
        listScroller.setAlignmentX(LEFT_ALIGNMENT);

        //Create a container so that we can add a title around
        //the scroll pane.  Can't add a title directly to the
        //scroll pane because its background would be white.
        //Lay out the label and scroll pane from top to button.
        JPanel listPane = new JPanel();
        listPane.setLayout(new BoxLayout(listPane, BoxLayout.Y_AXIS));
        JLabel label = new JLabel(labelText);
        label.setLabelFor (list);
        listPane.add(label);
        listPane.add(Box.createRigidArea(new Dimension(0,5)));
        listPane.add(listScroller);
        listPane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        //Lay out the buttons from left to right.
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPane.add(setButton);

        //Put everything together, using the content pane's BorderLayout.
        Container contentPane = getContentPane();
        contentPane.add(listPane, BorderLayout.CENTER);
        contentPane.add(buttonPane, BorderLayout.SOUTH);
        pack();
        addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent ke) {
                    int code = ke.getKeyCode();
                    if (code == KeyEvent.VK_ESCAPE) {
                        ke.consume();
                        value = null;
                        setVisible(false);
                    }
                }
            });
    }

    /**
     * Shows the dialog.
     */
    public String showDialog(Component comp) {
        value = null;
        setLocationRelativeTo(comp);
        setVisible(true);
        return value;
    }

    // ActionListener

    /**
     * Performs an action.
     */
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (cmd.equals("Cancel")) {
            setVisible(false);
            value = null;
        } else if (cmd.equals("Select")) {
            value = (String)list.getSelectedValue();
            setVisible(false);
            swingGui.showFileWindow(value, -1);
        }
    }

    /**
     * MouseListener implementation for {@link #list}.
     */
    private class MouseHandler extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                setButton.doClick();
            }
        }
    }
}

/**
 * Find function dialog.
 */
class FindFunction extends JDialog implements ActionListener {

    /**
     * Serializable magic number.
     */
    private static final long serialVersionUID = 559491015232880916L;

    /**
     * Last selected function.
     */
    private String value;

    /**
     * List of functions.
     */
    private JList list;

    /**
     * The debug GUI frame.
     */
    private SwingGui debugGui;

    /**
     * The "Select" button.
     */
    private JButton setButton;

    /**
     * The "Cancel" button.
     */
    private JButton cancelButton;

    /**
     * Creates a new FindFunction.
     */
    public FindFunction(SwingGui debugGui, String title, String labelText) {
        super(debugGui, title, true);
        this.debugGui = debugGui;

        cancelButton = new JButton("Cancel");
        setButton = new JButton("Select");
        cancelButton.addActionListener(this);
        setButton.addActionListener(this);
        getRootPane().setDefaultButton(setButton);

        list = new JList(new DefaultListModel());
        DefaultListModel model = (DefaultListModel)list.getModel();
        model.clear();

        String[] a = debugGui.dim.functionNames();
        java.util.Arrays.sort(a);
        for (int i = 0; i < a.length; i++) {
            model.addElement(a[i]);
        }
        list.setSelectedIndex(0);

        setButton.setEnabled(a.length > 0);
        list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        list.addMouseListener(new MouseHandler());
        JScrollPane listScroller = new JScrollPane(list);
        listScroller.setPreferredSize(new Dimension(320, 240));
        listScroller.setMinimumSize(new Dimension(250, 80));
        listScroller.setAlignmentX(LEFT_ALIGNMENT);

        //Create a container so that we can add a title around
        //the scroll pane.  Can't add a title directly to the
        //scroll pane because its background would be white.
        //Lay out the label and scroll pane from top to button.
        JPanel listPane = new JPanel();
        listPane.setLayout(new BoxLayout(listPane, BoxLayout.Y_AXIS));
        JLabel label = new JLabel(labelText);
        label.setLabelFor (list);
        listPane.add(label);
        listPane.add(Box.createRigidArea(new Dimension(0,5)));
        listPane.add(listScroller);
        listPane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        //Lay out the buttons from left to right.
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPane.add(setButton);

        //Put everything together, using the content pane's BorderLayout.
        Container contentPane = getContentPane();
        contentPane.add(listPane, BorderLayout.CENTER);
        contentPane.add(buttonPane, BorderLayout.SOUTH);
        pack();
        addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent ke) {
                    int code = ke.getKeyCode();
                    if (code == KeyEvent.VK_ESCAPE) {
                        ke.consume();
                        value = null;
                        setVisible(false);
                    }
                }
            });
    }

    /**
     * Shows the dialog.
     */
    public String showDialog(Component comp) {
        value = null;
        setLocationRelativeTo(comp);
        setVisible(true);
        return value;
    }

    // ActionListener

    /**
     * Performs an action.
     */
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (cmd.equals("Cancel")) {
            setVisible(false);
            value = null;
        } else if (cmd.equals("Select")) {
            if (list.getSelectedIndex() < 0) {
                return;
            }
            try {
                value = (String)list.getSelectedValue();
            } catch (ArrayIndexOutOfBoundsException exc) {
                return;
            }
            setVisible(false);
            Dim.FunctionSource item = debugGui.dim.functionSourceByName(value);
            if (item != null) {
                Dim.SourceInfo si = item.sourceInfo();
                String url = si.url();
                int lineNumber = item.firstLine();
                debugGui.showFileWindow(url, lineNumber);
            }
        }
    }

    /**
     * MouseListener implementation for {@link #list}.
     */
    class MouseHandler extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                setButton.doClick();
            }
        }
    }
}

/**
 * Gutter for FileWindows.
 */
class FileHeader extends JPanel implements MouseListener {

    /**
     * Serializable magic number.
     */
    private static final long serialVersionUID = -2858905404778259127L;

    /**
     * The line that the mouse was pressed on.
     */
    private int pressLine = -1;

    /**
     * The owning FileWindow.
     */
    private FileWindow fileWindow;

    /**
     * Creates a new FileHeader.
     */
    public FileHeader(FileWindow fileWindow) {
        this.fileWindow = fileWindow;
        addMouseListener(this);
        update();
    }

    /**
     * Updates the gutter.
     */
    public void update() {
        FileTextArea textArea = fileWindow.textArea;
        Font font = textArea.getFont();
        setFont(font);
        FontMetrics metrics = getFontMetrics(font);
        int h = metrics.getHeight();
        int lineCount = textArea.getLineCount() + 1;
        String dummy = Integer.toString(lineCount);
        if (dummy.length() < 2) {
            dummy = "99";
        }
        Dimension d = new Dimension();
        d.width = metrics.stringWidth(dummy) + 16;
        d.height = lineCount * h + 100;
        setPreferredSize(d);
        setSize(d);
    }

    /**
     * Paints the component.
     */
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        FileTextArea textArea = fileWindow.textArea;
        Font font = textArea.getFont();
        g.setFont(font);
        FontMetrics metrics = getFontMetrics(font);
        Rectangle clip = g.getClipBounds();
        g.setColor(getBackground());
        g.fillRect(clip.x, clip.y, clip.width, clip.height);
        int ascent = metrics.getMaxAscent();
        int h = metrics.getHeight();
        int lineCount = textArea.getLineCount() + 1;
        String dummy = Integer.toString(lineCount);
        if (dummy.length() < 2) {
            dummy = "99";
        }
        int startLine = clip.y / h;
        int endLine = (clip.y + clip.height) / h + 1;
        int width = getWidth();
        if (endLine > lineCount) endLine = lineCount;
        for (int i = startLine; i < endLine; i++) {
            String text;
            int pos = -2;
            try {
                pos = textArea.getLineStartOffset(i);
            } catch (BadLocationException ignored) {
            }
            boolean isBreakPoint = fileWindow.isBreakPoint(i + 1);
            text = Integer.toString(i + 1) + " ";
            int y = i * h;
            g.setColor(Color.blue);
            g.drawString(text, 0, y + ascent);
            int x = width - ascent;
            if (isBreakPoint) {
                g.setColor(new Color(0x80, 0x00, 0x00));
                int dy = y + ascent - 9;
                g.fillOval(x, dy, 9, 9);
                g.drawOval(x, dy, 8, 8);
                g.drawOval(x, dy, 9, 9);
            }
            if (pos == fileWindow.currentPos) {
                Polygon arrow = new Polygon();
                int dx = x;
                y += ascent - 10;
                int dy = y;
                arrow.addPoint(dx, dy + 3);
                arrow.addPoint(dx + 5, dy + 3);
                for (x = dx + 5; x <= dx + 10; x++, y++) {
                    arrow.addPoint(x, y);
                }
                for (x = dx + 9; x >= dx + 5; x--, y++) {
                    arrow.addPoint(x, y);
                }
                arrow.addPoint(dx + 5, dy + 7);
                arrow.addPoint(dx, dy + 7);
                g.setColor(Color.yellow);
                g.fillPolygon(arrow);
                g.setColor(Color.black);
                g.drawPolygon(arrow);
            }
        }
    }

    // MouseListener

    /**
     * Called when the mouse enters the component.
     */
    public void mouseEntered(MouseEvent e) {
    }

    /**
     * Called when a mouse button is pressed.
     */
    public void mousePressed(MouseEvent e) {
        Font font = fileWindow.textArea.getFont();
        FontMetrics metrics = getFontMetrics(font);
        int h = metrics.getHeight();
        pressLine = e.getY() / h;
    }

    /**
     * Called when the mouse is clicked.
     */
    public void mouseClicked(MouseEvent e) {
    }

    /**
     * Called when the mouse exits the component.
     */
    public void mouseExited(MouseEvent e) {
    }

    /**
     * Called when a mouse button is released.
     */
    public void mouseReleased(MouseEvent e) {
        if (e.getComponent() == this
                && (e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0) {
            int y = e.getY();
            Font font = fileWindow.textArea.getFont();
            FontMetrics metrics = getFontMetrics(font);
            int h = metrics.getHeight();
            int line = y/h;
            if (line == pressLine) {
                fileWindow.toggleBreakPoint(line + 1);
            } else {
                pressLine = -1;
            }
        }
    }
}

/**
 * An internal frame for script files.
 */
class FileWindow extends JInternalFrame implements ActionListener {

    /**
     * Serializable magic number.
     */
    private static final long serialVersionUID = -6212382604952082370L;

    /**
     * The debugger GUI.
     */
    private SwingGui debugGui;

    /**
     * The SourceInfo object that describes the file.
     */
    private Dim.SourceInfo sourceInfo;

    /**
     * The FileTextArea that displays the file.
     */
    FileTextArea textArea;

    /**
     * The FileHeader that is the gutter for {@link #textArea}.
     */
    private FileHeader fileHeader;

    /**
     * Scroll pane for containing {@link #textArea}.
     */
    private JScrollPane p;

    /**
     * The current offset position.
     */
    int currentPos;

    /**
     * Loads the file.
     */
    void load() {
        String url = getUrl();
        if (url != null) {
            RunProxy proxy = new RunProxy(debugGui, RunProxy.LOAD_FILE);
            proxy.fileName = url;
            proxy.text = sourceInfo.source();
            new Thread(proxy).start();
        }
    }

    /**
     * Returns the offset position for the given line.
     */
    public int getPosition(int line) {
        int result = -1;
        try {
            result = textArea.getLineStartOffset(line);
        } catch (javax.swing.text.BadLocationException exc) {
        }
        return result;
    }

    /**
     * Returns whether the given line has a breakpoint.
     */
    public boolean isBreakPoint(int line) {
        return sourceInfo.breakableLine(line) && sourceInfo.breakpoint(line);
    }

    /**
     * Toggles the breakpoint on the given line.
     */
    public void toggleBreakPoint(int line) {
        if (!isBreakPoint(line)) {
            setBreakPoint(line);
        } else {
            clearBreakPoint(line);
        }
    }

    /**
     * Sets a breakpoint on the given line.
     */
    public void setBreakPoint(int line) {
        if (sourceInfo.breakableLine(line)) {
            boolean changed = sourceInfo.breakpoint(line, true);
            if (changed) {
                fileHeader.repaint();
            }
        }
    }

    /**
     * Clears a breakpoint from the given line.
     */
    public void clearBreakPoint(int line) {
        if (sourceInfo.breakableLine(line)) {
            boolean changed = sourceInfo.breakpoint(line, false);
            if (changed) {
                fileHeader.repaint();
            }
        }
    }

    /**
     * Creates a new FileWindow.
     */
    public FileWindow(SwingGui debugGui, Dim.SourceInfo sourceInfo) {
        super(SwingGui.getShortName(sourceInfo.url()),
              true, true, true, true);
        this.debugGui = debugGui;
        this.sourceInfo = sourceInfo;
        updateToolTip();
        currentPos = -1;
        textArea = new FileTextArea(this);
        textArea.setRows(24);
        textArea.setColumns(80);
        p = new JScrollPane();
        fileHeader = new FileHeader(this);
        p.setViewportView(textArea);
        p.setRowHeaderView(fileHeader);
        setContentPane(p);
        pack();
        updateText(sourceInfo);
        textArea.select(0);
    }

    /**
     * Updates the tool tip contents.
     */
    private void updateToolTip() {
        // Try to set tool tip on frame. On Mac OS X 10.5,
        // the number of components is different, so try to be safe.
        int n = getComponentCount() - 1;
        if (n > 1) {
            n = 1;
        } else if (n < 0) {
            return;
        }
        Component c = getComponent(n);
        // this will work at least for Metal L&F
        if (c != null && c instanceof JComponent) {
            ((JComponent)c).setToolTipText(getUrl());
        }
    }

    /**
     * Returns the URL of the source.
     */
    public String getUrl() {
        return sourceInfo.url();
    }

    /**
     * Called when the text of the script has changed.
     */
    public void updateText(Dim.SourceInfo sourceInfo) {
        this.sourceInfo = sourceInfo;
        String newText = sourceInfo.source();
        if (!textArea.getText().equals(newText)) {
            textArea.setText(newText);
            int pos = 0;
            if (currentPos != -1) {
                pos = currentPos;
            }
            textArea.select(pos);
        }
        fileHeader.update();
        fileHeader.repaint();
    }

    /**
     * Sets the cursor position.
     */
    public void setPosition(int pos) {
        textArea.select(pos);
        currentPos = pos;
        fileHeader.repaint();
    }

    /**
     * Selects a range of characters.
     */
    public void select(int start, int end) {
        int docEnd = textArea.getDocument().getLength();
        textArea.select(docEnd, docEnd);
        textArea.select(start, end);
    }

    /**
     * Disposes this FileWindow.
     */
    @Override
    public void dispose() {
        debugGui.removeWindow(this);
        super.dispose();
    }

    // ActionListener

    /**
     * Performs an action.
     */
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (cmd.equals("Cut")) {
            // textArea.cut();
        } else if (cmd.equals("Copy")) {
            textArea.copy();
        } else if (cmd.equals("Paste")) {
            // textArea.paste();
        }
    }
}

/**
 * Table model class for watched expressions.
 */
class MyTableModel extends AbstractTableModel {

    /**
     * Serializable magic number.
     */
    private static final long serialVersionUID = 2971618907207577000L;

    /**
     * The debugger GUI.
     */
    private SwingGui debugGui;

    /**
     * List of watched expressions.
     */
    private List<String> expressions;

    /**
     * List of values from evaluated from {@link #expressions}.
     */
    private List<String> values;

    /**
     * Creates a new MyTableModel.
     */
    public MyTableModel(SwingGui debugGui) {
        this.debugGui = debugGui;
        expressions = Collections.synchronizedList(new ArrayList<String>());
        values = Collections.synchronizedList(new ArrayList<String>());
        expressions.add("");
        values.add("");
    }

    /**
     * Returns the number of columns in the table (2).
     */
    public int getColumnCount() {
        return 2;
    }

    /**
     * Returns the number of rows in the table.
     */
    public int getRowCount() {
        return expressions.size();
    }

    /**
     * Returns the name of the given column.
     */
    @Override
    public String getColumnName(int column) {
        switch (column) {
        case 0:
            return "Expression";
        case 1:
            return "Value";
        }
        return null;
    }

    /**
     * Returns whether the given cell is editable.
     */
    @Override
    public boolean isCellEditable(int row, int column) {
        return true;
    }

    /**
     * Returns the value in the given cell.
     */
    public Object getValueAt(int row, int column) {
        switch (column) {
        case 0:
            return expressions.get(row);
        case 1:
            return values.get(row);
        }
        return "";
    }

    /**
     * Sets the value in the given cell.
     */
    @Override
    public void setValueAt(Object value, int row, int column) {
        switch (column) {
        case 0:
            String expr = value.toString();
            expressions.set(row, expr);
            String result = "";
            if (expr.length() > 0) {
                result = debugGui.dim.eval(expr);
                if (result == null) result = "";
            }
            values.set(row, result);
            updateModel();
            if (row + 1 == expressions.size()) {
                expressions.add("");
                values.add("");
                fireTableRowsInserted(row + 1, row + 1);
            }
            break;
        case 1:
            // just reset column 2; ignore edits
            fireTableDataChanged();
        }
    }

    /**
     * Re-evaluates the expressions in the table.
     */
    void updateModel() {
        for (int i = 0; i < expressions.size(); ++i) {
            String expr = expressions.get(i);
            String result = "";
            if (expr.length() > 0) {
                result = debugGui.dim.eval(expr);
                if (result == null) result = "";
            } else {
                result = "";
            }
            result = result.replace('\n', ' ');
            values.set(i, result);
        }
        fireTableDataChanged();
    }
}

/**
 * A table for evaluated expressions.
 */
class Evaluator extends JTable {

    /**
     * Serializable magic number.
     */
    private static final long serialVersionUID = 8133672432982594256L;

    /**
     * The {@link TableModel} for this table.
     */
    MyTableModel tableModel;

    /**
     * Creates a new Evaluator.
     */
    public Evaluator(SwingGui debugGui) {
        super(new MyTableModel(debugGui));
        tableModel = (MyTableModel)getModel();
    }
}

/**
 * Tree model for script object inspection.
 */
class VariableModel implements TreeTableModel {

    /**
     * Serializable magic number.
     */
    private static final String[] cNames = { " Name", " Value" };

    /**
     * Tree column types.
     */
    private static final Class<?>[] cTypes =
        { TreeTableModel.class, String.class };

    /**
     * Empty {@link VariableNode} array.
     */
    private static final VariableNode[] CHILDLESS = new VariableNode[0];

    /**
     * The debugger.
     */
    private Dim debugger;

    /**
     * The root node.
     */
    private VariableNode root;

    /**
     * Creates a new VariableModel.
     */
    public VariableModel() {
    }

    /**
     * Creates a new VariableModel.
     */
    public VariableModel(Dim debugger, Object scope) {
        this.debugger = debugger;
        this.root = new VariableNode(scope, "this");
    }

    // TreeTableModel

    /**
     * Returns the root node of the tree.
     */
    public Object getRoot() {
        if (debugger == null) {
            return null;
        }
        return root;
    }

    /**
     * Returns the number of children of the given node.
     */
    public int getChildCount(Object nodeObj) {
        if (debugger == null) {
            return 0;
        }
        VariableNode node = (VariableNode) nodeObj;
        return children(node).length;
    }

    /**
     * Returns a child of the given node.
     */
    public Object getChild(Object nodeObj, int i) {
        if (debugger == null) {
            return null;
        }
        VariableNode node = (VariableNode) nodeObj;
        return children(node)[i];
    }

    /**
     * Returns whether the given node is a leaf node.
     */
    public boolean isLeaf(Object nodeObj) {
        if (debugger == null) {
            return true;
        }
        VariableNode node = (VariableNode) nodeObj;
        return children(node).length == 0;
    }

    /**
     * Returns the index of a node under its parent.
     */
    public int getIndexOfChild(Object parentObj, Object childObj) {
        if (debugger == null) {
            return -1;
        }
        VariableNode parent = (VariableNode) parentObj;
        VariableNode child = (VariableNode) childObj;
        VariableNode[] children = children(parent);
        for (int i = 0; i != children.length; i++) {
            if (children[i] == child) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns whether the given cell is editable.
     */
    public boolean isCellEditable(Object node, int column) {
        return column == 0;
    }

    /**
     * Sets the value at the given cell.
     */
    public void setValueAt(Object value, Object node, int column) { }

    /**
     * Adds a TreeModelListener to this tree.
     */
    public void addTreeModelListener(TreeModelListener l) { }

    /**
     * Removes a TreeModelListener from this tree.
     */
    public void removeTreeModelListener(TreeModelListener l) { }

    public void valueForPathChanged(TreePath path, Object newValue) { }

    // TreeTableNode

    /**
     * Returns the number of columns.
     */
    public int getColumnCount() {
        return cNames.length;
    }

    /**
     * Returns the name of the given column.
     */
    public String getColumnName(int column) {
        return cNames[column];
    }

    /**
     * Returns the type of value stored in the given column.
     */
    public Class<?> getColumnClass(int column) {
        return cTypes[column];
    }

    /**
     * Returns the value at the given cell.
     */
    public Object getValueAt(Object nodeObj, int column) {
        if (debugger == null) { return null; }
        VariableNode node = (VariableNode)nodeObj;
        switch (column) {
        case 0: // Name
            return node.toString();
        case 1: // Value
            String result;
            try {
                result = debugger.objectToString(getValue(node));
            } catch (RuntimeException exc) {
                result = exc.getMessage();
            }
            StringBuffer buf = new StringBuffer();
            int len = result.length();
            for (int i = 0; i < len; i++) {
                char ch = result.charAt(i);
                if (Character.isISOControl(ch)) {
                    ch = ' ';
                }
                buf.append(ch);
            }
            return buf.toString();
        }
        return null;
    }

    /**
     * Returns an array of the children of the given node.
     */
    private VariableNode[] children(VariableNode node) {
        if (node.children != null) {
            return node.children;
        }

        VariableNode[] children;

        Object value = getValue(node);
        Object[] ids = debugger.getObjectIds(value);
        if (ids == null || ids.length == 0) {
            children = CHILDLESS;
        } else {
            Arrays.sort(ids, new Comparator<Object>() {
                    public int compare(Object l, Object r)
                    {
                        if (l instanceof String) {
                            if (r instanceof Integer) {
                                return -1;
                            }
                            return ((String)l).compareToIgnoreCase((String)r);
                        } else {
                            if (r instanceof String) {
                                return 1;
                            }
                            int lint = ((Integer)l).intValue();
                            int rint = ((Integer)r).intValue();
                            return lint - rint;
                        }
                    }
            });
            children = new VariableNode[ids.length];
            for (int i = 0; i != ids.length; ++i) {
                children[i] = new VariableNode(value, ids[i]);
            }
        }
        node.children = children;
        return children;
    }

    /**
     * Returns the value of the given node.
     */
    public Object getValue(VariableNode node) {
        try {
            return debugger.getObjectProperty(node.object, node.id);
        } catch (Exception exc) {
            return "undefined";
        }
    }

    /**
     * A variable node in the tree.
     */
    private static class VariableNode {

        /**
         * The script object.
         */
        private Object object;

        /**
         * The object name.  Either a String or an Integer.
         */
        private Object id;

        /**
         * Array of child nodes.  This is filled with the properties of
         * the object.
         */
        private VariableNode[] children;

        /**
         * Creates a new VariableNode.
         */
        public VariableNode(Object object, Object id) {
            this.object = object;
            this.id = id;
        }

        /**
         * Returns a string representation of this node.
         */
        @Override
        public String toString() {
            return id instanceof String
                ? (String) id : "[" + ((Integer) id).intValue() + "]";
        }
    }
}

/**
 * A tree table for browsing script objects.
 */
class MyTreeTable extends JTreeTable {

    /**
     * Serializable magic number.
     */
    private static final long serialVersionUID = 3457265548184453049L;

    /**
     * Creates a new MyTreeTable.
     */
    public MyTreeTable(VariableModel model) {
        super(model);
    }

    /**
     * Initializes a tree for this tree table.
     */
    public JTree resetTree(TreeTableModel treeTableModel) {
        tree = new TreeTableCellRenderer(treeTableModel);

        // Install a tableModel representing the visible rows in the tree.
        super.setModel(new TreeTableModelAdapter(treeTableModel, tree));

        // Force the JTable and JTree to share their row selection models.
        ListToTreeSelectionModelWrapper selectionWrapper = new
            ListToTreeSelectionModelWrapper();
        tree.setSelectionModel(selectionWrapper);
        setSelectionModel(selectionWrapper.getListSelectionModel());

        // Make the tree and table row heights the same.
        if (tree.getRowHeight() < 1) {
            // Metal looks better like this.
            setRowHeight(18);
        }

        // Install the tree editor renderer and editor.
        setDefaultRenderer(TreeTableModel.class, tree);
        setDefaultEditor(TreeTableModel.class, new TreeTableCellEditor());
        setShowGrid(true);
        setIntercellSpacing(new Dimension(1,1));
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        DefaultTreeCellRenderer r = (DefaultTreeCellRenderer)tree.getCellRenderer();
        r.setOpenIcon(null);
        r.setClosedIcon(null);
        r.setLeafIcon(null);
        return tree;
    }

    /**
     * Returns whether the cell under the coordinates of the mouse
     * in the {@link EventObject} is editable.
     */
    public boolean isCellEditable(EventObject e) {
        if (e instanceof MouseEvent) {
            MouseEvent me = (MouseEvent)e;
            // If the modifiers are not 0 (or the left mouse button),
            // tree may try and toggle the selection, and table
            // will then try and toggle, resulting in the
            // selection remaining the same. To avoid this, we
            // only dispatch when the modifiers are 0 (or the left mouse
            // button).
            if (me.getModifiers() == 0 ||
                ((me.getModifiers() & (InputEvent.BUTTON1_MASK|1024)) != 0 &&
                 (me.getModifiers() &
                  (InputEvent.SHIFT_MASK |
                   InputEvent.CTRL_MASK |
                   InputEvent.ALT_MASK |
                   InputEvent.BUTTON2_MASK |
                   InputEvent.BUTTON3_MASK |
                   64   | //SHIFT_DOWN_MASK
                   128  | //CTRL_DOWN_MASK
                   512  | // ALT_DOWN_MASK
                   2048 | //BUTTON2_DOWN_MASK
                   4096   //BUTTON3_DOWN_MASK
                   )) == 0)) {
                int row = rowAtPoint(me.getPoint());
                for (int counter = getColumnCount() - 1; counter >= 0;
                     counter--) {
                    if (TreeTableModel.class == getColumnClass(counter)) {
                        MouseEvent newME = new MouseEvent
                            (MyTreeTable.this.tree, me.getID(),
                             me.getWhen(), me.getModifiers(),
                             me.getX() - getCellRect(row, counter, true).x,
                             me.getY(), me.getClickCount(),
                             me.isPopupTrigger());
                        MyTreeTable.this.tree.dispatchEvent(newME);
                        break;
                    }
                }
            }
            if (me.getClickCount() >= 3) {
                return true;
            }
            return false;
        }
        if (e == null) {
            return true;
        }
        return false;
    }
}

/**
 * Panel that shows information about the context.
 */
class ContextWindow extends JPanel implements ActionListener {

    /**
     * Serializable magic number.
     */
    private static final long serialVersionUID = 2306040975490228051L;

    /**
     * The debugger GUI.
     */
    private SwingGui debugGui;

    /**
     * The combo box that holds the stack frames.
     */
    JComboBox context;

    /**
     * Tool tips for the stack frames.
     */
    List<String> toolTips;

    /**
     * Tabbed pane for "this" and "locals".
     */
    private JTabbedPane tabs;

    /**
     * Tabbed pane for "watch" and "evaluate".
     */
    private JTabbedPane tabs2;

    /**
     * The table showing the "this" object.
     */
    private MyTreeTable thisTable;

    /**
     * The table showing the stack local variables.
     */
    private MyTreeTable localsTable;

    /**
     * The {@link #evaluator}'s table model.
     */
    private MyTableModel tableModel;

    /**
     * The script evaluator table.
     */
    private Evaluator evaluator;

    /**
     * The script evaluation text area.
     */
    private EvalTextArea cmdLine;

    /**
     * The split pane.
     */
    JSplitPane split;

    /**
     * Whether the ContextWindow is enabled.
     */
    private boolean enabled;

    /**
     * Creates a new ContextWindow.
     */
    public ContextWindow(final SwingGui debugGui) {
        this.debugGui = debugGui;
        enabled = false;
        JPanel left = new JPanel();
        JToolBar t1 = new JToolBar();
        t1.setName("Variables");
        t1.setLayout(new GridLayout());
        t1.add(left);
        JPanel p1 = new JPanel();
        p1.setLayout(new GridLayout());
        JPanel p2 = new JPanel();
        p2.setLayout(new GridLayout());
        p1.add(t1);
        JLabel label = new JLabel("Context:");
        context = new JComboBox();
        context.setLightWeightPopupEnabled(false);
        toolTips = Collections.synchronizedList(new java.util.ArrayList<String>());
        label.setBorder(context.getBorder());
        context.addActionListener(this);
        context.setActionCommand("ContextSwitch");
        GridBagLayout layout = new GridBagLayout();
        left.setLayout(layout);
        GridBagConstraints lc = new GridBagConstraints();
        lc.insets.left = 5;
        lc.anchor = GridBagConstraints.WEST;
        lc.ipadx = 5;
        layout.setConstraints(label, lc);
        left.add(label);
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        layout.setConstraints(context, c);
        left.add(context);
        tabs = new JTabbedPane(SwingConstants.BOTTOM);
        tabs.setPreferredSize(new Dimension(500,300));
        thisTable = new MyTreeTable(new VariableModel());
        JScrollPane jsp = new JScrollPane(thisTable);
        jsp.getViewport().setViewSize(new Dimension(5,2));
        tabs.add("this", jsp);
        localsTable = new MyTreeTable(new VariableModel());
        localsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        localsTable.setPreferredSize(null);
        jsp = new JScrollPane(localsTable);
        tabs.add("Locals", jsp);
        c.weightx  = c.weighty = 1;
        c.gridheight = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.WEST;
        layout.setConstraints(tabs, c);
        left.add(tabs);
        evaluator = new Evaluator(debugGui);
        cmdLine = new EvalTextArea(debugGui);
        //cmdLine.requestFocus();
        tableModel = evaluator.tableModel;
        jsp = new JScrollPane(evaluator);
        JToolBar t2 = new JToolBar();
        t2.setName("Evaluate");
        tabs2 = new JTabbedPane(SwingConstants.BOTTOM);
        tabs2.add("Watch", jsp);
        tabs2.add("Evaluate", new JScrollPane(cmdLine));
        tabs2.setPreferredSize(new Dimension(500,300));
        t2.setLayout(new GridLayout());
        t2.add(tabs2);
        p2.add(t2);
        evaluator.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                               p1, p2);
        split.setOneTouchExpandable(true);
        SwingGui.setResizeWeight(split, 0.5);
        setLayout(new BorderLayout());
        add(split, BorderLayout.CENTER);

        final JToolBar finalT1 = t1;
        final JToolBar finalT2 = t2;
        final JPanel finalP1 = p1;
        final JPanel finalP2 = p2;
        final JSplitPane finalSplit = split;
        final JPanel finalThis = this;

        ComponentListener clistener = new ComponentListener() {
                boolean t2Docked = true;
                void check(Component comp) {
                    Component thisParent = finalThis.getParent();
                    if (thisParent == null) {
                        return;
                    }
                    Component parent = finalT1.getParent();
                    boolean leftDocked = true;
                    boolean rightDocked = true;
                    boolean adjustVerticalSplit = false;
                    if (parent != null) {
                        if (parent != finalP1) {
                            while (!(parent instanceof JFrame)) {
                                parent = parent.getParent();
                            }
                            JFrame frame = (JFrame)parent;
                            debugGui.addTopLevel("Variables", frame);

                            // We need the following hacks because:
                            // - We want an undocked toolbar to be
                            //   resizable.
                            // - We are using JToolbar as a container of a
                            //   JComboBox. Without this JComboBox's popup
                            //   can get left floating when the toolbar is
                            //   re-docked.
                            //
                            // We make the frame resizable and then
                            // remove JToolbar's window listener
                            // and insert one of our own that first ensures
                            // the JComboBox's popup window is closed
                            // and then calls JToolbar's window listener.
                            if (!frame.isResizable()) {
                                frame.setResizable(true);
                                frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                                final EventListener[] l =
                                    frame.getListeners(WindowListener.class);
                                frame.removeWindowListener((WindowListener)l[0]);
                                frame.addWindowListener(new WindowAdapter() {
                                        @Override
                                        public void windowClosing(WindowEvent e) {
                                            context.hidePopup();
                                            ((WindowListener)l[0]).windowClosing(e);
                                        }
                                    });
                                //adjustVerticalSplit = true;
                            }
                            leftDocked = false;
                        } else {
                            leftDocked = true;
                        }
                    }
                    parent = finalT2.getParent();
                    if (parent != null) {
                        if (parent != finalP2) {
                            while (!(parent instanceof JFrame)) {
                                parent = parent.getParent();
                            }
                            JFrame frame = (JFrame)parent;
                            debugGui.addTopLevel("Evaluate", frame);
                            frame.setResizable(true);
                            rightDocked = false;
                        } else {
                            rightDocked = true;
                        }
                    }
                    if (leftDocked && t2Docked && rightDocked && t2Docked) {
                        // no change
                        return;
                    }
                    t2Docked = rightDocked;
                    JSplitPane split = (JSplitPane)thisParent;
                    if (leftDocked) {
                        if (rightDocked) {
                            finalSplit.setDividerLocation(0.5);
                        } else {
                            finalSplit.setDividerLocation(1.0);
                        }
                        if (adjustVerticalSplit) {
                            split.setDividerLocation(0.66);
                        }

                    } else if (rightDocked) {
                            finalSplit.setDividerLocation(0.0);
                            split.setDividerLocation(0.66);
                    } else {
                        // both undocked
                        split.setDividerLocation(1.0);
                    }
                }
                public void componentHidden(ComponentEvent e) {
                    check(e.getComponent());
                }
                public void componentMoved(ComponentEvent e) {
                    check(e.getComponent());
                }
                public void componentResized(ComponentEvent e) {
                    check(e.getComponent());
                }
                public void componentShown(ComponentEvent e) {
                    check(e.getComponent());
                }
            };
        p1.addContainerListener(new ContainerListener() {
            public void componentAdded(ContainerEvent e) {
                Component thisParent = finalThis.getParent();
                JSplitPane split = (JSplitPane)thisParent;
                if (e.getChild() == finalT1) {
                    if (finalT2.getParent() == finalP2) {
                        // both docked
                        finalSplit.setDividerLocation(0.5);
                    } else {
                        // left docked only
                        finalSplit.setDividerLocation(1.0);
                    }
                    split.setDividerLocation(0.66);
                }
            }
            public void componentRemoved(ContainerEvent e) {
                Component thisParent = finalThis.getParent();
                JSplitPane split = (JSplitPane)thisParent;
                if (e.getChild() == finalT1) {
                    if (finalT2.getParent() == finalP2) {
                        // right docked only
                        finalSplit.setDividerLocation(0.0);
                        split.setDividerLocation(0.66);
                    } else {
                        // both undocked
                        split.setDividerLocation(1.0);
                    }
                }
            }
            });
        t1.addComponentListener(clistener);
        t2.addComponentListener(clistener);
        setEnabled(false);
    }

    /**
     * Enables or disables the component.
     */
    @Override
    public void setEnabled(boolean enabled) {
        context.setEnabled(enabled);
        thisTable.setEnabled(enabled);
        localsTable.setEnabled(enabled);
        evaluator.setEnabled(enabled);
        cmdLine.setEnabled(enabled);
    }

    /**
     * Disables updating of the component.
     */
    public void disableUpdate() {
        enabled = false;
    }

    /**
     * Enables updating of the component.
     */
    public void enableUpdate() {
        enabled = true;
    }

    // ActionListener

    /**
     * Performs an action.
     */
    public void actionPerformed(ActionEvent e) {
        if (!enabled) return;
        if (e.getActionCommand().equals("ContextSwitch")) {
            Dim.ContextData contextData = debugGui.dim.currentContextData();
            if (contextData == null) { return; }
            int frameIndex = context.getSelectedIndex();
            context.setToolTipText(toolTips.get(frameIndex));
            int frameCount = contextData.frameCount();
            if (frameIndex >= frameCount) {
                return;
            }
            Dim.StackFrame frame = contextData.getFrame(frameIndex);
            Object scope = frame.scope();
            Object thisObj = frame.thisObj();
            thisTable.resetTree(new VariableModel(debugGui.dim, thisObj));
            VariableModel scopeModel;
            if (scope != thisObj) {
                scopeModel = new VariableModel(debugGui.dim, scope);
            } else {
                scopeModel = new VariableModel();
            }
            localsTable.resetTree(scopeModel);
            debugGui.dim.contextSwitch(frameIndex);
            debugGui.showStopLine(frame);
            tableModel.updateModel();
        }
    }
}

/**
 * The debugger frame menu bar.
 */
class Menubar extends JMenuBar implements ActionListener {

    /**
     * Serializable magic number.
     */
    private static final long serialVersionUID = 3217170497245911461L;

    /**
     * Items that are enabled only when interrupted.
     */
    private List<JMenuItem> interruptOnlyItems =
        Collections.synchronizedList(new ArrayList<JMenuItem>());

    /**
     * Items that are enabled only when running.
     */
    private List<JMenuItem> runOnlyItems =
        Collections.synchronizedList(new ArrayList<JMenuItem>());

    /**
     * The debugger GUI.
     */
    private SwingGui debugGui;

    /**
     * The menu listing the internal frames.
     */
    private JMenu windowMenu;

    /**
     * The "Break on exceptions" menu item.
     */
    private JCheckBoxMenuItem breakOnExceptions;

    /**
     * The "Break on enter" menu item.
     */
    private JCheckBoxMenuItem breakOnEnter;

    /**
     * The "Break on return" menu item.
     */
    private JCheckBoxMenuItem breakOnReturn;

    /**
     * Creates a new Menubar.
     */
    Menubar(SwingGui debugGui) {
        super();
        this.debugGui = debugGui;
        String[] fileItems  = {"Open...", "Run...", "", "Exit"};
        String[] fileCmds  = {"Open", "Load", "", "Exit"};
        char[] fileShortCuts = {'0', 'N', 0, 'X'};
        int[] fileAccelerators = {KeyEvent.VK_O,
                                  KeyEvent.VK_N,
                                  0,
                                  KeyEvent.VK_Q};
        String[] editItems = {"Cut", "Copy", "Paste", "Go to function..."};
        char[] editShortCuts = {'T', 'C', 'P', 'F'};
        String[] debugItems = {"Break", "Go", "Step Into", "Step Over", "Step Out"};
        char[] debugShortCuts = {'B', 'G', 'I', 'O', 'T'};
        String[] plafItems = {"Metal", "Windows", "Motif"};
        char [] plafShortCuts = {'M', 'W', 'F'};
        int[] debugAccelerators = {KeyEvent.VK_PAUSE,
                                   KeyEvent.VK_F5,
                                   KeyEvent.VK_F11,
                                   KeyEvent.VK_F7,
                                   KeyEvent.VK_F8,
                                   0, 0};

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');
        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic('E');
        JMenu plafMenu = new JMenu("Platform");
        plafMenu.setMnemonic('P');
        JMenu debugMenu = new JMenu("Debug");
        debugMenu.setMnemonic('D');
        windowMenu = new JMenu("Window");
        windowMenu.setMnemonic('W');
        for (int i = 0; i < fileItems.length; ++i) {
            if (fileItems[i].length() == 0) {
                fileMenu.addSeparator();
            } else {
                JMenuItem item = new JMenuItem(fileItems[i],
                                               fileShortCuts[i]);
                item.setActionCommand(fileCmds[i]);
                item.addActionListener(this);
                fileMenu.add(item);
                if (fileAccelerators[i] != 0) {
                    KeyStroke k = KeyStroke.getKeyStroke(fileAccelerators[i], Event.CTRL_MASK);
                    item.setAccelerator(k);
                }
            }
        }
        for (int i = 0; i < editItems.length; ++i) {
            JMenuItem item = new JMenuItem(editItems[i],
                                           editShortCuts[i]);
            item.addActionListener(this);
            editMenu.add(item);
        }
        for (int i = 0; i < plafItems.length; ++i) {
            JMenuItem item = new JMenuItem(plafItems[i],
                                           plafShortCuts[i]);
            item.addActionListener(this);
            plafMenu.add(item);
        }
        for (int i = 0; i < debugItems.length; ++i) {
            JMenuItem item = new JMenuItem(debugItems[i],
                                           debugShortCuts[i]);
            item.addActionListener(this);
            if (debugAccelerators[i] != 0) {
                KeyStroke k = KeyStroke.getKeyStroke(debugAccelerators[i], 0);
                item.setAccelerator(k);
            }
            if (i != 0) {
                interruptOnlyItems.add(item);
            } else {
                runOnlyItems.add(item);
            }
            debugMenu.add(item);
        }
        breakOnExceptions = new JCheckBoxMenuItem("Break on Exceptions");
        breakOnExceptions.setMnemonic('X');
        breakOnExceptions.addActionListener(this);
        breakOnExceptions.setSelected(false);
        debugMenu.add(breakOnExceptions);

        breakOnEnter = new JCheckBoxMenuItem("Break on Function Enter");
        breakOnEnter.setMnemonic('E');
        breakOnEnter.addActionListener(this);
        breakOnEnter.setSelected(false);
        debugMenu.add(breakOnEnter);

        breakOnReturn = new JCheckBoxMenuItem("Break on Function Return");
        breakOnReturn.setMnemonic('R');
        breakOnReturn.addActionListener(this);
        breakOnReturn.setSelected(false);
        debugMenu.add(breakOnReturn);

        add(fileMenu);
        add(editMenu);
        //add(plafMenu);
        add(debugMenu);
        JMenuItem item;
        windowMenu.add(item = new JMenuItem("Cascade", 'A'));
        item.addActionListener(this);
        windowMenu.add(item = new JMenuItem("Tile", 'T'));
        item.addActionListener(this);
        windowMenu.addSeparator();
        windowMenu.add(item = new JMenuItem("Console", 'C'));
        item.addActionListener(this);
        add(windowMenu);

        updateEnabled(false);
    }

    /**
     * Returns the "Break on exceptions" menu item.
     */
    public JCheckBoxMenuItem getBreakOnExceptions() {
        return breakOnExceptions;
    }

    /**
     * Returns the "Break on enter" menu item.
     */
    public JCheckBoxMenuItem getBreakOnEnter() {
        return breakOnEnter;
    }

    /**
     * Returns the "Break on return" menu item.
     */
    public JCheckBoxMenuItem getBreakOnReturn() {
        return breakOnReturn;
    }

    /**
     * Returns the "Debug" menu.
     */
    public JMenu getDebugMenu() {
        return getMenu(2);
    }

    // ActionListener

    /**
     * Performs an action.
     */
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        String plaf_name = null;
        if (cmd.equals("Metal")) {
            plaf_name = "javax.swing.plaf.metal.MetalLookAndFeel";
        } else if (cmd.equals("Windows")) {
            plaf_name = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
        } else if (cmd.equals("Motif")) {
            plaf_name = "com.sun.java.swing.plaf.motif.MotifLookAndFeel";
        } else {
            Object source = e.getSource();
            if (source == breakOnExceptions) {
                debugGui.dim.setBreakOnExceptions(breakOnExceptions.isSelected());
            } else if (source == breakOnEnter) {
                debugGui.dim.setBreakOnEnter(breakOnEnter.isSelected());
            } else if (source == breakOnReturn) {
                debugGui.dim.setBreakOnReturn(breakOnReturn.isSelected());
            } else {
                debugGui.actionPerformed(e);
            }
            return;
        }
        try {
            UIManager.setLookAndFeel(plaf_name);
            SwingUtilities.updateComponentTreeUI(debugGui);
            SwingUtilities.updateComponentTreeUI(debugGui.dlg);
        } catch (Exception ignored) {
            //ignored.printStackTrace();
        }
    }

    /**
     * Adds a file to the window menu.
     */
    public void addFile(String url) {
        int count = windowMenu.getItemCount();
        JMenuItem item;
        if (count == 4) {
            windowMenu.addSeparator();
            count++;
        }
        JMenuItem lastItem = windowMenu.getItem(count -1);
        boolean hasMoreWin = false;
        int maxWin = 5;
        if (lastItem != null &&
           lastItem.getText().equals("More Windows...")) {
            hasMoreWin = true;
            maxWin++;
        }
        if (!hasMoreWin && count - 4 == 5) {
            windowMenu.add(item = new JMenuItem("More Windows...", 'M'));
            item.setActionCommand("More Windows...");
            item.addActionListener(this);
            return;
        } else if (count - 4 <= maxWin) {
            if (hasMoreWin) {
                count--;
                windowMenu.remove(lastItem);
            }
            String shortName = SwingGui.getShortName(url);

            windowMenu.add(item = new JMenuItem((char)('0' + (count-4)) + " " + shortName, '0' + (count - 4)));
            if (hasMoreWin) {
                windowMenu.add(lastItem);
            }
        } else {
            return;
        }
        item.setActionCommand(url);
        item.addActionListener(this);
    }

    /**
     * Updates the enabledness of menu items.
     */
    public void updateEnabled(boolean interrupted) {
        for (int i = 0; i != interruptOnlyItems.size(); ++i) {
            JMenuItem item = interruptOnlyItems.get(i);
            item.setEnabled(interrupted);
        }

        for (int i = 0; i != runOnlyItems.size(); ++i) {
            JMenuItem item = runOnlyItems.get(i);
            item.setEnabled(!interrupted);
        }
    }
}

/**
 * Class to consolidate all cases that require to implement Runnable
 * to avoid class generation bloat.
 */
class RunProxy implements Runnable {

    // Constants for 'type'.
    static final int OPEN_FILE = 1;
    static final int LOAD_FILE = 2;
    static final int UPDATE_SOURCE_TEXT = 3;
    static final int ENTER_INTERRUPT = 4;

    /**
     * The debugger GUI.
     */
    private SwingGui debugGui;

    /**
     * The type of Runnable this object is.  Takes one of the constants
     * defined in this class.
     */
    private int type;

    /**
     * The name of the file to open or load.
     */
    String fileName;

    /**
     * The source text to update.
     */
    String text;

    /**
     * The source for which to update the text.
     */
    Dim.SourceInfo sourceInfo;

    /**
     * The frame to interrupt in.
     */
    Dim.StackFrame lastFrame;

    /**
     * The name of the interrupted thread.
     */
    String threadTitle;

    /**
     * The message of the exception thrown that caused the thread
     * interruption, if any.
     */
    String alertMessage;

    /**
     * Creates a new RunProxy.
     */
    public RunProxy(SwingGui debugGui, int type) {
        this.debugGui = debugGui;
        this.type = type;
    }

    /**
     * Runs this Runnable.
     */
    public void run() {
        switch (type) {
          case OPEN_FILE:
            try {
                debugGui.dim.compileScript(fileName, text);
            } catch (RuntimeException ex) {
                MessageDialogWrapper.showMessageDialog(
                    debugGui, ex.getMessage(), "Error Compiling "+fileName,
                    JOptionPane.ERROR_MESSAGE);
            }
            break;

          case LOAD_FILE:
            try {
                debugGui.dim.evalScript(fileName, text);
            } catch (RuntimeException ex) {
                MessageDialogWrapper.showMessageDialog(
                    debugGui, ex.getMessage(), "Run error for "+fileName,
                    JOptionPane.ERROR_MESSAGE);
            }
            break;

          case UPDATE_SOURCE_TEXT:
            {
                String fileName = sourceInfo.url();
                if (!debugGui.updateFileWindow(sourceInfo) &&
                        !fileName.equals("<stdin>")) {
                    debugGui.createFileWindow(sourceInfo, -1);
                }
            }
            break;

          case ENTER_INTERRUPT:
            debugGui.enterInterruptImpl(lastFrame, threadTitle, alertMessage);
            break;

          default:
            throw new IllegalArgumentException(String.valueOf(type));

        }
    }
}
