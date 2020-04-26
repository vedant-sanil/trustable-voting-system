package client;

import java.net.InetSocketAddress;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
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
import server.Server;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Client {
    /** Public key */
    private PublicKey publicKey;
    /** Private key */
    private PrivateKey privateKey;
    /** User name of current node */
    private String user_name;
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
    /** Data creation for registration */
    public Map<String, String> data = new LinkedHashMap<String, String>();

    public Client(int client_port, int server_port, int node_port) throws IOException, NoSuchAlgorithmException, InterruptedException{
        this.client_port = client_port;
        this.node_port = node_port;
        this.server_port = server_port;

        // Assign a user name to this node
        this.user_name = "Client_"+this.client_port;

        // Initialize Gson object
        this.gson = new Gson();

        // Register the Clients to the blockchain
        this.register();

        // Create the client
        this.client_skeleton = HttpServer.create(new InetSocketAddress(this.client_port), 0);
        this.client_skeleton.setExecutor(Executors.newCachedThreadPool());
    }

    /** Start the voting client */
    void start() {
        this.startSkeletons();
    }

    /** Stop the voting client */
    void stop() {
        this.client_skeleton.stop(0);
    }

    /** Method to start skeletons */
    private void startSkeletons() {
        if (this.skeleton_started) return;

        this.client_api();
        this.client_skeleton.start();

        this.skeleton_started = true;
    }

    /** Wrapper method over all the Client APIs */
    private void client_api() {
        this.startVote();
    }

    private void startVote() {
        this.client_skeleton.createContext("/startvote", (exchange -> {
            System.out.println("=========START VOTE========");
            String jsonString = "";
            int returnCode = 0;
            boolean found = false;

            if ("POST".equals(exchange.getRequestMethod())) {
                StartVoteRequest startVoteRequest = null;
                try {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                    startVoteRequest = gson.fromJson(isr, StartVoteRequest.class);

                    int chain_id = startVoteRequest.getChainId();
                    String vote_for = startVoteRequest.getVoteFor();

                    System.out.println("Chain Id which is come in "+ chain_id);
                    System.out.println("vote_for which is come in "+ vote_for);
                    // Create a Voted for object to be encrypted
                    VotedFor votedFor = new VotedFor(this.user_name, vote_for);
                    String votedForString = gson.toJson(votedFor);

                    // Encrypt with client's private key
                    String encrypted_voted_for = this.encrypt_rsa_privkey(votedForString, this.privateKey);
                    System.out.println("encrypted_voted_for at the StartVote" + encrypted_voted_for);

                    if (encrypted_voted_for.equals("Failed")) {
                        // Occurs if encryption fails with voted for
                        returnCode = 200;
                        boolean success = false;
                        String info = "Malformed voted for provided";
                        StatusReply statusReply = new StatusReply(success, info);
                        jsonString = gson.toJson(statusReply);
                    } else {
                        VoteContents voteContents = new VoteContents(chain_id, this.user_name, encrypted_voted_for);
                        String voteContentsString = gson.toJson(voteContents);
                        System.out.println("Vote Content String : "+ voteContentsString);
                        // Generate AES Encryption Key
                        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                        keyGen.init(128);
                        SecretKey secretKey = keyGen.generateKey();
                        // Generate AES Key

                        // Encrypt vote contents with AES
                        String encrypted_vote_content = this.encrypt(voteContentsString, secretKey);

                        if (encrypted_vote_content.equals("Failed")) {
                            // Occurs if AES encryption fails with vote contents
                            returnCode = 200;
                            boolean success = false;
                            String info = "Malformed vote contents provided with AES";
                            StatusReply statusReply = new StatusReply(success, info);
                            jsonString = gson.toJson(statusReply);
                        } else {
                            // Encrypt aes_encryption_key with Server's public key
                            String server_pubkey = "";

                            // Cycle through blocks to get server's public key
                            GetChainRequest request = new GetChainRequest(1);
                            HttpResponse<String> response = this.getResponse("/getchain", this.node_port, request);
                            GetChainReply getChainReply = gson.fromJson(response.body(), GetChainReply.class);

                            for (Block block : getChainReply.getBlocks()) {
                                Map<String, String> data = block.getData();
                                if (data.get("user_name").equals("Server_"+this.server_port)) {
                                    found = true;
                                    server_pubkey = data.get("public_key");
                                }
                            }

                            if (!found) {
                                // Server not registered to blockchain
                                returnCode = 422;
                                boolean success = false;
                                String info = "NonExistentServer";
                                StatusReply statusReply = new StatusReply(success, info);
                                jsonString = gson.toJson(statusReply);
                            } else {
                                // Encrypt aes session key with server's public key
                                System.out.println("Server Public Key is "+ server_pubkey);
                                PublicKey server_PublicKey = this.strToPubKey(server_pubkey);
                                String encrypted_session_key = this.encrypt_rsa_pubkey(secretKey, server_PublicKey);

                                if (encrypted_session_key.equals("Failed")) {
                                    // Occurs if encryption fails while encrypting aes session key
                                    returnCode = 200;
                                    boolean success = false;
                                    String info = "Failure in aes session key encryption";
                                    StatusReply statusReply = new StatusReply(success, info);
                                    jsonString = gson.toJson(statusReply);
                                } else {
                                    CastVoteRequest castVoteRequest = new CastVoteRequest(encrypted_vote_content, encrypted_session_key);
                                    System.out.println("CastVoteRequest Encrypted Votes : "+ castVoteRequest.getEncryptedVotes());
                                    System.out.println("CastVoteRequest Encrypted Session Key : "+ castVoteRequest.getEncryptedSessionKey());
                                    // Kick off cast vote in server
                                    HttpResponse<String> response2 = this.getResponse("/castvote", this.server_port, castVoteRequest);
                                    StatusReply statusReply = gson.fromJson(response2.body(), StatusReply.class);

                                    returnCode = 200;
                                    if (statusReply.getSuccess()) {
                                        // Cast vote returned a success
                                        String info="";
                                        StatusReply statusReply1 = new StatusReply(statusReply.getSuccess(), info);
                                        jsonString = gson.toJson(statusReply1);
                                    } else {
                                        String info = "IncorrectCastVoteReply";
                                        StatusReply statusReply1 = new StatusReply(statusReply.getSuccess(), info);
                                        jsonString = gson.toJson(statusReply1);
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
        System.out.println("=========START VOTES REGISTERED  END========");
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
        System.out.println("After Encoding pr_key" + pu_key);
        System.out.println("After Encoding pr_key" + pr_key);
//        pr_key = new String(this.privateKey.getEncoded(), "UTF-8");
//        pu_key = new String(this.publicKey.getEncoded(), "UTF-8");

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
     * Main function to kickstart the Client
     * @param command line args
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void main(String[] args) throws FileNotFoundException, IOException, NoSuchAlgorithmException, InterruptedException {
        Random rand = new Random();
        File file = new File("./Clientfile" + Integer.parseInt(args[0]) + ".output");
        PrintStream stream = new PrintStream(file);
        System.setOut(stream);
        System.setErr(stream);

        // Begin a client by providing client port and a single node port
        Client c = new Client(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
        c.start();
    }

    /**
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
//            System.out.println("Public Key is : "+ pubKey1);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return pubKey1;
    }

    /**
     * Method to encrypt string using RSA 2048 and private key
     * @param decrypt_str : string to be encrytped
     * @param decrypt_key : encryption key (RSA)
     * @return encrypted string
     * Encryption help taken from : https://www.devglan.com/java8/rsa-encryption-decryption-java
     */
    private String encrypt_rsa_privkey(String encrypt_str, PrivateKey encrypt_key) {
        String encrypted_str = "";
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, encrypt_key);
            encrypted_str = Base64.getEncoder().encodeToString(cipher.doFinal(encrypt_str.getBytes("UTF-8")));
            System.out.println("encrypt_rsa_privkey - Encrypted String is "+encrypted_str);
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed";
        }
        return encrypted_str;
    }

    /**
     * Method to encrypt string using RSA 2048 and public key
     * @param decrypt_str : string to be encrytped
     * @param decrypt_key : encryption key (RSA)
     * @return encrypted string
     * Encryption help taken from : https://www.devglan.com/java8/rsa-encryption-decryption-java
     */
    private String encrypt_rsa_pubkey(SecretKey encrypt_str, PublicKey encrypt_key) {
        String encrypted_str = "";
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, encrypt_key);
            encrypted_str = Base64.getEncoder().encodeToString(cipher.doFinal(encrypt_str.getEncoded()));
            System.out.println("encrypt_rsa_pubkey - Encrypted Session Key String is "+encrypted_str);
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed";
        }
        return encrypted_str;
    }

    /**
     * Method to encrypt a string using AES 128
     * @param string to be encrypted
     * @return encrypted string
     * Encryption help taken from: https://www.includehelp.com/java-programs/encrypt-decrypt-string-using-aes-128-bits-encryption-algorithm.aspx
     */
    private String encrypt(String encrypt_str, SecretKey encrypt_key) {
        String encrypted_str = "";
        try {
            Cipher cipher = Cipher.getInstance("AES");
//            IvParameterSpec ivParameterSpec = new IvParameterSpec(key);
            cipher.init(Cipher.ENCRYPT_MODE, encrypt_key);
            byte[] cipherText = cipher.doFinal(encrypt_str.getBytes());
            Base64.Encoder encoder = Base64.getEncoder();
            encrypted_str = encoder.encodeToString(cipherText);
            System.out.println("encrypt - Encrypted String is "+encrypted_str);
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed";
        }
        return encrypted_str;
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
}