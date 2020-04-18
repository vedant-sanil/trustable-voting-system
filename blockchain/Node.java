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
import message.*;

public class Node {
    /** List of blockchain node ports */
    private ArrayList<Integer> ports;
    /** List of communication skeletons per port */
    private ArrayList<HttpServer> node_skeleton;
    /** Skeleton Registration */
    public HttpServer temp_skeleton;
    /** Check if skeletons have been started */
    private boolean skeleton_started = false;
    /** Node number*/
    private int node_num;
    /** Gson object which can parse json to an object. */
    protected Gson gson;
    /** Block chain. < ArrayList of blocks > */
    public List<Block> blockchain = new ArrayList<Block>();
    /** Genesis Block */
    public Map<String, String> test1 = Map.of(
            "public_key", "xxx",
            "user_name", "xxx"
            );
    public Block genesisBlock = new Block(0, test1,Long.parseLong("5415419034"),3413,"xxx","xxx");

    public Node(int NODENUM, String args) throws IOException{
        // For each port
        String[] port_list = args.split(",");
        this.node_num = NODENUM;
        this.blockchain.add(genesisBlock);
        ports = new ArrayList<Integer>();
        node_skeleton = new ArrayList<HttpServer>();

        for (String arg : port_list) {
            Integer port = Integer.parseInt(arg);
            this.ports.add(port);
        }
        System.out.println("Port on which it needs to be created - "+this.ports.get(NODENUM));
        temp_skeleton = HttpServer.create(new java.net.InetSocketAddress(this.ports.get(NODENUM)), 0);
        temp_skeleton.setExecutor(Executors.newCachedThreadPool());
        this.node_skeleton.add(temp_skeleton);
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
            this.node_api(skeleton);
            skeleton.start();
        }

