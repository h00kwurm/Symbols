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


import java.io.*;
import java.util.*;
import java.math.BigInteger;



public abstract class Parser implements Constants{
	static final Integer ONE 	= new Integer( 1 ),
						 TWO 	= new Integer( 2 ),
						 THREE	= new Integer( 3 );


	ParserState  	pst;
	Parser( Environment env ){}
	abstract void translate( String s) throws ParseException;
	abstract boolean ready();
	abstract List get() throws ParseException;

	abstract public List compile( InputStream is, PrintStream ps  ) 
						throws ParseException, IOException;
	abstract public List compile( String s ) 
						throws ParseException;

	abstract public String prompt();


	abstract List compile_expr( List expr )         throws ParseException;
	abstract List compile_statement( List expr )    throws ParseException;
	abstract List compile_lval( List expr ) 		throws ParseException;
	abstract List compile_list( List expr ) 		throws ParseException;
	abstract List compile_func( List expr )    		throws ParseException;


	abstract boolean commandq( Object expr );


	List compile_command_args( List expr ){
		List s = Comp.vec2list( new Vector() );
		for(int n=expr.size()-1; n>=0; n--){
			// protect string arguments against evaluation
			Object x = expr.get(n);
			if( x instanceof Algebraic )
				s.add( x );
			else if( symbolq(x) ){
				s.add("$"+x);
			}else if( stringq(x) ){
				s.add("$"+((String)x).substring(1));
			}else if( x instanceof Vector ){
				s.addAll( compile_command_args( (List)x ));
			}				
		}
		return s;
	}

	void reset(){
		pst = new ParserState(null, 0);
	}


	Rule[] compile_rules(String[][] s) throws ParseException{
		Rule r[] = new Rule[s.length];
		for(int i=0; i<s.length; i++){
			Rule r1 = new Rule();
			reset();
			translate( s[i][0] );
			r1.rule_in = pst.tokens;
			reset();
			translate( s[i][1] );
			r1.rule_out = pst.tokens;
			r[i] = r1;
		}
		return r;
	}
			
		
	
	List compile_command( List expr ) {
		if(expr==null || expr.size()==0 || !(commandq(expr.get(0))))
			return null;
		List s = compile_command_args(expr.subList(1,expr.size())); 
		s.add( new Integer( s.size() ));
		try{
			String command = (String)expr.get(0);
			Class<?> c 		= Class.forName( Environment.CLASS_PREFIX + "Lambda"+command.toUpperCase() );
			s.add( (Lambda)c.newInstance() );
			return s;
		}catch( Exception e ){
			return null;
		}
	}

	Vector nonsymbols = new Vector();

	boolean symbolq( Object expr ){
		return expr instanceof String && 
			   ((String)expr).length()>0  &&
			   ((String)expr).charAt(0)!=' ' &&
			   ! nonsymbols.contains( expr );
	}

	boolean stringq( Object expr ){
		return expr instanceof String && ((String)expr).length()>0 && 
			   ((String)expr).charAt(0)==' ';
	}


	// tokenizer helpers

	
	static boolean oneof( char c, String s ){
		return s.indexOf(c)!=-1;
	}
	
	static boolean oneof( Object c, String s ){
		return c instanceof String &&
			   ((String)c).length()>0 &&
			   oneof( ((String)c).charAt(0), s);
	}
	
	static boolean oneof(Object c, Object[] d){
		for(int i=0; i<d.length; i++)
			if(d[i].equals(c)) return true;
		return false;
	}
	
	static boolean whitespace(char c){
		return oneof(c," \t\n\r");
	}

	
	static void skipWhitespace( StringBuffer s ){
		int i=0;
		while( i<s.length() && 
			   whitespace(s.charAt(i))) 
			i++;
		s.delete(0,i);
	}

	static int nextIndexOf( Object x, int idx, List list ){
		int n = list.size();
		while( idx < n ){
			if( x.equals( list.get( idx )))
				return idx;
			idx++;
		}
		return -1;
	}
	
	static Zahl readNumber( StringBuffer s ) throws ParseException{
		// Create substring of s that must contain the number
		int kmax = 0;
		while(kmax<s.length() && 
			  oneof(s.charAt(kmax),"0123456789.eE+-"))
			  kmax++;
		char[] substring = new char[kmax];
		s.getChars(0,kmax,substring,0);
		String sub = new String(substring);
		// Now try parseDouble repeatedly 
		for(int k=kmax; k>0; k--){
			try{
				String ts = sub.substring(0,k);
				// Resolve ambiguities: 
				// 2.^x --> 2 .^x
				// 2./x --> 2 ./x
				double x = Double.parseDouble(ts);
				if( ts.endsWith(".") && s.length()>k &&
					(s.charAt(k)=='^' || s.charAt(k)=='/'))
					continue;
				// Check and append imaginary unit
				boolean imag = false;
				if(s.length()>k && (s.charAt(k)=='i' || 
									s.charAt(k)=='j')){
						imag = true;
						k++;
				}								
				s.delete(0,k);
				// it worked, maybe it's a biginteger?
				if( Math.abs(x) > 1e15 ){// Largest exakt integer in double format
					try{
						BigInteger bi = new BigInteger(ts,10);
						// this worked too
						return imag ?
							(Zahl) (new Exakt( bi )).mult(Zahl.IONE):
							new Exakt( bi );
					}catch(Exception e2){
					}
				}
				return imag ? new Unexakt(0,x) : new Unexakt(x);
			}catch(Exception e){
							// try k-1
			}
		}
		throw new ParseException("Internal Error.");
	}
	
		// Cut piece from StringBuffer sb until excluding character c 
	// which is deleted
	static String cutstring(StringBuffer sb, char a, char b) throws ParseException{
		sb.delete(0,1);
		String s = sb.toString();
		int cnt = 1,i;
		for(i=0; i<s.length(); i++){
			char c = s.charAt(i);
			if(a!=b && c==a)
				cnt++;
			else if(c==b)
				cnt--;
			if(cnt==0)
				break;
		}
		if(cnt!=0)
			throw new ParseException("Unclosed "+a);
		s = s.substring(0,i); 	
		sb.delete(0,i+1);
		return s;
	}

	static String[]  listsep  = { ",", ";" };

	static String[] stringops = {"=", ","};
	static boolean stringopq( Object x ){
		return oneof(x, stringops);
	}

	static boolean number(char c){
		return oneof(c,"0123456789");
	}
	
	// Some readline's don't work
	static String readLine( InputStream is ) throws IOException{
		StringBuffer sb = new StringBuffer();
		int c;
		while( (c=is.read()) != -1 ){
			sb.append((char)c);
			if(c == '\n' || c == '\r' )
				return sb.toString();
		}
		if(sb.length()>0)
			return sb.toString();
		return null;
	}


}

class ParserState{
	Object    sub;
	Object    prev;
	List      tokens;
	int    	  inList;
	ParserState(Object sub,
				int inList){
		this.sub 	= sub;
		this.prev	= null;
		this.tokens = Comp.vec2list( new Vector() );
		this.inList = inList;
	}
}
