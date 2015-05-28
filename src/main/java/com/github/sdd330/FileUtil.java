package com.github.sdd330;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Utility class for file related operations
 */
public class FileUtil {
    /**
     * Get the first part of a file path, which is the root directory
     * or the file name if the path contains no directory
     * 
     * @param path
     * @return The first part of a file path
     */
    public static String getFirstPart(File path) {
        String first = "";
        File tmp = path;
        while (tmp != null) {
            first = tmp.getName();
            tmp = tmp.getParentFile();
        }
        return first;
    }

    /**
     * Get everything but the first part of a file path
     * 
     * @param path
     * @return Everything but the first part of the file path
     */
    public static File getTail(File path) {
        if (path.getParentFile() == null) {
            return null;
        } else {
            String first = getFirstPart(path);
            return new File(path.getPath().substring(first.length() + 1));
        }
    }

    /**
     * Calculates the relative path of one path relative to another
     * 
     * @param path The target path
     * @param relativeTo The path that the target path should be
     *            relative to
     * @return A path that is pointing to the target path but is
     *         relative to the relativeTo path
     */
    public static File relativePath(File path, File relativeTo) {
        File tmpPath = path;
        File tmpRelativeTo = relativeTo;

        if (tmpRelativeTo != null) {
            String t1 = getFirstPart(tmpPath);
            String t2 = getFirstPart(tmpRelativeTo);
            while (t2.equals(t1)) {
                tmpPath = getTail(tmpPath);
                tmpRelativeTo = getTail(tmpRelativeTo);
                t1 = getFirstPart(tmpPath);
                t2 = getFirstPart(tmpRelativeTo);
            }
        }
        while (tmpRelativeTo != null) {
            tmpPath = new File("..", tmpPath.getPath());
            tmpRelativeTo = tmpRelativeTo.getParentFile();
        }

        return tmpPath;
    }

    private static File remove(File path, int n) throws IOException {
        if (path == null) {
            throw new IOException();
        } else if (n == 0) {
            return path;
        } else if (!"..".equals(path.getName())) {
            return remove(path.getParentFile(), n - 1);
        } else {
            return remove(path.getParentFile(), n + 1);
        }
    }

    /**
     * Make a file path canonical by removing and resolving all ".."
     * directory references
     * 
     * @param path
     * @return A canonical path
     * @throws IOException
     */
    public static File canonical(File path) throws IOException {
        if ("..".equals(path.getName())) {
            return canonical(remove(path.getParentFile(), 1));
        } else if (path.getParentFile() != null) {
            return new File(canonical(path.getParentFile()), path.getName());
        } else {
            return path;
        }
    }

    /**
     * Copy a file
     * 
     * @param in Input file
     * @param out Output file
     * @param overwrite If false, only overwrite if in is newer than
     *            out
     * @throws IOException
     */
    public static void copy(File in, File out, boolean overwrite) throws IOException {
        if (overwrite || !out.exists() || (out.lastModified() < in.lastModified())) {
            FileChannel inChannel = new FileInputStream(in).getChannel();
            FileChannel outChannel = new FileOutputStream(out).getChannel();
            try {
                inChannel.transferTo(0, inChannel.size(), outChannel);
            } finally {
                if (inChannel != null) {
                    inChannel.close();
                }
                if (outChannel != null) {
                    outChannel.close();
                }
            }
        }
    }

    /**
     * Touch a file, setting its last modified timestamp to current
     * time
     * 
     * @param file
     * @throws IOException
     */
    public static void touch(File file) throws IOException {
        if (file.exists()) {
            if (!file.setLastModified(System.currentTimeMillis())) {
                throw new IOException("Could not touch file " + file.getAbsolutePath());
            }
        } else {
            try {
                File directory = file.getParentFile();
                if (!directory.exists()) {
                    directory.mkdirs();
                }
                file.createNewFile();
            } catch (IOException e) {
                throw new IOException("Could not create file " + file.getAbsolutePath(), e);
            }
        }
    }
}
