package net.sourceforge.jasymcaandroid.jasymca;
/*
   Jasymca	- Symbolic Calculator 
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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.  

*/
/** Poly - static class to  provide general functions
         - mainly used for polynomials
*/
public final class Poly{

	// Polynomial with main-est Variable
	public static Polynomial top = new Polynomial(SimpleVariable.top); 


	/** Midnight formula for the solution of the quadratic 
	equations x^2+p*x+q=0. Always returns 2 solutions.
	@param p  Algebraic coefficient.
	@param q  Algebraic coefficient.
	@return   Array of solutions.
	*/
	static Algebraic[] pqsolve(Algebraic p, Algebraic q ) throws JasymcaException{
		Algebraic r = p.mult(Zahl.MINUS).div(Zahl.TWO); // -p/2
		Algebraic s = FunctionVariable.create("sqrt", 
								r.mult(r).sub(q));
		Algebraic[] result = { r.add(s), r.sub(s) };
		return result;
	}


	/** The polynomial degree of variable v in expression 
	p is calculated. Occurances of v inside nonrational
	functions are ignored. 
	@param p  Algebraic object.
	@param v  Variable.
	@return   The polynomial degree of v in p.
	*/
	public static int degree( Algebraic p, Variable v){
		if(p instanceof Polynomial)
			return ((Polynomial)p).degree(v);
		if(p instanceof Rational){
			Rational r = (Rational)p;
			if( r.den.depends(v) )
				return 0;
			return degree(r.nom,v);
		}
		return 0;
	}

	/** The polynomial coefficient of variable v and degree n in
	expression p is calculated. Occurances of v inside nonrational
	functions are ignored. 
	@param p  Algebraic object.
	@param v  Variable.
	@param n  Integer exponent.
	@return   The coefficient of v^n in p.
	*/
	public static Algebraic coefficient(Algebraic p, Variable v, int n) throws JasymcaException{
		if(p instanceof Polynomial)
			return ((Polynomial)p).coefficient(v,n);
		if(p instanceof Rational){
			Rational r = (Rational)p;
			if(r.den.depends(v)){
				throw new JasymcaException("Cannot determine coefficient of "+v+" in "+r);
			}
			return coefficient(r.nom,v,n).div(r.den);
		}
		return n==0 ? p : Zahl.ZERO;
	}


	/** Polynomial division of one algebraic expression by
	another one with respect to the variable v. This routine calculates
	the quotient and remainder. The quotient is always a polynomial
	in v, but not necessarily in other variables.
	@param a  Two
	@param v  Variable.
	*/
	public static void polydiv( Algebraic[] a, Variable v) throws JasymcaException{ 
		int d0 = degree(a[0],v),d1= degree(a[1],v),d=d0-d1;
		if( d < 0  ){
			a[1] = a[0];
			a[0] = Zahl.ZERO;
			return;
		}
		if(d1 == 0){
			a[1] = Zahl.ZERO;
			return;
		}
		Algebraic[] cdiv = new Algebraic[d+1];
		Algebraic[] nom	 = new Algebraic[d0+1];
		for(int i=0; i<nom.length; i++)
			nom[i] = coefficient(a[0],v,i);
		Algebraic 	den	 =  coefficient(a[1], v, d1);
		for(int i=d, k=d0; i>=0; i--,k--){
			Algebraic cd = nom[k].div( den);
			cdiv[i] = cd;
			nom[k]  = Zahl.ZERO;					
			for(int j=k-1,l=d1-1; j>k-(d1+1); j--,l--)
				nom[j] = nom[j].sub( cd.mult( coefficient(a[1], v,l) ) );
		}
		a[0] = horner(v,cdiv,d+1);
		a[1] = horner(v,nom,d1);
//		System.out.println("Poly:"+a[0]+" Rat:"+a[1]);
		return;
	}
		
	/** Build polynomial expression using Horner's method.
	y=(((...(c[n-1]*x+c[n-2])*x+c[n-3])*x+....+c[0].
	@param x The variable of the polynomial.
	@param c The array of polynomial coefficients.
	@param n degree-1 of the polynomial, must be <= c.length.
	*/
	public static Algebraic horner(Variable x, Algebraic[] c, int n) throws JasymcaException{
		if(n==0) return Zahl.ZERO;
		if(n>c.length)
			throw new JasymcaException("Can not create horner polynomial.");
		Polynomial X = new Polynomial(x);
		Algebraic p = c[n-1];
		for(int i=n-2; i>=0; i--){
			p = p.mult(X).add(c[i]);
		}
		return p;
	}
	
	
	/** Build polynomial expression using Horner's method.
	y=(((...(c[n-1]*x+c[n-2])*x+c[n-3])*x+....+c[0].
	The degree of the polynomial is c.length-1.
	@param x The variable of the polynomial.
	@param c The array of polynomial coefficients.
	*/
	public static Algebraic horner(Variable x, Algebraic[] c) throws JasymcaException{
		return horner(x,c,c.length);
	}

	/** Create a copy of an array of Algebraics
	@param x  Array of algebraics
	@return   A copy of x.
	*/
	public static Algebraic[] clone(Algebraic[] x){
		Algebraic[] c = new Algebraic[x.length];
		for(int i=0; i<x.length;i++)
			c[i] = x[i];
		return c;
	}
	
	
	/** Remove leading Zero coefficients but leave coef[0]	
	@param x  Array of algebraics
	@return   reduced array.
	*/
	public static Algebraic[] reduce(Algebraic[] x){
		int len = x.length;
		while(len>0 && (x[len-1]==null || x[len-1].equals(Zahl.ZERO))){
			len--;
		}
		if(len == 0) len = 1;
		if(len!=x.length){
			Algebraic[] na= new Algebraic[len];
			for(int i=0; i<len; i++)
				na[i] = x[i];
			return na;
		}
		return x;
	}
	

