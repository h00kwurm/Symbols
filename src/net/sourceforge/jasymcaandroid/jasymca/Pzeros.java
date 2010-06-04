package net.sourceforge.jasymcaandroid.jasymca;
/* pzeros - POLYNOMIAL ZEROS

   derived from pzeros.f
   <http://netlib.org/numeralgo/na10>
   see original copyright notice below.

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


/*
   The Fortran sources have been translated to Java via f2c 
   and slightly revised. A few utility routines (complex 
   arithmetics etc) have been added, so that this module can 
   be used standalone without further package requirements 
   except java.lang. It has also been successfully tested on 
   j2me.
   
   
   Helmut Dersch
   Jan 2009
*/  



public class Pzeros{

/**
*  Example main function to test and demonstrate pzeros.
* Calculates the roots of a 3rd order polynomial
* a[0]+a[1]*x+a[2]*x^2+a[3]*x^3.
*/

	public static void main(String[] args){
		double[] ar   = { 0.0,  1.0,  1.0,  1.0 };	// realpart(a)
		double[] ai   = { 0.0,  0.0,  1.0,  0.0 };  // imagpart(a)
		boolean[] err = {true, true, true, true };  // error
		
		Pzeros.aberth( ar, ai, err );				// roots are
													// returned in a
													// errors should be
													// all false
	
		for(int i=0; i< ar.length-1; i++){
			System.out.println(i+": "+ar[i]
			           +"+i*"+ai[i] +"  "+err[i]);
		}
	}



	
/**
*	Calculates the roots of polynomial functions
*   y=a[0]+a[1]*x+a[2]*x^2+a[3]*x^3+...+a[n]*x^n
*   of any degree n having arbitrary complex coefficients a[i].
*   The method is derived from  
*   <a href=http://netlib.org/numeralgo/na10>pzeros.f</a>.
*   <p>
*   The real and imaginary parts of the coefficients are 
*   passed in two double arrays. The roots are returned
*   in the same arrays. There are (n+1) coefficients and
*   n roots. The coefficient a[n] must not be zero. Errors 
*   are returned in a boolean array separately for each root.
*   Success is indicated by false, i.e. no error. On return
*   each root should be checked against its flag.
*	<p>
*	All three arrays must be equal sized. This is a conveniance
*   method which sets most accessible parameters to reasonable
*   defaults. It also transparently deflates the polynomial
*   in case the constant coefficient is zero. See the sources
*	for a description of the method.
*
*   @param ar  realpart of polynomial coefficients a.
*   @param ai  imaginary part of polynomial coefficients a.
*   @param err  error flag for each root.
*/
	public static void aberth( double[]  ar,   		// realpart(a)
						double[]  ai,				// imagpart(a)
						boolean[] err ){			// error
		for(int i=0; i< err.length; i++){	
			err[i] = true;
		}
		if(ar.length != ai.length || ar.length != err.length)
			return;
		// check for zero constant coefficients
		// and deflate polynomial
		int n_zero = 0;
		while( n_zero < ar.length &&
		   ar[n_zero] == 0. && ai[n_zero] == 0.) n_zero++;
		if( n_zero == ar.length ) return;
		
		doublecomplex[] poly= new doublecomplex[ar.length-n_zero];
		for(int i=0; i< poly.length; i++){		
			poly[i] = Pzeros.pz.dc(ar[i+n_zero],ai[i+n_zero]);
		}
		int n 			= poly.length-1;   			// polynomial degree
		double eps 		= 2.22044604925031e-16;
		double big 		= Double.MAX_VALUE;
		double theSmall = Double.MIN_VALUE;
		int nitmax 		= 100;			   			// maximum number of iterations
		doublecomplex[] root = new doublecomplex[n];// results
		double[] radius = new double[n];			// accuracy of results
		boolean[] errs  = new boolean[n+1];			// error flag for each root
													// bug: n should suffice
		for(int i=0; i< n; i++){					// initialize vars
			root[i] 	= Pzeros.pz.dc();
			radius[i]	= 1.0;
			errs[i] 	= true;
		}
		int[] iter 	= new int[1]; iter[0] = 0;		// Actual number of iterations
		double[] apoly  = new double[n+1];			// Workspace
		double[] apolyr = new double[n+1];			// Workspace
		for(int i=0; i< n+1; i++){
			apoly[i] = 1.0;
			apolyr[i]= 1.0;
		}
		Pzeros.pz.polzeros_(n, poly, eps, big, theSmall, nitmax, root, 
			  		radius, errs, iter, apoly, apolyr);

		for( int i=0; i<n_zero; i++){
			ar[i] = 0.0;
			ai[i] = 0.0;
			err[i]= false;
		}
		for( int i=n_zero; i<ar.length-1; i++){
			ar[i] = root[i-n_zero].r;
			ai[i] = root[i-n_zero].i;
			err[i]= errs[i-n_zero];
		}
		return;
	}		


/**
*	Calculates the roots of polynomial functions
*   y=a[0]+a[1]*x+a[2]*x^2+a[3]*x^3+...+a[n]*x^n
*   of any degree n having arbitrary real coefficients a[i].
*   Based on sources by C.Bondi,
*   http://www.crbond.com/download/misc/bairstow.c
*   <p>
*   The coefficients are 
*   passed in a double array. The real parts of the roots are 
*   returned
*   in the same array, together with the imaginary parts in a separate
*   double array. There are (n+1) coefficients and
*   n roots. The coefficient a[n] must not be zero, and the
*   polynomial should be squarefree, i.e. contain no multiple
*   roots. Errors are returned in a boolean array separately 
*   for each root. Success is indicated by false, i.e. no error. 
*   On return each root should be checked against its flag.
*	All three arrays must be equal sized. 
*   <p> This method is less stable than Aberth's method and
*   only works with polynomials having real coefficients. Its
*   advantage is that complex conjugate roots are guaranteed
*   to have equal realparts and opposite imaginary parts.
*
*   @param ar  realpart of polynomial coefficients a. This array holds the
*			   realparts of the roots upon return.
*   @param ai  content of this array is ignored upon entry. On exit it 
*              contains the imaginary parts of the roots.
*   @param err error flag for each root.
*/

public static void bairstow( double[]  ar,   		// realpart(a)
						double[]  ai,				// imagpart(a), ignored
						boolean[] err ){			// error
		for(int i=0; i< err.length; i++){	
			err[i] = true;
		}
		if(ar.length != ai.length || ar.length != err.length)
			return;

		double[] a = new double[ar.length];
		for(int i=0; i<ar.length; i++){
			// Reverse order and normalize
			a[i] = ar[ar.length-i-1] / ar[ar.length-1];
		}

		int n = ar.length-1;
		double b[] =new double[n+1], c[] =new double[n+1];
		b[0]=c[0]=1.0;
		
		while (n > 2) {
			double r,s,dn,dr,ds,drn,dsn,eps;
			int i,iter;
			r = s = 0;
			dr = 1.0;
			ds = 0;
			eps = 1e-14;
			iter = 1;
			boolean precision_error_flag = false;

			while ((Math.abs(dr)+Math.abs(ds)) > eps) {
				if ((iter % 200) == 0) {
					r=Math.random() * 1000;
				}
				if ((iter % 500) == 0) {
					eps*=10.0;
					precision_error_flag = true;
				}
				b[1] = a[1] - r;
				c[1] = b[1] - r;

				for (i=2;i<=n;i++){
					b[i] = a[i] - r * b[i-1] - s * b[i-2];
					c[i] = b[i] - r * c[i-1] - s * c[i-2];
				}
				dn=c[n-1]  * c[n-3] - c[n-2] * c[n-2];
				drn=b[n]   * c[n-3] - b[n-1] * c[n-2];
				dsn=b[n-1] * c[n-1] - b[n]   * c[n-2];

				if (Math.abs(dn) < 1e-16) {
					dn = 1;
					drn = 1;
					dsn = 1;
				}
				dr = drn / dn;
				ds = dsn / dn;

				r += dr;
				s += ds;
				iter++;
			}
			for (i=0;i<n-1;i++) 
				a[i] = b[i];
			a[n] 	 = s;
			a[n-1] 	 = r;
			err[n-1] = precision_error_flag;
			err[n-2] = precision_error_flag;
			n-=2;
		}

		double real[] = new double[2], imag[] = new double[2];
		for (int i=a.length-1;i>=2;i-=2) {			// quadratics 
			pqsolve(a[i-1],a[i],real,imag);
			ar[i-1] = real[0]; ai[i-1] = imag[0]; 
			ar[i-2] = real[1]; ai[i-2] = imag[1]; 
		}
		if ((n % 2) == 1){
			ar[0] = -a[1]; ai[0] = 0.; err[0] = false;
		}else{
			err[0] = err[1] = false;
		}
		return;
	}


