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
// Sum of Exponentials a*exp(b*x)+c

public class Exponential extends Polynomial{
	public Variable 	expvar;
	public Algebraic     exp_b; 
	
	public Exponential(Algebraic a,Algebraic c, Variable x, Algebraic b){
		this.a = new Algebraic[2];
		this.a[0] = c;
		this.a[1] = a;
		Algebraic[] z  = new Algebraic[2];
		z[0] = Zahl.ZERO;
		z[1] = b;
		Object la = Lambda.pc.env.getValue("exp");
		if(!(la instanceof LambdaEXP))
			la = new LambdaEXP();
		this.var = new FunctionVariable("exp", new Polynomial(x, z),(LambdaAlgebraic)la);
		this.expvar = x;
		this.exp_b  = b;
	}
	
	// Only for casting
	public Exponential(Polynomial x){
		super(x.var, x.a);
		this.expvar = ((Polynomial)((FunctionVariable)this.var).arg).var;
		this.exp_b  = ((Polynomial)((FunctionVariable)this.var).arg).a[1];
	}
	
	// return x as exponential if it fits
	public static Algebraic poly2exp(Algebraic x){
		if(x instanceof Exponential) return x;
		if(x instanceof Polynomial && ((Polynomial)x).degree()==1 
			&& ((Polynomial)x).var instanceof FunctionVariable
			&& ((FunctionVariable)(((Polynomial)x).var)).fname.equals("exp")){
			Algebraic arg = ((FunctionVariable)(((Polynomial)x).var)).arg;
			if(arg instanceof Polynomial && ((Polynomial)arg).degree()==1 
			&& ((Polynomial)arg).a[0].equals(Zahl.ZERO)){
				return new Exponential((Polynomial)x);
			}
		}
		return x;
	}

	
	//public String toString(){ return "$"+super.toString()+"$"; }
	
	public Algebraic cc() throws JasymcaException{
		return new Exponential( a[1].cc(), a[0].cc(), expvar, exp_b.cc() );
	}

				
	// True if expression contains exponentials
	static boolean containsexp(Algebraic x) throws JasymcaException{
		if(x instanceof Zahl) return false;
		if(x instanceof Exponential) return true;
		if(x instanceof Polynomial){
			for(int i=0; i<((Polynomial)x).a.length; i++)
				if( containsexp( ((Polynomial)x).a[i] ))
					return true;
			if( ((Polynomial)x).var instanceof FunctionVariable ){
				return containsexp( ((FunctionVariable)((Polynomial)x).var).arg );
			}
			return false;
		}
		if(x instanceof Rational)
			return containsexp(((Rational)x).nom) || containsexp(((Rational)x).den);
		if(x instanceof Vektor){
			for(int i=0; i<((Vektor)x).length(); i++)
				if( containsexp( ((Vektor)x).get(i) ))
					return true;
			return false;
		}
		throw new JasymcaException("containsexp not suitable for x");
	}
						
			
	// Exponentials a*exp(b*x)+c
	public Algebraic add(Algebraic x) throws JasymcaException{
		if(x instanceof Zahl)
			return new Exponential( a[1], x.add(a[0]), expvar, exp_b );
		if(x instanceof Exponential){
			if(var.equals(((Exponential)x).var))
				return poly2exp(super.add(x));
			if(var.smaller(((Exponential)x).var))
				return x.add(this);
			return new Exponential( a[1], x.add(a[0]), expvar, exp_b );
		}
		return poly2exp(super.add(x));
	}
	
	public Algebraic mult(Algebraic x) throws JasymcaException{
		if(x.equals(Zahl.ZERO)) return x;
		if(x instanceof Zahl){			
			return new Exponential( a[1].mult(x), a[0].mult(x), expvar, exp_b );
		}
		if(x instanceof Exponential && expvar.equals(((Exponential)x).expvar) ){
			// (a*exp(bx)+c)*(d*exp(ex)+f) --> ad*exp((b+d)x) + cd*exp(ex) + af*exp(bx) +cf
			Exponential xp = (Exponential)x;
			Algebraic r = Zahl.ZERO;			
			// ad*exp((b+d)x)
			Algebraic nex = exp_b.add(xp.exp_b);
			if(nex.equals(Zahl.ZERO))
				r = a[1].mult(xp.a[1]);
			else{
				r = new Exponential( a[1].mult(xp.a[1]),Zahl.ZERO,expvar, nex); 
			}
			// c * (d*exp(ex)+f)
			r = r.add( a[0].mult(xp) );
			// f * (a*exp(bx)+c)
			r = r.add( mult(xp.a[0] ));
			r = r.reduce();
			return r;
		}
		return poly2exp(super.mult(x));
	}
	
