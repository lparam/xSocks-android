package io.github.xSocks.aidl;

import io.github.xSocks.aidl.Config;
import io.github.xSocks.aidl.IxSocksServiceCallback;

interface IxSocksService {
    int getState();

    oneway void registerCallback(IxSocksServiceCallback cb);
    oneway void unregisterCallback(IxSocksServiceCallback cb);

    oneway void start(in Config config);
    oneway void stop();
}
