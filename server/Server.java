package server;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
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
//import java.security.rsa.RSAPrivateCrtKeyImpl;
import java.security.*;
import message.*;

public class Server {
    /** User name of current node */
    private String user_name;
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
    /** Data creation for registration */
    public Map<String, String> data = new LinkedHashMap<String, String>();

    public Server(int server_port, int node_port) throws IOException, NoSuchAlgorithmException, InterruptedException {
        this.server_port = server_port;
        this.node_port = node_port;

        // Assign a user name to this node
        user_name = "Server_"+this.server_port;

        this.gson = new Gson();

        // Register votes
        this.register();

        // Create the server and node communication servers
        this.server_skeleton = HttpServer.create(new InetSocketAddress(this.server_port), 0);
        this.server_skeleton.setExecutor(Executors.newCachedThreadPool());

        //this.node_skeleton = HttpServer.create(new InetSocketAddress(this.node_port), 0);
        //this.node_skeleton.setExecutor(Executors.newCachedThreadPool());
    }

    /** Start the voting server */
    void start() {
        this.startSkeletons();
    }

    /** Stop the voting server */
    void stop() {
        this.server_skeleton.stop(0);
        //this.node_skeleton.stop(0);
    }

    /** Method to start skeletons */
    private void startSkeletons() {
        if (this.skeleton_started) return;

        this.server_api();
        this.server_skeleton.start();

        //this.server_node_api();
        //this.node_skeleton.start();

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

    /**
     * Mines a new block and adds to the blockchain
     */
    private void register() throws NoSuchAlgorithmException, IOException, InterruptedException{
        // Add data to identitiy chain
        int chain_id = 1;

        // Generate public-private key pair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair keys = kpg.generateKeyPair();
        String publicKey = new String(keys.getPublic().getEncoded());
        String privateKey = new String(keys.getPrivate().getEncoded());

        // Create data block
        this.data.put("public_key", publicKey);
        this.data.put("user_name", privateKey);

        // Mine a new block
        MineBlockRequest request = new MineBlockRequest(chain_id, this.data);
        HttpResponse<String> response = this.getResponse("/mineblock", this.node_port, request);
        BlockReply mined_reply = gson.fromJson(response.body(), BlockReply.class);

        // Add mined block to block chain
        if (mined_reply.getChainId() == 1) {
            Block new_block = mined_reply.getBlock();
            AddBlockRequest request1 = new AddBlockRequest(chain_id, new_block);
            response = this.getResponse("/addblock", this.node_port, request1);
            StatusReply status = gson.fromJson(response.body(), StatusReply.class);
            if (!status.getSuccess()) {
                System.out.println("Block could not be added!");
                return;
            }
        } else {
            System.out.println("Incorrect chain!");
            return;
        }
    }

    /**
     * Main function to kickstart the Server
     * @param command line args
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void main(String[] args) throws FileNotFoundException, IOException, NoSuchAlgorithmException, InterruptedException {
        File file = new File("./Serverfile.output");
        PrintStream stream = new PrintStream(file);
        System.setOut(stream);
        System.setErr(stream);

        // Begin a server by providing server port and a single node port
        Server s = new Server(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        s.start();
    }

    /**
     *
     * @param specific method to establish communication for
     * @param communication port
     * @param request Object
     * @return null
     * @throws IOException
     * @throws InterruptedException
     */
    private HttpResponse<String> getResponse(String method,
                                             int port,
                                             Object requestObj) throws IOException, InterruptedException {

        HttpResponse<String> response;
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + method))
                .setHeader("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestObj)))
                .build();
        System.out.println("HTTP Request is "+request);
        try {
            response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            return response;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}