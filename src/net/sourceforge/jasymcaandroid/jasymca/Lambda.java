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
import java.math.BigInteger;

import android.util.Log;



// Definitions for function pointers


// Lambda is a general function pointer
public abstract class Lambda implements Constants{

	static Processor pc;
	static Parser    pr;
	static final boolean debug = true;
	static void p(String s) { if (debug) Log.d("Lambda", s); }



	static int length = 1;   // Number of outputvalues
	
	
	public int lambda(Stack x) throws ParseException, JasymcaException{ return 0;  }
	
	
	static Algebraic getAlgebraic( Stack st ) throws ParseException, JasymcaException{
		Object arg_in = st.pop();
		if( !(arg_in instanceof Algebraic) ){
			pc.process_instruction( arg_in, true ); 
			arg_in = st.pop();
		}
		if( !(arg_in instanceof Algebraic) )
			throw new JasymcaException("Expected algebraic, got: "+arg_in);
		return (Algebraic)arg_in;
	}	
	
	static Zahl getNumber(Stack st) throws ParseException, JasymcaException{
		Object arg = st.pop();
		if(arg instanceof Algebraic)
			arg = new ExpandConstants().f_exakt((Algebraic)arg);
		if(!(arg instanceof Zahl) )
			throw new ParseException("Expected number, got "+arg);
		return (Zahl)arg;
	}

	
	static int getNarg( Stack<?> st ) throws ParseException, JasymcaException{
		Object arg_in = st.pop();
		if( !(arg_in instanceof Integer) )
			throw new JasymcaException("Expected Integer, got: "+arg_in);
		return ((Integer)arg_in).intValue();
	}	
	
	static String getSymbol( Stack<?> st ) throws ParseException, JasymcaException{
		Object arg_in = st.pop();
		if( !(arg_in instanceof String) ||
			((String)arg_in).length()==0 ||
			((String)arg_in).charAt(0)==' ')
			throw new JasymcaException("Expected Symbol, got: "+arg_in);
		return (String)arg_in;
	}	
	
	static Polynomial getPolynomial(Stack<?> st) throws ParseException, JasymcaException{
		Object arg = getAlgebraic( st );
		if(!(arg instanceof Polynomial) )
			throw new ParseException("Expected polynomial, got "+arg);
		return (Polynomial)arg;
	}
	
	static Vektor getVektor(Stack<?> st) throws ParseException, JasymcaException{
		Object arg = st.pop();
		if(!(arg instanceof Vektor) )
			throw new ParseException("Expected vector, got "+arg);
		return (Vektor)arg;
	}
	
	static Variable getVariable(Stack<?> st) throws ParseException, JasymcaException{
		Polynomial  p = getPolynomial(st);
		return p.var;
	}

	static int getInteger(Stack<?> st) throws ParseException, JasymcaException{
		Object arg = st.pop();
		if(!(arg instanceof Zahl) || !((Zahl)arg).integerq() )
			throw new ParseException("Expected integer, got "+arg);
		return ((Zahl)arg).intval();
	}

	static int getInteger(Algebraic arg) throws ParseException, JasymcaException{
		if(!(arg instanceof Zahl) || !((Zahl)arg).integerq() )
			throw new ParseException("Expected integer, got "+arg);
		return ((Zahl)arg).intval();
	}
	
	static List<?> getList(Stack<?> st) throws ParseException, JasymcaException{
		Object arg = st.pop();
		if(!(arg instanceof List) )
			throw new ParseException("Expected list, got "+arg);
		return (List<?>)arg;
	}
	
	
	static Zahl ensure_Zahl( Object x )
							throws JasymcaException{
		if( !(x instanceof Zahl) )
			throw new JasymcaException("Expected number, got "+x);
		return (Zahl)x;
	}

	static Environment sandbox = null;							
	// Insert x in rule "f(x)" and evaluate expression to Algebraic
	static Algebraic evalx(String rule, Algebraic x) throws JasymcaException{
		try{
			List pgm = pr.compile( rule );
			Environment global = pc.getEnvironment();
			if( sandbox == null ){
				sandbox = new Environment();
				sandbox.putValue( "x", new Polynomial(new SimpleVariable("x")));
				sandbox.putValue( "X", new Polynomial(new SimpleVariable("X")));
				sandbox.putValue( "a", new Polynomial(new SimpleVariable("a")));
				sandbox.putValue( "b", new Polynomial(new SimpleVariable("b")));
				sandbox.putValue( "c", new Polynomial(new SimpleVariable("c")));
			}
			pc.setEnvironment( sandbox );
			pc.process_list( pgm, true );
			pc.setEnvironment( global );
			Algebraic y = getAlgebraic( pc.stack );
			y = y.value( new SimpleVariable("x"), x );
			return y;
		}catch(Exception e){
			throw new JasymcaException("Could not evaluate expression "+rule+": "+e.toString());
		}
	}

}

// LambdaAlgebraic has functions for Algebraic variables
// This class is used for numeric functions (sin,exp, etc)
// for operators (+,-,..) and for many internal functions
abstract class LambdaAlgebraic extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{
		int narg = getNarg( st );
		switch(narg) {
			case 0: 
				throw new JasymcaException("Lambda functions expect argument.");
			case 1: 
				Algebraic arg = getAlgebraic( st );
				st.push( arg.map_lambda( this, null ) );
				break;
			case 2: 
				Algebraic arg2 = getAlgebraic( st );
				Algebraic arg1 = getAlgebraic( st );
				arg1 = arg1.promote( arg2 );
				st.push( arg1.map_lambda( this, arg2 ));
				break;
			default:
				Algebraic args[] = new Algebraic[narg];
				for(int i=narg-1; i>=0; i--){
					args[i] = getAlgebraic( st );
				}
				st.push( f_exakt(args) );
				break;
		}
		return 0;
	}
		
	
	/* Conveniance function for single variate functions
	*/
	Zahl f(Zahl x) throws JasymcaException{ return x;}
	
	Algebraic f_exakt(Algebraic x)              throws JasymcaException{ return null;}
	Algebraic f_exakt(Algebraic x, Algebraic y) throws JasymcaException{ return null;}
	Algebraic f_exakt(Algebraic[] x)            throws JasymcaException{ return null;}

	String diffrule=null, intrule=null, trigrule = null;
	
	public Algebraic integrate(Algebraic arg, Variable x) throws JasymcaException{ 
		if(!(arg.depends(x)))
			throw new JasymcaException("Expression in function does not depend on Variable.");
		if( !(arg instanceof Polynomial) || ((Polynomial)arg).degree()!=1 ||
			!((Polynomial)arg).ratfunc(x) || intrule==null)
			throw new JasymcaException("Can not integrate function ");
		try{
			Algebraic y = evalx( intrule, arg );
			// * dx/dz <----> /a[1]
			return y.div(((Polynomial)arg).a[1]);
		}catch(Exception e){
				throw new JasymcaException("Error integrating function");
		}
	}
				
}


