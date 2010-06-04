package net.sourceforge.jasymcaandroid.jasymca;
/* Jasymca	-	- Symbolic Calculator for Mobile Devices
   This version is written for J2ME, CLDC 1.1,  MIDP 2, JSR 75
   or J2SE


   Copyright (C) 2006,2009 - Helmut Dersch  der@hs-furtwangen.de
   
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

import java.util.*;
import java.io.*;

public class Polynomial extends Algebraic{
	public  Algebraic[] a   	=	null;
	public Variable 	var		=	null;

	// Constructors

	public Polynomial() {}

	/** Create polynomial with given variable and coefficients
	@param v  Variable.
	@param a  Coefficients.
	@return   The polynomial.
	*/		
	public Polynomial(Variable var, Algebraic[] a){
		this.var = var;
		this.a 	 = Poly.reduce(a);
	}

	/** Create polynomial with given variable and coefficients
	@param v  Variable.
	@param a  Vektor of coefficients.
	@return   The polynomial.
	*/		
	public Polynomial(Variable var, Vektor v) throws JasymcaException{
		this.var 	= var;
		this.a      = new Algebraic[v.length()];
		for(int i=0; i<a.length; i++)
			a[i] 	= v.get(a.length-1-i);
		this.a 	  	= Poly.reduce(a);
	}

	/** Create polynomial with given variable and 
	and default coefficients: p = var
	@param v  Variable.
	@return   The polynomial.
	*/		
	public Polynomial(Variable var){
		a 		= new Zahl[] { Zahl.ZERO, Zahl.ONE };
		this.var= var;
	}
	

	/** The main variable of this polynomial.
	@return   The main variable.
	*/		
	public Variable getVar(){
		return var;
	}
	
	
	/** The coefficients of this polynomial as vector.
	@return   The coeffcient vector.
	*/			
	public Vektor coeff(){
		Algebraic[] c = Poly.clone(a); 
		return new Vektor(c);
	}
		
	/** The polynomial coefficient of variable v and degree n is 
	calculated. Occurances of v inside nonrational functions are ignored. 
	@param v  Variable.
	@param n  Integer exponent.
	@return   The coefficient of v^n in p.
	*/	
	public Algebraic coefficient( Variable var, int n )throws JasymcaException{
		if( var.equals(this.var) )
			return coefficient(n);
		Algebraic c = Zahl.ZERO;
		for(int i=0; i<a.length; i++){
			Algebraic ci = a[i] ;
			if(ci instanceof Polynomial)
				c = c.add( ((Polynomial)ci).coefficient(var,n).
					mult((new Polynomial(this.var)).pow_n(i)));
			else if(n==0){
				c = c.add( ci.
					mult((new Polynomial(this.var)).pow_n(i)));
			}	
		}
		return c;
	}

	/** The polynomial coefficient of the main variable v and degree n is 
	calculated. Occurances of v inside nonrational functions are ignored. 
	@param n  Integer exponent.
	@return   The coefficient of v^n in p.
	*/	
	public Algebraic coefficient(int i) throws JasymcaException{
		if(i>=0 && i<a.length)
			return a[i];
		return Zahl.ZERO;
	}
	
			
	/** Query: Is this algebraic object a rational function
	of the variable v.
	@param var  The variable.
	@return     True if this object is a rational function
	of v, false otherwise.
	*/		
	public boolean ratfunc(Variable v){
		if( var instanceof FunctionVariable  &&
			((FunctionVariable)this.var).arg.depends(v))
			return false;
		for(int i=0; i<a.length; i++)
			if(!a[i].ratfunc(v))
				return false;
		return true;
	}

	/** The polynomial degree of the main variable v 
	is calculated. Occurances of v inside nonrational
	functions are ignored. 
	@return   The polynomial degree of v.
	*/	
	public int degree(){ return a.length-1; }

	/** The polynomial degree of variable v 
	is calculated. Occurances of v inside nonrational
	functions are ignored. 
	@param v  Variable.
	@return   The polynomial degree of v.
	*/
	public int degree(Variable v){ 
		if(v.equals(var))
			return a.length-1;
		int degree = 0;
		for(int i=0; i<a.length; i++){
			int d = Poly.degree(a[i], v);
			if( d > degree ) degree = d;
		}
		return degree;
	}
	
	/** Add two algebraic objects.
	@param x    Algebraic object to be added.
	@return     The sum this+x.
	*/	
	public Algebraic add( Algebraic p) throws JasymcaException{
		if(p instanceof Rational)
			return p.add(this);
		if(p instanceof Polynomial){
			if( var.equals(((Polynomial)p).var)){
				int len = Math.max(a.length, ((Polynomial)p).a.length);
				Algebraic[] csum = new Algebraic[len];
				for(int i=0; i<len; i++)
					csum[i] = coefficient(i).add(((Polynomial)p).coefficient(i));
				return (new Polynomial(var, csum)).reduce();
			}else if( var.smaller(((Polynomial)p).var)){
				return p.add( this );
			}
		}
		Algebraic[] csum = Poly.clone( a );
		csum[0] = a[0].add(p);
		return (new Polynomial(var, csum)).reduce();
	}

	/** Multiply two algebraic objects.
	@param x    Algebraic object to be multiplied.
	@return     The product this*x.
	*/			
	public Algebraic mult( Algebraic p) throws JasymcaException{
		if(p instanceof Rational)
			return p.mult(this);
		if(p instanceof Polynomial){
			if( var.equals(((Polynomial)p).var)){
				int len = a.length + ((Polynomial)p).a.length-1;
				Algebraic[] cprod = new Algebraic[len];
				for(int i=0; i<len; i++) cprod[i] = Zahl.ZERO;
				for(int i=0; i<a.length; i++)
					for(int k=0; k<((Polynomial)p).a.length; k++)
						cprod[i+k] = cprod[i+k].add( a[i].mult(((Polynomial)p).a[k]) );
				return new Polynomial(var, cprod).reduce();
			}else if( var.smaller( ((Polynomial)p).var )){
				return p.mult( this );
			}
		}		
		Algebraic[] cprod = new Algebraic[a.length];
		for(int i=0; i<a.length; i++)
			cprod[i] = a[i].mult(p);		
		return new Polynomial(var, cprod).reduce();
	}

	/** Divide two algebraic objects.
	@param x    The divisor algebraic object.
	@return     The quotient this/x.
	*/		
	public Algebraic div( Algebraic q) throws JasymcaException{
		if( q instanceof Zahl ){
			Algebraic c[] = new Algebraic[a.length];
			for(int i=0; i<a.length; i++)
				c[i] = a[i].div(q);
			return new Polynomial( var, c );
		}
		return super.div(q);
	}
	

	/** Reduce this algebraic object as much as possible, e.g.
	single element matrices are reduced to scalars etc.
	@return    This object reduced as much as possible..
	*/			
	public Algebraic reduce() throws JasymcaException{
		if( a.length == 0 )
			return Zahl.ZERO;
		if( a.length == 1 )
			return a[0].reduce();
		return this;
	}
	
		
	/** Create string representation of this polynomial.
	@return    A printable version of this polynomial.
	*/								
	public String toString(){
		Vector x = new Vector();	
		for(int i=a.length-1; i>0;i--){
			if(a[i].equals(Zahl.ZERO) )
				continue;	
			String s = "";		
			if(a[i].equals(Zahl.MINUS))
				s+="-";
			else if( !a[i].equals(Zahl.ONE) )
				s+=a[i].toString()+"*";
			s+=var.toString();
			if(i>1)
				s+="^"+i;
			x.addElement(s);
		}
		if( !a[0].equals(Zahl.ZERO) )
			x.addElement(a[0].toString());	
		String s = "";
		if(x.size()>1)
			s+="(";
		for(int i=0; i<x.size(); i++){
			s += (String)x.elementAt(i);
			if( i< x.size()-1 && !(((String)x.elementAt(i+1)).charAt(0)=='-'))
				s+="+";
		}
		if(x.size()>1)
			s+=")";
		return s;
	}

	/** Query: Is this algebraic object equal to x.
	@param x    The object for comparison.
	@return     True if this object equals x, false otherwise.
	*/			
	public boolean equals(Object x){
		if (! (x instanceof Polynomial) )
			return false;
		if(!(var.equals(((Polynomial)x).var)) || 
			a.length != ((Polynomial)x).a.length)
			return false;
		for(int i=0; i<a.length; i++)
			if(!a[i].equals(((Polynomial)x).a[i]))
				return false;
		return true;
	}
	
	

	/** Differentiate with respect to a variable.
	@param x    The variable.
	@return     The derivative.
	*/	
	public Algebraic deriv( Variable var )  throws JasymcaException{
		Algebraic r1 = Zahl.ZERO, r2 = Zahl.ZERO;
		Polynomial x = new Polynomial(this.var);
		//  differenziere poly nach this.var=x
		//  r1 = dpoly/dx = 
		//  ((((a[n-1]*x*(n-1)+a[n-2]*(n-2))*x+...+a[2]*2*x;
		for(int i=a.length-1; i>1; i--){					
			r1 = r1.add(a[i].mult(new Unexakt(i))).mult(x); 
		}
		if(a.length>1)
			r1 = r1.add(a[1]);

		//  differenziere coefficients nach var=v
		//  (((( da[n-1]/dv*x+da[n-2]/dv)*x+...+da[1]/dv*x;
		for(int i=a.length-1; i>0; i--){					
			r2 = r2.add(a[i].deriv(var)).mult(x);		
		}
		if(a.length>0)
			r2 = r2.add(a[0].deriv(var));

		//  differenziere this.var=x nach var=v
		//  dpoly/dv = r1*dx/dv + r2
		return r1.mult(this.var.deriv(var)).add(r2).reduce();
	}

	/** Query: Does this algebraic object depend on the variable var.
	@param var  The variable.
	@return     True if this object depends on var, false otherwise.
	*/			
	public boolean depends(Variable var){ 
		if(a.length==0) return false;
		if( this.var.equals(var) )
			return true;
		if( this.var instanceof FunctionVariable && ((FunctionVariable)this.var).arg.depends(var))
			return true;
		for(int i=0; i<a.length; i++)
			if(a[i].depends(var))
				return true;
		return false;
	}


	// Flag to stop infinite partial integrations
	static boolean loopPartial = false;

	/** Integrate with respect to a variable.
	@param x    The variable.
	@return     The integral.
	*/		
	public Algebraic integrate( Variable var ) throws JasymcaException{
		Algebraic in = Zahl.ZERO;
		for(int i=1; i<a.length; i++){
			if(!a[i].depends(var))
				if(var.equals(this.var))
					// c*x^n -->1/(n+1)*x^(n+1)
					in=in.add(a[i].mult(new Polynomial(var).pow_n(i+1).div(new Unexakt(i+1))));
				else if(this.var instanceof FunctionVariable && 
						((FunctionVariable)this.var).arg.depends(var))
					// f(x)
						if(i==1)
							in=in.add( ((FunctionVariable)this.var).integrate(var).mult(a[1]));
					// (f(x))^2, (f(x))^3 etc
					// give up here but try again after exponential normalization
						else
							throw new JasymcaException("Integral not supported.");
				else
					// Constant:  c --> c*x
					in=in.add(a[i].mult(new Polynomial(var).mult(new
													Polynomial(this.var).pow_n(i))));
			else
				if(var.equals(this.var))
					// c(x)*x^n , should not happen if this is canonical
					throw new JasymcaException("Integral not supported.");
				else if(this.var instanceof FunctionVariable && 
					((FunctionVariable)this.var).arg.depends(var)){
					if(i==1 && a[i] instanceof Polynomial && 
						((Polynomial)a[i]).var.equals(var)){
						// poly(x)*f(x)						
						// First attempt: try to isolate inner derivative
						// poly(x)*f(w(x)) --> check poly(x)/w' == q : const?
						//           yes   --> Int f dw * q
						p("Trying to isolate inner derivative "+this);
						try{
							FunctionVariable f = (FunctionVariable)this.var;
							Algebraic w = f.arg; 		// Innere Funktion
							Algebraic q = a[i].div(w.deriv(var));
							if(q.deriv(var).equals(Zahl.ZERO)){ // q - constant
								SimpleVariable v = new SimpleVariable("v");
								Algebraic p = FunctionVariable.create(f.fname, new Polynomial(v));
								Algebraic  r = p.integrate(v).value(v,w).mult(q);
								in=in.add(r);
								continue;
							}
						}catch(JasymcaException je){
							// Didn't work, try more methods
						}
						p("Failed.");
						
						// Some partial integrations follow. To 
						// avoid endless loops, we flag this section

						// Coefficients of a[i] must not depend on var
						for(int k=0;k<((Polynomial)a[i]).a.length;k++)
							if(((Polynomial)a[i]).a[k].depends(var))
								throw new JasymcaException("Function not supported by this method");


						if(loopPartial){
							loopPartial = false;
							p("Partial Integration Loop detected.");
							throw new JasymcaException("Partial Integration Loop: "+this);
						}

						// First attempt: x^n*f(x) , n-times diff!
						// works for exp,sin,cos
						p("Trying partial integration: x^n*f(x) , n-times diff "+ this);
						try{
							loopPartial=true;
							Algebraic  p = a[i];
							Algebraic  f = ((FunctionVariable)this.var).integrate(var);
							Algebraic  r = f.mult(p);
							while(!(p=p.deriv(var)).equals(Zahl.ZERO)){
								f = f.integrate(var).mult(Zahl.MINUS);
								r = r.add(f.mult(p));
							}
							loopPartial=false;
							in=in.add(r);
							continue;
						}catch (JasymcaException je){
							loopPartial=false;
						}
						p("Failed.");
						// Second attempt: x^n*f(x) , 1-times int!
						// works for log, atan	
						p("Trying partial integration: x^n*f(x) , 1-times int "+ this);
						try{
							loopPartial=true;
							Algebraic  p = a[i].integrate(var);
							Algebraic  f = new Polynomial((FunctionVariable)this.var);
							Algebraic  r = p.mult(f).sub(p.mult(f.deriv(var)).integrate(var));
							loopPartial=false;
							in=in.add(r);
							continue;
						}catch(JasymcaException je3){
							loopPartial=false;
						}
						p("Failed");
						// Add more attempts....
						throw new JasymcaException("Function not supported by this method");
					}else
						throw new JasymcaException("Integral not supported.");
				}else // mainvar independend of var, treat as constant and integrate a
					in=in.add(a[i].integrate(var).mult(new
									Polynomial(this.var).pow_n(i)));
		}
		if(a.length>0)
			in=in.add(a[0].integrate(var));
		return in;
	}
	
	/** Conjugate complex of a polynomial a+i*b.
	@return     The conjugate complex a-i*b.
	*/	
	public Algebraic cc() throws JasymcaException{
		Polynomial xn = new Polynomial( var.cc() );
		Algebraic r = Zahl.ZERO;
		for(int i=a.length-1; i>0; i--)
			r = r.add( a[i].cc() ).mult(xn);
		if(a.length>0)
			r = r.add(a[0].cc());
		return r;
	}
		
					
	/** The value of this polynomial, if variable var
	assumes the value of the algebraic expression v
	@param  var   the variable to substitute.
	@param  x     the value to substitute for var.
	*/
	public Algebraic value(Variable var, Algebraic x) throws JasymcaException{
		Algebraic r  = Zahl.ZERO;
		Algebraic v  = this.var.value(var,x);
		for(int i=a.length-1; i>0; i--){		// Horner
			r = r.add(a[i].value(var,x)).mult(v); 
		}
		if(a.length>0)
			r = r.add(a[0].value(var,x));
		return r;
	}

	/** The value of this polynomial, if the main variable var
	assumes the value of the algebraic expression v
	@param  x     the value to substitute for var.
	*/
	public Algebraic value(Algebraic x) throws JasymcaException{
		return value(this.var, x);
	}

	/** Query: Is this algebraic object exact.
	@return     True if this object is exact, false otherwise.
	*/		
	public boolean exaktq(){ 
		boolean exakt = a[0].exaktq();
		for(int i=1; i<a.length; i++)
			exakt = exakt && a[i].exaktq();
		return exakt;
	}
	
	
	/** Norm of an algebraic object. Always >= 0; 0 only if this==0.
	@return     The norm.
	*/	
	public double norm(){
		double norm=0.;
		for(int i=0; i<a.length; i++)
			norm+=a[i].norm();
		return norm;
	}

	/** Apply an algebraic function to this algebraic object.
	@param f    The algebraic function.
	@return     The function value.
	*/	
	public Algebraic map( LambdaAlgebraic f ) throws JasymcaException{
		Algebraic x = var instanceof SimpleVariable ? new Polynomial(var):
			FunctionVariable.create(((FunctionVariable)var).fname,
					f.f_exakt(((FunctionVariable)var).arg));
		Algebraic r=Zahl.ZERO;
		for(int i=a.length-1; i>0; i--){
			r=r.add(f.f_exakt(a[i])).mult(x);
		}
		if(a.length>0)
			r=r.add(f.f_exakt(a[0]));
		return r;
	}



	/** Divide polynomial through main coefficient.
	An exception is thrown if this is nonconstant or 0.
	@return     The monic polynomial.
	*/	
	public Polynomial monic() throws JasymcaException{
		Algebraic cm = a[a.length-1];
		if(cm.equals(Zahl.ONE))
			return this;
		if(cm.equals(Zahl.ZERO) || (cm.depends(var)) )
			throw new JasymcaException("Ill conditioned polynomial: main coefficient Zero or not number");
		Algebraic b[] = new Algebraic[a.length];
		b[a.length-1] = Zahl.ONE;
		for(int i=0; i<a.length-1; i++)
			b[i] = a[i].div(cm);
		return new Polynomial(var, b);
	}
	

	////////////////////////// Roots /////////////////////////////////////////////////////

	/** Square-free decomposition of this polynomial.
	Given y=p1*p2^2*p3^3*..., this function returns a vektor
	of factors [p1,p2,p3..].
	<p>
	The algorithm repeatedly devides by the derivative dy/dvar,
	and calculates the polynomial gcd.
	@param  var The Variable.
	@return     The vector of square-free factors. 
	*/	
	public Algebraic[] square_free_dec(Variable var) throws JasymcaException{
		if( !ratfunc(var) )
			return null;
		Algebraic dp = deriv(var);
		Algebraic gcd_pdp = Poly.poly_gcd(this,dp);
		Algebraic q = Poly.polydiv(this,gcd_pdp); 
		Algebraic p1 = Poly.polydiv(q, Poly.poly_gcd(q,gcd_pdp));
		if(gcd_pdp instanceof Polynomial &&
			gcd_pdp.depends( var ) &&
			 ((Polynomial)gcd_pdp).ratfunc(var)){
			Algebraic sq[] = ((Polynomial)gcd_pdp).square_free_dec(var);
			Algebraic result[] = new Algebraic[sq.length+1];
			result[0] = p1;
			for(int i=0; i<sq.length;i++)
				result[i+1]=sq[i];
			return result;
		}else{
			Algebraic result[] = { p1 };
			return result;
		}
	}
	
	/** Numeric reatest common denominator of all coefficients
	*/	
	public Zahl gcd_coeff() throws JasymcaException{
		Zahl gcd;
		if(a[0] instanceof Zahl)
			gcd = (Zahl)a[0];
		else if(a[0] instanceof Polynomial)
			gcd = ((Polynomial)a[0]).gcd_coeff();
		else
			throw new JasymcaException("Cannot calculate gcd from "+this);
		for(int i=1; i<a.length; i++){
			if(a[i] instanceof Zahl)
				gcd = gcd.gcd((Zahl)a[i]);
			else if(a[i] instanceof Polynomial)
				gcd = gcd.gcd(((Polynomial)a[i]).gcd_coeff());
			else
				throw new JasymcaException("Cannot calculate gcd from "+this);
		}
		return gcd;
	}

	/** Solve polynomial equation for variable var.
	Equations of degree 1,2 and biquadratic/bicubic..
	equations are solved symbolically, all others numerically.
	@param  var The Variable.
	@return     The solution vector. 
	*/	
	public Vektor solve(Variable var) throws JasymcaException{
		if(!var.equals(this.var)) // substitute var <--> top
			return ((Polynomial)value(var, Poly.top)).solve(SimpleVariable.top);			
		Algebraic[] factors = square_free_dec(var);
		Vector s = new Vector();
		int n = factors==null?0:factors.length;
		for(int i=0; i<n; i++){
			if(factors[i] instanceof Polynomial){
				Vektor sol = null;
				Algebraic equ = factors[i];
				try{ 							// (1) Symbolic solution 
					sol = ((Polynomial)equ).solvepoly();
				}catch(JasymcaException je){ 	// (2)  Numeric solution
					sol = ((Polynomial)equ).monic().roots();
				}
				for(int k=0; k<sol.length(); k++){
					s.addElement(sol.get(k));
				}
			}
		}
		Algebraic cn[] = new Algebraic[s.size()];
		for(int i=0; i<cn.length; i++){
			cn[i] = (Algebraic)s.elementAt(i);
		}
		return new Vektor(cn);
	}

	/** Solve polynomial equation for variable var.
	Try all symbolic solution methods (degree 1,2 and 
	biquadratic/bicubic). Throws exception if no
	symbolic solution could be found.
	@return     The solution vector. 
	*/	
	public Vektor solvepoly() throws JasymcaException{
		Vector s = new Vector();
		switch(degree()){
			case 0: break;
			case 1: 
				s.addElement(Zahl.MINUS.mult(a[0].div(a[1]) ));
				break;
			case 2: 
				Algebraic p = a[1].div(a[2]);
				Algebraic q = a[0].div(a[2]);
				p = Zahl.MINUS.mult(p).div(Zahl.TWO);
				q = p.mult(p).sub(q);
				if(q.equals(Zahl.ZERO)){
					s.addElement(p); 
					break;
				}
				q = FunctionVariable.create("sqrt", q);
				s.addElement(p.add(q));
				s.addElement(p.sub(q));
				break;
			default: 
				// Maybe biquadratic/bicubic etc
				// Calculate gcd of exponents for nonzero coefficients
				int gcd = -1;
				for(int i=1; i<a.length; i++){
					if(!a[i].equals(Zahl.ZERO)){
						if(gcd<0) gcd=i;
						else gcd=Poly.gcd(i,gcd);
					}
				}
				int deg = degree()/gcd;
				if(deg <3){ // Solveable
					Algebraic cn[] = new Algebraic[deg+1];
					for(int i=0; i<cn.length; i++)
						cn[i] = a[i*gcd];
					Polynomial pr = new Polynomial(var, cn);
					Vektor sn = pr.solvepoly();
					if(gcd==2){ // sol = +/-sqrt(sn)
						cn = new Algebraic[sn.length()*2];
						for(int i=0; i<sn.length(); i++){
							cn[2*i] = FunctionVariable.create("sqrt", sn.get(i));
							cn[2*i+1] = cn[2*i].mult(Zahl.MINUS);
						}
					}else{ // sol = sn^(1/gcd);
						cn = new Algebraic[sn.length()];
						Zahl wx = new Unexakt(1./gcd);
						for(int i=0; i<sn.length(); i++){
							Algebraic exp = FunctionVariable.create("log",sn.get(i));
							cn[i] = FunctionVariable.create("exp", exp.mult(wx));
						}
					}
					return new Vektor(cn);
				}
				throw new JasymcaException("Can't solve expression "+this);
		}
		return Vektor.create(s);
	}




	/** Solve polynomial equation p = 0. p must have
	constant coefficients. This routine is an interface to the
	aberth-method in pzeros.f (http://netlib.org/numeralgo/na10).
	@return     The solution vector. 
	*/	
	public Vektor roots() throws JasymcaException{
		// Handle trivial cases
		if(a.length==2){
			Algebraic[] result = { a[0].mult(Zahl.MINUS).div(a[1]) };
			return new Vektor(result);
		}else if(a.length==3){
			return new Vektor( Poly.pqsolve(a[1].div( a[2]),
									   a[0].div( a[2]) ));
		}
		// All other cases must have constant coefficients
		double[] ar  = new double[a.length];
		double[] ai  = new double[a.length];
		boolean[] err = new boolean[a.length];
		boolean komplex = false;

		for(int i=0; i<a.length; i++){
			Algebraic cf = a[i]; 
			if(!(cf instanceof Zahl) )
				throw new JasymcaException("Roots requires constant coefficients.");
			ar[i] = ((Zahl)cf).unexakt().real;
			ai[i] = ((Zahl)cf).unexakt().imag;
			if(ai[i] != 0.0)
				komplex = true;
		}
		if(komplex)
			Pzeros.aberth( ar, ai, err );
		else{
			Pzeros.bairstow( ar, ai, err );
			boolean ok = true;

			for (int i=0; i<err.length-1; i++){
				if(err[i]) ok= false;
			}
			if(!ok){
				for(int i=0; i<a.length; i++){
					Algebraic cf = a[i]; 
					ar[i] = ((Zahl)cf).unexakt().real;
					ai[i] = ((Zahl)cf).unexakt().imag;
				}
				Pzeros.aberth( ar, ai, err );
				//roots_real( ar, ai );
			}
		}
		Algebraic r[] = new Algebraic[a.length-1];
		for (int i=0; i<r.length; i++){
			if(!err[i]){
				Unexakt x0 = new Unexakt(ar[i],ai[i]);
				r[i] = x0;//new Polynomial(new Root(x0, this, i));
			}else{
				throw new JasymcaException("Could not calculate root "+i);
			}
		}

/*		
		if( !komplex ){ // Massage roots to real + cc-pairs
			r = roots_real( r );
		}
*/		
		return new Vektor(r);
	}
	
	/** Rounds the array of algebraics in x[] to conform
	to the requirements of roots of polynomials with real
	coefficients: Pairs of conjugate-complex zeros are identified,
	and forced to be exactly cc.
	Spurious real or imaginary parts are deleted.
	@param   x  The array of roots.
	@return     The array of rounded roots. 
	*/	
