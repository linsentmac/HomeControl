package com.lenovo.devicecontrol.conn_manager;

/**
 * Created by linsen on 17-11-6.
 */

public class WifiConnectionStateListener {

    /**
     * Called when the device is start connect to the network.
     */
    public void onWifiStartConnect() {
    }

    /**
     * Called when the device is connected to the network.
     */
    public void onWifiConnected() {
    }

    /**
     * Called when the device is disconnected from the network.
     */
    public void onWifiDisconnected() {
    }

    /**
     * Called when its suspended WIFI connection is resumed, meaning the connection
     * now allows throughput.
     *//*
    public void onWifiResumed() {
    }

    *//**
     * Called when its current WIFI connection is suspended, meaning there is no data throughput.
     *//*
    public void onWifiSuspended() {
    }

    *//**
     * Called when the device is disconnected from the network.
     *
     * @param cause wifi disconnection error cause.
     *//*
    public void onWifiDisconnectedWithCause(int cause) {
    }*/
}
