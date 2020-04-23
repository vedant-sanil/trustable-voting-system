/**
 *
 *      File Name -     MessageHandler.java
 *      Created By -    14736 Spring 2020 TAs
 *      Brief -
 *
 *          A wrapper class to perform HTTP post requests
 *          and parse replies of JSON formats
 *
 *
 *      Example usage:
 *
 *      int myTimeout = 10;
 *      MessageSender sender = new MessageSender(myTimeout);
 *
 *      // Request body is of JSON formats
 *      RequestT requestObject = new RequestT(...);
 *      ReplyT reply = sender.post(uri, requestObject, ReplyT.class);
 *
 *      // Request body is plain text
 *      String requestText = "text";
 *      ReplyT reply = sender.post(uri, requestText, ReplyT.class);
 */

package lib;

import java.lang.Class;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.google.gson.Gson;

import message.*;

public class MessageSender {
    private static Gson gson = new Gson();

    private int connection_timeout = 10;

    /**
     * Default constructor
     */
    public MessageSender() {}

    /**
     * Constructor with a specified timeout
     *
     * @param timeout in seconds
     */
    public MessageSender(int timeout)
    {
        connection_timeout = timeout;
    }

    /**
     * Sends post requests with JSON data and parse replies with JSON data
     *
     * @param uri
     * @param requestMsg    Request body object, specifying JSON data sent
     * @param classOfReplyT Response body object class
     * @param <RequestT>    Request body object type
     * @param <ReplyT>  Response body object type
     * @return  Response body object, specifying JSON data received
     * @throws Exception
     */
    public <RequestT, ReplyT>
    ReplyT post(String uri, RequestT requestMsg, Class<ReplyT> classOfReplyT)
            throws Exception
    {
        String requestBody = gson.toJson(requestMsg);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(connection_timeout))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request,
                HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();
        return gson.fromJson(responseBody, classOfReplyT);
    }

    /**
     * Sends post requests with text data and parse replies with JSON data
     *
     * @param uri
     * @param requestBody   Request body text
     * @param classOfReplyT Response body object class
     * @param <ReplyT> Response body object type
     * @return  Response body object, specifying JSON data received
     * @throws Exception
     */
    public <ReplyT>
    ReplyT post(String uri, String requestBody, Class<ReplyT> classOfReplyT)
            throws Exception
    {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Content-Type", "text/plain")
                .timeout(Duration.ofSeconds(connection_timeout))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request,
                HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();
        return gson.fromJson(responseBody, classOfReplyT);
    }
}
