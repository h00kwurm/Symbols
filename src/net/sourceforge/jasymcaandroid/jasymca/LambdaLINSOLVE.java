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

import java.util.*;

// A=[1 2 3;2 0 1;9 1 6]; b = [ 3  2  4 ];
public class LambdaLINSOLVE extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{
		int narg = getNarg( st );
		if(narg!=2)
			throw new ParseException("linsolve requires 2 arguments.");
		Algebraic M_in = getAlgebraic( st );
		Algebraic b_in = getAlgebraic( st );
		
		Matrix M = new Matrix( M_in );
		Matrix b = (b_in instanceof Vektor ? Matrix.column((Vektor)b_in) : new Matrix( b_in ) );
		
		Algebraic r = ((Matrix)b.transpose().div(M.transpose())).transpose().reduce();
		st.push( r );

		return 0;
	}	



/* Example session:
(c1) p1:[x1-2*x2+3*x3-x4+2*x5=2,3*x1-x2+5*x3-3*x4-x5=6,2*x1+x2+2*x3-2*x4-3*x5=9];
(d1)     [(2.0*x5+(-x4+(3.0*x3+(-2.0*x2+(x1-2.0))))), (-x5+(-3.0*x4+(5.0*x3+(-x2+(3.0*x1-6.0))))), (-3.0*x5+(-2.0*x4+(2.0*x3+(x2+(2.0*x1-9.0)))))]
(c2) linsolve(p1,[x1,x2,x3,x4,x5]);
(d2)     [(x4+(-13/7*x3+(4/7*x2+(-x1+2)))), (x5+(4/13*x4+(-7/13*x2+(-4/13*x1+8/13)))), -5]
(c3) p2:[x1-x2+x3-x4=1,x1-x2-x3+x4,x1-x2-2*x3+2*x4=-1/2];
(d3)     [(-x4+(x3+(-x2+(x1-1.0)))), (x4+(-x3+(-x2+x1))), (2.0*x4+(-2.0*x3+(-x2+(x1+0.5))))]
(c4) linsolve(p2,[x1,x2,x3,x4]);
(d4)     [(-2*x4+(2*x3+(x2-1))), (-x2+(x1-1/2)), 0]
(c5) p3:[x1+2*x2-x3+x4=1,2*x1-x2+2*x3+2*x4=2,3*x1+x2+x3+3*x4=3,x1-3*x2+3*x3+x4];
(d5)     [(x4+(-x3+(2.0*x2+(x1-1.0)))), (2.0*x4+(2.0*x3+(-x2+(2.0*x1-2.0)))), (3.0*x4+(x3+(x2+(3.0*x1-3.0)))), (x4+(3.0*x3+(-3.0*x2+x1)))]
(c6)linsolve(p3,[x1,x2,x3,x4]);                                                ;
(d6)     [(x4+(3/4*x2+(x1-9/8))), (x3+(-5/4*x2+3/8)), 1/2, -1/2]
(c7) p0:[x1-x2+2*x3=1,x1-2*x2-x3=2,3*x1-x2+5*x3=3,-2*x1+2*x2+3*x3=-4];
(d7)     [(2.0*x3+(-x2+(x1-1.0))), (-x3+(-2.0*x2+(x1-2.0))), (5.0*x3+(-x2+(3.0*x1-3.0))), (3.0*x3+(2.0*x2+(-2.0*x1+4.0)))]
(c8) linsolve(p0,[x1,x2,x3,x4]);                                      
(d8)     [(x3+2/7), (x1-10/7), (x2+1/7), 0]

(c8) linsolve(p0,[x1,x2,x3,x4]);                                      
(d8)     [(x3+2/7), (x1-10/7), (x2+1/7), 0]
(c9) p:[x1+x2=4,x1-x2=2];                                             
(d9)     [(x2+(x1-4.0)), (-x2+(x1-2.0))]
(c10) linsolve(p,[x1,x2]);
(d10)     [(x1-3), (x2-1)]
(c11) p:[x1+x2=a,x1-x2=b];
(d11)     [(x2+(x1-a)), (-x2+(x1-b))]
(c12) linsolve(p,[x1,x2]);
(d12)     [(x1+(-1/2*b-1/2*a)), (x2+(1/2*b-1/2*a))]
(c1) p:[x1+a*x2=3,x1-x2=1];
(d1)     [(a*x2+(x1-3.0)), (-x2+(x1-1.0))]
(c2) linsolve(p,[x1,x2]);  
(d2)     [((a+1)*x1+(-a-3))/(a+1), ((a+1)*x2-2)/(a+1)]

*/

	public int lambda2(Stack st) throws ParseException, JasymcaException{
		int narg = getNarg( st );
		if(narg!=2)
			throw new ParseException("linsolve requires 2 arguments.");
		Vektor expr = (Vektor)getVektor(st).rat();
		Vektor vars = getVektor(st);
		elim( expr, vars, 0 );
		subst( expr, vars, expr.length()-1 );

		st.push( expr );
		return 0;
	}	
	
	private static void subst( Vektor expr, Vektor vars, int n ) throws JasymcaException{
		if( n < 0)
			return;
		Algebraic pa = expr.get(n);
		if(pa instanceof Polynomial){
			Polynomial p = (Polynomial)pa;
			Variable v   = null;
			Algebraic c1 = null,c0;
			// Find variable in p and v
			for(int k=0; k<vars.length(); k++){
				Variable va = ((Polynomial)vars.get(k)).var;
				c1          = p.coefficient(va,1);
				if(!c1.equals(Zahl.ZERO)){
					v = va;
					break;
				}
			}
			if(v != null){
				expr.set(n, p.div(c1));
				Algebraic val = p.coefficient(v,0).mult(Zahl.MINUS).div(c1);
				for(int k=0; k<n; k++){
					Algebraic ps = expr.get(k);
					if(ps instanceof Polynomial)
						expr.set(k, ((Polynomial)ps).value(v,val));
				}
			}
		}
		subst(expr,vars,n-1);
	}
				

	
	private static void elim(Vektor expr, Vektor vars, int n) throws JasymcaException{

		if(n >= expr.length())
			return;
		// find var with largest coefficient
		double 		maxc = 0.0;
		int 		iv=0,ie=0;
		Variable 	vp=null;
		Algebraic 	f = Zahl.ONE;
		Polynomial 	pm = null;
		for(int i=0; i<vars.length(); i++){
			Variable v = ((Polynomial)vars.get(i)).var;
			for(int k=n; k<expr.length(); k++){
				Algebraic pa = expr.get(k);
				if(pa instanceof Polynomial){
					Polynomial p = (Polynomial)pa;
					Algebraic  c = p.coefficient(v, 1);
					double nm    = c.norm();
					if(nm>maxc){
						maxc = nm; vp=v; ie=k; iv=i;f=c;pm=p;
					}
				}
			}
		}

		if(maxc==0.0)
			return;
		// Move expression
		expr.set(ie, expr.get(n));
		expr.set(n, pm);

		for(int i=n+1; i<expr.length(); i++){
			Algebraic p = expr.get(i);
			if(p instanceof Polynomial){
				Algebraic fc = ((Polynomial)p).coefficient(vp,1);
				if(!fc.equals(Zahl.ZERO))
					p = p.sub(pm.mult(fc.div(f)));
			}
			expr.set(i, p);
		}
		elim(expr,vars,n+1);
	}

