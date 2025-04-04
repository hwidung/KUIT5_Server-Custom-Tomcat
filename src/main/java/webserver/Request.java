package webserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Request {
    private String method;
    private String uri;
    private Map<String, String> headers = new HashMap<>();
    private String body;

    public Request(BufferedReader br) throws IOException {
        String requestLine = br.readLine();
        if (requestLine != null) {
            String[] tokens = requestLine.split(" ");
            this.method = tokens[0];
            this.uri = tokens[1];

            String line;
            while (!(line = br.readLine()).isEmpty()) {
                String[] headerTokens = line.split(": ");
                headers.put(headerTokens[0], headerTokens[1]);
            }

            if (headers.containsKey("Content-Length")) {
                int contentLength = Integer.parseInt(headers.get("Content-Length"));
                char[] bodyChars = new char[contentLength];
                br.read(bodyChars, 0, contentLength);
                this.body = new String(bodyChars);
            }
        }
    }

    public String getMethod() {
        return method;
    }

    public String getUri() {
        return uri;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }
}