/*
	static void  roots_real( double real[], double imag[] ) throws JasymcaException{
		double eps = 1.0e-12;
		for(int i=0; i<real.length; i++){
			double r = real[i], j=imag[i];
			if( Math.abs(j) < eps*Math.abs(r) ){
				imag[i] = 0.0;
			}else if(i < real.length-1){
				// find the closest candidate for the cc-root
				int cc = i+1;
				double d_min = Math.abs( r-real[cc] ) + Math.abs( j+imag[cc] );				
				for(int k=i+2; k<real.length; k++){
					double d = Math.abs( r-real[k] ) + Math.abs( j+imag[k] );	
					if(d<d_min){
						d_min = d;
						cc = k;
					}
				}
				if( d_min/(Math.abs(r)+Math.abs(j)) < eps ) {
					double tmp = imag[i+1];
					imag[i]   = (j - imag[ cc ]) / 2.0;
					imag[i+1] = -imag[i];
					imag[cc]  = tmp;
					if( Math.abs(r) < eps*Math.abs(r) ){
						r = 0;
					}else{
						r = (r+real[cc])/2.0;
					}
					tmp 	  = real[i+1];
					real[i]   = r;
					real[i+1] = r;
					real[cc]  = tmp;
					i++;
				}
			}
		}
	}
*/
}				
	

	

