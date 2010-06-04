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


class OctaveParser extends Parser{
	Lambda CRV = new CreateVector(); // [	
	Lambda REF = new REFM(); 
	
	
	int IN_PARENT   = 1;
	int IN_BRACK    = 2;
	int IN_BLOCK    = 4;

	Rule rules[];
	
	String rules_in[][] = {
		{ "function  y = f  X end", 		"X f y 3 @FUNC"	},
		{ "if u X else Y end" 			, "Y X u 3 @BRANCH"	},
		{ "if u X end" 					, "X u 2 @BRANCH" 	},
		{ "for u X end" 			    , "X u 2 @FOR"   	},
		{ "while u X end" 				, "X u 2 @WHILE" 	},
		};
	
	String commands[] = {
			"format", "hold", "syms" , "clear" , "addpath"
		};
	
	public OctaveParser(  Environment env ){
		super(env);
		// create globals in env
		env.addPath( "." );
		env.addPath( "m" );
		
		// set globals
		env.globals.put("pi", Zahl.PI);
		env.globals.put("i" , Zahl.IONE);
		env.globals.put("j" , Zahl.IONE);
		env.globals.put("eps", new Unexakt( 2.220446049250313E-16 ));
		env.globals.put("ratepsilon", new Unexakt(2.0e-8));
		env.globals.put("algepsilon", new Unexakt(1.0e-8));
		env.globals.put("rombergit",  new Unexakt(11));
		env.globals.put("rombergtol", new Unexakt(1.0e-4));		
		
		
		pst = new ParserState(null, 0);
		
		Operator.OPS = new Operator[]{
			new Operator( "POW", ".**", 1, LEFT_RIGHT, BINARY ),
			new Operator( "PPR", "++", 1, RIGHT_LEFT, UNARY| LVALUE ),
			new Operator( "MMR", "--", 1, RIGHT_LEFT, UNARY| LVALUE ),
			new Operator( "PPL", "++", 1, LEFT_RIGHT, UNARY| LVALUE ),
			new Operator( "MML", "--", 1, LEFT_RIGHT, UNARY| LVALUE ),
			new Operator( "ADE", "+=",10, RIGHT_LEFT, BINARY| LVALUE ),
			new Operator( "SUE", "-=",10, RIGHT_LEFT, BINARY| LVALUE ),
			new Operator( "MUE", "*=",10, RIGHT_LEFT, BINARY| LVALUE ),
			new Operator( "DIE", "/=",10, RIGHT_LEFT, BINARY| LVALUE ),
			new Operator( "MPW", "**",  1, LEFT_RIGHT, BINARY ),
			new Operator( "MUL", ".*", 3, LEFT_RIGHT, BINARY ),
			new Operator( "DIV", "./", 3, LEFT_RIGHT, BINARY ),
			new Operator( "POW", ".^", 1, LEFT_RIGHT, BINARY ),
			new Operator( "EQU", "==", 6, LEFT_RIGHT, BINARY ),
			new Operator( "NEQ", "~=", 6, LEFT_RIGHT, BINARY ),
			new Operator( "GEQ", ">=", 6, LEFT_RIGHT, BINARY ),
			new Operator( "LEQ", "<=", 6, LEFT_RIGHT, BINARY ),
			new Operator( "TRN", ".'",  1, RIGHT_LEFT, UNARY ),
			new Operator( "GRE", ">",  6, LEFT_RIGHT, BINARY ),
			new Operator( "LES", "<",  6, LEFT_RIGHT, BINARY ),
			new Operator( "OR",  "|",  9, LEFT_RIGHT, BINARY ),
			new Operator( "NOT", "~",  8, LEFT_RIGHT, UNARY ),
			new Operator( "AND", "&",  7, LEFT_RIGHT, BINARY ),
			new Operator( "GRE", ">",  6, LEFT_RIGHT, BINARY ),
			new Operator( "GRE", ">",  6, LEFT_RIGHT, BINARY ),
			new Operator( "ASS", "=", 10, RIGHT_LEFT, BINARY| LVALUE ),
			new Operator( "CR1", ":",  5, LEFT_RIGHT, BINARY| TERNARY ),
			new Operator( "ADD", "+",  4, LEFT_RIGHT, UNARY | BINARY ),
			new Operator( "SUB", "-",  4, LEFT_RIGHT, UNARY | BINARY ),
			new Operator( "MMU", "*",  3, LEFT_RIGHT, BINARY ),
			new Operator( "MDR", "/",  3, LEFT_RIGHT, BINARY ),
			new Operator( "MDL", "\\", 3, LEFT_RIGHT, BINARY ),
			new Operator( "MPW", "^",  1, LEFT_RIGHT, BINARY ),
			new Operator( "ADJ", "'",  1, RIGHT_LEFT, UNARY  )
		};	

		// Set up hashtable for symbol identification
		for( int i=0; i<Operator.OPS.length; i++){
			nonsymbols.addElement( Operator.OPS[i].symbol );
		}
		for( int i=0; i<listsep.length; i++){
			nonsymbols.addElement(listsep[i] );
		}
		for( int i=0; i<commands.length; i++){
			nonsymbols.addElement(commands[i]);
		}
		for( int i=0; i<keywords.length; i++){
			nonsymbols.addElement(keywords[i]);
		}	
		// Compile set of rules
		try{
			rules = compile_rules( rules_in );
		}catch(ParseException p){
			// this should never happen in userland
		}
		// Ugly, but no easy way around
		Lambda.pr = this;
	}

