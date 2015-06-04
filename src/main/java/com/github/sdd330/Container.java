package com.github.sdd330;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;

/**
 * Container
 */
public class Container {

    private String name;
    private String image;
    private String[] sourceIncludes = {};
    private String[] sourceExcludes = {};
    private String[] testSourceIncludes = {};
    private String[] testSourceExcludes = {};
    private static final String CONTAINER_HOME = "/workspace";

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * @param name The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Returns the image.
     */
    public String getImage() {
        return image;
    }

    /**
     * @param image The image to set.
     */
    public void setImage(String image) {
        this.image = image;
    }

    /**
     * @return Returns the sourceIncludes.
     */
    public String[] getSourceIncludes() {
        return sourceIncludes;
    }

    /**
     * @param sourceIncludes The sourceIncludes to set.
     */
    public void setSourceIncludes(String[] sourceIncludes) {
        this.sourceIncludes = sourceIncludes;
    }

    /**
     * @return Returns the sourceExcludes.
     */
    public String[] getSourceExcludes() {
        return sourceExcludes;
    }

    /**
     * @param sourceExcludes The sourceExcludes to set.
     */
    public void setSourceExcludes(String[] sourceExcludes) {
        this.sourceExcludes = sourceExcludes;
    }

    /**
     * @return Returns the testSourceIncludes.
     */
    public String[] getTestSourceIncludes() {
        return testSourceIncludes;
    }

    /**
     * @param testSourceIncludes The testSourceIncludes to set.
     */
    public void setTestSourceIncludes(String[] testSourceIncludes) {
        this.testSourceIncludes = testSourceIncludes;
    }

    /**
     * @return Returns the testSourceExcludes.
     */
    public String[] getTestSourceExcludes() {
        return testSourceExcludes;
    }

    /**
     * @param testSourceExcludes The testSourceExcludes to set.
     */
    public void setTestSourceExcludes(String[] testSourceExcludes) {
        this.testSourceExcludes = testSourceExcludes;
    }

    /**
     * getSourceFiles
     * 
     * @param project
     * @return A list of source file
     */
    public List<SourceFile> getSourceFiles(MavenProject project) {
        List<SourceFile> files = new ArrayList<SourceFile>();

        for (String sourceDir : project.getCompileSourceRoots()) {
            if (new File(sourceDir).exists()) {
                DirectoryScanner ds = new DirectoryScanner();
                ds.setBasedir(sourceDir);
                ds.setExcludes(sourceExcludes);
                ds.addDefaultExcludes();
                ds.setIncludes(sourceIncludes);
                ds.scan();
                for (String file : ds.getIncludedFiles()) {
                    files.add(new SourceFile(new File(sourceDir), file));
                }
            }
        }

        return files;
    }

    /**
     * getTestSourceFiles
     * 
     * @param project
     * @return A list of source file
     */
    public List<SourceFile> getTestSourceFiles(MavenProject project) {
        List<SourceFile> files = new ArrayList<SourceFile>();

        for (String sourceDir : project.getTestCompileSourceRoots()) {
            if (new File(sourceDir).exists()) {
                DirectoryScanner ds = new DirectoryScanner();
                ds.setBasedir(sourceDir);
                ds.setExcludes(testSourceExcludes);
                ds.addDefaultExcludes();
                ds.setIncludes(testSourceIncludes);
                ds.scan();
                for (String file : ds.getIncludedFiles()) {
                    files.add(new SourceFile(new File(sourceDir), file));
                }
            }
        }

        return files;
    }

    /**
     * getDockerFileContent
     * 
     * @return The content of Dockerfile
     */
    public String getDockerFileContent(MavenProject project) {
        StringBuffer sb = new StringBuffer();
        sb.append("FROM " + image);
        sb.append("\n");
        sb.append("ADD settings.xml /root/.m2/settings.xml");
        sb.append("\n");
        sb.append("WORKDIR " + CONTAINER_HOME);
        sb.append("\n");
        sb.append("Add . " + CONTAINER_HOME);
        sb.append("\n");
        sb.append("VOLUME " + CONTAINER_HOME + "/target");
        sb.append("\n");
        //sb.append("CMD [\"mvn\",\"" + phase + "\"]");
        //sb.append("\n");
        return sb.toString();
    }

    /**
     * @param project
     * @param command
     * @return The content of docker compose
     */
    public String getDockerComposeContent(MavenProject project, File outputDirectory, String targetDir, String cmd) {
        StringBuffer sb = new StringBuffer();
        sb.append(name);
        sb.append(":");
        sb.append("\n");
        sb.append("    build: " + name);
        sb.append("\n");
        sb.append("    command: " + cmd);
        sb.append("\n");
        sb.append("    volumes:");
        sb.append("\n");
        sb.append("     - " + targetDir + ":" + CONTAINER_HOME + "/target");
        sb.append("\n");
        return sb.toString();
    }

}
