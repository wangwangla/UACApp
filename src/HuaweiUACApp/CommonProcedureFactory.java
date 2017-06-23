package HuaweiUACApp;

import java.util.List;
import java.util.Map.Entry;

import HuaweiUACApp.compiler.InMemoryJavaSourceCode;

public class CommonProcedureFactory {

    public static InMemoryJavaSourceCode getCommonProcJavaSource(
            List<BusinessLogic> bizLogics, List<BusinessLogic> bizLogicsPending) {
        StringBuilder stmtCodeBuilder = new StringBuilder();
        StringBuilder bizLogicListBuilder = new StringBuilder();

        for (BusinessLogic bizLogic : bizLogics) {
            for (Entry<Integer, String> entry : bizLogic.getQueries().entrySet()) {
                stmtCodeBuilder.append(String.format(
                        "%spublic static final SQLStmt stmt%06d = new SQLStmt(\"%s\");\n",
                        INDENTATION, entry.getKey(), entry.getValue()));
            }
            bizLogicListBuilder.append(String.format("%s%sm_bizLogicClasses.put(\"%s\", %s.class);\n",
                            INDENTATION, INDENTATION, bizLogic.getName(), bizLogic.getName()));
        }

        for (BusinessLogic bizLogic : bizLogicsPending) {
            for (Entry<Integer, String> entry : bizLogic.getQueries().entrySet()) {
                stmtCodeBuilder.append(String.format(
                        "%spublic static final SQLStmt stmt%06d = new SQLStmt(\"%s\");\n",
                        INDENTATION, entry.getKey(), entry.getValue()));
            }
            bizLogicListBuilder.append(String.format("%s%sm_bizLogicClasses.put(\"%s\", %s.class);\n",
                            INDENTATION, INDENTATION, bizLogic.getName(), bizLogic.getName()));
        }

        return new InMemoryJavaSourceCode(CodeGeneration.COMMON_PROC_CLASS_NAME,
                String.format(CodeGeneration.COMMON_PROC_TEMPLATE_SOURCE,
                            stmtCodeBuilder.toString(), bizLogicListBuilder.toString()));
    }

    private static final String INDENTATION = "    ";
}
