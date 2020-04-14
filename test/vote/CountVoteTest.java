/**
 *
 *      File Name -     CountVoteTest.java
 *      Created By -    14736 Spring 2020 TAs
 *      Brief -
 *
 *          This test initializes candidates, starts the voting process,
 *          and then performs a voting round.
 *
 *          The test suite keeps track of the votes cast by the voters,
 *          and checks if the vote blockchain provides a consistent view of the
 *          voting that happened.
 *
 *          For more details on the exceptions that must be handled -
 *              See the 'CountVotes' call in API/VotingServer.md
 */


package test.vote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;

import test.util.TestFailed;

import message.*;


/**
 *      Count the votes cast per election
 */
public class CountVoteTest extends VoteTest {


    /** Test notice. */
    public static final String notice =
            "Testing vote count on server and blockchain";

    int nCandidates;
    List<String> candidates = new ArrayList<>();

    Map<String, Integer> votes = new HashMap<>();

    /** Performs the tests.
     @throws TestFailed If any of the tests fail.
     */
    @Override
    protected void perform() throws TestFailed
    {
        // Prepare the test
        prepareCandidates();
        startVoting();

        // Perform the CountVote tests
        testServerVoteCount();
        testBlockchainVoteCount();
    }


    /**
     *      Creates a random list of eligible candidates
     *
     *      @throws TestFailed
     */
    private void prepareCandidates() throws TestFailed
    {
         List<String> eligible_candidates = getEligibleCandidates();
         Collections.shuffle(eligible_candidates);

         Random rand = new Random();

         nCandidates = 1 + rand.nextInt(client_ports.size());
         candidates = eligible_candidates.subList(0, nCandidates);

         for (String candidate : candidates)
         {
             boolean success = becomeOneCandidate(candidate);
             if (!success)
             {
                 throw new TestFailed("BecomeCandidateRequest failed: " +
                         "Expect success, returned failure.");
             }

             votes.put(candidate, 0);
         }
    }


    /**
     *      Start the voting process and keep track of the votes cast in
     *      a local map
     *
     *      @throws TestFailed
     */
    private void startVoting() throws TestFailed
    {
        Random rand = new Random();

        for (int i = 0; i < client_ports.size(); i++)
        {
            int voter_port = client_ports.get(i);

            int candidate_index = rand.nextInt(nCandidates);
            String candidate = candidates.get(candidate_index);

            boolean success = voteForCandidate(voter_port, candidate);
            if (!success)
            {
                throw new TestFailed("CastVoteRequest failed: " +
                        "Expect success, returned failure.");
            }

            votes.put(candidate, votes.getOrDefault(candidate, 0) + 1);
        }
    }

    /**
     * Check whether the voting server returns correct vote counts
     *
     * @throws TestFailed
     */
    private void testServerVoteCount() throws TestFailed
    {
        String uri = HOST_URI + server_port + COUNT_VOTES_URI;

        for (String candidate : candidates)
        {
            CountVotesRequest request = new CountVotesRequest(candidate);

            CountVotesReply reply;
            try
            {
                reply = sender.post(uri, request, CountVotesReply.class);
                if (reply == null) throw new Exception();
            }
            catch (Exception ex)
            {
                throw new TestFailed("CountVotesRequest failed: " +
                        "No response or incorrect format.");
            }

            if (!reply.getStatus())
            {
                throw new TestFailed("CountVotesRequest failed: " +
                        "Expect success, returned failure.");
            }

            if (reply.getVoteCount() != votes.getOrDefault(candidate, 0))
            {
                throw new TestFailed("Test server vote count failed: " +
                        "Inconsistent vote counts.");
            }
        }

        // Try to get vote count for an invalid candidate
        CountVotesRequest request = new CountVotesRequest("mike-bloomberg");
        CountVotesReply reply;

        try {
            reply = sender.post(uri, request, CountVotesReply.class);
            if (reply == null) throw new Exception();
        }
        catch (Exception ex) {
            throw new TestFailed("CountVotesRequest failed: " +
                    "No response or incorrect format.");
        }

        if (reply.getStatus())
        {
            throw new TestFailed("Invalid candidate's votes counted: " +
                    "Expect failure, returned success.");
        }
    }

    /**
     * Check whether the votes on the blockchain are correct
     *
     * @throws TestFailed
     */
    private void testBlockchainVoteCount() throws TestFailed
    {
        List<Block> blocks = getBlockchain(VOTECHAIN_ID);

        Map<String, Integer> chain_votes = new HashMap<>();
        for (String candidate : candidates)
        {
            chain_votes.put(candidate, 0);
        }

        int chain_length = blocks.size();
        for (int i = 1; i < chain_length; ++i)
        {
            Block block = blocks.get(i);

            Map<String, String> data = block.getData();
            String candidate = data.getOrDefault("vote", "");

            if (candidate.isEmpty())
            {
                throw new TestFailed("Error in vote chain data: " +
                        "Missing field for voted candidate.");
            }

            chain_votes.put(candidate, chain_votes.getOrDefault(candidate, 0) + 1);
        }

        if (!chain_votes.equals(votes))
        {
            throw new TestFailed("Test blockchain vote count failed: " +
                    "Inconsistent vote counts.");
        }
    }
}
