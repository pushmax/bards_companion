package com.example.bardscompanion;

import android.content.Context;
import android.util.Log;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class SimpleHttpServer {
    private static final String TAG = "SimpleHttpServer";
    private static final int PORT = 8080;
    
    private ServerSocket serverSocket;
    private ExecutorService executor;
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private Song currentSong = null;
    private final Gson gson = new Gson();
    private Context context;
    
    private static SimpleHttpServer instance;

    public SimpleHttpServer(Context context) {
        this.context = context;
        instance = this;
    }

    public boolean start() {
        try {
            serverSocket = new ServerSocket(PORT);
            executor = Executors.newFixedThreadPool(10);
            isRunning.set(true);
            
            executor.execute(() -> {
                while (isRunning.get()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        executor.execute(() -> handleClient(clientSocket));
                    } catch (IOException e) {
                        if (isRunning.get()) {
                            Log.e(TAG, "Error accepting client connection", e);
                        }
                    }
                }
            });
            
            Log.i(TAG, "HTTP Server started on port " + PORT);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to start server", e);
            return false;
        }
    }

    public void stop() {
        isRunning.set(false);
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing server socket", e);
            }
        }
        if (executor != null) {
            executor.shutdown();
        }
        instance = null;
        Log.i(TAG, "HTTP Server stopped");
    }
    
    public static void clearCurrentSongStatic() {
        if (instance != null) {
            instance.setCurrentSong(null);
        }
    }

    public void setCurrentSong(Song song) {
        this.currentSong = song;
    }

    public Song getCurrentSong() {
        return currentSong;
    }

    private void handleClient(Socket clientSocket) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));
             OutputStream outputStream = clientSocket.getOutputStream()) {

            String requestLine = reader.readLine();
            if (requestLine == null) return;

            String[] requestParts = requestLine.split(" ");
            if (requestParts.length < 2) return;

            String method = requestParts[0];
            String path = requestParts[1];

            // Skip headers
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                // Skip headers
            }

            if ("GET".equals(method)) {
                if ("/".equals(path)) {
                    serveWebApp(outputStream);
                } else if ("/current-song".equals(path)) {
                    serveCurrentSong(outputStream);
                } else {
                    serve404(outputStream);
                }
            } else {
                serve404(outputStream);
            }

        } catch (IOException e) {
            Log.e(TAG, "Error handling client", e);
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing client socket", e);
            }
        }
    }

    private void serveWebApp(OutputStream outputStream) throws IOException {
        String html = generateWebAppHtml();
        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n" +
                "Content-Length: " + html.getBytes(StandardCharsets.UTF_8).length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n" + html;
        outputStream.write(response.getBytes(StandardCharsets.UTF_8));
    }

    private void serveCurrentSong(OutputStream outputStream) throws IOException {
        String jsonResponse;
        if (currentSong != null) {
            jsonResponse = gson.toJson(currentSong);
        } else {
            jsonResponse = "null";
        }

        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Content-Length: " + jsonResponse.getBytes(StandardCharsets.UTF_8).length + "\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n" +
                "\r\n" + jsonResponse;
        outputStream.write(response.getBytes(StandardCharsets.UTF_8));
    }

    private void serve404(OutputStream outputStream) throws IOException {
        String html = "<html><body><h1>404 Not Found</h1></body></html>";
        String response = "HTTP/1.1 404 Not Found\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n" +
                "Content-Length: " + html.getBytes(StandardCharsets.UTF_8).length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n" + html;
        outputStream.write(response.getBytes(StandardCharsets.UTF_8));
    }

    private String generateWebAppHtml() {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Bards Companion - Client</title>\n" +
                "    <style>\n" +
                "        body {\n" +
                "            font-family: Arial, sans-serif;\n" +
                "            margin: 0;\n" +
                "            padding: 20px;\n" +
                "            background-color: #f5f5f5;\n" +
                "            display: flex;\n" +
                "            flex-direction: column;\n" +
                "            min-height: 100vh;\n" +
                "        }\n" +
                "        .container {\n" +
                "            flex: 1;\n" +
                "            display: flex;\n" +
                "            flex-direction: column;\n" +
                "            justify-content: center;\n" +
                "            align-items: center;\n" +
                "            text-align: center;\n" +
                "        }\n" +
                "        .song-container {\n" +
                "            background: white;\n" +
                "            border-radius: 8px;\n" +
                "            padding: 30px;\n" +
                "            box-shadow: 0 2px 10px rgba(0,0,0,0.1);\n" +
                "            max-width: 800px;\n" +
                "            width: 100%;\n" +
                "        }\n" +
                "        .song-title {\n" +
                "            font-size: 2.5em;\n" +
                "            color: #333;\n" +
                "            margin-bottom: 10px;\n" +
                "        }\n" +
                "        .song-author {\n" +
                "            font-size: 1.5em;\n" +
                "            color: #666;\n" +
                "            margin-bottom: 30px;\n" +
                "        }\n" +
                "        .song-lyrics {\n" +
                "            font-size: 1.2em;\n" +
                "            line-height: 1.6;\n" +
                "            color: #333;\n" +
                "            white-space: pre-wrap;\n" +
                "            text-align: left;\n" +
                "        }\n" +
                "        .waiting {\n" +
                "            font-size: 1.5em;\n" +
                "            color: #666;\n" +
                "        }\n" +
                "        .status {\n" +
                "            position: fixed;\n" +
                "            top: 10px;\n" +
                "            right: 10px;\n" +
                "            padding: 10px;\n" +
                "            background: #4CAF50;\n" +
                "            color: white;\n" +
                "            border-radius: 4px;\n" +
                "            font-size: 0.9em;\n" +
                "        }\n" +
                "        @media (max-width: 600px) {\n" +
                "            .song-title { font-size: 2em; }\n" +
                "            .song-author { font-size: 1.2em; }\n" +
                "            .song-lyrics { font-size: 1em; }\n" +
                "            .song-container { padding: 20px; }\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"status\" id=\"status\">Connected</div>\n" +
                "    <div class=\"container\">\n" +
                "        <div class=\"song-container\" id=\"songContainer\">\n" +
                "            <div class=\"waiting\" id=\"waitingMessage\">\n" +
                "                Waiting for the performer to select a song...\n" +
                "            </div>\n" +
                "            <div id=\"songContent\" style=\"display: none;\">\n" +
                "                <h1 class=\"song-title\" id=\"songTitle\"></h1>\n" +
                "                <h2 class=\"song-author\" id=\"songAuthor\"></h2>\n" +
                "                <div class=\"song-lyrics\" id=\"songLyrics\"></div>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "\n" +
                "    <script>\n" +
                "        let currentSong = null;\n" +
                "        let pollInterval;\n" +
                "\n" +
                "        function updateDisplay(song) {\n" +
                "            const waitingMessage = document.getElementById('waitingMessage');\n" +
                "            const songContent = document.getElementById('songContent');\n" +
                "            const songTitle = document.getElementById('songTitle');\n" +
                "            const songAuthor = document.getElementById('songAuthor');\n" +
                "            const songLyrics = document.getElementById('songLyrics');\n" +
                "\n" +
                "            if (song) {\n" +
                "                songTitle.textContent = song.name;\n" +
                "                songAuthor.textContent = 'by ' + song.author;\n" +
                "                songLyrics.textContent = song.lyrics;\n" +
                "                waitingMessage.style.display = 'none';\n" +
                "                songContent.style.display = 'block';\n" +
                "            } else {\n" +
                "                waitingMessage.style.display = 'block';\n" +
                "                songContent.style.display = 'none';\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        function pollCurrentSong() {\n" +
                "            fetch('/current-song')\n" +
                "                .then(response => response.json())\n" +
                "                .then(song => {\n" +
                "                    if (JSON.stringify(song) !== JSON.stringify(currentSong)) {\n" +
                "                        currentSong = song;\n" +
                "                        updateDisplay(song);\n" +
                "                    }\n" +
                "                    document.getElementById('status').textContent = 'Connected';\n" +
                "                    document.getElementById('status').style.backgroundColor = '#4CAF50';\n" +
                "                })\n" +
                "                .catch(error => {\n" +
                "                    console.error('Error polling current song:', error);\n" +
                "                    document.getElementById('status').textContent = 'Disconnected';\n" +
                "                    document.getElementById('status').style.backgroundColor = '#f44336';\n" +
                "                });\n" +
                "        }\n" +
                "\n" +
                "        // Start polling\n" +
                "        pollCurrentSong();\n" +
                "        pollInterval = setInterval(pollCurrentSong, 1000);\n" +
                "\n" +
                "        // Cleanup on page unload\n" +
                "        window.addEventListener('beforeunload', () => {\n" +
                "            if (pollInterval) {\n" +
                "                clearInterval(pollInterval);\n" +
                "            }\n" +
                "        });\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
    }
}