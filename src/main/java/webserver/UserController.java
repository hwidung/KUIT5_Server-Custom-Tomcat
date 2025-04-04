package webserver;

import db.MemoryUserRepository;
import http.util.HttpRequestUtils;
import model.User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class UserController implements Controller {

    @Override
    public void handle(Request request, Response response) throws IOException {
        String uri = request.getUri();
        if (uri.equals("/user/form.html")) {
            byte[] body = Files.readAllBytes(Paths.get("./webapp/user/form.html"));
            response.send200("text/html", body);
        } else if (uri.equals("/user/login.html")) {
            byte[] body = Files.readAllBytes(Paths.get("./webapp/user/login.html"));
            response.send200("text/html", body);
        } else if (uri.startsWith("/user/login") && request.getMethod().equals("POST")) {
            handleLogin(request, response);
        } else if (uri.startsWith("/user/signup") && request.getMethod().equals("POST")) {
            handleSignup(request, response);
        } else if (uri.equals("/user/userList")) {
            handleUserList(request, response);
        } else {
            response.send404();
        }
    }

    private void handleLogin(Request request, Response response) throws IOException {
        Map<String, String> params = HttpRequestUtils.parseQueryParameter(request.getBody());
        String userId = params.get("userId");
        String password = params.get("password");

        User user = MemoryUserRepository.getInstance().findUserById(userId);
        if (user != null && user.getPassword().equals(password)) {
            response.send302("/index.html");
        } else {
            response.send302("/user/login.html");
        }
    }

    private void handleSignup(Request request, Response response) throws IOException {
        Map<String, String> params = HttpRequestUtils.parseQueryParameter(request.getBody());
        String userId = params.get("userId");
        String password = params.get("password");
        String name = params.get("name");
        String email = params.get("email");

        User user = new User(userId, password, name, email);
        MemoryUserRepository.getInstance().addUser(user);

        response.send302("/index.html");
    }

    private void handleUserList(Request request, Response response) throws IOException {
        // 유저 리스트 처리 로직
    }
}