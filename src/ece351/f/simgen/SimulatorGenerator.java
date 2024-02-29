/* *********************************************************************
 * ECE351 
 * Department of Electrical and Computer Engineering 
 * University of Waterloo 
 * Term: Fall 2021 (1219)
 *
 * The base version of this file is the intellectual property of the
 * University of Waterloo. Redistribution is prohibited.
 *
 * By pushing changes to this file I affirm that I am the author of
 * all changes. I affirm that I have complied with the course
 * collaboration policy and have not plagiarized my work. 
 *
 * I understand that redistributing this file might expose me to
 * disciplinary action under UW Policy 71. I understand that Policy 71
 * allows for retroactive modification of my final grade in a course.
 * For example, if I post my solutions to these labs on GitHub after I
 * finish ECE351, and a future student plagiarizes them, then I too
 * could be found guilty of plagiarism. Consequently, my final grade
 * in ECE351 could be retroactively lowered. This might require that I
 * repeat ECE351, which in turn might delay my graduation.
 *
 * https://uwaterloo.ca/secretariat-general-counsel/policies-procedures-guidelines/policy-71
 * 
 * ********************************************************************/

package ece351.f.simgen;

import java.io.File;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import ece351.common.ast.AndExpr;
import ece351.common.ast.AssignmentStatement;
import ece351.common.ast.BinaryExpr;
import ece351.common.ast.ConstantExpr;
import ece351.common.ast.EqualExpr;
import ece351.common.ast.Expr;
import ece351.common.ast.NAndExpr;
import ece351.common.ast.NOrExpr;
import ece351.common.ast.NaryAndExpr;
import ece351.common.ast.NaryExpr;
import ece351.common.ast.NaryOrExpr;
import ece351.common.ast.NotExpr;
import ece351.common.ast.OrExpr;
import ece351.common.ast.UnaryExpr;
import ece351.common.ast.VarExpr;
import ece351.common.ast.XNOrExpr;
import ece351.common.ast.XOrExpr;
import ece351.common.visitor.ExprVisitor;
import ece351.f.FParser;
import ece351.f.analysis.DetermineInputVars;
import ece351.f.ast.FProgram;
import ece351.util.CommandLine;
import ece351.w.ast.WProgram;
import ece351.w.parboiled.WParboiledParser;

public final class SimulatorGenerator extends ExprVisitor {

	private PrintWriter out = new PrintWriter(System.out);
	private String indent = "";

	public static void main(final String arg) {
		main(new String[]{arg});
	}
	
	public static void main(final String[] args) {
		final CommandLine c = new CommandLine(args);
		final SimulatorGenerator s = new SimulatorGenerator();
		final PrintWriter pw = new PrintWriter(System.out);
		s.generate(c.getInputName(), FParser.parse(c), pw);
		pw.flush();
	}

	private void println(final String s) {
		out.print(indent);
		out.println(s);
	}
	
	private void println() {
		out.println();
	}
	
	private void print(final String s) {
		out.print(s);
	}

	private void indent() {
		indent = indent + "    ";
	}
	
	private void outdent() {
		indent = indent.substring(0, indent.length() - 4);
	}
	