	public String prompt(){
		return ">> ";
	}


	public List compile( InputStream is, PrintStream ps ) 
						throws ParseException, IOException{
		String s, sp = null;
		reset();
		while( (s=readLine(is)) != null ){ // EOF
			sp = s;
			translate(s);
			if( ready() )
				break;
			else{
				if(ps != null)
					ps.print("> "); // this needs to be different
			}
		}
		if(sp == null)
			return null;
		// 'end' - appending reader
		if( s==null && pst.inList == IN_BLOCK){
			List v = pst.tokens;
			pst = (ParserState)pst.sub;
			pst.tokens.add(v);
		}	
		return get();
	}

	public List compile( String s ) 
						throws ParseException{
		reset();
		translate(s);
		return get();
	}

	List get() throws ParseException{
		List r = pst.tokens;
		//System.out.println("in:"+r);
		//long t = System.currentTimeMillis();
		List pgm = compile_statement( r  );
		//System.out.println("pgm:"+pgm+"\n Compile Time:"+((System.currentTimeMillis()-t)/1000.)+"sec.");
		if( pgm != null )
			return pgm;
		throw new ParseException("Compilation failed.");
	}
	
	void translate( String s ) 
		throws ParseException{
		if( s==null ) return;
		StringBuffer sb = new StringBuffer(s);
		Object t;
		while( (t=nextToken(sb)) != null ){
			pst.tokens.add(t);
			pst.prev   = t;
		}
	}
	
	
	
	
	
	static  String FUNCTION = "function",
				   FOR 		= "for", 
				   WHILE	= "while",
				   IF		= "if",
				   ELSE		= "else",
				   END		= "end",
				   BREAK	= "break",
				   RETURN	= "return",
				   CONTINUE = "continue",
				   EXIT		= "exit";
	private String[]  keywords = { FUNCTION, FOR, WHILE, IF, ELSE, END, 
								   BREAK, RETURN, CONTINUE, EXIT };
	private String 	  sepright = ")]*/^!,;:=.<>'\\";
	private String 	  sepleft  = "*/^!,;:=.<>'\\+-";



	boolean refq( Object expr ){
		return expr instanceof String && ((String)expr).length()>0 && 
			   ((String)expr).charAt(0)=='@';
	}

	boolean commandq( Object x ){
		return oneof( x, commands );
	}
	
	boolean operatorq( Object expr ){
		return Operator.get(expr)!=null;
	}			





