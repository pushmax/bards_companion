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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class SimpleHttpServer {
    private static final String TAG = "SimpleHttpServer";
    private static final int PORT = 8080;
    
    private ServerSocket serverSocket;
    private ExecutorService executor;
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private Song currentSong = null;
    private final Gson gson = new Gson();
    private Context context;
    private DatabaseHelper databaseHelper;
    
    // Voting system
    private final Map<String, Long> clientVotes = new ConcurrentHashMap<>(); // clientId -> songId
    
    private static SimpleHttpServer instance;

    public SimpleHttpServer(Context context) {
        this.context = context;
        this.databaseHelper = new DatabaseHelper(context);
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
        // Clear votes when a new song starts
        if (song != null) {
            synchronized (clientVotes) {
                clientVotes.clear();
            }
        }
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
            String clientIP = clientSocket.getRemoteSocketAddress().toString();

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
                } else if ("/available-songs".equals(path)) {
                    serveAvailableSongs(outputStream);
                } else if (path.startsWith("/voting-state?")) {
                    serveVotingState(path, clientIP, outputStream);
                } else if (path.startsWith("/vote?")) {
                    handleVote(path, clientIP, outputStream);
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

    private void serveAvailableSongs(OutputStream outputStream) throws IOException {
        List<Song> allSongs = databaseHelper.getAllSongs();
        List<SongInfo> songInfos = new ArrayList<>();
        
        for (Song song : allSongs) {
            songInfos.add(new SongInfo(song.getId(), song.getAuthor(), song.getName()));
        }
        
        String jsonResponse = gson.toJson(songInfos);
        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Content-Length: " + jsonResponse.getBytes(StandardCharsets.UTF_8).length + "\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n" +
                "\r\n" + jsonResponse;
        outputStream.write(response.getBytes(StandardCharsets.UTF_8));
    }

    private void serveVotingState(String path, String clientIP, OutputStream outputStream) throws IOException {
        // Extract clientId and songId from query parameters: /vote?clientId=abc&songId=123
        String query = path.substring(path.indexOf('?') + 1);
        String[] params = query.split("&");
        String clientId = null;
        
        for (String param : params) {
            if (param.startsWith("clientId=")) {
                clientId = param.substring(9);
            } 
        }

        Map<Long, Integer> voteCounts = getVoteCounts();
        
        // Create simple response with just vote counts
        VotingStateResponse response = new VotingStateResponse();
        response.voteCounts = voteCounts;
        var clientVote = clientVotes.get(clientId);
        response.clientVote = String.valueOf(clientVote);
        
        String jsonResponse = gson.toJson(response);
        
        String httpResponse = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Content-Length: " + jsonResponse.getBytes(StandardCharsets.UTF_8).length + "\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n" +
                "\r\n" + jsonResponse;
        outputStream.write(httpResponse.getBytes(StandardCharsets.UTF_8));
    }
    
    private static class VotingStateResponse {
        public Map<Long, Integer> voteCounts;
        public String clientVote;
    }

    private void handleVote(String path, String clientIP, OutputStream outputStream) throws IOException {
        try {
            // Extract clientId and songId from query parameters: /vote?clientId=abc&songId=123
            String query = path.substring(path.indexOf('?') + 1);
            String[] params = query.split("&");
            String clientId = null;
            long songId = -1;
            
            for (String param : params) {
                if (param.startsWith("clientId=")) {
                    clientId = param.substring(9);
                } else if (param.startsWith("songId=")) {
                    songId = Long.parseLong(param.substring(7));
                }
            }
            
            if (clientId != null && songId != -1) {
                // Synchronize vote updates to prevent race conditions
                synchronized (clientVotes) {
                    // Check if client already voted for this song
                    Long currentVote = clientVotes.get(clientId);
                    if (currentVote != null && currentVote.equals(songId)) {
                        // Client is trying to vote for the same song again - ignore
                        String jsonResponse = "{\"success\": false, \"message\": \"Already voted for this song\"}";
                        String response = "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: application/json; charset=UTF-8\r\n" +
                                "Content-Length: " + jsonResponse.getBytes(StandardCharsets.UTF_8).length + "\r\n" +
                                "Access-Control-Allow-Origin: *\r\n" +
                                "Connection: close\r\n" +
                                "\r\n" + jsonResponse;
                        outputStream.write(response.getBytes(StandardCharsets.UTF_8));
                        return;
                    }
                    
                    // Update client's vote (this overwrites any previous vote)
                    clientVotes.put(clientId, songId);
                }
                
                String jsonResponse = "{\"success\": true, \"message\": \"Vote recorded\"}";
                String response = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: application/json; charset=UTF-8\r\n" +
                        "Content-Length: " + jsonResponse.getBytes(StandardCharsets.UTF_8).length + "\r\n" +
                        "Access-Control-Allow-Origin: *\r\n" +
                        "Connection: close\r\n" +
                        "\r\n" + jsonResponse;
                outputStream.write(response.getBytes(StandardCharsets.UTF_8));
            } else {
                serveBadRequest(outputStream);
            }
        } catch (Exception e) {
            serveBadRequest(outputStream);
        }
    }

    private void serveBadRequest(OutputStream outputStream) throws IOException {
        String jsonResponse = "{\"success\": false, \"message\": \"Bad request\"}";
        String response = "HTTP/1.1 400 Bad Request\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Content-Length: " + jsonResponse.getBytes(StandardCharsets.UTF_8).length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n" + jsonResponse;
        outputStream.write(response.getBytes(StandardCharsets.UTF_8));
    }

    private Map<Long, Integer> getVoteCounts() {
        Map<Long, Integer> voteCounts = new ConcurrentHashMap<>();
        synchronized (clientVotes) {
            for (Long songId : clientVotes.values()) {
                voteCounts.put(songId, voteCounts.getOrDefault(songId, 0) + 1);
            }
        }
        return voteCounts;
    }

    public Map<Long, Integer> getVoteCountsForDisplay() {
        return getVoteCounts();
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
                 "        body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; display: flex; flex-direction: column; min-height: 100vh; }\n" +
                 "        .container { flex: 1; display: flex; flex-direction: column; justify-content: center; align-items: center; text-align: center; }\n" +
                 "        .song-container { background: white; border-radius: 8px; padding: 16px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); max-width: 800px; width: 100%; margin-bottom: 24px; }\n" +
                 "        .voting-container { background: white; border-radius: 8px; padding: 20px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); max-width: 800px; width: 100%; margin-bottom: 20px; }\n" +
                 "        .song-title { font-size: 2.5em; color: #333; margin-bottom: 10px; }\n" +
                 "        .song-author { font-size: 1.5em; color: #666; margin-bottom: 30px; }\n" +
                 "        .song-lyrics { font-size: 1.2em; line-height: 1.6; color: #333; white-space: pre-wrap; text-align: left; }\n" +
                 "        .waiting { font-size: 1em; color: #666; }\n" +
                 "        .status { position: fixed; top: 10px; right: 10px; padding: 10px; background: #4CAF50; color: white; border-radius: 4px; font-size: 0.9em; }\n" +
                 "        .voting-header { font-size: 1.3em; font-weight: bold; margin-bottom: 15px; color: #333; }\n" +
                 "        .song-list { display: grid; gap: 10px; margin-top: 15px; }\n" +
                 "        .song-item { display: grid; grid-template-columns: 1fr 40px; align-items: center;  padding: 12px; border: 1px solid #ddd; border-radius: 6px; background: #f9f9f9; cursor: pointer; transition: all 0.3s ease; }\n" +
                 "        .song-item:hover { background: #e3f2fd; border-color: #2196F3; }\n" +
                 "        .song-item.voted { background: #c8e6c9; border-color: #4CAF50; }\n" +
                 "        .song-info { text-align: left; flex-grow: 1; }\n" +
                 "        .song-name { font-weight: bold; color: #333; }\n" +
                 "        .song-author-vote { font-size: 0.9em; color: #666; }\n" +
                 "        .vote-info { display: flex; align-items: center; justify-content: center; align-self: center; height: 100%; gap: 8px; width: 40px; }\n" +
                 "        .vote-count { background: #2196F3; color: white; padding: 4px 8px; border-radius: 12px; font-size: 0.8em; min-width: 20px; text-align: center; }\n" +
                 "        .vote-button { background: #4CAF50; color: white; border: none; padding: 6px 12px; border-radius: 4px; cursor: pointer; font-size: 0.8em; }\n" +
                 "        .current-vote { font-style: italic; color: #4CAF50; font-size: 0.8em; }\n" +
                 "        .vote-disabled { color: #999; font-size: 0.8em; font-style: italic; }\n" +
                 "        @media (max-width: 600px) { .song-title { font-size: 2em; } .song-author { font-size: 1.2em; } .song-lyrics { font-size: 1em; } .song-container, .voting-container { padding: 20px; } .song-item { flex-direction: column; align-items: flex-start; gap: 8px; } .vote-info { align-self: flex-end; } }\n" +
                 "    </style>\n" +
                 "</head>\n" +
                 "<body>\n" +
                 "    <div class=\"status\" id=\"status\">Connected</div>\n" +
                 "    <div class=\"container\">\n" +
                 "        <div class=\"song-container\" id=\"songContainer\">\n" +
                 "            <div class=\"waiting\" id=\"waitingMessage\">Waiting for the performer to select a song...</div>\n" +
                 "            <div id=\"songContent\" style=\"display: none;\">\n" +
                 "                <h1 class=\"song-title\" id=\"songTitle\"></h1>\n" +
                 "                <h2 class=\"song-author\" id=\"songAuthor\"></h2>\n" +
                 "                <div class=\"song-lyrics\" id=\"songLyrics\"></div>\n" +
                 "            </div>\n" +
                 "        </div>\n" +
                 "        <div class=\"voting-container\" id=\"votingContainer\" style=\"display: none;\">\n" +
                 "            <div class=\"voting-header\">🗳️ Vote for Next Song</div>\n" +
                 "            <div id=\"votingStatus\">Loading songs...</div>\n" +
                 "            <div class=\"song-list\" id=\"songList\"></div>\n" +
                 "        </div>\n" +
                 "    </div>\n" +
                 "    <script>\n" +
                 "        const clientId = localStorage.getItem('clientId') ?? 'client_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);\n" +
                 "        localStorage.setItem('clientId', clientId); " + 
                 "        let currentSong = undefined, availableSongs = [], votingState = {}, votedSong = null, pollInterval, isVoting = false;\n" +
                 "        function updateDisplay(song) {\n" +
                 "            const waitingMessage = document.getElementById('waitingMessage');\n" +
                 "            const songContent = document.getElementById('songContent');\n" +
                 "            const votingContainer = document.getElementById('votingContainer');\n" +
                 "            if (song && song !== null) {\n" +
                 "                document.getElementById('songTitle').textContent = song.name;\n" +
                 "                document.getElementById('songAuthor').textContent = 'by ' + song.author;\n" +
                 "                document.getElementById('songLyrics').textContent = song.lyrics;\n" +
                 "                waitingMessage.style.display = 'none';\n" +
                 "                songContent.style.display = 'block';\n" +
                 "                votingContainer.style.display = 'none';\n" +
                 "            } else {\n" +
                 "                waitingMessage.style.display = 'block';\n" +
                 "                songContent.style.display = 'none';\n" +
                 "                votingContainer.style.display = 'block';\n" +
                 "                loadAvailableSongs(); loadVotingState();\n" +
                 "            }\n" +
                 "        }\n" +
                 "        function loadAvailableSongs() { fetch('/available-songs').then(r => r.json()).then(songs => { availableSongs = songs.sort((a, b) => a.author.localeCompare(b.author)); updateSongList(); }).catch(console.error); }\n" +
                 "        function loadVotingState() { fetch('/voting-state?clientId=' + clientId).then(r => r.json()).then(response => { votingState = response.voteCounts; setVotedSong(response.clientVote); updateSongList(); }).catch(console.error); }\n" +
                 "        function updateSongList() {\n" +
                 "            const songList = document.getElementById('songList');\n" +
                 "            const votingStatus = document.getElementById('votingStatus');\n" +
                 "            if (availableSongs.length === 0) { songList.innerHTML = '<div class=\"waiting\">No songs available</div>'; return; }\n" +
                 "            const totalVotes = Object.values(votingState).reduce((sum, count) => sum + count, 0);\n" +
                 "            votingStatus.textContent = totalVotes > 0 ? totalVotes + ' total votes' : 'Be the first to vote!';\n" +
                 "            const sortedSongs = [...availableSongs].sort((a, b) => { const aVotes = votingState[a.id] || 0; const bVotes = votingState[b.id] || 0; if (bVotes !== aVotes) return bVotes - aVotes; const isARussian = /[а-яё]/i.test(a.author); const isBRussian = /[а-яё]/i.test(b.author); if (isARussian !== isBRussian) return isARussian ? 1 : -1; return a.author.localeCompare(b.author); });\n" +
                 "            songList.innerHTML = sortedSongs.map(song => {\n" +
                 "                const voteCount = votingState[song.id] || 0;\n" +
                 "                const isMyVote = String(votedSong) === String(song.id);\n" +
                 "                const hasVoted = votedSong !== null;\n" +
                 "                return '<div class=\"song-item ' + (isMyVote ? 'voted' : '') + '\" ' + 'onclick=\"voteForSong(' + song.id + ')\"' + '><div class=\"song-info\"><div class=\"song-name\">' + song.name + '</div><div class=\"song-author-vote\">by ' + song.author + '</div>' + (isMyVote ? '<div class=\"current-vote\">✓ Your vote</div>' : '') + '</div><div class=\"vote-info\">' + (voteCount > 0 ? '<span class=\"vote-count\">' + voteCount + '</span>' : '') + '</div></div>';\n" +
                 "            }).join('');\n" +
                 "        }\n" +
                 "function setVotedSong(value) { votedSong = value; }" +
                 "        function voteForSong(songId) { if (isVoting) return; isVoting = true; fetch('/vote?clientId=' + clientId + '&songId=' + songId).then(r => r.json()).then(result => { if (result.success) { setVotedSong(songId); loadVotingState(); } else { console.log('Vote rejected:', result.message); } isVoting = false; }).catch(e => { console.error(e); isVoting = false; }); }\n" +
                 "        function pollCurrentSong() {\n" +
                 "            fetch('/current-song').then(r => r.json()).then(song => {\n" +
                 "                if (JSON.stringify(song) !== JSON.stringify(currentSong)) {\n" +
                 "                    const wasNoSong = (currentSong === null || currentSong === undefined); currentSong = song; updateDisplay(song);\n" +
                 "                    if (wasNoSong !== (song === null || song === undefined) && (song === null || song === undefined)) { setVotedSong(null); loadAvailableSongs(); loadVotingState(); }\n" +
                 "                }\n" +
                 "                document.getElementById('status').textContent = 'Connected';\n" +
                 "                document.getElementById('status').style.backgroundColor = '#4CAF50';\n" +
                 "            }).catch(error => {\n" +
                 "                document.getElementById('status').textContent = 'Disconnected';\n" +
                 "                document.getElementById('status').style.backgroundColor = '#f44336';\n" +
                 "            });\n" +
                 "        }\n" +
                 "        updateDisplay(null); pollCurrentSong(); pollInterval = setInterval(pollCurrentSong, 2000); setInterval(loadVotingState, 2000);\n" +
                 "        window.addEventListener('beforeunload', () => { if (pollInterval) clearInterval(pollInterval); });\n" +
                 "    </script>\n" +
                 "</body></html>";
    }
}