/**
 *
 *      File Name -     VoteContents.java
 *      Created By -    Vedant Sanil, Sharath Chellappa
 *      Brief -
 *
 *          The class format of vote contents
 */

package message;

public class VoteContents {
    private int chain_id;
    private String user_name;
    private String encrypted_vote;

    public VoteContents(int chain_id, String user_name, String encrypted_vote) {
        this.chain_id = chain_id;
        this.user_name = user_name;
        this.encrypted_vote = encrypted_vote;
    }

    public int getChain_id() {
        return chain_id;
    }

    public String getEncrypted_vote() {
        return encrypted_vote;
    }

    public String getUser_name() {
        return user_name;
    }
}