package com.dockerbuild;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;

abstract class AbstractDockerMojo extends AbstractMojo {

    @Component(role = MavenSession.class)
    protected MavenSession session;

    @Component(role = MojoExecution.class)
    protected MojoExecution execution;

    public void execute() throws MojoExecutionException {
        DockerClient client = null;
        try {
            client = new DefaultDockerClient("unix:///var/run/docker.sock");
            execute(client);
        } catch (Exception e) {
            throw new MojoExecutionException("Exception caught", e);
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    protected abstract void execute(final DockerClient dockerClient) throws Exception;

}
