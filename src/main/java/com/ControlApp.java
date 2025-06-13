package com;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import services.primitives.VideoPlayerRemote; // Ensure this import path is correct
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * A command-line interface (CLI) application for controlling remote video players
 * via RMI. This version integrates the RMI connection and command-line menu
 * into a single class.
 */
public class ControlApp {

    // Stores active connections to remote video player services.
    // The key is a unique identifier (e.g., "hostname:port/serviceName")
    // and the value is the remote service stub.
    private static Map<String, VideoPlayerRemote> connectedMachines = new HashMap<>();

    // Scanner for reading user input from the console.
    private static Scanner scanner = new Scanner(System.in);

    /**
     * Main method to start the RMI Video Player Control Application.
     * Initializes the main menu loop.
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        System.out.println("Welcome to the RMI Video Player Control App!");
        mainMenu(); // Start the main interactive menu
        scanner.close(); // Close the scanner when the application exits
        System.out.println("Application exited. Goodbye!");
    }

    /**
     * Displays the main menu and handles user navigation between different actions.
     */
    private static void mainMenu() {
        while (true) {
            System.out.println("\n--- Main Menu ---");
            System.out.println("1. Connect to a new RMI machine");
            System.out.println("2. Select and control an existing machine");
            System.out.println("3. Exit");
            System.out.print("Enter your choice: ");

            String choice = scanner.nextLine().trim(); // Read user input and trim whitespace

            switch (choice) {
                case "1":
                    connectToNewMachine();
                    break;
                case "2":
                    selectAndControlMachine();
                    break;
                case "3":
                    return; // Exit the main menu loop and terminate the application
                default:
                    System.out.println("Invalid choice. Please enter 1, 2, or 3.");
            }
        }
    }

    /**
     * Prompts the user for RMI connection details (hostname, port, service name)
     * and attempts to establish a connection to a new remote video player.
     * If successful, the connection is added to the `connectedMachines` map.
     */
    public static void connectToNewMachine() {
        System.out.println("\n--- Connect to New Machine ---");
        System.out.print("Enter hostname (e.g., localhost): ");
        String hostname = scanner.nextLine().trim();

        int port;
        while (true) { // Loop until a valid port number is entered
            System.out.print("Enter port (e.g., 1099): ");
            try {
                port = Integer.parseInt(scanner.nextLine().trim());
                break; // Exit loop if parsing is successful
            } catch (NumberFormatException e) {
                System.out.println("Invalid port. Please enter a valid number.");
            }
        }

        System.out.print("Enter service name (e.g., test): ");
        String serviceName = scanner.nextLine().trim();

        // Create a unique identifier for this RMI service
        String machineIdentifier = String.format("%s:%d/%s", hostname, port, serviceName);

        // Check if this machine is already connected
        if (connectedMachines.containsKey(machineIdentifier)) {
            System.out.println("You are already connected to this machine: " + machineIdentifier);
            return;
        }

        try {
            System.out.println("Attempting to connect to RMI registry at " + hostname + ":" + port + "...");
            Registry registry = LocateRegistry.getRegistry(hostname, port);
            System.out.println("Looking up service '" + serviceName + "'...");
            VideoPlayerRemote remoteService = (VideoPlayerRemote) registry.lookup(serviceName);

            // Add the successfully connected service to our map
            connectedMachines.put(machineIdentifier, remoteService);
            System.out.println("Successfully connected to RMI machine: " + machineIdentifier);
        } catch (RemoteException e) {
            System.err.println("Connection error: Could not reach RMI registry or remote object.");
            System.err.println("Details: " + e.getMessage());
            System.err.println("Please ensure the RMI registry is running and the service is correctly bound.");
        } catch (NotBoundException e) {
            System.err.println("Service Not Found: The service '" + serviceName + "' is not bound in the registry.");
            System.err.println("Details: " + e.getMessage());
            System.err.println("Verify the service name and ensure the server has started correctly.");
        } catch (Exception e) {
            // Catch any other unexpected exceptions
            System.err.println("An unexpected error occurred during connection: " + e.getMessage());
            e.printStackTrace(); // Print full stack trace for debugging purposes
        }
    }

    /**
     * Displays a list of currently connected RMI machines and allows the user
     * to select one to control. If no machines are connected, it informs the user.
     */
    private static void selectAndControlMachine() {
        if (connectedMachines.isEmpty()) {
            System.out.println("No machines are currently connected. Please connect to a machine first (Option 1).");
            return;
        }

        System.out.println("\n--- Select Machine to Control ---");
        int i = 1;
        // Display each connected machine with a corresponding number
        for (String identifier : connectedMachines.keySet()) {
            System.out.println(i++ + ". " + identifier);
        }
        System.out.println("0. Go back to Main Menu");
        System.out.print("Enter the number of the machine to control: ");

        int machineChoice;
        try {
            machineChoice = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a number corresponding to a machine or 0 to go back.");
            return;
        }

        if (machineChoice == 0) {
            return; // User chose to go back to the Main Menu
        }

        // Validate the user's choice against the number of connected machines
        if (machineChoice < 1 || machineChoice > connectedMachines.size()) {
            System.out.println("Invalid machine number. Please choose from the list.");
            return;
        }

        // Retrieve the selected machine's identifier and its remote service stub
        String[] identifiers = connectedMachines.keySet().toArray(new String[0]);
        String selectedIdentifier = identifiers[machineChoice - 1];
        VideoPlayerRemote selectedService = connectedMachines.get(selectedIdentifier);

        // Pass control to the specific machine's control menu
        controlMachine(selectedIdentifier, selectedService);
    }

    /**
     * Provides a menu for controlling a specific remote video player service.
     * Allows sending play, pause, and restart commands.
     * @param identifier The unique string identifier of the selected machine.
     * @param service The remote `VideoPlayerRemote` service stub.
     */
    private static void controlMachine(String identifier, VideoPlayerRemote service) {
        while (true) {
            System.out.println("\n--- Controlling: " + identifier + " ---");
            System.out.println("1. Play Video");
            System.out.println("2. Pause Video");
            System.out.println("3. Restart Video");
            System.out.println("0. Go back to Select Machine Menu");
            System.out.print("Enter your command choice: ");

            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1":
                        service.playVideo();
                        System.out.println("Command sent: Play video on " + identifier);
                        break;
                    case "2":
                        service.pauseVideo();
                        System.out.println("Command sent: Pause video on " + identifier);
                        break;
                    case "3":
                        service.restartVideo();
                        System.out.println("Command sent: Restart video on " + identifier);
                        break;
                    case "0":
                        return; // Exit the control menu for this machine, go back to select machine menu
                    default:
                        System.out.println("Invalid command choice. Please enter 0, 1, 2, or 3.");
                }
            } catch (RemoteException e) {
                System.err.println("Communication error with " + identifier + ": " + e.getMessage());
                System.err.println("The connection to this machine might have been lost. Removing it from the list.");
                connectedMachines.remove(identifier); // Remove the broken connection
                return; // Go back to the select machine menu as this connection is no longer valid
            } catch (Exception e) {
                // Catch any other unexpected exceptions during command execution
                System.err.println("An unexpected error occurred while controlling " + identifier + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
