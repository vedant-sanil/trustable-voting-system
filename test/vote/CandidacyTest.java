/**
 *
 *      File Name -     CandidacyTest.java
 *      Created By -    14736 Spring 2020 TAs
 *      Brief -
 *
 *          This test checks if voting clients can successfully
 *          become candidates in the election
 *
 *          Note that any client can attempt to become a candidate for an
 *          election cycle.
 *
 *          For more details on the exceptions that must be handled -
 *              See the 'BecomeCandidate' and 'GetCandidates' calls
 *              in API/VotingServer.md
 */


package test.vote;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

import test.util.TestFailed;


/**
 *      Test the functionality of the /becomecandidate API call
 */
public class CandidacyTest extends VoteTest
{
    /** Test notice. */
    public static final String notice =
            "Testing candidacy management";

    Set<String> added_candidates = new HashSet<>();

    /** Performs the tests.

     @throws TestFailed If any of the tests fail.
     */
    @Override
    protected void perform() throws TestFailed
    {
        testEligibleCandidates();
        testIneligibleCandidates();
    }

    /**
     *      Test to see if an eligible client can become a candidate
     *
     *      A client which is already a candidate can't become a candidate again
     *
     *      @throws TestFailed
     */
    private void testEligibleCandidates() throws TestFailed
    {
        List<String> eligible_candidates = getEligibleCandidates();

        for (String candidate : eligible_candidates)
        {
            boolean first_success = becomeOneCandidate(candidate);
            if (!first_success)
            {
                throw new TestFailed("BecomeCandidateRequest failed: " +
                        "Expect success, returned failure.");
            }
            boolean second_success = becomeOneCandidate(candidate);
            if (second_success)
            {
                throw new TestFailed("BecomeCandidateRequest failed: " +
                        "Expect failure for second time becoming candidate.");
            }

            added_candidates.add(candidate);

            List<String> candidatesList = getCandidates();
            Set<String> candidates = new HashSet<>(candidatesList);

            if (!added_candidates.equals(candidates))
            {
                throw new TestFailed("BecomeCandidateRequest failed: " +
                        "Inconsistent candidates on server with requests.");
            }
        }
    }


    /**
     *      Only clients who are part of the blockchain system
     *      can become candidates.
     *
     *      @throws TestFailed
     */
    private void testIneligibleCandidates() throws TestFailed
    {
        String candidate = "invalid_candidate";
        boolean success = becomeOneCandidate(candidate);

        if (success)
        {
            throw new TestFailed("BecomeCandidateRequest failed: " +
                    "Expect failure, returned success.");
        }

    }
}
