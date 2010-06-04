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

// Find roots of univariate real polynomial
class LambdaROOTS extends LambdaALLROOTS{
}


/*
class LambdaPOLY extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{
		int narg = getNarg( st );
		Algebraic r = getAlgebraic(st);
		if( r instanceof Zahl )
			r = new Vektor((Zahl)r);
		if( r instanceof Vektor ){			
	    	Algebraic p = Poly.top.sub(((Vektor)r).get(0));
			for(int i=1; i<((Vektor)r).length(); i++){
				p = p.mult(Poly.top.sub(((Vektor)r).get(i)));
			}
			st.push( ((Polynomial)p).coeff().reverse() );
			return 0;
		}
		throw new ParseException("Wrong arguments to poly.");
	}
}
*/

/*
class LambdaPOLYVAL extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{
		int narg = getNarg( st );
		Algebraic p = getAlgebraic(st);
		p = new Vektor( p );
		Polyval pv = new Polyval( p );
		st.push( new Integer(1) );
		pv.lambda( st );
		return 0;
	}	
}

class Polyval extends LambdaAlgebraic{
	Polynomial p=null;
	Variable x;
	
	Polyval(){}
	
	Polyval( Algebraic c ) throws JasymcaException{
		x = SimpleVariable.top;
		if( c instanceof Vektor ) 
			p = new Polynomial( x, (Vektor)c );
		if(p==null)
			throw new JasymcaException("Can not build polynomial from "+c);
	}			
		
	Algebraic f_exakt(Algebraic val)throws JasymcaException{
		if(p!=null) 
				return p.value(x,val);
		return null;
	}
	
	Zahl f(Zahl x) throws JasymcaException{
		Algebraic r = f_exakt(x);
		if(r instanceof Zahl)
			return (Zahl)r;
		throw new JasymcaException("Not a constant:"+r);
	}
}
	
*/
/*
class LambdaPOLYFIT extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{
		int narg = getNarg( st );
		Vektor x = getVektor(st);
		Vektor y = getVektor(st);
		int n = getInteger(st);
		if( x.length() != y.length() )
			throw new ParseException("x and y dimensions not equal.");
		if( n < 1 || n >= x.length() )
			throw new ParseException("impossible polynomial degree.");
		// Create Matrices
		Algebraic c[][] = new Algebraic[x.length()][n+1];
		for( int i=0; i<c.length; i++ ){
			Algebraic xp = Zahl.ONE, x0 = x.get(i);
			for( int k=0; k< n+1; k++ ){
				c[i][n-k] = xp;
				xp = xp.mult( x0 );
			}
		}
		Matrix a = new Matrix( c );
		Matrix b = Matrix.column( y );
		st.push( b.adjunkt().div(a.adjunkt()).reduce());
		return 0;
	}
}
*/		
