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


public class XProcessor extends Processor {

	public XProcessor( Environment env ){
		super( env );
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
					 COLON		= 10;
					 
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
			if( s.startsWith(" ") )
				return STRING;
			if( s.startsWith("@") )
				return FUNCTION;
			if( s.startsWith("Lambda") )
				return FUNCTION;
			if( s.startsWith("$") )
				return LVALUE;
			if( s.startsWith("#") )  
				return PDIR;
			return SYMBOL;
		}
		if( x instanceof Integer )
			return NARG;
		if( x instanceof Lambda )
			return FUNCTION;
		return 0;
	}

	
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
				if( !(x instanceof Lambda) )
					x = env.getValue((String)x);
				return ((Lambda)x).lambda( stack );
			case SYMBOL:
				Object val = env.getValue((String)x);
				if( val != null )
					return process_instruction( val, canon );
				else if( canon ){
					x = new Polynomial( new SimpleVariable((String)x) );
					stack.push( x );
				}else{
					stack.push( x );
				}
				return 0;
			case PDIR:  // processor internal function
				String selector = ((String)x).substring(1);
				if( selector.equals(";") ){ 
					printStack();
				}else if( selector.equals(",") ){
					clearStack();
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
	

	void clearStack(){
		while(!stack.empty()){
			Object x = stack.pop();
		}
	}

	int result = 1;
	
	public void printStack(){
		while(!stack.empty()){
			Object x = stack.pop();
			if( x instanceof Algebraic ){
				String vname;
				if( ((Algebraic)x).name != null )
					vname = ((Algebraic)x).name;
				else{
					vname = "d"+ result++; 
					env.putValue(vname, x);
				}
				if(ps!=null){
					String s = "    "+vname+" = ";
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

