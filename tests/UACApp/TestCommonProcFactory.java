package UACApp;

import java.util.Arrays;

import org.junit.Test;

import UACApp.compiler.InMemoryJavaSourceCode;

public class TestCommonProcFactory {

    @Test
    public void test() {
        BusinessLogic info =
                new BusinessLogic("test", Arrays.asList("query1", "query2"), 5);
        InMemoryJavaSourceCode source = CommonProcedureFactory.getCommonProcJavaSource(Arrays.asList(info), null);
        System.out.println(source.getCharContent(true));
    }

}
