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

import java.util.LinkedHashSet;
import java.util.Set;

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
import ece351.v.ast.DesignUnit;
import ece351.v.ast.IfElseStatement;
import ece351.v.ast.Process;
import ece351.v.ast.VProgram;

/**
 * Process splitter.
 */
public final class Splitter extends PostOrderExprVisitor {
	private final Set<String> usedVarsInExpr = new LinkedHashSet<String>();

	public static void main(String[] args) {
		System.out.println(split(args));
	}
	
	public static VProgram split(final String[] args) {
		return split(new CommandLine(args));
	}
	
	public static VProgram split(final CommandLine c) {
		final VProgram program = DeSugarer.desugar(c);
        return split(program);
	}
	
	public static VProgram split(final VProgram program) {
		VProgram p = Elaborator.elaborate(program);
		final Splitter s = new Splitter();
		return s.splitit(p);
	}

	private VProgram splitit(final VProgram program) {
		// Determine if the process needs to be split into multiple processes
		// Split the process if there are if/else statements so that the if/else statements only assign values to one pin
		ImmutableList<DesignUnit> units = ImmutableList.of();
		for(DesignUnit current_design: program.designUnits){
			//this is out current design unit but with a empty Statement array 
			Architecture d1 = current_design.arch.varyStatements(ImmutableList.<Statement>of());

			//with the current designUnit we need to remap all the componenets
			for (Statement stmt :  current_design.arch.statements){
				
				//check to see if the statement is of type process 
				if(stmt instanceof Process){
					Process localP = (Process)stmt;

					//go through all the statements
					for(Statement s : localP.sequentialStatements){
						//if it is a process 
						if(s instanceof IfElseStatement){
							for(Statement new_statement: splitIfElseStatement((IfElseStatement)s))
							{
								d1 = d1.appendStatement(new_statement);
							}
						}
						else
						{
							//can have multiple statements in one procces, so add the whole thing
							//the break makes more sense now
							d1 = d1.appendStatement(stmt);
							break;
						}
					}

					
				}
				//its not proccess so we dont really do anything to it
				else{
					d1 = d1.appendStatement(stmt);
				}

			}
			units = units.append(new DesignUnit(d1, current_design.entity));	
		
		}

		return new VProgram(units); 
	}
	
	// You do not have to use this helper method, but we found it useful
	
	private ImmutableList<Statement> splitIfElseStatement(final IfElseStatement ifStmt) {
		//array of split proccesses
		ImmutableList<Statement> processes = ImmutableList.<Statement>of();
		// loop over each statement in the ifBody
		for(AssignmentStatement if_stmt: ifStmt.ifBody){
			// loop over each statement in the elseBody
			for(AssignmentStatement else_stmt: ifStmt.elseBody){
				// check if outputVars are the same
				if(if_stmt.outputVar.identifier.equals(else_stmt.outputVar.identifier)){
					// initialize/clear this.usedVarsInExpr
					this.usedVarsInExpr.clear();
					// call traverse a few times to build up this.usedVarsInExpr
					//add all the outputs, tthat will be added to the sensitivity list
					traverseExpr(if_stmt.expr);
					traverseExpr(else_stmt.expr);
					traverseExpr(ifStmt.condition);


					Process newProc = new Process();
					// build sensitivity list from this.usedVarsInExpr
					for(String e : usedVarsInExpr){
						newProc = newProc.appendSensitivity(e);
					}
					
					// build the resulting list of split statements
					//making the new ifElse that will go in the proccess
					IfElseStatement local = new IfElseStatement(ImmutableList.<AssignmentStatement>of().append((AssignmentStatement)else_stmt),ImmutableList.<AssignmentStatement>of().append((AssignmentStatement)if_stmt),ifStmt.condition);
					newProc = newProc.appendStatement(local);

					//add this new proccess into the immutableList
					processes = processes.append(newProc);
					
				}

			}

		}
		// return result
		return processes;
	}

	@Override
	public Expr visitVar(final VarExpr e) {
		this.usedVarsInExpr.add(e.identifier);
		return e;
	}

	// no-ops
	@Override public Expr visitConstant(ConstantExpr e) { return e; }
	@Override public Expr visitNot(NotExpr e) { return e; }
	@Override public Expr visitAnd(AndExpr e) { return e; }
	@Override public Expr visitOr(OrExpr e) { return e; }
	@Override public Expr visitXOr(XOrExpr e) { return e; }
	@Override public Expr visitNAnd(NAndExpr e) { return e; }
	@Override public Expr visitNOr(NOrExpr e) { return e; }
	@Override public Expr visitXNOr(XNOrExpr e) { return e; }
	@Override public Expr visitEqual(EqualExpr e) { return e; }
	@Override public Expr visitNaryAnd(NaryAndExpr e) { return e; }
	@Override public Expr visitNaryOr(NaryOrExpr e) { return e; }

}