	// solve x^2+px+q=0
	static void pqsolve(double p, double q, double r[], double i[]){
		p = -p/2.;
		q = p*p-q;
		if(q>=0){
			q = Math.sqrt(q);
			r[0] = p + q; i[0] = 0.0;
			r[1] = p - q; i[1] = 0.0;
		}else{
			q = Math.sqrt(-q);
			r[0] = p; i[0] = q;
			r[1] = p; i[1] =-q;
		}
	}

						
		

	static Pzeros pz = new Pzeros();

	// utility routines
	void PrintError(String s){
		// uncomment for debugging messages
		//System.out.println(s);
	}

	class doublecomplex{	
		double r,i;  
		public doublecomplex(double r, double i){
			this.r = r;
			this.i = i;
		}
		public String toString(){
			return r + " + i*" + i;
		}
	}

	// f2c stuff

	double z_abs(doublecomplex z){
		double temp,real,imag;

		real = z.r;
		imag = z.i;

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
		temp = real*Math.sqrt(1.0 + temp*temp);  /*overflow!!*/
		return(temp);
	}

	int pow_ii(int ap, int bp) {
		int pow, x, n;
		long u;

		x = ap;
		n = bp;

		if (n <= 0) {
			if (n == 0 || x == 1)
				return 1;
			if (x != -1)
				return x == 0 ? 1/x : 0;
			n = -n;
		}
		u = n;
		for(pow = 1; ; ){
			if((u & 01)!=0)
				pow *= x;
			if((u >>= 1)!=0)
				x *= x;
			else
				break;
		}
		return(pow);
	}


	void z_div(doublecomplex c, doublecomplex a, doublecomplex b){
		double ratio, den;
		double abr, abi, cr;

		if( (abr = b.r) < 0.)
			abr = - abr;
		if( (abi = b.i) < 0.)
			abi = - abi;
		if( abr <= abi ){
			if(abi == 0){
				PrintError("complex division by zero");
				c.r = 1.0;
				c.i = 1.0;
				return;
			}
			ratio = b.r / b.i ;
			den = b.i * (1 + ratio*ratio);
			cr = (a.r*ratio + a.i) / den;
			c.i = (a.i*ratio - a.r) / den;
		}else{
			ratio = b.i / b.r ;
			den = b.r * (1 + ratio*ratio);
			cr = (a.r + a.i*ratio) / den;
			c.i = (a.i - a.r*ratio) / den;
		}
		c.r = cr;
	}


