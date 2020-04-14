/**
 *
 *      File Name -     BecomeCandidateRequest.java
 *      Created By -    14736 Spring 2020 TAs
 *      Brief -
 *
 *          The request format for a BecomeCandidateRequest API call
 */

package message;

public class BecomeCandidateRequest {
    private String user_name;

    public BecomeCandidateRequest(String username) {
        this.user_name = username;
    }

    public String getCandidateName() { return this.user_name; }
}
