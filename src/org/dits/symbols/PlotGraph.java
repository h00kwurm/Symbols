package org.dits.symbols;

import java.io.Serializable;
import java.util.Vector;

import android.graphics.Color;

import net.sourceforge.jasymcaandroid.jasymca.JasymcaException;

public class PlotGraph implements Serializable {
	
	static public symbols activity;
	
	public final static int LINEAR = 0;
	public final static int LOGLIN = 1;
	public final static int LINLOG = 2;
	public final static int LOGLOG = 3;
	int plotmode = LINEAR;
	double minx;
	double maxx;
	double miny;
	double maxy;
	String Xlabel;
	String Ylabel;
	String Tlabel;
	Vector<PlotLine> PlotLines = new Vector<PlotLine>();
	public transient boolean Hold_b; 
	int    ntx = 10, nty = 10;
	double  a0, a1;
	
	public PlotGraph(int plotmode) {
		reset();
		setmode(plotmode);
	}

	public void setmode( int mode ){
		if(mode != plotmode)
			reset();
		plotmode = mode;
	}

	private void reset(){
  		PlotLines.removeAllElements();
  		minx=Double.POSITIVE_INFINITY;
  		maxx=Double.NEGATIVE_INFINITY;
  		miny=Double.POSITIVE_INFINITY;
  		maxy=Double.NEGATIVE_INFINITY;
   		Xlabel  = null;
  		Ylabel  = null;
  		Tlabel  = null;
	}

	public void addLine(Object[] params) throws JasymcaException{  
		if (!Hold_b){	reset();	}	
  	
		for (int i=0;i<params.length;){
			PlotLine line=new PlotLine(); // New Line

			if (i<params.length-1 && ! (params[i+1] instanceof String)){ 
				double x[] = (double[]) params[i]; 
				if(plotmode==LOGLIN || plotmode==LOGLOG)
					log10( x );
				line.lineMaxx = Math.max(max(x),maxx);
				line.lineMinx = Math.min(min(x),minx);

				double y[] = (double[]) params[i+1];
				if(plotmode==LINLOG || plotmode==LOGLOG)
					log10( y );
				line.lineMaxy = Math.max(max(y),maxy);
				line.lineMiny = Math.min(min(y),miny);
        
              	if (x.length!=y.length)
        			throw new JasymcaException("X and Y must be same length");
                line.setPoints(x,y);
              	i+=2;
          	}else{
				double x[] = (double[]) params[i]; 
 				if(plotmode==LINLOG || plotmode==LOGLOG)
					log10( x );
             	maxx=x.length;
              	minx=1;
              	maxy = max(x);
              	miny = min(x);

              	line.x=new double[x.length];
              	for (int ind=0;ind<x.length;ind++)
        			line.x[ind]=ind+1;

	     		line.setPoints(line.x,x);
             	i++;
          	}
      
          	maxx = Math.max(line.lineMaxx,maxx);
          	minx = Math.min(line.lineMinx,minx);

          	maxy= Math.max(line.lineMaxy,maxy);
          	miny = Math.min(line.lineMiny,miny);
      
 			if (i<params.length && (params[i] instanceof String)){
 				line.setLineAttributes( params[i].toString() );
      			i++;
  	  		}
          	PlotLines.addElement(line);
		}
		setMinMax();
		//repaint();
	}

// in-place conversion
	static void log10( double[] x ) throws JasymcaException{
		for(int i=0; i<x.length; i++){
			if(x[i] <= 0.0)
				throw new JasymcaException("Log from negative number.");
			x[i] = Math.log10( x[i] );
		}
	}

	
	// decimal exponent
    static int decExp(double x){
       return (int) (Math.log10(x));
    }
 
	// return largest p=10^n with p<x
	static double  largestp10( double x){
		double p=1.0;
		while(p<x) p*= 10.0;
		while(p>x) p/= 10.0;
		return p;
	}
 
