package net.sourceforge.jasymcaandroid.jasymca;
/* Jasymca	-	- Symbolic Calculator for Mobile Devices
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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.  */

/*------------------------------------------------------------*/

import java.util.*;
import java.io.*;


public class Processor implements Constants {

	Stack 		stack;
	Environment env;
	PrintStream ps = null;
	
	public Processor( Environment env ){
		this.stack  = new Stack();
		this.env    = env;
		Lambda.pc   = this;
	}
	
	void setEnvironment( Environment env ){
		this.env = env;
	}
	
	Environment getEnvironment(){
		return env;
	}
	
	void setPrintStream( PrintStream ps ){
		this.ps = ps;
	}
	
	boolean interrupt_flag = false;

	boolean check_interrupt(){	
		return interrupt_flag;
	}
	
	void set_interrupt( boolean flag ){
		interrupt_flag = flag;
	}
		
	
	static final int LIST 		= 1,
					 MATRIX 	= 2,
					 SCALAR		= 3,
					 STRING		= 4,
					 FUNCTION	= 5,
					 LVALUE		= 6,
					 SYMBOL		= 7,
					 NARG		= 8,
					 PDIR		= 9,
					 COLON		= 10,
					 SYMREF	    = 11;
					 
	static final int BREAK		= 1,
					 CONTINUE	= 2,
					 RETURN		= 3,
					 EXIT		= 4,
					 ERROR		= 5;
					 
	int instruction_type( Object x ){
		if( x instanceof List )
			return LIST;
		if( x instanceof Matrix || x instanceof Vektor )
			return MATRIX;
		if( x instanceof Algebraic )
			return SCALAR;
		if( x.equals(":") )
			return COLON;
		if( x instanceof String ){
			String s = (String)x;
			switch( s.charAt(0)){
				case '@': return SYMREF;
				case ' ': return STRING;
				case '$': return LVALUE;
				case '#': return PDIR;
				default:  return SYMBOL;
			}
		}
		if( x instanceof Integer )
			return NARG;
		if( x instanceof Lambda )
			return FUNCTION;
		return 0;
	}
		
	// canon is ignored in this processor	
	public int process_instruction( Object x, 
									 boolean canon ) 
								     throws ParseException, JasymcaException{
		if( interrupt_flag ){
			set_interrupt( false );
			throw new JasymcaException("Interrupted.");
		}
		switch( instruction_type( x ) ){
			case LIST:
			case SCALAR:
			case NARG:
			case STRING:
			case LVALUE:
			case COLON:
				stack.push( x );
				return 0;
			case MATRIX:
				stack.push( x );
				return 0;
			case FUNCTION:
				return ((Lambda)x).lambda( stack );
			case SYMREF:
				String s = ((String)x).substring(1);
				Object val = env.getValue( s );
				if( val == null ){ // try m-file
					try{
						LambdaLOADFILE.readFile( s + ".m" );
						val = env.getValue( s );
					}catch(Exception e){
					}
				}
				if( val instanceof Lambda )
					return ((Lambda)val).lambda( stack );
				if( val instanceof Algebraic ){
					Matrix mx = new Matrix((Algebraic)val); 
					Index  idx 	= Index.createIndex( stack, mx );
					mx = mx.extract( idx );
					stack.push( mx.reduce() );
					return 0;
				}
				if( val instanceof String && ((String)val).length()>1 ){
					s   = ((String)val).substring(1);
					val = env.getValue( s );
					if( val == null ){ // try m-file
						try{
							LambdaLOADFILE.readFile( s + ".m" );
							val = env.getValue( s );
						}catch(Exception e){
						}
					}
					if( val instanceof Lambda )
						return ((Lambda)val).lambda( stack );
				}
				throw new ParseException("Unknown symbol or incorrect symbol type: "+x);
			case SYMBOL:
				val = env.getValue((String)x);
				if( val == null ){ // try m-file
					try{
						LambdaLOADFILE.readFile( (String)x + ".m" );
						return 0;
					}catch(Exception e){
						throw new ParseException("Unknown symbol: "+x);
					}
				}
				return process_instruction( val, canon );
			case PDIR:  // processor internal function
				String selector = ((String)x).substring(1);
				if( selector.equals(";") ){ 
					clearStack();
				}else if( selector.equals(",") ){
					printStack();
				}else if( selector.equals("brk") ){
					return BREAK;
				}else if( selector.equals("exit") ){
					return EXIT;
				}else if( selector.equals("cont") ){
					return CONTINUE;
				}else if( selector.equals("ret") ){
					return RETURN;
				}else{			
					int nout = Integer.parseInt( selector );
					Lambda.length = nout;
				}
				return 0;
		}
		throw new JasymcaException( "Unrecognized instruction type: "+x );
	}
		
	synchronized public int process_list( List x, boolean canon ) 
							throws JasymcaException, ParseException{
// System.out.println("Prg:"+x);
		int n = x.size(), i = 0;
		try{	
			for( i=0; i<n; i++ ){
				Object z = x.get( i );
				int ret = process_instruction( z, canon );
				if( ret != 0 )
					return ret;
			}
			return 0;
		}catch(ParseException p){
			throw p;
//			throw new ParseException(
//			"At Program:"+x+"  Program Counter:"+i+":\n"+p);
		}catch(JasymcaException j){
			throw j;
//			throw new JasymcaException(
//			"At Program:"+x+"  Program Counter:"+i+":\n"+j);
		}
	}

	void clearStack(){
		while(!stack.empty()){
			Object x = stack.pop();
			if( stack.size()==0 && x instanceof Algebraic )
				env.putValue("ans", x);
		}
	}

	
	public void printStack(){
		while(!stack.empty()){
			Object x = stack.pop();
			if( x instanceof Algebraic ){
				String vname = "ans";
				env.putValue(vname, x);
				if( ((Algebraic)x).name != null )
					vname = ((Algebraic)x).name;
				if(ps!=null){
					String s = vname+" = ";
					ps.print( s );
					((Algebraic)x).print(ps);
					ps.println("");
				}
			}else if( x instanceof String ){
				ps.println((String)x);
			}
		}
	}			
}

