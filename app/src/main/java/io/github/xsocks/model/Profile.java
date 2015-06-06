package io.github.xsocks.model;

import com.google.gson.annotations.SerializedName;

public class Profile {
    @SerializedName("id")
    private int id = 0;
    @SerializedName("name")
    private String name = "Untitled";
    @SerializedName("host")
    private String host = "";
    @SerializedName("localPort")
    private int localPort = 1080;
    @SerializedName("remotePort")
    private int remotePort = 1073;
    @SerializedName("password")
    private String password = "";
    @SerializedName("route")
    private String route = "all";
    @SerializedName("global")
    private boolean global = true;
    @SerializedName("bypass")
    private boolean bypass = false;
    @SerializedName("udpdns")
    private boolean udpdns = false;
    private String individual = "";

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setLocalPort(int localPort) {
        this.localPort = localPort;
    }

    public void setRemotePort(int remotePort) {
        this.remotePort = remotePort;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public void setGlobal(boolean global) {
        this.global = global;
    }

    public void setBypass(boolean bypass) {
        this.bypass = bypass;
    }

    public void setUdpdns(boolean udpdns) {
        this.udpdns = udpdns;
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getHost() {
        return this.host;
    }

    public int getLocalPort() {
        return localPort;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public String getPassword() {
        return password;
    }

    public String getRoute() {
        return route;
    }

    public boolean isGlobal() {
        return global;
    }

    public boolean isBypass() {
        return bypass;
    }

    public boolean isUdpdns() {
        return udpdns;
    }

    public void setIndividual(String individual) {
        this.individual = individual;
    }

    public String getIndividual() {

        return individual;
    }

}