        this.skeleton_started = true;
    }

    private void node_api(HttpServer skeleton) {
        this.getBlockChain(skeleton);
        this.addBlock(skeleton);
        this.broadcastBlock(skeleton);
    }

    private void getBlockChain(HttpServer skeleton) {
        skeleton.createContext("/getchain", (exchange ->
        {
            String jsonString = "";
            int returnCode = 0;
            if ("POST".equals(exchange.getRequestMethod())) {
                GetChainRequest getChainRequest = null;
                GetChainReply getChainReply = null;
                try {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                    getChainRequest = gson.fromJson(isr, GetChainRequest.class);
                    System.out.println("Coming into getchain: Value is");
                    System.out.println(getChainRequest.chain_id);
                    getChainReply = new GetChainReply(getChainRequest.chain_id, blockchain.size(), blockchain);
                    System.out.println("getChainReply: "+getChainReply);
                    jsonString = gson.toJson(getChainReply);
                    returnCode = 200;
                    System.out.println("JSON String: "+jsonString);
                } catch (Exception e) {
                    returnCode = 404;
                    jsonString="Request information is incorrect";
                }
            } else {
                jsonString = "The REST method should be POST for <getBlockChain>!\n";
                returnCode = 400;
            }
            this.generateResponseAndClose(exchange, jsonString, returnCode);
        }));
    }


    private void mineBlock(HttpServer skeleton) {
        skeleton.createContext("/mineblock", (exchange ->
        {
            String jsonString = "";
            int returnCode = 0;
            if ("POST".equals(exchange.getRequestMethod())) {
                GetChainRequest getChainRequest = null;
                GetChainReply getChainReply = null;
                try {

                } catch (Exception e) {
                    e.printStackTrace();
                    returnCode = 404;
                    jsonString="Request information is incorrect";
                }
            } else {
                jsonString = "The REST method should be POST for <mineBlock>!\n";
                returnCode = 400;
            }
            this.generateResponseAndClose(exchange, jsonString, returnCode);
        }));
    }



    private void addBlock(HttpServer skeleton) {
        skeleton.createContext("/addblock", (exchange ->
        {
            String jsonString = "";
            int returnCode = 0;
            if ("POST".equals(exchange.getRequestMethod())) {
                AddBlockRequest addBlockRequest = null;
                try {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                    addBlockRequest = gson.fromJson(isr, AddBlockRequest.class);
                    System.out.println("Coming into addblock: Chain ID is");
                    System.out.println(addBlockRequest.getChainId());
                    System.out.println("Coming into addblock: Block is");
                    System.out.println(addBlockRequest.getBlock().toString());
                    Map<String, Object> resmap = new HashMap<String, Object>();
                    this.synchronizeBlockchain(this.ports);
                    resmap.put("info", "");
                    BroadcastRequest reqToSend = new BroadcastRequest(addBlockRequest.getChainId(), "PRECOMMIT", addBlockRequest.getBlock());
                    HttpResponse<String> response = null;
                    try {
                        int vote_cnt = 0;
                        for (int i = 0; i < this.ports.size(); i++)
                        {
                            if (i != this.node_num) {
                                response = this.getResponse("/broadcastblock", this.ports.get(i), reqToSend);
                                if (response.statusCode() == 200)
                                {
                                    vote_cnt += 1;
                                }
                            }
                        }
                        System.out.println("Vote Count is "+ vote_cnt +" | Ports.length is "+this.ports.size());
                        if (vote_cnt > (this.ports.size() * 0.66))
                        {
                            reqToSend = new BroadcastRequest(addBlockRequest.getChainId(), "COMMIT", addBlockRequest.getBlock());
                            resmap.put("success", "true");
                            for (int i = 0; i < this.ports.size(); i++)
                            {
                                if (i != this.node_num) {
                                    response = this.getResponse("/broadcastblock", this.ports.get(i), reqToSend);
                                }
                            }
                            returnCode = 200;
                        } else {
                            returnCode = 409;
                            resmap.put("success", "false");
                        }
                        System.out.println("RESMAP at addBlock - "+resmap);
                        jsonString = gson.toJson(resmap);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    returnCode = 404;
                    jsonString="Request information is incorrect";
                }
            } else {
                jsonString = "The REST method should be POST for <addBlock>!\n";
                returnCode = 400;
            }
            this.generateResponseAndClose(exchange, jsonString, returnCode);
        }));
    }


    private void broadcastBlock(HttpServer skeleton) {
        skeleton.createContext("/broadcastblock", (exchange ->
        {
            String jsonString = "";
            int returnCode = 0;
            if ("POST".equals(exchange.getRequestMethod())) {
                BroadcastRequest getBroadcastRequest = null;
                try {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                    getBroadcastRequest = gson.fromJson(isr, BroadcastRequest.class);
                    System.out.println("Coming into broadcastblock: Chain ID is");
                    System.out.println(getBroadcastRequest.chain_id);
                    System.out.println("Coming into broadcastblock: Request Type is");
                    System.out.println(getBroadcastRequest.request_type);
                    System.out.println("Coming into broadcastblock: Block is");
                    System.out.println(getBroadcastRequest.block.toString());
                    Map<String, Object> resmap = new HashMap<String, Object>();
                    this.synchronizeBlockchain(this.ports);
                    resmap.put("info", "");
                    if (getBroadcastRequest.request_type.equals("PRECOMMIT"))
                    {
                        if (getBroadcastRequest.block.previous_hash.equals(blockchain.get(blockchain.size() - 1).hash))
                        {
                            String computed_hash = Block.computeHash(getBroadcastRequest.block);
                            if (computed_hash.equals(getBroadcastRequest.block.hash))
                            {
                                returnCode = 200;
                                resmap.put("success", "true");
                            } else {
                                returnCode = 409;
                                resmap.put("success", "false");
                            }
                        } else {
                            returnCode = 409;
                            resmap.put("success", "false");
                        }
                    } else {
                        blockchain.add(getBroadcastRequest.block);
                        resmap.put("success", "true");
                        returnCode = 200;
                    }
                    System.out.println("RESMAP at addBlock - "+resmap);
                    jsonString = gson.toJson(resmap);
                } catch (Exception e) {
                    e.printStackTrace();
                    returnCode = 404;
                    jsonString="Request information is incorrect";
                }
            } else {
                jsonString = "The REST method should be POST for <mineBlock>!\n";
                returnCode = 400;
            }
            this.generateResponseAndClose(exchange, jsonString, returnCode);
        }));
    }

    private void synchronizeBlockchain(List<Integer> ports) {
        System.out.println("++++++++++++++SYNC BLOCKCHAIN+++++++++++++++++");
        GetChainRequest reqToSend = new GetChainRequest(1);
        GetChainReply reply = null;
        HttpResponse<String> response = null;
        for (int i = 0; i < ports.size(); i++)
        {
            if (this.node_num != i)
            {
                System.out.println("Port Value: "+ports.get(i));
                try {
                    response = this.getResponse("/getchain", this.ports.get(i), reqToSend);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                reply = gson.fromJson(response.body(), reply.getClass());
                System.out.println("Reply is "+reply);
                if (reply.getBlocks().size() > blockchain.size())
                {
                    blockchain = reply.getBlocks();
                    System.out.println("Blockchain is being updated");
                }
                System.out.println("Blockchain value is "+this.blockchain);
            }
        }
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        Random rand = new Random();
        File file = new File("./Blockchain" + args[0] + ".output");
        PrintStream stream = new PrintStream(file);
        System.setOut(stream);
        System.setErr(stream);
        System.out.println("PORTS ARE: "+args[0]+" "+args[1]);
        System.out.println("++++++++++++++++++++++++++++++++");
        Node n = new Node(Integer.parseInt(args[0]), args[1]);
        n.start();
    }

    /** Function to generate reponse */
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


    /**
     * call this function when you want to write to response and close the connection.
     */
    private void generateResponseAndClose(HttpExchange exchange, String respText, int returnCode) throws IOException {
        exchange.sendResponseHeaders(returnCode, respText.getBytes().length);
        OutputStream output = exchange.getResponseBody();
        output.write(respText.getBytes());
        output.flush();
        exchange.close();
        System.out.println("++++++++++++++++++++++++++++++++");
    }
}