	doublecomplex dc(){ return new doublecomplex(0.,0.); }
	doublecomplex dc(double r, double i){ return new doublecomplex(r,i); }


/* Following are the translated fortran sources */

/****************************************************************************
  * All the software  contained in this library  is protected by copyright. *
  * Permission  to use, copy, modify, and  distribute this software for any *
  * purpose without fee is hereby granted, provided that this entire notice *
  * is included  in all copies  of any software which is or includes a copy *
  * or modification  of this software  and in all copies  of the supporting *
  * documentation for such software.                                        *
  ***************************************************************************
  * THIS SOFTWARE IS BEING PROVIDED "AS IS", WITHOUT ANY EXPRESS OR IMPLIED *
  * WARRANTY. IN NO EVENT, NEITHER  THE AUTHORS, NOR THE PUBLISHER, NOR ANY *
  * MEMBER  OF THE EDITORIAL BOARD OF  THE JOURNAL  "NUMERICAL ALGORITHMS", *
  * NOR ITS EDITOR-IN-CHIEF, BE  LIABLE FOR ANY ERROR  IN THE SOFTWARE, ANY *
  * MISUSE  OF IT  OR ANY DAMAGE ARISING OUT OF ITS USE. THE ENTIRE RISK OF *
  * USING THE SOFTWARE LIES WITH THE PARTY DOING SO.                        *
  ***************************************************************************
  * ANY USE  OF THE SOFTWARE  CONSTITUTES  ACCEPTANCE  OF THE TERMS  OF THE *
  * ABOVE STATEMENT.                                                        *
  ***************************************************************************

   AUTHOR:

       DARIO ANDREA BINI
       UNIVERSITY OF PISA, ITALY
       E-MAIL: bini@dm.unipi.it

   REFERENCE:

    -  NUMERICAL COMPUTATION OF POLYNOMIAL ZEROS BY MEANS OF 
       ABERTH'S METHOD
       NUMERICAL ALGORITHMS, 13 (1996), PP. 179-200   

   SOFTWARE REVISION DATE:

       JUNE, 1996

   SOFTWARE LANGUAGE:

       FORTRAN

  ***************************************************************************/


/* Table of constant values */

static int c__9 = 9;
static int c__1 = 1;
doublecomplex c_b35 = new doublecomplex(1.,0.);
static int c__3 = 3;
static int c__2 = 2;

/**************************************************************************/
/*    NUMERICAL COMPUTATION OF THE ROOTS OF A POLYNOMIAL HAVING          * */
/*        COMPLEX COEFFICIENTS, BASED ON ABERTH'S METHOD.                * */
/*                      Version 1.4, June   1996                         * */
/*    (D. Bini, Dipartimento di Matematica, Universita' di Pisa)         * */
/*                         (bini@dm.unipi.it)                            * */
/**************************************************************************/
/* Work performed under the support of the ESPRIT BRA project 6846 POSSO * */
/**************************************************************************/
/***********         SUBROUTINES AND FUNCTIONS                 ************/
/**************************************************************************/
/*  The following modules are listed:                                    * */
/*  POLZEROS  :  computes polynomial roots by means of Aberth's method   * */
/*    ABERTH  :  computes the Aberth correction                          * */
/*    NEWTON  :  computes p(x)/p'(x) by means of Ruffini-Horner's rule   * */
/*    START   :  Selects N starting points by means of Rouche's theorem  * */
/*    CNVEX   :  Computes the convex hull, used by START                 * */
/*    CMERGE  :  Used by CNVEX                                           * */
/*    LEFT    :  Used by CMERGE                                          * */
/*    RIGHT   :  Used by CMERGE                                          * */
/*    CTEST   :  Convexity test, Used by CMERGE                          * */
/**************************************************************************/
/*                                                                       * */
/*                                                                       * */
/**************************************************************************/
/*********************** SUBROUTINE POLZEROS ******************************/
/**************************************************************************/
/*                        GENERAL COMMENTS                               * */
/**************************************************************************/
/*  This routine approximates the roots of   the  polynomial             * */
/*  p(x)=a(n+1)x^n+a(n)x^(n-1)+...+a(1), a(j)=cr(j)+I ci(j), I**2=-1,    * */
/*  where a(1) and a(n+1) are nonzero.                                   * */
/*  The coefficients are complex*16 numbers. The routine is fast, robust * */
/*  against overflow, and allows to deal with polynomials of any degree. * */
/*  Overflow situations are very unlikely and may occurr if there exist  * */
/*  simultaneously coefficients of moduli close to BIG and close to      * */
/*  SMALL, i.e., the greatest and the smallest positive real*8 numbers,  * */
/*  respectively. In this limit situation the program outputs a warning  * */
/*  message. The computation can be speeded up by performing some side   * */
/*  computations in single precision, thus slightly reducing the         * */
/*  robustness of the program (see the comments in the routine ABERTH).  * */
/*  Besides a set of approximations to the roots, the program delivers a * */
/*  set of a-posteriori error bounds which are guaranteed in the most    * */
/*  part of cases. In the situation where underflow does not allow to    * */
/*  compute a guaranteed bound, the program outputs a warning message    * */
/*  and sets the bound to 0. In the situation where the root cannot be   * */
/*  represented as a complex*16 number the error bound is set to -1.     * */
/**************************************************************************/
/*  The computation is performed by means of Aberth's method             * */
/*  according to the formula                                             * */
/*           x(i)=x(i)-newt/(1-newt*abcorr), i=1,...,n             (1)   * */
/*  where newt=p(x(i))/p'(x(i)) is the Newton correction and abcorr=     * */
/*  =1/(x(i)-x(1))+...+1/(x(i)-x(i-1))+1/(x(i)-x(i+1))+...+1/(x(i)-x(n)) * */
/*  is the Aberth correction to the Newton method.                       * */
/**************************************************************************/
/*  The value of the Newton correction is computed by means of the       * */
/*  synthetic division algorithm (Ruffini-Horner's rule) if |x|<=1,      * */
/*  otherwise the following more robust (with respect to overflow)       * */
/*  formula is applied:                                                  * */
/*                    newt=1/(n*y-y**2 R'(y)/R(y))                 (2)   * */
/*  where                                                                * */
/*                    y=1/x                                              * */
/*                    R(y)=a(1)*y**n+...+a(n)*y+a(n+1)            (2')   * */
/*  This computation is performed by the routine NEWTON.                 * */
/**************************************************************************/
/*  The starting approximations are complex numbers that are             * */
/*  equispaced on circles of suitable radii. The radius of each          * */
/*  circle, as well as the number of roots on each circle and the        * */
/*  number of circles, is determined by applying Rouche's theorem        * */
/*  to the functions a(k+1)*x**k and p(x)-a(k+1)*x**k, k=0,...,n.        * */
/*  This computation is performed by the routine START.                  * */
/**************************************************************************/
/*                              STOP CONDITION                           * */
/**************************************************************************/
/* If the condition                                                      * */
/*                     |p(x(j))|<EPS s(|x(j)|)                      (3)  * */
/* is satisfied,    where      s(x)=s(1)+x*s(2)+...+x**n * s(n+1),       * */
/* s(i)=|a(i)|*(1+3.8*(i-1)),  EPS is the machine precision (EPS=2**-53  * */
/* for the IEEE arithmetic), then the approximation x(j) is not updated  * */
/* and the subsequent iterations (1)  for i=j are skipped.               * */
/* The program stops if the condition (3) is satisfied for j=1,...,n,    * */
/* or if the maximum number NITMAX of  iterations   has   been reached.  * */
/* The condition (3) is motivated by a backward rounding error analysis  * */
/* of the Ruffini-Horner rule, moreover the condition (3) guarantees     * */
/* that the computed approximation x(j) is an exact root of a slightly   * */
/* perturbed polynomial.                                                 * */
/**************************************************************************/
/*             INCLUSION DISKS, A-POSTERIORI ERROR BOUNDS                * */
/**************************************************************************/
/* For each approximation x of a root, an a-posteriori absolute error    * */
/* bound r is computed according to the formula                          * */
/*                   r=n(|p(x)|+EPS s(|x|))/|p'(x)|                 (4)  * */
/* This provides an inclusion disk of center x and radius r containing a * */
/* root.                                                                 * */
/**************************************************************************/
/**************************************************************************/
/**************       MEANING OF THE INPUT VARIABLES         **************/
/**************************************************************************/
/**************************************************************************/
/*                                                                       * */
/*  -- N     : degree of the polynomial.                                 * */
/*  -- POLY  : complex vector of N+1 components, POLY(i) is the          * */
/*           coefficient of x**(i-1), i=1,...,N+1 of the polynomial p(x) * */
/*  -- EPS   : machine precision of the floating point arithmetic used   * */
/*            by the computer, EPS=2**(-53)  for the IEEE standard.      * */
/*  -- BIG   : the max real*8, BIG=2**1023 for the IEEE standard.        * */
/*  -- SMALL : the min positive real*8, SMALL=2**(-1074) for the IEEE.   * */
/*  -- NITMAX: the max number of allowed iterations.                     * */
/**************************************************************************/
/**************************************************************************/
/**************      MEANING OF THE OUTPUT VARIABLES         **************/
/**************************************************************************/
/**************************************************************************/
/*  ROOT   : complex vector of N components, containing the              * */
/*           approximations to the roots of p(x).                        * */
/*  RADIUS : real vector of N components, containing the error bounds to * */
/*           the approximations of the roots, i.e. the disk of center    * */
/*           ROOT(i) and radius RADIUS(i) contains a root of p(x), for   * */
/*           i=1,...,N. RADIUS(i) is set to -1 if the corresponding root * */
/*           cannot be represented as floating point due to overflow or  * */
/*           underflow.                                                  * */
/*  ERR    : vector of N components detecting an error condition;        * */
/*           ERR(j)=.TRUE. if after NITMAX iterations the stop condition * */
/*                         (3) is not satisfied for x(j)=ROOT(j);        * */
/*           ERR(j)=.FALSE.  otherwise, i.e., the root is reliable,      * */
/*                         i.e., it can be viewed as an exact root of a  * */
/*                         slightly perturbed polynomial.                * */
/*           The vector ERR is used also in the routine convex hull for  * */
/*           storing the abscissae of the vertices of the convex hull.   * */
/*  ITER   : number of iterations peformed.                              * */
/**************************************************************************/
/**************************************************************************/
/*************    MEANING OF THE AUXILIARY VARIABLES         **************/
/**************************************************************************/
/**************************************************************************/
/*  APOLY  : real vector of N+1 components used to store the moduli of   * */
/*           the coefficients of p(x) and the coefficients of s(x) used  * */
/*           to test the stop condition (3).                             * */
/*  APOLYR : real vector of N+1 components used to test the stop         * */
/*           condition                                                   * */
/**************************************************************************/
/******         WARNING:   2 is the output unit                    ********/
/**************************************************************************/
		

void polzeros_(int n, doublecomplex[] poly, double eps, 
			  double big, double theSmall, int nitmax, doublecomplex[] root, 
			  double[] radius, boolean[] err, int[] iter, double[] apoly, double[] apolyr){
    /* System generated locals */
    int i__1, i__2, i__3, i__4;
    double d__1, d__2;
    doublecomplex z__1=dc(), z__2=dc(), z__3=dc(), z__4=dc();


    /* Local variables */
    double amax;
    doublecomplex corr=dc();
    int i;

    doublecomplex abcorr=dc();
    int[] nzeros=new int[1];

 
    /* Function Body */
    if (z_abs(poly[n]) == 0.) 
	{
		PrintError("Inconsistent data: the leading coefficient is zero");
		return ;
    }
    if (z_abs(poly[0]) == 0.) 	
	{
		PrintError("The constant term is zero: deflate the polynomial");
		return ;
    }
/* Compute the moduli of the coefficients */
    amax = 0.;
    i__1 = n + 1;
    for (i = 1; i <= i__1; ++i) {
	apoly[i-1] = z_abs(poly[i-1]);
/* Computing MAX */
	d__1 = amax; d__2 = apoly[i-1];
	amax = Math.max(d__1,d__2);
	apolyr[i-1] = apoly[i-1];
/* L10: */
    }
    if (amax >= big / (n + 1)) 
	{
		PrintError("WARNING: COEFFICIENTS TOO BIG, OVERFLOW IS LIKELY");
    }
/* Initialize */
    i__1 = n;
    for (i = 1; i <= i__1; ++i) {
	radius[i-1] = 0.;
	err[i-1] = true;
/* L20: */
    }
/* Select the starting points */
    start_(n, apolyr, root, radius, nzeros, theSmall, big, err);
/* Compute the coefficients of the backward-error polynomial */
    i__1 = n + 1;
    for (i = 1; i <= i__1; ++i) {
	apolyr[n - i + 2-1] = eps * apoly[i-1] * ((n - i + 1) * (float)3.8 + 1)
		;
	apoly[i-1] = eps * apoly[i-1] * ((i - 1) * (float)3.8 + 1);
/* L30: */
    }
    if (apoly[1-1] == 0. || apoly[n + 1-1] == 0.) 
	{
		PrintError("WARNING: THE COMPUTATION OF SOME INCLUSION RADIUS MAY FAIL. THIS IS REPORTED BY RADIUS=0");
    }
    i__1 = n;
    for (i = 1; i <= i__1; ++i) {
	err[i-1] = true;
	if (radius[i-1] == -1.) {
	    err[i-1] = false;
	}
/* L40: */
    }
/* Starts Aberth's iterations */
    i__1 = nitmax;
    for (iter[0] = 1; iter[0] <= i__1; ++iter[0]) {
	i__2 = n;
	for (i = 1; i <= i__2; ++i) {
	    if (err[i-1]) {
		newton_(n, poly, apoly, apolyr, root[i-1], theSmall, 
			radius, corr, err, i-1);
		if (err[i-1]) {
		    aberth_(n, i, root, abcorr);
		    i__3 = i;
		    i__4 = i;
		    z__4.r = corr.r * abcorr.r - corr.i * abcorr.i;
		    z__4.i = corr.r * abcorr.i + corr.i * abcorr.r;
		    z__3.r = 1 - z__4.r;
		    z__3.i = -z__4.i;
		    z_div(z__2, corr, z__3);
		    z__1.r = root[i__4-1].r - z__2.r;
		    z__1.i = root[i__4-1].i - z__2.i;
		    root[i__3-1].r = z__1.r;
		    root[i__3-1].i = z__1.i;
		} else {
		    ++nzeros[0];
		    if (nzeros[0] == n) {
			return ;
		    }
		}
	    }
/* L50: */
	}
    }
} /* polzeros_ */

/**************************************************************************/
/*                             SUBROUTINE NEWTON                         * */
/**************************************************************************/
/* Compute  the Newton's correction, the inclusion radius (4) and checks * */
/* the stop condition (3)                                                * */
/**************************************************************************/
/* Input variables:                                                      * */
/*     N     : degree of the polynomial p(x)                             * */
/*     POLY  : coefficients of the polynomial p(x)                       * */
/*     APOLY : upper bounds on the backward perturbations on the         * */
/*             coefficients of p(x) when applying Ruffini-Horner's rule  * */
/*     APOLYR: upper bounds on the backward perturbations on the         * */
/*             coefficients of p(x) when applying (2), (2')              * */
/*     Z     : value at which the Newton correction is computed          * */
/*     SMALL : the min positive real*8, SMALL=2**(-1074) for the IEEE.   * */
/**************************************************************************/
/* Output variables:                                                     * */
/*     RADIUS: upper bound to the distance of Z from the closest root of * */
/*             the polynomial computed according to (4).                 * */
/*     CORR  : Newton's correction                                       * */
/*     AGAIN : this variable is .true. if the computed value p(z) is     * */
/*             reliable, i.e., (3) is not satisfied in Z. AGAIN is       * */
/*             .false., otherwise.                                       * */
/**************************************************************************/

void newton_(int n, doublecomplex[] poly, double[] apoly, double[] apolyr, 
			doublecomplex z, double theSmall, double[] radius, doublecomplex corr,
			boolean[] again, int ik){
    /* System generated locals */
    int i__1;
    double d__1;
    doublecomplex z__1=dc(), z__2=dc();


    /* Local variables */
    double absp;
    doublecomplex ppsp=dc();
    int i;
    doublecomplex p=dc(), p1=dc();
    double ap, az;
    doublecomplex zi=dc(), den=dc();
    double azi;


    /* Function Body */
    az = z_abs(z);
/* If |z|<=1 then apply Ruffini-Horner's rule for p(z)/p'(z) */
/* and for the computation of the inclusion radius */
    if (az <= 1.) {
	i__1 = n + 1;
	p.r = poly[i__1-1].r;
	p.i = poly[i__1-1].i;
	ap = apoly[n + 1-1];
	p1.r = p.r;
	p1.i = p.i;
	for (i = n; i >= 2; --i) {
	    z__2.r = p.r * z.r - p.i * z.i;
	    z__2.i = p.r * z.i + p.i * z.r;
	    i__1 = i;
	    z__1.r = z__2.r + poly[i__1-1].r;
	    z__1.i = z__2.i + poly[i__1-1].i;
	    p.r = z__1.r;
	    p.i = z__1.i;
	    z__2.r = p1.r * z.r - p1.i * z.i;
	    z__2.i = p1.r * z.i + p1.i * z.r;
	    z__1.r = z__2.r + p.r;
	    z__1.i = z__2.i + p.i;
	    p1.r = z__1.r;
	    p1.i = z__1.i;
	    ap = ap * az + apoly[i-1];
/* L10: */
	}
	z__2.r = p.r * z.r - p.i * z.i;
	z__2.i = p.r * z.i + p.i * z.r;
	z__1.r = z__2.r + poly[1-1].r;
	z__1.i = z__2.i + poly[1-1].i;
	p.r = z__1.r;
	p.i = z__1.i;
	ap = ap * az + apoly[1-1];
	z_div(z__1, p, p1);
	corr.r = z__1.r;
	corr.i = z__1.i;
	absp = z_abs(p);
	ap = ap;
	again[ik] = (absp > theSmall + ap);
	if (! again[ik]) {
	    radius[ik] = n * (absp + ap) / z_abs(p1);
	}
	return;
    } else {
/* If |z|>1 then apply Ruffini-Horner's rule to the reversed polynomia
l */
/* and use formula (2) for p(z)/p'(z). Analogously do for the inclusio
n */
/* radius. */
	z_div(z__1, c_b35, z);
	zi.r = z__1.r;
	zi.i = z__1.i;
	azi = 1 / az;
	p.r = poly[1-1].r;
	p.i = poly[1-1].i;
	p1.r = p.r;
	p1.i = p.i;
	ap = apolyr[n + 1-1];
	for (i = n; i >= 2; --i) {
	    z__2.r = p.r * zi.r - p.i * zi.i;
	    z__2.i = p.r * zi.i + p.i *  zi.r;
	    i__1 = n - i + 2;
	    z__1.r = z__2.r + poly[i__1-1].r;
	    z__1.i = z__2.i + poly[i__1-1].i;
	    p.r = z__1.r;
	    p.i = z__1.i;
	    z__2.r = p1.r * zi.r - p1.i * zi.i;
	    z__2.i = p1.r * zi.i + p1.i *  zi.r;
	    z__1.r = z__2.r + p.r;
	    z__1.i = z__2.i + p.i;
	    p1.r = z__1.r;
	    p1.i = z__1.i;
	    ap = ap * azi + apolyr[i-1];
/* L20: */
	}
	z__2.r = p.r * zi.r - p.i * zi.i;
	z__2.i = p.r * zi.i + p.i * zi.r;
	i__1 = n + 1;
	z__1.r = z__2.r + poly[i__1-1].r;
	z__1.i = z__2.i + poly[i__1-1].i;
	p.r = z__1.r;
	p.i = z__1.i;
	ap = ap * azi + apolyr[1-1];
	absp = z_abs(p);
	again[ik] = absp > theSmall + ap;
	z__2.r = p.r * z.r - p.i * z.i;
	z__2.i = p.r * z.i + p.i * z.r;
	z_div(z__1, z__2, p1);
	ppsp.r = z__1.r;
	ppsp.i = z__1.i;
	d__1 = (double) n;
	z__2.r = d__1 * ppsp.r;
	z__2.i = d__1 * ppsp.i;
	z__1.r = z__2.r - 1;
	z__1.i = z__2.i;
	den.r = z__1.r;
	den.i = z__1.i;
	z_div(z__2, ppsp, den);
	z__1.r = z.r * z__2.r - z.i * z__2.i;
	z__1.i = z.r * z__2.i + z.i * z__2.r;
	corr.r = z__1.r;
	corr.i = z__1.i;
	if (again[ik]) {
	    return;
	}
	radius[ik] = z_abs(ppsp) + ap * az / z_abs(p1);
	radius[ik] = n * radius[ik] / z_abs(den);
	radius[ik] *= az;
    }
} /* newton_ */


/**************************************************************************/
/*                             SUBROUTINE ABERTH                         * */
/**************************************************************************/
/* Compute  the Aberth correction. To save time, the reciprocation of    * */
/* ROOT(J)-ROOT(I) could be performed in single precision (complex*8)    * */
/* In principle this might cause overflow if both ROOT(J) and ROOT(I)    * */
/* have too theSmall moduli.                                                * */
/**************************************************************************/
/* Input variables:                                                      * */
/*     N     : degree of the polynomial                                  * */
/*     ROOT  : vector containing the current approximations to the roots * */
/*     J     : index of the component of ROOT with respect to which the  * */
/*             Aberth correction is computed                             * */
/**************************************************************************/
/* Output variable:                                                      * */
/*     ABCORR: Aberth's correction (compare (1))                         * */
/**************************************************************************/

void aberth_(int n, int j, doublecomplex[] root, doublecomplex abcorr){
    /* System generated locals */
    int i__1, i__2;
    doublecomplex z__1=dc(), z__2=dc();

     /* Local variables */
    int i;
    doublecomplex z=dc(), zj=dc();

/* The next variable Z could be defined as complex*8 to speed up the */
/* computation, this slightly reduces the robustness of the program */
    /* Parameter adjustments */

    /* Function Body */
    abcorr.r = 0.;
    abcorr.i = 0.;
    i__1 = j;
    zj.r = root[i__1-1].r;
    zj.i = root[i__1-1].i;
    i__1 = j - 1;
    for (i = 1; i <= i__1; ++i) {
	i__2 = i;
	z__1.r = zj.r - root[i__2-1].r;
	z__1.i = zj.i - root[i__2-1].i;
	z.r = z__1.r;
	z.i = z__1.i;
	z_div(z__2, c_b35, z);
	z__1.r = abcorr.r + z__2.r;
	z__1.i = abcorr.i + z__2.i;
	abcorr.r = z__1.r;
	abcorr.i = z__1.i;
/* L10: */
    }
    i__1 = n;
    for (i = j + 1; i <= i__1; ++i) {
	i__2 = i;
	z__1.r = zj.r - root[i__2-1].r;
	z__1.i = zj.i - root[i__2-1].i;
	z.r = z__1.r;
	z.i = z__1.i;
	z_div(z__2, c_b35, z);
	z__1.r = abcorr.r + z__2.r;
	z__1.i = abcorr.i + z__2.i;
	abcorr.r = z__1.r;
	abcorr.i = z__1.i;
/* L20: */
    }
} /* aberth_ */

/**************************************************************************/
/*                             SUBROUTINE START                          * */
/**************************************************************************/
/* Compute  the starting approximations of the roots                     * */
/**************************************************************************/
/* Input variables:                                                      * */
/*     N     :  number of the coefficients of the polynomial             * */
/*     A     :  moduli of the coefficients of the polynomial             * */
/*     SMALL : the min positive real*8, SMALL=2**(-1074) for the IEEE.   * */
/*     BIG   : the max real*8, BIG=2**1023 for the IEEE standard.        * */
/* Output variables:                                                     * */
/*     Y     :  starting approximations                                  * */
/*     RADIUS:  if a component is -1 then the corresponding root has a   * */
/*              too big or too small modulus in order to be represented  * */
/*              as double float with no overflow/underflow               * */
/*     NZ    :  number of roots which cannot be represented without      * */
/*              overflow/underflow                                       * */
/* Auxiliary variables:                                                  * */
/*     H     :  needed for the computation of the convex hull            * */
/**************************************************************************/
/* This routines selects starting approximations along circles center at * */
/* 0 and having suitable radii. The computation of the number of circles * */
/* and of the corresponding radii is performed by computing the upper    * */
/* convex hull of the set (i,log(A(i))), i=1,...,n+1.                    * */
/**************************************************************************/

int start_(int n, double[] a, doublecomplex[] y, double[] radius, 
           int[] nz, double theSmall, double big, boolean[] h){
    /* System generated locals */
    int i__1, i__2, i__3;
    double d__1, d__2;
    doublecomplex z__1=dc(), z__2=dc(), z__3=dc();


    /* Local variables */
    int iold;
    double xbig, temp;
    int i, j;
    double r=0.;
    int jj;
    double th, xsmall;
    int nzeros;
    double ang;


    /* Function Body */
    xsmall = Math.log(theSmall);
    xbig = Math.log(big);
    nz[0] = 0;
/* Compute the logarithm A(I) of the moduli of the coefficients of */
/* the polynomial and then the upper covex hull of the set (A(I),I) */
    i__1 = n + 1;
    for (i = 1; i <= i__1; ++i) {
	if (a[i-1] != 0.) {
	    a[i-1] = Math.log(a[i-1]);
	} else {
	    a[i-1] = -1e30;
	}
/* L10: */
    }
    i__1 = n + 1;
    cnvex_(i__1, a, h);
/* Given the upper convex hull of the set (A(I),I) compute the moduli */
/* of the starting approximations by means of Rouche's theorem */
    iold = 1;
    th = 6.2831853071796 / n;
    i__1 = n + 1;
    for (i = 2; i <= i__1; ++i) {
	if (h[i-1]) {
	    nzeros = i - iold;
	    temp = (a[iold-1] - a[i-1]) / nzeros;
/* Check if the modulus is too small */
	    if (temp < -xbig && temp >= xsmall) {
			nz[0] += nzeros;
			r = 1. / big;
	    }
	    if (temp < xsmall) {
			nz[0] += nzeros;
	    }
/* Check if the modulus is too big */
	    if (temp > xbig) {
			r = big;
			nz[0] += nzeros;
	    }
/* Computing MAX */
	    d__1 = -xbig;
	    if (temp <= xbig && temp > Math.max(d__1,xsmall)) {
			r = Math.exp(temp);
	    }
/* Compute NZEROS approximations equally distributed in the disk o
f */
/* radius R */
	    ang = 6.2831853071796 / nzeros;
	    i__2 = i - 1;
	    for (j = iold; j <= i__2; ++j) {
			jj = j - iold + 1;
			if (r <= 1. / big || r == big) {
		    radius[j-1] = -1.;
		}
		i__3 = j;
		d__1 = Math.cos(ang * jj + th * i + .7);
		d__2 = Math.sin(ang * jj + th * i + .7);
		z__3.r = d__2 * (float)0.;
		z__3.i = d__2 * (float)1.;
		z__2.r = d__1 + z__3.r;
		z__2.i = z__3.i;
		z__1.r = r * z__2.r;
		z__1.i = r * z__2.i;
		y[i__3-1].r = z__1.r;
		y[i__3-1].i = z__1.i;
/* L30: */
	    }
	    iold = i;
	}
/* L20: */
    }
    return 0;
} /* start_ */


/**************************************************************************/
/*                             SUBROUTINE CNVEX                          * */
/**************************************************************************/
/* Compute  the upper convex hull of the set (i,a(i)), i.e., the set of  * */
/* vertices (i_k,a(i_k)), k=1,2,...,m, such that the points (i,a(i)) lie * */
/* below the straight lines passing through two consecutive vertices.    * */
/* The abscissae of the vertices of the convex hull equal the indices of * */
/* the TRUE  components of the logical output vector H.                  * */
/* The used method requires O(nlog n) comparisons and is based on a      * */
/* divide-and-conquer technique. Once the upper convex hull of two       * */
/* contiguous sets  (say, {(1,a(1)),(2,a(2)),...,(k,a(k))} and           * */
/* {(k,a(k)), (k+1,a(k+1)),...,(q,a(q))}) have been computed, then       * */
/* the upper convex hull of their union is provided by the subroutine    * */
/* CMERGE. The program starts with sets made up by two consecutive       * */
/* points, which trivially constitute a convex hull, then obtains sets   * */
/* of 3,5,9... points,  up to  arrive at the entire set.                 * */
/* The program uses the subroutine  CMERGE; the subroutine CMERGE uses   * */
/* the subroutines LEFT, RIGHT and CTEST. The latter tests the convexity * */
/* of the angle formed by the points (i,a(i)), (j,a(j)), (k,a(k)) in the * */
/* vertex (j,a(j)) up to within a given tolerance TOLER, where i<j<k.    * */
/**************************************************************************/

int cnvex_(int n, double a[], boolean h[]){
    /* System generated locals */
    int i__1, i__2, i__3;
    /* Local variables */
    int i, j, k, m, jc, nj;

    /* Function Body */
    i__1 = n;
    for (i = 1; i <= i__1; ++i) {
		h[i-1] = true;
/* L10: */
    }
/* compute K such that N-2<=2**K<N-1 */
    k = (int) (Math.log(n - 2.) / Math.log(2.));
    i__1 = k + 1;
    if (pow_ii(c__2, i__1) <= n - 2) {
		++k;
    }
/* For each M=1,2,4,8,...,2**K, consider the NJ pairs of consecutive */
/* sets made up by M+1 points having the common vertex */
/* (JC,A(JC)), where JC=M*(2*J+1)+1 and J=0,...,NJ, */
/* NJ=MAX(0,INT((N-2-M)/(M+M))). */
/* Compute the upper convex hull of their union by means of the */
/* subroutine CMERGE */
    m = 1;
    i__1 = k;
    for (i = 0; i <= i__1; ++i) {
/* Computing MAX */
		i__2 = 0;
		i__3 = (n - 2 - m) / (m + m);
		nj = Math.max(i__2,i__3);
		i__2 = nj;
		for (j = 0; j <= i__2; ++j) {
	    	jc = (j + j + 1) * m + 1;
	    	cmerge_(n, a, jc, m, h);
/* L30: */
	}
	m += m;
/* L20: */
    }
    return 0;
} /* cnvex_ */


/**************************************************************************/
/*                             SUBROUTINE LEFT                           * */
/**************************************************************************/
/* Given as input the integer I and the vector H of logical, compute the * */
/* the maximum integer IL such that IL<I and H(IL) is TRUE.              * */
/**************************************************************************/
/* Input variables:                                                      * */
/*     N   : length of the vector H                                      * */
/*     H   : vector of logical                                           * */
/*     I   : integer                                                     * */
/**************************************************************************/
/* Output variable:                                                      * */
/*     IL  : maximum integer such that IL<I, H(IL)=.TRUE.                * */
/**************************************************************************/

int left_(int n, boolean h[], int i, int[] il){
    /* Function Body */
    for (il[0] = i - 1; il[0] >= 0; --il[0]) {
		if (h[il[0]-1]) {
	    	return 0;
		}
/* L10: */
    }
    return 0;
} /* left_ */



/**************************************************************************/
/*                             SUBROUTINE RIGHT                          * */
/**************************************************************************/
/**************************************************************************/
/* Given as input the integer I and the vector H of logical, compute the * */
/* the minimum integer IR such that IR>I and H(IL) is TRUE.              * */
/**************************************************************************/
/**************************************************************************/
/* Input variables:                                                      * */
/*     N   : length of the vector H                                      * */
/*     H   : vector of logical                                           * */
/*     I   : integer                                                     * */
/**************************************************************************/
/* Output variable:                                                      * */
/*     IR  : minimum integer such that IR>I, H(IR)=.TRUE.                * */
/**************************************************************************/

int right_(int n, boolean h[], int i, int[] ir){
    /* System generated locals */
    int i__1;


    /* Function Body */
    i__1 = n;
    for (ir[0] = i + 1; ir[0] <= i__1; ++ir[0]) {
		if (h[ir[0]-1]) {
	    	return 0;
	}
/* L10: */
    }
    return 0;
} /* right_ */



/**************************************************************************/
/*                             SUBROUTINE CMERGE                         * */
/**************************************************************************/
/* Given the upper convex hulls of two consecutive sets of pairs         * */
/* (j,A(j)), compute the upper convex hull of their union                * */
/**************************************************************************/
/* Input variables:                                                      * */
/*     N    : length of the vector A                                     * */
/*     A    : vector defining the points (j,A(j))                        * */
/*     I    : abscissa of the common vertex of the two sets              * */
/*     M    : the number of elements of each set is M+1                  * */
/**************************************************************************/
/* Input/Output variable:                                                * */
/*     H    : vector defining the vertices of the convex hull, i.e.,     * */
/*            H(j) is .TRUE. if (j,A(j)) is a vertex of the convex hull  * */
/*            This vector is used also as output.                        * */
/**************************************************************************/

int cmerge_(int n, double[] a, int i, int m, boolean[] h){
    /* System generated locals */
    int i__1, i__2;

    boolean tstl, tstr;
	int ill[]=new int[1], irr[]=new int[1];
	int il[]=new int[1], ir[]=new int[1];

/* at the left and the right of the common vertex (I,A(I)) determine */
/* the abscissae IL,IR, of the closest vertices of the upper convex */
/* hull of the left and right sets, respectively */
    /* Parameter adjustments */

    /* Function Body */
    left_(n, h, i, il);
    right_(n, h, i, ir);
/* check the convexity of the angle formed by IL,I,IR */
    if (ctest_(n, a, il[0], i, ir[0])) {
		return 0;
    } else {
/* continue the search of a pair of vertices in the left and right */
/* sets which yield the upper convex hull */
		h[i-1] = false;
		while( true ){
//L10:
			if (il[0] == i - m) {
	    		tstl = true;
			} else {
	    		left_(n, h, il[0], ill);
	    		tstl = ctest_(n, a, ill[0], il[0], ir[0]);
			}
/* Computing MIN */
			i__1 = n;
			i__2 = i + m;
			if (ir[0] == Math.min(i__1,i__2)) {
	    		tstr = true;
			} else {
	    		right_(n, h, ir[0], irr);
	   		 	tstr = ctest_(n, a, il[0], ir[0], irr[0]);
			}
			h[il[0]-1] = tstl;
			h[ir[0]-1] = tstr;
			if (tstl && tstr) {
	    		return 0;
			}
			if (! tstl) {
	    		il[0] = ill[0];
			}
			if (! tstr) {
	    		ir[0] = irr[0];
			}
		}
//	goto L10;
    }
} /* cmerge_ */


/**************************************************************************/
/*                             FUNCTION CTEST                            * */
/**************************************************************************/
/* Test the convexity of the angle formed by (IL,A(IL)), (I,A(I)),       * */
/* (IR,A(IR)) at the vertex (I,A(I)), up to within the tolerance         * */
/* TOLER. If convexity holds then the function is set to .TRUE.,         * */
/* otherwise CTEST=.FALSE. The parameter TOLER is set to 0.4 by default. * */
/**************************************************************************/
/* Input variables:                                                      * */
/*     N       : length of the vector A                                  * */
/*     A       : vector of double                                        * */
/*     IL,I,IR : integers such that IL<I<IR                              * */
/**************************************************************************/
/* Output:                                                               * */
/*     .TRUE. if the angle formed by (IL,A(IL)), (I,A(I)), (IR,A(IR)) at * */
/*            the vertex (I,A(I)), is convex up to within the tolerance  * */
/*            TOLER, i.e., if                                            * */
/*            (A(I)-A(IL))*(IR-I)-(A(IR)-A(I))*(I-IL)>TOLER.             * */
/*     .FALSE.,  otherwise.                                              * */
/**************************************************************************/

boolean ctest_(int n, double[] a, int il, int i, int ir){
    /* System generated locals */
    boolean ret_val;

    /* Local variables */
    double s1, s2;


    /* Function Body */
    s1 = a[i-1] - a[il-1];
    s2 = a[ir-1] - a[i-1];
    s1 *= ir - i;
    s2 *= i - il;
    ret_val = false;
    if (s1 > s2 + .4) {
		ret_val = true;
    }
    return ret_val;
} /* ctest_ */

}

