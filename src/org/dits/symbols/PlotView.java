package org.dits.symbols;

import java.util.Iterator;
import java.util.Vector;

import net.sourceforge.jasymcaandroid.jasymca.NumFmt;
import net.sourceforge.jasymcaandroid.jasymca.NumFmtVar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.Paint.Style;
import android.graphics.Path.Direction;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.graphics.drawable.shapes.RectShape;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class PlotView extends View {

	private int xleft, xright, ytop, ybottom;
	private ColorDrawable bg;
	private Vector<ShapeDrawable> lines;
	private ShapeDrawable line, axis;
	int width;
	int height;

	public PlotView(Context context) {
		super(context);
		bg = new ColorDrawable(Color.LTGRAY);
		xleft = 20  +10;
		xright = 20;
		ytop = 20;
		ybottom = 20;
	
		RectShape rs = new RectShape();
		axis = new ShapeDrawable(rs);
		Paint paint = axis.getPaint();
		paint.setColor(Color.BLACK);
		paint.setStyle(Paint.Style.STROKE);
		
		lines = new Vector<ShapeDrawable>();
		
	}
	
	String xlabel, ylabel;

	@Override
	protected void onDraw(Canvas canvas) {
		bg.draw(canvas);
		for (Iterator<ShapeDrawable> iterator = lines.iterator(); iterator.hasNext();) {
			ShapeDrawable line = iterator.next();
			line.draw(canvas);
		}
		axis.draw(canvas);

		paint.setColor(Color.BLACK);
		paint.setTextSize(14);
		paint.setTypeface(Typeface.MONOSPACE);
			
		paint.setTextAlign(Align.CENTER);
		drawOrnaments(canvas, paint);

		paint.setTextAlign(Align.LEFT);
		if(pointerMode != PMODE_POINT)
			drawStraightLine(canvas);
		if(pointerMode != PMODE_LINE && xp>=0)
			drawPointer(canvas);
	}

	NumFmt fmt = new NumFmtVar(10, 4);

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		width = w;
		height = h;
		bg.setBounds(0, 0, w, h);
		for (Iterator<ShapeDrawable> iterator = lines.iterator(); iterator.hasNext();) {
			ShapeDrawable line = iterator.next();
			line.setBounds(xleft,ytop, w-xright,h-ybottom);			
		}
		axis.setBounds(xleft,ytop, w-xright,h-ybottom);
		
	}

	public void addLine(double[] x, double[] y, int color) {
		Path path = new Path();
		int len = Math.min(x.length, y.length);
		for(int i = 0; i < len; i++)
		{
			float xi = (float) x[i];
			float yi = (float) y[i];
			if(i == 0)
				path.moveTo(xi, yi);
			else
				path.lineTo(xi, yi);
		}

		path.offset(-rect.left, -rect.top);
		PathShape shape = new PathShape(path, rect.right-rect.left, rect.bottom-rect.top);
		line = new ShapeDrawable(shape);
		line.getPaint().setColor(color);
		line.getPaint().setStyle(Paint.Style.STROKE);
		line.setBounds(xleft,ytop, width-xright,height-ybottom);			
		lines.add(line);
	}

	public void addErrorBar(double[] x, double[] y, double[] eu, double[] el, int color) {
		Path path = new Path();
		int len = Math.min(x.length, y.length);
		float delta = rect.right - rect.left;
		delta /= 100; // TODO tuning
		for(int i = 0; i < len; i++)
		{
			float xi = (float) x[i];
			float yi = (float) y[i];
			float eui = (float)eu[i];
			float eli = (float)el[i];
			path.moveTo(xi, yi+eui);
			path.lineTo(xi, yi-eli);
			path.moveTo(xi-delta, yi+eui);
			path.lineTo(xi+delta, yi+eui);
			path.moveTo(xi-delta, yi-eli);
			path.lineTo(xi+delta, yi-eli);
		}
		path.offset(-rect.left, -rect.top);
		PathShape shape = new PathShape(path, rect.right-rect.left, rect.bottom-rect.top);
		line = new ShapeDrawable(shape);
		line.getPaint().setColor(color);
		line.getPaint().setStyle(Paint.Style.STROKE);
		line.setBounds(xleft,ytop, width-xright,height-ybottom);			
		lines.add(line);
		
	}

	private RectF rect  = new RectF();
	public void setMinMax(double left, double right, double bottom, double top)
	{
		rect.bottom = (float)bottom;
		rect.top    = (float)top;
		rect.left   = (float)left;
		rect.right  = (float)right;
		Path path = new Path();
		if(bottom < 0 && top > 0)
		{
			path.moveTo(rect.left, 0f);
			path.lineTo(rect.right, 0f);
		} 
		if(left < 0 && right > 0)
		{
			path.moveTo(0f, rect.top);
			path.lineTo(0f, rect.bottom);
		}
		if(!path.isEmpty())
		{
			path.offset(-rect.left, -rect.top);
			PathShape shape = new PathShape(path, rect.right-rect.left, rect.bottom-rect.top);
			line = new ShapeDrawable(shape);
			line.getPaint().setColor(Color.WHITE);
			line.getPaint().setStyle(Paint.Style.STROKE);
// TODO 5,5 is dependent on scaling!
			//line.getPaint().setPathEffect(new DashPathEffect(new float[]{5,5}, 1));
			line.setBounds(xleft,ytop, width-xright,height-ybottom);			
			lines.add(line);
		}
		a1=0.; a0=(top+bottom)/2.;
		xp = yp = Float.NEGATIVE_INFINITY;

	}
	
	public void clear() {
		lines.clear();
	}

	double hitX = Double.NaN;
	double hitY = Double.NaN;

	private int plotmode = PlotGraph.LINEAR;
	
	private Paint paint = new Paint();
	private int ntx = 10;
	private int nty = 10;
	
	/* (non-Javadoc)
	 * @see android.view.View#onTouchEvent(android.view.MotionEvent)
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		Log.d("onTouchEvent", "Touch " + event.getAction() + " x=" + event.getX() + " y=" + event.getY());
		xp = event.getX();
		yp = event.getY();
		hitX = getXCoordinate(xp);
		hitY = getYCoordinate(yp);
		if(plotmode == PlotGraph.LOGLIN || plotmode == PlotGraph.LOGLOG)
			hitX =  Math.pow(10.0, hitX);
		if(plotmode == PlotGraph.LINLOG || plotmode == PlotGraph.LOGLOG)
			hitY =  Math.pow(10.0, hitY);
		invalidate();
		return super.onTouchEvent(event);
	}

	double a0,a1;

	static final int PMODE_POINT = 0;
	static final int PMODE_LINE  = 1;
	static final int PMODE_LINE_POINT  = 2;
	
	int pointerMode = PMODE_POINT;
	private float xp;
	private float yp;

	void drawStraightLine(Canvas g)
	{
		// Left Point
		double minx = rect.left;
		double maxx = rect.right;
		double maxy = rect.top;
		double miny = rect.bottom;
		double xl = minx;
		double yl = a1*xl+a0;
		if(yl>maxy){
			yl=maxy;
			xl=(yl-a0)/a1;
		}else if(yl<miny){
			yl=miny;
			xl=(yl-a0)/a1;
		}
		// Right Point
		double xr = maxx;
		double yr = a1*xr+a0;
		if(yr>maxy){
			yr=maxy;
			xr=(yr-a0)/a1;
		}else if(yr<miny){
			yr=miny;
			xr=(yr-a0)/a1;
		}
		if(xp>=0 && pointerMode==PMODE_LINE){ // Recalculate a0,a1
			double xm = getXCoordinate(xp);
			double ym = getYCoordinate(yp);
			int Xr = getScreenX(xr), Yr = getScreenY(yr), 
				Xl = getScreenX(xl), Yl = getScreenY(yl);
			if( (xp-Xr)*(xp-Xr) + (yp-Yr)*(yp-Yr) < (xp-Xl)*(xp-Xl) + (yp-Yl)*(yp-Yl) ){
				a1 = (ym-yl)/(xm-xl);
				a0 = yl-a1*xl;
				xr = maxx;
				yr = a1*xr+a0;
				if(yr>maxy){
					yr=maxy;
					xr=(yr-a0)/a1;
				}else if(yr<miny){
					yr=miny;
					xr=(yr-a0)/a1;
				}
			}else{
				a1 = (yr-ym)/(xr-xm);
				a0 = yr-a1*xr;
				xl = minx;
				yl = a1*xl+a0;
				if(yl>maxy){
					yl=maxy;
					xl=(yl-a0)/a1;
				}else if(yl<miny){
					yl=miny;
					xl=(yl-a0)/a1;
				}
			}
		}
		// Finally, draw the line
		paint.setColor(Color.GREEN);
		paint.setStyle(Style.STROKE);
		g.drawLine(getScreenX(xl),getScreenY(yl),getScreenX(xr),getScreenY(yr), paint);
		 // Display Coordinate Message
		if(pointerMode==PMODE_LINE){
			drawMessage(g, xp, yp, new String[] {
					"a1="+fmt.toString(a1),
					"a0="+fmt.toString(a0) });
		}

	}

	void drawMessage(Canvas g, float xp, float yp, String[] msg){
		// get width of widest string
		double[] sw = new double[msg.length];
		for(int i=0; i<sw.length; i++)
			sw[i] = stringWidth(paint, msg[i] );
		
		int fh = fontHeight(paint);
		int mw = (int)PlotGraph.max(sw) + 10,			// size of message board
		    mh = msg.length * fh + 6;

		// Position (opposite to xp,yp)
		int xw = xp<width/2  ? width -xright-mw  : xleft;
     	int yw = yp<height/2 ? height-ybottom-mh : ytop;
       	paint.setColor(Color.WHITE);
       	paint.setStyle(Style.FILL);
      	g.drawRect(xw,yw,mw+xw,mh+yw, paint);
      	paint.setColor(Color.BLACK);
		for(int i=0; i<msg.length; i++){      	
     		g.drawText( msg[i],xw+5,yw+(i+1)*fh+3, paint);
     	}
	}		
		
	
// TODO: Make this not a pointer dot, instead make it an optionally vertical or horizontally scrolling bar
	void drawPointer(Canvas g){
	// Pointer active, draw red circle
		paint.setColor(Color.RED);
		paint.setStyle(Style.FILL);
		g.drawCircle(xp, yp, 3, paint);
		paint.setColor(Color.WHITE);
		g.drawPoint(xp, yp, paint);
		double X = getXCoordinate(xp);
		if( plotmode==PlotGraph.LOGLIN || plotmode==PlotGraph.LOGLOG )
			X = Math.pow(10.0,X);
		double Y = getYCoordinate(yp);
		if( plotmode==PlotGraph.LINLOG || plotmode==PlotGraph.LOGLOG )
			Y = Math.pow(10.0,Y);
		
		drawMessage(g, xp, yp, new String[] {
					"X="+fmt.toString(X),
					"Y="+fmt.toString(Y) });
	}  	
	
	/**
	 * @param xlabel: the xlabel to set
	 */
	void setXlabel(String xlabel) {
		this.xlabel = xlabel;
		if(xlabel != null) 
			ybottom = 20 + 28;
	}

	/**
	 * @param ylabel the ylabel to set
	 */
	void setYlabel(String ylabel) {
		this.ylabel = ylabel;
	}
	
	void setNT(int x, int y) {
		ntx = x;
		nty = y;
	}
	
	
	// decimal exponent
    static int decExp(double x){
       return (int) (Math.log10(x));
    }

	void drawOrnaments(Canvas g, Paint paint){
		int lenMajor = height/40,i; 			// Length of Ticmarks
      	double x = 0, y = 0;
		int axis_w = width -xleft-xright;
		int axis_h = height-ytop-ybottom;

       	String label;
		float maxx = rect.right;
		float minx = rect.left;
		float maxy = rect.top;
		float miny = rect.bottom;
		int lenlabel 	= stringWidth(paint, " 00.00 "); 
	  	int maxnumtics 	= axis_w / lenlabel;
      	double dX = (maxx- minx) / ntx;
      	double startx = minx;
      	while((maxx-minx)/dX>maxnumtics){
 			dX*=2;
 			startx = (int)(startx/dX+0.5) * dX;
 			if((maxx-minx)/dX>maxnumtics){
 				dX *= 2.5;
 				startx = (int)(startx/dX+0.5) * dX;
 			}
 		}  

 		if(plotmode==PlotGraph.LOGLIN || plotmode==PlotGraph.LOGLOG){
 			while( Math.abs( (startx/dX)-Math.round(startx/dX)) > 0.01 )
 				startx += dX;
 			if( dX < 1.0 )
 				dX = 1.0;
 			else if( dX == 2.5 )
 				dX = 2.0;
 			startx = dX * Math.floor( startx/dX );
 		}
      	int exponent = Math.max(decExp(maxx),decExp(minx))-1;
	  	if(Math.abs(exponent) <2) exponent=0;
      	double scf = Math.pow(10.,(double)exponent);
		
		for(i=0,x=startx;x<=maxx; i++, x+=dX){
			int xworld = getScreenX(x);
			if(xworld < xleft)
				continue;
			// draw ticmarks
			g.drawLine(xworld,ytop,xworld,ytop+lenMajor, paint);
			g.drawLine(xworld,height-ybottom-lenMajor,xworld,height-ybottom, paint);

			if((plotmode==PlotGraph.LOGLIN || plotmode==PlotGraph.LOGLOG) &&
			   (dX < 1.5 )){
					for(int k=2; k<=9; k++){
						int xk = getScreenX(x + Math.log10((double)k));
						if(xk>xleft && xk <xleft+width){ 
							g.drawLine(xk,ytop,xk,ytop+lenMajor/2, paint);
							g.drawLine(xk,height-ybottom-lenMajor/2,xk,height-ybottom, paint);
					}
				}
			}

			if(plotmode==PlotGraph.LOGLIN || plotmode==PlotGraph.LOGLOG)
				label = "10^";
			else
				label = "";
			// draw label
        	if(x+dX>maxx && exponent!=0 ){
           		label="E"+exponent;
        	}else{
           		label+= fmt.toString(x/scf);
        	}
         	centerText(g,label,height-ybottom+1.5*fontHeight(paint),xworld);
      	}

		if(xlabel != null)
			centerText(g,xlabel,height-ybottom+3*fontHeight(paint),
						xleft+(width-xleft-xright)/2);

		if(ylabel != null)
			centerTextV(g,ylabel, fontHeight(paint),
						ytop+(height-ytop-ybottom)/2);



		// Vertical unit dY  

      	int hilabel = fontHeight( paint );
	  	maxnumtics = (int)((axis_h) / (1.5*hilabel));
      	double dY = (maxy- miny) / nty;
      	double starty = miny;
      	while((maxy-miny)/dY>maxnumtics){
 			dY*=2;
 			starty= (int)(starty/dY+0.5) * dY;
      		if((maxy-miny)/dY>maxnumtics){
 				dY*=2.5;
 				starty= (int)(starty/dY+0.5) * dY;
 			}   
  		}   
 		if(plotmode==PlotGraph.LINLOG || plotmode==PlotGraph.LOGLOG){
 			if( dY < 1.0 )
 				dY = 1.0;
 			else if( dY == 2.5 )
 				dY = 2.0;
			starty = dY * Math.floor( starty/dY );
 		}

      	exponent = Math.max(decExp(maxy),decExp(miny))-1;
	  	if(Math.abs(exponent) <2) exponent=0;
      	scf = Math.pow(10.,(double)exponent);

      	for(i=0,y=starty;y<=maxy; i++, y+=dY){
			int yworld = getScreenY(y);
			if(yworld > height-ybottom)
				continue;
			g.drawLine(xleft,yworld,xleft+lenMajor,yworld, paint);
			g.drawLine(xleft+axis_w,yworld,xleft+axis_w-lenMajor,yworld, paint);

			if( (plotmode==PlotGraph.LINLOG || plotmode==PlotGraph.LOGLOG) &&
			     dY<1.5 ){
				for(int k=2; k<=9; k++){
					int yk = getScreenY(y + Math.log10((double)k));
					if(yk>ytop && yk<ytop+height){
						g.drawLine(xleft,yk,xleft+lenMajor/2,yk, paint);
						g.drawLine(xleft+axis_w,yk,xleft+axis_w-lenMajor/2,yk, paint);
					}
				}
			}
			if(plotmode==PlotGraph.LINLOG || plotmode==PlotGraph.LOGLOG)
				label = "10^";
			else
				label = "";
			// draw label
          	if(y+dY>maxy && exponent!=0){
              	label="E"+exponent;
          	}else{
          		label += fmt.toString(y/scf);
          	}
          	Align a = paint.getTextAlign();
          	paint.setTextAlign(Align.RIGHT);
          	g.drawText(label,xleft, yworld+hilabel/2-3, paint);
          	paint.setTextAlign(a);
		}
			
	}

	private void centerTextV(Canvas g, String label, double x, float y) {
		g.rotate(-90, (float) x, y);
		g.drawText(label, (float)x, y, paint);
		g.rotate(+90, (float) x, y);
	}

	private void centerText(Canvas g, String label, double y, float x) {
		g.drawText(label, x, (float) y, paint);
		
	}

	private int fontHeight(Paint g) {
		return Math.round(g.getTextSize());
	}

	private int getScreenY(double y) {
		double a = (height-ytop-ybottom)/(rect.bottom-rect.top);
		double b = ytop - a * rect.top;
        return (int)(a*y + b + 0.5);
	}

	private int getScreenX(double x) {
		double a = (width-xleft-xright)/(rect.right-rect.left);
		double b = xleft - a* rect.left;
        return (int)(a*x + b + 0.5);
	}

	  // Math Coordinates
	  public float getXCoordinate(float x){
	      float a = (rect.right-rect.left)/(float)(width-xleft-xright);
	      float b = rect.left - a*xleft;
	      return a*x+b;
	  }

	  public float getYCoordinate(float y){
	      float a = (rect.bottom-rect.top)/(float)(height-ytop-ybottom);
	      float b = rect.top - a*ytop;
	      return a*y+b;
	  }


	
	private int stringWidth(Paint g, String string) {
		Rect bounds = new Rect();
		g.getTextBounds(string, 0, string.length(), bounds);
		return bounds.right;
	}

	void setPlotmode(int plotmode) {
		this.plotmode = plotmode;
	}

	boolean doCommand(int itemId) {
		switch(itemId) {
		case R.id.menu_clear: 
			hitX = hitY = Double.NaN;
			xp = yp = Float.NEGATIVE_INFINITY;
			invalidate();
			return true;	
		case R.id.menu_line:
			pointerMode = PMODE_LINE;
			invalidate();
			return true;
		case R.id.menu_point:
			pointerMode = PMODE_POINT;
			invalidate();
			return true;
		}
		
		return false;
	}

	static interface MarkerStrategy {
		void draw(Path p, float x, float y, float dx, float dy);
	}
	static class PlusMarker implements MarkerStrategy {
		public void draw(Path p, float x, float y, float dx, float dy) {
			p.moveTo(x-dx,y);
			p.lineTo(x+dx,y);
			p.moveTo(x,y-dy);
			p.lineTo(x,y+dy);			
		}
	}
	static class XMarker implements MarkerStrategy {
		public void draw(Path p, float x, float y, float dx, float dy) {
			p.moveTo(x-dx, y-dy);
			p.lineTo(x+dx, y+dy);
			p.moveTo(x+dx, y-dy);
			p.lineTo(x-dx, y+dy);
		}
	}
	static class StarMarker implements MarkerStrategy {
		
		public void draw(Path p, float x, float y, float dx, float dy) {
			PLUSMARKER.draw(p,x,y,dx,dy);
			XMARKER.draw(p, x, y, dx*0.66f, dy*0.66f);
		}
	}
	static class OMarker implements MarkerStrategy {

		public void draw(Path p, float x, float y, float dx, float dy) {
			RectF oval = new RectF(x-dx, y-dy, x+dx, y+dy);
			p.addOval(oval, Direction.CW);
		}
		
	}
	
	
	static final MarkerStrategy PLUSMARKER = new PlusMarker();
	static final MarkerStrategy XMARKER = new XMarker();
	static final MarkerStrategy OMARKER = new OMarker();
	static final MarkerStrategy STARMARKER = new StarMarker();
	public void addMarker(double[] x, double[] y, char marker, int color) {
		MarkerStrategy strategy;
		float dx = (rect.left-rect.right)/ width * 3; // TODO : scaling is definitely off, needs re-epsilon-ing
		float dy = (rect.top - rect.bottom) / height * 3; //TODO: needs re-delta-ing from dx's new epsilon
		switch(marker) {
		default: strategy = XMARKER; break;
		case '+': strategy = PLUSMARKER; break;
		case '*': strategy = STARMARKER; break;
		case 'o': strategy = OMARKER; break;
		}
		
		Path p = new Path();
		for(int i = 0; i < x.length; i++) {
			strategy.draw(p, (float)x[i], (float)y[i], dx, dy);
		}
		p.offset(-rect.left, -rect.top);
		PathShape shape = new PathShape(p, rect.right-rect.left, rect.bottom-rect.top);
		line = new ShapeDrawable(shape);
		line.getPaint().setColor(color);
		line.getPaint().setStyle(Paint.Style.STROKE);
		line.setBounds(xleft,ytop, width-xright,height-ybottom);			
		lines.add(line);
		
	}
}
