package projet.server;

import java.rmi.Remote;
import java.rmi.RemoteException;

// Interface RMI pour la communication entre le client et le serveur
public interface ScreenSharingInterface extends Remote {
    // Envoie une capture d'écran du client au serveur
    void sendScreenshot(byte[] image) throws RemoteException;

    // Reçoit une capture d'écran du serveur au client
    byte[] receiveScreenshot() throws RemoteException;

    // Envoie les coordonnées d'un clic de souris du client au serveur
    void sendMouseClick(int x, int y) throws RemoteException;

    // Envoie une touche pressée du client au serveur
    void sendKeyPress(int keyCode) throws RemoteException;

    // Méthodes pour envoyer et recevoir des fichiers

    // Envoie un fichier du client au serveur
    void sendFile(String fileName, byte[] data) throws RemoteException;

    // Reçoit un fichier du serveur au client
    byte[] receiveFile(String fileName) throws RemoteException;

    // Liste les fichiers disponibles sur le serveur
    String[] listFiles() throws RemoteException;
}
