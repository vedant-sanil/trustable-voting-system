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
    /** Payload for the Genesis Block */
    public Map<String, String> identity_payload = new LinkedHashMap<String,String>();
    public Map<String, String> vote_payload = new LinkedHashMap<String,String>();
    /** Nonce for the Mining */
    public long nonce;
    /** First blocks in each of the chains */
    public Block genesisBlockTest, genesisBlockVoteTest, genesisBlock, genesisBlockVote;

    /** Flag to check if node is asleep or not */
    public boolean isSleep = false;

    /** Constructor for each Node
     *
     * @param NODENUM - Values 0,1,2,3. Gets the position of the args and starts Nodes on each of those ports.
     * @param args - All the ports of the nodes. Totally 4 in number
     * @throws IOException
     */
    public Node(int NODENUM, String args) throws IOException{

        /** Creating the identity and vote chain payloads */
        this.identity_payload.put("public_key", "xxx");
        this.identity_payload.put("user_name", "xxx");
        this.vote_payload.put("vote", "xxx");
        this.vote_payload.put("voter_credential", "xxx");

        /** Initialisation of Nonce */
        this.nonce = 0;

        /** Blocks to compute the first hash */
        this.genesisBlockTest = new Block(0, identity_payload,Long.parseLong("5415419034"),nonce,"xxx","xxx");
        this.genesisBlockVoteTest = new Block(0, vote_payload,Long.parseLong("5415419034"),nonce,"xxx","xxx");
        String hash = Block.computeHash(genesisBlockTest);
        String hashVote = Block.computeHash(genesisBlockVoteTest);

        /** Final Genesis Blocks which are to be added to the Blockchain and Vote Chain */
        this.genesisBlock = new Block(0, identity_payload,Long.parseLong("5415419034"),nonce,"xxx",hash);
        this.genesisBlockVote = new Block(0, vote_payload,Long.parseLong("5415419034"),nonce,"xxx",hashVote);


        /** Find each port in args list */
        String[] port_list = args.split(",");
        this.node_num = NODENUM;

        /** Add the Genesis Blocks to the respective chains */
        this.blockchain.add(genesisBlock);
        this.votechain.add(genesisBlockVote);

        /** Creates a list of ports which will give information on the peer nodes */
        ports = new ArrayList<Integer>();
        node_skeleton = new ArrayList<HttpServer>();

        /** Add each port in the port list to the ports Arraylist */
        for (String arg : port_list) {
            Integer port = Integer.parseInt(arg);
            this.ports.add(port);
        }

        /** Create the node skeleton for the API Commands */
        temp_skeleton = HttpServer.create(new java.net.InetSocketAddress(this.ports.get(NODENUM)), 0);
        temp_skeleton.setExecutor(Executors.newCachedThreadPool());
        this.node_skeleton.add(temp_skeleton);

        /** GSON Object for the conversions from GSON to Class Objects */
        this.gson = new Gson();
    }

    /**
     * Starts the blockchain servers
     *
     */
    void start() {
        this.startSkeletons();
    }

    /**
     * Stops the naming servers
     *
     */
    void stop() {
        for (HttpServer skeleton : this.node_skeleton) {
            skeleton.stop(0);
        }
    }

    /**
     * Function to start skeletons
     *
     */
    private void startSkeletons() {
        if (this.skeleton_started) return;

        for (HttpServer skeleton : this.node_skeleton) {
            this.node_api(skeleton);
            skeleton.start();
        }

        this.skeleton_started = true;
    }

    /** Registers each of the APIs to be used by the Node
     *
     * @param skeleton - HTTP Server skeleton to send and recieve information
     */
    private void node_api(HttpServer skeleton) {
        this.getBlockChain(skeleton);
        this.mineBlock(skeleton);
        this.addBlock(skeleton);
        this.broadcastBlock(skeleton);
        this.sleepChain(skeleton);
    }

    /**
     * GetChain API to return the blocks for the node.
     * @param skeleton
     */
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

                    /** Return the chain depending on the Chain IDs */
                    if (getChainRequest.getChainId() == 1)
                    {
                        getChainReply = new GetChainReply(getChainRequest.getChainId(), this.blockchain.size(), this.blockchain);
                    }
                    else
                    {
                        getChainReply = new GetChainReply(getChainRequest.getChainId(), this.votechain.size(), this.votechain);
                    }

                    /** Returns jsonString */
                    jsonString = gson.toJson(getChainReply);
                    returnCode = 200;
                } catch (Exception e) {
                    returnCode = 404;
                    jsonString = "Request information is incorrect";
                }
            } else {
                jsonString = "The REST method should be POST for <getBlockChain>!\n";
                returnCode = 400;
            }
            this.generateResponseAndClose(exchange, jsonString, returnCode);
        }));
    }

    /** Puts the node to sleep for the required period of time.
     *
     * @param skeleton
     */
    private void sleepChain(HttpServer skeleton) {
        skeleton.createContext("/sleep", (exchange ->
        {
            String jsonString = "";
            int returnCode = 0;
            SleepRequest sleepRequest = null;

            /** If Sleep is true, then don't run */
            if ("POST".equals(exchange.getRequestMethod()) && this.isSleep == false) {
                try {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                    sleepRequest = gson.fromJson(isr, SleepRequest.class);

                    /** Creates the Hashmap for returning */
                    Map<String, Object> resmap = new HashMap<String, Object>();
                    resmap.put("success", "true");
                    resmap.put("info", "");
                    jsonString = gson.toJson(resmap);
                    returnCode = 200;
                } catch (Exception e) {
                    returnCode = 404;
                    jsonString = "Request information is incorrect";
                }
            } else {
                jsonString = "The REST method should be POST for <getBlockChain>!\n";
                returnCode = 400;
            }
            this.generateResponseAndClose(exchange, jsonString, returnCode);

            /** Boolean variable which is set to true whenever needing to sleep */
            synchronized (this) {
                this.isSleep = true;
            }

            /** Sleeping for the set of seconds requested off the Sleep Request */
            try {
                Thread.sleep(sleepRequest.getTimeout()*1000);
            } catch (Exception e) {
                e.printStackTrace();
            }

            /** Resetting the Flag to false */
            synchronized (this) {
                this.isSleep = false;
            }
        }));
    }

    /**
     * Function for mining the block.
     *
     * @param skeleton
     */
    private void mineBlock(HttpServer skeleton) {
        skeleton.createContext("/mineblock", (exchange ->
        {
            String jsonString = "";
            int returnCode = 0;

            /** If Sleep is true, then don't run */
            if ("POST".equals(exchange.getRequestMethod())) {
                MineBlockRequest mineBlockRequest = null;
                BlockReply blockReply = null;
                try {
                    Block init_block = null;
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                    mineBlockRequest = gson.fromJson(isr, MineBlockRequest.class);

                    /** Synchronize the chain before mining */
                    this.synchronize(mineBlockRequest.getChainId());

                    /** Get time, increment nonce and add init block */
                    long curr_time = System.currentTimeMillis();

                    /** Get current hash by mining through difficulty 5 */
                    String mined_hash = "xxx";
                    String prev_hash = "xxx";

                    /** If Chain ID is 1, then make sure difficulty is 5. Else difficulty is 1. */
                    if (mineBlockRequest.getChainId() == 1) {
                        prev_hash = this.blockchain.get(this.blockchain.size()-1).getHash();
                        /** The minedhash must start with 5 zeros. Else keep mining */
                        while (!mined_hash.startsWith("00000")) {
                            nonce++;
                            /** Create temporary new block */
                            init_block = new Block(this.node_num, mineBlockRequest.getData(),
                                    curr_time, nonce, prev_hash, mined_hash);
                            mined_hash = Block.computeHash(init_block);
                        }
                    } else {
                        prev_hash = this.votechain.get(this.votechain.size()-1).getHash();

                        /** The minedhash must start with 1 zeros. Else keep mining */
                        while (!mined_hash.startsWith("0")) {
                            nonce++;
                            /** Create temporary new block */
                            init_block = new Block(this.node_num, mineBlockRequest.getData(),
                                    curr_time, nonce, prev_hash, mined_hash);
                            mined_hash = Block.computeHash(init_block);
                        }
                    }
                    /** Successfully create blockchain reply */
                    blockReply = new BlockReply(mineBlockRequest.getChainId(), new Block(this.node_num, mineBlockRequest.getData(),
                            curr_time, nonce, prev_hash, mined_hash));
                    jsonString = gson.toJson(blockReply);
                    returnCode = 200;
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

    /**
     * Adding a block to the chain
     *
     * @param skeleton
     */
    private void addBlock(HttpServer skeleton) {
        skeleton.createContext("/addblock", (exchange ->
        {
            String jsonString = "";
            int returnCode = 0;
            Map<String, Object> resmap = new HashMap<String, Object>();

            /** If Sleep is true, then don't run */
            if ("POST".equals(exchange.getRequestMethod()) && this.isSleep == false) {
                AddBlockRequest addBlockRequest = null;
                try {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                    addBlockRequest = gson.fromJson(isr, AddBlockRequest.class);

                    /** Synchronizing the Chains */
                    this.synchronize(addBlockRequest.getChainId());
                    resmap.put("info", "");
                    BroadcastRequest reqToSend = new BroadcastRequest(addBlockRequest.getChainId(), "PRECOMMIT", addBlockRequest.getBlock());
                    HttpResponse<String> response = null;
                    try {
                        int vote_cnt = 1;

                        /** Send a broadcast request with Pre Commit to each of the ports */
                        for (int i = 0; i < this.ports.size(); i++)
                        {
                            if (i != this.node_num) {
                                response = this.getResponse("/broadcast", this.ports.get(i), reqToSend);

                                if (response.statusCode() == 200)
                                {
                                    vote_cnt += 1;
                                }
                            }
                        }

                        /** If the count is greater than 2/3, make it a commit message and send it over to each of the
                         * 4 ports including own port. This is responsible for the adding of blocks
                         */
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

                        /** Creating the jsonString from the hashmap */
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


    /** Add the block if a COMMIT message comes in. Else check if block can be added and return true if possible.
     *
     * @param skeleton
     */
    private void broadcastBlock(HttpServer skeleton) {
        skeleton.createContext("/broadcast", (exchange ->
        {
            String jsonString = "";
            int returnCode = 0;
            Map<String, Object> resmap = new HashMap<String, Object>();

            /** If Sleep is true, then don't run */
            if ("POST".equals(exchange.getRequestMethod()) && this.isSleep == false) {
                BroadcastRequest getBroadcastRequest = null;
                try {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                    getBroadcastRequest = gson.fromJson(isr, BroadcastRequest.class);

                    /** Synchronizing the chains */
                    this.synchronize(getBroadcastRequest.getChainId());
                    resmap.put("info", "");

                    /** Check the following if PRECOMMIT:
                     * 1. Check if previous hash is equal to last block's current hash.
                     * 2. Check if the hash computed again with this block is equal to the hash mentioned in the block.
                     */
                    if (getBroadcastRequest.getRequestType().equals("PRECOMMIT"))
                    {
                        if (getBroadcastRequest.getChainId() == 1) {
                            if (getBroadcastRequest.getBlock().getPreviousHash().equals(blockchain.get(blockchain.size() - 1).getHash())) {
                                String computed_hash = Block.computeHash(getBroadcastRequest.getBlock());

                                if (computed_hash.equals(getBroadcastRequest.getBlock().getHash())) {
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
                            if (getBroadcastRequest.getBlock().getPreviousHash().equals(votechain.get(votechain.size() - 1).getHash())) {
                                String computed_hash = Block.computeHash(getBroadcastRequest.getBlock());

                                if (computed_hash.equals(getBroadcastRequest.getBlock().getHash())) {
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

                        /** Add to the chain if not added already earlier. */
                        if (getBroadcastRequest.getChainId() == 1 && !blockchain.get(blockchain.size() - 1).getHash().equals(getBroadcastRequest.getBlock().getHash()))
                        {
                            blockchain.add(getBroadcastRequest.getBlock());
                        } else if (getBroadcastRequest.getChainId() == 2 && !votechain.get(votechain.size() - 1).getHash().equals(getBroadcastRequest.getBlock().getHash())){
                            votechain.add(getBroadcastRequest.getBlock());
                        }
//                        this.synchronize(getBroadcastRequest.getChainId());
                        resmap.put("success", "true");
                        returnCode = 200;
                    }

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

    /**
     * Responsible for synchronizing the chain.
     * 1. Get the longer chain
     * 2. If chain sizes are same then make sure newer chain is taken. (newer timestamp)
     * @param chain_id
     */
    private void synchronize(int chain_id) {
        for (int port_num : this.ports) {
            if (port_num != this.ports.get(this.node_num)) {
                // Communicating with peer nodes
                try {
                    GetChainRequest request = new GetChainRequest(1);
                    HttpResponse<String> response = this.getResponse("/getchain", port_num, request);
                    GetChainReply message = gson.fromJson(response.body(), GetChainReply.class);
                    List<Block> other_chain = new ArrayList<Block>();

                    if (chain_id == 1) {
                        /** Synchronizing Block Chain */
                        if (this.blockchain.size() <= message.getChainLength()) {
                            if (this.blockchain.size() == message.getChainLength()) {
                                other_chain = message.getBlocks();
                                for (int i = 0; i < this.blockchain.size(); i++) {
                                    if (this.blockchain.get(i).getTimestamp() < other_chain.get(i).getTimestamp()) {
                                        for (int j = 0; j < message.getBlocks().size(); j++)
                                            this.blockchain.set(j, message.getBlocks().get(j));
                                        break;
                                    }
                                }
                            } else {
                                this.blockchain = message.getBlocks();
                            }
                        }
                    } else {
                        /** Synchronizing Vote Chain */
                        request = new GetChainRequest(2);
                        response = this.getResponse("/getchain", port_num, request);
                        message = gson.fromJson(response.body(), GetChainReply.class);
                        other_chain = new ArrayList<Block>();

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
                                }
                            } else {
                                this.votechain = message.getBlocks();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * Main Function
     * @param args
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void main(String[] args) throws FileNotFoundException, IOException {
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
    }
}
