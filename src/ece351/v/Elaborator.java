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

package ece351.v;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.parboiled.common.ImmutableList;

import ece351.common.ast.AndExpr;
import ece351.common.ast.AssignmentStatement;
import ece351.common.ast.ConstantExpr;
import ece351.common.ast.EqualExpr;
import ece351.common.ast.Expr;
import ece351.common.ast.NAndExpr;
import ece351.common.ast.NOrExpr;
import ece351.common.ast.NaryAndExpr;
import ece351.common.ast.NaryOrExpr;
import ece351.common.ast.NotExpr;
import ece351.common.ast.OrExpr;
import ece351.common.ast.Statement;
import ece351.common.ast.VarExpr;
import ece351.common.ast.XNOrExpr;
import ece351.common.ast.XOrExpr;
import ece351.common.visitor.PostOrderExprVisitor;
import ece351.util.CommandLine;
import ece351.v.ast.Architecture;
import ece351.v.ast.Component;
import ece351.v.ast.DesignUnit;
import ece351.v.ast.Entity;
import ece351.v.ast.IfElseStatement;
import ece351.v.ast.Process;
import ece351.v.ast.VProgram;

/**
 * Inlines logic in components to architecture body.
 */
public final class Elaborator extends PostOrderExprVisitor {

	private final Map<String, String> current_map = new LinkedHashMap<String, String>();
	
	public static void main(String[] args) {
		System.out.println(elaborate(args));
	}
	
	public static VProgram elaborate(final String[] args) {
		return elaborate(new CommandLine(args));
	}
	
	public static VProgram elaborate(final CommandLine c) {
        final VProgram program = DeSugarer.desugar(c);
        return elaborate(program);
	}
	
	public static VProgram elaborate(final VProgram program) {
		final Elaborator e = new Elaborator();
		return e.elaborateit(program);
	}

	private VProgram elaborateit(final VProgram root) {

		// our ASTs are immutable. so we cannot mutate root.
		// we need to construct a new AST that will be the return value.
		// it will be like the input (root), but different.
		VProgram result = new VProgram();
		int compCount = 0;

		//this will house all designunits with the same identifier
		Map<String, DesignUnit> units = new LinkedHashMap<String, DesignUnit>();
		//adding all the design units with its identifier
		for(DesignUnit unit: root.designUnits){
			units.put(unit.identifier, unit);
		}

		//going over all the design units, going to map inputs and outputs
		for(DesignUnit current_design: root.designUnits){
			//this is out current design unit but with a empty component array 
			Architecture d1 = current_design.arch.varyComponents(ImmutableList.<Component>of());

			//with the current designUnit we need to remap all the componenets
			for (var component :  current_design.arch.components){
				
				compCount ++;

				Architecture componentArch = units.get(component.entityName).arch;
				Entity componentEntity = units.get(component.entityName).entity;
				ImmutableList<String> entityInputs = componentEntity.input;
				ImmutableList<String> entityOutput = componentEntity.output;

				//for the components we need to find which signals correspond with the enetity

				//we need to add the inputs first then the 
				ImmutableList<String> parameters = entityInputs;
				
				//add the outputs after
				for(String p: entityOutput){
					parameters = parameters.append(p);
				}

				//going through all the inputs and outputs and mapping it in the hashmap
				for (int i = 0; i< parameters.size(); ++i ){
					current_map.put(parameters.get(i),component.signalList.get(i));
				}

				//go through the internal signals and map them to different variables
				for(String s: componentArch.signals){
					//we need to modify new signal 
					var n= "comp"+compCount+"_"+s;
					d1 = d1.appendSignal(n);
					current_map.put(s,n);

				}

				//at this point we have modfied all the signals no we need go through the actual statements within the achitecture and do the replacements
				for(Statement stmt: componentArch.statements){
					//depending on the type of statement we have we need to process it differently

					if(stmt instanceof Process){

						d1 = d1.appendStatement((Statement)expandProcessComponent((Process)stmt));
					}
					else if(stmt instanceof IfElseStatement){

						d1 = d1.appendStatement((Statement)changeIfVars((IfElseStatement)stmt));
					}
					else if(stmt instanceof AssignmentStatement){

						d1 = d1.appendStatement((Statement)changeStatementVars((AssignmentStatement)stmt));
					}
					
					
				}

			}
			
			DesignUnit tmp = new DesignUnit(d1, current_design.entity);
			result = result.append(tmp);
			units.put(current_design.identifier, tmp);
			//clear the map before the next run
			current_map.clear();

		}

		return result;
	}



	
	// you do not have to use these helper methods; we found them useful though
	private Process expandProcessComponent(final Process process) {
			//change the sensitvity List
			ImmutableList<String> new_sensitivitylist = ImmutableList.of();
			ImmutableList<Statement> new_sequentialstatement = ImmutableList.of();
			for(String signal: process.sensitivityList)
			{
				new_sensitivitylist = new_sensitivitylist.append(current_map.get(signal));
			}
			for(Statement statement: process.sequentialStatements)
			{
				if(statement instanceof IfElseStatement)
					new_sequentialstatement = new_sequentialstatement.append((Statement)changeIfVars((IfElseStatement)statement));
				else if (statement instanceof AssignmentStatement)
					new_sequentialstatement = new_sequentialstatement.append((Statement)changeStatementVars((AssignmentStatement)statement));
				else 
					new_sequentialstatement =  new_sequentialstatement.append(expandProcessComponent((Process)statement));
			}
			return new Process(new_sequentialstatement, new_sensitivitylist);
	}
	
