package com.github.sdd330;

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
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * AbstractDockerBuildMojo
 */
abstract public class AbstractDockerBuildMojo extends AbstractMojo {

    /**
     * <code>DOCKER_COMPOSE_FILE</code>
     */
    protected static final String DOCKER_COMPOSE_FILE = "docker-compose.yml";

    /**
     * The Maven project.
     */
    @Parameter(property = "project", required = true, readonly = true)
    protected MavenProject project;

    @Parameter(property = "skip")
    private boolean skip;

    /**
     * The containers configuration.
     */
    @Parameter(property = "containers")
    protected List<Container> containers;

    /**
     * The local mount directory.
     */
    @Parameter(property = "mountDirectory")
    protected String mountDirectory;

    /**
     * The session object.
     */
    @Component(role = MavenSession.class)
    protected MavenSession session;

    /**
     * The execution object.
     */
    @Component(role = MojoExecution.class)
    protected MojoExecution execution;

    /**
     * The system settings for Maven. This is the instance resulting
     * from merging global and user-level settings files.
     */
    @Component
    protected Settings settings;

    /**
     * Write file
     * 
     * @param path
     * @param content
     * @throws IOException
     */
    protected void writeFile(String path, String content) throws IOException {

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

    /**
     * Check if result code is failure
     * 
     * @param result
     * @return true if result code is failure, false if result code is
     *         not failure
     */
    protected boolean isResultCodeAFailure(int result) {
        return result != 0;
    }

    /**
     * @param containerSource
     * @throws Exception
     */
    protected void removePluginInPomInnerContainers(String containerSource) throws Exception {
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
    }

    /**
     * Prepare Docker compose file
     * 
     * @param outputDirectory
     * @throws Exception
     */
    protected void prepareSource(File outputDirectory) throws Exception {

        StringBuffer sb = new StringBuffer();
        for (Container container : containers) {
            String containerSource = outputDirectory.getAbsolutePath() + File.separator + container.getName();

            String containerTarget = containerSource + File.separator + "target";
            String mountTarget = mountDirectory + File.separator + "target" + File.separator + container.getName()
                    + File.separator + "target";
            File f = new File(containerTarget);
            if (!f.exists()) {
                f.mkdirs();
            }

            getLog().info("Preparing source files for container:" + container.getName());
            List<SourceFile> files = container.getSourceFiles(project);

            for (SourceFile file : files) {
                String targetFileName = containerSource + File.separator + "src" + File.separator + "main"
                        + File.separator + "java" + File.separator + file.getSource();
                String targetFileDirName = targetFileName.substring(0, targetFileName.lastIndexOf(File.separator));
                File targetFileDir = new File(targetFileDirName);
                if (!targetFileDir.exists()) {
                    targetFileDir.mkdirs();
                }
                File targetFile = new File(targetFileName);
            	getLog().info("Copying source file "+ file.getFile() +" for container:" + container.getName());
                FileUtil.copy(file.getFile(), targetFile, true);
            }

            getLog().info("Preparing test source files for container:" + container.getName());
            List<SourceFile> testfiles = container.getTestSourceFiles(project);

            for (SourceFile testfile : testfiles) {
                String targetFileName = containerSource + File.separator + "src" + File.separator + "test"
                        + File.separator + "java" + File.separator + testfile.getSource();
                String targetFileDirName = targetFileName.substring(0, targetFileName.lastIndexOf(File.separator));
                File targetFileDir = new File(targetFileDirName);
                if (!targetFileDir.exists()) {
                    targetFileDir.mkdirs();
                }
                File targetFile = new File(targetFileName);
                getLog().info("Copying test source file "+ testfile.getFile() +" for container:" + container.getName());
                FileUtil.copy(testfile.getFile(), targetFile, true);
            }

            getLog().info("Preparing Dockerfile for container:" + container.getName());
            String containerDockerfilePath = containerSource + File.separator + "Dockerfile";
            writeFile(containerDockerfilePath, container.getDockerFileContent(project));

            String mvnSettingFilePath = project.getBasedir() + File.separator + "settings.xml";
            File mvnSettingFile = new File(mvnSettingFilePath);
            if (!mvnSettingFile.exists()) {
                getLog().error("Maven settings.xml does not exits in project root");
            }

            String containerMvnSettingFilePath = containerSource + File.separator + "settings.xml";
            File containerMvnSettingFile = new File(containerMvnSettingFilePath);
            FileUtil.copy(mvnSettingFile, containerMvnSettingFile, true);

            removePluginInPomInnerContainers(containerSource);

            String cmd = "mvn " + execution.getLifecyclePhase();
            sb.append(container.getDockerComposeContent(project, outputDirectory, mountTarget, cmd));
            sb.append("\n");
        }

        getLog().info("Create docker-compose.yml");
        String dockerComposeFilePath = outputDirectory.getAbsolutePath() + File.separator + DOCKER_COMPOSE_FILE;
        writeFile(dockerComposeFilePath, sb.toString());
    }

    /**
     * Execute a command
     * 
     * @param workingDirectory
     * @param cmd
     * @param args
     * @throws Exception
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void executeCommand(File workingDirectory, String cmd, String[] args) throws Exception {
        CommandLine commandLine = new CommandLine(cmd);
        Executor exec = new DefaultExecutor();
        exec.setWorkingDirectory(workingDirectory);
        OutputStream stdout = System.out;
        OutputStream stderr = System.err;
        Map enviro = new HashMap();
        Properties systemEnvVars = CommandLineUtils.getSystemEnvVars();
        enviro.putAll(systemEnvVars);
        commandLine.addArguments(args, false);
        getLog().info("Executing command line: " + commandLine);
        int resultCode = executeCommandLine(exec, commandLine, enviro, stdout, stderr);
        if (isResultCodeAFailure(resultCode)) {
            throw new MojoExecutionException("Result of " + commandLine + " execution is: '" + resultCode + "'.");
        }
    }

    /**
     * Execute command line
     * 
     * @param exec
     * @param commandLine
     * @param enviro
     * @param out
     * @param err
     * @return Result code
     * @throws ExecuteException
     * @throws IOException
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected int executeCommandLine(Executor exec, CommandLine commandLine, Map enviro, OutputStream out,
            OutputStream err) throws ExecuteException, IOException {
        exec.setStreamHandler(new PumpStreamHandler(out, err, System.in));
        return exec.execute(commandLine, enviro);
    }

    /**
     * Abstract execute to be implemented by concrete mojos.
     * 
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    protected abstract void internalExecute() throws MojoExecutionException, MojoFailureException;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        if (skip) {
            getLog().info("Skipping execution");
            return;
        }

        internalExecute();
    }
}
