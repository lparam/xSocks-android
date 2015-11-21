package io.github.xSocks.aidl;

interface IxSocksServiceCallback {
    oneway void stateChanged(int state, String msg);
}
