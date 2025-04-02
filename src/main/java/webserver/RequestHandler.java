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
            log.log(Level.INFO, "Request Line: " + requestLine);
            // 요청 라인에서 URI 파싱
            String[] tokens = requestLine.split(" ");
            String method = tokens[0];
            String uri = tokens[1];
            log.log(Level.INFO, "Method: " + method + ", URI: " + uri);

            if (uri.equals("/user/form.html")) {
                handleFormRequest(dos);
            } else if (uri.equals("/user/login.html")) {
                handleLoginRequest(dos);
            } else if (uri.startsWith("/user/login")) {
                if (method.equals("POST")) {
                    // POST 요청 처리
                    handleUserLoginPost(br, dos);
                }
            } else if (uri.startsWith("/user/signup")) {
                if (method.equals("POST")) {
                    // POST 요청 처리
                    handleUserCreatePost(br, dos);
                } else {
                    // GET 요청 처리
                    handleUserCreateGet(uri, dos);
                }
            } else if (uri.equals("/user/userList")) {
                handleUserListRequest(br, dos);
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
                    if (uri.endsWith(".css")){
                        response200Header(dos, body.length, "text/css");
                    } else {
                        response200Header(dos, body.length,"text/html");
                    }
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

    private void handleUserListRequest(BufferedReader br, DataOutputStream dos) throws IOException {
        // 헤더에서 쿠키 값 확인
        String line;
        boolean isLogined = false;
        while (!(line = br.readLine()).isEmpty()){
            if (line.startsWith("Cookie")){
                String[] cookies = line.split(": ")[1].split("; ");
                for (String cookie: cookies){
                    if (cookie.equals("logined=true")){
                        isLogined = true;
                        break;
                    }
                }
            }
        }
        if (isLogined){
            String filePath = "./webapp/user/userList.html";
            File file = new File(filePath);
            if (file.exists()){
                byte[] body = Files.readAllBytes(file.toPath());
                response200Header(dos, body.length,"text/html");
                responseBody(dos,body);
            } else {
                response404Header(dos);
            }
        } else {
            response302Header(dos, "/user/login.html");
        }
    }

    private void handleUserLoginPost(BufferedReader br, DataOutputStream dos) throws IOException {
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
        log.log(Level.INFO, "Request Body: " + body);
        Map<String, String> params = HttpRequestUtils.parseQueryParameter(body);

        String userId = params.get("userId");
        String password = params.get("password");
        log.log(Level.INFO, "Login Attempt: userId=" + userId + ", password=" + password);

        User user = MemoryUserRepository.getInstance().findUserById(userId);
        if (user != null && user.getPassword().equals(password)) {
            log.log(Level.INFO, "Login Success: userId=" + userId);
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location: /index.html \r\n");
            dos.writeBytes("Set-Cookie: logined=true; Path=/ \r\n");
            dos.writeBytes("\r\n");
        } else {
            log.log(Level.INFO, "Login Failed: userId=" + userId);
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location: /user/login.html \r\n");
            dos.writeBytes("Set-Cookie: logined=false; Path=/ \r\n");
            dos.writeBytes("\r\n");
        }
    }

    private void handleLoginRequest(DataOutputStream dos) throws IOException {
        String filePath = "./webapp/user/login.html";
        File file = new File(filePath);

        if (file.exists()) {
            byte[] body = Files.readAllBytes(file.toPath());
            response200Header(dos, body.length, "text/html");
            responseBody(dos, body);
        } else {
            response404Header(dos);
        }
    }

    private void handleUserCreateGet(String uri, DataOutputStream dos) throws IOException {
        String queryString = uri.split("\\?")[1];
        Map<String, String> params = HttpRequestUtils.parseQueryParameter(queryString);

        String userId = params.get("userId");
        String password = params.get("password");
        String name = params.get("name");
        String email = params.get("email");
        log.log(Level.INFO, "Signup Attempt (GET): userId=" + userId + ", password=" + password + ", name=" + name + ", email=" + email);
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
        log.log(Level.INFO, "Request Body: " + body);
        Map<String, String> params = HttpRequestUtils.parseQueryParameter(body);

        String userId = params.get("userId");
        String password = params.get("password");
        String name = params.get("name");
        String email = params.get("email");

        log.log(Level.INFO, "Signup Attempt (POST): userId=" + userId + ", password=" + password + ", name=" + name + ", email=" + email);
        User user = new User(userId, password, name, email);
        MemoryUserRepository.getInstance().addUser(user);

        response302Header(dos, "/index.html");
    }

    private void handleFormRequest(DataOutputStream dos) throws IOException {
        String filePath = "./webapp/user/form.html";
        File file = new File(filePath);

        if (file.exists()) {
            byte[] body = Files.readAllBytes(file.toPath());
            response200Header(dos, body.length,"text/html");
            responseBody(dos, body);
        } else {
            response404Header(dos);
        }
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent, String contentType) throws IOException {
        dos.writeBytes("HTTP/1.1 200 OK \r\n");
        dos.writeBytes("Content-Type: "+ contentType + ";charset=utf-8\r\n");
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