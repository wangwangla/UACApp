package HuaweiUACApp;

import java.io.IOException;

import org.voltdb.VoltTable;
import org.voltdb.client.Client;
import org.voltdb.client.ProcCallException;

public class DataGenerator {

    /*
     Schema:
     CREATE TABLE TABLE0 (
       FLAG tinyint DEFAULT '0' NOT NULL,
       PK BIGINT NOT NULL,
       VALUE1 varchar(50 BYTES),
       VALUE2 varchar(50 BYTES),
       VALUE3 varchar(50 BYTES),
       CONSTRAINT PK_TABLE0 PRIMARY KEY (PK)
     );
     PARTITION TABLE TABLE0 ON COLUMN PK;
     CREATE INDEX IDX_TABLE0 ON TABLE0 (PK, VALUE1, VALUE2);
     */

    public static void generate(Client client, int rowCount) {
        for (int tableId = 0; tableId < CodeGeneration.DB_TABLE_COUNT; tableId++) {
            generate(client, tableId, rowCount);
        }
    }

    public static void generate(Client client, int tableId, int rowCount) {
        ClientApp.printLogStatic(DataGenerator.class.getSimpleName(),
                String.format("Generating %d rows of data for table 'TABLE%d'...", rowCount, tableId));
        String procedureName = String.format("TABLE%d.insert", tableId);
        try {
            VoltTable vt = client.callProcedure("@AdHoc", "SELECT MAX(PK) FROM TABLE" + tableId).getResults()[0];
            long pkValue = vt.asScalarLong();
            if (vt.wasNull()) {
                pkValue = 0;
            }
            ClientApp.printLogStatic(DataGenerator.class.getSimpleName(),
                    String.format("The PK value will start from %d.", pkValue));
            for (int i = 0; i < rowCount; i++, pkValue++) {
                String value = "VALUE " + pkValue;
                client.callProcedure(procedureName, 0, pkValue, value, value, value);
            }
        } catch (IOException | ProcCallException e) { }
    }
}
