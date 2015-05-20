package io.github.xsocks.aidl;

import io.github.xsocks.aidl.Config;
import io.github.xsocks.aidl.IXsocksServiceCallback;

interface IXsocksService {
    int getState();

    oneway void registerCallback(IXsocksServiceCallback cb);
    oneway void unregisterCallback(IXsocksServiceCallback cb);

    oneway void start(in Config config);
    oneway void stop();
}