	public Algebraic reduce() throws JasymcaException{
		if( a[1].reduce().equals(Zahl.ZERO) )
			return a[0].reduce();
		if( exp_b.equals(Zahl.ZERO) ){
			return a[0].add(a[1]).reduce();
		}
		return this;
	}	
	
	public Algebraic div(Algebraic x) throws JasymcaException{
		if(x instanceof Zahl){
			return new Exponential((Polynomial)super.div(x));
		}
		return super.div(x);
	}
	
		// Map f to coefficients and arg
	public Algebraic map( LambdaAlgebraic f ) throws JasymcaException{
		return poly2exp(super.map(f));
	}
	

	//////// Static functions to normalize exponential expressions ////	
	
	// get gcd g from expression with variables 
	// exp(x*a/g),exp(x*b/g), exp(x*c/g)	
	public static Zahl exp_gcd( Vector v, Variable x) throws JasymcaException{		
		Zahl gcd = Zahl.ZERO;
		int k=0;
		for(int i=0; i<v.size(); i++) {
			Algebraic a = (Algebraic)v.elementAt(i);
			Algebraic c ;
			if( Poly.degree(a,x)==1 && 
			  (c = Poly.coefficient(a,x,1)) instanceof Zahl ){
				k++;
				gcd = gcd.gcd( (Zahl)c );
			}
		}
		//return gcd;
		return (k>0 ? gcd : Zahl.ONE);
	}			   	   

	public static Algebraic reduce_exp( Algebraic p) throws JasymcaException{
		Algebraic[] a = { p };
		a = reduce_exp( a );
		return a[0];
	}
			   
	// get gcd g from expression with variables 
	// exp(x*a/g),exp(x*b/g), exp(x*c/g)	
	public static Algebraic[] reduce_exp( Algebraic[] p) throws JasymcaException{
		Vector v 	= new Vector();
		Vector vars = new Vector();
		GetExpVars2 g = new GetExpVars2(v);
		for(int i=0; i<p.length; i++)
			g.f_exakt(p[i]);

		for(int i=0; i<v.size(); i++) {
			Algebraic a = (Algebraic)v.elementAt(i);
			Variable x = null;
			if( a instanceof Polynomial ){
				x = ((Polynomial)a).var;
			}else
				continue;
			if( vars.contains( x ) )
				continue;
			else
				vars.addElement( x );
			Zahl gcd = exp_gcd(v, x);
			if( !gcd.equals(Zahl.ZERO)  && !gcd.equals(Zahl.ONE) ){
				SubstExp sb = new SubstExp( gcd, x );
				for(int k=0; k<p.length; k++){
					p[k] = sb.f_exakt( p[k] );
				}
			}
		}
		return p;
	}			   	   			
		
}	
////////////////// Hyperbolic Conversions ////////////////////////////

// subst exp(x*a/g),exp(x*b/g), exp(x*c/g) 
//  ---> exp(x/g)^a,exp(x/g)^b, exp(x/g)^c

class SubstExp extends LambdaAlgebraic{
	Zahl gcd;
	Variable var;
	Variable t = new SimpleVariable("t_exponential");
	
	public SubstExp(Zahl gcd, Variable var){
		this.gcd = gcd;
		this.var = var;
	}

	public SubstExp(Variable var, Algebraic expr) throws JasymcaException{ 
		this.var = var;
		Vector v 	= new Vector();
		(new GetExpVars2(v)).f_exakt( expr );
		this.gcd = Exponential.exp_gcd( v, var);
		if( gcd.equals(Zahl.ZERO) )  t = var;
	}
	
