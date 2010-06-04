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

import java.util.*;
import java.io.*;

class LambdaERROR extends Lambda {
	public int lambda(Stack st) throws ParseException, JasymcaException{
		LambdaPRINTF.printf( st );
		return Processor.ERROR;
	}
}


class LambdaEVAL extends Lambda {
	public int lambda(Stack st) throws ParseException, JasymcaException{
		int narg = getNarg( st );
		Object s_in = st.pop();
		if(!(s_in instanceof String) )
			throw new JasymcaException("Argument to EVAL must be string.");
		String s = (String)s_in;
		List pgm = pr.compile( s );
		return pc.process_list( pgm, true );
	}
}


// Function: block (expr_1, ..., expr_n)
// ([v_1, ..., v_m], expr_1, ..., expr_n)
class LambdaBLOCK extends Lambda {
	public int lambda( Stack st ) throws ParseException, JasymcaException{
		int narg = getNarg( st );
		Environment local = pc.env.copy();

		List code = getList( st );
		Stack ups = new Stack();
		
		int ret = UserProgram.process_block( code, ups, local, false );
		pc.env.update(local);
		if( ret != Processor.ERROR && !ups.empty() ){
			Object y = ups.pop();
			st.push(y);
		}else{
			throw new JasymcaException("Error processing block.");
		}
		return 0;
	}
}



// Branch
// "@if ( x , y , z )"
// Arguments:
// 0 - Condition
// 1 - Code for Condition=True 
// 2 - Code for Condition=False

class LambdaBRANCH extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{ 
		// Variable definition, store in Prefix format and return success
		int narg = getNarg( st ), sel;
		List cond, b_true, b_false;
		switch( narg ){
			case 2: 
				cond   = getList( st );
				b_true = getList( st );
				pc.process_list( cond, true );
				sel    = getInteger( pc.stack );
				if( sel == 1 ){
					return pc.process_list( b_true, true );
				}else if( sel != 0 )
					throw new JasymcaException("Branch requires boolean type.");
				break;
			case 3: 
				cond 	= getList( st );
				b_true  = getList( st );
				b_false = getList( st );
				pc.process_list( cond, true );
				sel     = getInteger( pc.stack );
				if( sel == 1 ){
					return pc.process_list( b_true, true );
				}else if( sel == 0 ){
					return pc.process_list( b_false, true );
				}else
					throw new JasymcaException("Branch requires boolean type, got "+sel);
			default: throw new JasymcaException("Wrong number of arguments to branch.");
		}
		return 0;
	}
}

/*

// Loop
// "@do 1 , x , y , w , z , v"
// Arguments:
// 0 - selector(integer) 1:thru, 2:while, 3:unless
// 1 - loop variable (number if none)
// 2 - loop variable initial value
// 3 - loop variable step width
// 4 - loop condition (depending on type)
// 5 - loop body



class Loop extends Lambda{
	public Object lambda(Object x) throws ParseException, JasymcaException{
		Environment local = js.env.copy(), global = js.env;

		Object args = getArgs(car(x));
//		p("Loop1: "+args);

		int type = getInteger(args, 0);
		
		// Initialize loop variable
		Object vname = nth(args,1);
		Object val 	 = Zahl.ZERO;
		
		if(!Zahl.ZERO.equals(vname)){
			val = getArg(args, 2);
		}else{
			vname = "DefaultLoopVariable";
		}
		local.putValue(vname.toString(), val);
		
		Algebraic stepwidth = getAlgebraic(args, 3);
		
		Object step   = Lisp.list("@ass",
						Lisp.list(vname,
						Lisp.list(
						Lisp.list("@add", 
						Lisp.list(vname, stepwidth )))));

//		p("Loop, step = "+step);
		
		boolean steppos = true;
		if( stepwidth instanceof Zahl )
			steppos = !( ((Zahl)stepwidth).smaller( Zahl.ZERO ));

		Object cond	   = null;
		switch( type ){
			case 1:  // for ... thru
				cond    = Lisp.list( steppos ? "@gre": "@les", 
						  Lisp.cons(vname, Lisp.list(nth(args,4))));
				break;
			case 2:  // for ... while
				cond    = Lisp.list( "NOT", Lisp.list(nth(args,4)) );
				break;
			case 3:  // for ... unless
				cond    = nth(args,4);
				break;
		}
		
		
//		p("Loop, cond = "+cond);
		
		Object code = getArgs(nth(args,5));
		Object res  = Zahl.ZERO;

		js.env = local;
//p("env:"+js.env);
		while(true){
			Object c = js.evalPrefix(cond, true);
			if(c.equals(Zahl.ONE)){
				break;
			}else if(!c.equals(Zahl.ZERO))
				throw new JasymcaException("Not boolean: "+c);
			int size = length(code);
			for(int i=0; i<size; i++){
//				p("Loop executing: "+ nth(code,i));
				res = js.evalPrefix(nth(code,i), true);
//				p("res="+res);
			}
			js.evalPrefix(step);
		}
		js.env = global;
		js.env.update(local);
		return res;
	}
}


*/