	// you do not have to use these helper methods; we found them useful though
	private  IfElseStatement changeIfVars(final IfElseStatement s) {
		Expr new_statement = traverseExpr(s.condition);
		ImmutableList<AssignmentStatement> new_IfBody = ImmutableList.of();
		ImmutableList<AssignmentStatement> new_ElseBody = ImmutableList.of();
		
		for(AssignmentStatement stmt: s.ifBody)
		{
			new_IfBody = new_IfBody.append(changeStatementVars(stmt));
		}
		for(AssignmentStatement stmt: s.elseBody)
		{
			new_ElseBody = new_ElseBody.append(changeStatementVars(stmt));
		}
		return new IfElseStatement(new_ElseBody, new_IfBody, new_statement);
	}

	// you do not have to use these helper methods; we found them useful though
	private AssignmentStatement changeStatementVars(final AssignmentStatement s){
		//this will do the osubsitutions for you 
		VarExpr new_outputvar = (VarExpr)traverseExpr(s.outputVar);
		Expr new_expr = traverseExpr(s.expr);
		return new AssignmentStatement(new_outputvar, new_expr);
	}
	
	
	@Override
	public Expr visitVar(VarExpr e) {
		// TODO replace/substitute the variable found in the map
		//if we have the variable in our map we need to replace it
		String Var = current_map.get(e.identifier);
		if (Var == null){
			return e;
		}
		return new VarExpr(Var);
	}
	
	// do not rewrite these parts of the AST
	@Override public Expr visitConstant(ConstantExpr e) { return e; }
	@Override public Expr visitNot(NotExpr e) { return e; }
	@Override public Expr visitAnd(AndExpr e) { return e; }
	@Override public Expr visitOr(OrExpr e) { return e; }
	@Override public Expr visitXOr(XOrExpr e) { return e; }
	@Override public Expr visitEqual(EqualExpr e) { return e; }
	@Override public Expr visitNAnd(NAndExpr e) { return e; }
	@Override public Expr visitNOr(NOrExpr e) { return e; }
	@Override public Expr visitXNOr(XNOrExpr e) { return e; }
	@Override public Expr visitNaryAnd(NaryAndExpr e) { return e; }
	@Override public Expr visitNaryOr(NaryOrExpr e) { return e; }
}
