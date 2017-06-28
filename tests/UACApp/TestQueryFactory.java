package UACApp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestQueryFactory {

    @Test
    public void test() {
        String expectedQuery =
                "SELECT T0.VALUE1 FROM TABLE1 T0 INNER JOIN TABLE2 T1 ON T0.PK = T1.PK " +
                "INNER JOIN TABLE3 T2 ON T1.PK = T2.PK INNER JOIN TABLE4 T3 ON T2.PK = T3.PK " +
                "INNER JOIN TABLE5 T4 ON T3.PK = T4.PK ORDER BY 1;";
        String actualQuery = QueryFactory.getQuery(1, 2, 3, 4, 5);
        System.out.println(actualQuery);
        assertEquals(expectedQuery, actualQuery);
    }
}
