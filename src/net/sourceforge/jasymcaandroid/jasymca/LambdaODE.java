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

/* Example Session
(c8) ode(y*tan(x)+cos(x),y,x);
(d8)     (0.25*sin(2.0*x)+(0.5*x+C))/cos(x)
(c9) diff(d8,x);              
(d9)     (0.25*sin(x)*sin(2.0*x)+((0.5*x+C)*sin(x)+(0.5*cos(x)*cos(2.0*x)+0.5*cos(x))))/cos(x)^2
(c10) d8*tan(x)+cos(x);        
(d10)     (((0.25*sin(2.0*x)+(0.5*x+C))/cos(x)*tan(x))+cos(x))
(c11) d9-d10;                  
(d11)     ((0.25*sin(x)*sin(2.0*x)+((0.5*x+C)*sin(x)+(0.5*cos(x)*cos(2.0*x)+0.5*cos(x))))/cos(x)^2-(((0.25*sin(2.0*x)+(0.5*x+C))/cos(x)*tan(x))+cos(x)))
(c12) trigrat(d11);            
(d12)     0.0
*/


// ode2(a,b,c) Loest gewoehnliche Differentialgleichungen 1. und 2. Ordnung 
// a fuer b als 
// Funktion von c.
// lineare Gleichung y'=P(x)*y+Q(x)
// 

class LambdaODE extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{
		int narg 		= getNarg( st );
		Algebraic dgl 	= getAlgebraic(st);
		Variable y 		= getVariable(st);
		Variable x 		= getVariable(st);
		
		Algebraic p		= Poly.coefficient( dgl, y,1);
		Algebraic q		= Poly.coefficient( dgl, y,0);
		Algebraic pi 	= LambdaINTEGRATE.integrate(p,x);
		if(pi instanceof Rational && !pi.exaktq())
			pi = new LambdaRAT().f_exakt(pi);


		Variable  vexp  = new FunctionVariable( "exp", pi, new LambdaEXP());
		Algebraic dn    = new Polynomial(vexp);
		Algebraic qi 	= LambdaINTEGRATE.integrate(q.div(dn),x);
		if(qi instanceof Rational && !qi.exaktq())
			qi = new LambdaRAT().f_exakt(qi);
		Algebraic cn    = new Polynomial(new SimpleVariable("C"));
		
		Algebraic res   = qi.add(cn).mult(dn);
		res = new ExpandUser().f_exakt( res ); 
		p("User Function expand: "+res);
		res = new TrigExpand().f_exakt( res);
		p("Trigexpand: "+res);
		res = new NormExp().f_exakt( res);
		p("Norm: "+res);

		if( res instanceof Rational){
			res=new LambdaRAT().f_exakt(res);
		}

		res = new TrigInverseExpand().f_exakt( res);
		p("Triginverse: "+res);
		res = new SqrtExpand().f_exakt( res);
		st.push( res );
		return 0;
	}		
		
}


