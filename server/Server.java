package server;

import java.net.InetSocketAddress;
import java.util.*;
import java.lang.*;
import java.io.*;
import com.google.gson.Gson;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.net.URI;
import java.net.http.HttpClient;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import message.*;

public class Server {
    /** Port number of blockchain node*/
    private int node_port;
    /** Port number of Server node*/
    private int server_port;
    /** HttpServer skeleton that perfroms the requisite communication*/
    private HttpServer server_skeleton;
    /** HttpServer skeleton that perfroms the requisite communication*/
    private HttpServer node_skeleton;
    /** Check if skeleton has started */
    private boolean skeleton_started = false;
    /** Gson object which can parse json to an object */
    protected Gson gson;

    public Server(int server_port, int node_port) throws IOException{
        this.server_port = server_port;
        this.node_port = node_port;

        // Create the server and node communication servers
        this.server_skeleton = HttpServer.create(new InetSocketAddress(this.server_port), 0);
        this.server_skeleton.setExecutor(Executors.newCachedThreadPool());

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
        this.server_skeleton.stop(0);
        this.node_skeleton.stop(0);
    }

    /** Method to start skeletons */
    private void startSkeletons() {
        if (this.skeleton_started) return;

        this.server_api();
        this.server_skeleton.start();

        this.server_node_api();
        this.node_skeleton.start();

        this.skeleton_started = true;
    }

    /** Wrapper method over all the Server APIs */
    private void server_api() {
        return;
    }

    /** Wrapper method over all the Server-Node APIs */
    private void server_node_api() {
        return;
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        File file = new File("./Serverfile.output");
        PrintStream stream = new PrintStream(file);
        System.setOut(stream);
        System.setErr(stream);

        // Begin a server by providing server port and a single node port
        Server s = new Server(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        s.start();
    }
}