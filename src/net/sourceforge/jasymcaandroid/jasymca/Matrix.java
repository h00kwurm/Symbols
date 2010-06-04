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
import java.io.*;

public class Matrix extends Algebraic{
	private Algebraic a[][];

	// Constructors

	/** Create Matrix given array of
	components.
	@param a Array of components
	@return Matrix with components a
	*/	
	public Matrix(Algebraic[][] a){
		this.a = a;
	}

	/** Create Matrix with 
	identical components.
	@param x Matrix component.
	@param nrow number of rows.
	@param ncol number of coulmns.
	@return Matrix with identical components,
	*/	
	public Matrix(Algebraic x, int nrow, int ncol){
		this.a = new Algebraic[nrow][ncol];
		for(int i=0; i<nrow; i++){
			for(int k=0; k<ncol; k++){
				a[i][k] = x;
			}
		}
	}

	/** Create Matrix with given 
	row and column dimensions.
	All components are initialized to 0.
	@param nrow number of rows.
	@param ncol number of coulmns.
	@return Matrix with zeros,
	*/	
	public Matrix(int nrow, int ncol){
		this( Zahl.ZERO, nrow, ncol );
	}


	/** Create real Matrix from
	parts of double array.
	@param b double array.
	@param nr number of rows.
	@param nc number of columns.
	@return nrxnc Matrix from b..
	*/	
	public Matrix(double[][] b, int nr, int nc){
		a =new Algebraic[nr][nc];
		nr = Math.min(nr,b.length);
		nc = Math.min(nc,b[0].length);
		for(int i=0; i<nr; i++)
			for(int k=0; k<nc; k++){
				a[i][k]=new Unexakt(b[i][k]);
			}
	}

	/** Create real Matrix from
	double array.
	@param b double array.
	@return Matrix real components.
	*/	
	public Matrix(double[][] b){
		this(b, b.length, b[0].length);
	}
	


	/** Create Matrix from
	arbitrary Algebraic object x.
	if x is a Matrix, return x,
	else promote to Matrix.
	@param x Algebraic object.
	@return Matrix x.
	*/	
	public Matrix(Algebraic x){
		if(x==null){
			this.a = new Algebraic[][] {{ Zahl.ZERO }};
		}else if(x instanceof Vektor){
			this.a = new Algebraic[][] { ((Vektor)x).get() };
		}else if(x instanceof Matrix){
			this.a = ((Matrix)x).a;
		}else{
			this.a = new Algebraic[][] {{ x }};
		}
	}
	
	
	// Routines to get/set properties

	/** Get Matrix component by index.
	@param  i Rowindex, 0<=i<nrow
	@param  k Columnindex, 0<=i<ncol
	@return the Matrix component a[i][k].
	*/
	public Algebraic get(int i, int k) throws JasymcaException{ 
		if(i<0 || i>=a.length || k<0 || k>=a[0].length)
			throw new JasymcaException("Index out of bounds.");
		return a[i][k];
	}

	/** Set Matrix component by index.
	@param  i Rowindex, 0<=i<nrow
	@param  k Columnindex, 0<=i<ncol
	@param  x Algebraic object to inserted
	*/
	public void set(int i, int k, Algebraic x) throws JasymcaException{ 
		if(i<0 || i>=a.length || k<0 || k>=a[0].length)
			throw new JasymcaException("Index out of bounds.");
		a[i][k] = x;
	}

	/** Number of rows in this matrix.
	@return  Number of rows.
	*/
	public int nrow(){ return a.length   ;}


	/** Number of columns in this matrix.
	@return  Number of rows.
	*/
	public int ncol(){ return a[0].length;}



	/** Extract nrxnc Matrix components as
	double array.
	@param nr Number of rows.
	@param nc Number of columns,
	@return  double array of real components.
	*/
	public double[][] getDouble(int nr, int nc) throws JasymcaException{ 
		if(nr==0) nr = a.length;
		if(nc==0) nc = a[0].length;
		double b[][] = new double[nr][nc];
		nr = Math.min(nr,a.length);
		nc = Math.min(nc,a[0].length);
		for(int i=0; i<nr; i++){
			for(int k=0; k<nc; k++){
				Algebraic x = a[i][k];
				if(!(x instanceof Unexakt) || x.komplexq() )
					throw new JasymcaException("Not a real, double Matrix");
				b[i][k] = ((Unexakt)x).real;
			}
		}
		return b;
	}


