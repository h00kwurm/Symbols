package net.sourceforge.jasymcaandroid.jasymca;
/*
   Jasymca	- Symbolic Calculator 
   This version is written for J2ME, CLDC 1.1,  MIDP 2, JSR 75
   or J2SE


   Copyright (C) 2006, 2009 - Helmut Dersch  der@hs-furtwangen.de
   
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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.  

*/
import java.io.*;

public abstract class Algebraic{
	/** Name of variable which this Algebraic is bound to.
	*/
	String name = null;	 

	/** Add two algebraic objects.
	@param x    Algebraic object to be added.
	@return     The sum this+x.
	*/
	public abstract Algebraic add (Algebraic x) throws JasymcaException;


	/** Subtract two algebraic objects.
	@param x    Algebraic object to be subtracted.
	@return     The difference this-x.
	*/
	public Algebraic sub (Algebraic x) throws JasymcaException{
		return add(x.mult(Zahl.MINUS));
	}
	
	/** Multiply two algebraic objects.
	@param x    Algebraic object to be multiplied.
	@return     The product this*x.
	*/	
	public abstract Algebraic mult(Algebraic x) throws JasymcaException;	

	
	/** Divide two algebraic objects.
	@param x    The divisor algebraic object.
	@return     The quotient this/x.
	*/	
	public Algebraic div (Algebraic x) throws JasymcaException{
		if( x instanceof Polynomial )
			return (new Rational( this, (Polynomial)x )).reduce();
		if( x instanceof Rational )
			return ((Rational)x).den.mult(this).div(
					((Rational)x).nom );
		if( !x.scalarq() )
			return (new Matrix(this)).div( x );
		throw new JasymcaException(
			"Can not divide "+this+" through "+x);
	}


	/** Integer power function x^n. 
	@param n    The exponent n.
	@return     The power to n of this algebraic object.
	*/	
	public Algebraic pow_n(int n) throws JasymcaException{
		Algebraic pow, x=this;
		if(n<=0){
			if( n==0 || equals(Zahl.ONE) )
				return Zahl.ONE;
			if( equals( Zahl.ZERO ) )
				throw new JasymcaException("Division by Zero.");
			x = Zahl.ONE.div(x);
			n = -n;
		}
		for(pow = Zahl.ONE; ; ){
			if( (n & 1) != 0 ){
				pow = pow.mult( x );
			}
			if( (n >>= 1) != 0)
				x = x.mult(x);
			else
				break;
		}
		return pow;
	}
	

	/** Conjugate complex of an algebraic object a+i*b.
	@return     The conjugate complex a-i*b.
	*/	
	public abstract Algebraic cc() throws JasymcaException;

	/** The real part of an algebraic object a+i*b.
	@return     The real part a.
	*/	
	public Algebraic realpart() throws JasymcaException{
		return add(cc()).div(Zahl.TWO);
	}

	/** The imaginary part of an algebraic object a+i*b.
	@return     The imaginary part b.
	*/	
	public Algebraic imagpart() throws JasymcaException{
		return sub(cc()).div(Zahl.TWO).div(Zahl.IONE);
	}
	
	
	/** Differentiate with respect to a variable.
	@param x    The variable.
	@return     The derivative.
	*/	
	public abstract Algebraic deriv( Variable var ) throws JasymcaException;

	/** Integrate with respect to a variable.
	@param x    The variable.
	@return     The integral.
	*/	
	public abstract Algebraic integrate( Variable var ) throws JasymcaException;


	/** Norm of an algebraic object. Always >= 0; 0 only if this==0.
	@return     The norm.
	*/	
	public abstract double norm();

	/** Apply an algebraic function to this algebraic object.
	@param f    The algebraic function.
	@return     The function value.
	*/	
	public abstract Algebraic map( LambdaAlgebraic f ) throws JasymcaException;


	/** Rationalize all numeric constants in this algebraic object.
	Future arithmetic operations on this object are exact.
	@return     This algebraic object with rationalized numeric constants.
	*/	
	public Algebraic rat() throws JasymcaException{
		return map( new LambdaRAT() );
	}

	/** Reduce this algebraic object as much as possible, e.g.
	single element matrices are reduced to scalars etc.
	@return    This object reduced as much as possible..
	*/		
	public Algebraic reduce()  throws JasymcaException{ 
		return this; 
	}


