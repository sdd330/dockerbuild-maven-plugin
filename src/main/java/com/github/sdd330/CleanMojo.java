package com.github.sdd330;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * CleanMojo
 */
@Mojo(name = "clean", defaultPhase = LifecyclePhase.PRE_CLEAN)
public class CleanMojo extends AbstractDockerBuildMojo {

    @Override
    protected void internalExecute() throws MojoExecutionException, MojoFailureException {
        try {
            File outputDirectory = new File(project.getBasedir().getAbsolutePath() + File.separator + "target");

            if (outputDirectory.exists()) {
                executeCommand(outputDirectory, "docker-compose", new String[] { "rm", "-f" });
            }

        } catch (Exception e) {
            throw new MojoExecutionException("Command execution failed.", e);
        }
    }
}