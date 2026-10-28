package com.offsec.nethunter.rdp.engine;

import android.util.Log;
import java.net.Socket;
import java.net.SocketException;

/**
 * Socket Optimization Engine for RDP Streaming.
 * Eliminates Nagle's algorithm delay and configures low-latency buffer windows.
 */
public class SocketTuner {
    private static final String TAG = "SocketTuner";

    public static void tuneSocket(Socket socket, RdpEngineConfig config) {
        if (socket == null || !socket.isConnected()) return;

        try {
            // Disable Nagle's algorithm (TCP_NODELAY): sends packets immediately without waiting to fill MTU
            socket.setTcpNoDelay(config.isTcpNoDelay());

            // Set high throughput receive buffer
            socket.setReceiveBufferSize(config.getSocketRcvBufSize());

            // Set send buffer size
            socket.setSendBufferSize(config.getSocketSndBufSize());

            // Enable TCP Keep-Alive to detect dropped WiFi/Cellular connections early
            socket.setKeepAlive(true);

            // Set performance preferences: connectionTime=0, latency=1 (Highest priority), bandwidth=2
            socket.setPerformancePreferences(0, 1, 2);

            // Minimize traffic class delay (IPTOS_LOWDELAY = 0x10)
            socket.setTrafficClass(0x10);

            Log.d(TAG, "Socket tuned for ultra-low-latency 60FPS RDP streaming.");
        } catch (SocketException e) {
            Log.w(TAG, "Socket tuning partial failure: " + e.getMessage());
        }
    }
}
