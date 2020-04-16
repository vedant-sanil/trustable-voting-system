package blockchain;

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

public class Node {
    /** List of blockchain node ports */
    private ArrayList<Integer> ports;
    /** List of communication skeletons per port */
    private ArrayList<HttpServer> node_skeleton;
    /** Check if skeletons have been started */
    private boolean skeleton_started = false;
    /** Node number*/
    private int node_num;
    /** Gson object which can parse json to an object. */
    protected Gson gson;

    public Node(int NODENUM, String args) throws IOException{
        // For each port
        String[] port_list = args.split(",");
        this.node_num = NODENUM;
        ports = new ArrayList<Integer>();
        node_skeleton = new ArrayList<HttpServer>();

        for (String arg : port_list) {
            Integer port = Integer.parseInt(arg);
            this.ports.add(port);
            HttpServer temp_skeleton = HttpServer.create(new java.net.InetSocketAddress(port), 0);
            temp_skeleton.setExecutor(Executors.newCachedThreadPool());
            this.node_skeleton.add(temp_skeleton);
        }

        this.gson = new Gson();
    }

    /** Starts the blockchain servers*/
    void start() {
        this.startSkeletons();
    }

    /** Stops the naming servers*/
    void stop() {
        for (HttpServer skeleton : this.node_skeleton) {
            skeleton.stop(0);
        }
    }

    /** Function to start skeletons */
    private void startSkeletons() {
        if (this.skeleton_started) return;

        for (HttpServer skeleton : this.node_skeleton) {
            skeleton.start();
        }

        this.skeleton_started = true;
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        Random rand = new Random();
        File file = new File("./Blockchain" + rand.nextInt() + ".output");
        PrintStream stream = new PrintStream(file);
        System.setOut(stream);
        System.setErr(stream);

        System.out.print(args.length);

        Node n = new Node(Integer.parseInt(args[0]), args[1]);
        n.start();
    }
}