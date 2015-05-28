package com.dockerbuild;

import java.io.File;

/**
 * Represents a source file to be processed by jsmodule
 */
public class SourceFile {
    private String source;
    private File file;

    /**
     * Creates a new instance of <code>SourceFile</code>.
     * 
     * @param sourceDir
     * @param source
     */
    public SourceFile(File sourceDir, String source) {
        this.source = source;
        this.file = new File(sourceDir, source);
    }

    /**
     * Get the file path relative to the source directory
     * 
     * @return The relative file path
     */
    public String getSource() {
        return source;
    }

    /**
     * Get the absolute file path
     * 
     * @return The absolute file path
     */
    public File getFile() {
        return file;
    }
}