	/** Extract  Matrix components as
	double array.
	@return  double array of real components.
	*/
	public double[][] getDouble() throws JasymcaException{ 
		return getDouble(0,0);
	}
	

	/** Extract  Matrix column.
	@param k Column index, 1<=k<=ncol
	@return  Column.
	*/
	public Algebraic col( int k ){
		Algebraic c[][] = new Algebraic[a.length][1];
		for(int i=0; i<a.length; i++)
			c[i][0]=a[i][k-1];
		return new Matrix(c).reduce();
	}
		
	/** Extract  Matrix row.
	@param k Row index, 1<=k<=nrow
	@return  Row.
	*/
	public Algebraic row( int k ){
		Algebraic c[] = new Algebraic[a[0].length];
		for(int i=0; i<a[0].length; i++)
			c[i]=a[k-1][i];
		return new Vektor(c).reduce();
	}
	

	/** Insert  Matrix x at position
	specified by index idx. Adjust size if necessary.
	@param x The matrix to be inserted.
	@param idx The index specifier.
	*/
	public void insert(Matrix x, Index idx) throws JasymcaException{
		// Adjust size if necessary
		if(idx.row_max > nrow() || idx.col_max > ncol()){
			Matrix e = new Matrix( Math.max(idx.row_max,nrow()),
								   Math.max(idx.col_max,ncol()));
			for(int i=0; i<nrow(); i++)
				for(int k=0; k<ncol(); k++)
					e.a[i][k] = a[i][k];
			a = e.a;
		}
		if( x.nrow() == 1 && x.ncol() == 1 ){
			for(int i=0; i<idx.row.length; i++)
				for(int k=0; k<idx.col.length; k++)
					a[idx.row[i]-1][idx.col[k]-1] = x.a[0][0];
			return;
		}	
		if( x.nrow() == idx.row.length && x.ncol() == idx.col.length ){
			for(int i=0; i<idx.row.length; i++)
				for(int k=0; k<idx.col.length; k++)
					a[idx.row[i]-1][idx.col[k]-1] = x.a[i][k];
			return;
		}
		throw new JasymcaException("Wrong index dimension.");
	}
		
	/** Extract  Matrix x from position
	specified by index idx. 
	@param idx The index specifier.
	@return The extracted matrix.
	*/
	public Matrix extract(Index idx) throws JasymcaException{
		if(idx.row_max > nrow() || idx.col_max > ncol()){
			throw new JasymcaException("Index out of range.");
		}
		Matrix x = new Matrix( idx.row.length, idx.col.length );
		for(int i=0; i<idx.row.length; i++)
			for(int k=0; k<idx.col.length; k++)
				x.a[i][k] = a[idx.row[i]-1][idx.col[k]-1];
		return x;
	}


	/** Create n x 1  Matrix from Vektor
	with length n.
	@param x The Vektor.
	@return The column matrix.
	*/
	public static Matrix column(Vektor x) throws JasymcaException{
		return (new Matrix(x)).transpose();
	}
		
	/** Create 1 x n  Matrix from Vektor
	with length n.
	@param x The Vektor.
	@return The row matrix.
	*/
	public static Matrix row(Vektor x) throws JasymcaException{
		return new Matrix(x);
	}
		
	/** Conjugate complex of an algebraic object a+i*b.
	@return     The conjugate complex a-i*b.
	*/		
	public Algebraic cc() throws JasymcaException{
		Algebraic b[][] = new Algebraic[a.length][a[0].length];
		for(int i=0; i<a.length; i++)
			for(int k=0; k<a[0].length; k++)
				b[i][k] = a[i][k].cc();
		return new Matrix(b);
	}
	
	
	/** Add two algebraic objects. If x is an equalsized 
	matrix, perform matrixaddition, else add x to each
	component.
	@param x    Algebraic object to be added.
	@return     The sum this+x.
	*/
	public Algebraic add (Algebraic x) throws JasymcaException{
		if( x.scalarq() )
			x = x.promote( this );
		if( x instanceof Matrix && equalsized((Matrix)x )){
			Algebraic b[][] = new Algebraic[a.length][a[0].length];
			for(int i=0; i<a.length; i++)
				for(int k=0; k<a[0].length; k++)
					b[i][k] = a[i][k].add(((Matrix)x).a[i][k]);
			return new Matrix(b);
		}
		throw new JasymcaException("Wrong arguments for add:"+this+","+x);
	}

