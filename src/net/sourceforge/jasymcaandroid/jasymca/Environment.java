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

// Environment for variables and functions
// Stored by name, case-insensitive

public class Environment extends Hashtable<Object, Object>{
	
	final static public String CLASS_PREFIX = "net.sourceforge.jasymcaandroid.jasymca.";
	
	static Vector<String> path = new Vector<String>();
	static Hashtable<Object,Object> globals = new Hashtable<Object, Object>();
	
	public Environment(){
	}
	
	void addPath( String s ){
		if( !path.contains( s ) )
			path.addElement( s );
	}

	public Environment copy(){
		Environment e = new Environment();
		Enumeration<?> k = keys();
		while(k.hasMoreElements()){
			Object key = k.nextElement();
			e.put(key,get(key));
		}
		return e;
	}

	public void update(Environment local){
		Enumeration<?> kl = local.keys();
		while(kl.hasMoreElements()){
			Object key = kl.nextElement();
			if(get(key) != null){ // element may have been changed
				put(key,local.get(key));
			}
		}
	}

	

	public String toString(){
		Enumeration<Object> k = keys();
		String s="";
		while(k.hasMoreElements()){
			Object key = k.nextElement();
			s += (key+": "); 
			s += (getValue((String)key)+"\n");
		}
		k = globals.keys();
		s += "Globals:\n";
		while(k.hasMoreElements()){
			Object key = k.nextElement();
			s += (key+": "); 
			s += (getValue((String)key)+"\n");
		}
		return s;
	}
		
		
	// Store Variable	
	public void putValue(String var, Object x){
//		var  = var.toUpperCase();
		// Way to cancel variables
		if(x.equals("null")){
			remove(var);
		}else{
			if( x instanceof Lambda )
				globals.put( var, x );
			else
				put( var, x);
		}
	}


	// Value of Variable var	
	public Object getValue(String var){
//		var 		= var.toUpperCase();
		// String variable always evaluate to themselves
		if( var.startsWith(" ") )
			return var;
		Object r 	= get(var);
		if( r != null ){
			return r;
		}
		r = globals.get(var);
		if( r != null )
			return r;		

		// If this is an uninstantiated Operator, create an instance
		// Let the Java Classloader do the work
			try{
				String fname = CLASS_PREFIX + "Lambda"+var.toUpperCase();
				Class<?> c 	= Class.forName(fname);
				Lambda f 	= (Lambda)c.newInstance();
				putValue(var, f);
				r 			= f;
			}catch(Exception e){
			}

		return r;
	}
	
	// Get numeric constant
	public Zahl getnum(String var){
		var 		= var;//.toUpperCase();
		Object r 	= get(var);
		if(r==null)
			r = globals.get(var);
		if(r instanceof Zahl)
			return (Zahl)r;
		return null;
	}
}


