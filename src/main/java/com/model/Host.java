package com.model;

public class Host {
    private String name, host, service;
    private int port,indiceVideo;

    public Host(String name, String host, String service, int port, int indiceVideo){
        this.name = name;
        this.host = host;
        this.service = service;
        this.port = port;
        this.indiceVideo = indiceVideo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getIndiceVideo() {
        return indiceVideo;
    }

    public void setIndiceVideo(int indiceVideo) {
        this.indiceVideo = indiceVideo;
    }
}
