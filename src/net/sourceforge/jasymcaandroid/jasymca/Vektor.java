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

import java.util.Vector;
import java.io.*;

public class Vektor extends Algebraic{
	private Algebraic a[];


	// Constructors

	/** Create a vector given the components
	as array.
	@param a array of components
	@return vektor with components a
	*/
	public Vektor(Algebraic[] a){
		this.a = a;
	}

	/** Create a vector with identical
	components.
	@param x The vector component..
	@param n vektor length.
	@return vektor with n identical components.
	*/
	public Vektor(Algebraic x, int n){
		this.a = new Zahl[n];
		for(int i=0; i<n; i++)
			this.a[i] = x;
	}

	/** Create a vector with given length.
	All components are initialized to 0.
	@param n vektor length.
	@return vektor with n zeros.
	*/
	public Vektor(int n){
		this( Zahl.ZERO, n );
	}

	/** Create a vector from an algebraic
	object x. If x is a vector, return
	a copy of x. If x is a scalar, return
	a single element vektor.
	@param x Algebraic.
	@return copy of x or vector containing x.
	*/
	public Vektor(Algebraic c){
		if(c instanceof Vektor)
			a = Poly.clone(((Vektor)c).a);
		else{
			a = new Algebraic[] {c};
		}
	}

	/** Create real vector from double
	array.
	@param x Array of doubles.
	@return vektor with components x.
	*/
	public Vektor(double[] x){
		a = new Algebraic[x.length];
		for(int i=0; i<x.length; i++)
			a[i] = new Unexakt(x[i]);
	}
	
	/** Create complex vector from 
	two double arrays.
	@param r Array of real parts.
	@param i Array of imaginary parts.
	@return complex vektor with components r+j*i.
	*/
	public Vektor(double[] r, double[] i){
		a = new Algebraic[r.length];
		for(int k=0; k<r.length; k++)
			a[k] = new Unexakt(r[k], i[k]);
	}


	/** Create Vector from java.util.Vector.
	All elements of java.util.Vector must
	be Algebraics, otherwise an exception is thrown.
	@param v java.util.Vector of components.
	@return vector with components taken from v.
	*/
	public static Vektor create(Vector v) throws JasymcaException{
		Algebraic[] a = new Algebraic[v.size()];
		for(int i=0; i<a.length; i++){
			Object x = v.elementAt(i);
			if(!(x instanceof Algebraic))
				throw new JasymcaException("Error creating Vektor.");
			a[i] = (Algebraic)x;
		}
		return new Vektor(a);
	}

	// Methods for accessing components	

	/** Get vector component by index.
	Index i must be 0<=i<length().
	@param  index of component.
	@return the vector component with index i.
	*/
	public Algebraic get(int i) throws JasymcaException{ 
		if(i<0 || i>=a.length)
			throw new JasymcaException("Index out of bounds.");
		return a[i];
	}

	/** Get vector components as array.
	@return array of vector components.
	*/
	public Algebraic[] get() { 
		return a;
	}


	/** Get vector components as java.util.Vector.
	@return components as java.util.Vector.
	*/	
	public Vector vector(){
		Vector r = new Vector(a.length);	
		for(int i=0; i<a.length; i++)
			r.addElement(a[i]);
		return r;
	}


	/** Get vector components as double array.
	Throws exception if vector is complex or
	not constant.
	@return array of vector components.
	*/
	public double[] getDouble() throws JasymcaException{ 
		double[] x = new double[a.length];
		for(int i=0; i<a.length; i++){
			Algebraic c = a[i];
			if( !(c instanceof Zahl) )
				throw new JasymcaException(
					"Vector element not constant:"+c);
			x[i] = ((Zahl)c).unexakt().real;
		}
		return x;
	}
	
