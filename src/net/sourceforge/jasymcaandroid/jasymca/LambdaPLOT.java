package net.sourceforge.jasymcaandroid.jasymca;

import java.util.Stack;

import org.dits.symbols.PlotGraph;

public class LambdaPLOT extends Lambda {
	public static PlotGraph pg;
	public int lambda(Stack st) throws ParseException, JasymcaException{
		return plotArgs(PlotGraph.LINEAR, st);
	}
	
	int plotArgs( int plotmode, Stack st )throws  ParseException, JasymcaException{
		if( pg==null ){
			pg = new PlotGraph(plotmode);
		}else
			pg.setmode(plotmode);
		int narg = getNarg( st );
		Object pargs[] = new Object[narg];
		for(int i=0; i<narg; i++){
			Object x = st.pop();
			if( x instanceof Vektor ){
				x = new ExpandConstants().f_exakt((Vektor)x);
				pargs[i] = ((Vektor)x).getDouble();	
			}else
				pargs[i] = x;	
		}
		pg.addLine( pargs );

		pg.show();

		return 0;
	}		
}
class LambdaERRORBAR extends LambdaPLOT{
	public int lambda(Stack st) throws ParseException, JasymcaException{
		return plotArgs(PlotGraph.LINEAR, st);
	}
	
	int plotArgs( int plotmode, Stack st )throws  ParseException, JasymcaException{
		if( LambdaPLOT.pg==null ){
			pg = new PlotGraph(plotmode);
		}else
			pg.setmode(plotmode);
		int narg = getNarg( st );
		Object pargs[] = new Object[narg];
		for(int i=0; i<narg; i++){
			Object x = st.pop();
			if( x instanceof Vektor ){
				x = new ExpandConstants().f_exakt((Vektor)x);
				pargs[i] = ((Vektor)x).getDouble();	
			}else
				pargs[i] = x;	
		}
		pg.addLineErrorbars( pargs );
		pg.show();
		return 0;
	}		
}

class LambdaLOGLOG extends LambdaPLOT{
	public int lambda(Stack args) throws ParseException, JasymcaException{
		return plotArgs(PlotGraph.LOGLOG, args);
	}
}
class LambdaSEMILOGX extends LambdaPLOT{
	public int lambda(Stack args) throws ParseException, JasymcaException{
		return plotArgs(PlotGraph.LOGLIN, args);
	}
}
class LambdaSEMILOGY extends LambdaPLOT{
	public int lambda(Stack args) throws ParseException, JasymcaException{
		return plotArgs(PlotGraph.LINLOG, args);
	}
}

class LambdaHOLD extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{
		int narg = getNarg( st );
		if(LambdaPLOT.pg == null){
			while(narg-- > 0)
				st.pop();
			if(pc.ps != null){
				pc.ps.println( "No plot to hold." );
			}
			return 0;
		}
		if( narg == 0 ){
			LambdaPLOT.pg.Hold_b = !LambdaPLOT.pg.Hold_b;
		}else{
			Object arg = st.pop();
			if( "$on".equals( arg.toString() )) {
				LambdaPLOT.pg.Hold_b = true;
			}else if( "$off".equals( arg.toString() )) {
				LambdaPLOT.pg.Hold_b = false;
			}else{
				throw new JasymcaException("Invalid argument to hold.");
			}
		}
		if( !LambdaPLOT.pg.Hold_b ) 
			LambdaPLOT.pg.setVisible(false);
		if(pc.ps != null){
			pc.ps.println( "Current plot "+(LambdaPLOT.pg.Hold_b?"held.":"released."));
		}
		return 0;
	}
}

class LambdaXLABEL extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{
		int narg = getNarg( st );
		Object arg = st.pop();
		if( !(arg instanceof String ))
			throw new JasymcaException("Argument must be string.");
		if(LambdaPLOT.pg != null)
				LambdaPLOT.pg.setXlabel( (String)arg );
		return 0;
	}
}

class LambdaYLABEL extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{
		int narg = getNarg( st );
		Object arg = st.pop();
		if( !(arg instanceof String ))
			throw new JasymcaException("Argument must be string.");
		if(LambdaPLOT.pg != null)
				LambdaPLOT.pg.setYlabel( (String)arg );
		return 0;
	}
}
class LambdaTITLE extends Lambda{
	public int lambda(Stack st) throws ParseException, JasymcaException{
		int narg = getNarg( st );
		Object arg = st.pop();
		if( !(arg instanceof String ))
			throw new JasymcaException("Argument must be string.");
		if(LambdaPLOT.pg != null)
				LambdaPLOT.pg.setTlabel( (String)arg );
		return 0;
	}
}
