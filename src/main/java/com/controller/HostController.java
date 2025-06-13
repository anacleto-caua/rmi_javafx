package com.controller;

import com.model.Host;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.*;
import javafx.stage.Stage;
import services.VideoRemoteHostService;
import javafx.scene.Node;
import javafx.fxml.Initializable;

import java.io.File;
import java.net.URL;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ResourceBundle;
import java.rmi.registry.Registry;

public class HostController implements Initializable {
    @FXML private TextField nameField;
    @FXML private TextField hostField;
    @FXML private TextField serviceField;
    @FXML private TextField portField;
    @FXML private VBox vboxContainer;
    @FXML private MediaView mediaView;
    @FXML private ComboBox<String> videoComboBox;

    private int indiceVideo = 0;
    private MediaPlayer mediaPlayer; // Still need this locally to manage the player
    private VideoRemoteHostService videoRemoteService; // Reference to the actual service implementation

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadComboBox();
    }

    @FXML
    public void createHost(ActionEvent event){
        try{
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            init(stage);

            String name = nameField.getText().trim();
            String hostname = hostField.getText().trim();
            String serviceName = serviceField.getText().trim();
            int port = Integer.parseInt(portField.getText().trim());

            Host host = new Host(name, hostname, serviceName, port, indiceVideo);

            if(checkFields(host)) {
                initHost(host);
            } else {
                loadErroFields();
            }

        } catch (NumberFormatException e) {
            System.err.println("Erro: A porta deve ser um número válido. " + e.getMessage());
            loadErroFields();
        } catch (Exception e) {
            System.err.println("Erro ao criar host: " + e.getMessage());
            Alert alert = loadErro();
            alert.setTitle("Erro Geral");
            alert.setHeaderText("Erro Inesperado");
            alert.setContentText("Ocorreu um erro ao tentar criar o host: " + e.getMessage());
            alert.showAndWait();
        }
    }

    public void init(Stage stage) {
        stage.setOnCloseRequest(event-> {
            Platform.exit();
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose(); // Dispose on close
            }
            try{
                // Unbind the service if it's still bound
                if (videoRemoteService != null) { // You might need a way to check if it's bound
                    // You'd need a method to unbind, or rely on JVM shutdown to clean up
                    // For RMI, exiting the JVM usually unbinds the remote objects
                    System.out.println("Attempting to unbind RMI service...");
                    try {
                        Registry registry = LocateRegistry.getRegistry(Integer.parseInt(portField.getText().trim()));
                        registry.unbind(serviceField.getText().trim());
                        System.out.println("Service unbound successfully.");
                    } catch (Exception unbindEx) {
                        System.err.println("Error unbinding service: " + unbindEx.getMessage());
                    }
                }
                Thread.sleep(1000);
                System.exit(0);
            }catch(Exception e){
                System.out.println("Erro ao encerrar: " + e.getMessage());
            }
        });
    }

    private void initHost(Host host){
        try {
            // Create the service instance
            videoRemoteService = new VideoRemoteHostService();

            Registry registry;
            try {
                registry = LocateRegistry.getRegistry(host.getPort());
                registry.list(); // Check if registry is active
            } catch (java.rmi.ConnectException e) {
                System.out.println("RMI Registry not found on port " + host.getPort() + ". Creating a new one...");
                registry = LocateRegistry.createRegistry(host.getPort());
            }

            // Bind the service to the registry
            registry.rebind(host.getService(), videoRemoteService);
            System.out.println("VideoRemoteHostService bound to RMI Registry.");

            String rmi = "rmi://" + host.getHost() + ":" + host.getPort() + "/" + host.getService();
            System.out.println("Hosting server listening at: [ " + rmi + " ]");

        } catch (Exception e) {
            System.err.println("Erro ao inicializar host RMI: " + e.getMessage());
            Alert alert = loadErro();
            alert.setTitle("Erro RMI");
            alert.setHeaderText("Problema ao iniciar o serviço RMI.");
            alert.setContentText("Pode ser que o Registry já esteja rodando na porta " + host.getPort() + " ou outro erro ocorreu. Detalhes: " + e.getMessage());
            alert.showAndWait();
            return; // Stop initialization if RMI setup fails
        }

        initViewVideo(); // Display the video player and pass the MediaPlayer to the service
    }

    private void initViewVideo() {
        if (vboxContainer == null) {
            System.err.println("vboxContainer is null. Cannot initialize video view.");
            return;
        }
        vboxContainer.getChildren().clear();

        File[] files = videoFolder();
        if (files == null || files.length == 0 || indiceVideo < 0 || indiceVideo >= files.length) {
            System.err.println("No video files found or invalid video index.");
            return;
        }

        File videoFile = files[indiceVideo];
        String videoUri = videoFile.toURI().toString();

        Media media = new Media(videoUri);
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }
        mediaPlayer = new MediaPlayer(media);

        if (mediaPlayer == null) {
            System.err.println("MediaPlayer is null. Cannot initialize MediaView.");
            return;
        }

        mediaView = new MediaView(mediaPlayer); // Re-assigning mediaView is important

        mediaView.setFitWidth(700);
        mediaView.setFitHeight(400);
        mediaView.setPreserveRatio(true);

        vboxContainer.getChildren().add(mediaView);

        mediaPlayer.setAutoPlay(false);

        // --- Crucial step: Pass the MediaPlayer to the remote service ---
        if (videoRemoteService != null) {
            try {
                videoRemoteService.setMediaPlayer(mediaPlayer);
            } catch (RemoteException e) {
                System.err.println("Failed to set MediaPlayer on remote service: " + e.getMessage());
            }
        }
    }

    private File[] videoFolder(){
        File folder = new File("src/main/resources/video");
        if (!folder.exists() || !folder.isDirectory()) {
            System.err.println("Video folder not found or is not a directory: " + folder.getAbsolutePath());
            return new File[0];
        }
        return folder.listFiles();
    }

    private void loadComboBox(){
        File[] files = videoFolder();
        if (files == null || files.length == 0) {
            System.err.println("No video files found in the specified directory.");
            videoComboBox.setDisable(true);
            return;
        }

        for (File file : files) {
            videoComboBox.getItems().add(file.getName());
        }

        if (!videoComboBox.getItems().isEmpty()) {
            videoComboBox.getSelectionModel().selectFirst();
            indiceVideo = videoComboBox.getSelectionModel().getSelectedIndex();
        } else {
            indiceVideo = -1;
        }

        videoComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                indiceVideo = videoComboBox.getSelectionModel().getSelectedIndex();
                // When a new video is selected, stop the current one and load the new one.
                // This will also pass the new MediaPlayer to the remote service.
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                }
                // initViewVideo();
            }
        });
    }

    private boolean checkFields(Host host) {
        if (host.getHost() == null || host.getHost().trim().isEmpty()) return false;
        if (host.getName() == null || host.getName().trim().isEmpty()) return false;
        if (host.getService() == null || host.getService().trim().isEmpty()) return false;
        if (host.getPort() <= 0) return false;
        if (indiceVideo == -1) return false;
        return true;
    }

    private boolean checkID(Host host){
        String hostPort= "rmi://" + host.getHost() + ":" + host.getPort() + "/";
        try{
            String[] objetos = Naming.list(hostPort);
            for(String obj : objetos){
                String id = obj.substring(obj.lastIndexOf('/') + 1);
                if(host.getService().equals(id)) return false;
            }
        }catch (Exception e) {
            System.err.println("Host Error while checking ID: "+e.getMessage());
            Alert alert = loadErro();
            alert.setTitle("Erro de Conexão RMI");
            alert.setHeaderText("Não foi possível verificar o serviço RMI.");
            alert.setContentText("Verifique o host e a porta, ou se o Registry está ativo. Erro: " + e.getMessage());
            alert.showAndWait();
            return false;
        }
        return true;
    }

    private void loadErroID(String id){
        Alert alert = loadErro();
        alert.setTitle("Erro de Registro");
        alert.setHeaderText("ID já registrado");
        alert.setContentText("O ID \"" + id + "\" já está em uso no RMI.\nEscolha outro identificador.");
        alert.showAndWait();
    }

    private void loadErroFields(){
        Alert alert = loadErro();
        alert.setTitle("Campos inválidos");
        alert.setHeaderText("Preencha todos os campos corretamente");
        alert.setContentText("Verifique se todos os campos foram preenchidos, se a porta é válida e se um vídeo foi selecionado.");
        alert.showAndWait();
    }

    private Alert loadErro(){
        Alert alert = new Alert(AlertType.ERROR);
        alert.getDialogPane().getStyleClass().add("modern-card");
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/erro.css").toExternalForm());
        return alert;
    }

    // --- Modified Methods for Video Playback Control ---

    @FXML
    private void playVideo(ActionEvent event) {
        if (videoRemoteService != null) {
            try {
                videoRemoteService.playVideo();
            } catch (RemoteException e) {
                System.err.println("Error calling playVideo on remote service: " + e.getMessage());
                // Handle RMI exception, e.g., show an alert to the user
                Alert alert = loadErro();
                alert.setTitle("Erro de Serviço Remoto");
                alert.setHeaderText("Não foi possível reproduzir o vídeo.");
                alert.setContentText("Verifique a conexão RMI. Erro: " + e.getMessage());
                alert.showAndWait();
            }
        } else {
            System.err.println("VideoRemoteHostService is not initialized.");
        }
    }

    @FXML
    private void pauseVideo(ActionEvent event) {
        if (videoRemoteService != null) {
            try {
                videoRemoteService.pauseVideo();
            } catch (RemoteException e) {
                System.err.println("Error calling pauseVideo on remote service: " + e.getMessage());
                Alert alert = loadErro();
                alert.setTitle("Erro de Serviço Remoto");
                alert.setHeaderText("Não foi possível pausar o vídeo.");
                alert.setContentText("Verifique a conexão RMI. Erro: " + e.getMessage());
                alert.showAndWait();
            }
        } else {
            System.err.println("VideoRemoteHostService is not initialized.");
        }
    }

    @FXML
    private void restartVideo(ActionEvent event) {
        if (videoRemoteService != null) {
            try {
                videoRemoteService.restartVideo();
            } catch (RemoteException e) { 
                System.err.println("Error calling restartVideo on remote service: " + e.getMessage());
                Alert alert = loadErro();
                alert.setTitle("Erro de Serviço Remoto");
                alert.setHeaderText("Não foi possível reiniciar o vídeo.");
                alert.setContentText("Verifique a conexão RMI. Erro: " + e.getMessage());
                alert.showAndWait();
            }
        } else {
            System.err.println("VideoRemoteHostService is not initialized.");
        }
    }
}