///////////// Unused routines for cubic and quartic roots
/*
	static Unexakt[] cuberoot( Unexakt x ){
		double r = Math.pow( x.real*x.real +x.imag*x.imag, 1./6.) ;
		double phi = Math.atan2( x.imag, x.real ) / 3.0;
		Unexakt[] c = new Unexakt[3];
		c[0] = new Unexakt( r*Math.cos( phi ), r*Math.sin( phi ));
		phi += 2.0 * Math.PI / 3.0;
		c[1] = new Unexakt( r*Math.cos( phi ), r*Math.sin( phi ));
		phi += 2.0 * Math.PI / 3.0;
		c[2] = new Unexakt( r*Math.cos( phi ), r*Math.sin( phi ));
		return c;
	}
	
	
	static Algebraic[] cubesolve(Algebraic a, Algebraic b, Algebraic c ) throws JasymcaException{
		Unexakt Z3=new Unexakt(3.0), Z9=new Unexakt(9.0),
		        Z2_27=new Unexakt(2.0/27.0),
		        Za=new Unexakt(-1.0/2.0,  Math.sqrt(3.0)/2.0),
		        Zb=new Unexakt(-1.0/2.0, -Math.sqrt(3.0)/2.0);
		        
		Algebraic p = Z3.mult(b).sub(a.mult(a)).div( Z9 );
		Algebraic q = c.add(
					Z2_27.mult(a).mult(a).mult(a)).sub(
					a.mult(b).div( Z3 )).div(Zahl.TWO);
					

		Algebraic D   = p.pow_n(3).add(q.pow_n(2)); 
		Algebraic D2  = FunctionVariable.create("sqrt", D);

		q = q.mult(Zahl.MINUS);
		Algebraic[] u = cuberoot((Unexakt)q.add(D2));
		Algebraic[] v = cuberoot((Unexakt)q.sub(D2));

		// select best combinations with u*v=-p
		int m = 0;
		double tmin = u[m/3].mult(v[m%3]).add(p).norm();
		for(int i=1; i<9; i++){
			double t = u[i/3].mult(v[i%3]).add(p).norm();
			if(t<tmin){
				tmin = t; m=i;
			}
		}
		
		Algebraic[] x = new Unexakt[3];
		Algebraic   d = a.div(Z3);
		x[0] = u[m/3].add(v[m%3]).sub(d);
		x[1] = u[m/3].mult(Za).add(v[m%3].mult(Zb)).sub(d);
		x[2] = u[m/3].mult(Zb).add(v[m%3].mult(Za)).sub(d);
		return x;
	}
	
	
		

	// this works for constant complex coefficients				
	public Vektor quartic() throws JasymcaException{
		// Handle trivial cases
		if(coef.length==2){
			Algebraic[] result = { coef[0].mult(Zahl.MINUS).div(coef[1]) };
			return new Vektor(result);
		}else if(coef.length==3){
			return new Vektor( pqsolve(coef[1].div( coef[2]),
									   coef[0].div( coef[2]) ));
		}
		Unexakt[] c = new Unexakt[coef.length];
		Unexakt Z4=new Unexakt(4.0), Z8=new Unexakt(8.0);
		for(int i=0; i<coef.length; i++){
			Algebraic cf = coef[coef.length-i-1]; // Reverse order
			if(!(cf instanceof Zahl))
				throw new JasymcaException("Quartic requires constant coefficients.");
			c[i] = ((Zahl)cf).unexakt();
			c[i] = (Unexakt)c[i].div(c[0]);						  
		}
		
		// Handle trivial cases
		if(coef.length==4){
			return new Vektor( cubesolve(c[1],c[2],c[3]) );
		}
		
		// Kubische Hilfsgleichung
		Algebraic a = Zahl.MINUS.mult(c[2]).div(Zahl.TWO);
		Algebraic b = c[1].mult(c[3]).div(Z4).sub(c[4]);
		Algebraic c1 = c[4].mult(Z4.mult(c[2]).sub(c[1].mult(c[1]))).
						sub(c[3].mult(c[3])).div(Z8);

		Algebraic[] ys =	cubesolve( a, b, c1 );
		Algebraic y = ys[0];
		if( ys[1].imagpart().norm() < y.imagpart().norm() )
			y = ys[1];
		if( ys[2].imagpart().norm() < y.imagpart().norm() )
			y = ys[2];
//		((Unexakt)y).imag = 0.0;
		
		Algebraic A = FunctionVariable.create("sqrt",
					Z8.mult(y).add(c[1].mult(c[1])).sub(Z4.mult(c[2])));
		// quadratische Hilfsgleichung
		Algebraic p = c[1].add(A).div(Zahl.TWO);
		Algebraic q = y.add(c[1].mult(y).sub(c[3]).div(A));
		
		Algebraic[] xs1 = pqsolve( p, q );
		
		A = A.mult(Zahl.MINUS);		
		// quadratische Hilfsgleichung
		p = c[1].add(A).div(Zahl.TWO);
		q = y.add(c[1].mult(y).sub(c[3]).div(A));
		
		Algebraic[] xs2 = pqsolve(p, q );
		
		Algebraic xs[] = new Algebraic[4];
		xs[0] = xs1[0];xs[1] = xs1[1];
		xs[2] = xs2[0];xs[3] = xs2[1];
		
		return new Vektor(xs);
	}
*/
		
