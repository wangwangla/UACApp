package UACApp;

import java.time.ZonedDateTime;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import org.voltdb.CLIConfig;
import org.voltdb.client.Client;
import org.voltdb.client.ClientConfig;
import org.voltdb.client.ClientFactory;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ClientStats;
import org.voltdb.client.ClientStatsContext;
import org.voltdb.client.ClientStatusListenerExt;
import org.voltdb.client.ProcedureCallback;

public abstract class ClientApp {

    // handy, rather than typing this out several times
    private static final String HORIZONTAL_RULE =
            "----------" + "----------" + "----------" + "----------" +
            "----------" + "----------" + "----------" + "----------";

    // validated command line configuration
    protected final TheClientConfig m_config;

    // Reference to the database connection we will use
    protected Client m_client;

    protected Timer m_timer;

    // Benchmark start time
    protected long m_benchmarkStartTS;

    // Statistics manager objects from the client
    protected final ClientStatsContext m_periodicStatsContext;
    protected final ClientStatsContext m_fullStatsContext;

    /**
     * Constructor for benchmark instance.
     * Configures VoltDB client and prints configuration.
     *
     * @param config Parsed & validated CLI options.
     */
    public ClientApp(TheClientConfig config) {
        m_config = config;
        m_client = null;
        try {
            m_client = getClientAndConnect(config);
        }
        catch (Exception e) {
            System.err.println("Could not connect to database: " + e.getMessage());
            System.exit(-1);
        }

        m_fullStatsContext = m_client.createStatsContext();
        m_periodicStatsContext = m_client.createStatsContext();
        printTaskHeader("Command Line Configuration");
        printLog(config.getConfigDumpString());
        if(config.latencyreport) {
            printLog("NOTICE: Option latencyreport is ON for async run, please set a reasonable ratelimit.\n");
        }
    }

    public abstract void run() throws Exception;

    protected void resetStats() {
        // reset the stats after warmup
        m_fullStatsContext.fetchAndResetBaseline();
        m_periodicStatsContext.fetchAndResetBaseline();

        // print periodic statistics to the console
        m_benchmarkStartTS = System.currentTimeMillis();
        schedulePeriodicStats();
    }

    /**
     * Create a Timer task to display performance data on the Vote procedure
     * It calls printStatistics() every displayInterval seconds
     */
    protected void schedulePeriodicStats() {
        m_timer = new Timer();
        TimerTask statsPrinting = new TimerTask() {
            @Override
            public void run() { printStatistics(); }
        };
        m_timer.scheduleAtFixedRate(statsPrinting,
                m_config.displayinterval * 1000,
                m_config.displayinterval * 1000);
    }

    /**
     * Prints a one line update on performance that can be printed
     * periodically during a benchmark.
     */
    public synchronized void printStatistics() {
        ClientStats stats = m_periodicStatsContext.fetchAndResetBaseline().getStats();
        long time = Math.round((stats.getEndTimestamp() - m_benchmarkStartTS) / 1000.0);

        StringBuilder statsBuilder = new StringBuilder();
        statsBuilder.append(String.format("%02d:%02d:%02d ", time / 3600, (time / 60) % 60, time % 60));
        statsBuilder.append(String.format("Throughput %d/s, ", stats.getTxnThroughput()));
        statsBuilder.append(String.format("Aborts/Failures %d/%d",
                stats.getInvocationAborts(), stats.getInvocationErrors()));

        if (m_config.latencyreport) {
            statsBuilder.append(String.format(", Avg/95%% Latency %.2f/%.2fms",
                                stats.getAverageLatency(),
                                stats.kPercentileLatencyAsDouble(0.95)));
        }
        printLog(statsBuilder.toString());
    }

    /**
     * Prints the results of the voting simulation and statistics
     * about performance.
     *
     * @throws Exception if anything unexpected happens.
     */
    public synchronized void printResults() throws Exception {
        ClientStats stats = m_fullStatsContext.fetch().getStats();
        printTaskHeader("Client Workload Statistics");
        printLog(String.format("Average throughput:            %,9d txns/sec", stats.getTxnThroughput()));
        if (m_config.latencyreport) {
            printLog(String.format("Average latency:               %,9.2f ms", stats.getAverageLatency()));
            printLog(String.format("10th percentile latency:       %,9.2f ms", stats.kPercentileLatencyAsDouble(.1)));
            printLog(String.format("25th percentile latency:       %,9.2f ms", stats.kPercentileLatencyAsDouble(.25)));
            printLog(String.format("50th percentile latency:       %,9.2f ms", stats.kPercentileLatencyAsDouble(.5)));
            printLog(String.format("75th percentile latency:       %,9.2f ms", stats.kPercentileLatencyAsDouble(.75)));
            printLog(String.format("90th percentile latency:       %,9.2f ms", stats.kPercentileLatencyAsDouble(.9)));
            printLog(String.format("95th percentile latency:       %,9.2f ms", stats.kPercentileLatencyAsDouble(.95)));
            printLog(String.format("99th percentile latency:       %,9.2f ms", stats.kPercentileLatencyAsDouble(.99)));
            printLog(String.format("99.5th percentile latency:     %,9.2f ms", stats.kPercentileLatencyAsDouble(.995)));
            printLog(String.format("99.9th percentile latency:     %,9.2f ms", stats.kPercentileLatencyAsDouble(.999)));

            printTaskHeader("System Server Statistics");
            printLog(String.format("Reported Internal Avg Latency: %,9.2f ms\n", stats.getAverageInternalLatency()));

            printTaskHeader("Latency Histogram");
            printLog(stats.latencyHistoReport());
        }
        m_client.writeSummaryCSV(stats, m_config.statsfile);
    }