	////////////////// GCD routines adapted from Davenports book /////////////////

	/** Polynomial division of one algebraic expression by
	another one. This routine calculates
	the quotient. The quotient is always a polynomial
	in all variables.
	@param p  The dividend polynomial.
	@param q  The divisor polynomial.
	@return   The quotient polynomial.
	*/
	public static Algebraic polydiv( Algebraic p1, Algebraic q1) throws JasymcaException{
		if(q1 instanceof Zahl)
			return p1.div(q1);
		if(p1.equals(Zahl.ZERO))
			return Zahl.ZERO;
		if(!(p1 instanceof Polynomial) || !(q1 instanceof Polynomial))
			throw new JasymcaException
			("Polydiv is implemented for polynomials only.Got "+p1+" / "+q1);
		Polynomial p = (Polynomial)p1;
		Polynomial q = (Polynomial)q1;
		if(p.var.equals(q.var)){
			int len = p.degree() - q.degree();
			if(len<0){
				throw new JasymcaException("Polydiv requires zero rest.");
			}
			Algebraic[] cdiv = new Algebraic[len+1];
			Algebraic[] nom	 = clone( p.a );
			Algebraic 	den	 = q.a[q.a.length-1];
			for(int i=len, k=nom.length-1; i>=0; i--,k--){
				cdiv[i] = polydiv(nom[k], den);
				nom[k]  = Zahl.ZERO;					
				for(int j=k-1,l=q.a.length-2; j>k-q.a.length; j--,l--)
						nom[j] = nom[j].sub( cdiv[i].mult(q.a[l]));
			}
			return horner(p.var,cdiv);
		}else{
			Algebraic[] cn = new Algebraic[p.a.length];
			for(int i=0; i<p.a.length; i++)
				cn[i] = polydiv(p.a[i], q1);				
			return horner(p.var,cn);
		}
	}


	/** Polynomial modulo division of one algebraic expression by
	another one. 
	@param p  The dividend polynomial.
	@param q  The divisor polynomial.
	@return   p mod q.
	*/
	public static Algebraic mod( Algebraic p, Algebraic q, Variable r) throws JasymcaException{

		int len = degree(p,r) - degree(q,r);
		if(len<0){
			return p;
		}
		Algebraic[] cdiv = new Algebraic[len+1];
		Algebraic[] nom	 = new Algebraic[degree(p,r)+1];
		for(int i=0; i<nom.length; i++)
			nom[i] = coefficient(p,r,i);
		Algebraic 	den	 =  coefficient(q,r,degree(q,r));
		for(int i=len, k=nom.length-1; i>=0; i--,k--){
			cdiv[i] = polydiv(nom[k], den);
			nom[k] = Zahl.ZERO;					
			for(int j=k-1,l=(degree(q,r)+1)-2; j>k-  (degree(q,r)+1); j--,l--)
				nom[j] = nom[j].sub( cdiv[i].mult(coefficient(q,r,l)));   
		}
		return horner(r,nom,nom.length-1 -len);
	}
	


	public static Algebraic euclid( Algebraic p, Algebraic q, Variable r) throws JasymcaException{
		// p,q are numbers or polynomials
		// Alles bezogen auf Variable r !
		int dp = degree(p,r);
		int dq = degree(q,r);
		Algebraic a = dp<dq ? p : p.mult( coefficient(q,r,dq).pow_n(dp-dq+1));
		Algebraic b = q;
		Algebraic c = mod(a, b,r);
		Algebraic result = c.equals(Zahl.ZERO) ? b : euclid(b,c,r);
		return result;
	}
//(3.1974*x^2-24.3*x+46.17)/z
	// Davenport, p133	
	public static Algebraic poly_gcd( Algebraic p, Algebraic q) throws JasymcaException{
		// poly_gcd does not work with Exponential simplifications

		if( p.equals(Zahl.ZERO ))    return q;
		if( q.equals(Zahl.ZERO )) 	 return p;
		if( p instanceof Zahl || q instanceof Zahl ) return Zahl.ONE;
		// r is the mainest of the two variables
		Variable r = ((Polynomial)q).var.smaller(((Polynomial)p).var) ?
						((Polynomial)p).var : ((Polynomial)q).var;
		Algebraic pc = content(p,r), qc = content(q,r);
		Algebraic eu = euclid( polydiv(p,pc), polydiv(q,qc), r);
		Algebraic re = polydiv(eu, content(eu,r)).mult(poly_gcd(pc,qc));
		if(re instanceof Zahl) return Zahl.ONE;
		Polynomial rp = (Polynomial)re;
		Algebraic res = rp;
		if(rp.a[rp.degree()] instanceof Zahl)
			res = rp.div(rp.a[rp.degree()]); // Normalize Polynomial
		return res;
	}
	
	// Davenport, p133	
	public static Algebraic content(Algebraic p, Variable r) throws JasymcaException{
		if( p instanceof Zahl ) return p;
		Algebraic result = coefficient(p,r,0);
		for(int i=0; i<=degree(p,r) && !result.equals(Zahl.ONE); i++)
			result = poly_gcd(result, coefficient(p,r,i));
		return result;
	}
	
	// Euklid; a>b!=0!
	static int gcd(int a, int b){
		int c = 1;
		while(c!=0){
			c = a % b;
			a = b;
			b = c;
		}
		return a;  
	}

}