// Define Function. Example f(x):=3*x^2+2;
// Functions are stored as evaluated (compiled) Algebraics 
// Functions can be differentiated, integrated or used in symbolic equations
class LambdaFUNC extends Lambda{
	public int lambda(Stack st) throws ParseException,JasymcaException{ 
		int narg = getNarg( st );
		if(narg!=3)
			throw new ParseException("Wrong function definition.");
//p("func:"+x);
	
		List ret		= getList( st );
		List prot		= getList( st );
		if( prot.size() < 1 || !(prot.get(prot.size()-1) instanceof String) )
			throw new ParseException("Wrong function definition.");
		String fname  	= ((String)prot.get(prot.size()-1)).substring(1);
		List vars_in	= prot.subList(0, prot.size()-1);
		List code_in	= getList( st );

		// 1. in variable names
		SimpleVariable vars[] = null;
		if( vars_in.size() != 0 ){
			int fnarg = ((Integer)vars_in.get( vars_in.size()-1 )).intValue();
			vars = new SimpleVariable[ fnarg ];
			for(int i=0; i<vars.length; i++){
				vars[i] = new SimpleVariable( (String)vars_in.get(vars.length-i-1) );
			}
		}		
		SimpleVariable result = new SimpleVariable( ((String)ret.get(0)).substring(1));

		Lambda  func	 = null; 
		Environment env  = new Environment();
		Stack ups		 = new Stack();

		// Add variables to environment
		for(int i=0; i<vars.length; i++){
			env.putValue( vars[i].name, new Polynomial( vars[i] ) );
//				vars[i] = new SimpleVariable( (String)vars_in.get(i) );
		}

		Object y = null;	
		if( vars.length==1 ){
			int res = UserProgram.process_block( code_in, ups, env, true );
			if( res != Processor.ERROR ){
				y = env.getValue( result.name );
			}
		}
		
		if( y instanceof Algebraic ){
			func = new UserFunction( fname, vars, (Algebraic)y, result, env );
		}else{
			func = new UserProgram( fname, vars, code_in, result, env, ups );
		}
		pc.env.putValue(fname, func);
		st.push( func );
		return 0;
	}
}

// User defined functions 
class UserProgram extends Lambda{
	String 	fname;
	List body;
	SimpleVariable[] var;
	SimpleVariable   result;
	Environment env=null;
	Stack ups= null;

	public UserProgram() throws ParseException,JasymcaException {}
	
	public UserProgram(  String fname, 
						 SimpleVariable var[], 
						 List body,
						 SimpleVariable result,
						 Environment env,
						 Stack ups){
		this.fname = fname;
		this.var   = var;
		this.body  = body;
		this.result= result;
		this.env   = env;
		this.ups   = ups;
	}

	public int lambda(Stack st) throws ParseException,JasymcaException{ 
		int narg = getNarg( st );
		if( var.length != narg )
			throw new JasymcaException(fname+" requires "+var.length+" Arguments.");
		// Evaluate args in global environment
		// and store values in local environment
		for(int i=0; i<var.length; i++){
			Object a = st.pop();
			env.putValue( var[i].name, a );
		}
		// process private code in sandbox
		int ret = process_block( body, ups, env, result!=null );
		if( ret != Processor.ERROR ){
			Object y = (result!=null?env.getValue( result.name ):ups.pop());
			if( y instanceof Algebraic && result!=null)
				((Algebraic)y).name = result.name;
			if( y!= null )
				st.push(y);
		}
		return 0;
	}
	
	static int process_block( List code, Stack st, Environment env, boolean clear_stack){
//System.out.println("Block: "+code+"\n"+st+"\n"+env);
		Environment global = pc.getEnvironment();
		Stack old_stack = pc.stack;
		pc.setEnvironment( env );
		pc.stack = st;
		int ret;
		try{
			ret = pc.process_list( code, true );
		}catch( Exception e ){
			ret = Processor.ERROR;
		}
		pc.stack = old_stack;
		pc.setEnvironment( global );
		if( clear_stack ){
			//st.clear();
			while( !st.empty() )
				st.pop();
		}
		return ret;
	}
	
	
}


// User defined functions 
class UserFunction extends LambdaAlgebraic{
	String 	fname;
	Algebraic body;
	SimpleVariable[] var;
	SimpleVariable   result;
	Environment env=null;

	public UserFunction() throws ParseException,JasymcaException {}
	
	public UserFunction( String fname, 
						 SimpleVariable var[], 
						 Algebraic body,
						 SimpleVariable result,
						 Environment env){
		this.fname = fname;
		this.var   = var;
		this.body  = body;
		this.result= result;
		this.env   = env;
	}

	Zahl f( Zahl x ) throws JasymcaException{ 
		Algebraic y=f_exakt(x);
		if(y instanceof Zahl)
				return (Zahl)y;
		y = (new ExpandConstants()).f_exakt(y);
		if(y instanceof Zahl)
				return (Zahl)y;
		throw new JasymcaException("Can not evaluate Function "+fname+
				" to number, got "+y+" for "+x);
	}

	Algebraic f_exakt(Algebraic x) throws JasymcaException{ 
		if(var.length != 1)
			throw new JasymcaException("Wrong number of arguments.");
		Algebraic y = body.value(var[0], x);
		return y;
	}

	Algebraic f_exakt(Algebraic x, Algebraic y) throws JasymcaException{ 
		if(var.length != 2)
			throw new JasymcaException("Wrong number of arguments.");
		Algebraic z = body.value(var[0], y);
		z = z.value(var[1], x);
		return z;
	}

