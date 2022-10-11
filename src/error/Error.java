package error;

public class Error implements Comparable<Error> {
    private int lineNumber;
    private ErrorType type;

    public Error(int lineNumber, ErrorType type) {
        this.lineNumber = lineNumber;
        this.type = type;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public ErrorType getType() {
        return type;
    }

    @Override
    public String toString() {
        return lineNumber + " " + type.toString() + "\n";
    }

    @Override
    public int compareTo(Error o) {
        if (lineNumber == o.lineNumber) {
            return 0;
        } else if (lineNumber < o.lineNumber) {
            return -1;
        }
        return 1;
    }

    public boolean equals(Error o) {
        return lineNumber == o.lineNumber;
    }
}