	public Object nextToken(StringBuffer s) 
					        throws ParseException{
		if( pst.inList == IN_BRACK &&
			pst.prev != null       &&
			! oneof( pst.prev, sepleft ) ){
			int k=0;
			for( ; k<s.length() &&
				   whitespace(s.charAt(k)); k++ );
			if( k == s.length() ){	
				s.delete(0,k);
				return ";";
			}else if( k > 0 ){
				char c = s.charAt(k);
				if( c=='+' || c=='-' ){
					if( s.length()>k+1 && !whitespace(s.charAt(k+1)) ){
						s.delete(0,k);
						return ",";
					}
				}else if( !oneof(s.charAt(k),sepright) ) {
					s.delete(0,k);
					return ",";
				}
			}
		}
		if( pst.inList == IN_BLOCK &&
			pst.prev != null       &&
			! oneof( pst.prev, listsep ) ){
			int k=0;
			for( ; k<s.length() &&
				   whitespace(s.charAt(k)); k++ );
			if( k == s.length() ){	
				s.delete(0,k);
				return ",";
			}
		}
		skipWhitespace( s );
		if( s.length() < 1 )
			return null;
		char c0 = s.charAt(0);
		
		switch( c0 ){
			case '"': // string literal
				return ' '+cutstring(s,'"','"');
			case '(': // list
				if( symbolq( pst.prev ) ){
					pst.prev = "@"+pst.prev;
					pst.tokens.remove( pst.tokens.size()-1 );
					pst.tokens.add( pst.prev );
				}
				pst = new ParserState( pst, IN_PARENT);
				return nextToken(s.delete(0,1));
			case ')': // list
				if( pst.inList != IN_PARENT  )
					throw new ParseException("Wrong parenthesis.");				
				List t = pst.tokens;
				pst = (ParserState)pst.sub;
				s.delete(0,1);
				return t;				
			case '[': // vektor 
				pst = new ParserState( pst, IN_BRACK);
				return nextToken(s.delete(0,1));
			case ']':
				if( pst.inList != IN_BRACK  )
					throw new ParseException("Wrong brackets.");
				t = pst.tokens;
				// remove trailing ';'s
				while( t.size()>0 && ";".equals( t.get( t.size()-1 ))){
					t.remove( t.size()-1 );
				}
				t.add( 0, "[" );
				pst = (ParserState)pst.sub;
				s.delete(0,1);
				return t;
			case '%':
			case '#':
				s.delete(0, s.length());
				return null;
			case '\'': // string literal
				if( pst.prev==null || stringopq( pst.prev ) )
					return ' '+cutstring(s,'\'','\'');
				else
					return readString( s );
			case ';': case ',':
				s.delete(0,1);
				return ""+c0;
			case '0': case '1': case '2': case '3':
			case '4': case '5': case '6': case '7':
			case '8': case '9': 
				return readNumber( s );
			case '.':
				if( s.length()>1 && number(s.charAt(1)) )
					return readNumber( s );
				else
					return readString( s );
			default : 
				return readString( s );
		}	
	}

	boolean ready(){
		return pst.sub == null;
	}

	private String separator = "()[]\n\t\r +-*/^!,;:=.<>'\\&|";


	Object readString( StringBuffer s )throws ParseException{
		// Operator ? maximum length = 3 (.**)
		int len = s.length()>2?3:s.length();
		char[] substring = new char[len];
		s.getChars(0,len,substring,0);
		String st = new String(substring);
		Operator op = Operator.get( st );
		if(op != null){
			s.delete(0, op.symbol.length());
			return op.symbol;
		}
		// Symbol
		int k=1;
		while(k<s.length() && !oneof(s.charAt(k), separator)) k++;
		substring = new char[k];
		s.getChars(0,k,substring,0);
		String t = new String( substring );
		s.delete(0,k);
		
		// Keyword
		if( t.equals(IF) || t.equals(FOR) || t.equals(WHILE) ||
			t.equals(FUNCTION) ){
			if(pst.inList == IN_PARENT || pst.inList == IN_BRACK)
				throw new ParseException("Block starts within list.");
			pst.tokens.add(t);
			pst = new ParserState( pst, IN_BLOCK );
			return nextToken(s);
		}
		if( t.equals(ELSE) ){
			if(pst.inList != IN_BLOCK)
				throw new ParseException("Orphaned else.");
			List v = pst.tokens;
			((ParserState)pst.sub).tokens.add( v );
			pst = new ParserState( pst.sub, IN_BLOCK );
			return ELSE;				
		}
		if( t.equals(END) ){
			if(pst.inList != IN_BLOCK)
				throw new ParseException("Orphaned end.");
			List v   = pst.tokens;
			pst = (ParserState)pst.sub;
			return v;				
		}
		return t;						
	}	

	
	
	
	List compile_unary( Operator op, List expr )throws ParseException{
		List  arg_in = ( op.left_right() ? 
						 expr.subList( 1, expr.size() ) :
						 expr.subList( 0, expr.size()-1 ));
		List  arg = (op.lvalue() ? compile_lval( arg_in ) :
                                   compile_expr( arg_in ));
		if( arg==null) 
			return null;
		arg.add( ONE );
		arg.add( op.getLambda() );
		return arg;
	}

