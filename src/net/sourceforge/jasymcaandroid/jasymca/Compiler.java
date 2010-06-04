package net.sourceforge.jasymcaandroid.jasymca;
/*
package com.example.helloandroid.jasymca;
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


public class Compiler{
	Parser p;
	static String[] expr_vars  = {"u","v","w","z"};
	static String[] stmnt_vars = {"X","Y"};
	static String[] lval_vars  = {"y"};
	static String[] func_vars  = {"f"};
	static String[] list_vars  = {"x"};
	List     rule_in, rule_out;

	Hashtable<Object, Object> vars; 

	boolean variableq( Object x ){
		return Parser.oneof( x, expr_vars) 
			|| Parser.oneof( x, stmnt_vars)
			|| Parser.oneof( x, lval_vars )
			|| Parser.oneof( x, func_vars )
			|| Parser.oneof( x, list_vars );
	}
	
	Object match( Object v, List expr ) throws ParseException{
		Object r = null;
		if( p.oneof(v, expr_vars) )
			r = p.compile_expr( expr );
		else if( p.oneof(v, stmnt_vars) )
			r = p.compile_statement( expr );
		else if( p.oneof(v, lval_vars ))
			r = p.compile_lval( expr );
		else if( p.oneof(v, func_vars ))
			r = p.compile_func( expr );
		else if( Parser.oneof(v, list_vars ))
			r = p.compile_list( expr );
		return r;
	}
		
	
	List change(){
		List r = Comp.vec2list(new Vector());
		for(int i=0; i<rule_out.size(); i++){
			Object x = rule_out.get(i);
			if(variableq(x)){
				r.add( vars.get( x ) );
			}else if( x instanceof Zahl ) {
				int xi = ((Zahl)x).intval();
				r.add( new Integer(xi));
			}else
				r.add( x );
		}
		return r;
	}
	
	String toString(Hashtable<?, ?> h){
		String s = "";
		Enumeration<Object> k = vars.keys();
		while(k.hasMoreElements()){
			Object key = k.nextElement();
			Object val = h.get(key);
			s = s+"key:"+key+"   val:"+val+"\n";
		}
		return s;
	}

	
	List compile( List<?> expr ) throws ParseException{
//System.out.println("Co.:"+rule_in+"  "+rule_out+"  "+expr);
		if( expr.size() != rule_in.size() )
			return null;
		if( matcher( rule_in, expr ) ){
			return change();				
		}else
			return null;
	}
	
	/* Match expression against rule.
	*/
	boolean matcher(List rule, List expr) throws ParseException{
		// empty rule matches empty expression
		if(rule.size()==0){
			return expr.size()==0;
		}
		// at least one expr-token matches each rule-token
		if(rule.size() > expr.size())
			return false;
		// process one token
		Object x = rule.get(0);		
			
		if( variableq(x) ){
			// try all possible match combinations
//			for(int i=1; i<=expr.size()+1-rule.size(); i++){
			int start = expr.size()+1-rule.size();
			for(int i=start; i>=1; i--){
				Object xv = match(x, expr.subList( 0, i));
				if(xv!=null && 
				   matcher(rule.subList(1,rule.size()),
						   expr.subList(i,expr.size()))){
					vars.put(x,xv);
					return true;
				}
			}
			// No match found
			return false;
		}
		// tokens must match
		Object y = expr.get(0);
		// either as list
		if(x instanceof List){
			return (y instanceof List) && 
					matcher((List)x, (List)y) &&
					matcher(rule.subList(1,rule.size()), 
					        expr.subList(1,expr.size()));
		}
		// or as atomic token
		if(x.equals(y)){
			return matcher(rule.subList(1,rule.size()), 
			               expr.subList(1,expr.size()));
		}
		return false;
	}
				
				
		
	
	public Compiler( List rule_in, 
	                 List rule_out,
	                 Parser p) throws ParseException{
		vars = new Hashtable();
		this.rule_in = rule_in;
		this.rule_out= rule_out;
		this.p = p;
	}
}

class Rule{
	List rule_in;
	List rule_out;
}
