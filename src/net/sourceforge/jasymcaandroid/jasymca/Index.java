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
	

class Index{
	int row[];  
	int col[];
	int row_max, col_max;
	
	public Index(int row[], int col[]){
		this.row = row;
		this.col = col;
		row_max = maxint(row);
		col_max = maxint(col);
	}

	//
	public Index(int row, int col, Algebraic x){
		int width = 1, height = 1;
		if(x instanceof Vektor)
			width = ((Vektor)x).length();
		else if(x instanceof Matrix){
			width  = ((Matrix)x).nrow();
			height = ((Matrix)x).ncol();
		}
		this.row = series(row,row+height-1);
		this.col = series(col,col+width-1);
		row_max = maxint(this.row);
		col_max = maxint(this.col);
	}

	private int maxint(int[] c){
		int max = c[0];
		for(int i=1; i<c.length; i++)
			if(c[i] > max) max=c[i];
		return max;
	}
		

	
	public String toString(){
		String s = "Index = \nRows: ";
		for(int i=0; i<row.length; i++){
			s += "  "+row[i];
		}
		s += "\nColumns: ";
		for(int k=0; k<col.length; k++){
			s += "  "+col[k];
		}
		return s;
	}

	public static Index createIndex(Algebraic idx_in, Matrix x ) throws JasymcaException{
		if( !idx_in.constantq() )
			throw new JasymcaException("Index not constant: "+idx_in);
		idx_in = idx_in.reduce();
		if(idx_in instanceof Zahl && ((Zahl)idx_in).integerq()){
			int row[] = new int[]{1};
			int col[] = new int[]{((Zahl)idx_in).intval()};
			return new Index(row,col);
		}else if( idx_in instanceof Vektor && ((Vektor)idx_in).length()==2 ){
			Algebraic r = ((Vektor)idx_in).get(0);
			Algebraic c = ((Vektor)idx_in).get(1);
			if( r instanceof Zahl && ((Zahl)r).integerq() &&
			    c instanceof Zahl && ((Zahl)c).integerq() ){
				int row[] = new int[]{((Zahl)r).intval()};
				int col[] = new int[]{((Zahl)c).intval()};
				return new Index(row,col);
			}
		}
		throw new JasymcaException("Not a legel index: "+idx_in);
	}
			    
	
	public static Index createIndex( Stack st, Matrix x ) throws ParseException, JasymcaException{
		int row[], col[];
		int length = Lambda.getNarg( st );
		Object rdx, cdx;
		if( length > 1 ){
			rdx = st.pop();
			if( ":".equals(rdx) )
				row = series( 1, x.nrow() );
			else
				row = setseries( (Algebraic)rdx );
			cdx = st.pop();
		}else{
			cdx = st.pop();
			row = new int[1];
			row[0] = 1;
		}

		if( ":".equals(cdx) )
			col = series( 1, x.ncol() );
		else
			col = setseries(  (Algebraic)cdx );
		return new Index(row,col);
	}

	static int[] setseries(Algebraic c) throws ParseException, JasymcaException{
		int[] s;
		if( c instanceof Zahl && ((Zahl)c).integerq() ){
			s = new int[1];
			s[0] = ((Zahl)c).intval();
		}else if( c instanceof Vektor ){
			s = new int[ ((Vektor)c).length() ];
			for( int i=0; i<s.length; i++){
				Algebraic a = ((Vektor)c).get(i);
				if( a instanceof Zahl && ((Zahl)a).integerq() ){
					s[i] = ((Zahl)a).intval();
				}else{
					throw new ParseException("Not a legal index: "+a);
				}
			}
		}else
			throw new ParseException("Not a legal index: "+c);
		return s;
	}
					
	static int[] series(int a, int b){
		int[] c = new int[b+1-a];
		for(int i=0; i<c.length; i++)
			c[i] = a+i;
		return c;
	}

}

class REFX extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int narg = getNarg( st ); 
		Algebraic x = getAlgebraic( st );
		Algebraic index_in = CreateVector.crv( st );
		if( index_in.constantq() ){
			Matrix mx = new Matrix((Algebraic)x); 
			Index  idx 	= Index.createIndex( index_in, mx );
			mx = mx.extract( idx );
			x = mx.reduce();
		}else{
			MatRef mr = new MatRef( (Algebraic)x );
			x = new Polynomial(new FunctionVariable("MR("+x+")",index_in,mr));
		}
		st.push( x );
		return 0;
	}
}
	
class REFM extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int narg = getNarg( st ); 
		Algebraic x = getAlgebraic( st );
		Matrix mx = new Matrix((Algebraic)x); 
		Index  idx 	= Index.createIndex( st, mx );
		mx = mx.extract( idx );
		st.push( mx.reduce() );
		return 0;
	}
}
	

class MatRef extends LambdaAlgebraic{
	Matrix 	mx;
	
	public MatRef( Algebraic x ){
		this.mx = new Matrix(x);
	}
	
	Algebraic f_exakt(Algebraic x) throws JasymcaException{
		if( x.constantq() ){
			Index  idx 	= Index.createIndex( x, mx );
			Matrix m = mx.extract( idx );
			return m.reduce();
		}
		return null;
	}
}
