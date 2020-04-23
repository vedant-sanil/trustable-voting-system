/**
 *
 *      File Name -     VoteTest.java
 *      Created By -    14736 Spring 2020 TAs
 *      Brief -
 *
 *          This file contains the helper/utility functions required to
 *          run all the tests under "/test/vote/"
 *
 */


package test.vote;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import test.Config;
import test.util.Test;
import test.util.TestFailed;

import lib.MessageSender;
import message.*;



/**
 *      The base testing suite providing helper functions all the vote tests
 */
abstract class VoteTest extends Test
{
    protected static final String HOST_NAME = "127.0.0.1";
    protected static final String HOST_URI = "http://127.0.0.1:";

    // blockchain apis
    protected static final String GET_CHAIN_URI = "/getchain";
    protected static final String MINE_BLOCK_URI = "/mineblock";
    protected static final String ADD_BLOCK_URI = "/addblock";
    protected static final String BROADCAST_URI = "/broadcast";
    protected static final String SLEEP_URI = "/sleep";

    // server apis
    protected static final String SERVER_STATUS_URI = "/checkserver";
    protected static final String BECOME_CANDIDATE_URI = "/becomecandidate";
    protected static final String GET_CANDIDATES_URI = "/getcandidates";
    protected static final String CAST_VOTE_URI = "/castvote";
    protected static final String COUNT_VOTES_URI = "/countvotes";

    // client apis
    protected static final String START_VOTE_URI = "/startvote";

    protected static final int CLIENT_TIMEOUT = 5;

    protected static final int KEYCHAIN_ID = 1;
    protected static final int VOTECHAIN_ID = 2;
    protected static final int[] CHAIN_IDS = {KEYCHAIN_ID, VOTECHAIN_ID};

    protected Process server_process;
    protected List<Process> node_processes = new ArrayList<>();
    protected List<Process> client_processes = new ArrayList<>();

    protected int server_port;
    protected List<Integer> node_ports = new ArrayList<>();
    protected List<Integer> client_ports = new ArrayList<>();

    protected MessageSender sender = new MessageSender(CLIENT_TIMEOUT);

    abstract protected void perform() throws TestFailed;


