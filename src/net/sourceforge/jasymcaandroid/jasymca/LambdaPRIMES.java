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

import android.util.Log;
import java.math.BigInteger;


public class LambdaPRIMES extends Lambda{

	public int lambda(Stack st) throws ParseException,JasymcaException{ 
		int narg = getNarg( st );
		Algebraic x = getAlgebraic( st );
		if( !( x instanceof Zahl ) && !((Zahl)x).integerq() )
			throw new JasymcaException("Expected integer argument.");
		if( ((Zahl)x).smaller(Zahl.ZERO) ) {
			x = x.mult( Zahl.MINUS );
		}
		Algebraic res = null;
		if( x instanceof Unexakt ){
			long xl = (long)((Zahl)x).unexakt().real;
			res = teiler(xl);
		}else{
			BigInteger xb = ((Exakt)x).real[0];
			if( xb.compareTo( BigInteger.valueOf( Long.MAX_VALUE ) ) <= 0 ){
				long xl = xb.longValue();
				res = teiler(xl);
			}else{
				long startTime = System.currentTimeMillis();
				res = teiler(xb);
				Log.d("PRIMES", Long.toString(System.currentTimeMillis()-startTime));
			}
		}
		if( res != null )
			st.push( res );
		return 0;
	}


	static final int mod[] = { 1, 7, 11, 13, 17, 19, 23, 29 };
	static final int moddif[] = { 1, 6, 4, 2, 4, 2, 4, 6 };
	
	static long kleinsterTeiler(long X, long start){
		long stop = (long)Math.ceil(Math.sqrt((double)X));
		if(start>stop)
			return X;
		long b = start/30L;
		b *= 30L;
		long m = start%30L;
		int i = 0;
	    while(m>mod[i]) i++;

		while(start<=stop){			
			if( pc.check_interrupt())
				return -1L;		
			if(X%start == 0)
				return start;
			i++;
			if(i>=mod.length){
				i=0;
				b+=30L;
				start=b;
			}
			start += moddif[i];
		}
		return X;
	}

	static Vektor teiler( long X ) throws JasymcaException{
		Vector teiler = new Vector();
		
		while( X%2L == 0){
			teiler.addElement(Zahl.TWO);
			X/=2L;
		}
		while( X%3L == 0){
			teiler.addElement(Zahl.THREE);
			X/=3L;
		}
		while( X%5L == 0){
			teiler.addElement(new Unexakt(5.0));
			X/=5L;
		}
		long f = 7L;
		while( X != 1L){
			f = kleinsterTeiler(X, f);
			if( f<0 )
				return null;
			teiler.addElement(new Exakt(f,1L));
			X /= f;
		}
		return Vektor.create(teiler);
	}

	static BigInteger kleinsterTeiler(BigInteger X, BigInteger start){
		// if X=2^n then sqrt(x)<2^(n/2+1)
		byte[] stop_in = new byte[X.bitLength()/2/8+1+2];
		stop_in[0] = (byte)1;
		for(int i=1; i<stop_in.length; i++)
			stop_in[i] = (byte)0;
		BigInteger stop = new BigInteger(stop_in);
		if(start.compareTo( stop ) > 0)
			return X;
		BigInteger b30 = BigInteger.valueOf( 30L );
		BigInteger b = start.divide( b30 );
		b = b.multiply( b30 );
		int m = start.mod( b30 ).intValue();
		int i = 0;
	    while(m>mod[i]) i++;

		while(start.compareTo( stop ) <= 0){	
			if( pc.check_interrupt() )
				return null;	
			if(X.mod( start ).equals(BigInteger.ZERO) )
				return start;
			i++;
			if(i>=mod.length){
				i=0;
				b=b.add( b30 ); 
				start=b;
			}
			start = start.add( BigInteger.valueOf( (long)moddif[i] ));
		}
		return X;
	}

	static Vektor teiler( BigInteger X ) throws JasymcaException{
		Vector teiler = new Vector();
		BigInteger b2 = BigInteger.valueOf( 2L );	
		while( X.mod(b2).equals( BigInteger.ZERO )) {
			teiler.addElement(Zahl.TWO);
			X = X.divide( b2 );
		}
		BigInteger b3 = BigInteger.valueOf( 3L );	
		while( X.mod(b3).equals( BigInteger.ZERO )) {
			teiler.addElement(Zahl.THREE);
			X = X.divide( b3 );
		}
		BigInteger b5 = BigInteger.valueOf( 5L );	
		while( X.mod(b5).equals( BigInteger.ZERO )) {
			teiler.addElement(new Unexakt(5.0));
			X = X.divide( b5 );
		}
		BigInteger f = BigInteger.valueOf( 7L );
		while( !X.equals(BigInteger.ONE)) {
			f = kleinsterTeiler(X, f);
			if( f== null )
				return null;
			teiler.addElement(new Exakt(f));
			X = X.divide( f );
			if(X.bitLength()<62)
			{
				long Xlong = X.longValue();
				long flong = f.longValue();
				while( Xlong != 1L){
					flong = kleinsterTeiler(Xlong, flong);
					if( flong<0 )
						return null;
					teiler.addElement(new Exakt(flong,1L));
					Xlong /= flong;
				}
				break;
			}
		}
		return Vektor.create(teiler);
	}
}

/*
class LambdaISPRIME extends Lambda{

	public int lambda(Stack st) throws ParseException,JasymcaException{ 
		int narg = getNarg( st );
		Algebraic x = getAlgebraic( st );
		int certainty = 100;
		if(narg==2)
			certainty = getInteger( st );
		if( !( x instanceof Zahl ) && !((Zahl)x).integerq() )
			throw new JasymcaException("Expected integer argument.");
		if( ((Zahl)x).smaller(Zahl.ZERO) ) {
			x = x.mult( Zahl.MINUS );
		}
		BigInteger b;
		if( x instanceof Unexakt ){
			b = BigInteger.valueOf( (long)((Zahl)x).unexakt().real );
		}else{
			b = ((Exakt)x).real[0];
		}
		//public boolean isProbablePrime(int certainty)
		st.push( b.isProbablePrime(certainty) ? Zahl.ONE : Zahl.ZERO );
		return 0;
	}
}
*/
