/**
 *
 *      File Name -     MessageHandler.java
 *      Created By -    14736 Spring 2020 TAs
 *      Brief -
 *
 *          A message handler for handling HTTP requests with JSON data
 *
 *
 *
 *      Example usage:
 *
 *      class MyHandler extends MessageHandler
 *      {
 *          public void handle(HttpExchange ex) throws IOException
 *          {
 *              // override the method
 *              RequestT request = this.getRequestBody(ex, RequestT.class);
 *              ...
 *              ReplyT reply = new ReplyT(...);
 *              this.sendResponse(ex, retcode, reply);
 *          }
 *      }
 *
 *      import com.sun.net.httpserver.HttpServer;
 *
 *      // start a http server and handles requests at an uri
 *      HttpServer server = HttpServer.create(...);
 *      server.createContext(uri, new MyHandler());
 *      server.start();
 */

package lib;

import java.lang.Class;
import java.lang.reflect.Type;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStreamReader;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import message.*;

public abstract class MessageHandler implements HttpHandler {
    public static Gson gson = new Gson();

    /**
     * Parse the request body of JSON format
     * @param ex    Http exchange object
     * @param classOfT  Request body object class
     * @param <T>   Request body object type
     * @return  Request body object, specifying JSON data received
     * @throws IOException
     */
    public <T> T getRequestBody(HttpExchange ex, Class<T> classOfT)
            throws IOException {
        InputStreamReader isr = new InputStreamReader(ex.getRequestBody(), "utf-8");
        T t = gson.fromJson(isr, classOfT);
        isr.close();
        return t;
    }

    /**
     * Parse the request body of primitive type data
     * @param ex    Http exchange object
     * @param typeOfT   Request body primitive data type
     * @param <T>   Request body primitive data type
     * @return  Request body data
     * @throws IOException
     */
    public <T> T getRequestBody(HttpExchange ex, Type typeOfT)
            throws IOException {
        InputStreamReader isr = new InputStreamReader(ex.getRequestBody(), "utf-8");
        T t = gson.fromJson(isr, typeOfT);
        isr.close();
        return t;
    }

    /**
     * Send response to client with response body in text
     * @param httpExchange  Http exchange object
     * @param retcode   Return code
     * @param resp  Response body
     * @throws IOException
     */
    public void sendResponse(HttpExchange httpExchange, int retcode, String resp)
            throws IOException {
        httpExchange.sendResponseHeaders(retcode, resp.getBytes().length);
        OutputStream output = httpExchange.getResponseBody();
        output.write(resp.getBytes());
        output.flush();
        httpExchange.close();
    }

    /**
     * Send response to client with response body in JSON format
     * @param httpExchange  Http exchange object
     * @param retcode   Return code
     * @param respObj   Response body
     * @param <T>   Response body object, specifying JSON data
     * @throws IOException
     */
    public <T> void sendResponse(HttpExchange httpExchange, int retcode, T respObj)
            throws IOException {
        sendResponse(httpExchange, retcode, gson.toJson(respObj));
    }
}
