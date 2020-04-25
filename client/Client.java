package client;

import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
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

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Client {
    /** Public key */
    private String publicKey;
    /** Private key */
    private String privateKey;
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
    /** Data creation for vote */
    public Map<String, String> vote = new LinkedHashMap<String, String>();

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
        return;
    }

    /** Wrapper method over all the Client-Blockchain Node APIs */
    private void client_node_api() {
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
        this.publicKey = new String(keys.getPublic().getEncoded(), "UTF-8");
        this.privateKey = new String(keys.getPrivate().getEncoded(), "UTF-8");

        // Create data block
        this.data.put("public_key", this.publicKey);
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
        File file = new File("./Clientfile" + rand.nextInt() + ".output");
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
            return null;
        }
        return decrypted_str;
    }
}