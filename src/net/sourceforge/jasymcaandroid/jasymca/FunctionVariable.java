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
import java.util.Stack;

public class FunctionVariable extends Variable{
	public String fname;
	public Algebraic arg;
	public LambdaAlgebraic la;
	
	public FunctionVariable( String fname, Algebraic arg, LambdaAlgebraic la){
		this.fname 	= fname;
		this.arg 	= arg;
		this.la 	= la;
	}
	public Algebraic deriv( Variable x ) throws JasymcaException{ 
		if(equals(x))
			return Zahl.ONE;
		if(!arg.depends(x))
			return Zahl.ZERO;
		if(la==null)
			throw new JasymcaException("Can not differentiate "+fname+
										"  : No definition.");

		// Apply diffrule to arg
		String diffrule = la.diffrule;
		if(diffrule==null)
			throw new JasymcaException("Can not differentiate "+fname+
										" : No rule available.");
		
		Algebraic y = Lambda.evalx( diffrule, arg );
		// Multiply with diff(arg)
		return y.mult(arg.deriv(x));
	}
	
	public Algebraic integrate( Variable x ) throws JasymcaException{
		arg = arg.reduce();
		if(la==null)
			throw new JasymcaException("Can not integrate "+fname);
		return la.integrate(arg, x);
	}
	
	// return f(arg), evaluate as much as possible
	public static Algebraic create(String f, Algebraic arg) throws JasymcaException{
		arg = arg.reduce();
		Object fl = Lambda.pc.env.getValue(f);
		if(fl!=null && fl instanceof LambdaAlgebraic ){
			Algebraic r = ((LambdaAlgebraic)fl).f_exakt(arg);
			if(r != null)
				return r;
			if(arg instanceof Unexakt){ // Evaluate function
				return ((LambdaAlgebraic)fl).f((Zahl)arg);
			}
		}else
			fl = null;
		return new Polynomial(new FunctionVariable(f,arg,(LambdaAlgebraic)fl));
	}

		
	public boolean equals( Object x ){ 
		return x instanceof FunctionVariable &&
				fname.equals(((FunctionVariable)x).fname) &&
				 arg.equals(((FunctionVariable)x).arg);
	}
	
	public Algebraic value(Variable var, Algebraic x) throws JasymcaException{
		if(equals(var))
			return x;
		else{
			x = arg.value( var, x );
			Algebraic r = la.f_exakt(x);
			if(r != null)
				return r;
			if(x instanceof Unexakt){ // Evaluate function
				return la.f((Zahl)x);
			}
			return new Polynomial(new FunctionVariable(fname,x,la));
		}
	}
/*
	public Algebraic value(Variable var, Algebraic x) throws JasymcaException{
		if(equals(var))
			return x;
		else
			return create(fname, arg.value(var, x));
	}
*/


	public boolean smaller(Variable v)  throws JasymcaException{
		if(v==SimpleVariable.top)
			return true;
		if(v instanceof SimpleVariable)
			return false; // All Function-Variables are larger
		if(!((FunctionVariable)v).fname.equals(fname))
			return fname.compareTo(((FunctionVariable)v).fname) < 0;
		if(arg.equals(((FunctionVariable)v).arg))
			return false;
		if(arg instanceof Polynomial && ((FunctionVariable)v).arg instanceof Polynomial){
			Polynomial a = (Polynomial)arg;
			Polynomial b = (Polynomial)((FunctionVariable)v).arg;
			if(!a.var.equals(b.var))
				return a.var.smaller(b.var);
			if(a.degree() != b.degree())
				return a.degree() < b.degree();
			for(int i= a.a.length-1; i>=0; i--){
				if(!a.a[i].equals(b.a[i])){
					if(a.a[i] instanceof Zahl && b.a[i] instanceof Zahl)
						return ((Zahl)a.a[i]).smaller((Zahl)b.a[i]);
					return a.a[i].norm()<b.a[i].norm();
				}
			}
		}
		return false;		
	}
	
	public Variable cc() throws JasymcaException{
		if(fname.equals("exp") || fname.equals("log") || fname.equals("sqrt"))
			return new FunctionVariable(fname, arg.cc(),la);
		throw new  JasymcaException("Can't calculate cc for Function "+fname);
	}
		
	public String toString(){
		String a = arg.toString();
		if(a.startsWith("(") && a.endsWith(")"))
			return fname+a;
		else
			return fname+"("+a+")";
	}

}
