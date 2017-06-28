package UACApp.procedures;

public class SomeWeirdLibrary {

    public static boolean weirdMethod(Object[] args) {
        for (Object arg : args) {
            if (arg != null) {
                arg.toString();
            }
        }
        return true;
    }

}