	Algebraic f_exakt(Algebraic[] x) throws JasymcaException{ 
		if(var.length != x.length)
			throw new JasymcaException("Wrong number of arguments.");
		Algebraic y = body;
		for( int i=0; i<x.length; i++)
			y = y.value(var[x.length-i-1], x[i]);
		return y;
	}

	

	// todo: eliminate
	Algebraic fv(Vektor x) throws JasymcaException{
		Environment global = pc.env;
		pc.env = env;
		Algebraic r = body;
		pc.env = global;
		for(int i=0; i<var.length; i++){
			r = r.value(var[i], x.get(i));
		}
		return r;			 
	}

	public Algebraic integrate(Algebraic arg, Variable x) throws JasymcaException{ 
		if( !(body instanceof Algebraic) ){
			throw new JasymcaException("Can not integrate function "+fname);			
		}
		if(!(arg.depends(x)))
			throw new JasymcaException("Expression in function does not depend on Variable.");
		if(var.length == 1)
			return ((Algebraic)body).value(var[0], arg).integrate(x);
		if(arg instanceof Vektor && ((Vektor)arg).length()==var.length)
			return fv((Vektor)arg).integrate(x);
		throw new JasymcaException("Wrong argument to function "+fname);
	}
	
		
}
		




// Convert to floating point number as much as 
// possible, use ALGEPSILON to reduce numbers
class LambdaFLOAT extends LambdaAlgebraic{
	double eps = 1.e-8;
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int   narg = getNarg( st );
		Algebraic exp = getAlgebraic(st);
		Zahl a = pc.env.getnum("algepsilon");
		if(a !=null){
			double epstry = a.unexakt().real;
			if(epstry>0) eps = epstry;
		}
		// Eval constants to numbers (pi etc)
		exp = new ExpandConstants().f_exakt(exp);
		st.push(exp.map(this));
		return 0;
	}
	Zahl f(Zahl x) throws JasymcaException{ 
		Unexakt f = x.unexakt();
		if(f.equals(Zahl.ZERO))
			return f;
		double abs = ((Unexakt)f.abs()).real;
		double r = f.real;
		if( Math.abs(r/abs)<eps ) r = 0.;
		double i = f.imag;
		if( Math.abs(i/abs)<eps ) i = 0.;
		return new Unexakt(r,i);
	}

	Algebraic f_exakt(Algebraic x) throws JasymcaException{ 
		return x.map(this);
	}
}

class LambdaMATRIX extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int   narg = getNarg( st );
		Algebraic[][]a = new Algebraic[narg][];
		for(int i=0; i<narg; i++){
			Algebraic b = getAlgebraic(st);
			if( b instanceof Vektor )
				a[i] = ((Vektor)b).get();
			else{
				a[i] = new Algebraic[1];
				a[i][0] = b;
			}
			if( a[i].length != a[0].length )
				throw new JasymcaException(
					"Matrix rows must have equal length.");
		}
		st.push( new Matrix(a).reduce() );
		return 0;
	}
}

class LambdaFORMAT extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int nargs = getNarg( st );
		if(nargs==1){
			Object arg  = st.pop();
			if( "$short".equals( arg.toString() )) {
				Jasymca.fmt = new NumFmtVar( 10, 5 );
				return 0;
			} else if( "$long".equals( arg.toString() )) {
				Jasymca.fmt = new NumFmtJava();
				return 0;
			}
			throw new JasymcaException("Usage: format long | short | base significant");
		}else if(nargs==2){
			int base  = getInteger( st );
			int nsign = getInteger( st );
			if(base<2 || nsign<1)
				throw new JasymcaException("Invalid variables.");
			Jasymca.fmt = new NumFmtVar( base, nsign );
			return 0;
		}
		throw new JasymcaException("Usage: format long | short | base significant");
	}
}

class LambdaSYMS extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int nargs = getNarg( st );
		while( nargs-- > 0 ){
			Object arg  = st.pop();
			if( arg instanceof String){
				String s = ((String)arg).substring(1);
				pc.env.putValue( s, new Polynomial( new SimpleVariable(s )));
			}
		}
		return 0;
	}
}

class LambdaCLEAR extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int nargs = getNarg( st );
		while( nargs-- > 0 ){
			Object arg  = st.pop();
			if( arg instanceof String){
				String s = ((String)arg).substring(1);
				pc.env.remove(s);
			}
		}
		return 0;
	}
}

				
	

// Operator for '[' symbol
class CreateVector extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		// if arg is a list of scalars or vektors of scalars:
		// create vector
		// if arg is a list of vectors of vectors, create matrix
		// if arg is a list of matrices, create matrix
		int nr = getNarg( st );
		int nrow = 1, ncol = 1;

		Matrix m = new Matrix(nr,1);
		while(nr-- > 0){
			Algebraic row = crv( st ); 			// insert row vector												
			Index idx = new Index( nrow, ncol, row);
			m.insert( new Matrix(row), idx);
			nrow = idx.row_max+1;
		}
		st.push( m.reduce() );
		return 0;
	}
	
	static Algebraic crv( Stack st ) throws ParseException, JasymcaException{ 
		int nc = getNarg( st );
		if(nc == 1)
			return getAlgebraic( st );
		Matrix m = new Matrix(1,nc);
		int nrow = 1, ncol = 1;
		while( nc-- > 0 ){
			Algebraic x = getAlgebraic( st );
			Index idx = new Index( nrow, ncol, x);
			m.insert( new Matrix(x), idx);
			ncol = idx.col_max+1;
		}
		return m.reduce();
	}
	
	
}


// Operator for 'a:b:c' symbol
class CR1 extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		// Variable definition, store in Prefix format and return success
		int narg = getNarg( st );


		Algebraic a,b,c = getAlgebraic(st); 
		if( narg==2 ){
			b = Zahl.ONE;
			a = getAlgebraic(st); 
		}else{
			b = getAlgebraic(st); 
			a = getAlgebraic(st); 
		}
		Algebraic na  = c.sub(a).div(b);
		if(!(na instanceof Zahl)){
			na = (new ExpandConstants()).f_exakt(na);
		}
		if(!(na instanceof Zahl))
			throw new ParseException("CreateVector requires numbers.");
		int n = (int)( ((Zahl)na).unexakt().real+1.0);
		Algebraic[] coord = new Algebraic[n];
		for(int i=0; i<n; i++)
			coord[i] = a.add(b.mult(new Unexakt((double)i)));  
		st.push( new Vektor(coord) );
		return 0;
	}
}

