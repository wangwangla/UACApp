package UACApp;

import java.util.Random;

import org.voltdb.VoltTable;
import org.voltdb.VoltType;

public class VoltTableParamFactory {

    private static Random RANDOM = new Random();
    private static final int rowCountMax =
            (CodeGeneration.MAX_BIZ_LOGIC_STMT_COUNT - 1) /
            CodeGeneration.DB_TABLE_COUNT + 2;

    public static VoltTable getRandomVoltTable() {
        VoltTable vt = new VoltTable(
                new VoltTable.ColumnInfo("BIGINT_COLUMN", VoltType.BIGINT));
        int rowCount = RANDOM.nextInt(rowCountMax);
        for (int i = 0; i < rowCount; i++) {
            vt.addRow(RANDOM.nextInt(2) + 1);
        }
        return vt;
    }

}