	/** Query: Is this algebraic object a scalar.
	@return     True if this object is a scalar, false otherwise.
	*/		
	public boolean scalarq(){ return false; }
	

	/** Query: Do these Matrices have equal dimensions.
	@return     True if x has the same dimensions, false otherwise.
	*/		
	public boolean equalsized( Matrix x ){
		return nrow()==x.nrow() && ncol()==x.ncol();
	}

	/** Multiply two algebraic objects. If x is an equalsized 
	matrix, multiply each component of x, else multiply x to each
	component.
	@param x    Algebraic object to be multiplied.
	@return     The product this*x.
	*/
	public Algebraic mult(Algebraic x) throws JasymcaException{
		if( x.scalarq() ){
			Algebraic b[][] = new Algebraic[a.length][a[0].length];
			for(int i=0; i<a.length; i++)
				for(int k=0; k<a[0].length; k++){
					b[i][k] = a[i][k].mult(x);
			}
			return new Matrix(b);
		}			
		Matrix xm = new Matrix(x);
		if( ncol() != xm.nrow() )
			throw new JasymcaException("Matrix dimensions wrong.");
		Algebraic b[][] = new Algebraic[a.length][xm.a[0].length];
		for(int i=0; i<a.length; i++)
			for(int k=0; k<xm.a[0].length; k++){
				b[i][k] = a[i][0].mult(xm.a[0][k]);
				for(int l=1; l<xm.a.length; l++)
					b[i][k] = b[i][k].add( a[i][l].mult(xm.a[l][k]) );
			}
		return new Matrix(b);
	}


	/** Divide two algebraic objects.
	@param x    The divisor algebraic object.
	@return     The quotient this/x.
	*/	
	public Algebraic div(Algebraic x) throws JasymcaException{
		if( x.scalarq() ){
			Algebraic b[][] = new Algebraic[a.length][a[0].length];
			for(int i=0; i<a.length; i++)
				for(int k=0; k<a[0].length; k++){
					b[i][k] = a[i][k].div(x);
			}
			return new Matrix(b);
		}
		return mult( (new Matrix(x)).pseudoinverse());
	}
	
	public static Matrix eye(int nr, int nc){
		Algebraic b[][] = new Algebraic[nr][nc];
		for(int i=0; i<nr; i++)
			for(int k=0; k<nc; k++)
				b[i][k] = (i==k ? Zahl.ONE : Zahl.ZERO);
		return new Matrix(b);
	}
		
	
	public Algebraic mpow(int n) throws JasymcaException{
		if(n==0){
			return Matrix.eye(a.length, a[0].length);
		}
		if(n==1)
			return this;
		if(n>1)
			return pow_n(n);  ;
		return (new Matrix(mpow(-n))).invert();
	}
			
		
	
	/** Reduce this algebraic object as much as possible, e.g.
	single element matrices are reduced to scalars etc.
	@return    This object reduced as much as possible..
	*/		
	public Algebraic reduce(){
		if(a.length == 1)
			return (new Vektor(a[0])).reduce();
		else
			return this;
	}


	
	
	/** Differentiate with respect to a variable.
	@param x    The variable.
	@return     The derivative.
	*/	
	public Algebraic deriv(Variable var) throws JasymcaException{
		Algebraic b[][] = new Algebraic[nrow()][ncol()];
		for(int i=0; i<a.length; i++)
			for(int k=0; k<a[0].length; k++)
				b[i][k] = a[i][k].deriv(var);
		return new Matrix(b);
	}
	
	/** Integrate with respect to a variable.
	@param x    The variable.
	@return     The integral.
	*/	
	public Algebraic integrate(Variable var) throws JasymcaException{
		Algebraic b[][] = new Algebraic[nrow()][ncol()];
		for(int i=0; i<a.length; i++)
			for(int k=0; k<a[0].length; k++)
				b[i][k] = a[i][k].integrate(var);
		return new Matrix(b);
	}
	
