package HuaweiUACApp.procedures;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;

public class CommonProcTemplate extends VoltProcedure {

// $ Declare SQL statements $
    private static Map<String, Class<? extends AbstractBusinessLogic>>
            m_bizLogicClasses = new HashMap<>();

    static {
// $ Register business logic procedures in the map $
    }

    public VoltTable[] run(long partitionValue,
                           String procedureName,
                           VoltTable vtIn1, VoltTable vtIn2,
                           VoltTable vtIn3, VoltTable vtIn4,
                           VoltTable vtIn5, VoltTable vtIn6)
                                   throws InstantiationException, IllegalAccessException,
                                          IllegalArgumentException, InvocationTargetException,
                                          NoSuchMethodException, SecurityException {

        Class<? extends AbstractBusinessLogic> bizLogicClass =
                m_bizLogicClasses.get(procedureName);
        if (bizLogicClass == null) {
            throw new VoltAbortException("Invalid business logic name.");
        }

        // Instantiate the business logic procedure and run.
        AbstractBusinessLogic bizLogic = bizLogicClass.getConstructor(VoltProcedure.class).newInstance(this);
        return bizLogic.run(partitionValue, vtIn1, vtIn2, vtIn3, vtIn4, vtIn5, vtIn6);
    }
}
