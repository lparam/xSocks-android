package io.github.xSocks.aidl;

import android.os.Parcel;
import android.os.Parcelable;

public class Config implements Parcelable {

    public boolean isGlobalProxy = true;
    public boolean isBypassApps = false;
    public boolean isUdpDns = false;

    public String profileName = "Untitled";
    public String proxy = "";
    public String sitekey = "";
    public String route = "all";

    public String proxiedAppString = "";

    public int remotePort = 1073;
    public int localPort = 1080;

    public static final Parcelable.Creator<Config> CREATOR = new Parcelable.Creator<Config>() {
        public Config createFromParcel(Parcel in) {
            return new Config(in);
        }

        public Config[] newArray(int size) {
            return new Config[size];
        }
    };

    public Config(boolean isGlobalProxy, boolean isBypassApps,
                  boolean isUdpDns, String profileName, String proxy, String sitekey,
                  String proxiedAppString, String route, int remotePort, int localPort) {
        this.isGlobalProxy = isGlobalProxy;
        this.isBypassApps = isBypassApps;
        this.isUdpDns = isUdpDns;
        this.profileName = profileName;
        this.proxy = proxy;
        this.sitekey = sitekey;
        this.proxiedAppString = proxiedAppString;
        this.route = route;
        this.remotePort = remotePort;
        this.localPort = localPort == 0 ? this.localPort : localPort;
    }

    private Config(Parcel in) {
        readFromParcel(in);
    }

    public void readFromParcel(Parcel in) {
        isGlobalProxy = in.readInt() == 1;
        isBypassApps = in.readInt() == 1;
        isUdpDns = in.readInt() == 1;
        profileName = in.readString();
        proxy = in.readString();
        sitekey = in.readString();
        proxiedAppString = in.readString();
        route = in.readString();
        remotePort = in.readInt();
        localPort = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(isGlobalProxy ? 1 : 0);
        out.writeInt(isBypassApps ? 1 : 0);
        out.writeInt(isUdpDns ? 1 : 0);
        out.writeString(profileName);
        out.writeString(proxy);
        out.writeString(sitekey);
        out.writeString(proxiedAppString);
        out.writeString(route);
        out.writeInt(remotePort);
        out.writeInt(localPort);
    }
}
