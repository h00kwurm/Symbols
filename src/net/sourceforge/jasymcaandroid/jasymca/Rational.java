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

public class Rational extends Algebraic{

	Algebraic  nom; // Zahl oder Polynomial
	Polynomial den;

	// Constructors

	/** Create rational given noinator and denominator
	@param nom  Nominator.
	@param den  Denominator.
	@return   The rational expression nominator/denominator.
	*/			
	public Rational(Algebraic nom, Polynomial den) throws JasymcaException{
		Algebraic norm = den.a[den.degree()];
		if(norm instanceof Zahl){
			this.nom = nom.div(norm);
			this.den = (Polynomial)den.div(norm);
		}else{
			this.nom = nom;
			this.den = den;
		}
	}

	/** Query: Is this algebraic object a rational function
	of the variable v.
	@param var  The variable.
	@return     True if this object is a rational function
	of v, false otherwise.
	*/			
	public boolean ratfunc(Variable v){
		return nom.ratfunc(v) && den.ratfunc(v);
	}
	

	/** Reduce this algebraic object as much as possible, e.g.
	single element matrices are reduced to scalars etc.
	@return    This object reduced as much as possible..
	*/		
	public Algebraic reduce() throws JasymcaException{
		if(nom instanceof Zahl){
			if( nom.equals(Zahl.ZERO) )
				return Zahl.ZERO;
			return this;
		}

		Algebraic pq[] = { nom, den };
		pq = Exponential.reduce_exp(pq);
		if( !nom.equals(pq[0]) || !den.equals(pq[1]) ){
			return pq[0].div(pq[1]).reduce();
		}
		if(exaktq()){
			Algebraic gcd= Poly.poly_gcd(den,nom);
			if(!gcd.equals(Zahl.ONE)){
				Algebraic n = Poly.polydiv(nom,gcd);
				Algebraic d = Poly.polydiv(den,gcd);
				if(d.equals(Zahl.ONE))
					return n;
				else if(d instanceof Zahl)
					return n.div(d);
				else
					return new Rational(n,(Polynomial)d);
			}
		}
		return this;
	}

	/** Query: Is this algebraic object exact.
	@return     True if this object is exact, false otherwise.
	*/		
	public boolean exaktq(){ 
		return nom.exaktq() && den.exaktq();
	}
	
	
	
	/** Add two algebraic objects.
	@param x    Algebraic object to be added.
	@return     The sum this+x.
	*/	
	public Algebraic add(Algebraic x) throws JasymcaException{
		if(x instanceof Rational)
			return nom.mult(((Rational)x).den).add(((Rational)x).
					nom.mult(den)).div(den.mult(((Rational)x).den)).reduce();
		else{
			return nom.add(x.mult(den)).div(den).reduce();
		}
	}
	

	/** Multiply two algebraic objects.
	@param x    Algebraic object to be multiplied.
	@return     The product this*x.
	*/		
	public Algebraic mult(Algebraic x) throws JasymcaException{
		if(x instanceof Rational)
			return nom.mult(((Rational)x).nom).div(den.mult(((Rational)x).den)).reduce();
		else
			return nom.mult(x).div(den).reduce();
	}

	/** Divide two algebraic objects.
	@param x    The divisor algebraic object.
	@return     The quotient this/x.
	*/		
	public Algebraic div(Algebraic x) throws JasymcaException{
		if(x instanceof Rational)
			return nom.mult(((Rational)x).den).div(den.mult(((Rational)x).nom)).reduce();
		else
			return nom.div(den.mult(x)).reduce();
	}

	/** Create string representation of this rational.
	@return    A printable version of this rational.
	*/									
	public String toString(){
		return "(" + nom + "/" + den + ")";
	}
	
	/** Query: Is this algebraic object equal to x.
	@param x    The object for comparison.
	@return     True if this object equals x, false otherwise.
	*/		
	public boolean equals(Object x){
		return x instanceof Rational && ((Rational)x).nom.equals(nom) 
									 && ((Rational)x).den.equals(den);
	}
	
	/** Differentiate with respect to a variable.
	@param x    The variable.
	@return     The derivative.
	*/	
	public Algebraic deriv( Variable var ) throws JasymcaException{
		return nom.deriv(var).mult(den).sub(den.deriv(var).mult(nom)).div(den.mult(den)).reduce();
	}

	/** Integrate with respect to a variable.
	@param x    The variable.
	@return     The integral.
	*/	
	public Algebraic integrate( Variable var ) throws JasymcaException{
		if(!den.depends(var))
			return nom.integrate(var).div(den);
		// Try f'/f
		Algebraic quot = den.deriv(var).div(nom);
		if(quot.deriv(var).equals(Zahl.ZERO)){
			// J.Puettschneider sei Dank
			return FunctionVariable.create("log",den).div(quot);
//			return FunctionVariable.create("log",den).mult(quot);
		}
		Algebraic q[] = {nom, den};
		Poly.polydiv( q, var);
		if(!q[0].equals(Zahl.ZERO) && nom.ratfunc(var) && den.ratfunc(var))
			return q[0].integrate(var).add(q[1].div(den).integrate(var));
		// Constant coefficients
		if( ratfunc(var) ){
			Algebraic r = Zahl.ZERO;
			Vektor h = horowitz(nom,den,var);
			if(h.get(0) instanceof Rational)  // Square part
				r = r.add(h.get(0));
			if(h.get(1) instanceof Rational)  // Squarefree part
				r = r.add( new TrigInverseExpand().f_exakt(((Rational)h.get(1)).intrat(var) ));
			return r;
		}
		throw new JasymcaException("Could not integrate Function "+this);
	}

