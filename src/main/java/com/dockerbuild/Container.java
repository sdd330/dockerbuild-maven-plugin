package com.dockerbuild;

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
     * getDockerFileContent
     * 
     * @return The content of Dockerfile
     */
    public String getDockerFileContent(MavenProject project, String phase) {
        StringBuffer sb = new StringBuffer();
        sb.append("FROM " + image);
        sb.append("\n");
        sb.append("ADD settings.xml /root/.m2/settings.xml");
        sb.append("\n");
        sb.append("WORKDIR " + CONTAINER_HOME);
        sb.append("\n");
        sb.append("Add . " + CONTAINER_HOME);
        sb.append("\n");
        sb.append("CMD [\"mvn\",\"" + phase + "\"]");
        sb.append("\n");
        return sb.toString();
    }

    /**
     * @param project
     * @param command
     * @return The content of docker compose
     */
    public String getDockerComposeContent(MavenProject project, File outputDirectory, Artifactory artifactory) {
        StringBuffer sb = new StringBuffer();
        sb.append(name);
        sb.append(":");
        sb.append("\n");
        sb.append("    build: " + name);
        sb.append("\n");
        if (artifactory != null) {
            sb.append("    links:");
            sb.append("\n");
            sb.append("     - " + artifactory.getName());
            sb.append("\n");
        }
        return sb.toString();
    }
}