/*

	public Object lambda1(Object x) throws ParseException, JasymcaException{
		Object args = getArgs(car(x));
		if(length(args)!=2)
			throw new ParseException("linsolve requires 2 arguments.");
		Vektor expr = (Vektor)getVektor(args,0).rat();
		Vektor vars = getVektor(args,1);
//		if(expr.coord.length != vars.coord.length)
//			throw new ParseException("Number of vars != Number of equations");
		Algebraic an[][] = new Algebraic[expr.length()][vars.length()];
		Algebraic cn[]   = new Algebraic[expr.length()];
		for(int i=0; i<expr.length(); i++){
			Algebraic y = expr.get(i);
			for(int k=0; k<vars.length(); k++){
				if(y instanceof Polynomial){
					an[i][k] = ((Polynomial)y).coefficient(((Polynomial)vars.get(k)).var,1);
					y = y.sub(an[i][k].mult(vars.get(k)));
				}else
					an[i][k] = Zahl.ZERO;
			}
			cn[i] = y.mult(Zahl.MINUS);
		}
		Matrix a = new Matrix(an);
		Vektor c = new Vektor(cn);
		System.out.println("A="+a);
		System.out.println("c="+c);
		eliminierung(a,c);
		System.out.println("A="+a);
		System.out.println("c="+c);
		return substitution(a,c);
	}	
*/
	private static void eliminierung(Matrix a, Vektor c) throws JasymcaException{
		int n = c.length();

		for(int k=0; k<n-1; k++){
			pivot(a,c,k);
			for(int i=k+1; i<n; i++){
				Algebraic factor = a.get(i,k).div(a.get(k,k));
				for(int j=k; j<n; j++){
					a.set(i,j, a.get(i,j).sub(factor.mult(a.get(k,j))));
				}
				c.set(i, c.get(i).sub(factor.mult(c.get(k))));
			}
		}
	}

	public static Vektor substitution(Matrix a, Vektor c) throws JasymcaException{
		int n = c.length();
		Algebraic x[]=new Algebraic[n];

		x[n-1] = c.get(n-1).div(a.get(n-1,n-1));
		for(int i=n-2; i>=0; i--){
			Algebraic sum = Zahl.ZERO;
			for(int j=i+1; j<n; j++){
				sum = sum.add(a.get(i,j).mult(x[j]));
			}
			x[i] = c.get(i).sub(sum).div(a.get(i,i));
		}
		return new Vektor(x);
	}

	
		
	public static Vektor Gauss(Matrix a, Vektor c) throws JasymcaException{
		int n = c.length();
		Algebraic x[]=new Algebraic[n];
		// Vorwaertseliminierung
		for(int k=0; k<n-1; k++){
			pivot(a,c,k);
			if( !a.get(k,k).equals( Zahl.ZERO) ){
				for(int i=k+1; i<n; i++){
					Algebraic factor = a.get(i,k).div(a.get(k,k));
					for(int j=k+1; j<n; j++){
						a.set(i,j, a.get(i,j).sub(factor.mult(a.get(k,j))));
					}
					c.set(i, c.get(i).sub(factor.mult(c.get(k))));
				}
			}
		}
		// Rueckwaertssubstitution
		x[n-1] = c.get(n-1).div(a.get(n-1,n-1));
		for(int i=n-2; i>=0; i--){
			Algebraic sum = Zahl.ZERO;
			for(int j=i+1; j<n; j++){
				sum = sum.add(a.get(i,j).mult(x[j]));
			}
			x[i] = c.get(i).sub(sum).div(a.get(i,i));
		}
		return new Vektor(x);
	}
	
	private static int pivot(Matrix a, Vektor c, int k) throws JasymcaException{
		int pivot = k, n=c.length();
		double maxa = a.get(k,k).norm();
		for(int i=k+1; i<n; i++){
			double dummy = a.get(i,k).norm();
			if(dummy>maxa){
				maxa=dummy;
				pivot=i;
			}
		}
		if(pivot!=k){
			for(int j=k;j<n;j++){
				Algebraic dummy = a.get(pivot,j);
				a.set(pivot,j,a.get(k,j));
				a.set(k,j, dummy);
			}
			Algebraic dummy = c.get(pivot);
			c.set(pivot, c.get(k));
			c.set(k, dummy);
		}
		return pivot;
	}


}				
	