class LambdaEYE extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		// Variable definition, store in Prefix format and return success
		int narg = getNarg( st );
		if(narg<1)
			throw new JasymcaException("Usage: EYE( nrow, ncol ).");
		int nrow = getInteger( st );
		int ncol = nrow;
		if(narg>1)
			ncol = getInteger( st );
		st.push( Matrix.eye(nrow, ncol).reduce() );
		return 0;
	}
}
		
class LambdaZEROS extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		// Variable definition, store in Prefix format and return success
		int narg = getNarg( st );
		if(narg<1)
			throw new JasymcaException("Usage: ZEROS( nrow, ncol ).");
		int nrow = getInteger( st );
		int ncol = nrow;
		if(narg>1)
			ncol = getInteger( st );
		st.push( new Matrix(nrow, ncol).reduce() );
		return 0;
	}
}
		
class LambdaONES extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		// Variable definition, store in Prefix format and return success
		int narg = getNarg( st );
		if(narg<1)
			throw new JasymcaException("Usage: ONES( nrow, ncol ).");
		int nrow = getInteger( st );
		int ncol = nrow;
		if(narg>1)
			ncol = getInteger( st );
		st.push( new Matrix(Zahl.ONE, nrow, ncol).reduce() );
		return 0;
	}
}
		
class LambdaRAND extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		// Variable definition, store in Prefix format and return success
		int narg = getNarg( st );
		if(narg<1)
			throw new JasymcaException("Usage: RAND( nrow, ncol ).");
		int nrow = getInteger( st );
		int ncol = nrow;
		if(narg>1)
			ncol = getInteger( st );
		Algebraic a[][] = new Algebraic[nrow][ncol];
		for(int i=0; i<nrow; i++)
			for(int k=0; k<ncol; k++)
				a[i][k] = new Unexakt( Math.random());
		st.push( new Matrix(a).reduce() );
		return 0;
	}
}
		
class LambdaDIAG extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		// Variable definition, store in Prefix format and return success
		int narg = getNarg( st );
		if(narg<1)
			throw new JasymcaException("Usage: DIAG( matrix, k ).");
		Algebraic x = getAlgebraic( st ).reduce();
		int k = 0;
		if(narg>1)
			k = getInteger( st );
		if( x.scalarq() ){
			x = new Vektor(new Algebraic[] {x});
		}
		if( x instanceof Vektor ){ 			// Create Matrix
			Vektor xv = (Vektor)x;
			if(k>=0){
				Matrix m = new Matrix(xv.length()+k, xv.length()+k);
				for(int i=0; i<xv.length(); i++){
					m.set(i, i+k, xv.get(i));
				}
				st.push( m );
			}else{
				Matrix m = new Matrix(xv.length()-k, xv.length()-k);
				for(int i=0; i<xv.length(); i++){
					m.set(i-k, i, xv.get(i));
				}
				st.push( m );
			}
		}else if (x instanceof Matrix ){ 	// Extract vector
			Matrix xm = (Matrix)x;
			if(k>=0 && k<xm.ncol()){
				Algebraic a[] = new Algebraic[xm.ncol()-k];
				for(int i=0; i<a.length; i++){
					a[i] = xm.get(i, i+k);
				}
				st.push(new Vektor(a));
			}else if(k<0 && (-k)<xm.nrow()){
				Algebraic a[] = new Algebraic[xm.nrow()+k];
				for(int i=0; i<a.length; i++){
					a[i] = xm.get(i-k, i);
				}
				st.push(new Vektor(a));
			}else
				throw new JasymcaException("Argument k to DIAG out of range.");
				
		}else
			throw new JasymcaException("Argument to DIAG must be vector or matrix.");
		return 0;
	}
}
		



// GCD for numbers and polynomials
class LambdaGCD extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		// Variable definition, store in Prefix format and return success
		int   narg = getNarg( st );
		if(narg<2)
			throw new ParseException("GCD requires at least 2 arguments.");
		Algebraic gcd = getAlgebraic(st);
		for(int i=1; i<narg; i++){
			gcd = gcd(gcd,  getAlgebraic(st));
		}
		st.push( gcd );
		return 0;
	}
	
	Algebraic gcd(Algebraic x, Algebraic y) throws JasymcaException{ 
		if(!x.exaktq())
			x = new LambdaRAT().f_exakt(x);
		if(!y.exaktq())
			y = new LambdaRAT().f_exakt(y);
		if(x instanceof Zahl && y instanceof Zahl){
			return  ((Zahl)x).gcd((Zahl)y);
		}
		if(x instanceof Polynomial){
			Zahl gcd_x = ((Polynomial)x).gcd_coeff();
			if(y instanceof Polynomial){ // poly_gcd * coef_gcd
				Zahl gcd_y = ((Polynomial)y).gcd_coeff();
				return Poly.poly_gcd(x,y).mult(gcd_x.gcd(gcd_y));
			}if(y instanceof Zahl){
				return gcd_x.gcd( (Zahl)y );
			}
		}
		if(y instanceof Polynomial && x instanceof Zahl)
			return gcd(y,x);
		throw new JasymcaException("Not implemented.");
	}
}
	
class LambdaEXPAND extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{
		int narg = getNarg( st );
		Object x = st.pop();
		if(x instanceof List){
			pc.process_list((List)x, true);
			x = pc.stack.pop();
		}
		if(x instanceof Algebraic)
			x = new SqrtExpand().f_exakt( (Algebraic) x );
		st.push( x ); 
		return 0;
	}
}
	
class LambdaREALPART extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int   narg = getNarg( st );
		st.push( getAlgebraic(st).realpart()); 
		return 0;
	}
}
	
class LambdaIMAGPART extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int   narg = getNarg( st );
		st.push( getAlgebraic(st).imagpart()); 
		return 0;
	}
}

class LambdaCONJ extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int   narg = getNarg( st );
		st.push( getAlgebraic(st).cc()); 
		return 0;
	}
}
	
