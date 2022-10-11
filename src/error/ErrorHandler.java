package error;

import utils.IOUtils;

import java.util.ArrayList;
import java.util.List;

public class ErrorHandler {

    public static List<Error> errors = new ArrayList<>();

    public static int loopCount = 0;

    public static void printErrors() {
        errors.sort(Error::compareTo);
        for (Error error : errors) {
            IOUtils.error(error.toString());
        }
    }

    public static void addError(Error newError) {
        for (Error error : errors) {
            if (error.equals(newError)) {
                return;
            }
        }
        errors.add(newError);
    }
}