	// Subst exp(x*gcd) ---> t for integration
	// d var / d t = log(t) / gcd
	public Algebraic ratsubst( Algebraic expr ) throws JasymcaException{ 
		if( gcd.equals(Zahl.ZERO) ) return expr;
		if( !expr.depends( var ) )
			return expr;
		if( expr instanceof Rational )
			return ratsubst(((Rational)expr).nom).div(ratsubst( ((Rational)expr).den ) );
		if( expr instanceof Polynomial &&
			((Polynomial)expr).var instanceof FunctionVariable && 
			((FunctionVariable)((Polynomial)expr).var).fname.equals("exp") &&
			((FunctionVariable)((Polynomial)expr).var).arg instanceof Polynomial &&
			((Polynomial)((FunctionVariable)((Polynomial)expr).var).arg).var.equals(var) &&
			((Polynomial)((FunctionVariable)((Polynomial)expr).var).arg).degree()==1 &&
			((Polynomial)((FunctionVariable)((Polynomial)expr).var).arg).a[0].equals(Zahl.ZERO)){
			Polynomial pexpr = (Polynomial)expr;
			int degree = pexpr.degree();
			Algebraic a[] = new Algebraic[degree+1];
			for( int i=0; i<=degree; i++){
				Algebraic cf = pexpr.a[i];
				if( cf.depends( var ) )
					throw new JasymcaException("Rationalize failed: 2");
				a[i] = cf;
			}
			return new Polynomial( t, a );
		}
		throw new JasymcaException("Could not rationalize "+expr);
	}
	
	// expr( x ) dx --->  expr( t ) * dx/dt *dt
	public Algebraic rational( Algebraic expr ) throws JasymcaException{
		return ratsubst( expr ).div(gcd).div( new Polynomial(t)).reduce();
	}
	
	public Algebraic rat_reverse( Algebraic expr ) throws JasymcaException{ 
		if( gcd.equals(Zahl.ZERO) ) return expr;
		Zahl gc = gcd;// ( gcd.imagq() ? (Zahl)gcd.div( Zahl.IONE ) : gcd );
		Algebraic s = new Exponential( Zahl.ONE, Zahl.ZERO, var, 
							 Zahl.ONE.mult(gc));
		return expr.value( t, s );
	}	
	
		
	
	Algebraic f_exakt(Algebraic f) throws JasymcaException{ 
		if( gcd.equals(Zahl.ZERO) ) return f;
		if( f instanceof Polynomial ){
			Polynomial p = (Polynomial)f;
			if(p.var instanceof FunctionVariable  &&
			   ((FunctionVariable)p.var).fname.equals("exp") &&
			   Poly.degree(((FunctionVariable)p.var).arg,var) == 1){
			   Algebraic arg = ((FunctionVariable)p.var).arg;
			   Algebraic new_coef[] = new Algebraic[2];
			   new_coef[1] = gcd.unexakt();
			   new_coef[0] = Zahl.ZERO;
			   Algebraic new_arg = new Polynomial(var, new_coef );
			   
			   Algebraic subst = FunctionVariable.create("exp", new_arg);
			   
			   Algebraic exp = Poly.coefficient(arg,var,1).div(gcd);
			   if( !(exp instanceof Zahl) && !((Zahl)exp).integerq() )
			   	  throw new JasymcaException("Not integer exponent in exponential simplification.");
			   subst = subst.pow_n( ((Zahl)exp).intval() );
			   subst = subst.mult( FunctionVariable.create(
			   					"exp", Poly.coefficient(arg,var,0)));
			   
			   int n = p.a.length;
			   Algebraic r = f_exakt( p.a[n-1] );
			   for(int i=n-2; i>=0; i--){
			      r = r.mult(subst).add(f_exakt(p.a[i]));
			   }
			   return r;
			}
		}
		return f.map(this);
	}
}

			



	// Try to convert to normalized exponentials
class NormExp extends LambdaAlgebraic{