class LambdaANGLE extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int   narg = getNarg( st );
		Algebraic x = getAlgebraic(st); 
		Object atan2 = pc.env.getValue("atan2");
		if( !(atan2 instanceof LambdaAlgebraic) ){
			throw new JasymcaException("Function ATAN2 not installed.");
		}
		st.push( ((LambdaAlgebraic)atan2).f_exakt( x.imagpart(), x.realpart() ));
		return 0;
	}
}



	

// Continued fraction expansion
class LambdaCFS extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int   narg = getNarg( st );
		Algebraic y = getAlgebraic(st).rat(); 
		if(!(y instanceof Exakt))
			throw new ParseException("Argument must be exact number");
		double eps = 1.e-5;
		if(narg>1)
			eps = getNumber(st).unexakt().real;
		st.push(((Exakt)y).cfs(eps));	
		return 0;
	}
}
	
class LambdaDIFF extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int   narg = getNarg( st );
		if(narg==0)
			throw new ParseException("Argument to diff missing.");
		Algebraic f = getAlgebraic(st); // Function to differentiate
		Variable v;
		if(narg>1){
			v = getVariable(st);
		}else{
			if( f instanceof Polynomial )
				v = ((Polynomial)f).var;
			else if(f instanceof Rational )
				v = ((Rational)f).den.var;
			else
				throw new ParseException("Could not determine Variable.");
		}
		Algebraic df = f.deriv(v);
		if(df instanceof Rational && !df.exaktq())
			df = new LambdaRAT().f_exakt(df);
		st.push( df );
		return 0;
	}
}

// SUBST (a, b, c), substitutes a for b in c
// if c evaluates to a prefix expressions, substitute using parser
// if c evaluates to Algebraic and b is SimpleVariable, substitute using value()
// if c evaluates to Algebraic and b is not SimpleVariable, 
// convert c and b to prefix and substitute using parser
class LambdaSUBST extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int   narg = getNarg( st );
		if(narg!=3)
			throw new ParseException("Usage: SUBST (a, b, c), substitutes a for b in c");
		Algebraic  a = getAlgebraic(st);
		Polynomial b = getPolynomial(st);
		Algebraic  c = getAlgebraic(st);
		
		// solve a = b for b's variable
		Variable bx 	= b.var;
		while(bx instanceof FunctionVariable){
			Algebraic arg = ((FunctionVariable)bx).arg;
			if( !(arg instanceof Polynomial) ){
				throw new JasymcaException("Can not solve "+b+" for a variable.");
			}
			bx = ((Polynomial)arg).var;
		}
		Vektor  sol 	= LambdaSOLVE.solve( a.sub(b), bx);

		Algebraic res[] = new Algebraic[sol.length()];		
		for(int i=0; i<sol.length(); i++){
			Algebraic y = sol.get(i);
			res[i] 		= c.value(bx, y);
		}
		st.push( new Vektor(res).reduce() );
		return 0;
	}
}

// COEFF
// coeff(a,b,c) Koeffizienten von b der Potenz C in Ausdruck a.
class LambdaCOEFF extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int   narg = getNarg( st );
		if(narg!=3)
			throw new ParseException("Usage: COEFF (a, b, c), find coeff of b^c in a");
		Polynomial a = getPolynomial(st);
		Variable   b = getVariable(st);
		Algebraic c_in = getAlgebraic(st);
		if(c_in.scalarq())
			st.push( a.coefficient( b, getInteger( c_in ) ));
		else if(c_in instanceof Vektor){
			Vektor c = (Vektor)c_in;
			Algebraic v[] = new Algebraic[c.length()];
			for(int i=0; i<v.length; i++){
				v[i] = a.coefficient( b, getInteger( c.get(i) ));
			}
			st.push( new Vektor(v));
		}else
			throw new ParseException("Usage: COEFF (a, b, c), find coeff of b^c in a");
		return 0;
	}
}



// SUM (exp, ind, lo, hi)
class LambdaSUM extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int   narg = getNarg( st );
		if( narg == 1){  // Matlab Version: sum over Vector/Matrix
			Algebraic x = getAlgebraic( st );
			if( x.scalarq() && !x.constantq() )
				throw new JasymcaException("Unknown variable dimension: "+x);
			Matrix m = new Matrix(x);
			boolean addcols = (m.ncol() > 1);
			if( narg > 1 ){
				if( getInteger(st)==2 )
						addcols = false;
			}
			if( addcols ){
				Algebraic s = m.col(1);
				for(int i=2; i<=m.ncol(); i++)
					s = s.add(m.col(i));
				st.push( s );
			}else{
				Algebraic s = m.row(1);
				for(int i=2; i<=m.nrow(); i++)
					s = s.add(m.row(i));
				st.push( s );
			}
			return 0;
		}
		if(narg!=4)
			throw new ParseException("Usage: SUM (exp, ind, lo, hi)");
		Algebraic exp = getAlgebraic(st);
		Variable  v   = getVariable (st);
		int        lo = getInteger  (st);
		int        hi = getInteger  (st);
		Algebraic sum = Zahl.ZERO;
		for( ; lo <= hi; lo++)		
			sum = sum.add(exp.value(v,new Unexakt((double)lo)));
		st.push( sum );
		return 0;
	}
}

// LSUM (exp, ind, list)
class LambdaLSUM extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int   narg = getNarg( st );
		if(narg!=3)
			throw new ParseException("Usage: LSUM (exp, ind, list)");
		Algebraic exp = getAlgebraic(st);
		Variable  v   = getVariable (st);
		Vektor list   = getVektor(st);
		Algebraic sum = Zahl.ZERO;
		for( int i=0; i<list.length(); i++)		
			sum = sum.add(exp.value(v,list.get(i)));
		st.push( sum );	
		return 0;
	}
}



// Divide and remainder of two polynomials
class LambdaDIVIDE extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int   size = getNarg( st );
		if(size!=3 && size!=2)
			throw new ParseException("Usage: DIVIDE (p1, p2, var)");
		Algebraic p1 = getAlgebraic(st);
		if(!p1.exaktq())
			p1 = new LambdaRAT().f_exakt(p1);
		Algebraic p2 = getAlgebraic(st);
		if(!p2.exaktq())
			p2 = new LambdaRAT().f_exakt(p2);
		Algebraic a[] = { p1, p2 };
		if(size==3){
			Variable v = getVariable(st);
			Poly.polydiv( a, v);
		}else{
			if(p1 instanceof Zahl && p2 instanceof Zahl)
				a = ((Zahl)p1).div( p2, a) ;
			else{
				a[0] = Poly.polydiv( p1, p2 );
				a[1] = p1.sub(a[0].mult(p2));
			}
		}
		st.push(new Vektor(a));
		return 0;
	}
}


