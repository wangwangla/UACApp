    package HuaweiApp;

import java.util.Arrays;

import org.junit.Test;

import HuaweiUACApp.BusinessLogic;
import HuaweiUACApp.CommonProcedureFactory;
import HuaweiUACApp.compiler.InMemoryJavaSourceCode;

public class TestCommonProcFactory {

    @Test
    public void test() {
        BusinessLogic info =
                new BusinessLogic("test", Arrays.asList("query1", "query2"), 5);
        InMemoryJavaSourceCode source = CommonProcedureFactory.getCommonProcJavaSource(Arrays.asList(info), null);
        System.out.println(source.getCharContent(true));
    }

}