	Algebraic f_exakt(Algebraic f) throws JasymcaException{ 
		if(f instanceof Rational){
			Algebraic nom = f_exakt(((Rational)f).nom);
			Algebraic den = f_exakt(((Rational)f).den);
			if( den instanceof Zahl)
				return f_exakt(nom.div(den));
			if(den instanceof Exponential  &&  
					((Polynomial)den).a[0].equals(Zahl.ZERO) &&
					((Polynomial)den).a[1] instanceof Zahl ){
			// Convert p/(exp(x)) to p*exp(-x)
				if(nom instanceof Zahl || nom instanceof Polynomial){//Exponential){
					Exponential denx = (Exponential)den;
					Exponential den_inv = new Exponential(Zahl.ONE.div(denx.a[1]),Zahl.ZERO, 
											denx.expvar, denx.exp_b.mult(Zahl.MINUS));
					return nom.mult(den_inv);
				}
			}
			f = nom.div(den);
//			if(f instanceof Rational)
//				return norm_rat_exp((Rational)f);
			return f;
		}
		if(f instanceof Exponential)
			return f.map(this);
		if(!(f instanceof Polynomial))
			return f.map(this);
		Polynomial fp = (Polynomial)f;
		if(!(fp.var instanceof FunctionVariable) || !((FunctionVariable)fp.var).fname.equals("exp"))
			return f.map(this);
		Algebraic arg = ((FunctionVariable)fp.var).arg.reduce();
		if(arg instanceof Zahl)
			return  fp.value( FunctionVariable.create("exp",arg) ).map(this);
		if(!(arg instanceof Polynomial) || !(((Polynomial)arg).degree()==1) )
			return f.map(this);
		// Can be normalized 
		// Convert a[i]*(exp(ax+b))^i ----> (a[i]*exp(bi)) * exp(aix)
		Algebraic r = Zahl.ZERO;
		Algebraic a = ((Polynomial)arg).a[1];
		for(int i=1; i<fp.a.length; i++){
			Algebraic b = ((Polynomial)arg).a[0];
			Zahl I = new Unexakt((double)i);
			// Try to further reduce b = dy+e etc
			// exp(bi) = exp(diy)*exp(id)
			Algebraic ebi = Zahl.ONE;
			while(b instanceof Polynomial && ((Polynomial)b).degree()==1){
				Algebraic f1 = FunctionVariable.create( "exp",
									new Polynomial( ((Polynomial)b).var).mult(
										((Polynomial)b).a[1].mult(I)));
				f1 = Exponential.poly2exp(f1);
				ebi = ebi.mult(f1);
				b = ((Polynomial)b).a[0];
			}
			ebi = ebi.mult( FunctionVariable.create("exp", b.mult(I)));						
			Algebraic cf = f_exakt(fp.a[i].mult( ebi ));
			Algebraic f2 = FunctionVariable.create( "exp", 
									new Polynomial( ((Polynomial)arg).var).mult(
										a.mult(I)));
			f2 = Exponential.poly2exp(f2);
			r = r.add(cf.mult(f2));
		}
		if(fp.a.length>0)
			r = r.add(f_exakt(fp.a[0]));
		return Exponential.poly2exp(r); 
	}
}



// Collect exponentials: exp(2ax)+exp(ax) --> (exp(ax)^2+exp(ax))
class CollectExp extends LambdaAlgebraic{
	Vector v;
	public CollectExp(Algebraic f) throws JasymcaException{
		v = new Vector();
		// Get a list of variables
		new GetExpVars(v).f_exakt(f); 
	}

	Algebraic f_exakt(Algebraic x1) throws JasymcaException{ 
		if(v.size()==0) return x1;
		if( !(x1 instanceof Exponential) )
			return x1.map(this);
		Exponential e = (Exponential)x1;
		// Find largest multiple
		int exp = 1; 
		Algebraic exp_b = e.exp_b;
		if(exp_b instanceof Zahl && ((Zahl)exp_b).smaller(Zahl.ZERO)){
			exp *= -1;
			exp_b = exp_b.mult(Zahl.MINUS);
		}
		Variable x = e.expvar;
		for(int i=0; i<v.size(); i++){
			Polynomial y = (Polynomial)v.elementAt(i);
			if(y.var.equals(x)){
				Algebraic rat = exp_b.div(y.a[1]);
				if(rat instanceof Zahl && !((Zahl)rat).komplexq() ){
					int cfs = cfs( ((Zahl)rat).unexakt().real );
					if(cfs != 0 && cfs!=1){
						exp *= cfs;
						exp_b = exp_b.div(new Unexakt((double)cfs));
					}
				}
			}
		}
		Algebraic p = new Polynomial(x).mult(exp_b);
		p = FunctionVariable.create("exp",p).pow_n(exp);
		return p.mult(f_exakt(e.a[1])).add(f_exakt(e.a[0]));
		
	}
	