	/** Creates a new Vector with reversed
	order.
	@return   reversed vector.
	*/
	public Vektor reverse(){
		Algebraic b[] = new Algebraic[a.length];
		for(int i=0; i<a.length; i++){
			b[i] = a[a.length-i-1];
		}
		return new Vektor(b);
	}


	
	/** Set vector component by index.
	Index i must be 0<=i<length().
	@param  index of component.
	@param  algebraic object to be inserted.
	*/
	public void set(int i, Algebraic x) throws JasymcaException{ 
		if(i<0 || i>=a.length)
			throw new JasymcaException("Index out of bounds.");
		a[i] = x;
	}
	
	/** Length of vector.
	@return  Length of vektor.
	*/
	public int length(){
		return a.length;
	}


	/** Query: Is this algebraic object a scalar.
	@return     True if this object is a scalar, false otherwise.
	*/		
	public boolean scalarq(){ 
		return false; 
	}


	/** Query: Is this algebraic object konstant, i.e does not
	depend on any variable.
	@return     True if this object is constant, false otherwise.
	*/		
	public boolean constantq(){
		for(int i=0; i<a.length; i++)
			if(!a[i].constantq())
				return false;
		return true;
	}

	/** Query: Is this algebraic object exact.
	@return     True if this object is exact, false otherwise.
	*/		
	public boolean exaktq(){ 
		boolean exakt = a[0].exaktq();
		for(int i=1; i<a.length; i++)
			exakt = exakt && a[i].exaktq();
		return exakt;
	}
	
	
	
	/** Reduce this algebraic object as much as possible, e.g.
	single element matrices are reduced to scalars etc.
	@return    This object reduced as much as possible..
	*/		
	public Algebraic reduce(){
		if(a.length==1)
			return a[0];
		return this;
	}

	/** Conjugate complex of an algebraic object a+i*b.
	@return     The conjugate complex a-i*b.
	*/		
	public Algebraic cc() throws JasymcaException{
		Algebraic b[] = new Algebraic[a.length];
		for(int i=0; i<a.length; i++)
			b[i] = a[i].cc();
		return new Vektor(b);
	}
	
	/** Apply an algebraic function to this algebraic object.
	@param f    The algebraic function.
	@return     The function value.
	*/	
	public Algebraic map( LambdaAlgebraic f ) throws JasymcaException{
		Algebraic cn[] = new Algebraic[a.length];
		for(int i=0; i<a.length; i++)
			cn[i] = f.f_exakt(a[i]);
		return new Vektor(cn); 
	}

	/** Apply an algebraic function to this algebraic object.
	@param f    The algebraic function.
	@return     The function value.
	*/	
	public Algebraic map_lambda( LambdaAlgebraic f, Algebraic arg2 ) 
							throws ParseException,JasymcaException{
		Algebraic[] b  = new Algebraic[a.length];
		if( arg2 instanceof Vektor && 
			((Vektor)arg2).length()==a.length ){
		// Binary operators: process components indivicually
			for(int i=0; i<b.length; i++){
				Algebraic c = ((Vektor)arg2).get(i);
				Object r = a[i].map_lambda( f, c );
				if( r instanceof Algebraic )
					b[i] = (Algebraic)r;
				else 
					throw new JasymcaException(
						"Cannot evaluate function to algebraic.");
			}
		}else{
			for(int i=0; i<b.length; i++){
				Object r = a[i].map_lambda( f, arg2 );
				if( r instanceof Algebraic )
					b[i] = (Algebraic)r;
				else 
					throw new JasymcaException(
						"Cannot evaluate function to algebraic.");
			}
		}
		return new Vektor( b );
	}
	
		/** The value of this algebraic expression, if variable var
	assumes the value of the algebraic expression v
	@param  var   the variable to substitute.
	@param  x     the value to substitute for var.
	*/
	public Algebraic value(Variable var, Algebraic x) throws JasymcaException{
		Algebraic[] b  = new Algebraic[a.length];
		for(int i=0; i<b.length; i++)
			b[i] = a[i].value(var,x);
		return new Vektor(b);
	}



