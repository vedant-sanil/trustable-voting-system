/**
 *
 *      File Name -     GetCandidatesReply.java
 *      Created By -    14736 Spring 2020 TAs
 *      Brief -
 *
 *          The response format for a GetCandidates API call
 */

package message;

import java.util.List;


public class GetCandidatesReply {
    private List<String> candidates;

    public GetCandidatesReply(List<String> candidatesList) {
        this.candidates = candidatesList;
    }

    public List<String> getCandidates() {
         return candidates;
     }
}
