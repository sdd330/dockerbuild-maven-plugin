package com.github.sdd330;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * TestMojo
 */
@Mojo(name = "install", defaultPhase = LifecyclePhase.INSTALL)
public class InstallMojo extends AbstractDockerBuildMojo {

    @Override
    protected void internalExecute() throws MojoExecutionException, MojoFailureException {

        try {

            File outputDirectory = new File(project.getBasedir().getAbsolutePath() + File.separator + "target");
            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs();
            }

            prepareSource(outputDirectory);

            executeCommand(outputDirectory, "docker-compose", new String[] { "build" });
            executeCommand(outputDirectory, "docker-compose", new String[] { "up" });

        } catch (Exception e) {
            throw new MojoExecutionException("Command execution failed.", e);
        }

    }
}
