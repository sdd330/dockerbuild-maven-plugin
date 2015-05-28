package com.dockerbuild;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.spotify.docker.client.DockerClient;

/**
 * Maven_3_jdk_7:
 *   image: maven:3-jdk-7
 *   working_dir: /app
 *   command: mvn compile
 *   external_links:
 *     - artifactory
 * Maven_3_jdk_8:
 *   image: maven:3-jdk-8
 *   working_dir: /app
 *   command: mvn compile
 *   external_links:
 *     - artifactory
 */

/**
 * Used to build docker images.
 */
@Mojo(name = "build")
public class BuildMojo extends AbstractDockerMojo {

    private static final String DOCKER_COMPOSE_FILE = "docker-compose.yml";

    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject project;

    @Parameter(property = "artifactory")
    private Artifactory artifactory;

    @Parameter(property = "containers")
    private List<Container> containers;

    @Parameter(property = "outputDirectory")
    private File outputDirectory;

    @Component
    private MojoExecution execution;

    private void writeFile(String path, String content) throws IOException {
        File targetFile = new File(path);
        if (targetFile.exists()) {
            targetFile.delete();
        }

        String targetFileDirName = path.substring(0, path.lastIndexOf(File.separator));
        File targetFileDir = new File(targetFileDirName);
        if (!targetFileDir.exists()) {
            targetFileDir.mkdirs();
        }

        targetFile = new File(path);
        targetFile.createNewFile();

        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;
        try {
            fileWriter = new FileWriter(path, false);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bufferedWriter != null) {
                    bufferedWriter.flush();
                    bufferedWriter.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

    }

    private boolean isResultCodeAFailure(int result) {
        return result != 0;
    }

    private int executeCommandLine(Executor exec, CommandLine commandLine, Map enviro, OutputStream out,
            OutputStream err) throws ExecuteException, IOException {
        exec.setStreamHandler(new PumpStreamHandler(out, err, System.in));
        return exec.execute(commandLine, enviro);
    }

    @Override
    protected void execute(DockerClient dockerClient) throws Exception {

        if (true) {
            getLog().info("DockerBuild: preparing docker-compose.yml");

            File f = new File(outputDirectory.getAbsolutePath());
            if (!f.exists()) {
                f.mkdirs();
            }

            StringBuffer sb = new StringBuffer();
            for (Container container : containers) {
                String containerSource = outputDirectory.getAbsolutePath() + File.separator + container.getName();
                f = new File(containerSource);
                if (!f.exists()) {
                    f.mkdirs();
                }

                List<SourceFile> files = container.getSourceFiles(project);

                for (SourceFile file : files) {
                    String targetFileName = containerSource + File.separator + "src" + File.separator
                            + file.getSource();
                    String targetFileDirName = targetFileName.substring(0, targetFileName.lastIndexOf(File.separator));
                    File targetFileDir = new File(targetFileDirName);
                    if (!targetFileDir.exists()) {
                        targetFileDir.mkdirs();
                    }
                    File targetFile = new File(targetFileName);
                    FileUtil.copy(file.getFile(), targetFile, true);
                }

                String containerDockerfilePath = containerSource + File.separator + "Dockerfile";
                writeFile(containerDockerfilePath,
                        container.getDockerFileContent(project, execution.getLifecyclePhase()));

                String mvnSettingFilePath = project.getBasedir() + File.separator + "settings.xml";
                File mvnSettingFile = new File(mvnSettingFilePath);
                if (!mvnSettingFile.exists()) {
                    getLog().error("Maven settings.xml does not exits in project root");
                }

                String containerMvnSettingFilePath = containerSource + File.separator + "settings.xml";
                File containerMvnSettingFile = new File(containerMvnSettingFilePath);
                FileUtil.copy(mvnSettingFile, containerMvnSettingFile, true);

                // Remove dockerbuild-maven-plugin when build inner containers
                String pomFilePath = project.getBasedir() + File.separator + "pom.xml";
                File pomFile = new File(pomFilePath);

                String containerPomFilePath = containerSource + File.separator + "pom.xml";
                File containerPomFile = new File(containerPomFilePath);
                FileUtil.copy(pomFile, containerPomFile, true);

                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setIgnoringElementContentWhitespace(true);
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document xmldoc = db.parse(containerPomFilePath);
                NodeList pluginList = xmldoc.getElementsByTagName("plugin");
                for (int i = 0; i < pluginList.getLength(); i++) {
                    Element plugin = (Element) pluginList.item(i);
                    for (Node node = plugin.getFirstChild(); node != null; node = node.getNextSibling()) {
                        if (node.getNodeType() == Node.ELEMENT_NODE) {
                            if ((node.getTextContent() != null)
                                    && node.getTextContent().equalsIgnoreCase("dockerbuild-maven-plugin")) {
                                plugin.getParentNode().removeChild(plugin);
                                break;
                            }
                        }
                    }
                }

                TransformerFactory factory = TransformerFactory.newInstance();
                Transformer former = factory.newTransformer();
                former.transform(new DOMSource(xmldoc), new StreamResult(new File(containerPomFilePath)));

                sb.append(container.getDockerComposeContent(project, outputDirectory, artifactory));
                if (artifactory != null) {
                    sb.append(artifactory.getName());
                    sb.append(":");
                    sb.append("\n");
                    sb.append("    image: " + artifactory.getImage());
                    sb.append("\n");
                    sb.append("    ports:");
                    sb.append("\n");
                    sb.append("     - \"" + artifactory.getPort() + ":" + artifactory.getPort() + "\"");
                    sb.append("\n");
                }
                sb.append("\n");
            }

            String dockerComposeFilePath = outputDirectory.getAbsolutePath() + File.separator + DOCKER_COMPOSE_FILE;
            writeFile(dockerComposeFilePath, sb.toString());

            try {
                CommandLine commandLine = new CommandLine("docker-compose");
                Executor exec = new DefaultExecutor();
                exec.setWorkingDirectory(outputDirectory);
                OutputStream stdout = System.out;
                OutputStream stderr = System.err;
                Map enviro = new HashMap();
                Properties systemEnvVars = CommandLineUtils.getSystemEnvVars();
                enviro.putAll(systemEnvVars);
                String[] args = new String[] { "up" };
                commandLine.addArguments(args, false);
                getLog().info("Executing command line: " + commandLine);
                int resultCode = executeCommandLine(exec, commandLine, enviro, stdout, stderr);
                if (isResultCodeAFailure(resultCode)) {
                    throw new MojoExecutionException("Result of " + commandLine + " execution is: '" + resultCode
                            + "'.");
                }
                CommandLine commandLineRM = new CommandLine("docker-compose");
                String[] argsRM = new String[] { "rm", "-f" };
                commandLine.addArguments(argsRM, false);
                getLog().info("Executing command line: " + commandLineRM);
                int resultCodeRM = executeCommandLine(exec, commandLineRM, enviro, stdout, stderr);
                if (isResultCodeAFailure(resultCodeRM)) {
                    throw new MojoExecutionException("Result of " + commandLineRM + " execution is: '" + resultCodeRM
                            + "'.");
                }
            } catch (ExecuteException e) {
                throw new MojoExecutionException("Command execution failed.", e);
            } catch (IOException e) {
                throw new MojoExecutionException("Command execution failed.", e);
            }
        }
    }
}
