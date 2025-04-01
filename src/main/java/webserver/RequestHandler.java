package webserver;

import db.MemoryUserRepository;
import http.util.HttpRequestUtils;
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
            String uri = tokens[1];

            if (uri.equals("/user/form.html")) {
                handleFormRequest(dos);
            } else if (uri.startsWith("/user/signup")) {
                handleUserCreate(uri, dos);
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

    private void handleUserCreate(String uri, DataOutputStream dos) throws IOException {
        // 쿼리스트링 파싱
        String queryString = uri.split("\\?")[1];
        Map<String, String> params = HttpRequestUtils.parseQueryParameter(queryString);

        // User 객체 생성 및 저장
        String userId = params.get("userId");
        String password = params.get("password");
        String name = params.get("name");
        String email = params.get("email");

        User user = new User(userId, password, name, email);
        MemoryUserRepository.getInstance().addUser(user);

        // 302 리다이렉트 응답
        response302Header(dos, "/index.html");
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