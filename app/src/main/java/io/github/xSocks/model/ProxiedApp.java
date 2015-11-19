package io.github.xSocks.model;

public class ProxiedApp {
    private int uid;
    private String name;
    private String packageName;
    private boolean proxied;

    public ProxiedApp(int uid, String name, String packageName, boolean proxied) {
        this.uid = uid;
        this.name = name;
        this.packageName = packageName;
        this.proxied = proxied;
    }

    public int getId() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public String getPackageName() {
        return packageName;
    }

    public boolean getProxied() {
        return proxied;
    }

    public void setProxied(boolean proxied) {
        this.proxied = proxied;
    }
}
