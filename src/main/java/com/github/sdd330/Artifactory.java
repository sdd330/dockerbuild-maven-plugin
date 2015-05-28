package com.github.sdd330;

/**
 * Artifactory
 */
public class Artifactory {
    private String name;
    private String image;
    private Integer port;

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
     * @return Returns the port.
     */
    public Integer getPort() {
        return port;
    }

    /**
     * @param port The port to set.
     */
    public void setPort(Integer port) {
        this.port = port;
    }

}
