package HuaweiUACApp;

public class QueryFactory {

    private static void checkTableId(int tableId) {
        if (tableId >= CodeGeneration.DB_TABLE_COUNT) {
            throw new IllegalArgumentException(
                    String.format("Table ID cannot be >= %s.", CodeGeneration.DB_TABLE_COUNT));
        }
    }

    public static String getQuery(Integer...tableIds) {
        if (tableIds == null || tableIds.length == 0) {
            throw new IllegalArgumentException("At least one table ID should be specified.");
        }
        StringBuilder queryBuilder = new StringBuilder();
        checkTableId(tableIds[0]);
        queryBuilder.append("SELECT T0.VALUE1 FROM TABLE");
        queryBuilder.append(tableIds[0]).append(" T0 ");
        for (int i = 1; i < tableIds.length; i++) {
            Integer tableId = tableIds[i];
            checkTableId(tableId);
            queryBuilder.append("INNER JOIN ");
            queryBuilder.append("TABLE").append(tableId).append(" T").append(i);
            queryBuilder.append(" ON T").append(i - 1).append(".PK = T").append(i);
            queryBuilder.append(".PK ");
        }
        queryBuilder.append("WHERE T0.PK = ? ORDER BY 1;");
        return queryBuilder.toString();
    }
}
