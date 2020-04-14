/**
 *
 *      File Name -     CastVoteTest.java
 *      Created By -    14736 Spring 2020 TAs
 *      Brief -
 *
 *          This test initializes candidates and performs the voting process,
 *
 *          The test requires you to implement:
 *
 *              StartVote in the voting clients - This is used by the to
 *              ask a client to vote for a particular candidate
 *
 *              CastVote in the voting server - This call is used to
 *              send a client's vote to the authority voting server.
 *
 *          For more details on the exceptions that must be handled -
 *              See the 'CastVote' call in API/VotingServer.md and
 *                  'StartVote' call in API/VotingClient.md
 */


package test.vote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import test.util.TestFailed;

import message.*;

/**
 *  Test the CastVote API call
 */
public class CastVoteTest extends VoteTest
{
    /** Test notice. */
    public static final String notice =
            "Testing voting process";

    int nCandidates;
    List<String> candidates = new ArrayList<>();

    /** Performs the tests.

     @throws TestFailed If any of the tests fail.
     */
    @Override
    protected void perform() throws TestFailed
    {
        // Prepare the test
        prepareCandidates();

        // Perform the CastVote tests
        testMalformedVote();
        testCorrectAndFakeVotes();
    }


    /**
     *      Prepares candidates for the election
     *
     *      @throws TestFailed
     */
    private void prepareCandidates() throws TestFailed
    {
        List<String> eligible_candidates = getEligibleCandidates();
        Collections.shuffle(eligible_candidates);


        Random rand = new Random();

        nCandidates = 1 + rand.nextInt(eligible_candidates.size());

        candidates = eligible_candidates.subList(0, nCandidates);

        for (String candidate : candidates)
        {
            boolean success = becomeOneCandidate(candidate);
            if (!success)
            {
                throw new TestFailed("BecomeCandidateRequest failed: " +
                        "Expect success, returned failure.");
            }
        }
    }


    /**
     *      Test to see if a malformed/unencrypted vote is rejected by the
     *      server
     *
     *      @throws TestFailed
     */
    private void testMalformedVote() throws TestFailed
    {
        String requestURI = HOST_URI + server_port + CAST_VOTE_URI;

        Random rand = new Random();
        int candidate_index = rand.nextInt(nCandidates);

        // Send an un-encrypted vote directly to server
        String incorrect_vote = candidates.get(candidate_index);
        CastVoteRequest castVoteRequest = new CastVoteRequest(incorrect_vote, incorrect_vote);

        StatusReply statusReply;
        try
        {
            statusReply = sender.post(requestURI, castVoteRequest,
                    StatusReply.class);
            if (statusReply == null) throw new Exception();
        }
        catch (Exception e)
        {
            throw new TestFailed("CastVoteRequest failed: " +
                    "No response or incorrect format.");
        }

        if (statusReply.getSuccess())
        {
            throw new TestFailed("Test incorrect votes failed: " +
                    "Expect failure, return success.");
        }
    }


    /**
     *      Test to see if proper and improper vote requests
     *      are handled properly by the server
     *
     *      @throws TestFailed
     */
    private void testCorrectAndFakeVotes() throws TestFailed
    {
        Random rand = new Random();

        for (int voter_port : client_ports)
        {
            int candidate_index = rand.nextInt(nCandidates);
            String candidate = candidates.get(candidate_index);


            // ask a client to vote for an invalid candidate
            boolean invalid_vote_success = voteForCandidate(voter_port, "covid-19");
            if (invalid_vote_success)
            {
                throw new TestFailed("Invalid candidate: " +
                "Expect failure, return success.");
            }

            // ask a client to vote for candidate
            boolean first_success = voteForCandidate(voter_port, candidate);
            if (!first_success)
            {
                throw new TestFailed("Test correct votes failed: " +
                        "Expect success, return failure.");
            }

            boolean second_success = voteForCandidate(voter_port, candidate);
            if (second_success)
            {
                throw new TestFailed("Test correct votes failed: " +
                "Expect failure in second time voting.");
            }
        }
    }
}
