package mattpvaughn.io.github.emulator;

public class Utils {

    // A single method to log from in case my needs every change for logging. 
    // This could output to a log file when running from a build I'm not in
    // control of.
    public static void log(String str) {
        System.out.println(str);
    }

}