	/** The value of this algebraic expression, if variable var
	assumes the value of the algebraic expression v
	@param  var   the variable to substitute.
	@param  x     the value to substitute for var.
	*/
	public Algebraic value(Variable var, Algebraic x) throws JasymcaException{
		return this;
	}
	

	// Following are conveniance methods to query properties
	
	/** Query: Does this algebraic object depend on the variable var.
	@param var  The variable.
	@return     True if this object depends on var, false otherwise.
	*/		
	public boolean depends(Variable var) { return false; }

	/** Query: Is this algebraic object a rational function
	of the variable v.
	@param var  The variable.
	@return     True if this object is a rational function
	of v, false otherwise.
	*/		
	public boolean ratfunc(Variable v){ return true; }

	/** Query: Does this algebraic object depend on the variable var,
	does not recurse into functions.
	@param var  The variable.
	@return     True if this object depends on var, false otherwise.
	*/			
	public boolean depdir(Variable var){ 
		return depends(var) && ratfunc(var);
	}

	/** Query: Is this algebraic object konstant, i.e does not
	depend on any variable.
	@return     True if this object is constant, false otherwise.
	*/		
	public boolean constantq(){ return false; }
	

	/** Query: Is this algebraic object equal to x.
	@param x    The object for comparison.
	@return     True if this object equals x, false otherwise.
	*/		
	public abstract boolean equals(Object x);	


	/** Query: Is this algebraic object complex.
	@return     True if this object is complex, false otherwise.
	*/		
	public boolean komplexq() throws JasymcaException{
		return !imagpart().equals(Zahl.ZERO);
	}

	/** Query: Is this algebraic object a scalar.
	@return     True if this object is a scalar, false otherwise.
	*/		
	public boolean scalarq(){ return true; }

	/** Query: Is this algebraic object exact.
	@return     True if this object is exact, false otherwise.
	*/		
	public boolean exaktq(){ return false; }
	
	/** Promote this algebraic object to at least the same 
	type and size as b. b must be of type Vektor or
	Matrix. If this is a scalar type, an algebraic Object
	the same size as b is returned with constant 
	components equal to this. If a has equal size to b,
	a is returned unchanged. In all other cases 
	this is returned.
	@param a Algebraic object to be promoted.
	@param b Algebraic object specifying the target size.
	@return Algebraic object equal sized to b.
	*/
	public Algebraic promote(	Algebraic b) 
								throws JasymcaException{
		if( b.scalarq() )
			return this;
		if( b instanceof Vektor ){
			Vektor bv = (Vektor)b;
			if( this instanceof Vektor &&
				((Vektor)this).length() == bv.length() )
					return this;
			if( scalarq() )
				return new Vektor( this, bv.length() );
		}
		if( b instanceof Matrix ){
			Matrix bm = (Matrix)b;
			if( this instanceof Matrix &&
			    bm.equalsized((Matrix)this ))
					return this;
			if( scalarq() )
				return new Matrix( this,bm.nrow(), bm.ncol());
		}
		throw new JasymcaException("Wrong argument type.");
	}
	
	/** Print a string representation of this algebraic object
	into a given Outputstream.
	@param     The Outputstream where data will be sent to.
	*/		
	public void print( PrintStream p ){
		p.print( StringFmt.compact(toString()) );
	}
	

	/** Auxiliary routine for Error reporting.
	*/		
	static void p(String s){ Lambda.p(s); }

	public Algebraic map_lambda( LambdaAlgebraic lambda, Algebraic arg2 ) 
							throws ParseException,JasymcaException{
		if( arg2 == null ){
			Algebraic r = lambda.f_exakt( this );
			if( r!=null )
				return r;
			String fname = lambda.getClass().getName();
			if(fname.startsWith(Environment.CLASS_PREFIX + "Lambda")){
				fname = fname.substring(Environment.CLASS_PREFIX.length() + "Lambda".length());
				fname = fname.toLowerCase();
				return FunctionVariable.create(fname, this);
			}
			// give up
		 	throw new JasymcaException("Wrong type of arguments.");				
		}else
			return lambda.f_exakt( this, arg2 );
	}

}



