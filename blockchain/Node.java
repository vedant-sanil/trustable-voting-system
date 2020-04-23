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
    public List<Block> votechain = new ArrayList<Block>();
    /** Genesis Block */
    public Map<String, String> test1 = new LinkedHashMap<String,String>();
    public Map<String, String> test2 = new LinkedHashMap<String,String>();



    public long nonce;
    public Block genesisBlockTest, genesisBlockVoteTest, genesisBlock, genesisBlockVote;

    /** Flag to check if node is asleep or not */
    public boolean isSleep = false;

    public Node(int NODENUM, String args) throws IOException{
        this.test1.put("public_key", "xxx");
        this.test1.put("user_name", "xxx");
        this.test2.put("vote", "xxx");
        this.test2.put("voter_credential", "xxx");
        this.nonce = 0;
        this.genesisBlockTest = new Block(0, test1,Long.parseLong("5415419034"),nonce,"xxx","xxx");
        this.genesisBlockVoteTest = new Block(0, test2,Long.parseLong("5415419034"),nonce,"xxx","xxx");
        String hash = Block.computeHash(genesisBlockTest);
        String hashVote = Block.computeHash(genesisBlockVoteTest);
        this.genesisBlock = new Block(0, test1,Long.parseLong("5415419034"),nonce,"xxx",hash);
        this.genesisBlockVote = new Block(0, test2,Long.parseLong("5415419034"),nonce,"xxx",hashVote);


        // For each port
        String[] port_list = args.split(",");
        this.node_num = NODENUM;
        this.blockchain.add(genesisBlock);
        this.votechain.add(genesisBlockVote);
        System.out.println("------------------------"+genesisBlock.getHash());
        ports = new ArrayList<Integer>();
        node_skeleton = new ArrayList<HttpServer>();

        for (String arg : port_list) {
            Integer port = Integer.parseInt(arg);
            this.ports.add(port);
        }
        System.out.println("List of ports are "+this.ports);
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
        this.mineBlock(skeleton);
        this.addBlock(skeleton);
        this.broadcastBlock(skeleton);
        this.sleepChain(skeleton);
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
                    if (getChainRequest.getChainId() == 1)
                    {
                        getChainReply = new GetChainReply(getChainRequest.getChainId(), this.blockchain.size(), this.blockchain);
                        System.out.println("getChainReply Size of Blockchain : "+this.blockchain.size());
                        System.out.println("getChainReply Blockchain : "+this.blockchain);
                    }
                    else
                    {
                        getChainReply = new GetChainReply(getChainRequest.getChainId(), this.votechain.size(), this.votechain);
                        System.out.println("getChainReply Size of Votechain : "+this.votechain.size());
                        System.out.println("getChainReply Blockchain : "+this.votechain);
                    }
                    jsonString = gson.toJson(getChainReply);
                    returnCode = 200;
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


    private void sleepChain(HttpServer skeleton) {
        skeleton.createContext("/sleep", (exchange ->
        {
            String jsonString = "";
            int returnCode = 0;
            SleepRequest sleepRequest = null;
            if ("POST".equals(exchange.getRequestMethod()) && this.isSleep == false) {
                try {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                    sleepRequest = gson.fromJson(isr, SleepRequest.class);
                    System.out.println("Coming into sleepchain: Value is");
                    System.out.println(sleepRequest.getTimeout());
                    Map<String, Object> resmap = new HashMap<String, Object>();
                    resmap.put("success", "true");
                    resmap.put("info", "");
                    jsonString = gson.toJson(resmap);
                    returnCode = 200;
//                    System.out.println("JSON String: " + jsonString);
                } catch (Exception e) {
                    returnCode = 404;
                    jsonString = "Request information is incorrect";
                }
            } else {
                jsonString = "The REST method should be POST for <getBlockChain>!\n";
                returnCode = 400;
            }
            this.generateResponseAndClose(exchange, jsonString, returnCode);
            synchronized (this) {
                this.isSleep = true;
            }
            try {
                Thread.sleep(sleepRequest.getTimeout()*1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            synchronized (this) {
                this.isSleep = false;
            }
        }));
    }

    private void mineBlock(HttpServer skeleton) {
        skeleton.createContext("/mineblock", (exchange ->
        {
            System.out.println("============MINEBLOCK============");
            String jsonString = "";
            int returnCode = 0;
            if ("POST".equals(exchange.getRequestMethod())) {
                MineBlockRequest mineBlockRequest = null;
                BlockReply blockReply = null;
                try {
                    Block init_block = null;
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                    mineBlockRequest = gson.fromJson(isr, MineBlockRequest.class);
                    // Synchronize the chain before mining
                    this.synchronize(mineBlockRequest.getChainId());

                    // Get time, increment nonce and add init block
                    long curr_time = System.currentTimeMillis();
                    // Get current hash by mining through difficulty 5
                    String mined_hash = "xxx";
                    String prev_hash = "xxx";
                    if (mineBlockRequest.getChainId() == 1) {
                        prev_hash = this.blockchain.get(this.blockchain.size()-1).getHash();
                        while (!mined_hash.startsWith("00000")) {
                            nonce++;
                            // Create temporary new block
                            init_block = new Block(this.node_num, mineBlockRequest.getData(),
                                    curr_time, nonce, prev_hash, mined_hash);
                            mined_hash = Block.computeHash(init_block);
                        }
                        System.out.println("Mined Hash == "+mined_hash);
                    } else {
                        prev_hash = this.votechain.get(this.votechain.size()-1).getHash();
                        while (!mined_hash.startsWith("0")) {
                            nonce++;
                            // Create temporary new block
                            init_block = new Block(this.node_num, mineBlockRequest.getData(),
                                    curr_time, nonce, prev_hash, mined_hash);
                            mined_hash = Block.computeHash(init_block);
                        }
                        System.out.println("Mined Hash == "+mined_hash);
                    }
                    // Successfully create blockchain reply
                    blockReply = new BlockReply(mineBlockRequest.getChainId(), new Block(this.node_num, mineBlockRequest.getData(),
                            curr_time, nonce, prev_hash, mined_hash));
                    jsonString = gson.toJson(blockReply);
                    returnCode = 200;
                    System.out.println("Reply Block created with Chain ID: "+blockReply.getChainId());
                    System.out.println("Reply Block created with Block ID: "+blockReply.getBlock().getId());
                    System.out.println("Reply Block created with Hash: "+blockReply.getBlock().getHash());
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
            System.out.println("============MINEBLOCK============");
        }));
    }




    private void addBlock(HttpServer skeleton) {
        skeleton.createContext("/addblock", (exchange ->
        {
            String jsonString = "";
            int returnCode = 0;
            Map<String, Object> resmap = new HashMap<String, Object>();
            if ("POST".equals(exchange.getRequestMethod()) && this.isSleep == false) {
                AddBlockRequest addBlockRequest = null;
                try {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                    addBlockRequest = gson.fromJson(isr, AddBlockRequest.class);
                    System.out.println("Coming into addblock: Chain ID is");
                    System.out.println(addBlockRequest.getChainId());
                    System.out.println("Coming into addblock: Block is");
                    System.out.println(addBlockRequest.getBlock().toString());
                    this.synchronize(addBlockRequest.getChainId());
                    resmap.put("info", "");
                    BroadcastRequest reqToSend = new BroadcastRequest(addBlockRequest.getChainId(), "PRECOMMIT", addBlockRequest.getBlock());
                    HttpResponse<String> response = null;
                    try {
                        int vote_cnt = 1;
                        for (int i = 0; i < this.ports.size(); i++)
                        {
                            if (i != this.node_num) {
                                response = this.getResponse("/broadcast", this.ports.get(i), reqToSend);
                                System.out.println(i+" Node: Response Status code: "+response.statusCode());
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
                                    response = this.getResponse("/broadcast", this.ports.get(i), reqToSend);
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
                if (this.isSleep == true) {
                    returnCode = 409;
                    resmap.put("info", "");
                    resmap.put("success", "false");
                    jsonString = gson.toJson(resmap);
                } else {
                    jsonString = "The REST method should be POST for <addBlock>!\n";
                    returnCode = 400;
                }
            }
            this.generateResponseAndClose(exchange, jsonString, returnCode);
        }));
    }


    private void broadcastBlock(HttpServer skeleton) {
        skeleton.createContext("/broadcast", (exchange ->
        {
            String jsonString = "";
            int returnCode = 0;
            Map<String, Object> resmap = new HashMap<String, Object>();
            if ("POST".equals(exchange.getRequestMethod()) && this.isSleep == false) {
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
                    this.synchronize(getBroadcastRequest.getChainId());
                    resmap.put("info", "");
                    if (getBroadcastRequest.request_type.equals("PRECOMMIT"))
                    {
                        if (getBroadcastRequest.getChainId() == 1) {
                            System.out.println("BroadcastRequest Previous Hash - " + getBroadcastRequest.block.getPreviousHash() + " == Last Block's hash - " + blockchain.get(blockchain.size() - 1).getHash());
                            if (getBroadcastRequest.block.getPreviousHash().equals(blockchain.get(blockchain.size() - 1).getHash())) {
                                String computed_hash = Block.computeHash(getBroadcastRequest.block);
                                System.out.println("Computed Hash (" + computed_hash + ") == BroadcastRequest Block's hash (" + getBroadcastRequest.block.getHash() + ")");
                                if (computed_hash.equals(getBroadcastRequest.block.getHash())) {
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
                            System.out.println("BroadcastRequest Previous Hash - " + getBroadcastRequest.block.getPreviousHash() + " == Last Block's hash - " + votechain.get(votechain.size() - 1).getHash());
                            if (getBroadcastRequest.block.getPreviousHash().equals(votechain.get(votechain.size() - 1).getHash())) {
                                String computed_hash = Block.computeHash(getBroadcastRequest.block);
                                System.out.println("Computed Hash (" + computed_hash + ") == BroadcastRequest Block's hash (" + getBroadcastRequest.block.getHash() + ")");
                                if (computed_hash.equals(getBroadcastRequest.block.getHash())) {
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
                        }
                    } else {
                        if (getBroadcastRequest.getChainId() == 1 && !blockchain.get(blockchain.size() - 1).getHash().equals(getBroadcastRequest.block.getHash()))
                        {
                            blockchain.add(getBroadcastRequest.block);
                        } else if (getBroadcastRequest.getChainId() == 2 && !votechain.get(votechain.size() - 1).getHash().equals(getBroadcastRequest.block.getHash())){
                            votechain.add(getBroadcastRequest.block);
                        }
//                        this.synchronize(getBroadcastRequest.getChainId());
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
                if (this.isSleep == true) {
                    returnCode = 409;
                    resmap.put("info", "");
                    resmap.put("success", "false");
                    jsonString = gson.toJson(resmap);
                } else {
                    jsonString = "The REST method should be POST for <addBlock>!\n";
                    returnCode = 400;
                }
            }
            this.generateResponseAndClose(exchange, jsonString, returnCode);
        }));
    }

    private void synchronize(int chain_id) {
        System.out.println("++++++++++++++SYNC BLOCKCHAIN+++++++++++++++++");
        for (int port_num : this.ports) {
            if (port_num != this.ports.get(this.node_num)) {
                // Communicating with peer nodes
                try {
                    GetChainRequest request = new GetChainRequest(1);
                    HttpResponse<String> response = this.getResponse("/getchain", port_num, request);
                    GetChainReply message = gson.fromJson(response.body(), GetChainReply.class);
                    List<Block> other_chain = new ArrayList<Block>();

                    if (chain_id == 1) {
                        // Synchronizing Block Chain
                        System.out.println("SYNCHRONIZING BLOCKCHAIN");
                        if (this.blockchain.size() <= message.getChainLength()) {
                            if (this.blockchain.size() == message.getChainLength()) {
                                other_chain = message.getBlocks();
                                for (int i = 0; i < this.blockchain.size(); i++) {
                                    if (this.blockchain.get(i).getTimestamp() < other_chain.get(i).getTimestamp()) {
                                        for (int j = 0; j < message.getBlocks().size(); j++)
                                            this.blockchain.set(j, message.getBlocks().get(j));
                                        break;
                                    }
                                    System.out.println("Block " + i + " - " + blockchain.get(i).toString());
                                    System.out.println("Block " + i + " CurrentHash - " + blockchain.get(i).getHash());
                                }
                            } else {
                                System.out.println(port_num + " - Current blockchain not up-to-date, re-update");
                                System.out.println("");
                                this.blockchain = message.getBlocks();
                            }
                        }
                    } else {
                        // Synchronizing Vote Chain
                        request = new GetChainRequest(2);
                        response = this.getResponse("/getchain", port_num, request);
                        message = gson.fromJson(response.body(), GetChainReply.class);
                        other_chain = new ArrayList<Block>();
                        System.out.println("SYNCHRONIZING VOTECHAIN");
                        if (this.votechain.size() <= message.getChainLength()) {
                            if (this.votechain.size() == message.getChainLength())
                            {
                                other_chain = message.getBlocks();
                                for (int i = 0; i < this.votechain.size(); i++) {
                                    if (this.votechain.get(i).getTimestamp() < other_chain.get(i).getTimestamp())
                                    {
                                        for (int j = 0; j < message.getBlocks().size(); j++)
                                            this.votechain.set(j, message.getBlocks().get(j));
                                        break;
                                    }
                                    System.out.println("Block "+i+" - "+ votechain.get(i).toString());
                                    System.out.println("Block "+i+" CurrentHash - "+ votechain.get(i).getHash());
                                }
                            } else {
                                System.out.println(port_num + " - Current votechain not up-to-date, re-update");
                                System.out.println("");
                                this.votechain = message.getBlocks();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("++++++++++++++END SYNC BLOCKCHAIN+++++++++++++++++");
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
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
