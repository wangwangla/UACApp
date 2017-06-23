package HuaweiUACApp;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import org.voltdb.client.Client;
import org.voltdb.utils.InMemoryJarfile;

import HuaweiUACApp.ClientApp.TheClientConfig;
import HuaweiUACApp.compiler.InMemoryClassLoader;
import HuaweiUACApp.compiler.InMemoryJavaCompiler;
import HuaweiUACApp.compiler.InMemoryJavaSourceCode;

public class UACWork implements Runnable {

    private static final String HORIZONTAL_RULE =
            "----------" + "----------" + "----------" + "----------" +
            "----------" + "----------" + "----------" + "----------";

    // Random number generator
    private final Random m_random = new Random();
    // List of all the loaded business logics so far.
    private final List<BusinessLogic> m_bizLogics;
    // List of business logics pending for UAC process.
    private final List<BusinessLogic> m_bizLogicsUACPending;
    // Business logic factory.
    private final BusinessLogicFactory m_bizLogicFactory;
    // Keep the list of dropped business logics in the current round of UAC.
    private final List<String> m_droppedLogicNames;

    private InMemoryJavaCompiler m_compiler;
    private Client m_client;
    private AtomicBoolean m_commonProcCreated = new AtomicBoolean(false);

    UACWork(TheClientConfig config, List<BusinessLogic> bizLogics) {
        m_client = null;
        m_bizLogics = bizLogics;
        m_bizLogicsUACPending = new ArrayList<>();
        m_droppedLogicNames = new ArrayList<>();
        m_bizLogicFactory = new BusinessLogicFactory();
        try {
            m_client = ClientApp.getClientAndConnect(config);
        }
        catch (Exception e) {
            System.err.println("Could not connect to the database: " + e.getMessage());
            System.exit(-1);
        }
    }

    protected boolean isCommonProcCreated() {
        return m_commonProcCreated.get();
    }

    @Override
    public void run() {
        printTaskHeader("UACThread started.");
        while (true) {
            try {
                randomlyDropLogics();
                randomlyGenerateNewLogics();
                compileAll();
                execUpdateClasses();
                printTaskHeader(String.format("UAC task complete, wait for %d seconds to start the next one.",
                        CodeGeneration.UAC_INTERVAL / 1000));
                Thread.sleep(CodeGeneration.UAC_INTERVAL);
            } catch (Exception e) {
                try {
                    m_client.close();
                } catch (InterruptedException e1) { }
                printLog("The UAC thread is exiting.");
                break;
            }
        }
    }

    private void randomlyGenerateNewLogics() {
        // Generate at least one business logic.
        int numOfBusinessLogicsToAdd = m_random.nextInt(CodeGeneration.MAX_BIZ_LOGIC_BATCH_SIZE) + 1;

        printTaskHeader(String.format(
                "Generating %d business logics...\n",
                numOfBusinessLogicsToAdd));

        for (int i = 0; i < numOfBusinessLogicsToAdd; i++) {
            BusinessLogic newBizLogic = m_bizLogicFactory.getNewRandomBusinessLogic();
            m_bizLogicsUACPending.add(newBizLogic);
            printLog(newBizLogic.toString());
        }
    }

    private void randomlyDropLogics() {
        printTaskHeader(String.format(
                "Removing business logics with an independent probablity of %d%%...",
                CodeGeneration.PERCENTAGE_BE_DROPPED));

        boolean hasLogicRemoved = false;
        Iterator<BusinessLogic> iterator = m_bizLogics.iterator();
        m_droppedLogicNames.clear();
        while (iterator.hasNext()) {
            BusinessLogic bizLogic = iterator.next();
            if (m_random.nextInt(100) + 1 <= CodeGeneration.PERCENTAGE_BE_DROPPED) {
                printLog(String.format(
                        "Remove business logic %s", bizLogic.getName()));
                iterator.remove();
                m_droppedLogicNames.add(CodeGeneration.PACKAGE_PATH + bizLogic.getName());
                hasLogicRemoved = true;
            }
        }
        if (! hasLogicRemoved) {
            printLog("No business logic is being removed.");
        }
    }

    private void compileAll() {
        printTaskHeader("Compiling...");

        m_compiler = new InMemoryJavaCompiler();
        // Add the "weird library"
        m_compiler.addInMemoryJavaSource(CodeGeneration.WEIRD_LIBRARY_SOURCE);
        // Add common procedure.
        InMemoryJavaSourceCode commonProcSource =
                CommonProcedureFactory.getCommonProcJavaSource(m_bizLogics, m_bizLogicsUACPending);
        m_compiler.addInMemoryJavaSource(commonProcSource);
        // Add the abstract business logic class.
        m_compiler.addInMemoryJavaSource(CodeGeneration.ABSTRACT_BIZ_LOGIC_SOURCE);
        // Add all the business logics.
        for (BusinessLogic bizProc : m_bizLogics) {
            m_compiler.addInMemoryJavaSource(bizProc.getJavaSourceCode());
        }
        for (BusinessLogic bizProc : m_bizLogicsUACPending) {
            m_compiler.addInMemoryJavaSource(bizProc.getJavaSourceCode());
        }

        if (m_compiler.compile()) {
            printLog("Successful.");
        }
        else {
            printLog(m_compiler.getDiagnostics());
        }
    }

    // Load the new classes and remove the chosen old classes.
    private void execUpdateClasses() throws Exception {
        printTaskHeader("Executing @UpdateClasses...");
        // Build a VoltDB in-memory Jar file.
        InMemoryJarfile jarFile =
                VoltInMemoryJarFileBuilder.buildFromInMemoryClassLoader(
                        (InMemoryClassLoader)m_compiler.getClassLoader());
        m_client.callProcedure("@UpdateClasses",
                jarFile.getFullJarBytes(), // new classes
                String.join(",", m_droppedLogicNames)); // classes to remove
        if (! m_commonProcCreated.get()) {
            printLog(String.format("Create procedure \"%s\"...", CodeGeneration.COMMON_PROC_CLASS_NAME));
            m_client.callProcedure("@AdHoc",
                    String.format("DROP PROCEDURE %s IF EXISTS;",
                            CodeGeneration.COMMON_PROC_CLASS_NAME));
            m_client.callProcedure("@AdHoc",
                    String.format("CREATE PROCEDURE PARTITION ON TABLE TABLE0 COLUMN PK FROM CLASS %s%s;",
                            CodeGeneration.PACKAGE_PATH, CodeGeneration.COMMON_PROC_CLASS_NAME));
            m_commonProcCreated.set(true);
        }
        // Now new business logics are updated successfully, we can make them visible to the reproducer app
        // by adding them to the list.
        m_bizLogics.addAll(m_bizLogicsUACPending);
        m_bizLogicsUACPending.clear();
        printLog("@UpdateClasses succeeded.");
    }

    protected static void printLogStatic(String className, String msg) {
        String header = String.format("%s [%s] ",
                ZonedDateTime.now().format(CodeGeneration.DT_FORMAT),
                className);

        System.out.println(String.format("%s%s", header, msg));
    }

    protected void printLog(String msg) {
        printLogStatic(this.getClass().getSimpleName(), msg);
    }

    protected void printTaskHeader(String taskString) {
        printLog(HORIZONTAL_RULE);
        printLog(taskString);
    }
}
