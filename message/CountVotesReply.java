/**
 *
 *      File Name -     CountVotesReply.java
 *      Created By -    14736 Spring 2020 TAs
 *      Brief -
 *
 *          The response format for a CountVotes API call
 */

package message;


public class CountVotesReply {
    private boolean success;
    private int vote_count;

    public CountVotesReply(boolean success, int votes) {
        this.success = success;
        this.vote_count = votes;
    }

    public boolean getStatus() { return this.success; }

    public int getVoteCount() {
        return this.vote_count;
    }
}