    public static Client getClientAndConnect(TheClientConfig config) throws InterruptedException {
        ClientConfig clientConfig = new ClientConfig(config.user, config.password,
                new StatusListener(System.currentTimeMillis(), config));
        clientConfig.setMaxTransactionsPerSecond(config.ratelimit);
        clientConfig.setProcedureCallTimeout(0);
        Client client = ClientFactory.createClient(clientConfig);
        connect(client, config.servers);
        return client;
    }

    /**
     * Connect to a set of servers in parallel. Each will retry until
     * connection. This call will block until all have connected.
     *
     * @param servers A comma separated list of servers using the hostname:port
     * syntax (where :port is optional).
     * @throws InterruptedException if anything bad happens with the threads.
     */
    public static void connect(Client client, String servers) throws InterruptedException {
        printLogStatic("CLIENT", "Connecting to VoltDB...");

        String[] serverArray = servers.split(",");
        final CountDownLatch connections = new CountDownLatch(serverArray.length);

        // use a new thread to connect to each server
        for (final String server : serverArray) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    connectToOneServerWithRetry(client, server);
                    connections.countDown();
                }
            }).start();
        }
        // block until all have connected
        connections.await();
    }

    /**
     * Connect to a single server with retry. Limited exponential back-off.
     * No timeout. This will run until the process is killed if it's not
     * able to connect.
     *
     * @param server hostname:port or just host name (host name can be IP).
     */
    public static void connectToOneServerWithRetry(Client client, String server) {
        int sleep = 1000;
        while (true) {
            try {
                client.createConnection(server);
                break;
            }
            catch (Exception e) {
                System.err.printf("Connection failed - retrying in %d second(s).\n", sleep / 1000);
                try { Thread.sleep(sleep); } catch (Exception interruted) {}
                if (sleep < 8000) sleep += sleep;
            }
        }
        printLogStatic("CLIENT", String.format("Connected to VoltDB node at: %s.\n", server));
    }

    /**
     * Callback to handle the response to a stored procedure call.
     * Tracks response types.
     *
     */
    public static class TheClientCallback implements ProcedureCallback {
        @Override
        public void clientCallback(ClientResponse response) throws Exception {
            if (response.getStatus() != ClientResponse.SUCCESS) {
                System.err.println("Response not success: " + response.getStatusString());
            }
        }
    }

   /**
     * Uses included {@link CLIConfig} class to
     * declaratively state command line options with defaults
     * and validation.
     */
    public static class TheClientConfig extends CLIConfig {
        @Option(desc = "Interval for performance feedback, in seconds.")
        long displayinterval = 5;

        @Option(desc = "Number of inital rows of data in each table.")
        int datasize = 10000;

        @Option(desc = "Benchmark duration, in seconds.")
        int duration = 180;

        @Option(desc = "Warmup duration in seconds.")
        int warmup = 5;

        @Option(desc = "Comma separated list of the form server[:port] to connect to.")
        String servers = "localhost";

        @Option(desc = "Maximum TPS rate for benchmark.")
        int ratelimit = Integer.MAX_VALUE;

        @Option(desc = "Report latency for async benchmark run.")
        boolean latencyreport = false;

        @Option(desc = "Filename to write raw summary statistics to.")
        String statsfile = "";

        @Option(desc = "User name for connection.")
        String user = "";

        @Option(desc = "Password for connection.")
        String password = "";

        @Override
        public void validate() {
            if (duration <= 0) exitWithMessageAndUsage("duration must be > 0");
            if (warmup < 0) exitWithMessageAndUsage("warmup must be >= 0");
            if (displayinterval <= 0) exitWithMessageAndUsage("displayinterval must be > 0");
            if (ratelimit <= 0) exitWithMessageAndUsage("ratelimit must be > 0");
        }
    }

    /**
     * Provides a callback to be notified on node failure.
     * This example only logs the event.
     */
    public static class StatusListener extends ClientStatusListenerExt {

        long m_benchmarkStartTS;
        TheClientConfig m_config;

        StatusListener(long startTS, TheClientConfig theConfig) {
            m_benchmarkStartTS = startTS;
            m_config = theConfig;
        }

        @Override
        public void connectionLost(String hostname, int port, int connectionsLeft, DisconnectCause cause) {
            // if the benchmark is still active
            if ((System.currentTimeMillis() - m_benchmarkStartTS) < (m_config.duration * 1000)) {
                System.err.printf("Connection to %s:%d was lost.\n", hostname, port);
            }
        }
    }

    protected static void printLogStatic(String className, String msg, Object...args) {
        if (args != null) {
            msg = String.format(msg, args);
        }
        String header = String.format("%s [%s] ",
                ZonedDateTime.now().format(CodeGeneration.DT_FORMAT),
                className);

        System.out.println(String.format("%s%s",
                header, msg.replaceAll("\n", "\n" + header)));
    }

    protected void printLog(String msg, Object...args) {
        printLogStatic(this.getClass().getSimpleName(), msg, args);
    }

    protected void printTaskHeader(String taskString) {
        printLog(HORIZONTAL_RULE);
        printLog(taskString);
    }
}
