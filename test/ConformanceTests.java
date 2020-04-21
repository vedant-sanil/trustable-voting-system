/**
 *
 *      File Name -     ConformanceTests.java
 *      Created By -    14736 Spring 2020 TAs
 *      Brief -
 *
 *          This file is the test suite driver.
 *
 *          Commands to run (Also present in README.md):
 *          To run tests for checkpoint     - make checkpoint
 *          To run tests for the final      - make test
 *          To run the speed test           - make speed
 */

package test;

import test.util.Series;
import test.util.SeriesReport;
import test.util.Test;

public class ConformanceTests
{
    private static final int timeout = 120;

    /** Runs the tests.

     @param args Test arguments
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void main(String[] args)
    {
        // Create the test list, the series object, and run the test series.
        Class<? extends Test>[] tests =
                new Class[] {
                test.blockchain.MessageTest.class,
                test.blockchain.MiningTest.class,
                test.blockchain.ConsensusTest.class,
                test.blockchain.SpeedTest.class,
                test.vote.RegistrationTest.class,
                test.vote.CandidacyTest.class,
                test.vote.CastVoteTest.class,
                test.vote.CountVoteTest.class,
        };


        String testname = (args.length > 0) ? args[0] : "";

        if (testname.equals("checkpoint"))
        {
            tests = new Class[] {
                test.blockchain.MessageTest.class,
                test.blockchain.MiningTest.class,
                test.blockchain.ConsensusTest.class,
                test.blockchain.SpeedTest.class,
            };
        }
        else if (testname.equals("speed"))
        {
            // speed test only
            tests = new Class[] { test.blockchain.SpeedTest.class };
        }

        Series series = new Series(tests);
        SeriesReport report = series.run(timeout, System.out);

        // Print the report and exit with an appropriate exit status.
        report.print(System.out);
        System.exit(report.successful() ? 0 : 2);
    }
}