	List compile_ternary( Operator op, List expr, int k )throws ParseException{
		int n = expr.size();
		for(int k0 = k-2; k0>0; k0--){
			if( op.symbol.equals( expr.get(k0) )){
				List left_in = expr.subList( 0, k0 ) ;
				List left = compile_expr( left_in );
				if( left==null) 
					continue;
				List mid_in = expr.subList( k0+1, k ); 
				List mid = compile_expr( mid_in );
				if( mid==null) 
					continue;
				List right_in = expr.subList( k+1, expr.size() );
				List right = compile_expr( right_in );
				if( right==null) 
					continue;
				left.addAll( mid );
				left.addAll( right);
				left.add( THREE );
				left.add( op.getLambda() );
				return left;	
			}
		}
		return null;
	}
		
	List compile_binary( Operator op, List expr, int k )throws ParseException{
		List left_in = expr.subList(0, k) ;
		List left    = (op.lvalue() ? compile_lval( left_in ) :
                                      compile_expr( left_in ));
		if( left==null) 
			return null;;
		List right_in = expr.subList( k+1, expr.size() );
		List right    = compile_expr( right_in );
		if( right==null) 
			return null;

		Integer nargs = TWO;
		if(op.lvalue()){
			Object left_narg = left.get( 0 );
			if( left_narg instanceof Integer){
				nargs = (Integer)left_narg;
				right.add( right.size()-1, "#"+nargs );
				left.remove(0);
			}else{
				nargs = ONE;
			}
		}
		left.addAll( right );
		left.add( nargs );
		left.add( op.getLambda() );
		return left;
	}
	
	List translate_op( List expr ) throws ParseException{
		List s;
		int n = expr.size();
		
		for(int pred = 10; pred >= 0; pred--){
			for(int i=0; i<n; i++){
				int k=i;
				if(pred!=6)
					k=n-i-1;
				Operator op =  Operator.get( expr.get(k), 
								k==0 ? Operator.START : (k==n-1 ?Operator.END : Operator.MID)) ;
				if(op==null || op.precedence != pred )
					continue;
				if( op.unary() && ( (k==0   &&  op.left_right()) ||
									(k==n-1 && !op.left_right()))) {
					s = compile_unary( op, expr );
					if( s!=null )
						return s;
					else
						continue;
				}
				if(k>2 && k<n-1 && op.ternary() ){
					s = compile_ternary( op, expr, k );
					if( s!=null )
						return s;
				}
				if(k>0 && k<n-1 && op.binary() ){
					s = compile_binary( op, expr, k );
					if( s!=null )
						return s;
				}
			}
		}
		return null;
	}
			

	

	List compile_vektor( List expr ) throws ParseException{
		if(expr==null || expr.size()==0 || !"[".equals(expr.get(0)) )
			return null;
		expr = expr.subList( 1, expr.size() );			
		List r = Comp.vec2list( new Vector() );
		int i=0,ip=0, nrow=1;
		while( (i=nextIndexOf(";",ip,expr))!=-1){//    
			List x 	= expr.subList( ip, i);
			List xs = compile_list( x );
			if( xs==null )
				return null;
			xs.addAll( r );
			r = xs;
			nrow++;
			ip=i+1;
		}
		List x 	= expr.subList( ip, expr.size());
		List xs = compile_list( x );
		if( xs==null )
			return null;
		xs.addAll( r );
		r = xs;
		r.add( new Integer( nrow ) );
		r.add( CRV );
		return r;
	}

	List compile_list( List expr ) throws ParseException{
		/*	<list> ::=  <expr> 
					 |  <expr> , <list>
		*/
		if(expr==null)
			return null;
		List r = Comp.vec2list( new Vector() );
		if(expr.size()==0){			
			r.add(new Integer(0));
			return r;
		}
		int i, ip=0, n = 1;
		while( (i=nextIndexOf(",",ip,expr))!=-1){//    
			List  x = expr.subList( ip, i);
			List xs = compile_expr( x );
			if( xs==null )
				return null;
			xs.addAll( r );
			r = xs;
			n++;
			ip=i+1;
		}
		List x  = expr.subList( ip, expr.size());
		List xs = compile_expr( x );
		if( xs==null )
			return null;
		xs.addAll( r );
		r = xs;
		r.add( new Integer( n ));
		return r;
	}


