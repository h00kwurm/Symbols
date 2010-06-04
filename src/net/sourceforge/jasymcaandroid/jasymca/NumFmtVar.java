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

public class NumFmtVar implements NumFmt{
	double base;
	int    ibase;
	int    nsign;
	double mantisse_min, mantisse_max;

	public NumFmtVar( int ibase, int nsign ){
		this.base 	= (double)ibase;
		this.ibase 	= ibase;
		this.nsign 	= nsign;
		mantisse_min = 1.0;
		while( nsign-- > 0 )
			mantisse_min *= base;
		mantisse_max = mantisse_min * base;
	}
	
	public String toString( double x ){
		if( x<0.0 )
			return "-" + toString( -x );
		if( x==0.0 )
			return "0";
		int exp = nsign-1;
		while( x<mantisse_min) { exp--; x*=base; }
		while( x>=mantisse_min){ exp++; x/=base; }
		long xl = (long) Math.round( x );
		String r = "";
		int nc = nsign;
		while(xl != 0L){
			nc--;
			int digit = digit(xl % ibase);
			if( !( r.equals("") && digit=='0') ) 
				r = (char)digit + r;
			xl = xl / ibase;
		}
		exp -= nc;
		if(exp>nsign-1 || exp<-1){
			if(r.length()==1)
				r = r+"0";
			return sub(r,0,1)+"."+sub(r,1,r.length())+"E"+exp;
		}
		if(exp==-1)
			return "0."+r;
		else
			return sub(r,0,exp+1)+
			(r.length()>exp+1?"."+sub(r,exp+1,r.length()):"");
	}
			

	private int digit( long x ){
		if( x < 10 )
			return '0'+(int)x;
		return 'A'+(int)x-10;
	}
	
	String sub( String s, int a, int b ){
		if( s.length() >= b )
			return s.substring(a,b);
		String r = "";
		while(a < b){
			if( a < s.length() )
				r += s.charAt(a);
			else
				r += "0";
			a++;
		}
		return r;
	}

/*	
	public String formatDouble( double x, int base, int nsign ){
		if(x<0.0)  return "-"+formatDouble(-x,base,nsign);
		if(x==0.0) return "0";
		int exp = (int) Math.floor( Math.log(x) / Math.log((double)base));
		long xl = (long) Math.round( x / Math.pow( base, exp-(nsign-1) ));
		String r = "";
		int nc = nsign;
		while(xl != 0L){
			nc--;
			int digit = digit(xl % base);
			if( !( r.equals("") && digit=='0') ) 
				r = (char)digit + r;
			xl = xl / base;
		}
		exp -= nc;
		if(exp>nsign-1 || exp<-1){
			if(r.length()==1)
				r = r+"0";
			return sub(r,0,1)+"."+sub(r,1,r.length())+"E"+exp;
		}
		if(exp==-1)
			return "0."+r;
		else
			return sub(r,0,exp+1)+
			(r.length()>exp+1?"."+sub(r,exp+1,r.length()):"");
	}
*/
}	

	

