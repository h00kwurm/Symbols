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

public class Unexakt extends Zahl{
	public double real, imag; 
	
	public Unexakt(){}
	
	public Unexakt(double real, double imag){
		this.real = real;
		this.imag = imag;
	}
	
	public Unexakt(double real){
		this(real,0.);
	}
			
	public double norm(){
      double r;
      if (Math.abs(real) > Math.abs(imag)) {
         r = imag/real;
         r = Math.abs(real)*Math.sqrt(1+r*r);
      } else if (imag != 0) {
         r = real/imag;
         r = Math.abs(imag)*Math.sqrt(1+r*r);
      } else {
         r = 0.0;
      }
      return r;
	}
	
	public Unexakt arg(){
		return new Unexakt(Math.atan2(imag, real));
	}
	
	public Algebraic add(Algebraic x) throws JasymcaException{
		if(x instanceof Unexakt)
			return new Unexakt(real+((Unexakt)x).real, imag+((Unexakt)x).imag);
		return x.add(this);
	}
	
	public Algebraic mult(Algebraic x) throws JasymcaException{
		if(x instanceof Unexakt)
			return new Unexakt(real*((Unexakt)x).real - imag*((Unexakt)x).imag,
							real*((Unexakt)x).imag + imag*((Unexakt)x).real);
		return x.mult(this);
	}
	
	public Algebraic div(Algebraic x) throws JasymcaException{
		if(x instanceof Unexakt){
			Unexakt a = this, b = (Unexakt)x, c = new Unexakt(0.);
			double ratio, den, abr, abi, cr;
			if( (abr = b.real) < 0.)
				abr = - abr;
			if( (abi = b.imag) < 0.)
				abi = - abi;
			if( abr <= abi ){
				if(abi == 0)
					throw new JasymcaException("Division by Zero.");
			
				ratio  = b.real / b.imag ;
				den    = b.imag * (1 + ratio*ratio);
				cr     = (a.real*ratio + a.imag) / den;
				c.imag = (a.imag*ratio - a.real) / den;
			}else{
				ratio = b.imag / b.real ;
				den = b.real * (1 + ratio*ratio);
				cr = (a.real + a.imag*ratio) / den;
				c.imag = (a.imag - a.real*ratio) / den;
			}
			c.real = cr;
			return c;
		}
		if(x instanceof Exakt){
			return new Exakt(real,imag).div(x);
		}
		return super.div(x);
	}
	
	
	public String toString(){
		if(imag==0.)
//			return ""+real;
			return Jasymca.fmt.toString( real );
		if(real==0.)
			return Jasymca.fmt.toString( imag ) +"i";
		return "("+Jasymca.fmt.toString( real )
		       +(imag>0?"+":"")+Jasymca.fmt.toString( imag )+"i)";
	}
	
	
	public boolean integerq(){
		return imag==0. && Math.round(real) == real;
	}	

	public boolean komplexq(){
		return imag!=0;
	}	

	public boolean imagq(){
		return imag!=0 && real==0;
	}	

	public Algebraic realpart() throws JasymcaException{
		return new Unexakt(real);
	}

	public Algebraic imagpart() throws JasymcaException{
		return new Unexakt(imag);
	}


	
	public boolean equals(Object x){
		if(x instanceof Unexakt)
			return ((Unexakt)x).real == real && ((Unexakt)x).imag == imag;
		if(x instanceof Exakt){
			return ((Exakt)x).tofloat().equals(this);
		}		
		return false;
	}
	
	public boolean smaller( Zahl x) throws JasymcaException{
		Unexakt xu = x.unexakt();
		if( real == xu.real )
			return imag < xu.imag;
		else
			return real < xu.real;
	}

	public int intval(){ return (int)real; }				

	
	public Zahl abs(){
		return new Unexakt(z_abs( real, imag ));	
	}

	private double z_abs(double real, double imag){
		double temp;

		if(real < 0)
			real = -real;
		if(imag < 0)
			imag = -imag;
		if(imag > real){
			temp = real;
			real = imag;
			imag = temp;
		}
		if((real+imag) == real)
			return(real);

		temp = imag/real;
		temp = real*Math.sqrt(1.0 + temp*temp);  
		return(temp);
	}

	public Algebraic map_lambda( LambdaAlgebraic lambda, Algebraic arg2 ) 
							throws ParseException,JasymcaException{
		if( arg2 == null ){
			Zahl r = lambda.f( this );
			if(r!=null)
				return r;
		}
		return super.map_lambda(lambda, arg2);
	}


		// Rationalize
	public Algebraic rat(){
		return new Exakt(real,imag);
	}
	
}
		
		