	List compile_lval( List expr ) throws ParseException{
		/*  <lvalue> ::=   <lvalue1>
						| [ <lvalue1>, <lvalue1>, <lvalue1>, ...]
		*/		
		if(expr==null || expr.size()==0) return null;
		List r = compile_lval1( expr );
		if( r!=null )
			 return r;
		if( expr.size()==1 ){
			if( expr.get(0) instanceof List )
				return compile_lval((List)expr.get(0));
			else
				return null;
		}
		if( !"[".equals( expr.get(0)) )
			return null;
		expr = expr.subList(1, expr.size());
		
		r = Comp.vec2list( new Vector() );
		int i, n = 1;
		while( (i=expr.indexOf(","))!=-1 ) {
			List x  = expr.subList( 0, i);
			List xs = compile_lval1( x );
			if( xs==null )
				return null;
			xs.addAll( r );
			r = xs;
			expr = expr.subList( i+1, expr.size() );
			n++;
		}
		List xs = compile_lval1( expr );
		if( xs==null )
			return null;
		xs.addAll( r );
		r = xs;
		r.add( 0, new Integer( n ) );
		return r;
	}

	
	List compile_lval1( List expr ) throws ParseException{
		/*  <lvalue1> ::=   <symbol>
						 |  <symbol> <index>
						 | (<lvalue1>)
		*/		
		if( expr == null ) return null;
		switch( expr.size() ){
			case 1:	// Solitary token
				Object x = expr.get(0);
				if( x instanceof List )
					return compile_lval1((List)x);
				if( symbolq(x) && !refq(x) ){
					List s = Comp.vec2list( new Vector() );
					s.add( "$"+x );
					return s;
				}
				return null;
			case 2: 
				x = expr.get(0);
				if( !symbolq(x) || !refq(x) || !(expr.get(1) instanceof List) )
					return null;
				List ref = compile_index( (List)expr.get(1) );
				if( ref == null )
					return null;
				ref.add( "$"+((String)x).substring(1) );
				return ref;
			default: return null;
		}
	}

 	List compile_index( List expr ) throws ParseException{
		/*  <index> ::=   <expr>
					|    :
					| <expr>,<expr>
					|    :  ,<expr>
					| <expr>,  :
					|    :  ,  :
		*/		
		if(expr==null || expr.size()==0)
			return null;

		if(expr.size() == 1 && ":".equals(expr.get(0)) ){	
				List s = Comp.vec2list( new Vector() );
				s.add(":");
				s.add(ONE);
				return s;
		}
		List r = compile_expr( expr );
		if(r!=null){
			r.add( ONE );
			return r;
		}
			
		int c = expr.indexOf(",");
		if( c== -1)
			return null;

		List left_in  = expr.subList(0, c);
		List right_in = expr.subList(c+1, expr.size());
		
		if( left_in != null && left_in.size()==1 && ":".equals(left_in.get(0)) ) {
			if( right_in != null && right_in.size()==1 && ":".equals( right_in.get(0) ) ){
				List s = Comp.vec2list( new Vector());
				s.add( ":" );
				s.add( ":" );
				s.add( TWO );
				return s;
			}else{
				List right = compile_expr( right_in );
				if(right==null)
					return null;
				right.add( ":" );
				right.add( TWO );
				return right;
			}
		}else{
			List left = compile_expr( left_in );
			if(left==null)
				return null;
			if( right_in != null && right_in.size()==1 && ":".equals( right_in.get(0) ) ){
				left.add( 0, ":" );
				left.add( TWO );
				return left;
			}else{
				List right = compile_expr( right_in );
				if(right==null)
					return null;
				right.addAll( left );
				right.add( TWO );
				return right;
			}
		}
	}


		

