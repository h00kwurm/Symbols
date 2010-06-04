/* Jasymca for Android

   Copyright (C) 2009 - Wim van Velthoven
   
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
package org.dits.symbols;

import java.util.Iterator;
import java.util.Vector;

import org.dits.symbols.PlotGraph.PlotLine;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;

/**
 * @author Wim
 *
 */
public class PlotActivity extends Activity {

	public static final String TITLE = "title";
	public static final String X = "x";
	public static final String Y = "y";
	public static final String PLOTGRAPH = "plotGraph";

	private PlotView plotter;
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("onCreate" , PLOTGRAPH);
		plotter  = new PlotView(this);
		Intent intent = getIntent();
		setContentView(plotter);
		Display display = getWindowManager().getDefaultDisplay();
		plotter.width = display.getWidth();
		plotter.height = display.getHeight();
		if(intent != null)
			onNewIntent(intent);
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onNewIntent(android.content.Intent)
	 */
	@Override
	protected void onNewIntent(Intent intent) {
		Log.d("PlotActivity", "onNewIntent" + intent);
		super.onNewIntent(intent);
		setIntent(intent);
		PlotGraph graph = (PlotGraph) intent.getSerializableExtra(PLOTGRAPH);
		if(graph != null)
		{
			Vector<PlotLine> lines = graph.PlotLines;
			if(lines.isEmpty())
				return;
			plotter.clear();
			String title = graph.Tlabel;
			if(title != null) setTitle(title);
			plotter.setXlabel(graph.Xlabel);
			plotter.setYlabel(graph.Ylabel);
			plotter.setNT(graph.ntx, graph.nty);
			plotter.setPlotmode(graph.plotmode);
			plotter.setMinMax(graph.minx, graph.maxx, graph.miny, graph.maxy);
			Log.d("onNewIntent", "lines = " + lines.size());
			for (Iterator<PlotLine> iterator = lines.iterator(); iterator.hasNext();) {
				PlotLine line = iterator.next();
				if(line.eu != null)
					plotter.addErrorBar(line.x, line.y, line.eu, line.el, line.color);
				if(line.marker == ' ')
					plotter.addLine(line.x, line.y, line.color);
				else
					plotter.addMarker(line.x, line.y, line.marker, line.color);
			}
			plotter.invalidate();
			return;
		}
		
		
		String title = intent.getStringExtra(TITLE);
		if(title != null) setTitle(title);
		double x[]  = intent.getDoubleArrayExtra(X);
		double y[]  = intent.getDoubleArrayExtra(Y);
		if(x != null && y != null)
			plotter.addLine(x, y, Color.YELLOW);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.plot_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d("PlotActivity", item.toString());
		return plotter.doCommand(item.getItemId());
	}
}
