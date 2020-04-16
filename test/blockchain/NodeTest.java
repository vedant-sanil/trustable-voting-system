/**
 *
 *      File Name -     NodeTest.java
 *      Created By -    14736 Spring 2020 TAs
 *      Brief -
 *
 *          This file contains the helper/utility functions required to
 *          run all the tests under "/test/blockchain/"
 *
 */

package test.blockchain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import test.Config;
import test.util.Test;
import test.util.TestFailed;

import lib.MessageSender;
import message.*;


/**
 *      The base testing suite providing helper functions all the blockchain tests
 */
abstract class NodeTest extends Test
{
    protected static final String HOST_NAME = "127.0.0.1";
    protected static final String HOST_URI = "http://127.0.0.1:";
    protected static final String GET_CHAIN_URI = "/getchain";
    protected static final String MINE_BLOCK_URI = "/mineblock";
    protected static final String ADD_BLOCK_URI = "/addblock";
    protected static final String BROADCAST_URI = "/broadcast";
    protected static final String SLEEP_URI = "/sleep";

    protected static final int BROADCAST_TIMEOUT_MS = 5000;
    protected static final int SLEEP_TIMEOUT = 30;
    protected static final int CLIENT_TIMEOUT = 10;

    protected static final int KEYCHAIN_ID = 1;
    protected static final int VOTECHAIN_ID = 2;
    protected static final int[] CHAIN_IDS = {KEYCHAIN_ID, VOTECHAIN_ID};
    protected static final String[] CHAIN_PROOFS = {"00000", ""};
    protected static final String[] BROADCAST_TYPES = {"PRECOMMIT", "COMMIT"};

    private List<Process> servers = new ArrayList<>();
    protected List<Integer> nodes = new ArrayList<>();
    protected MessageSender client = new MessageSender(CLIENT_TIMEOUT);

    abstract protected void perform() throws TestFailed;


    /**
     *      Spawns processes that start blockchain nodes.
     *
     *      @throws TestFailed
     */
    protected Process spawnProcess(String command) throws Throwable
    {
        Process process = null;
        try
        {
            Runtime runtime = Runtime.getRuntime();
            process = runtime.exec(command);
        }
        catch (Exception ex)
        {
            throw new Throwable();
        }

        return process;
    }

    /**
     *      Initializes the testing environment for the blockchain tests
     *
     *      @throws TestFailed
     */
    protected void initialize() throws TestFailed
    {
        for (int i = 0; i < Config.node_ports.length; i++) {
            nodes.add(Config.node_ports[i]);
        }

        String configs[] = Config.getNodeConfigs();
        try
        {
            for (int i = 0; i < configs.length; i++) {
                String cmd = configs[i];
                Process server = spawnProcess(cmd);
                servers.add(server);
            }
        }
        catch (Throwable t)
        {
            throw new TestFailed("unable to start blockchain node");
        }

        // Attempt to make the connection.
        for (int i = 0; i < nodes.size(); i++)
        {
            int port = nodes.get(i);
            Socket socket;

            while (true)
            {
                try
                {
                    socket = new Socket();
                    socket.connect(new InetSocketAddress(HOST_NAME, port));
                    break;
                }
                catch (IOException e)
                {
                    // Ignore the exception to give server some time to start up
                    e.printStackTrace();
                }
            }

            // Make a best effort to close the socket if the connection is
            // successful.
            try
            {
                socket.close();
            }
            catch(IOException e) { }
        }
        System.out.println("Here");
    }

    /**
     *      Stops all the servers when the test ends
     */
    @Override
    protected void clean()
    {
        for (int i = 0; i < servers.size(); i++)
        {
            Process server = servers.get(i);

            if(server != null)
            {
                server.destroy();

                try
                {
                    server.waitFor();
                }
                catch(InterruptedException e) { }

                servers.set(i, null);
            }
        }

        servers = new ArrayList<>();
    }


    /**
     *      Creates a mine block request and sends it to different blockchain
     *      nodes.
     *
     *      @throws TestFailed
     */
    protected Block mineBlock(int node, int chain_id, Map<String, String> data)
            throws TestFailed
    {
        MessageSender sender = new MessageSender(CLIENT_TIMEOUT);

        MineBlockRequest mine_request = new MineBlockRequest(
                chain_id, data);
        String mine_uri = HOST_URI + nodes.get(node) + MINE_BLOCK_URI;

        BlockReply mine_reply;
        Block block;
        try
        {
            mine_reply = sender.post(mine_uri, mine_request,
                    BlockReply.class);
            if (mine_reply == null) throw new Exception();

            block = mine_reply.getBlock();
            if (block == null) throw new Exception();
        }
        catch (Exception ex)
        {
            throw new TestFailed("MineBlock failed: " +
                    "No response or incorrect format.");
        }
        return block;
    }


    /**
     *      Creates a add block request and sends it to different blockchain
     *      nodes.
     *
     *      @throws TestFailed
     */
    protected boolean addBlock(int node, int chain_id, Block block)
            throws TestFailed
    {
        MessageSender sender = new MessageSender(CLIENT_TIMEOUT);

        AddBlockRequest add_request = new AddBlockRequest(
                chain_id, block);
        String add_uri = HOST_URI + nodes.get(node) + ADD_BLOCK_URI;

        StatusReply add_reply;
        try
        {
            add_reply = sender.post(add_uri, add_request,
                    StatusReply.class);
            if (add_reply == null) throw new Exception();
        }
        catch (Exception ex)
        {
            throw new TestFailed("AddBlock failed: " +
                    "No response or incorrect format");
        }
        return add_reply.getSuccess();
    }


    /**
     *      Creates a bloadcast block request and sends it to different blockchain
     *      nodes.
     *
     *      The receiving node then has to broadcast the block to all
     *      other nodes.
     *
     *      @throws TestFailed
     */
    protected boolean broadcastBlock(int node, int chain_id, Block block,
                                     String request_type) throws TestFailed
    {
        MessageSender sender = new MessageSender(CLIENT_TIMEOUT);

        BroadcastRequest request = new BroadcastRequest(
                chain_id, request_type, block);
        String uri = HOST_URI + nodes.get(node) + BROADCAST_URI;

        StatusReply reply;
        try
        {
            reply = sender.post(uri, request, StatusReply.class);
            if (reply == null) throw new Exception();
        }
        catch (Exception ex)
        {
            throw new TestFailed("BroadcastBlock failed: " +
                    "No response or incorrect format");
        }
        return reply.getSuccess();
    }


    /**
     *      Puts a node to sleep
     *
     *      @throws TestFailed
     */
    protected void disconnect(int node) throws TestFailed
    {
        MessageSender sender = new MessageSender(CLIENT_TIMEOUT);

        String uri = HOST_URI + nodes.get(node) + SLEEP_URI;
        SleepRequest request = new SleepRequest(SLEEP_TIMEOUT);

        StatusReply sleep_reply;
        try
        {
            sleep_reply = sender.post(uri, request, StatusReply.class);

            if (sleep_reply == null) throw new Exception();

            if (!sleep_reply.getSuccess()) throw new Exception();
        }
        catch (Exception ex)
        {
            throw new TestFailed("Error: Failed to disconnenct.");
        }
    }
}
