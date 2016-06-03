package com.ece6102;

import com.ece6102.tools.JSONError;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;


public final class Authentication {

    // CLIENT_ID
    private static final String CLIENT_ID = "45783031879-hpkot78o84l86umbh9u79i4ketfravej.apps.googleusercontent.com";

    //CLIENT_SECRET
    private static final String CLIENT_SECRET = "KWOZd1m1syKX1klLgIUHUlQr";


    //Callback URI that google will redirect to after successful authentication
    private static final String CALLBACK_URI = "http://localhost:8080/OAuth2v1/";

    // start google authentication constants
    private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v1/userinfo";
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    public static String getUserInfo(String accessToken) throws IOException {
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken(accessToken);
        Credential credential1 = new Credential(BearerToken.authorizationHeaderAccessMethod()).setFromTokenResponse(tokenResponse);
        final HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(credential1);
        //Make an authenticated request
        final GenericUrl url = new GenericUrl(USER_INFO_URL);
        final HttpRequest request = requestFactory.buildGetRequest(url);
        request.getHeaders().setContentType("application/json");

        String jsonIdentity;
        try {
            jsonIdentity = request.execute().parseAsString();
        } catch (Exception e) {
            jsonIdentity = "{'error': 'User not authenticated'}";
        }

        return jsonIdentity;

    }

    public static String authentication(String token) {
        Connection conn = null;
        PreparedStatement statement = null;
        String userID;
        try {
            // For debug
            if (token.equals("test"))
                return "test";

            String googleResponse = getUserInfo(token);
            JsonObject googleJSON = new JsonParser().parse(googleResponse).getAsJsonObject();

            // Test Google Response
            if (googleJSON.isJsonNull() || googleJSON.has("error"))
                return "";

            userID = googleJSON.get("id").getAsString();

            // Load context
            DataSource ds = null;
            Context initContext = null;
            initContext = new InitialContext();
            Context envContext  = (Context)initContext.lookup("java:/comp/env");
            ds = (DataSource)envContext.lookup("jdbc/managment_bdd");

            // Verify User
            conn = ds.getConnection();

            // Request on the table
            statement = conn.prepareStatement("SELECT ID FROM user WHERE ID = ?");
            statement.setString(1, userID);
            ResultSet rs = statement.executeQuery();

            if (!rs.next())
                return "";

        } catch (IOException e) {
            e.printStackTrace();
            return "";
        } catch (NamingException e) {
            e.printStackTrace();
            return "";
        } catch (SQLException e) {
            e.printStackTrace();
            return "";
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException ignored) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
        return userID;
    }



}

