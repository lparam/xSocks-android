package io.github.xSocks.service;


import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class xSocksVpnThread extends Thread {
    private String TAG = "xSocksVpnService";

    private volatile boolean isRunning = true;
    private volatile LocalServerSocket serverSocket = null;

    private xSocksVpnService vpnService;

    public xSocksVpnThread(xSocksVpnService vpnService) {
        this.vpnService = vpnService;
    }

    private void closeServerSocket() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // ignore
            }
            serverSocket = null;
        }
    }

    public void stopThread() {
        isRunning = false;
        closeServerSocket();
    }

    @Override
    public void run() {
        String PATH = "/data/data/io.github.xSocks/protect_path";

        try {
            new File(PATH).delete();
        } catch (Exception e) {
        }

        try {
            LocalSocket localSocket = new LocalSocket();
            localSocket.bind(new LocalSocketAddress(PATH, LocalSocketAddress.Namespace.FILESYSTEM));
            serverSocket = new LocalServerSocket(localSocket.getFileDescriptor());

        } catch (IOException e) {
            Log.e(TAG, "unable to bind", e);
            return;
        }

        ExecutorService pool = Executors.newFixedThreadPool(4);

        while (isRunning) {
            try {
                LocalSocket socket = serverSocket.accept();

                pool.execute(() -> {
                    try {
                        InputStream input = socket.getInputStream();
                        OutputStream output = socket.getOutputStream();

                        input.read();

                        FileDescriptor[] fds = socket.getAncillaryFileDescriptors();

                        if (fds != null && fds.length > 0) {
                            Method getInt = FileDescriptor.class.getDeclaredMethod("getInt$");
                            int fd = (int) getInt.invoke(fds[0]);
                            boolean ret = vpnService.protect(fd);

                            io.github.xSocks.System.jniclose(fd);

                            output.write(ret ? 0 : 1);

                            input.close();
                            output.close();
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Error when protect socket", e);
                    }

                    // close socket
                    try {
                        socket.close();
                    } catch (Exception e) {
                        // ignore
                    }
                });

            } catch (IOException e) {
                Log.e(TAG, "Error when accept socket", e);
                return;
            }
        }
    }

}