	//  x = a/b, return a
	// calculates cfs first approximation
	// result is non-zero only if approximation better than tol
	int cfs(double x){
		if(x<0) return cfs(-x);
		int a0 = (int)Math.floor(x);
		if(x==(double)a0)
			return a0;
		int a1 = (int)Math.floor(1./(x-a0));
		int z = a0*a1+1;
		if(Math.abs((double)z/(double)a1-x) < 1.e-6)
			return z;
		return 0;
	}
	
}

// Find all  exponentials: exp(ax), exp(bx) ...., return in Vector
class GetExpVars extends LambdaAlgebraic{
	Vector v;
	public GetExpVars(Vector v){
		this.v = v;
	}

	Algebraic f_exakt(Algebraic f) throws JasymcaException{ 
		if(f instanceof Exponential){
			Algebraic x = new Polynomial(((Exponential)f).expvar);
			x = x.mult(((Exponential)f).exp_b);
			v.addElement(x);
			f_exakt(((Exponential)f).a[1]);
			f_exakt(((Exponential)f).a[0]);
			return Zahl.ONE; // dummy
		}
		return f.map(this);
	}
}

// Find all  exponentials: exp(ax), exp(bx) ...., also in Polynomials,
// return in Vector
class GetExpVars2 extends LambdaAlgebraic{
	Vector v;
	public GetExpVars2(Vector v){
		this.v = v;
	}
	
	Algebraic f_exakt(Algebraic f) throws JasymcaException{ 
		if( f instanceof Polynomial ){
			Polynomial p = (Polynomial)f;
			if(p.var instanceof FunctionVariable  &&
			   ((FunctionVariable)p.var).fname.equals("exp")){
			   v.addElement( ((FunctionVariable)p.var).arg );
			}
			for(int i=0; i<p.a.length; i++)
				f_exakt(p.a[i]);
			return Zahl.ONE;
		}
		return f.map(this);
	}
}

// Eliminate all exponentials
class DeExp extends LambdaAlgebraic{
	Algebraic f_exakt(Algebraic f) throws JasymcaException{ 
		if(f instanceof Exponential){
			Exponential x = (Exponential)f;
			Algebraic cn[] = new Algebraic[2];
			cn[0] = f_exakt(x.a[0]);
			cn[1] = f_exakt(x.a[1]);
			return new Polynomial(x.var, cn);
		}
		return f.map(this);
	}
}
		


//////// Numeric Hyperbolic Functions ////////////////////////////////////


class LambdaEXP extends LambdaAlgebraic{
	public LambdaEXP(){ diffrule = "exp(x)"; intrule = "exp(x)"; }

	Zahl f( Zahl x){ 
		Unexakt z = x.unexakt();
		double  r = Math.exp(z.real);
		if(z.imag!=0.)
			return new Unexakt( r*Math.cos(z.imag) , r*Math.sin(z.imag) );
		return new Unexakt(r);
	}

	Algebraic f_exakt(Algebraic x) throws JasymcaException{ 
		if(x.equals(Zahl.ZERO))
			return Zahl.ONE;
		if(x instanceof Polynomial && ((Polynomial)x).degree()==1 &&
		((Polynomial)x).a[0].equals(Zahl.ZERO) ){ 
			Polynomial xp = (Polynomial)x;
			if(xp.var instanceof SimpleVariable &&
				((SimpleVariable)xp.var).name.equals(Zahl.pi)){ // exp(n*i*pi)
				Algebraic q = xp.a[1].div(Zahl.IONE);
				if(q instanceof Zahl){
					return fzexakt((Zahl)q);
				}
			}		
			if(xp.a[1] instanceof Zahl &&
				xp.var instanceof FunctionVariable &&
				((FunctionVariable)xp.var).fname.equals("log")){ 
					if( ((Zahl)xp.a[1]).integerq()) { // exp(n*log(x)) --> x^n
						int n = ((Zahl)xp.a[1]).intval();
						return ((FunctionVariable)xp.var).arg.pow_n(n);
					}
			}
		}
		return null;
	}
	
