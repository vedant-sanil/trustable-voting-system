/**
 *
 *      File Name -     Config.java
 *      Created By -    14736 Spring 2020 TAs
 *      Brief -
 *
 *          This file sets up the configuration of all the components
 *          in the blockchain and voting system.
 *
 *          Note: Students might have to change the commands in this file to
 *          make the project run as per their setup.
 *          If you decide to use Java as your preferred programming language,
 *          you might NOT have to change this file
 */


package test;

/**
 * Configuration for tests
 *
 * <p>
 *     This file configures the ports and starting commands for blockchain nodes,
 *     voting server and voting client.
 * </p>
 */
public class Config {

    /* Ports of blockchain nodes */
    public static final int[] node_ports = {7771, 7772, 7773, 7774};

    /* Port of voting authority server */
    public static final int server_port = 8000;

    /* Ports of voting clients */
    public static final int[] client_ports = {9001, 9002, 9003};

    /* Path separator on running machine */
    private static final String separator = System.getProperty("path.separator", ":");

    /*
        Base command to start a blockchain node
        To-do : Change this string to start your blockchain node as required!
        Note: If you are doing the project in Java, you might not need to
        change the string below
    */
    public static final String node_config = String.format(
            "java -cp .%sgson-2.8.6.jar%scommons-codec-1.11.jar blockchain.Node",
            separator, separator);

    /*
        Base command to start a voting server
        To-do : Change this string to start your voting server as required!
        Note: If you are doing the project in Java, you might not need to
        change the string below
    */
    public static final String server_config = String.format(
            "java -cp .%sgson-2.8.6.jar%scommons-codec-1.11.jar server.Server",
            separator, separator);


    /*
        Base command to start a voting client
        To-do : Change this string to start your voting clients as required!
        Note: If you are doing the project in Java, you might not need to
        change the string below
    */
    public static final String client_config = String.format(
            "java -cp .%sgson-2.8.6.jar%scommons-codec-1.11.jar client.Client",
            separator, separator);


    /**
     * Get the commands to start blockchain nodes, by concatenating the base command
     * with required arguments, for example, "java MyClass 0 7001,7002,7003"
     *
     * @return array of command strings
     */
    public static String[] getNodeConfigs() {
        String peers = "";
        for (int i = 0; i < node_ports.length; i++) {
            peers += node_ports[i];
            peers += (i < node_ports.length - 1) ? "," : "";
        }

        String[] commands = new String[node_ports.length];
        for (int i = 0; i < node_ports.length; i++) {
            commands[i] = node_config + " " + i + " " + peers;
        }

        return commands;
    }


    /**
     *      The functions below are used only after the checkpoint
     */

    /**
     * Get the commands to start server nodes, by concatenating the base command
     * with required arguments, for example, "java MyClass 8000 7001"
     *
     * @return command string to start server
     */
    public static String getServerConfig() {
        return String.format("%s %d %d", server_config, server_port, node_ports[0]);
    }

    /**
     * Get the commands to start client nodes, by concatenating the base command
     * with required arguments, for example, "java MyClass 9001 8000 7001"
     *
     * @return command string to start client
     */
    public static String[] getClientConfig() {
        String[] commands = new String[client_ports.length];
        for (int i = 0; i < client_ports.length; i++) {
            commands[i] = String.format("%s %d %d %d",
                    client_config, client_ports[i], server_port, node_ports[0]);
        }
        return commands;
    }
}