	/** Norm of an algebraic object. Always >= 0; 0 only if this==0.
	@return     The norm.
	*/		
	public double norm(){
		return nom.norm()/den.norm();
	}
	

	/** Conjugate complex of an algebraic object a+i*b.
	@return     The conjugate complex a-i*b.
	*/	
	public Algebraic cc() throws JasymcaException{
		return nom.cc().div(den.cc());
	}


	/** Query: Does this algebraic object depend on the variable var.
	@param var  The variable.
	@return     True if this object depends on var, false otherwise.
	*/			
	public boolean depends(Variable var){
		return nom.depends(var) || den.depends(var);
	}

	/** The value of this algebraic expression, if variable var
	assumes the value of the algebraic expression v
	@param  var   the variable to substitute.
	@param  x     the value to substitute for var.
	*/
	public Algebraic value(Variable var, Algebraic x) throws JasymcaException{
		return nom.value(var,x).div(den.value(var,x));
	}

	/** Apply an algebraic function to this algebraic object.
	@param f    The algebraic function.
	@return     The function value.
	*/			
	public Algebraic map( LambdaAlgebraic f ) throws JasymcaException{
		return f.f_exakt(nom).div(f.f_exakt(den));
	}


	/** Horowitz decomposition of p/q:
	Find c/d and a/b so that Int p/q dx = c/d + Int a/b
	with degree(a) < degree(b). Returns the vector [ c/d, a/b ]
	@param p    Nominator.
	@param q    Denominator.
	@param x    Variable.
	@return     The vektor [ c/d, a/b ].
	*/	
	public static Vektor horowitz(Algebraic p, Polynomial q, Variable x) throws JasymcaException{
		if(Poly.degree(p,x)>=Poly.degree(q,x))
			throw new JasymcaException("Degree of p must be smaller than degree of q");
		p= p.rat(); q = (Polynomial)q.rat();
		Algebraic d = Poly.poly_gcd(q, q.deriv(x));
		Algebraic b = Poly.polydiv(q,d); //   ???q.div(d,null)[0];
		int m = b instanceof Polynomial? ((Polynomial)b).degree():0;
		int n = d instanceof Polynomial? ((Polynomial)d).degree():0;
		SimpleVariable a[] = new SimpleVariable[m];
		Polynomial X = new Polynomial(x);
		Algebraic A = Zahl.ZERO;
		for(int i=a.length-1; i>=0; i--){
			a[i] = new SimpleVariable("a"+i);
			A=A.add(new Polynomial(a[i]));
			if(i>0) A = A.mult(X);
		}
			
		SimpleVariable c[] = new SimpleVariable[n];
		Algebraic C = Zahl.ZERO;
		for(int i=c.length-1; i>=0; i--){
			c[i] = new SimpleVariable("c"+i);
			C=C.add(new Polynomial(c[i]));
			if(i>0) C = C.mult(X);
		}
		Algebraic r = Poly.polydiv( C.mult(b).mult( d.deriv(x)),d);
		r = b.mult(C.deriv(x)).sub( r ).add(d.mult(A));		
		Algebraic aik[][] = new Algebraic[m+n][m+n];
		Algebraic cf, co[] = new Algebraic[m+n];
		for(int i=0; i<m+n; i++){
			co[i] 	= Poly.coefficient(p,x,i);
			cf 		= Poly.coefficient(r,x,i);
			for(int k=0; k<m; k++){
				aik[i][k] =  cf.deriv(a[k]);
			}
			for(int k=0; k<n; k++){
				aik[i][k+m]=  cf.deriv(c[k]);
			}
		}
		Vektor s = LambdaLINSOLVE.Gauss(new Matrix(aik), new Vektor(co));
		// s = [ a(0)...a(m-1) c(0 ... c(n-1) ]
		A = Zahl.ZERO;
		for(int i=m-1; i>=0; i--){
			A=A.add(s.get(i));
			if(i>0) A = A.mult(X);
		}
		C = Zahl.ZERO;
		for(int i=n-1; i>=0; i--){
			C=C.add(s.get(i+m));
			if(i>0) C = C.mult(X);
		}
	  	co = new Algebraic[2];
		co[0] = C.div(d); 
		co[1] = A.div(b);
		return new Vektor(co);
	}	


	/** Integrate this= nom/den. The nominator must have
	lower degree than the denominator, which must be squarefree.
	@param  x Variable
	@return the integral Int nom/den dx	
	*/
	Algebraic intrat(Variable x) throws JasymcaException{
		// Wir benutzen: Residue(f/g (a)) = (x-a)*(f/g)(a) = f(a)/g'(a)
		Algebraic de = den.deriv(x);
		if(de instanceof Zahl){ // trivial case
			return makelog(nom.div(de), x, den.a[0].mult(Zahl.MINUS).div(de));
		}
		Algebraic  r  = nom.div(de);
		Vektor     xi = den.monic().roots();
		Algebraic   rs = Zahl.ZERO;

		for(int i=0; i<xi.length(); i++){
			Algebraic c = r.value(x,xi.get(i));
			rs = rs.add(makelog(c,x,xi.get(i)));
		}

		return rs;
	}
	
	/** Create the function c*log(x-a) which integrate c/(x-a)
	@param a the constant a in c/(x-a)
	@param c the constant c in c/(x-a)
	@return the integral Int c/(x-a) dx = c*log(x-a)
	*/
	Algebraic makelog(Algebraic c, Variable x, Algebraic a) throws JasymcaException{
		Algebraic arg = new Polynomial(x).sub(a);
		return FunctionVariable.create("log", arg).mult(c);
	}
}	
	
