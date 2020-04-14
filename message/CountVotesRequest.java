/**
 *
 *      File Name -     CountVotesRequest.java
 *      Created By -    14736 Spring 2020 TAs
 *      Brief -
 *
 *          The request format for a CountVotes API call
 */

package message;

public class CountVotesRequest {

    private String return_votes_for;

    public CountVotesRequest(String countVotesFor) {
        this.return_votes_for = countVotesFor;
    }

    public String getCountVotesFor() {
        return this.return_votes_for;
    }
}