	/** Add two algebraic objects. 
	@param x    Algebraic object to be added.
	@return     The sum this+x.
	*/
	public Algebraic add (Algebraic x) throws JasymcaException{
		if( x.scalarq() )
			x = x.promote( this );
		if( x instanceof Vektor  &&
		    ((Vektor)x).length() == a.length ) {
			Algebraic b[] = new Algebraic[a.length];
			for(int i=0; i<a.length; i++)
				b[i] = a[i].add( ((Vektor)x).a[i] );
			return new Vektor(b);
		}
		throw new JasymcaException("Wrong Vektor dimension.");
	}

	/** Vector multiplication.
	@param x    Algebraic object to be multiplied.
	@return     The product this*x.
	*/
	public Algebraic mult(Algebraic x) throws JasymcaException{
		if( x.scalarq() ){
			Algebraic b[] = new Algebraic[a.length];
			for(int i=0; i<a.length; i++)
				b[i] = x.mult(a[i]);
			return new Vektor(b);
		}
		if( x instanceof Vektor  &&
		    ((Vektor)x).length() == a.length ) {
			Algebraic r = Zahl.ZERO;
			for(int i=0; i<a.length; i++)
				r = r.add( a[i].mult( ((Vektor)x).a[i] ));
			return r;
		}
		throw new JasymcaException("Wrong Vektor dimension.");
	}
	
	/** Divide two algebraic objects. If x is an equalsized 
	vector, divide each component of x, else divide each
	component by x.
	@param x    The divisor algebraic object.
	@return     The quotient this/x.
	*/	
	public Algebraic div(Algebraic x) throws JasymcaException{
		if( x.scalarq() ){
			Algebraic b[] = new Algebraic[a.length];
			for(int i=0; i<a.length; i++)
				b[i] = a[i].div(x);
			return new Vektor(b);
		}
		throw new JasymcaException("Divide not implemented for vektors");
	}
	
	/** Differentiate with respect to a variable.
	@param x    The variable.
	@return     The derivative.
	*/	
	public Algebraic deriv(Variable var) throws JasymcaException{
		Algebraic nc[] = new Algebraic[a.length];
		for(int i=0; i<a.length; i++)
			nc[i] = a[i].deriv(var);
		return new Vektor(nc);
	}
	
	/** Integrate with respect to a variable.
	@param x    The variable.
	@return     The integral.
	*/	
	public Algebraic integrate(Variable var) throws JasymcaException{
		Algebraic nc[] = new Algebraic[a.length];
		for(int i=0; i<a.length; i++)
			nc[i] = a[i].integrate(var);
		return new Vektor(nc);
	}
	
	/** Norm of an algebraic object. Always >= 0; 0 only if this==0.
	@return     The norm.
	*/	
	public double norm(){
		double r = 0.;
		for(int i=0; i<a.length; i++)
			r += a[i].norm();
		return r;
	}
	
	/** Query: Is this algebraic object equal to x.
	@param x    The object for comparison.
	@return     True if this object equals x, false otherwise.
	*/		
	public boolean equals(Object x){
		if(!(x instanceof Vektor) || ((Vektor)x).a.length!=a.length)
			return false;
		for(int i=0; i<a.length; i++)
			if(!a[i].equals(((Vektor)x).a[i]))
				return false;
		return true;
	}	
	
	/** Create string representation of this vector.
	@return    String representation of this vector.
	*/								
	public String toString(){ 
		String r = "[ ";
		for(int i=0; i<a.length; i++){
			r += StringFmt.compact(a[i].toString());
			if(i<a.length-1)
				r += "  ";
		}
		return r+" ]";
	}
	
	public void print( PrintStream p ){
		p.print( "[ " );
		for(int i=0; i<a.length; i++){
			String r = StringFmt.compact(a[i].toString());
			if(i<a.length-1)
				r += "  ";
			p.print( r );
		}
		p.print( " ]" );
	}
	
	


	/** Query: Does this algebraic object depend on the variable var.
	@param var  The variable.
	@return     True if this object depends on var, false otherwise.
	*/		
	public boolean depends(Variable var) {
		for(int i=0; i<a.length; i++)
			if(a[i].depends(var))
				return true;
		return false; 
	}
	

	
}
