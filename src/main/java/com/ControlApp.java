package com;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import services.primitives.VideoPlayerRemote;

import java.rmi.RemoteException;
import java.rmi.NotBoundException;

public class ControlApp {

    public static void main(String[] args) {
        String hostname = "localhost";
        int port = 1099;
        String serviceName = "test";

        connectToRmi(hostname, port, serviceName);
    }

    public static void connectToRmi(String hostname, int port, String serviceName){
        try {
            Registry registry = LocateRegistry.getRegistry(hostname, port);

            VideoPlayerRemote remoteService = (VideoPlayerRemote) registry.lookup(serviceName);

            remoteService.pauseVideo();

        } catch (RemoteException e) {
            // Handle exceptions related to remote communication (e.g., server down, network issues).
            System.err.println("Client remote exception: " + e.getMessage());
            e.printStackTrace();
        } catch (NotBoundException e) {
            // Handle cases where the remote object is not found in the registry
            // (e.g., incorrect name, server not started).
            System.err.println("Client NotBound exception: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            // Catch any other general exceptions.
            System.err.println("Client general exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
