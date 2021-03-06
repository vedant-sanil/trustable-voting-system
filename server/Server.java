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
import javax.crypto.*;
import java.security.*;
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
    /** List of already voted candidates */
    List<String> list_names;

    public Server(int server_port, int node_port) throws IOException, NoSuchAlgorithmException, InterruptedException {
        this.server_port = server_port;
        this.node_port = node_port;

        // Assign a user name to this node
        this.user_name = "Server_"+this.server_port;

        // Initialize objects
        this.gson = new Gson();
        this.candidate_names = new ArrayList<String>();
        this.list_names = new ArrayList<String>();

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
        this.countVotes();
    }

    /**
     * Function to count votes
     */
    private void countVotes() {
        this.server_skeleton.createContext("/countvotes", (exchange -> {

            String jsonString = "";
            int returnCode = 0;
            if ("POST".equals(exchange.getRequestMethod())) {
                CountVotesRequest countVotesRequest;
                try {
                    InputStreamReader isr  = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                    countVotesRequest = gson.fromJson(isr, CountVotesRequest.class);

                    String candidate_to_count = countVotesRequest.getCountVotesFor();
                    int count = 0;
                    boolean block_found = false;

                    // Get vote chain
                    GetChainRequest request = new GetChainRequest(2);
                    HttpResponse<String> response = this.getResponse("/getchain", this.node_port, request);
                    GetChainReply getChainReply = gson.fromJson(response.body(), GetChainReply.class);

                    // Check if block has been encountered while voting but not in blockchain
                    if (list_names.contains(candidate_to_count)) {
                        block_found = true;
                    }

                    // Check if block in blockchain
                    for (Block block : getChainReply.getBlocks()) {
                        Map<String, String> vote = block.getData();
                        String voted_for = vote.get("vote");
                        if (voted_for.equals(candidate_to_count)) {
                            count++;
                        }
                    }

                    // If block either in blockchain or has voted, then return count
                    if (block_found) {
                        returnCode = 200;
                        boolean success = true;
                        CountVotesReply countVotesReply = new CountVotesReply(success, count);
                        jsonString = gson.toJson(countVotesReply);
                    } else {
                        returnCode = 422;
                        boolean success = false;
                        String info = "InvalidCandidate";
                        StatusReply statusReply = new StatusReply(success,info);
                        jsonString = gson.toJson(statusReply);
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
    }

    /**
     * Method to cast votes for a candidate
     */
    private void castVote() {
        this.server_skeleton.createContext("/castvote", (exchange -> {
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

                    byte[] aes_session_key = this.decrypt_rsa(encrypted_session_key, this.privateKey);

                    if (aes_session_key == null) {
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
                                    VotedFor votedFor = gson.fromJson(decrypted_voted_for, VotedFor.class);
                                    String candidate = votedFor.getVoted_for();

                                    if (!this.list_names.contains(votedFor.getUser_name())) {
                                        if (this.candidate_names.contains(candidate)) {
                                            // Add candidate to List
                                            list_names.add(votedFor.getUser_name());
                                            // Candidate succesfully voted for
                                            returnCode = 200;
                                            boolean success = true;
                                            String info = "VoteSuccessful";
                                            StatusReply statusReply = new StatusReply(success, info);
                                            jsonString = gson.toJson(statusReply);

                                            // Add candidate to voting chain
                                            // Create data block
                                            this.vote.put("vote", candidate);
                                            this.vote.put("voter_credential", this.encrypt_rsa_pubkey(voter, client_publicKey));

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
                                                    return;
                                                }
                                            } else {
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
                                    } else {
                                        // Candidate already voted
                                        returnCode = 409;
                                        boolean success = false;
                                        String info = "DuplicateVote";
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
        //
    }

    /**
     * Function to check and add a client as candidate
     */
    private void becomeCandidates() {
        this.server_skeleton.createContext("/becomecandidate", (exchange -> {

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
                return;
            }
        } else {
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
    }

    /**
     * Method to convert String to PublicKey
     * @param publicKey : Input string public key
     * @return public key of type PublicKey
     */
    private PublicKey strToPubKey(String publicKey) {
        PublicKey pubKey1 = null;
        try {
            byte[] publicBytes = Base64.getDecoder().decode(publicKey);
            X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(publicBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            pubKey1 = keyFactory.generatePublic(x509EncodedKeySpec);
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
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, decrypt_key);
            decrypted_str = new String(cipher.doFinal(Base64.getDecoder().decode(decrypt_str)), "UTF-8");

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
    private byte[] decrypt_rsa(String decrypt_str, PrivateKey decrypt_key) {
        byte[] decrypted_str;
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, decrypt_key);
            byte[] test = Base64.getDecoder().decode(decrypt_str);
            decrypted_str = cipher.doFinal(test);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return decrypted_str;
    }


    /**
     * Method to encrypt string using RSA 2048 and public key
     * @param decrypt_str : string to be encrytped
     * @param decrypt_key : encryption key (RSA)
     * @return encrypted string
     * Encryption help taken from : https://www.devglan.com/java8/rsa-encryption-decryption-java
     */
    private String encrypt_rsa_pubkey(String encrypt_str, PublicKey encrypt_key) {
        String encrypted_str = "";
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, encrypt_key);
            encrypted_str = Base64.getEncoder().encodeToString(cipher.doFinal(encrypt_str.getBytes("UTF-8")));
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed";
        }
        return encrypted_str;
    }

    /**
     * Method to decrypt a string using AES 128
     * @param decrypt_str : string to be decrypted
     * @param decrypt_key : decryption key (AES)
     * @return decrypted string
     * Encryption help taken from: https://www.includehelp.com/java-programs/encrypt-decrypt-string-using-aes-128-bits-encryption-algorithm.aspx
     */
    private String decrypt_aes(String decrypt_str, byte[] decrypt_key) {
        String decrypted_str = "";
        try {
            Cipher cipher = Cipher.getInstance("AES");
            SecretKey secretKey = new SecretKeySpec(decrypt_key, 0, decrypt_key.length, "AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            Base64.Decoder decoder = Base64.getDecoder();
            byte[] cipherText = decoder.decode(decrypt_str);
            decrypted_str = new String(cipher.doFinal(cipherText), "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed";
        }

        return decrypted_str;
    }
}