// TAYLOR (exp, var, pt, pow)
class LambdaTAYLOR extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int   narg = getNarg( st );
		if(narg!=4)
			throw new ParseException("Usage: TAYLOR (exp, var, pt, pow)");
		Algebraic exp = getAlgebraic(st);
		Variable  v   = getVariable(st);
		Algebraic pt  = getAlgebraic(st);
		int       n   = getInteger(st);

		Algebraic r = exp.value(v, pt);
		Algebraic t = new Polynomial(v).sub(pt);
		double nf = 1.0;
		for(int i=1; i<=n; i++){
			exp = exp.deriv(v);
			nf *= i; // Fakultaet
			r = r.add(exp.value(v,pt).mult(t.pow_n(i)).div(new Unexakt(nf)));
		}
		st.push( r );
		return 0;
	}
}


// SAVE (filename,arg1, arg2,...,argi)
// arg = all saves everything except functions
class LambdaSAVE extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int   size = getNarg( st );
		if(size<2)
			throw new ParseException("Usage: SAVE (filename,arg1, arg2,...,argi)");
		String filename = st.pop().toString().substring(1);
		try{
			OutputStream f = Jasymca.getFileOutputStream( (String)filename, true);
			for(int i=1; i<size; i++){
				String var = st.pop().toString().substring(1);
				if("ALL".equalsIgnoreCase(var)){
					Enumeration en = pc.env.keys();
					while(en.hasMoreElements()){
						Object key = en.nextElement();
						if(!"pi".equalsIgnoreCase((String)key)){ // Would be reread as variable
							Object val = pc.env.getValue((String)key);
							if(!(val instanceof Lambda)){
								String line = key.toString()+":"+val.toString()+";\n";
								f.write(line.getBytes());
							}
						}
					}
				}else{
					Object val  = pc.env.getValue(var);
					String line = var.toString()+":"+val.toString()+";\n";
					f.write(line.getBytes());
				}
			}f.close();
			System.out.println( "Wrote variables to "+filename );
		}catch(Exception e){
			throw new JasymcaException("Could not write to "+filename+" :"+e.toString());
		}
		return 0;
	}
}

//  LOADFILE (filename) 
class LambdaLOADFILE extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int   narg = getNarg( st );
		if(narg!=1)
			throw new ParseException("Usage: LOADFILE (filename)");
		Object filename = st.pop();
		if(!pr.stringq(filename))
			throw new JasymcaException( filename+" not a valid filename.");			
		try{
// Bug in original code Wimvvv: skip 1st char (= type)
			readFile( ((String)filename).substring(1) );
			System.out.println( "Loaded Variables from "+filename);
		}catch(Exception e){
			throw new JasymcaException("Could not read from "+filename+" :"+e.toString());
		}
		return 0;
	}
	
		// search for file in path
	// read file and feed through evalloop
	@SuppressWarnings("unchecked")
	public static void readFile( String fname ) 
				throws IOException, ParseException, JasymcaException{ 
		String sep="/";
		String s;
		Class c = LambdaLOADFILE.class;

		for(int i=0; i<pc.env.path.size(); i++){
			String dir = (String)pc.env.path.elementAt(i);
			s = fname.startsWith( sep ) ? dir+fname : dir+sep+fname;
			InputStream f = c.getResourceAsStream(s);
			if( f == null ){
				try{
					f = Jasymca.getFileInputStream( s );
				}catch(Exception e){
					continue;
				}
			}
			if( f== null )
				continue;
			readFile( f );
			return;
		}
		throw new IOException("Could not open "+fname+".");
	}
	
	public static void readFile( InputStream f ) throws JasymcaException{
		Stack old_stack = pc.stack;
		pc.stack = new Stack();
		try{
			while(true){
				List code   = pr.compile(f,null);   // Convert to prefix notation
				if( code 	== null  ) break;       // EOF
				pc.process_list( code, true );
			}
			f.close();
			pc.stack = old_stack;
			return;
		}catch(Exception e){
			pc.stack = old_stack;
			throw new JasymcaException(e.toString());
		}
	}		

}


// Rationalize all numbers, user function
class LambdaRAT extends LambdaAlgebraic{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int   narg = getNarg( st );
		Algebraic arg = getAlgebraic(st).reduce();
		if(arg instanceof Unexakt) 
			st.push (f((Zahl)arg));
		else if(arg instanceof Exakt)  
			st.push( arg );
		else
			st.push( FunctionVariable.create(
				getClass().getName().substring((Environment.CLASS_PREFIX + "Lambda").length()).toLowerCase(), arg));
		return 0;
	}	
	Algebraic f_exakt(Algebraic x) throws JasymcaException{ 
		if(x instanceof Zahl) return (Zahl)x.rat();		
		return x.map(this);
	}
	Zahl f( Zahl x) throws JasymcaException{ 
		return (Zahl)x.rat();
	}
}
				
				

// Square free decomposition of polynomial
class LambdaSQFR extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int   narg = getNarg( st );
		Algebraic f = getAlgebraic( st );
		if(f instanceof Zahl){
			st.push( f );
			return 0;
		}
		if(!(f instanceof Polynomial))
			throw new ParseException("Argument to sqfr() must be polynomial.");
		f = ((Polynomial)f).rat();
		Algebraic[] fs = ((Polynomial)f).square_free_dec(((Polynomial)f).var);
		if(fs==null){
			st.push( f );
			return 0;
		}
		st.push( new Vektor( fs ));
		return 0;
	}
}


// Find roots of univariate real polynomial
class LambdaALLROOTS extends Lambda{
	
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int   narg = getNarg( st );
		Algebraic x = getAlgebraic( st ); // Evaluate to canonical form
		// Matlab version
		if( x instanceof Vektor )
			x = new Polynomial(new SimpleVariable("x"), (Vektor)x);

		if(!(x instanceof Polynomial))
			throw new JasymcaException("Argument to allroots must be polynomial.");
		Polynomial p = (Polynomial)((Polynomial)x).rat();
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
		st.push( Vektor.create(v) );
		return 0;
	}
}
			

