package cisneros.simpleproxyserver;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class SimpleProxyServer {

    private static final int PORT = 8089;
    private static final String WIFI_HOST = "192.168.1.68"; // sin espacios
    private static final int WIFI_PORT = 8081;
    private static final int MAX_THREADS = 200;

    public static void main(String[] args) {
        ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREADS);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Proxy server escuchando en el puerto " + PORT);

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    threadPool.submit(() -> handleConnection(clientSocket));
                } catch (IOException e) {
                    System.err.println("Error al aceptar conexión: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.err.println("Error al iniciar el servidor: " + e.getMessage());
            System.err.println("¿Ya estás usando el puerto " + PORT + "?");
        }
    }

    private static void handleConnection(Socket clientSocket) {
        System.out.println("Cliente conectado: " + clientSocket.getInetAddress());

        try (Socket wifiSocket = new Socket(WIFI_HOST, WIFI_PORT);
             InputStream clientInput = clientSocket.getInputStream();
             OutputStream clientOutput = clientSocket.getOutputStream();
             InputStream wifiInput = wifiSocket.getInputStream();
             OutputStream wifiOutput = wifiSocket.getOutputStream()) {

            System.out.println("Conectado al servidor WiFi: " + WIFI_HOST + ":" + WIFI_PORT);

            // Hilos para redirigir datos
            Thread clientToWifi = new Thread(() -> transferData(clientInput, wifiOutput));
            Thread wifiToClient = new Thread(() -> transferData(wifiInput, clientOutput));
            clientToWifi.start();
            wifiToClient.start();

            clientToWifi.join();
            wifiToClient.join();

        } catch (IOException e) {
            System.err.println("Error en conexión con cliente o servidor WiFi: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Conexión interrumpida.");
        } finally {
            try {
                clientSocket.close();
            } catch (IOException ignore) {}
            System.out.println("Conexión cerrada: " + clientSocket.getInetAddress());
        }
    }

    private static void transferData(InputStream input, OutputStream output) {
        try {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
                output.flush();
            }
        } catch (IOException e) {
            // Se ignora el error para evitar interrupciones ruidosas
        }
    }
}
