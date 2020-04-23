package client;

import java.net.InetSocketAddress;
import java.util.*;
import java.lang.*;
import java.io.*;
import com.google.gson.Gson;
import java.util.concurrent.Executors;
import java.net.URI;
import java.net.http.HttpClient;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import message.*;

public class Client {
    /** Port number of blockchain node*/
    private int node_port;
    /** Port number of Client*/
    private int client_port;
    /** Port number of Server*/
    private int server_port;
    /** HttpServer skeleton that perfroms the requisite communication*/
    private HttpServer client_skeleton;
    /** HttpServer skeleton that perfroms the requisite communication*/
    private HttpServer node_skeleton;
    /** Check if skeleton has started */
    private boolean skeleton_started = false;
    /** Gson object which can parse json to an object */
    protected Gson gson;

    public Client(int client_port, int server_port, int node_port) throws IOException{
        this.client_port = client_port;
        this.node_port = node_port;
        this.server_port = server_port;

        // Create the server and node communication servers
        this.client_skeleton = HttpServer.create(new InetSocketAddress(this.client_port), 0);
        this.client_skeleton.setExecutor(Executors.newCachedThreadPool());

        this.node_skeleton = HttpServer.create(new InetSocketAddress(this.node_port), 0);
        this.node_skeleton.setExecutor(Executors.newCachedThreadPool());

        this.gson = new Gson();
    }

    /** Start the voting server */
    void start() {
        this.startSkeletons();
    }

    /** Stop the voting server */
    void stop() {
        this.client_skeleton.stop(0);
        this.node_skeleton.stop(0);
    }

    /** Method to start skeletons */
    private void startSkeletons() {
        if (this.skeleton_started) return;

        this.client_api();
        this.client_skeleton.start();

        this.client_node_api();
        this.node_skeleton.start();

        this.skeleton_started = true;
    }

    /** Wrapper method over all the Server APIs */
    private void client_api() {
        return;
    }

    /** Wrapper method over all the Server-Blockchain Node APIs */
    private void client_node_api() {
        return;
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        Random rand = new Random();
        File file = new File("./Clientfile" + rand + ".output");
        PrintStream stream = new PrintStream(file);
        System.setOut(stream);
        System.setErr(stream);

        // Begin a server by providing server port and a single node port
        Client c = new Client(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
        c.start();
    }
}