package com.github.sdd330;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * PackageMojo
 */
@Mojo(name = "package", defaultPhase = LifecyclePhase.PACKAGE)
public class PackageMojo extends AbstractDockerBuildMojo {
	
	@Override
    protected void internalExecute() throws MojoExecutionException, MojoFailureException {
		getLog().info( "Doing nothing here." );
	}
	
}
