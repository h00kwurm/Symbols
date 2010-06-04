package net.sourceforge.jasymcaandroid.jasymca;
/*
   Jasymca	- Symbolic Calculator 
   This version is written for J2ME, CLDC 1.1,  MIDP 2, JSR 75
   or J2SE


   Copyright (C) 2006, 2009 - Helmut Dersch  der@hs-furtwangen.de
   
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

public class Operator implements Constants{
	String mnemonic;
	String symbol;
	int precedence;
	int associativity;	
	int type;
	Lambda func = null;
	
	static Operator[] OPS = new Operator[0];

	public boolean unary()  { return (type & UNARY)  !=0; }
	public boolean binary() { return (type & BINARY) !=0; }
	public boolean ternary(){ return (type & TERNARY)!=0; }
	public boolean lvalue() { return (type & LVALUE) !=0; }
	public boolean list()   { return (type & LIST)   !=0; }
	public boolean left_right() { return associativity == LEFT_RIGHT; }
	
	public Operator( String mnemonic, 
					 String symbol,
					 int precedence, 
					 int associativity,
					 int type ){
		this.mnemonic 		= mnemonic;
		this.symbol 		= symbol;
		this.precedence 	= precedence;
		this.associativity 	= associativity;
		this.type 			= type;
	}
	
	
	public String toString(){
		return symbol;
	}
	

	// return operator that text begins with
	static Operator get( Object text_in ){
		if( !(text_in instanceof String) )
			return null;
		String text = (String) text_in;
		for(int k=0; k<OPS.length; k++){
			Operator op = OPS[k];
			if(text.startsWith( op.symbol ))
				return op;
		}
		return null;
	}			
			
	// return operator that text begins with
	static Operator get( Object text_in, int pos ){
		if( !(text_in instanceof String) )
			return null;
		String text = (String) text_in;
		for(int k=0; k<OPS.length; k++){
			Operator op = OPS[k];
			if(text.startsWith( op.symbol )){
				switch(pos){
					case START: 
						if(op.unary() && op.left_right())
							return op;
						continue;
					case END:
						if(op.unary() && !op.left_right())
							return op;
						continue;
					case MID:
						if( op.binary() || op.ternary() )
							return op;
						continue;
				}
			}
		}
		return null;
	}			
			
	Lambda getLambda(){
		if( func==null ){
			try{
				Class<?> c 		= Class.forName(Environment.CLASS_PREFIX + mnemonic);
				func 			= (Lambda)c.newInstance();
			}catch( Exception e ){
			}
		}
		return func;
	}
}

class ADJ extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int   narg = getNarg( st );
		Matrix m = new Matrix(getAlgebraic( st ));
		st.push( m.adjunkt().reduce() );
		return 0;
	}
}

class TRN extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int   narg = getNarg( st );
		Matrix m = new Matrix(getAlgebraic( st ));
		st.push( m.transpose().reduce() );
		return 0;
	}
}

class FCT extends LambdaAlgebraic{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int   narg = getNarg( st );
		Algebraic arg = getAlgebraic( st );
		if(arg instanceof Zahl) 
			st.push( f((Zahl)arg ) );
		else
			st.push( FunctionVariable.create("factorial", arg) );
		return 0;
	}	
	
	Algebraic f_exakt(Algebraic x) throws JasymcaException{ 
		if(x instanceof Zahl) return f((Zahl)x);;		
		return null;
	}
	
	
	Zahl f( Zahl x) throws JasymcaException{ 
		if(!x.integerq() || x.smaller(Zahl.ZERO))
			throw new JasymcaException("Argument to factorial must be a positive integer, is "+x);
		Algebraic r = Zahl.ONE;
		while(Zahl.ONE.smaller(x)){
			r=r.mult(x);
			x=(Zahl)x.sub(Zahl.ONE);
		}
		return (Zahl)r;
	}
}

class LambdaFACTORIAL extends FCT{}

class FCN extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int narg 		= getNarg( st );
		List code_in 	= getList(st);
		String fname 	= getSymbol( st ).substring(1);
		int nvar        = getNarg( st );
		SimpleVariable[] vars = new SimpleVariable[nvar];
		for(int i=0; i<nvar; i++)
			vars[i] = new SimpleVariable( getSymbol( st ) );

		Lambda  func	 = null; 
		Environment env  = new Environment();
		Stack ups		 = new Stack();

		Object y = null;		
		
		if( nvar==1 ){
			int res = UserProgram.process_block( code_in, ups, env, false );
			if( res != Processor.ERROR ) 
				y=ups.pop();
		}			
		if( y instanceof Algebraic ){	
			func = new UserFunction( fname, vars, (Algebraic)y, null, null );
		}else{
			func = new UserProgram( fname, vars, code_in, null, env, ups ); 
		}	
		pc.env.putValue(fname, func);
		st.push( fname );
		return 0;
	}
}

class POW extends LambdaAlgebraic{	
	Algebraic f_exakt(Algebraic x, Algebraic y) throws JasymcaException{ 
		if(x.equals(Zahl.ZERO)){
			if( y.equals( Zahl.ZERO ))
				return Zahl.ONE;
			return Zahl.ZERO;
		}
		if( y instanceof Zahl && ((Zahl)y).integerq() )
			return x.pow_n(((Zahl)y).intval());
		return FunctionVariable.create("exp",FunctionVariable.
				create("log",x).mult(y));
	}
}



// ++
class PPR extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		return ASS.lambdai( st, true, false );
	}
}

// --
class MMR extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		return ASS.lambdai( st, false, false );
	}
}

// ++
class PPL extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		return ASS.lambdai( st, true, true );
	}
}

// --
class MML extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		return ASS.lambdai( st, false, true );
	}
}

// +=
class ADE extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		return ASS.lambdap( st, Operator.get("+").getLambda() );
	}
}

// -=
class SUE extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		return ASS.lambdap( st, Operator.get("-").getLambda() );
	}
}

// *=
class MUE extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		return ASS.lambdap( st, Operator.get("*").getLambda() );
	}
}

// /=
class DIE extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		return ASS.lambdap( st, Operator.get("/").getLambda() );
	}
}



class ADD extends LambdaAlgebraic{
	Algebraic f_exakt(Algebraic x) throws JasymcaException{ 
			return x;
	}	
	Algebraic f_exakt(Algebraic x, Algebraic y) throws JasymcaException{ 
		return x.add(y);
	}	
	Zahl f( Zahl x) throws JasymcaException{
		return (Zahl)f_exakt(x);
	}
}
	
class SUB extends LambdaAlgebraic{
	Algebraic f_exakt(Algebraic x) throws JasymcaException{ 
		return x.mult(Zahl.MINUS);
	}	
	Algebraic f_exakt(Algebraic x, Algebraic y) throws JasymcaException{ 
		return x.add( y.mult(Zahl.MINUS) );
	}	
	Zahl f( Zahl x) throws JasymcaException{
		return (Zahl)f_exakt(x);
	}
}
	
class MUL extends LambdaAlgebraic{
	Algebraic f_exakt(Algebraic x, Algebraic y) throws JasymcaException{ 
		return x.mult(y);
	}
	
}

class MMU extends LambdaAlgebraic{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int   narg = getNarg( st );
		if(narg!=2)
			throw new ParseException("Wrong number of arguments for \"*\".");
		Algebraic b = getAlgebraic( st );
		Algebraic a = getAlgebraic( st );
		if( b.scalarq() )
			st.push( a.mult(b) );
		else if( a.scalarq() )
			st.push( b.mult(a));
		else if( a instanceof Vektor && b instanceof Vektor ){
			st.push( a.mult(b) );
		}else{
			st.push( new Matrix(a).mult(new Matrix(b)).reduce() );
		}
		return 0;
	}
}

class MPW extends LambdaAlgebraic{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int   narg = getNarg( st );
		Algebraic a = getAlgebraic( st );
		Algebraic b = getAlgebraic( st );
		if( a.scalarq() && b.scalarq() ){
			st.push (new POW().f_exakt( b, a ));
			return 0;
		}
		if( !(a instanceof Zahl) || !((Zahl)a).integerq())
			throw new JasymcaException("Wrong arguments to function Matrixpow.");
		st.push( new Matrix(b).mpow( ((Zahl)a).intval() ));
		return 0;
	}
}
	
class DIV extends LambdaAlgebraic{
	Algebraic f_exakt(Algebraic x, Algebraic y) throws JasymcaException{ 
		return x.div(y);
	}
}

class MDR extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int   narg = getNarg( st );
		if(narg!=2)
			throw new ParseException("Wrong number of arguments for \"/\".");
		Algebraic b = getAlgebraic( st );
		Matrix    a = new Matrix( getAlgebraic( st ));
		st.push( a.div(b).reduce() );
		return 0;
	}
}

class MDL extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		int   narg = getNarg( st );
		if(narg!=2)
			throw new ParseException("Wrong number of arguments for \"\\\".");
		Matrix b = new Matrix( getAlgebraic( st ));
		Matrix a = new Matrix( getAlgebraic( st ));
		st.push( ((Matrix)b.transpose().div(a.transpose())).transpose().reduce() );
		return 0;
	}
}

class EQU extends LambdaAlgebraic{
	Algebraic f_exakt(Algebraic x1, Algebraic y1) throws JasymcaException{
		Zahl x = ensure_Zahl(x1);
		Zahl y = ensure_Zahl(y1);
		return y.equals(x)  ? 
			   Zahl.ONE : Zahl.ZERO;
	}		
}
		
class NEQ extends LambdaAlgebraic{
	Algebraic f_exakt(Algebraic x1, Algebraic y1) throws JasymcaException{
		Zahl x = ensure_Zahl(x1);
		Zahl y = ensure_Zahl(y1);
		return y.equals(x)  ? 
			   Zahl.ZERO : Zahl.ONE;
	}		
}
		
class GEQ extends LambdaAlgebraic{	
	Algebraic f_exakt(Algebraic x1, Algebraic y1) throws JasymcaException{
		Zahl x = ensure_Zahl(x1);
		Zahl y = ensure_Zahl(y1);
		return x.smaller(y)  ? 
			   Zahl.ZERO : Zahl.ONE;
	}		
}
		
class GRE extends LambdaAlgebraic{
	Algebraic f_exakt(Algebraic x1, Algebraic y1) throws JasymcaException{
		Zahl x = ensure_Zahl(x1);
		Zahl y = ensure_Zahl(y1);
		return y.smaller(x) ? 
			   Zahl.ONE : Zahl.ZERO;
	}		
}
		
class LEQ extends LambdaAlgebraic{
	Algebraic f_exakt(Algebraic x1, Algebraic y1) throws JasymcaException{
		Zahl x = ensure_Zahl(x1);
		Zahl y = ensure_Zahl(y1);
		return y.smaller(x) ? 
			   Zahl.ZERO : Zahl.ONE;
	}		
}
		
class LES extends LambdaAlgebraic{
	Algebraic f_exakt(Algebraic x1, Algebraic y1) throws JasymcaException{
		Zahl x = ensure_Zahl(x1);
		Zahl y = ensure_Zahl(y1);
		return x.smaller(y) ? 
			   Zahl.ONE : Zahl.ZERO;
	}		
}

class NOT extends LambdaAlgebraic{
	Zahl f(Zahl x) throws JasymcaException{
		return x.equals(Zahl.ZERO) ? Zahl.ONE : Zahl.ZERO;
	}
}
class OR extends LambdaAlgebraic{
	Algebraic f_exakt(Algebraic x1, Algebraic y1) throws JasymcaException{
		Zahl x = ensure_Zahl(x1);
		Zahl y = ensure_Zahl(y1);
		return x.equals(Zahl.ONE) || y.equals(Zahl.ONE) ?
			Zahl.ONE : Zahl.ZERO;
	}
}
class AND extends LambdaAlgebraic{
	Algebraic f_exakt(Algebraic x1, Algebraic y1) throws JasymcaException{
		Zahl x = ensure_Zahl(x1);
		Zahl y = ensure_Zahl(y1);
		return x.equals(Zahl.ONE) && y.equals(Zahl.ONE)?
			Zahl.ONE : Zahl.ZERO;
	}
}

class LambdaGAMMA extends LambdaAlgebraic{
	Zahl f(Zahl x) throws JasymcaException{
		return new Unexakt( Sfun.gamma( x.unexakt().real ) ); 
	}
}

class LambdaGAMMALN extends LambdaAlgebraic{
	Zahl f(Zahl x) throws JasymcaException{
		return new Unexakt( Sfun.logGamma( x.unexakt().real ) ); 
	}
}

	

