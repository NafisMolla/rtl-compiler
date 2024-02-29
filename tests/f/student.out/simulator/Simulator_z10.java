import java.util.*;
import ece351.w.ast.*;
import ece351.w.parboiled.*;
import static ece351.util.Boolean351.*;
import ece351.util.CommandLine;
import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.IOException;
import ece351.util.Debug;

public final class Simulator_z10 {
    public static void main(final String[] args) {
        final String s = File.separator;
        // read the input F program
        // write the output
        // read input WProgram
        final CommandLine cmd = new CommandLine(args);
        final String input = cmd.readInputSpec();
        final WProgram wprogram = WParboiledParser.parse(input);
        // construct storage for output
        final Map<String,StringBuilder> output = new LinkedHashMap<String,StringBuilder>();
        output.put("g", new StringBuilder());
        // loop over each time step
        final int timeCount = wprogram.timeCount();
        for (int time = 0; time < timeCount; time++) {
        // values of input variables at this time step
        // values of output variables at this time step
        final boolean in_a = wprogram.valueAtTime("a", time);
        final boolean in_b = wprogram.valueAtTime("b", time);
        final boolean in_d = wprogram.valueAtTime("d", time);
        final String out_g = g(in_a, in_b, in_d) ? "1 " : "0 ";
        output.get("g").append(out_g);
        }
        try {
        final File f = cmd.getOutputFile();
        f.getParentFile().mkdirs();
        final PrintWriter pw = new PrintWriter(new FileWriter(f));
        System.out.println(wprogram.toString());
        pw.println(wprogram.toString());
        System.out.println(f.getAbsolutePath());
        for (final Map.Entry<String,StringBuilder> e : output.entrySet()) {
        System.out.println(e.getKey() + ":" + e.getValue().toString()+ ";");
        pw.write(e.getKey() + ":" + e.getValue().toString()+ ";\n"); }
        pw.close();
        } catch (final IOException e) {
        Debug.barf(e.getMessage()); }
    }
    public static boolean g(final boolean a, final boolean b, final boolean d) { return 
or(and(a, b) , and(a, d) )      ; }
}
