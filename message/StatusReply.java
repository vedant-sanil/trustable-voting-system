/**
*
*      File Name -     StatusReply.java
*      Created By -    14736 Spring 2020 TAs
*      Brief -
*
*          The response format for the
*               BroadcastRequest and GetServerStatus API calls
*/

package message;

public class StatusReply {
    private boolean success;
    private String info;

    public StatusReply (boolean success) {
        this.success = success;
        this.info = "";
    }

    public StatusReply (boolean success, String info) {
        this.success = success;
        this.info = info;
    }

    public boolean getSuccess() { return success; }

    public String getInfo() { return info; }
}
