package services.primitives;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface VideoPlayerRemote extends Remote {

    void playVideo() throws RemoteException;
    void pauseVideo() throws RemoteException;
    void restartVideo() throws RemoteException;

}
