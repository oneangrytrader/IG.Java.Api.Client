package ig.api.client.rest;

import ig.api.client.rest.request.AuthenticationRequest;
import ig.api.client.rest.response.AuthenticationResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class RestClient {

    private final String baseUri;
    private final String SESSION_URI = "/gateway/deal/session";    
    private final CloseableHttpClient closeableHttpClient;
    private AuthenticationResponse authenticationResponse;

    public RestClient(String environment) {
        baseUri = String.format("https://%sapi.ig.com", "live".equals(environment) ? "" : "demo-");
        closeableHttpClient = HttpClients.createDefault();
    }

    private boolean ShouldAuthenticate(String authResponsePath) throws IOException {
        Path filePath = Path.of(authResponsePath);
        if (!Files.exists(filePath)) {
            return true;
        } else {
            String fileContent = Files.readString(filePath);
            AuthenticationResponse authResponse = AuthenticationResponse.fromJsonString(fileContent);
            authenticationResponse = authResponse;
            int hours = GetElapsedHours(authResponse.getDate(), new Date(System.currentTimeMillis()));
            return hours > 5;
        }
    }

    private static int GetElapsedHours(Date start, Date end) {
        final int MILLI_TO_HOUR = 1000 * 60 * 60;
        return Math.abs((int) (end.getTime() - start.getTime()) / MILLI_TO_HOUR);
    }

    public AuthenticationResponse Authenticate(String username, String password, String apiKey, String authResponsePath) throws IOException, InterruptedException {

        if (ShouldAuthenticate(authResponsePath)) {

            HttpPost httpPost = new HttpPost(baseUri + SESSION_URI);
            AuthenticationRequest request = new AuthenticationRequest();
            request.setIdentifier(username);
            request.setPassword(password);
            StringEntity entity = new StringEntity(AuthenticationRequest.toJsonString(request));
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setHeader("Version", "3");
            httpPost.setHeader("X-IG-API-KEY", apiKey);
            try (CloseableHttpResponse response = closeableHttpClient.execute(httpPost)) {
                String responseString = new BasicResponseHandler().handleResponse(response);
                AuthenticationResponse authResponse = AuthenticationResponse.fromJsonString(responseString);
                authResponse.setDate(new Date(System.currentTimeMillis()));
                authenticationResponse = authResponse;
                Files.write(Path.of(authResponsePath), AuthenticationResponse.toJsonString(authResponse).getBytes());
            }
        }

        return authenticationResponse;
    }
    
    public void Close() throws IOException{
        closeableHttpClient.close();
    }
}