 	// Set min and max so that axis-length = n * 10^a
	// mit 1 <= n <= 10
	// also, set a1 and a0
	void setMinMax(){
      	if (maxx==minx)  maxx++;
      	double div = largestp10( maxx-minx )/10;
      	int ntx1 = (int)Math.ceil (maxx / div);
      	int ntx2 = (int)Math.floor(minx / div);
      	ntx = ntx1 - ntx2;
      	maxx = ntx1 * div;
      	minx = ntx2 * div;
 
      	if (maxy==miny)  maxy++;
      	div = largestp10( maxy-miny )/10;
      	int nty1 = (int)Math.ceil (maxy / div);
      	int nty2 = (int)Math.floor(miny / div);
      	nty = nty1 - nty2;
      	maxy = nty1 * div;
      	miny = nty2 * div;
 
		a1=0.; a0=(maxy+miny)/2.;
	}

    	

	private void repaint() {
		show();
		
	}

	public void addLineErrorbars(Object[] params) throws JasymcaException{  
		if (!Hold_b){	reset();	}	
  	
  		if( params.length<3 )
  			throw new JasymcaException("At least 3 arguments required.");

		PlotLine line=new PlotLine(); // New Line
		double x[] = (double[]) params[0]; 
		line.lineMaxx = Math.max(max(x),maxx);
		line.lineMinx = Math.min(min(x),minx);

		double y[] = (double[]) params[1];
		line.lineMaxy = Math.max(max(y),maxy);
		line.lineMiny = Math.min(min(y),miny);
        if (x.length!=y.length)
        	throw new JasymcaException("X and Y must be same length");
        line.setPoints(x,y);
 
 		double el[] = (double[]) params[2], eu[] = el; 
        if (el.length!=y.length)
        	throw new JasymcaException("Errors and Y must be same length");
        int i=3;
        if (params.length>3 && !(params[3] instanceof String)){
        	eu = (double[]) params[3];
          	if (eu.length!=y.length)
        		throw new JasymcaException("Errors and Y must be same length");
        	i++;
        }
        line.eu = eu;
        line.el = el;
        
       	maxx = Math.max(line.lineMaxx,maxx);
        minx = Math.min(line.lineMinx,minx);

        maxy= Math.max(line.lineMaxy,maxy);
        miny = Math.min(line.lineMiny,miny);
      
 		if (i<params.length && (params[i] instanceof String)){
 			line.setLineAttributes( params[i].toString() );
      			i++;
  	  	}
        PlotLines.addElement(line);

		setMinMax();
		//repaint();
	}
	
	static double max(double []x){
		double max=x[0];
		for (int i=1;i<x.length;i++){
			if (x[i]>max)
				max=x[i];
      	}
      	return max;
	}

	static double min(double []x){
		double min=x[0];
		for (int i=1;i<x.length;i++){
			if (x[i]<min)
				min=x[i];
		}
		return min;
	}
	
	public void show() {
		activity.startPlot(this);
	}
	
	public static class PlotLine implements Serializable {

		public double[] x,y;
		public double[] el;
		public double[] eu;
		public double lineMiny;
		public double lineMaxy;
		public double lineMinx;
		public double lineMaxx;
		int color = Color.BLUE;
		char marker = ' ';

		public void setPoints(double[] xp, double[] yp) {
			x=xp; y=yp;
			lineMaxx = max(x);
			lineMinx = min(x);
			lineMaxy = max(y);
			lineMiny = min(y);
		}
		public void setLineAttributes(String options){
       		for (int i=0;i< options.length();i++){
       			switch( options.charAt(i) ){
       				case 'r': color = Color.RED; 	break;
       				case 'g': color = Color.GREEN; 	break;
       				case 'b': color = Color.BLUE; 	break;
       				case 'y': color = Color.YELLOW; break;
       				case 'm': color = Color.MAGENTA;break;
       				case 'c': color = Color.CYAN; 	break;
       				case 'w': color = Color.WHITE; 	break;
       				case 'k': color = Color.BLACK; 	break;
       				default: marker = options.charAt(i);
       			}
       		}
       	}
		
	}

	public void setVisible(boolean b) {
		if(b)
			show();
		
	}
    public void setXlabel( String s ){
    	Xlabel = s;
    	repaint();
    }
    
    public void setYlabel( String s ){
    	Ylabel = s;
     	repaint();
    }

    public void setTlabel( String s ){
    	Tlabel = s;
     	repaint();
    }
	
}
