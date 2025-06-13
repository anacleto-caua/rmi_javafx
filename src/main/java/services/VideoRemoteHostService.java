package services;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import javafx.application.Platform;
import javafx.scene.media.MediaPlayer;
import services.primitives.VideoPlayerRemote;

public class VideoRemoteHostService extends UnicastRemoteObject implements VideoPlayerRemote {

        private transient MediaPlayer currentMediaPlayer; // Use 'transient' and make it private

    public VideoRemoteHostService() throws RemoteException {
        super(); // Call the constructor of the UnicastRemoteObject superclass
    }

    // Method to set the MediaPlayer instance
    public void setMediaPlayer(MediaPlayer player) throws RemoteException {
        // Ensure UI updates are on the JavaFX Application Thread
        Platform.runLater(() -> {
            this.currentMediaPlayer = player;
            System.out.println("MediaPlayer instance set in VideoRemoteHostService.");
        });
    }

    @Override
    public void playVideo() throws RemoteException {
        // Ensure UI updates are on the JavaFX Application Thread
        Platform.runLater(() -> {
            if (currentMediaPlayer != null) {
                currentMediaPlayer.play();
                System.out.println("Video playback initiated remotely.");
            } else {
                System.err.println("Cannot play video: MediaPlayer is not set.");
            }
        });
    }

    @Override
    public void pauseVideo() throws RemoteException {
        // Ensure UI updates are on the JavaFX Application Thread
        Platform.runLater(() -> {
            if (currentMediaPlayer != null) {
                currentMediaPlayer.pause();
                System.out.println("Video playback paused remotely.");
            } else {
                System.err.println("Cannot pause video: MediaPlayer is not set.");
            }
        });
    }

    @Override
    public void restartVideo() throws RemoteException {
        // Ensure UI updates are on the JavaFX Application Thread
        Platform.runLater(() -> {
            if (currentMediaPlayer != null) {
                currentMediaPlayer.seek(currentMediaPlayer.getStartTime());
                currentMediaPlayer.play();
                System.out.println("Video playback restarted remotely.");
            } else {
                System.err.println("Cannot restart video: MediaPlayer is not set.");
            }
        });
    }

}
