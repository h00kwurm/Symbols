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

class LambdaINTEGRATE extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{
		int narg = getNarg( st );
		if(narg==0)
			throw new ParseException("Argument to integrate missing.");
		Algebraic f = getAlgebraic(st); // Function to integrate
		Variable  v; 
		if(narg>1){
			v = getVariable(st);
		}else{
			if( f instanceof Polynomial )
				v = ((Polynomial)f).var;
			else if(f instanceof Rational )
				v = ((Rational)f).den.var;
			else
				throw new ParseException("Could not determine Variable.");
		}
		Algebraic fi = integrate(f,v);
		if(fi instanceof Rational && !fi.exaktq())
			fi = new LambdaRAT().f_exakt(fi);
		st.push( fi );
		return 0;		
	}	
	
	public static Algebraic integrate(Algebraic expr, Variable v)throws JasymcaException{
		Algebraic e = new ExpandUser().f_exakt(expr);
		try{
			e = e.integrate(v);
			e = new TrigInverseExpand().f_exakt(e);
			e = remove_constant( e, v );
			return e;//new TrigInverseExpand().f_exakt(e.integrate(v));
		}catch (JasymcaException j){
		}
		// Second attempt: Use trigonometric/exponential Normalization
		p("Second Attempt: "+expr);
		expr = new ExpandUser().f_exakt(expr);
		p("Expand User Functions: "+expr);
		expr = new TrigExpand().f_exakt( expr );
		e = expr;
		try{
			expr = new NormExp().f_exakt(expr);		
			p("Norm Functions: "+expr);
			expr = expr.integrate(v);
			p("Integrated: "+expr);
			expr = remove_constant( expr, v );
			expr = new TrigInverseExpand().f_exakt(expr);
			return expr;
		}catch(JasymcaException j){
		}
		// Expression has been normalized in last steps
		p("Third Attempt: "+expr);
		expr = e;
		SubstExp se = new SubstExp( v, expr );		
		expr = se.f_exakt( expr );
		expr = se.rational( expr );
		p("Rationalized: "+expr);
		expr = expr.integrate( se.t );
		p("Integrated: "+expr);
		expr = se.rat_reverse(expr);
		p("Reverse subst.: "+expr);		
		expr = remove_constant( expr, v );
		expr = new TrigInverseExpand().f_exakt(expr);
		expr = remove_constant( expr, v );
		return expr;
	}
	
	static Algebraic remove_constant( Algebraic expr, Variable x )throws JasymcaException{
		if( !expr.depends(x) )
			return Zahl.ZERO;
		if( expr instanceof Polynomial ){
			((Polynomial)expr).a[0] = remove_constant(
				((Polynomial)expr).a[0], x);
			return expr;
		}
		if( expr instanceof Rational ){
			Polynomial den = ((Rational)expr).den;
			Algebraic nom  = ((Rational)expr).nom;
			if( !den.depends(x) ) {
				return remove_constant( nom,x ).div(den) ;
			}
			if(nom instanceof Polynomial){
				Algebraic a[] = new Algebraic[]{ nom, den };
				Poly.polydiv(a, den.var);
				if(!a[0].depends(x) ) {
					return a[1].div( den );
				}
			}
		}
		return expr;
	}
		
		
}


class LambdaROMBERG extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{
		int narg = getNarg( st );
		if(narg!=4)
			throw new ParseException("Usage: ROMBERG (exp,var,ll,ul)");
		Algebraic exp = getAlgebraic(st);
		Variable  v   = getVariable(st);
		Algebraic ll  = getAlgebraic(st);
		Algebraic ul  = getAlgebraic(st);
		// Expand constants like pi
		LambdaAlgebraic xc = new ExpandConstants(); // evaluate pi etc.
		exp = xc.f_exakt(exp);
		ll  = xc.f_exakt(ll);
		ul  = xc.f_exakt(ul);

		// Check arguments
		if( !(ll instanceof Zahl) || !(ul instanceof Zahl) )
			throw new ParseException("Usage: ROMBERG (exp,var,ll,ul)");
		
		double rombergtol = 1.0e-4;
		int    rombergit  = 11;
		Zahl a1 = pc.env.getnum("rombergit");
		if(a1 !=null){
			rombergit = a1.intval();
		}
		a1 = pc.env.getnum("rombergtol");
		if(a1 !=null){
			rombergtol = a1.unexakt().real;
		}
		double a = ((Zahl)ll).unexakt().real;
		double b = ((Zahl)ul).unexakt().real;
		double I[][] = new double[rombergit][rombergit];
		int i=0,n=1;
		Algebraic t = trapez( exp, v, n, a, b);
		if(!(t instanceof Zahl))
			throw new ParseException("Expression must evaluate to number");
		I[0][0] = ((Zahl)t).unexakt().real;
		double epsa = 1.1*rombergtol;
		while(epsa>rombergtol && i<rombergit-1){
			i++;
			n *= 2;
			t = trapez( exp, v, n, a, b);
			I[0][i] = ((Zahl)t).unexakt().real;
			double f = 1.;
			for(int k= 1; k<=i; k++){
				f *= 4; // 4^k
				I[k][i] = I[k-1][i]+(I[k-1][i]-I[k-1][i-1])/(f-1.);
			}
/*
			for(int k= 0; k<=i; k++){
				System.out.print(I[k][i]+" ");
			}
			p("");
*/			

			epsa = Math.abs(( I[i][i]- I[i-1][i-1]) /I[i][i]);
		}
		st.push( new Unexakt(I[i][i]) );
		return 0;
	}
	
	Algebraic trapez( Algebraic exp, Variable v, int n, double a, double b) throws JasymcaException{
		Algebraic sum = Zahl.ZERO;
		double step = (b-a)/n;
		for(int i=1; i<n; i++){
			Algebraic x = exp.value(v, new Unexakt(a+step*i));
			sum = sum.add(x);
		}
		sum = exp.value(v, new Unexakt(a)).add(sum.mult(Zahl.TWO)).add(exp.value(v, new Unexakt(b)));
		return new Unexakt(b-a).mult(sum).div(new Unexakt(2.*n));
	}
}

			
