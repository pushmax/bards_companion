package com.example.bardscompanion;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class NetworkUtils {
    
    public static String getLocalIpAddress(Context context) {
        // First try to get IP from WiFi connection
        String wifiIp = getWifiIpAddress(context);
        if (wifiIp != null && !wifiIp.equals("0.0.0.0")) {
            return wifiIp;
        }
        
        // Fallback to network interfaces
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        // Return IPv4 addresses only
                        if (sAddr != null && sAddr.indexOf(':') < 0) {
                            return sAddr;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            // Ignore
        }
        
        return "Unable to get IP";
    }
    
    private static String getWifiIpAddress(Context context) {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                int ipAddress = wifiInfo.getIpAddress();
                
                if (ipAddress != 0) {
                    return String.format("%d.%d.%d.%d",
                            (ipAddress & 0xff),
                            (ipAddress >> 8 & 0xff),
                            (ipAddress >> 16 & 0xff),
                            (ipAddress >> 24 & 0xff));
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
    
    public static boolean isWifiConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && 
                   activeNetwork.getType() == ConnectivityManager.TYPE_WIFI &&
                   activeNetwork.isConnected();
        }
        return false;
    }
}