	/** Norm of an algebraic object. Always >= 0; 0 only if this==0.
	@return     The norm.
	*/	
	public double norm(){
		double n = 0.;
		for(int i=0; i<a.length; i++)
			for(int k=0; k<a[0].length; k++)
				n += a[i][k].norm();
		return n;
	}
	
	/** Query: Is this algebraic object konstant, i.e does not
	depend on any variable.
	@return     True if this object is constant, false otherwise.
	*/		
	public boolean constantq(){
		for(int i=0; i<a.length; i++)
			for(int k=0; k<a[0].length; k++)
				if(!a[i][k].constantq())
					return false;
		return true;
	}

	
	/** Query: Is this algebraic object equal to x.
	@param x    The object for comparison.
	@return     True if this object equals x, false otherwise.
	*/		
	public boolean equals(Object x){
		if(!(x instanceof Matrix) || !equalsized((Matrix)x) )
			return false;
		for(int i=0; i<a.length; i++)
			for(int k=0; k<a[0].length; k++)
				if(!a[i][k].equals(((Matrix)x).a[i][k]))
					return false;
		return true;
	}	
	
	/** Apply an algebraic function to this algebraic object.
	@param f    The algebraic function.
	@return     The function value.
	*/	
	public Algebraic map_lambda( LambdaAlgebraic f, Algebraic arg2 ) 
							  throws ParseException,JasymcaException{
		Algebraic[][] b = new Algebraic[a.length][a[0].length];
		if( arg2 instanceof Matrix && equalsized( (Matrix)arg2) ){
		// Binary operators: process components indivicually
			for(int i=0; i<a.length; i++){
				for(int k=0; k<a[0].length; k++){
					Algebraic c = ((Matrix)arg2).get(i,k);
					Object r = a[i][k].map_lambda( f, c );
					if( r instanceof Algebraic )
						b[i][k] = (Algebraic)r;
					else 
						throw new JasymcaException(
								"Cannot evaluate function to algebraic.");
				}
			}
		}else{
			for(int i=0; i<a.length; i++){
				for(int k=0; k<a[0].length; k++){
					Object r = a[i][k].map_lambda( f, arg2 );
					if( r instanceof Algebraic )
						b[i][k] = (Algebraic)r;
					else 
						throw new JasymcaException(
								"Cannot evaluate function to algebraic.");
				}
			}
		}
		return new Matrix( b );
	}
			
		/** The value of this algebraic expression, if variable var
	assumes the value of the algebraic expression v
	@param  var   the variable to substitute.
	@param  x     the value to substitute for var.
	*/
	public Algebraic value(Variable var, Algebraic x) throws JasymcaException{
		Algebraic[][] b = new Algebraic[a.length][a[0].length];
			for(int i=0; i<a.length; i++)
				for(int k=0; k<a[0].length; k++)
					b[i][k] = a[i][k].value(var,x);
		return new Matrix(b);
	}

	
	/** Create string representation of this matrix.
	@return    String representation of this matrix.
	*/								
	public String toString(){ 
		// Get maximum length of components
		int max = 0;
		String r = "";
		for(int i=0; i<a.length; i++){
			for(int k=0; k<a[0].length; k++){
				int l = StringFmt.compact(a[i][k].toString()).length();
				if(l > max)
					max = l;
			}
		}	
		max += 2;
		for(int i=0; i<a.length; i++){
			r += "\n  ";
			for(int k=0; k<a[0].length; k++){
				String c = StringFmt.compact(a[i][k].toString());
				r += c;
				for(int m=0; m<max-c.length(); m++)
					r += " ";
			}
		}
		return r;
	}
	
	
	public void print( PrintStream p ){
		// Get maximum length of components
		int max = 0;
		for(int i=0; i<a.length; i++){
			for(int k=0; k<a[0].length; k++){
				int l = StringFmt.compact(a[i][k].toString()).length();
				if(l > max)
					max = l;
			}
		}	
		max += 2;
		for(int i=0; i<a.length; i++){
			p.print( "\n  " );
			for(int k=0; k<a[0].length; k++){
				String r = StringFmt.compact(a[i][k].toString());
				p.print( r );
				for(int m=0; m<max-r.length(); m++)
					p.print(" ");
			}
		}
	}


