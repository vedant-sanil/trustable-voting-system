package server;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.X509EncodedKeySpec;
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

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Server {
    /** Public key */
    private PublicKey publicKey;
    /** Private key */
    private PrivateKey privateKey;
    /** User name of current node */
    private String user_name;
    /** Port number of blockchain node*/
    private int node_port;
    /** Port number of Server node*/
    private int server_port;
    /** HttpServer skeleton that perfroms the requisite communication*/
    private HttpServer server_skeleton;
    /** Check if skeleton has started */
    private boolean skeleton_started = false;
    /** Gson object which can parse json to an object */
    protected Gson gson;
    /** Data creation for registration */
    public Map<String, String> data = new LinkedHashMap<String, String>();
    /** Data creation for vote */
    public Map<String, String> vote = new LinkedHashMap<String, String>();
    /** List of eligible candidates */
    List<String> candidate_names;

    public Server(int server_port, int node_port) throws IOException, NoSuchAlgorithmException, InterruptedException {
        this.server_port = server_port;
        this.node_port = node_port;

        // Assign a user name to this node
        this.user_name = "Server_"+this.server_port;

        // Initialize objects
        this.gson = new Gson();
        this.candidate_names = new ArrayList<String>();

        // Register Server to blockchain
        this.register();

        // Create the server and node communication servers
        this.server_skeleton = HttpServer.create(new InetSocketAddress(this.server_port), 0);
        this.server_skeleton.setExecutor(Executors.newCachedThreadPool());
    }

    /** Start the voting server */
    void start() {
        this.startSkeletons();
    }

    /** Stop the voting server */
    void stop() {
        this.server_skeleton.stop(0);
    }

    /** Method to start skeletons */
    private void startSkeletons() {
        if (this.skeleton_started) return;

        this.server_api();
        this.server_skeleton.start();

        this.skeleton_started = true;
    }

    /** Wrapper method over all the Server APIs */
    private void server_api() {
        this.becomeCandidates();
        this.getCandidates();
        this.castVote();
    }

    /**
     * Method to cast votes for a candidate
     */
    private void castVote() {
        //System.out.println("=========CAST VOTES REGISTERED========");
        this.server_skeleton.createContext("/castvote", (exchange -> {
            System.out.println("=========CAST VOTES========");
            String jsonString = "";
            int returnCode = 0;
            boolean found = false;

            if ("POST".equals(exchange.getRequestMethod())) {
                CastVoteRequest castVoteRequest = null;
                try {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                    castVoteRequest = gson.fromJson(isr, CastVoteRequest.class);

                    String encrypted_vote_contents = castVoteRequest.getEncryptedVotes();
                    String encrypted_session_key = castVoteRequest.getEncryptedSessionKey();

                    String aes_session_key = this.decrypt_rsa(encrypted_session_key, this.privateKey);

                    if (aes_session_key.equals("Failed")) {
                        // Occurs if input vote string is malformed
                        returnCode = 200;
                        boolean success = false;
                        String info = "Malformed input data provided";
                        StatusReply statusReply = new StatusReply(success, info);
                        jsonString = gson.toJson(statusReply);
                    } else {
                        // Decrypt vote contents with AES session key
                        String decrypted_vote_contents = this.decrypt_aes(encrypted_vote_contents, aes_session_key);

                        if (decrypted_vote_contents.equals("Failed")) {
                            // Occurs if vote contents are malformed
                            returnCode = 200;
                            boolean success = false;
                            String info = "Malformed vote contents provided";
                            StatusReply statusReply = new StatusReply(success, info);
                            jsonString = gson.toJson(statusReply);
                        } else {
                            VoteContents voteContents = gson.fromJson(decrypted_vote_contents, VoteContents.class);
                            int chain_id = voteContents.getChain_id();
                            String voter = voteContents.getUser_name();
                            String encrypted_vote = voteContents.getEncrypted_vote();
                            String client_pubkey = "";

                            // Cycle through blocks to get client's public key
                            GetChainRequest request = new GetChainRequest(1);
                            HttpResponse<String> response = this.getResponse("/getchain", this.node_port, request);
                            GetChainReply getChainReply = gson.fromJson(response.body(), GetChainReply.class);

                            for (Block block : getChainReply.getBlocks()) {
                                Map<String, String> data = block.getData();
                                if (data.get("user_name").equals(voter)) {
                                    found = true;
                                    client_pubkey = data.get("public_key");
                                }
                            }

                            if (!found) {
                                // Client not found in identity blockchain
                                returnCode = 422;
                                boolean success = false;
                                String info = "NonExistentClient";
                                StatusReply statusReply = new StatusReply(success, info);
                                jsonString = gson.toJson(statusReply);
                            } else {
                                // Client pubKey found as string, convert to publicKey and decrypt
                                PublicKey client_publicKey = this.strToPubKey(client_pubkey);
                                String decrypted_voted_for = this.decrypt_rsa_pubkey(encrypted_vote,client_publicKey);

                                if (decrypted_voted_for.equals("Failed")) {
                                    // Occurs if voted for contents are malformed
                                    returnCode = 200;
                                    boolean success = false;
                                    String info = "Malformed voted for data provided";
                                    StatusReply statusReply = new StatusReply(success, info);
                                    jsonString = gson.toJson(statusReply);
                                } else {
                                    VotedFor votedFor = gson.fromJson(decrypted_vote_contents, VotedFor.class);
                                    String candidate = votedFor.getVoted_for();

                                    if (this.candidate_names.contains(candidate)) {
                                        // Candidate succesfully voted for
                                        returnCode = 200;
                                        boolean success = true;
                                        String info = "VoteSuccessful";
                                        StatusReply statusReply = new StatusReply(success, info);
                                        jsonString = gson.toJson(statusReply);

                                        // Add candidate to voting chain
                                        // Create data block
                                        this.vote.put("voted_for", candidate);
                                        this.vote.put("user_name", voter);

                                        // Mine a new block
                                        MineBlockRequest request1 = new MineBlockRequest(chain_id, this.vote);
                                        response = this.getResponse("/mineblock", this.node_port, request1);
                                        BlockReply mined_reply = gson.fromJson(response.body(), BlockReply.class);

                                        // Add mined block to block chain
                                        if (mined_reply.getChainId() == 2) {
                                            Block new_block = mined_reply.getBlock();
                                            AddBlockRequest request2 = new AddBlockRequest(chain_id, new_block);
                                            response = this.getResponse("/addblock", this.node_port, request2);
                                            StatusReply status = gson.fromJson(response.body(), StatusReply.class);
                                            if (!status.getSuccess()) {
                                                System.out.println("Block could not be added!");
                                                return;
                                            }
                                        } else {
                                            System.out.println("Incorrect chain!");
                                            return;
                                        }
                                    } else {
                                        // Candidate not found
                                        returnCode = 422;
                                        boolean success = false;
                                        String info = "InvalidCandidate";
                                        StatusReply statusReply = new StatusReply(success, info);
                                        jsonString = gson.toJson(statusReply);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                jsonString = "The REST method should be POST for <getCandidates>!\n";
                returnCode = 400;
            }
            this.generateResponseAndClose(exchange, jsonString, returnCode);
        }));
        //System.out.println("=========CAST VOTES REGISTERED  END========");
    }

    /**
     * Function to check and add a client as candidate
     */
    private void becomeCandidates() {
        this.server_skeleton.createContext("/becomecandidate", (exchange -> {
            System.out.println("=========BECOME CANDIDATES========");
            String jsonString = "";
            BecomeCandidateRequest becomeCandidateRequest = null;
            StatusReply statusReply = null;
            int returnCode = 0;
            if ("POST".equals(exchange.getRequestMethod())) {
                returnCode = 200;
                InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                becomeCandidateRequest = gson.fromJson(isr, BecomeCandidateRequest.class);

                // Get Candidate details
                String candidateName = becomeCandidateRequest.getCandidateName();

                if (this.candidate_names.contains(candidateName)) {
                    // Candidate already exists
                    returnCode = 409;
                    boolean success = false;
                    String info = "NodeAlreadyCandidate";
                    statusReply = new StatusReply(success, info);
                    jsonString = gson.toJson(statusReply);
                } else {
                    try {
                        boolean found = false;
                        GetChainRequest request = new GetChainRequest(1);
                        HttpResponse<String> response = this.getResponse("/getchain", this.node_port, request);
                        GetChainReply getChainReply = gson.fromJson(response.body(), GetChainReply.class);

                        // Get blocks in the blockchain and check if public key of candidate is registered
                        List<Block> blocks = getChainReply.getBlocks();

                        for (Block block : blocks) {
                            Map<String, String> data = block.getData();
                            if (data.get("user_name").equals(candidateName)) {
                                found = true;
                            }
                        }

                        // If not found, return error else add client to candidate list
                        if (!found) {
                            returnCode = 422;
                            boolean success = false;
                            String info = "CandidatePublicKeyUnknown";
                            statusReply = new StatusReply(success, info);
                            jsonString = gson.toJson(statusReply);
                        } else {
                            returnCode = 200;
                            boolean success = true;
                            String info = "ClientSuccessfullyAddedAsCandidate";
                            statusReply = new StatusReply(success, info);
                            jsonString = gson.toJson(statusReply);
                            this.candidate_names.add(candidateName);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                jsonString = "The REST method should be POST for <getCandidates>!\n";
                returnCode = 400;
            }
            this.generateResponseAndClose(exchange, jsonString, returnCode);
        }));
    }

    /**
     * Function to obtain a list of all eligible candidates
     */
    private void getCandidates() {
        this.server_skeleton.createContext("/getcandidates", (exchange -> {
            System.out.println("=========GET CANDIDATES========");
            String jsonString = "";
            int returnCode = 0;
            if ("POST".equals(exchange.getRequestMethod())) {
                returnCode = 200;
                GetCandidatesReply getCandidatesReply = new GetCandidatesReply(this.candidate_names);
                jsonString = gson.toJson(getCandidatesReply);
            } else {
                jsonString = "The REST method should be POST for <getCandidates>!\n";
                returnCode = 400;
            }
            this.generateResponseAndClose(exchange, jsonString, returnCode);
        }));
    }

    /**
     * Mines a new block and adds to the blockchain
     */
    private void register() throws NoSuchAlgorithmException, IOException, InterruptedException{
        // Add data to identitiy chain
        int chain_id = 1;
        String pr_key;
        String pu_key;

        // Generate public-private key pair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair keys = kpg.generateKeyPair();
        this.publicKey = keys.getPublic();
        this.privateKey = keys.getPrivate();
//        pr_key = new String(this.privateKey.getEncoded(), "UTF-8");
//        pu_key = new String(this.publicKey.getEncoded(), "UTF-8");
        pr_key = Base64.getEncoder().encodeToString(this.privateKey.getEncoded());
        pu_key = Base64.getEncoder().encodeToString(this.publicKey.getEncoded());

        // Create data block
        this.data.put("public_key", pu_key);
        this.data.put("user_name", this.user_name);

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

    /**
     * Function to generate a response based on a json string, Http exchange object and a return code
     * @param exchange
     * @param respText : response text
     * @param returnCode
     * @throws IOException
     */
    private void generateResponseAndClose(HttpExchange exchange, String respText, int returnCode) throws IOException {
        exchange.sendResponseHeaders(returnCode, respText.getBytes().length);
        OutputStream output = exchange.getResponseBody();
        output.write(respText.getBytes());
        output.flush();
        exchange.close();
        System.out.println("++++++++++++++++++++++++++++++++");
    }

    /**
     * Method to convert String to PublicKey
     * @param publicKey : Input string public key
     * @return public key of type PublicKey
     */
    private PublicKey strToPubKey(String publicKey) {
        PublicKey pubKey1 = null;
        try {
//            byte[] publicBytes = publicKey.getBytes("UTF-8");
            byte[] publicBytes = Base64.getDecoder().decode(publicKey);
            X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(publicBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            pubKey1 = keyFactory.generatePublic(x509EncodedKeySpec);
            System.out.println("Public Key is : "+ pubKey1);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return pubKey1;
    }

    /**
     * Method to decrypt string using RSA 2048
     * @param decrypt_str : string to be decrypted
     * @param decrypt_key : decryption key (RSA)
     * @return decrypted string
     * Encryption help taken from : https://www.devglan.com/java8/rsa-encryption-decryption-java
     */
    private String decrypt_rsa_pubkey(String decrypt_str, PublicKey decrypt_key) {
        String decrypted_str = "";
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, decrypt_key);
            decrypted_str = new String(cipher.doFinal(decrypt_str.getBytes("UTF-8")), "UTF-8");
            System.out.println("decrypt_rsa_pubkey - Decrypted String is "+decrypted_str);
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed";
        }
        return decrypted_str;
    }

    /**
     * Method to decrypt string using RSA 2048
     * @param decrypt_str : string to be decrypted
     * @param decrypt_key : decryption key (RSA)
     * @return decrypted string
     * Encryption help taken from : https://www.devglan.com/java8/rsa-encryption-decryption-java
     */
    private String decrypt_rsa(String decrypt_str, PrivateKey decrypt_key) {
        String decrypted_str = "";
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, decrypt_key);
            decrypted_str = new String(cipher.doFinal(decrypt_str.getBytes("UTF-8")), "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed";
        }
        System.out.println("decrypt_rsa - Decrypted String is "+decrypted_str);
        return decrypted_str;
    }

    /**
     * Method to decrypt a string using AES 128
     * @param decrypt_str : string to be decrypted
     * @param decrypt_key : decryption key (AES)
     * @return decrypted string
     * Encryption help taken from: https://www.includehelp.com/java-programs/encrypt-decrypt-string-using-aes-128-bits-encryption-algorithm.aspx
     */
    private String decrypt_aes(String decrypt_str, String decrypt_key) {
        String decrypted_str = "";
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            byte[] key = decrypt_key.getBytes("UTF-8");
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(key);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
            Base64.Decoder decoder = Base64.getDecoder();
            byte[] cipherText = decoder.decode(decrypt_key.getBytes("UTF-8"));
            decrypted_str = new String(cipher.doFinal(), "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed";
        }
        System.out.println("decrypt_aes - Decrypted String is "+decrypted_str);
        return decrypted_str;
    }
}