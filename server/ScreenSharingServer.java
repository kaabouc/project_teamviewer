package projet.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.awt.*;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.List;

public class ScreenSharingServer extends UnicastRemoteObject implements ScreenSharingInterface {
    private static final long serialVersionUID = 1L;
    private List<byte[]> screenshots;

    // Constructeur du serveur, initialise la liste des captures d'écran et crée le répertoire pour les fichiers reçus
    protected ScreenSharingServer() throws RemoteException {
        screenshots = new ArrayList<>();
        File fileDir = new File(System.getProperty("user.home") + "/Desktop/server_files");
        if (!fileDir.exists()) {
            fileDir.mkdir();
        }
    }

    // Méthode pour recevoir une capture d'écran du client
    @Override
    public synchronized void sendScreenshot(byte[] image) throws RemoteException {
        screenshots.add(image);
    }

    // Méthode pour envoyer la dernière capture d'écran au client
    @Override
    public synchronized byte[] receiveScreenshot() throws RemoteException {
        return screenshots.isEmpty() ? null : screenshots.get(screenshots.size() - 1);
    }

    // Méthode pour simuler un clic de souris sur le serveur à une position donnée
    @Override
    public synchronized void sendMouseClick(int x, int y) throws RemoteException {
        try {
            Robot robot = new Robot();
            robot.mouseMove(x, y);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    // Méthode pour simuler une pression de touche sur le serveur
    @Override
    public synchronized void sendKeyPress(int keyCode) throws RemoteException {
        try {
            Robot robot = new Robot();
            robot.keyPress(keyCode);
            robot.keyRelease(keyCode);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    // Méthode pour recevoir un fichier du client et le stocker dans un répertoire spécifique
    @Override
    public synchronized void sendFile(String fileName, byte[] data) throws RemoteException {
        File fileDir = new File(System.getProperty("user.home") + "/Desktop/server_files");
        try (FileOutputStream fos = new FileOutputStream(new File(fileDir, fileName))) {
            fos.write(data);
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RemoteException("Error writing file: " + e.getMessage(), e);
        }
    }

    // Méthode pour envoyer un fichier au client
    @Override
    public synchronized byte[] receiveFile(String fileName) throws RemoteException {
        File fileDir = new File(System.getProperty("user.home") + "/Desktop/server_files");
        try {
            return Files.readAllBytes(new File(fileDir, fileName).toPath());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RemoteException("Error reading file: " + e.getMessage(), e);
        }
    }

    // Méthode pour lister les fichiers disponibles dans le répertoire du serveur
    @Override
    public synchronized String[] listFiles() throws RemoteException {
        File folder = new File(System.getProperty("user.home") + "/Desktop/server_files");
        return folder.list();
    }

    // Méthode principale pour démarrer le serveur
    public static void main(String[] args) {
        try {
            // Démarrer le registre RMI sur le port 1099
            LocateRegistry.createRegistry(1099);
            ScreenSharingServer server = new ScreenSharingServer();
            // Enregistrer l'objet serveur avec le nom "ScreenSharingServer"
            Naming.rebind("ScreenSharingServer", server);
            System.out.println("Server is ready.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