	/** Apply an algebraic function to this algebraic object.
	@param f    The algebraic function.
	@return     The function value.
	*/	
	public Algebraic map( LambdaAlgebraic f ) throws JasymcaException{
		Algebraic cn[][] = new Algebraic[a.length][a[0].length];
		for(int i=0; i<a.length; i++)
			for(int k=0; k<a[0].length; k++)
				cn[i][k] = f.f_exakt(a[i][k]);
		return new Matrix(cn);
	}
	
	public Matrix transpose() throws JasymcaException{
		Algebraic b[][] = new Algebraic[a[0].length][a.length];
		for(int i=0; i<a.length; i++)
			for(int k=0; k<a[0].length; k++)
				b[k][i] = a[i][k];
		return new Matrix(b);
	}
		
	public Matrix adjunkt() throws JasymcaException{
		Algebraic b[][] = new Algebraic[a[0].length][a.length];
		for(int i=0; i<a.length; i++)
			for(int k=0; k<a[0].length; k++)
				b[k][i] = a[i][k].cc();
		return new Matrix(b);
	}
		
	public Matrix invert() throws JasymcaException{
		Algebraic det = det();
		if( det.equals( Zahl.ZERO ) )
			throw new JasymcaException("Matrix not invertible.");			
		Algebraic b[][] = new Algebraic[a.length][a.length];
		if( a.length==1 )
			b[0][0] = Zahl.ONE.div(det);
		else{
			for(int i=0; i<a.length; i++)
				for(int k=0; k<a[0].length; k++)
					b[i][k] = unterdet(k,i).div(det);
		}
		return new Matrix(b);
	}
	
	public Algebraic min() throws JasymcaException{
		Algebraic r[] = new Algebraic[ncol()];
		for(int i=0; i<ncol(); i++){
			Algebraic min = a[0][i];
			if(!(min instanceof Zahl))
				throw new JasymcaException("MIN requires constant arguments.");
			for(int k=1; k<nrow(); k++){
				Algebraic x = a[k][i];
				if(!(x instanceof Zahl))
				throw new JasymcaException("MIN requires constant arguments.");
				if( ((Zahl)x).smaller( (Zahl)min ))
					min = x;
			}
			r[i] = min;
		}
		return new Vektor(r).reduce();
	}
	
	public Algebraic max() throws JasymcaException{
		Algebraic r[] = new Algebraic[ncol()];
		for(int i=0; i<ncol(); i++){
			Algebraic max = a[0][i];
			if(!(max instanceof Zahl))
				throw new JasymcaException("MAX requires constant arguments.");
			for(int k=1; k<nrow(); k++){
				Algebraic x = a[k][i];
				if(!(x instanceof Zahl))
				throw new JasymcaException("MAX requires constant arguments.");
				if( ((Zahl)max).smaller( (Zahl)x ))
					max = x;
			}
			r[i] = max;
		}
		return new Vektor(r).reduce();
	}
				
