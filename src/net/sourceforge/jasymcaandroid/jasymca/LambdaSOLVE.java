package net.sourceforge.jasymcaandroid.jasymca;
/* Jasymca	-	- Symbolic Calculator for Mobile Devices
   This version is written for J2ME, CLDC 1.1,  MIDP 2, JSR 75
   or J2SE


   Copyright (C) 2006 - Helmut Dersch  der@hs-furtwangen.de
   
   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2, or (at your option)
   any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.  */

/*------------------------------------------------------------*/

import java.util.Vector;
import java.util.Stack;

class LambdaSOLVE extends Lambda{

	/*
		Solve expr=0 for var
	Strategie:
	(1) Normalize Equation.
	(2) Find number of x-dependent variables
	(3) if(number == 1) Solve for this variable using polynomial methods
	(4) if(variable !=  x) Invert var(x) to x and return solution.
	(5) if(number==2) try sqrt(x)<-->x solutions, otherwise give up.
	*/
	
		
	public int lambda(Stack st) throws ParseException, JasymcaException{
		int narg = getNarg( st );
		if(narg!=2)
			throw new ParseException("solve requires 2 arguments.");
		// Normalize and check expression
		Algebraic expr = getAlgebraic(st).rat();	
		if(!(expr instanceof Polynomial || expr instanceof Rational))
			throw new JasymcaException("Wrong format for Expression in solve.");

		// Variable to solve for
		Variable var = getVariable(st);
		
		Algebraic r = solve(expr, var).reduce();
//		r = linfaktor(r,var);
		st.push(r);
		return 0;
	}
	
	// Create linear factor x-a 
	public static Algebraic linfaktor(Algebraic expr,Variable var) throws JasymcaException{
		if(expr instanceof Vektor){
			Algebraic cn[] = new Algebraic[((Vektor)expr).length()];
			for(int i=0; i<((Vektor)expr).length(); i++)
				cn[i] = linfaktor(((Vektor)expr).get(i),var);
			return new Vektor(cn);
		}
		return new Polynomial(var).sub(expr);
	}
	
