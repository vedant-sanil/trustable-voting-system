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

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Server {
    /** Public key */
    private String publicKey;
    /** Private key */
    private String privateKey;
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
        System.out.println("=========CAST VOTES REGISTERED========");
        this.server_skeleton.createContext("/castvote", (exchange -> {
            System.out.println("=========CAST VOTES========");
            String jsonString = "";
            int returnCode = 0;
            if ("POST".equals(exchange.getRequestMethod())) {
                System.out.println("Here");
                CastVoteRequest castVoteRequest = null;
                try {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                    castVoteRequest = gson.fromJson(isr, CastVoteRequest.class);

                    String encrypted_vote_contents = castVoteRequest.getEncryptedVotes();
                    String encrypted_session_key = castVoteRequest.getEncryptedSessionKey();

                    String aes_session_key = this.decrypt(encrypted_session_key, this.privateKey);
                    System.out.println(aes_session_key);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                jsonString = "The REST method should be POST for <getCandidates>!\n";
                returnCode = 400;
            }
            this.generateResponseAndClose(exchange, jsonString, returnCode);
        }));
        System.out.println("=========CAST VOTES REGISTERED  END========");
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

        // Generate public-private key pair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair keys = kpg.generateKeyPair();
        this.publicKey = new String(keys.getPublic().getEncoded(), "UTF-8");
        this.privateKey = new String(keys.getPrivate().getEncoded(), "UTF-8");

        // Create data block
        this.data.put("public_key", publicKey);
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
     * Method to encrypt a string using AES 128
     * @param string to be encrypted
     * @return encrypted string
     * Encryption help taken from: https://www.includehelp.com/java-programs/encrypt-decrypt-string-using-aes-128-bits-encryption-algorithm.aspx
     */
    private String encrypt(String encrypt_str, String encrypt_key) {
        String encrypted_str = "";
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKC5PADDING");
            byte[] key = encrypt_key.getBytes("UTF-8");
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(key);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
            byte[] cipherText = cipher.doFinal(encrypt_str.getBytes("UTF-8"));
            Base64.Encoder encoder = Base64.getEncoder();
            encrypted_str = encoder.encodeToString(cipherText);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return encrypted_str;
    }

    /**
     * Method to decrypt a string using AES 128
     * @param string to be decrypted
     * @return decrypted string
     * Encryption help taken from: https://www.includehelp.com/java-programs/encrypt-decrypt-string-using-aes-128-bits-encryption-algorithm.aspx
     */
    private String decrypt(String decrypt_str, String decrypt_key) {
        String decrypted_str = "";
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKC5PADDING");
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
        return decrypted_str;
    }
}