	public Algebraic find() throws JasymcaException{
		Vector v = new Vector();
		for(int i=0; i<nrow(); i++){
			for(int k=0; k<ncol(); k++){
				if( !Zahl.ZERO.equals(a[i][k]) ){
					v.addElement( new Unexakt( i*nrow()+k+1.0 ) );
				}
			}
		}
		Vektor vx =  Vektor.create( v );
		if(nrow()==1)
			return vx;
		return  column(vx);
	}
		
				
	public Polynomial charpoly(Variable x) throws JasymcaException{
		Polynomial p = new Polynomial(x);
		Matrix m = (Matrix)(sub(Matrix.eye(a.length,a[0].length).mult(p)));
		p = (Polynomial)(m.det2());
		p = (Polynomial)p.rat();
		return p;
	}
		
	
	public Vektor eigenvalues() throws JasymcaException{
		Variable x = SimpleVariable.top; // new SimpleVariable("lambda");
		Polynomial p = charpoly(x);
		Algebraic ps[] = p.square_free_dec(p.var);
		Vektor r;
		Vector v = new Vector();
		for(int i=0; i<ps.length; i++){
			if(ps[i] instanceof Polynomial){
				r= ((Polynomial)ps[i]).monic().roots();
				for(int k=0; r != null && k<r.length() ; k++){
					for(int j=0; j<=i; j++)
						v.addElement(r.get(k));
				}
			}
		}
		return Vektor.create(v);
	}
		
	
	public Algebraic det() throws JasymcaException{
		if(a.length != a[0].length)
			return Zahl.ZERO;
		switch(a.length){
			case 1:
				return a[0][0];
			case 2:
				return a[0][0].mult(a[1][1]).sub(a[0][1].mult(a[1][0]));
			case 3:
				return a[0][0].mult(a[1][1]).mult(a[2][2]).add(
					   a[0][1].mult(a[1][2]).mult(a[2][0])).add(
					   a[0][2].mult(a[1][0]).mult(a[2][1])).sub(
					   a[0][2].mult(a[1][1]).mult(a[2][0])).sub(
					   a[0][0].mult(a[1][2]).mult(a[2][1])).sub(
					   a[0][1].mult(a[1][0]).mult(a[2][2]));
			default:	
				Matrix c = copy();
				int perm = c.rank_decompose(null,null);
				Algebraic r = c.get(0,0); 
				for(int i=1; i<c.nrow(); i++){
					r=r.mult(c.get(i,i));
				}
				return (perm%2  == 0 ? r : r.mult(Zahl.MINUS));
			/*	
				Algebraic d = unterdet(0,0).mult(a[0][0]);
				for(int i=1; i<a.length; i++){
					d = d.add(unterdet(i,0).mult(a[i][0]));
				} 
				return d;
			*/
				
		}
	}
	
	Algebraic det2() throws JasymcaException{
		if(a.length != a[0].length)
			return Zahl.ZERO;
		if(a.length < 4)
			return det();
		Algebraic d = unterdet(0,0).mult(a[0][0]);
			for(int i=1; i<a.length; i++){
				d = d.add(unterdet(i,0).mult(a[i][0]));
			}
		return d;
	}
			
			
	
	public Algebraic unterdet(int i, int k) throws JasymcaException{
		if(i<0 || i>a.length || k<0 || k>a[0].length )
			throw new JasymcaException("Operation not possible.");
		Algebraic b[][] = new Algebraic[a.length-1][a[0].length-1];
		int i1,i2,k1,k2;
		for(i1=0,i2=0; i1<a.length-1; i1++,i2++){
			if(i2==i) i2++;
			for(k1=0,k2=0; k1<a[0].length-1; k1++,k2++){
				if(k2==k) k2++;
				b[i1][k1] = this.a[i2][k2];
			}
		}
		Algebraic u = (new Matrix(b)).det2();
		if( (i+k)%2 == 0 )
			return u;
		return u.mult(Zahl.MINUS);		
	}

	
	
	// Routines for calculating pseudoinverses
	
	// Exchange rows starting at index k
	// so that largest column element is at k
	// return previous row number of k
	int pivot(int k) throws JasymcaException{
		if(k>=ncol()) return k;
		int pivot = k;
		double maxa = a[k][k].norm();
		for(int i=k+1; i<nrow(); i++){
			double dummy = a[i][k].norm();
			if(dummy>maxa){
				maxa=dummy;
				pivot=i;
			}
		}
		if( maxa == 0.0 ){ 
			int kn = pivot(k+1);
			if(kn==k+1)
				return k;
			else
				return kn;
		}
		if(pivot!=k){
			// exchange row(k) <-> row(pivot)
			for(int j=k;j<ncol();j++){
				Algebraic dummy = a[pivot][j];
				a[pivot][j] = a[k][j];
				a[k][j] = dummy;
			}
		}
		return pivot;
	}
	
	/** Query: Are all elements in this
	row zero.
	*/
	private boolean row_zero(int k){
		if( k >= nrow() )
			return true;
		for(int i=0; i<ncol(); i++){
			if(a[k][i] != Zahl.ZERO)
				return false;
		}
		return true;
	}
	
	/** Query: Is this algebraic object exact.
	@return     True if this object is exact, false otherwise.
	*/		
	public boolean exaktq(){ 
		boolean exakt = true;
		for(int i=0; i<a.length; i++)
			for(int k=0; k<a[0].length; k++)
				exakt = exakt && a[i][k].exaktq();
		return exakt;
	}
	

	
	/** Remove this row, adjust size.
	*/
	private void remove_row(int i){
		if( i>= nrow() ) return;
		Algebraic b[][] = new Algebraic[nrow()-1][];
		for(int k=0; k<i; k++)
			b[k] = a[k];
		for(int k=i+1; k<nrow(); k++)
			b[k-1] = a[k];
		a = b;
	}
	