class LambdaFOR extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{
		int narg = getNarg( st );

		List cond   = getList( st );
		List body	= getList( st );

		pc.process_list( cond, true );
		if( pc.stack.empty() || !(pc.stack.peek() instanceof Vektor) 
			|| ((Algebraic)pc.stack.peek()).name==null ){
			throw new ParseException("Wrong format in for-loop.");
		}
		Vektor vals = (Vektor)pc.stack.pop();
		
		for(int i=0; i<vals.length() ; i++){
			pc.env.putValue(vals.name, vals.get(i));
			int ret = pc.process_list( body, true );
			switch( ret ){
				case Processor.BREAK:
					return 0;
				case Processor.RETURN:
				case Processor.EXIT:
				case Processor.ERROR:
					return ret;
				case Processor.CONTINUE:
			}
		}
		return 0;
	}
}

class LambdaXFOR extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{
		int narg = getNarg( st );

		List cond     = getList( st );
		List step_in  = getList( st );
		List thru_in  = getList( st );
		List body	  = getList( st );

		pc.process_list( cond, true );
		if( pc.stack.empty() || !(pc.stack.peek() instanceof Zahl) 
			|| ((Algebraic)pc.stack.peek()).name==null ){
			throw new ParseException("Non-constant initializer in for loop.");
		}
		Zahl x  = (Zahl)pc.stack.pop();
		String xname = x.name;
		pc.process_list( step_in, true );
		if( pc.stack.empty() || !(pc.stack.peek() instanceof Zahl)){
			throw new ParseException("Step size must be constant.");
		}
		Zahl step = (Zahl)pc.stack.pop();
		pc.process_list( thru_in, true );
		if( pc.stack.empty() || !(pc.stack.peek() instanceof Zahl)) {
			throw new ParseException("Wrong format in for-loop.");
		}
		Zahl thru = (Zahl)pc.stack.pop();

		boolean pos = !step.smaller( Zahl.ZERO );
		
		while( true ){
			if( (pos ? thru.smaller(x) : x.smaller(thru)) )
				break;
			pc.env.putValue(xname, x);
			int ret = pc.process_list( body, true );
			switch( ret ){
				case Processor.BREAK:
					return 0;
				case Processor.RETURN:
				case Processor.EXIT:
				case Processor.ERROR:
					return ret;
				case Processor.CONTINUE:
			}
			x = (Zahl)x.add( step );
		}
		return 0;
	}
}

class LambdaWHILE extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{
		int narg = getNarg( st );
		List cond   = getList( st );
		List body	= getList( st );
		while( true){
			pc.process_list( cond, true );
			Object c = pc.stack.pop();
			if(c.equals(Zahl.ZERO)){
				break;
			}else if(!c.equals(Zahl.ONE))
				throw new JasymcaException("Not boolean: "+c);
			int ret = pc.process_list( body, true );
			switch( ret ){
				case Processor.BREAK:
					return 0;
				case Processor.RETURN:
				case Processor.EXIT:
				case Processor.ERROR:
					return ret;
				case Processor.CONTINUE:
			}
		}
		return 0;
	}
}


class LambdaPRINTF extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{
		printf( st );
		return 0;
	}
	
	static void printf( Stack st )throws ParseException, JasymcaException{
		int narg = getNarg( st );
		Object s_in = st.pop();
		if(!(s_in instanceof String) )
			throw new JasymcaException("Argument to PRINTF must be string.");
		String fmt = (String)s_in;
		int idx, i=1;
		String cs = "%f";
		while( (idx = fmt.indexOf( cs )) != -1  && !st.empty() && narg-- > 1) {
			Object n = st.pop();
			if(n != null){
				StringBuffer sb = new StringBuffer( fmt );
				sb.delete( idx, idx+cs.length() );
				sb.insert( idx, n.toString() );
				fmt = sb.toString();
			}else
				break;
		}
		while( (idx = fmt.indexOf( "\\n" )) != -1  ) {
			StringBuffer sb = new StringBuffer( fmt );
			sb.delete( idx, idx+"\\n".length() );
			sb.insert( idx, "\n" );
			fmt = sb.toString();
		}
		if(pc.ps != null){
			pc.ps.print( fmt.toString() );
		}
	}		
}	

class LambdaPAUSE extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{
		int narg   = getNarg( st );
		int millis = Math.abs( getInteger( st ) );
		try{
			Thread.sleep( millis );
		}catch(Exception e){
		}
		return 0;
	}
}

