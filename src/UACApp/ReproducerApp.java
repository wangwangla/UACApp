package UACApp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.voltdb.client.NoConnectionsException;
import org.voltdb.client.NullCallback;

public final class ReproducerApp extends ClientApp {

    public ReproducerApp(TheClientConfig config) {
        super(config);
    }

    private final List<BusinessLogic> m_bizLogics =
            Collections.synchronizedList(new ArrayList<>());
    private final Random m_random = new Random();
    private static final int DEFAULT_DATASIZE = 100000;

    private void runCommonProc() throws NoConnectionsException, IOException {
        int bizLogicIndex = m_random.nextInt(m_bizLogics.size());
        int partitionValue = m_random.nextInt(m_config.datasize == 0 ? DEFAULT_DATASIZE : m_config.datasize);
        m_client.callProcedure(new NullCallback(),
                CodeGeneration.COMMON_PROC_CLASS_NAME,
                partitionValue,                             // partition value
                m_bizLogics.get(bizLogicIndex).getName(),    // procedureName
                VoltTableParamFactory.getRandomVoltTable(),  // vtIn1
                VoltTableParamFactory.getRandomVoltTable(),  // vtIn2
                VoltTableParamFactory.getRandomVoltTable(),  // vtIn3
                VoltTableParamFactory.getRandomVoltTable(),  // vtIn4
                VoltTableParamFactory.getRandomVoltTable(),  // vtIn5
                VoltTableParamFactory.getRandomVoltTable()); // vtIn6
    }

    /**
     * Core benchmark code.
     * Connect. Initialize. Run the loop. Cleanup. Print Results.
     *
     * @throws Exception if anything unexpected happens.
     */
    @Override
    public void run() throws Exception {
        printTaskHeader("Setup & Initialization");

        printLog(String.format("Populating tables with inital data (%d rows)", m_config.datasize));
        DataGenerator.generate(m_client, m_config.datasize);

        printLog("Waiting for the common procedure and the business logics to initialize.");
        UACWork uacWork = new UACWork(m_config, m_bizLogics);
        Thread uacThread = new Thread(uacWork);
        uacThread.start();

        while (! uacWork.isCommonProcCreated()) {
            Thread.sleep(2000);
        }

        printTaskHeader("Start query workload.");

        // Run the query workload loop for the requested warm up time
        // The throughput may be throttled depending on client configuration
        printLog(String.format("Warming up for %d seconds...", m_config.warmup));

        final long warmupEndTime = System.currentTimeMillis() + (1000l * m_config.warmup);
        while (warmupEndTime > System.currentTimeMillis()) {
            runCommonProc();
        }

        resetStats();

        // Run the query workload loop for the requested duration
        // The throughput may be throttled depending on client configuration
        printLog("Running CommonProc with random parameters...");
        final long appEndTime = System.currentTimeMillis() + (1000l * m_config.duration);
        while (appEndTime > System.currentTimeMillis()) {
            runCommonProc();
        }

        // cancel periodic stats printing
        m_timer.cancel();

        uacThread.interrupt();

        // block until all outstanding txns return
        m_client.drain();

        // print the summary results
        printResults();

        // close down the client connections
        m_client.close();
    }

    /**
     * Main routine creates a benchmark instance and kicks off the run method.
     *
     * @param args Command line arguments.
     * @throws Exception if anything goes wrong.
     * @see {@link TheClientConfig}
     */
    public static void main(String[] args) throws Exception {
        // create a configuration from the arguments
        TheClientConfig config = new TheClientConfig();
        config.parse(ReproducerApp.class.getName(), args);

        ReproducerApp app = new ReproducerApp(config);
        app.run();
    }
}
