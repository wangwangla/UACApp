package HuaweiUACApp.procedures;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltProcedure.VoltAbortException;
import org.voltdb.VoltTable;

public abstract class AbstractBusinessLogic {

    protected VoltProcedure m_commonProc = null;

    protected abstract List<SQLStmt> getQueriesToQueue();

    private static final List<Class<?>> OBJECT_TYPES =
            Arrays.asList(
                    Integer.class, Double.class, String.class, StringBuilder.class,
                    Long.class, Short.class, Byte.class, Boolean.class, Float.class);

    public final VoltTable[] run(long partitionValue,
                                 VoltTable vtIn1, VoltTable vtIn2,
                                 VoltTable vtIn3, VoltTable vtIn4,
                                 VoltTable vtIn5, VoltTable vtIn6) {
        if (m_commonProc == null) {
            throw new VoltAbortException("Business logics have to be run from the CommonProc.");
        }

        List<SQLStmt> queriesToQueue = getQueriesToQueue();
        List<VoltTable> tables = Arrays.asList(vtIn1, vtIn2, vtIn3, vtIn4, vtIn5, vtIn6);

        // Call a weird function that takes about 1000 objects as parameter.
        // This is what Huawei did and it is suspected that this can corrupt JVM stack???
        Object[] params = new Object[1000];
        for (int i = 0; i < 1000; i++) {
            try {
                params[i] = OBJECT_TYPES.get(i % OBJECT_TYPES.size()).newInstance();
            }
            catch (IllegalAccessException | InstantiationException e) {
                // Nothing we can do about it, leave it as NULL.
            }
            SomeWeirdLibrary.weirdMethod(params);
        }

        int currentTableId = 0;
        VoltTable vt = tables.get(currentTableId);
        if (vt == null) {
            // If we test from sqlcmd, we probably will pass all NULLs for VoltTables.
            // In this case, simply queue all the queries, once for each.
            for (int queryId = 0; queryId < queriesToQueue.size(); queryId++) {
                m_commonProc.voltQueueSQL(queriesToQueue.get(queryId), partitionValue);
            }
        }
        else {
            // If we test using the app, then try to make use of the values in the VoltTables.
            int queryId = 0;
            while (queryId < queriesToQueue.size()) {
                if (! vt.advanceRow()) {
                    currentTableId++;
                    if (currentTableId == tables.size()) {
                        break;
                    }
                    vt = tables.get(currentTableId);
                    continue;
                }
                long queryRepetitionCount = vt.getLong(0);
                SQLStmt queryToQueue = queriesToQueue.get(queryId);
                for (long i = 0; i < queryRepetitionCount; i++) {
                    m_commonProc.voltQueueSQL(queryToQueue, partitionValue);
                }
            }
        }
        return m_commonProc.voltExecuteSQL();
    }

    private static final DateTimeFormatter DT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS");
    protected static void printLogStatic(String className, String msg) {
        String header = String.format("%s [%s] ",
                ZonedDateTime.now().format(DT_FORMAT),
                className);

        System.out.println(String.format("%s%s",
                header, msg.replaceAll("\n", "\n" + header)));
    }
}
