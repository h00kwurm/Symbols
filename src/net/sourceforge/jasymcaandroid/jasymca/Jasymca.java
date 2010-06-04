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

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.util.Log;

public class Jasymca  implements Runnable{

	static InputStream getFileInputStream( String fname) throws IOException{
// resources in res/raw
		if(fname.startsWith("m/") && fname.endsWith(".m"))
		{
			int l = fname.length();
			String name = fname.substring(2,l-2);
			Resources R = context.getResources();
			int id = R.getIdentifier(name, "raw", context.getPackageName());
			if(id != 0)
			{
				return R.openRawResource(id);
			}
		} 
// Current dir is de filesdir!
		if(!fname.startsWith("/")) {
			File file = new File(context.getFilesDir(), fname);
			return new java.io.FileInputStream(file);
		}
		return new java.io.FileInputStream( fname );
	}

	public static ContextWrapper context;
	static OutputStream getFileOutputStream( String fname, boolean append) throws IOException{
		if(fname.indexOf('/')<0)
		{
			int mode = Context.MODE_WORLD_WRITEABLE;
			if(append) mode |= Context.MODE_APPEND;
			return context.openFileOutput(fname, mode);
		} else if(!fname.startsWith("/")) {
			fname = context.getFileStreamPath(fname).getAbsolutePath();
		}
		return new java.io.FileOutputStream( fname, append );
	}


	public void readFile(InputStream in) throws JasymcaException {
		LambdaLOADFILE.readFile(in);
	}
	
	static String JasymcaRC = "Jasymca.";

        
///


	// Jasymca = Java Symbolic Calculator
	// Syntax loosely related to GNU-Maxima
	
	// Internal Design:
	// Expressions are Lisp-lists in prefix notation
	// Elements are either parseable Lists or Algebraics
	// Variables are one of
	// --- SimpleVariable 			 = name (String) 
	// --- FunctionVariable          = function (LambdaAlgebraic) + Argument (Algebraic)
	// Algebraics are one of
	// --- Zahl.Unexakt ---> double complex
	// --- Zahl.Exakt	---> BigInteger rational complex
	// --- Polynomial 	---> Coefficients (Algebraic) + Variable
	// --- Polynomial.Constant 	---> Coefficients (Algebraic) + immutable Variable
	// --- Rational     ---> Nominator (Algebraic) + Denominator (Polynomial)
	// --- Vektor       ---> Components (Algebraic)
	// --- Matrix       ---> Vektor of Components
	
	// Environment for variables, functions
	// and operators
	// Stored by name, case-(in)sensitive
	public Environment env; 
	public Processor   proc=null;
	public Parser      pars;
	
	String ui = "Octave";
	
	// All output uses these channels
	PrintStream ps;
	InputStream is;
	
	public void interrupt(){
		if( proc != null )
			proc.set_interrupt(true);
	}
	
	public boolean isAlive() { 
		return evalLoop != null && evalLoop.isAlive();
	}

	static NumFmt fmt = new NumFmtVar( 10, 5 );	

	Thread evalLoop   = null;

	public Jasymca(){
		this( "Octave" );
	}

	public Jasymca(String ui){
		setup_ui( ui, true );
		//welcome += "Executing in "+ui+"-Mode.\n";
	}
	
	public void setup_ui( String ui, boolean clear_env ){
		if( clear_env ){
			env 	= new Environment();
		}
		if( ui != null )
			this.ui = ui;
		if( this.ui.equals("Maxima") ){
			// Setup environment
			proc	= new XProcessor( env );
			pars    = new MaximaParser( env );			
		}else if( this.ui.equals("Octave") ){
			// Setup environment
			proc	= new Processor( env );
			pars    = new OctaveParser( env );			
		}else{
			System.out.println("Mode "+this.ui+" not available.");
			System.exit(0);
		}
	}
		
			

	public void start(InputStream is, PrintStream ps){
		this.is = is;
		this.ps = ps;
		
		// Read startup file
		try{
			String fname = JasymcaRC + ui + ".rc";
			InputStream file = getFileInputStream(fname);
			LambdaLOADFILE.readFile( file );
		}catch(Exception e){
		}		
		//ps.print(welcome); Welcome used to be a private String 'Welcome to Jasymca...blah, blah, blah'
		proc.setPrintStream( ps );
		evalLoop = new Thread(this);
		evalLoop.start();
	}
	
	public void run(){
		while(true){
			//ps.print( pars.prompt() );			// Prompt
			try{
				proc.set_interrupt( false );
				List<?> code   = pars.compile(is, ps);   // check
				if( code == null  ){
					ps.println(""); // convert this to an save line.
					continue;  
				}
				if( proc.process_list( code, false ) == Processor.EXIT){
					ps.println("\nGoodbye."); // convert this to an entire termination of the program
					return;
				}
				proc.printStack();
			}catch(Exception e){
				ps.println("\n"+e); // Here is the actual error. I need to print this seperately.
				proc.clearStack();
				Log.w("Jasymca", e);
			}
		}
	}

	/**
	 * @param welcome the welcome to set
	 
	public void setWelcome(String welcome) {
		this.welcome = welcome;
	}

	/**
	 * @return the welcome
	 
	public String getWelcome() {
		return welcome;
	}
	*/
}				
