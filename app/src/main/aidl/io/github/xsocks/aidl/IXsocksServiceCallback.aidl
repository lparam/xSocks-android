package io.github.xsocks.aidl;

interface IXsocksServiceCallback {
    oneway void stateChanged(int state, String msg);
}