	/** Remove this column, adjust size.
	*/
	void remove_col(int i){
		if( i>= ncol() ) return;
		Algebraic b[][] = new Algebraic[nrow()][ncol()-1];
		for(int j=0; j<nrow(); j++){
			for(int k=0; k<i; k++)
				b[j][k]   = a[j][k];
			for(int k=i+1; k<ncol(); k++)
				b[j][k-1] = a[j][k];
		}
		a = b;
	}
	
	// Create elementary matrix: subtract row k m-times from row i
	static Matrix elementary(int n, int i, int k, Algebraic m)throws JasymcaException{
		Matrix t = eye(n,n);
		t.a[i][k] = m;
		return t;
	}
	
	// exchange rows i and k
	static Matrix elementary(int n, int i, int k)throws JasymcaException{
		Matrix t = eye(n,n);
		t.a[k][k] = t.a[i][i] = Zahl.ZERO;
		t.a[i][k] = t.a[k][i] = Zahl.ONE;
		return t;
	}
	
	
	// rank decomposition of mxn Matrix A with rank r: 
	// calculates B (mxr) and C (rxn) such that A=BC	
	public int rank_decompose(Matrix B, Matrix P) throws JasymcaException{	
		// Gauss elimination
		int m = nrow(), n=ncol(), perm=0;
		// Vorwaertseliminierung
		Matrix C = eye(m,m);
		Matrix D = eye(m,m);
		for(int k=0; k<m-1; k++){
			int pivot = pivot(k);
			if(pivot != k){
				Matrix E = elementary(m,k,pivot);
				C = (Matrix)C.mult(E);
				D = (Matrix)D.mult(E);
				perm++;
			}
			// Find first nonvanishing element in row k starting from k
			int p = k;
			for(p=k; p<n; p++){
				if( !a[k][p].equals(Zahl.ZERO) ) break;
			}
			if( p<n ){
				for(int i=k+1; i<m; i++){
					if( !a[i][p].equals(Zahl.ZERO) ){
						Algebraic f = a[i][p].div(a[k][p]);
						a[i][p] = Zahl.ZERO;
						for(int j=p+1; j<n; j++){
							a[i][j] = a[i][j].sub(f.mult(a[k][j]));
						}
						C = (Matrix)C.mult(elementary(m,i,k,f));
					}
				}
			}
		}
		int nm = Math.max(n,m);
		for(int i=nm-1; i>=0; i--){
			if( row_zero(i) ){ // remove this row and corresponding column
				remove_row(i);
				C.remove_col(i);
			}
		}
		if(B!=null)
			B.a = C.a;
		if(P!=null)
			P.a = D.a;
		return perm;
	}
	
	public Matrix copy(){
		int nr = nrow(), nc = ncol();
		Algebraic[][] b = new Algebraic[nr][nc];
		for(int i=0; i<nr; i++)
			for(int k=0; k<nc; k++)
				b[i][k] = a[i][k];
		return new Matrix(b);
	}
	
	public Matrix pseudoinverse() throws JasymcaException{
		if( !det().equals(Zahl.ZERO) ){
			return invert();
		}

		Matrix c  = copy();
		Matrix b  = new Matrix(1,1);
		c.rank_decompose(b, null);
		
		int rank  = c.nrow();
		if( rank == nrow() ){  // full row rank
			Matrix ad = adjunkt();
			return (Matrix) ad.mult( ((Matrix)(mult(ad))).invert() );
		}else if( rank == ncol() ){ // full col rank
			Matrix ad = adjunkt();
			return (Matrix) ((Matrix)(ad.mult(this))).invert().mult( ad );
		}
		
		Matrix ca = c.adjunkt();
		Matrix ba = b.adjunkt();

		return (Matrix) ca.mult( ((Matrix)c.mult(ca)).invert() ).
		       mult( ((Matrix)ba.mult(b)).invert() ).mult(ba);
	}
}
