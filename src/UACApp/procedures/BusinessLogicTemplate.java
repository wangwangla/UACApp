package UACApp.procedures;

import java.util.ArrayList;
import java.util.List;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;

public class BusinessLogicTemplate extends AbstractBusinessLogic {

    // Used by VoltDB to instantiate the procedure.
    public BusinessLogicTemplate() {
        m_commonProc = null;
    }

    public BusinessLogicTemplate(VoltProcedure commonProc) {
        m_commonProc = commonProc;
    }

    @Override
    protected List<SQLStmt> getQueriesToQueue() {
        List<SQLStmt> queriesToQueue = new ArrayList<>();

// $ Add queries here $
        return queriesToQueue;
    }
}