	public static Vektor solve(Algebraic expr, Variable var) throws JasymcaException{

		p("Solve: "+expr+" = 0, Variable: "+var); 
		// Normalize expression
		expr = new ExpandUser().f_exakt(expr);
		expr = new TrigExpand().f_exakt( expr );
		p("TrigExpand: "+expr);
		expr = new NormExp().f_exakt(expr);
		p("Norm: "+expr);
		expr = new CollectExp(expr).f_exakt(expr);
		p("Collect: "+expr);
		expr = new SqrtExpand().f_exakt(expr);
		p("SqrtExpand: "+expr);
		if( expr instanceof Rational){
			expr=new LambdaRAT().f_exakt(expr);
			if( expr instanceof Rational)
				expr=((Rational)expr).nom;
		}
		
		p("Canonic Expression: "+expr); 
		
		if(!(expr instanceof Polynomial) || !((Polynomial)expr).depends(var))
			throw new JasymcaException("Expression does not depend of variable."); 
		
		Polynomial p = (Polynomial)expr;

		// Start solve
		Vektor sol = null;
		Vector dep = depvars(p,var); // List of variables which depend on var
		if(dep.size()==0){ // This should never happen
			throw new JasymcaException("Expression does not depend of variable."); 
		}
		if(dep.size()==1){ // Try to solve for this variable
			Variable dvar = (Variable)dep.elementAt(0);
			p("Found one Variable: "+dvar);
			sol = p.solve(dvar);
			p("Solution: "+dvar+" = "+sol);
			if(!dvar.equals(var)){ // dvar is FunctionVariable, invert and solve
				Vector s = new Vector();
				for(int i=0; i<sol.length(); i++){
					p("Invert: "+sol.get(i)+" = "+dvar);
					Algebraic sl = finvert( (FunctionVariable)dvar, sol.get(i) );
					p("Result: "+sl+" = 0");
					// Solve this expression and add solutions to result
					Vektor t = solve(sl, var);
					p("Solution: "+var+" = "+t);
					for(int k=0; k<t.length(); k++){
						Algebraic tn = t.get(k);
						if(!s.contains(tn)) s.addElement(tn);
					}
				}
				sol = Vektor.create(s);
			}
		}else if(dep.size()==2){
			// We might be able to handle sqrt(x),x-cases
			// Todo: remove artificial wrong solutions
			p("Found two Variables: "+dep.elementAt(0)+", "+dep.elementAt(1));
			if(dep.contains(var)){
				FunctionVariable f = (FunctionVariable) (dep.elementAt(0).equals(var)?
										dep.elementAt(1):dep.elementAt(0));
				if(f.fname.equals("sqrt")){
					p("Solving "+p+" for "+f);
					sol = p.solve(f);
					p("Solution: "+f+" = "+sol);
					Vector s = new Vector();
					for(int i=0; i<sol.length(); i++){
						p("Invert: "+sol.get(i)+" = "+f);
						Algebraic sl = finvert( (FunctionVariable)f, sol.get(i) );
						p("Result: "+sl+" = 0");
						if(sl instanceof Polynomial && depvars(((Polynomial)sl),var).size()==1){
							// Solve this expression and add solutions to result
							p("Solving "+sl+" for "+var);
							Vektor t = solve(sl, var);
							p("Solution: "+var+" = "+t);
							for(int k=0; k<t.length(); k++){
								Algebraic tn = t.get(k);
								if(!s.contains(tn)) s.addElement(tn);
							}
						}else
							throw new JasymcaException("Could not solve equation.");
					}
					sol = Vektor.create(s);
				}else{ // Add more algorithms....
					throw new JasymcaException("Can not solve equation.");
				}
			}else{ // Add more algorithms....
				throw new JasymcaException("Can not solve equation.");
			}
		}else{ // Add more algorithms....
				throw new JasymcaException("Can not solve equation.");
		}
		// Beatify solution
		/*
		p("Simplifying Solution:");
		for(int i=0; i<sol.coord.length; i++){
			sol.coord[i] = new SqrtExpand().f_exakt(sol.coord[i]);
			p("Sqrt Expansion: "+sol.coord[i]);
			sol.coord[i] = new NormExp().f_exakt(sol.coord[i]);
			p("Exponential Normalization "+sol.coord[i]);
			sol.coord[i] = new TrigInverseExpand().f_exakt(sol.coord[i]);
			p("Inverse Trigonometric Expansion "+sol.coord[i]);
		}*/
		return sol;
	}			


	// List of variables, which depend on var
	private static Vector depvars(Polynomial p, Variable var) throws JasymcaException{
		Vector r = new Vector();
		if( ! p.var.deriv(var).equals(Zahl.ZERO) )
			r.addElement(p.var);
		for(int i=0; i<p.a.length; i++){
			if(p.a[i] instanceof Polynomial){
				Vector c = depvars((Polynomial)p.a[i],var);
				if(c.size()>0){
					for(int k=0; k<c.size(); k++){
						Object v = c.elementAt(k);
						if(!r.contains(v)) r.addElement(v);
					}
				}
			}
		}
		return r;
	}		
					
	
	// f(a(x)) = b(x)  --> f^(-1)(b(x))-a(x) = 0
	static Algebraic finvert( FunctionVariable f, Algebraic b) throws JasymcaException{
		if(f.fname.equals("sqrt")){
			return b.mult(b).sub(f.arg);
		}
		if(f.fname.equals("exp")){
			return FunctionVariable.create("log",b).sub(f.arg);
		}
		if(f.fname.equals("log")){
			return FunctionVariable.create("exp",b).sub(f.arg);
		}
		if(f.fname.equals("tan")){
			return FunctionVariable.create("atan",b).sub(f.arg);
		}
		if(f.fname.equals("atan")){
			return FunctionVariable.create("tan",b).sub(f.arg);
		}
		throw new JasymcaException("Could not invert "+f);		
	}
}
