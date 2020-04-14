/**
 *
 *      File Name -     StartVoteRequest.java
 *      Created By -    14736 Spring 2020 TAs
 *      Brief -
 *
 *          The request format for a StartVote API call
 */

package message;


public class StartVoteRequest {

	int chain_id;
	String vote_for;

    public StartVoteRequest(int chain_id, String vote_for) {

    	this.chain_id = chain_id;
		this.vote_for = vote_for;
    }

	public int getChainId() {
		return this.chain_id;
	}


	public String getVoteFor() {
		return this.vote_for;
	}
}
