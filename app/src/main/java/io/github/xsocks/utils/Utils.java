package io.github.xsocks.utils;

import android.os.Build;
import android.util.Log;

import org.xbill.DNS.AAAARecord;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

import io.github.xsocks.BuildConfig;

public class Utils {
    private static String TAG = "Xsocks";

    public static boolean isLollipopOrAbove() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static String readFromFile(String name) {
        File file = new File(Constants.Path.BASE, name);
        StringBuilder text = new StringBuilder();
        if (file.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                text.append(br.readLine());
                br.close();
            }
            catch (IOException e) {
                Log.e(TAG, "Can not read file: " + e.toString());
            }
        } else {
            Log.e(TAG, "File not found: " + name);
        }
        return text.toString();
    }

    public static String resolve(String host, int addrType) {
        try {
            Lookup lookup = new Lookup(host, addrType);
            SimpleResolver resolver = new SimpleResolver("114.114.114.114");
            resolver.setTimeout(5);
            lookup.setResolver(resolver);
            Record[] result = lookup.run();
            if (result == null) return null;

            List<Record> records = java.util.Arrays.asList(result);
            java.util.Collections.shuffle(records);
            for (Record record : records) {
                if (addrType == Type.A) {
                    return ((ARecord) record).getAddress().getHostAddress();
                } else if (addrType == Type.AAAA) {
                    return ((AAAARecord) record).getAddress().getHostAddress();
                }
            }

        } catch (Exception ex) {
            return null;
        }

        return null;
    }

    public static String resolve(String host) {
        try {
            InetAddress addr = InetAddress.getByName(host);
            return addr.getHostAddress();
        } catch (Exception e) {
            return null;
        }
    }

    public static String resolve(String host, boolean enableIPv6) {
        String addr;
        if (enableIPv6 && isIPv6Support()) {
            addr = resolve(host, Type.AAAA);
            if (addr != null) {
                return addr;
            }
        }

        addr = resolve(host, Type.A);
        if (addr != null) {
            return addr;
        }

        addr = resolve(host);

        return addr;
    }

    private static boolean isIPv6Support() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface intf = interfaces.nextElement();
                Enumeration<InetAddress> addrs = intf.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (!addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                        String sAddr = addr.getHostAddress().toUpperCase();
                        if (isIPv6Address(sAddr)) {
                            if (BuildConfig.DEBUG) Log.d(TAG, "IPv6 address detected");
                            return true;
                        }
                    }
                }
            }
        } catch (Exception ex) {
                Log.e(TAG, "Failed to get interfaces' addresses.", ex);
        }
        return false;
    }

    public static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;

        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    private static final Pattern IPV4_PATTERN =
            Pattern.compile(
                    "^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$");

    private static final Pattern IPV6_STD_PATTERN =
            Pattern.compile(
                    "^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$");

    private static final Pattern IPV6_HEX_COMPRESSED_PATTERN =
            Pattern.compile(
                    "^((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)::((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)$");

    public static boolean isIPv4Address(final String input) {
        return IPV4_PATTERN.matcher(input).matches();
    }

    public static boolean isIPv6StdAddress(final String input) {
        return IPV6_STD_PATTERN.matcher(input).matches();
    }

    public static boolean isIPv6HexCompressedAddress(final String input) {
        return IPV6_HEX_COMPRESSED_PATTERN.matcher(input).matches();
    }

    public static boolean isIPv6Address(final String input) {
        return isIPv6StdAddress(input) || isIPv6HexCompressedAddress(input);
    }

}
