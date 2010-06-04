package net.sourceforge.jasymcaandroid.jasymca;

/*
   Jasymca	- Symbolic Calculator 
   This version is written for J2ME, CLDC 1.1,  MIDP 2, JSR 75
   or J2SE


   Copyright (C) 2006/2009 - Helmut Dersch  der@hs-furtwangen.de
   
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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.  

*/


import java.util.*;

class LambdaFFT extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{
		int narg = getNarg( st );
		Vektor x = getVektor(st);
		x = (Vektor)new ExpandConstants().f_exakt(x);
		double[] re = ((Vektor)x.realpart()).getDouble();	
		double[] im = ((Vektor)x.imagpart()).getDouble();	
		int n = re.length;
		double power=Math.log(n)/Math.log(2.0);
		if (power!=Math.round(power)){
			double []outRe=new double[n];
    		double []outIm=new double[n];
    		dft(re,im,outRe,outIm);
    		re=outRe;
    		im=outIm;
		}else{
			ifft_1d(re,im,-1);
		}
		Unexakt[] a = new Unexakt[n];
		for(int i=0; i<n; i++)
			a[i] = new Unexakt(re[i],im[i]);
		st.push(new Vektor(a));
		return 0;
	}
	
	
	static void dft(double []re,double []im,double []outRe,double []outIm){    	
    	int N=re.length;
    	
    	for (int k=0;k<N;k++){
    		outRe[k] = outIm[k] = 0.0;
    		for (int n=0;n<N;n++){
    			double ang=-2.0*Math.PI*k*n/N;
    			double eim=Math.sin(ang);
    			double ere=Math.cos(ang);
    			outRe[k]+=re[n]*ere-im[n]*eim;
    			outIm[k]+=re[n]*eim+im[n]*ere;
			}  		
    	}
	}

	static void idft(double []re,double []im,double []outRe,double []outIm){    	
    	int N=re.length;
    	
    	for (int k=0;k<N;k++){
    		outRe[k] = outIm[k] = 0.0;
    		for (int n=0;n<N;n++){
    			double ang=2.0*Math.PI*k*n/N;
    			double eim=Math.sin(ang);
    			double ere=Math.cos(ang);
    			outRe[k]+=re[n]*ere-im[n]*eim;
    			outIm[k]+=re[n]*eim+im[n]*ere;
			}  		
   			outRe[k] /= N;
   			outIm[k] /= N;
     	}
	}



	static void ifft_1d(  double[] re,double im [],int sign){
		double  u_r,u_i, w_r,w_i, t_r,t_i;
		int     ln, nv2, k, l, le, le1, j, ip, i, n;

		n = re.length;
    	ln = (int)( Math.log( (double)n )/Math.log(2) + 0.5 );
    	nv2 = n / 2;
    	j = 1;
 		for (i = 1; i < n; i++ ){
			if (i < j){
	    		t_r = re[i - 1];
	    		t_i = im[i - 1];
	    		re[i - 1]= re[j - 1];
	    		im[i - 1] = im[j - 1];
	    		re[j - 1] = t_r;
	    		im[j - 1] = t_i;
			}
			k = nv2;
			while (k < j){
	    		j = j - k;
	    		k = k / 2;
			}
			j = j + k;
    	}

 		for (l = 1; l <= ln; l++) {	
     	 	le = (int)(Math.exp( (double)l * Math.log(2) ) + 0.5 );
	  		le1 = le / 2;
			u_r = 1.0;
			u_i = 0.0;
			w_r =  Math.cos( Math.PI / (double)le1 );
			w_i =  sign*Math.sin( Math.PI / (double)le1 );
			for (j = 1; j <= le1; j++){
	    		for (i = j; i <= n; i += le){
					ip = i + le1;
					t_r = re[ip - 1] * u_r - u_i * im[ip - 1];
					t_i = im[ip - 1] * u_r + u_i * re[ip - 1];

					re[ip - 1] = re[i - 1] - t_r;
					im[ip - 1] = im[i - 1] - t_i; 

					re[i - 1] =  re[i - 1] + t_r;
					im[i - 1] =  im[i - 1]+ t_i;  
	    		}
	    		t_r = u_r * w_r - w_i * u_i;
	    		u_i = w_r * u_i + w_i * u_r;
	    		u_r = t_r;
			} 
    	}
    	if(sign>0)
   			for(i=0; i<n; i++){
   				re[i]/=n;
   				im[i]/=n;
   			}
		return;
	}
}

class LambdaIFFT extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{
		int narg = getNarg( st );
		Vektor x = getVektor(st);
		x = (Vektor)new ExpandConstants().f_exakt(x);
		double[] re = ((Vektor)x.realpart()).getDouble();	
		double[] im = ((Vektor)x.imagpart()).getDouble();	
		int n = re.length;
		double power=Math.log(n)/Math.log(2.0);
		if (power!=Math.round(power)){
			double []outRe=new double[n];
    		double []outIm=new double[n];
    		LambdaFFT.idft(re,im,outRe,outIm);
    		re=outRe;
    		im=outIm;
		}else{
			LambdaFFT.ifft_1d(re,im,1);
		}
		Unexakt[] a = new Unexakt[n];
		for(int i=0; i<n; i++)
			a[i] = new Unexakt(re[i],im[i]);
		st.push(new Vektor(a));
		return 0;
	}
}