class LambdaDET extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int   narg = getNarg( st );
		Matrix m = new Matrix(getAlgebraic( st ));
		st.push( m.det());
		return 0;
	}
}
			

class LambdaEIG extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int   narg = getNarg( st );
		Matrix m = new Matrix(getAlgebraic( st ));
		st.push( m.eigenvalues());
		return 0;
	}
}
			
class LambdaINV extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int   narg = getNarg( st );
		Matrix m = new Matrix(getAlgebraic( st ));
		st.push( m.invert() );
		return 0;
	}
}
			
class LambdaPINV extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int   narg = getNarg( st );
		Matrix m = new Matrix(getAlgebraic( st ));
		st.push( m.pseudoinverse() );
		return 0;
	}
}
			
class LambdaHILB extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int   narg = getNarg( st );
		int n = getInteger( st );
		Algebraic a[][] = new Algebraic[n][n];
		for(int i=0; i<n; i++)
			for(int k=0; k<n; k++)
				a[i][k] = new Exakt(1L, (long)(i+k+1));
		st.push( new Matrix( a ));
		return 0;
	}
}
			

			
class LambdaLU extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int   narg = getNarg( st );
		Matrix m = new Matrix( getAlgebraic( st ) ).copy();
		Matrix B = new Matrix(1,1);
		Matrix P = new Matrix(1,1);
		m.rank_decompose( B, P );
		if(length != 2 && length != 3)
			throw new JasymcaException("Usage: [l,u,p] = LU( Matrix ).");
		if( length>=2 ){
			st.push( B );
			st.push( m );
			if(length==3)
				st.push( P );
		}
		length = 1;			
		return 0;
	}
}

	

class LambdaSQRT extends LambdaAlgebraic{
	public LambdaSQRT(){ diffrule = "1/(2*sqrt(x))"; intrule = "2/3*x*sqrt(x)"; }

	// Integrate root of squares
	static String intrule2 = 
		"(2*a*x+b)*sqrt(X)/(4*a)+(4*a*c-b*b)/(8*a*sqrt(a))*log(2*sqrt(a*X)+2*a*x+b)";
		
	public Algebraic integrate(Algebraic arg, Variable x) throws JasymcaException{ 
		try{
			return super.integrate(arg,x);
		}catch(JasymcaException je){
			
		if(!(arg.depends(x)))
			throw new JasymcaException("Expression in function does not depend on Variable.");
		if( !(arg instanceof Polynomial) || ((Polynomial)arg).degree()!=2 ||
			!((Polynomial)arg).ratfunc(x) )
			throw new JasymcaException("Can not integrate function ");
		
		Algebraic xp = new Polynomial(x);
		Polynomial X = (Polynomial)arg;

		Algebraic y = evalx( intrule2, xp );
		y = y.value( new SimpleVariable("X"), X );
		y = y.value( new SimpleVariable("a"), X.a[2] );
		y = y.value( new SimpleVariable("b"), X.a[1] );
		y = y.value( new SimpleVariable("c"), X.a[0] );

		y = new SqrtExpand().f_exakt(y);		
		return y;
		}
	}

	Zahl f( Zahl x) throws JasymcaException{
		Unexakt z = x.unexakt();
		if(z.imag == 0.){
			if(z.real<0.)
				return new Unexakt(0, Math.sqrt(-z.real));
			return new Unexakt(Math.sqrt(z.real));
		}
		double sr  = Math.sqrt(Math.sqrt(z.real*z.real+z.imag*z.imag));
		double phi = Math.atan2(z.imag,z.real)/2.;
		return new Unexakt( sr*Math.cos(phi), sr*Math.sin(phi));
	}

	Algebraic f_exakt(Algebraic x) throws JasymcaException{ 
		if(x.equals(Zahl.ONE) || x.equals(Zahl.ZERO))
			return x;
		if(x.equals(Zahl.MINUS))
			return Zahl.IONE;
		if(x instanceof Zahl){ 
			return fzexakt((Zahl)x);
		}
	
		if(x instanceof Polynomial && ((Polynomial)x).degree()==1 &&
		((Polynomial)x).a[0].equals(Zahl.ZERO) &&
		((Polynomial)x).a[1].equals(Zahl.ONE) &&
		((Polynomial)x).var instanceof FunctionVariable &&
			((FunctionVariable)((Polynomial)x).var).fname.equals("exp"))
			return FunctionVariable.create("exp",
				((FunctionVariable)((Polynomial)x).var).arg.div(Zahl.TWO));
		return null;
	}
	
	
	Algebraic fzexakt(Zahl x) throws JasymcaException{
		if(x instanceof Exakt && !x.komplexq()){
			if(x.smaller(Zahl.ZERO)){
				Algebraic r = fzexakt((Zahl)x.mult(Zahl.MINUS));
				if(r!=null) return Zahl.IONE.mult(r);
				return r;
			}
			long nom = ((Exakt)x).real[0].longValue();
			long den = ((Exakt)x).real[1].longValue();
			// x = sqrt(a/b) = sqrt((a0^2*a1)/(b0^2*b1)) = (a0/(b0*b1))*sqrt(a1*b1);
			long a0 = introot(nom), a1 = nom/(a0*a0);
			long b0 = introot(den), b1 = den/(b0*b0);
			BigInteger br[] = { BigInteger.valueOf(a0),
								BigInteger.valueOf(b0*b1) };
			Exakt r = new Exakt(br);
			a0 = a1*b1;
			if(a0 == 1L) return r;
			return r.mult( new Polynomial(new FunctionVariable("sqrt",
									new Exakt(  BigInteger.valueOf(a0) ), this)));
		}
		return null;
	}
	
	// Find and return largest squared factor in x:
	// x = a^2*b ---> a
	long introot(long x){
		long s = 1L, f, g, t[] = { 2L, 3L, 5L};

		for(int i=0; i<t.length; i++){
			g = t[i]; f = g*g;
			while(x % f == 0L && x != 1L){
				s *= g; x /= f;
			}
		}
		for(long i= 6L; x!=1L ; i+=6L){	
			g = i+1; f = g*g;
			while(x % f == 0L && x != 1L){
				s *= g; x /= f;
			}
			g = i+5; f = g*g;
			while(x % f == 0L && x != 1L){
				s *= g; x /= f;
			}
			if(f>x) break;
		}
		return s;
	}		
}

