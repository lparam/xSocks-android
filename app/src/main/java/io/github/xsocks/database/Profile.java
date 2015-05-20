package io.github.xsocks.database;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;

@RealmClass
public class Profile extends RealmObject {
    @PrimaryKey
    private int id = 0;
    private String name = "Untitled";
    private String host = "";
    private int localPort = 1080;
    private int remotePort = 1073;
    private String password = "";
    private String route = "all";
    private boolean global = true;
    private boolean bypass = false;
    private boolean udpdns = false;
    private String individual = "";

    public int getId() { return this.id; }

    public void setId(final int id) { this.id = id; }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getLocalPort() {
        return localPort;
    }

    public void setLocalPort(int localPort) {
        this.localPort = localPort;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public void setRemotePort(int remotePort) {
        this.remotePort = remotePort;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public boolean isGlobal() {
        return global;
    }

    public void setGlobal(boolean global) {
        this.global = global;
    }

    public boolean isBypass() {
        return bypass;
    }

    public void setBypass(boolean bypass) {
        this.bypass = bypass;
    }

    public boolean isUdpdns() {
        return udpdns;
    }

    public void setUdpdns(boolean udpdns) {
        this.udpdns = udpdns;
    }

    public String getIndividual() {
        return individual;
    }

    public void setIndividual(String individual) {
        this.individual = individual;
    }
}
