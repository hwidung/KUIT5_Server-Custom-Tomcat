package webserver;

import db.MemoryUserRepository;
import http.util.HttpRequestUtils;
import http.util.IOUtils;
import model.User;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RequestHandler implements Runnable {
    private static final Logger log = Logger.getLogger(RequestHandler.class.getName());
    private Socket connection;

    public RequestHandler(Socket connection) {
        this.connection = connection;
    }

    @Override
    public void run() {
        log.log(Level.INFO, "New Client Connect! Connected IP : " + connection.getInetAddress() + ", Port : " + connection.getPort());
        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            DataOutputStream dos = new DataOutputStream(out);

            // HTTP 요청 메시지 읽기
            String requestLine = br.readLine();
            if (requestLine == null) {
                return;
            }

            // 요청 라인에서 URI 파싱
            String[] tokens = requestLine.split(" ");
            String method = tokens[0];
            String uri = tokens[1];

            if (uri.equals("/user/form.html")) {
                handleFormRequest(dos);
            } else if (uri.startsWith("/user/signup")) {
                if (method.equals("POST")) {
                    // POST 요청 처리
                    handleUserCreatePost(br, dos);
                } else {
                    // GET 요청 처리
                    handleUserCreateGet(uri, dos);
                }
            } else {
                // 기본 경로 설정
                if (uri.equals("/")) {
                    uri = "/index.html";
                }
                // 요청한 파일 경로 설정
                String filePath = "./webapp" + uri;
                File file = new File(filePath);

                if (file.exists()) {
                    byte[] body = Files.readAllBytes(file.toPath());
                    response200Header(dos, body.length);
                    responseBody(dos, body);
                } else {
                    // 404 Not Found 응답
                    response404Header(dos);
                }
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage());
        }
    }

    private void handleUserCreateGet(String uri, DataOutputStream dos) throws IOException {
        String queryString = uri.split("\\?")[1];
        Map<String, String> params = HttpRequestUtils.parseQueryParameter(queryString);

        String userId = params.get("userId");
        String password = params.get("password");
        String name = params.get("name");
        String email = params.get("email");

        User user = new User(userId, password, name, email);
        MemoryUserRepository.getInstance().addUser(user);

        response302Header(dos, "/index.html");
    }

    private void handleUserCreatePost(BufferedReader br, DataOutputStream dos) throws IOException {
        int contentLength = 0;
        while (true) {
            final String line = br.readLine();
            if (line.isEmpty()) {
                break;
            }
            if (line.startsWith("Content-Length")) {
                contentLength = Integer.parseInt(line.split(": ")[1]);
            }
        }

        String body = IOUtils.readData(br, contentLength);
        Map<String, String> params = HttpRequestUtils.parseQueryParameter(body);

        String userId = params.get("userId");
        String password = params.get("password");
        String name = params.get("name");
        String email = params.get("email");

        User user = new User(userId, password, name, email);
        MemoryUserRepository.getInstance().addUser(user);

        response302Header(dos, "/index.html");

    }

    private void handleFormRequest(DataOutputStream dos) throws IOException {
        String filePath = "./webapp/user/form.html";
        File file = new File(filePath);

        if (file.exists()) {
            byte[] body = Files.readAllBytes(file.toPath());
            response200Header(dos, body.length);
            responseBody(dos, body);
        } else {
            response404Header(dos);
        }
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) throws IOException {
        dos.writeBytes("HTTP/1.1 200 OK \r\n");
        dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
        dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
        dos.writeBytes("\r\n");
    }

    private void response302Header(DataOutputStream dos, String location) throws IOException {
        dos.writeBytes("HTTP/1.1 302 Found \r\n");
        dos.writeBytes("Location: " + location + "\r\n");
        dos.writeBytes("\r\n");
    }

    private void response404Header(DataOutputStream dos) throws IOException {
        dos.writeBytes("HTTP/1.1 404 Not Found \r\n");
        dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
        dos.writeBytes("\r\n");
        dos.writeBytes("<h1>404 Not Found</h1>");
    }

    private void responseBody(DataOutputStream dos, byte[] body) throws IOException {
        dos.write(body, 0, body.length);
        dos.flush();
    }
}