	 List compile_statement( List expr_in ) throws ParseException{
		/*  <statement> ::=   <expr>
						|     <command>
						|     if <expr> <statement> end
						|     <statement>;<statement>
						|     <statement>,<statement>
		*/
		if( expr_in == null ){
			return null;
		}
		if( expr_in.size()==0) 
			return Comp.vec2list(new Vector());
		List   expr  = Comp.clonelist( expr_in );
		Object first = expr.get(0);
		
		for( int i=0; i<rules.length; i++ ){
			Rule r = rules[i];
			if( r.rule_in.get(0).equals( first )  &&
				expr.size() >= r.rule_in.size() ){
				Compiler c    = new Compiler( r.rule_in, r.rule_out, this );
				List expr_sub = expr.subList(0, r.rule_in.size());
				List s = c.compile( expr_sub );
				if( s!=null ){
					Comp.clear( expr, 0, expr_sub.size());
					// expr_sub.clear();
					if(expr.size() == 0)
						return s;
					List t = compile_statement( expr );
					if( t==null)
						return null;
					s.addAll( t );
					return s;
				}
			}
		}
		if( commandq( first ) ){
			List expr_sub = expr;
			int is = expr.indexOf(";");
			if( is>0 )
				expr_sub = expr.subList(0, is+1);
			List s = compile_command( expr_sub );
			if( s!=null ){
				if( is>0 ){
					s.add( "#;" );
					Comp.clear(expr, 0, is+1);
					List t = compile_statement( expr );
					if( t==null)
						return null;
					s.addAll( t );
				}
				return s;
			}
			return null;
		}
		
		String lend = null;
		int ic = expr.indexOf(",");
		int is = expr.indexOf(";");
		if( ic>=0 && (ic<is || is==-1) ){
			lend ="#,";
		}else if( is>=0 && (is<ic || ic==-1) ){
			lend ="#;";
			ic = is;
		}
		if(ic==0){
			Comp.clear(expr,0,1);
			return compile_statement( expr );
		}
		if(lend != null){
			List expr_sub = expr.subList(0, ic);
			List s = compile_expr( expr_sub );
			if( s!=null ){
				s.add( lend );
				Comp.clear(expr, 0, ic+1);
				if(expr.size() == 0)
					return s;
				List t = compile_statement( expr );
				if( t==null)
					return null;
				s.addAll( t );
				return s;
			}
		}else{
			return compile_expr( expr );
		}
		return null;
	}

	
	String compile_keyword( Object x ){
		if( x.equals(BREAK) )
			return "#brk";
		else if(x.equals(CONTINUE))
			return "#cont";
		else if(x.equals(EXIT))
			return "#exit";
		else if(x.equals(RETURN))
			return "#ret";
		return null;
	}
	
	List compile_func( List expr ) throws ParseException{
		/* <@symbol> <list> */
		if( expr.size()==2 ){
			Object op     = expr.get(0);
			Object ref_in = expr.get(1);
			if( symbolq( op )  && 
				refq( op ) &&
				ref_in instanceof List ) {
				List ref = compile_list( (List)ref_in );	
				if( ref != null ) {
					ref.add( op );
					return ref;
				}
			}
		}
		return null;
	}
		
	
	 List compile_expr( List expr ) throws ParseException{
		if( expr == null || expr.size()==0) return null;
		/*  <expr> ::=   <Algebraic>
					|    <Stringliteral>
					|    <symbol>
					|    <vektor>
					|    (<expr>)
					|    <symbol> <list>
					|    <symbol> <index>
					|    <op1> <expr>
					|    <expr> <op3> <expr> <op3> <expr>
					|    <expr> <op2> <expr>
		*/


		if( expr.size()==1 ){
			Object x = expr.get(0);
			if( x instanceof Algebraic ){
				List s = Comp.vec2list(new Vector());
				s.add( x );
				return s;
			}
			
			if( x instanceof String ){
				Object y = compile_keyword( x );
				if( y!= null ){
					List s = Comp.vec2list(new Vector());
					s.add( y );
					return s;
				}
				if( stringq( x ) ) { 
					List s = Comp.vec2list( new Vector());
					s.add( x );
					return s;
				}
				if( symbolq(x)  ) {
					if( refq(x) ) // Functionhandle
						x = "$"+ ((String)x).substring(1);
					List s = Comp.vec2list( new Vector());
					s.add( x );
					return s;
				}
				return null;
			}
			if( x instanceof List ){
				List xs = compile_vektor( (List)x );
				if( xs!=null )
					return xs;
				return compile_expr( (List)x );
			}
		}
		List res = compile_func( expr );
		if( res!=null)
			return res;

		res = translate_op( expr );
		if(res != null)
			return res;


		// <expr><index>
		Object ref_in = expr.get(expr.size()-1);
		if( ! (ref_in instanceof List) )
			return null;
		List ref = compile_index( (List)ref_in );
		if( ref==null )
			return null;
		List left_in = expr.subList(0,expr.size()-1);
		if( left_in.size()==1 
			&& symbolq(left_in.get(0))
			&& refq(left_in.get(0))){
			ref.addAll(left_in);
			return ref;
		}
		List left = compile_expr( left_in );
		if(left != null){
			ref.addAll(left);
			ref.add( TWO );
			ref.add( REF );
			return ref;
		}
		
		return null;
	}
				
}


