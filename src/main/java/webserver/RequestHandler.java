package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RequestHandler implements Runnable {
    private static final Logger log = Logger.getLogger(RequestHandler.class.getName());
    private Socket connection;
    private Map<String, Controller> controllers = new HashMap<>();

    public RequestHandler(Socket connection) {
        this.connection = connection;
        controllers.put("/user", new UserController());
    }

    @Override
    public void run() {
        log.log(Level.INFO, "New Client Connect! Connected IP : " + connection.getInetAddress() + ", Port : " + connection.getPort());
        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            DataOutputStream dos = new DataOutputStream(out);

            Request request = new Request(br);
            Response response = new Response(dos);

            String uri = request.getUri();
            if (uri.equals("/")) {
                uri = "/index.html";
            }

            String filePath = "./webapp" + uri;
            File file = new File(filePath);

            if (file.exists()) {
                byte[] body = Files.readAllBytes(file.toPath());
                if (uri.endsWith(".css")) {
                    response.send200("text/css", body);
                } else {
                    response.send200("text/html", body);
                }
            } else {
                response.send404();
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage());
        }
    }
}