	public void generate(final String fName, final FProgram program, final PrintWriter out) {
		this.out = out;
		final String cleanFName = fName.replace('-', '_');
		
		// header
		println("import java.util.*;");
		println("import ece351.w.ast.*;");
		println("import ece351.w.parboiled.*;");
		println("import static ece351.util.Boolean351.*;");
		println("import ece351.util.CommandLine;");
		println("import java.io.File;");
		println("import java.io.FileWriter;");
		println("import java.io.StringWriter;");
		println("import java.io.PrintWriter;");
		println("import java.io.IOException;");
		println("import ece351.util.Debug;");
		println();
		println("public final class Simulator_" + cleanFName + " {");
		indent();
		println("public static void main(final String[] args) {");
		indent();
		//boiler plate 
		println("final String s = File.separator;");

		println("// read the input F program");
		println("// write the output");
		println("// read input WProgram");

		println("final CommandLine cmd = new CommandLine(args);");
		println("final String input = cmd.readInputSpec();");
		println("final WProgram wprogram = WParboiledParser.parse(input);");

		println("// construct storage for output");
		println("final Map<String,StringBuilder> output = new LinkedHashMap<String,StringBuilder>();");
		
		//looping through all the fomulas and adding the output variables
		for(AssignmentStatement childs: program.formulas)
		{
			println("output.put(\"" + childs.outputVar.toString() + "\", new StringBuilder());");
		}
		
		println("// loop over each time step");
		println("final int timeCount = wprogram.timeCount();");
		println("for (int time = 0; time < timeCount; time++) {");
		//this sores all the expressions we have in our program
		Set<String> exprs = DetermineInputVars.inputVars(program);

		println("// values of input variables at this time step");

		println("// values of output variables at this time step");
		//looping through the individual expressions at each time instance
		//making the variables that will be used in our function call
		for(String children: exprs)
		{
			println("final boolean in_" + children + " = wprogram.valueAtTime(\"" + children + "\", time);");
		}	
		
		//assingn the leftsides to the respective rightsides
		for(AssignmentStatement formulas: program.formulas)
		{
			//what is the value of the output at time = whatever 
			println("final String out_" + formulas.outputVar.toString() + " = " + generateCall(formulas) + " ? \"1 \" : \"0 \";");
			println("output.get(\"" + formulas.outputVar.toString() + "\").append(out_" + formulas.outputVar.toString() + ");");
		}

		//
		println("}");
		println("try {");
		println("final File f = cmd.getOutputFile();");
		println("f.getParentFile().mkdirs();");
		println("final PrintWriter pw = new PrintWriter(new FileWriter(f));");
		println("System.out.println(wprogram.toString());"); 
		println("pw.println(wprogram.toString());");
		println("System.out.println(f.getAbsolutePath());");
		println("for (final Map.Entry<String,StringBuilder> e : output.entrySet()) {");
		println("System.out.println(e.getKey() + \":\" + e.getValue().toString()+ \";\");");
		println("pw.write(e.getKey() + \":\" + e.getValue().toString()+ \";\\n\"); }");
		println("pw.close();");
		println("} catch (final IOException e) {");
		println("Debug.barf(e.getMessage()); }");

		// end main method
		outdent();
		println("}");
		

		//generating the custom fuction decllaration for each individual formula 
		for(AssignmentStatement formulas: program.formulas)
		{
			println(generateSignature(formulas) + " { return "); 
			//this will println(or(a,b), etc ... )
			this.traverseExpr(formulas.expr);
			println(" ; }");
		}


		// end class
		outdent();
		println("}");

	}

	@Override
	public Expr traverseNaryExpr(final NaryExpr e) {
		e.accept(this);
		final int size = e.children.size();
		for (int i = 0; i < size; i++) {
			final Expr c = e.children.get(i);
			traverseExpr(c);
			if (i < size - 1) {
				// common case
				out.print(", ");
			}
		}
		out.print(") ");
		return e;
	}

	@Override
	public Expr traverseBinaryExpr(final BinaryExpr e) {
		e.accept(this);
		traverseExpr(e.left);
		out.print(", ");
		traverseExpr(e.right);
		out.print(") ");
		return e;
	}

	@Override
	public Expr traverseUnaryExpr(final UnaryExpr e) {
		e.accept(this);
		traverseExpr(e.expr);
		out.print(") ");
		return e;
	}

	@Override
	public Expr visitConstant(final ConstantExpr e) {
		out.print(Boolean.toString(e.b));
		return e;
	}

	@Override
	public Expr visitVar(final VarExpr e) {
		out.print(e.identifier);
		return e;
	}

	@Override public Expr visitNot(final NotExpr e) { return visitOp(e); }
	@Override public Expr visitAnd(final AndExpr e) { return visitOp(e); }
	@Override public Expr visitOr(final OrExpr e) { return visitOp(e); }
	@Override public Expr visitNaryAnd(final NaryAndExpr e) { return visitOp(e); }
	@Override public Expr visitNaryOr(final NaryOrExpr e) { return visitOp(e); }
	@Override public Expr visitNOr(final NOrExpr e) { return visitOp(e); }
	@Override public Expr visitXOr(final XOrExpr e) { return visitOp(e); }
	@Override public Expr visitXNOr(final XNOrExpr e) { return visitOp(e); }
	@Override public Expr visitNAnd(final NAndExpr e) { return visitOp(e); }
	@Override public Expr visitEqual(final EqualExpr e) { return visitOp(e); }
	
	private Expr visitOp(final Expr e) {
		out.print(e.operator());
		out.print("(");
		return e;
	}
	
	public String generateSignature(final AssignmentStatement f) {
		return generateList(f, true);
	}
	
	public String generateCall(final AssignmentStatement f) {
		return generateList(f, false);
	}
	//generates 
	private String generateList(final AssignmentStatement f, final boolean signature) {
		final StringBuilder b = new StringBuilder();
		if (signature) {
			b.append("public static boolean ");
		}
		b.append(f.outputVar);
		b.append("(");
		// loop over f's input variables
		
		// TODO: longer code snippet
		for (int i = 0; i < DetermineInputVars.inputVars(f).size(); i++) {
			if (signature) {
				b.append("final boolean ");
			} else {
				b.append("in_");
			}
			b.append(DetermineInputVars.inputVars(f).toArray()[i]);

			if (i < DetermineInputVars.inputVars(f).size() - 1) {
				b.append(", ");
			}
		}

		b.append(")");
		return b.toString();
	}

}
