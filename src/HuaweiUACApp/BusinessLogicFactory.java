package HuaweiUACApp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class BusinessLogicFactory {

    private final AtomicInteger m_logicSeqId = new AtomicInteger(1);
    private final AtomicInteger m_querySeqId = new AtomicInteger(1);
    private final Random m_random = new Random();

    public BusinessLogic getNewRandomBusinessLogic() {
        //Generate the logic name based on a unique monotonically increasing sequential ID.
        String bizProcName = String.format("%s%06d",
                CodeGeneration.BIZ_LOGIC_NAME_PREFIX, m_logicSeqId.getAndIncrement());

        // Add at least one SQL statement to the procedure.
        int numOfStmtsToAdd = m_random.nextInt(CodeGeneration.MAX_BIZ_LOGIC_STMT_COUNT) + 1;

        // Generate the queries which evaluate joins on multiple tables.
        List<String> queriesToAdd = new ArrayList<>();
        for (int i = 0; i < numOfStmtsToAdd; i++) {
            // The query should involve some tables (>= 1)
            int numOfTablesToInvolve = m_random.nextInt(CodeGeneration.DB_TABLE_COUNT) + 1;

            // The list of involved tables should not have duplicates.
            // And it could be in different orders.
            Set<Integer> tableIdsToInvolve = new HashSet<>();
            while (tableIdsToInvolve.size() < numOfTablesToInvolve) {
                // table id start from 0.
                tableIdsToInvolve.add(m_random.nextInt(CodeGeneration.DB_TABLE_COUNT));
            }
            Integer[] tableIds = new Integer[tableIdsToInvolve.size()];
            tableIdsToInvolve.toArray(tableIds);

            // Generate the query using the random table IDs.
            String query = QueryFactory.getQuery(tableIds);
            queriesToAdd.add(query);
        }

        return new BusinessLogic(bizProcName, queriesToAdd,
                                 m_querySeqId.getAndAdd(queriesToAdd.size()));
    }
}
