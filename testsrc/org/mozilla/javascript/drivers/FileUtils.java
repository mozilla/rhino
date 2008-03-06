package org.mozilla.javascript.drivers;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {

    public static File[] recursiveListFiles(File dir, FileFilter filter) {
        if (!dir.isDirectory())
            throw new IllegalArgumentException(dir + " is not a directory");
        List<File> fileList = new ArrayList<File>();
        recursiveListFilesHelper(dir, filter, fileList);
        return fileList.toArray(new File[fileList.size()]);
    }

    public static void recursiveListFilesHelper(File dir, FileFilter filter,
                                                List<File> fileList)
    {
        for (File f: dir.listFiles()) {
            if (f.isDirectory()) {
                recursiveListFilesHelper(f, filter, fileList);
            } else {
                if (filter.accept(f))
                    fileList.add(f);
            }
        }
    }
}