class LambdaSIGN extends LambdaAlgebraic{
	public LambdaSIGN(){ 
		diffrule = "x-x";  // 0 geht nicht wegen parser: korrigieren!
		intrule  = "x*sign(x)"; 
	}

	Algebraic f_exakt(Algebraic x) throws JasymcaException{ 
		if(x instanceof Zahl)
			return f((Zahl)x);
		return null;
	}

	Zahl f( Zahl x) throws JasymcaException{
		return x.smaller(Zahl.ZERO)?Zahl.MINUS:Zahl.ONE;
	}
}

class LambdaABS extends LambdaAlgebraic{
	public LambdaABS(){ 
		diffrule = "sign(x)";  
		intrule  = "sign(x)*x^2/2"; 
	}

	Algebraic f_exakt(Algebraic x) throws JasymcaException{ 
		if(x instanceof Zahl)
			return f((Zahl)x);
		return FunctionVariable.create( "sqrt", x.mult( x.cc() ));
	}

	Zahl f( Zahl x) throws JasymcaException{
		return new Unexakt(x.norm());
	}
}



// Expand all user functions
class ExpandUser extends LambdaAlgebraic{
	Algebraic f_exakt(Algebraic x1) throws JasymcaException{ 
		if( !(x1 instanceof Polynomial) )
			return x1.map(this);
		Polynomial p = (Polynomial)x1;
		if(p.var instanceof SimpleVariable)
			return p.map(this);
		FunctionVariable f = (FunctionVariable)p.var;
		Object lx = pc.env.getValue(f.fname);
		if(!(lx instanceof UserFunction))
			return p.map(this);		
		UserFunction la = (UserFunction)lx;
		if( !(la.body instanceof Algebraic) )
			return x1;
		Algebraic body = (Algebraic)la.body;
		Algebraic x;
		if(la.var.length==1)
			x = body.value(la.var[0], f.arg);
		else if(f.arg instanceof Vektor && ((Vektor)f.arg).length()==la.var.length)
			x = la.fv((Vektor)f.arg);
		else 
			throw new JasymcaException("Wrong argument to function "+la.fname);
		
		Algebraic r=Zahl.ZERO;
		for(int i=p.a.length-1; i>0; i--){
			r=r.add(f_exakt(p.a[i])).mult(x);
		}
		if(p.a.length>0)
			r=r.add(f_exakt(p.a[0]));
		return r;
	}
}


// Assignment     x:3; --> assign value 3 to variable x
// assigning "null" deletes the variable
class ASS extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int narg = getNarg( st ); 

		Object val[] = new Object[narg];
		for(int i=0; i<narg; i++)
			val[i] = st.pop();
/*		
		if(narg>1){
			int nout = getNarg( st );
			if(nout != narg){
				throw new JasymcaException("Wrong number of Output variables.");
			}
		}
*/		
		for(int i=narg-1; i>=0; i--){
			String name   = getSymbol( st );
			if( !name.startsWith("$") )
				throw new JasymcaException("Illegal lvalue: "+name);
			name = name.substring(1);
			boolean idxq  = !st.empty() && 
							 st.peek() instanceof Integer; 
			if( !idxq ){
				pc.env.putValue( name, val[i] );
				if(val[i] instanceof Algebraic)
					((Algebraic)val[i]).name=name;
			}else{
				if(!(val[i] instanceof Algebraic)){
					throw new JasymcaException("No index allowed here: "+val[i]);
				}
				Matrix rhs 	= new Matrix( (Algebraic)val[i] );
				Matrix lhs  = new Matrix( (Algebraic)pc.env.getValue( name ) );
				Index  idx 	= Index.createIndex( st, lhs );			
				lhs.insert( rhs, idx );
				val[i] = lhs.reduce();
				pc.env.putValue(name, val[i] );
			}
		}
		for(int i=0; i<narg; i++)
			st.push( val[i] );
		return 0;
	}
	
	static int lambdap(Stack st, Lambda op) throws ParseException, JasymcaException{
		int narg = getNarg( st );
		Object y = st.pop();
		String name   = getSymbol( st );
		if( !name.startsWith("$") )
			throw new JasymcaException("Illegal lvalue: "+name);
		List t = Comp.vec2list( new Vector() );
		t.add( name );
		t.add( name.substring(1) );
		t.add( y );
		t.add( new Integer(2) );
		t.add( op );
		t.add( new Integer(1) );		
		t.add( Operator.get("=").getLambda() );
		pc.process_list( t, true );
		return 0;
	}

	static int lambdai(Stack st, boolean sign, boolean pre) throws ParseException, JasymcaException{
		int narg = getNarg( st );
		String name   = getSymbol( st );
		if( !name.startsWith("$") )
			throw new JasymcaException("Illegal lvalue: "+name);
		Object p = null;
		if(!pre)
			p = pc.env.getValue( name.substring(1) );
		List t = Comp.vec2list( new Vector());
		t.add( name );
		t.add( name.substring(1) );
		t.add( Zahl.ONE );
		t.add( new Integer(2) );
		t.add( sign ? Operator.get("+").getLambda() :
					   Operator.get("-").getLambda());
		t.add( new Integer(1) );
		t.add( Operator.get("=").getLambda() );
		pc.process_list( t, true );
		if(!pre && p != null){
			if( p instanceof Algebraic )
				((Algebraic)p).name = null;
			st.pop();
			st.push( p );
		}	
		return 0;
	}

}


class LambdaWHO extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		if(pc.ps != null){
			pc.ps.println( pc.env.toString() );
		}
		return 0;
	}
}

class LambdaADDPATH extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{
		int narg = getNarg( st );
		while(narg-- > 0){
			Object s = st.pop();
			if( !(s instanceof String) )
				throw new JasymcaException("Usage: ADDPATH( dir1, dir2, ... )");
			pc.env.addPath( ((String)s).substring(1) );
		}
		return 0;
	}
}

class LambdaPATH extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{
		int n = pc.env.path.size();
		String s = "";
		while(n-- > 0){
			Object p = pc.env.path.elementAt(n);
			s = s+p;
			if(n!=0)
				s = s+":";
		}
		if(pc.ps != null){
			pc.ps.println( s );
		}
		return 0;
	}
}

