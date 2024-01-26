package com.example.wifipocv2;

import android.net.Network;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ExecutorService;

public class VirtualDatagramSocket implements Runnable {
    private DatagramSocket socket;
    private int localNodeVirtualAddress;
    ExecutorService ioExecutorService;
    //    private VirtualRouter router;
//    private MNetLogger logger;
    String name = null;
    Network boundNetwork = null;

    @Override
    public void run() {
        byte[] buffer = new byte[VirtualPacket.MAX_PAYLOAD_SIZE];
        int localPort = socket.getLocalPort();
//        logger(Log.DEBUG, logPrefix + " Started on " + localPort + " waiting for first packet", null);
        Log.w("A", " Started on " + localPort + " waiting for first packet");
        while (!Thread.interrupted() && !this.socket.isClosed()) {
//            try {
//                val rxPacket = DatagramPacket(buffer, 0, buffer.size)
//                this.socket.receive(rxPacket);
//
//                val rxVirtualPacket = VirtualPacket.fromDatagramPacket(rxPacket)
//                router.route(
//                        packet = rxVirtualPacket,
//                        datagramPacket = rxPacket,
//                        virtualNodeDatagramSocket = this,
//                        )
//            }catch(e: Exception) {
//                if(!socket.isClosed)
//                    logger(Log.WARN, "$logPrefix : run : exception handling packet", e)
//            }
//        }
//        logger(Log.DEBUG, "$logPrefix : run : finished")
        }
    }
}