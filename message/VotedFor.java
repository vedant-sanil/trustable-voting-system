/**
 *
 *      File Name -     VotedFor.java
 *      Created By -    Vedant Sanil, Sharath Chellappa
 *      Brief -
 *
 *          The class format wrapping around data voted for
 */

package message;

public class VotedFor {
    private String user_name;
    private String voted_for;

    public VotedFor(String user_name, String voted_for) {
        this.user_name = user_name;
        this.voted_for = voted_for;
    }

    public String getUser_name() {
        return user_name;
    }

    public String getVoted_for() {
        return voted_for;
    }
}