    /**
     *      Spawns processes that start blockchain nodes/voting server/clients.
     *
     *      @throws TestFailed
     */
    protected Process spawnProcess(String command) throws Throwable
    {
        Process process = null;
        try
        {
            // System.out.println(command);
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
     *      Performs a simple ping request to the clients and server.
     *      This is used to give the voting server and clients enough time to
     *      add their public key to the blockchain on bootup, before running
     *      the tests
     *
     */
    protected void ping(int port)
    {
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


    /**
     *      Initializes the testing environment for the vote tests
     *
     *      @throws TestFailed
     */
    protected void initialize() throws TestFailed
    {
        // Configure testing ports
        for (int i = 0; i < Config.node_ports.length; i++) {
            node_ports.add(Config.node_ports[i]);
        }

        server_port = Config.server_port;

        for (int i = 0; i < Config.client_ports.length; i++) {
            client_ports.add(Config.client_ports[i]);
        }

        // Start blockchain nodes.
        String nodeConfigs[] = Config.getNodeConfigs();
        try
        {
            for (int i = 0; i < nodeConfigs.length; i++) {
                String cmd = nodeConfigs[i];
                Process server = spawnProcess(cmd);
                node_processes.add(server);
            }
        }
        catch (Throwable t)
        {
            throw new TestFailed("unable to start blockchain node");
        }

        // Attempt to connect blockchain nodes
        for (int port : node_ports)
        {
            ping(port);
        }

        // Spawn a process to run the authority server
        try {
            server_process = spawnProcess(Config.getServerConfig());

            // Attempt to connect server
            ping(server_port);
        }
        catch (InterruptedException ex) {}
        catch (Throwable t) {
            throw new TestFailed("unable to start authority server node");
        }


        // Spawn processes to run the voting client_processes
        String clientConfigs[] = Config.getClientConfig();
        try
        {
            for (int i = 0; i < clientConfigs.length; i++) {
                String cmd = clientConfigs[i];
                Process server = spawnProcess(cmd);
                client_processes.add(server);

                // Attempt to connect client
                ping(client_ports.get(i));
            }
        }
        catch (InterruptedException ex) {}
        catch (Throwable t)
        {
            throw new TestFailed("unable to start client");
        }
    }

    /**
     *      Stops all the servers when the test ends
     */
    @Override
    protected void clean()
    {
        for (int i = 0; i < node_processes.size(); i++)
        {
            Process server = node_processes.get(i);

            if(server != null)
            {
                kill(server.toHandle());

                try
                {
                    server.waitFor();
                }
                catch(InterruptedException e) { }

                node_processes.set(i, null);
            }
        }

        node_processes = new ArrayList<>();

        // Destroy the authority server
        if (server_process != null) {
            kill(server_process.toHandle());
            try
            {
                server_process.waitFor();
            }
            catch(InterruptedException e) { }
        }

        for (int i = 0; i < client_processes.size(); i++)
        {
            Process server = client_processes.get(i);

            if(server != null)
            {
                kill(server.toHandle());

                try
                {
                    server.waitFor();
                }
                catch(InterruptedException e) { }

                client_processes.set(i, null);
            }
        }

        client_processes = new ArrayList<>();
    }

    protected void kill(ProcessHandle handle) {
        handle.descendants().forEach(this::kill);
        handle.destroy();
    }


    /**
     *      Gets the list of blocks present in the public key and voting
     *      blockchains depending on the chain_id parameter
     *
     *      @throws TestFailed
     */
    protected List<Block> getBlockchain(int chain_id) throws TestFailed
    {
        String getChainURI = HOST_URI + node_ports.get(0) + GET_CHAIN_URI;
        GetChainRequest request = new GetChainRequest(chain_id);

        GetChainReply keyChain;
        try
        {
            keyChain = sender.post(getChainURI, request, GetChainReply.class);
            if (keyChain == null) throw new Exception();
        }
        catch (Exception e)
        {
            throw new TestFailed("Get Chain Request failed: " +
                    "No response or incorrect format.");
        }

        return keyChain.getBlocks();
    }



    /**
     *      Gets the list of clients which could potentially
     *      do the BecomeCandidate API call to become a client.
     *      This is essentially the list of nodes which have added
     *      their public keys to the public key blockchain
     *
     *      @throws TestFailed
     */
    protected List<String> getEligibleCandidates() throws TestFailed
    {
        List<Block> blocks = getBlockchain(KEYCHAIN_ID);
        List<String> eligible_candidates = new ArrayList<>();

        for (int i = 2; i < blocks.size(); i++)
        {
            Block block = blocks.get(i);
            String userName = block.getData().getOrDefault("user_name", "");

            if (!userName.isEmpty()) {
                eligible_candidates.add(userName);
            }
        }
        return eligible_candidates;
    }



    /**
     *      Gets the list of candidates contesting in the election
     *
     *      @throws TestFailed
     */
    protected List<String> getCandidates() throws TestFailed
    {
        String requestURI = HOST_URI + server_port + GET_CANDIDATES_URI;

        GetCandidatesReply reply;
        try
        {
            reply = sender.post(requestURI, "", GetCandidatesReply.class);
            if (reply == null) throw new Exception();
        }
        catch (Exception e)
        {
            throw new TestFailed("Get Candidate Request failed: " +
                    "No response or incorrect format.");
        }

        return reply.getCandidates();
    }


    /**
     *      Sends the BecomeCandidate request to the server
     *      on behalf of the client
     *
     *      @throws TestFailed
     */
    protected boolean becomeOneCandidate(String candidate) throws TestFailed
    {
        String candidateURI = HOST_URI + server_port + BECOME_CANDIDATE_URI;
        BecomeCandidateRequest requestBody = new BecomeCandidateRequest(candidate);

        StatusReply response;
        try
        {
            response = sender.post(candidateURI, requestBody, StatusReply.class);
            if (response == null) throw new Exception();
        } catch (Exception e) {
            throw new TestFailed("Become Candidate Request failed: " +
                    "No response or incorrect format.");
        }

        return response.getSuccess();
    }


    /**
     *      Create the StartVote message that is sent from the test to
     *      the client node.
     *      The client node must then create  the castVote packet accordingly
     *      and send it to the voting server
     *
     *      @throws TestFailed
     */
    protected boolean voteForCandidate(int voter_port, String candidate) throws TestFailed
    {
        String requestURI = HOST_URI + voter_port + START_VOTE_URI;

        StartVoteRequest requestBody = new StartVoteRequest(2, candidate);

        StatusReply reply;
        try
        {
            reply = sender.post(requestURI, requestBody, StatusReply.class);
            if (reply == null) throw new Exception();
        }
        catch (Exception e)
        {
            throw new TestFailed("Start Vote Request failed: " +
                    "No response or incorrect format.");

        }

        return reply.getSuccess();
    }
}