	// Calculate exp(x*i*pi)
	Algebraic fzexakt(Zahl x) throws JasymcaException{
		if(x.smaller(Zahl.ZERO)){
			Algebraic r = fzexakt((Zahl)x.mult(Zahl.MINUS));
			if(r!=null) return r.cc();
			return r;
		}
		if( x.integerq() ){
			if( x.intval() % 2 == 0 ){ // n even
				return Zahl.ONE;
			}else	// odd
				return Zahl.MINUS;
		}		
		Algebraic qs = x.add(new Unexakt(.5));
		if( ((Zahl)qs).integerq() ){
			if( ((Zahl)qs).intval() % 2 == 0 ){ // n even
				return Zahl.IMINUS;
			}else{	// odd
				return Zahl.IONE;
				}
		}
		qs = x.mult(new Unexakt(4));
		if( ((Zahl)qs).integerq() ){
			Algebraic sq2 = FunctionVariable.create("sqrt",new Unexakt(0.5));
			switch( ((Zahl)qs).intval() % 8 ){
				case 1: return Zahl.ONE.add(Zahl.IONE).div(Zahl.SQRT2);
				case 3: return Zahl.MINUS.add(Zahl.IONE).div(Zahl.SQRT2);
				case 5: return Zahl.MINUS.add(Zahl.IMINUS).div(Zahl.SQRT2);
				case 7: return Zahl.ONE.add(Zahl.IMINUS).div(Zahl.SQRT2);
			}
		}
		qs = x.mult(new Unexakt(6));
		if( ((Zahl)qs).integerq() ){
			switch( ((Zahl)qs).intval() % 12 ){
				case 1: return Zahl.SQRT3.add(Zahl.IONE).div(Zahl.TWO);
				case 2: return Zahl.ONE.add(Zahl.SQRT3.mult(Zahl.IONE)).div(Zahl.TWO);
				case 4: return Zahl.SQRT3.mult(Zahl.IONE).add(Zahl.MINUS).div(Zahl.TWO);
				case 5: return Zahl.IONE.sub(Zahl.SQRT3).div(Zahl.TWO);
				case 7: return Zahl.IMINUS.sub(Zahl.SQRT3).div(Zahl.TWO);
				case 8: return Zahl.SQRT3.mult(Zahl.IMINUS).sub(Zahl.ONE).div(Zahl.TWO);
				case 10:return Zahl.SQRT3.mult(Zahl.IMINUS).add(Zahl.ONE).div(Zahl.TWO);
				case 11:return Zahl.IMINUS.add(Zahl.SQRT3).div(Zahl.TWO);
			}
		}
		return null;
	}
}

class LambdaLOG extends LambdaAlgebraic{
	public LambdaLOG(){ diffrule = "1/x"; intrule = "x*log(x)-x"; }
	Zahl f( Zahl x){ 
		Unexakt z = x.unexakt();
		if(z.real<0 || z.imag != 0.)
			return new Unexakt( Math.log( z.real*z.real+z.imag*z.imag )/2, Math.atan2(z.imag,z.real));
		return new Unexakt(Math.log(z.real)); 
	}
	
	
	Algebraic f_exakt(Algebraic x) throws JasymcaException{ 
		if(x.equals(Zahl.ONE))
			return Zahl.ZERO;
		if(x.equals(Zahl.MINUS))
			return Zahl.PI.mult(Zahl.IONE);
		// Trigonometric conversions

		if(x instanceof Polynomial && ((Polynomial)x).degree()==1 &&
		((Polynomial)x).a[0].equals(Zahl.ZERO) &&
//		((Polynomial)x).a[1].equals(Zahl.ONE) &&
		((Polynomial)x).var instanceof FunctionVariable &&
			((FunctionVariable)((Polynomial)x).var).fname.equals("exp"))
			return ((FunctionVariable)((Polynomial)x).var).arg.add
				(FunctionVariable.create("log",((Polynomial)x).a[1]));
		return null;
	}
	
			

}



