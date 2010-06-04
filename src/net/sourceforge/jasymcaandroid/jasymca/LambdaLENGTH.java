package net.sourceforge.jasymcaandroid.jasymca;
/* Jasymca	-	- Symbolic Calculator for Mobile Devices
   This version is written for J2ME, CLDC 1.1,  MIDP 2, JSR 75
   or J2SE


   Copyright (C) 2009 - Helmut Dersch  der@hs-furtwangen.de
   
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

class LambdaLENGTH extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{
		int narg = getNarg( st );
		Algebraic x = getAlgebraic( st );
		if( x.scalarq() && !x.constantq() )
			throw new JasymcaException("Unknown variable dimension: "+x);
		Matrix m = new Matrix(x);
		st.push( new Unexakt( (double)Math.max( m.ncol(), m.nrow() )));
		return 0;
	}
}


class LambdaPROD extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{
		int narg = getNarg( st );
		Algebraic x = getAlgebraic( st );
		if( x.scalarq() && !x.constantq() )
			throw new JasymcaException("Unknown variable dimension: "+x);
		Matrix mx = new Matrix(x);
		Algebraic s = mx.col(1);
		for(int i=2; i<=mx.ncol(); i++)
			s = s.mult(mx.col(i));
		st.push( s );
		return 0;
	}
}

class LambdaSIZE extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{
		int narg = getNarg( st );
		Algebraic x = getAlgebraic( st );
		if( x.scalarq() && !x.constantq() )
			throw new JasymcaException("Unknown variable dimension: "+x);
		Matrix mx = new Matrix(x);
		Unexakt nr = new Unexakt((double)mx.nrow()),
				nc = new Unexakt((double)mx.ncol());
		if( length==2 ){
			st.push( nr );
			st.push( nc );
			length = 1;
		}else{
			st.push( new Vektor( new Algebraic[]{nr,nc} ));
		}
		return 0;
	}
}

class LambdaMIN extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{
		int narg = getNarg( st );
		Algebraic x = getAlgebraic( st );
		Matrix mx;
		if( x instanceof Vektor )
			mx = Matrix.column((Vektor) x);
		else
			mx = new Matrix(x); 
		st.push(mx.min());
		return 0;
	}
}

class LambdaMAX extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{
		int narg = getNarg( st );
		Algebraic x = getAlgebraic( st );
		Matrix mx;
		if( x instanceof Vektor )
			mx = Matrix.column((Vektor) x);
		else
			mx = new Matrix(x); 
		st.push(mx.max());
		return 0;
	}
}

class LambdaFIND extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{
		int narg = getNarg( st );
		Algebraic x = getAlgebraic( st );
		Matrix mx = new Matrix(x); 
		st.push(mx.find());
		return 0;
	}
}

