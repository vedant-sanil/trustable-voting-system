/**
 *
 *      File Name -     CastVoteRequest.java
 *      Created By -    14736 Spring 2020 TAs
 *      Brief -
 *
 *          The request format for a CastVote API call
 */

package message;

public class CastVoteRequest {
    private String encrypted_vote_contents;
    private String encrypted_session_key;

    public CastVoteRequest(String enc_vote_contents, String sesh_key) {
        this.encrypted_vote_contents = enc_vote_contents ;
        this.encrypted_session_key = sesh_key;
    }

    public String getEncryptedVotes() {
        return this.encrypted_vote_contents;
    }

    public String getEncryptedSessionKey() {
        return this.encrypted_session_key;
    }
}
