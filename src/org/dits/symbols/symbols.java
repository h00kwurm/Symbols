package org.dits.symbols;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import net.sourceforge.jasymcaandroid.jasymca.Jasymca;
import net.sourceforge.jasymcaandroid.jasymca.JasymcaException;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

public class symbols extends Activity implements OnClickListener, OnKeyListener {
	
	public static final String REQUEST_CODE = "requestCode";

	private static final int MENU_INTERRUPT = R.id.menu_interrupt;
	private static final int MENU_CLEAR = R.id.menu_clear;
	private static final int MENU_QUIT = R.id.menu_quit;
	private static final int MENU_PLOT = R.id.menu_plot;
	private static final int MENU_LOAD = R.id.menu_load;
	private static final int MENU_SAVE = R.id.menu_save;
	private static final int MENU_HELP = R.id.menu_help;
	
	EditText output;
	AutoCompleteTextView input;
	Jasymca jasymca; 
	EditTextInputStream in; 
	EditTextOutputStream out;
	ArrayList<InputOutput> kIO;
	
 /** Called when the activity is first created. */
 @Override
 public void onCreate(Bundle savedInstanceState) {
     super.onCreate(savedInstanceState);   
     Jasymca.context = this;
     PlotGraph.activity = this;
     plotstarter = new Handler();
     setContentView(R.layout.main);
     
     String[] availCommands = getResources().getStringArray(R.array.stdcommands);
     ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, availCommands);
             
     input = (AutoCompleteTextView) findViewById(R.id.input);
     input.setAdapter(adapter);
     input.setThreshold(2);
     output = (EditText) findViewById(R.id.output);
     startupMode = getMode();
     input.setOnClickListener(this);
     input.setOnKeyListener(this);
     input.requestFocus();
     jasymca = new Jasymca(getSelectedMode());
     in = new EditTextInputStream();
     out = new EditTextOutputStream();
     ps = new PrintStream(out);
     kIO = new ArrayList<InputOutput>();
		jasymca.start(in, ps);
 }

	private boolean getMode() {
		SharedPreferences pref = 
			PreferenceManager.getDefaultSharedPreferences(this);
		return pref.getBoolean("mode", false);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	private void startHelp() {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse("http://www.google.com"));
		startActivity(intent);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    case MENU_HELP:
	    	startHelp();
	    	return true;
	    case MENU_CLEAR:
	    	stopJasymca();
	    	history.clear();inHistory = 0;
	    	jasymca.setup_ui(null, true);
	    	jasymca.start(in, ps);
	    	return true;
	    case MENU_PLOT:
	    		startPlot();
	    		break;
	    case MENU_LOAD:
	    case MENU_SAVE:
	    		startFile(item.getItemId(), null);
	    		break;
	    case R.id.menu_import:
	    case R.id.menu_export:
	    case R.id.menu_output:
	    		startFile(item.getItemId(), Uri.fromFile(Environment.getExternalStorageDirectory()));
	    		break;
	    case R.id.menu_mode:
	    	Intent intent = new Intent(this, JasymcaPrefs.class);
	    	startActivity(intent);
	    	return true;	    
	    case MENU_INTERRUPT:
	    	if(jasymca.isAlive())
	    	{
	    		in.interrupt();
	    		jasymca.interrupt();
	    	}
	    	return true;
	    case MENU_QUIT:
	    	stopJasymca();
	    	finish();
	        return true;
	    }
	    return false;
	}

	private void stopJasymca() {
		in.interrupt();
		if(jasymca.isAlive())
		{ 	jasymca.interrupt();
			in.append("\nexit;\n");
		}
		if(jasymca.isAlive())
		{
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				Log.d("stopJasymca", e.getMessage(), e);
			}
		}
	}

	
	/* (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(resultCode == RESULT_OK)
		{
			Uri r = data.getData();
			String file = r.getPath();
			String scheme = r.getScheme();
			Log.d("JasymcaActivity", "result uri = " + scheme + " + " + file);
			fileAction(r, requestCode);
		}
	}

	boolean startupMode;
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		Log.d("onResume", "resuming...");
		boolean newMode = getMode();
		if(newMode != startupMode)
		{
			Log.d("onResume", "restarting in " + newMode);
			ps.print("restarting to "+ newMode); // Here I need to change the minor detail of which version I'm in currently...
			startupMode = newMode;
			restartJasymca();
		}
	}

	private void restartJasymca() {
		String selected = getSelectedMode();
		Log.d("restartJasymca", selected);
		stopJasymca();
		jasymca.setup_ui(selected, false);
		//jasymca.setWelcome("Restarting in " + selected + " mode.\n");
		//Instead change the color appropriately since selected is fixed ...
		jasymca.start(in, ps); }

	/**
	 * Return the string <em>maxima</em> or <em>octave</em>. 
	 * Uses startupMode as selector.
	 * @return the selected mode
	 */
	private String getSelectedMode() {
		return startupMode?"Maxima":"Octave";
	}
	

	private void fileAction(Uri uri, int requestCode) {
		switch(requestCode) {
		case MENU_SAVE:
		case R.id.menu_export:
		case R.id.menu_output:
			Log.d("fileAction", "Save " + uri);
			try {
					File f = new File(uri.getPath());
					f.getParentFile().mkdirs();
					OutputStream outfile = new FileOutputStream(f);
					PrintWriter writer = new PrintWriter(outfile);
					if(requestCode == R.id.menu_output)
					{
						writer.print(output.getText());
					} else {
						for (Iterator<String> iterator = history.iterator(); iterator.hasNext(); ) {
							String line = iterator.next();
							writer.println(line);
					}}
					writer.close();
			} catch (IOException e) {
					out.write(e.toString());
					e.printStackTrace();
				}
			break;
		case MENU_LOAD:
		case R.id.menu_import:
			Log.d("fileAction", "Load " + uri);
			try {
					out.write("Reading "+uri+" ....");
					InputStream in = getContentResolver().openInputStream(uri);
					jasymca.readFile(in);
					out.write("...finished.\n");
				} catch (IOException e) {
					e.printStackTrace();
					out.write(e.toString());
					out.write("\n");
				} catch (JasymcaException e) {
					e.printStackTrace();
					out.write(e.toString());
					out.write("\n");
				}
			break;
		default:
			Log.e("fileAction", "unknown requestCode " + requestCode + " file=" + uri);
		}
	}

	private void startFile(int requestCode, Uri data)
	{
		Intent intent;
		if(requestCode == MENU_SAVE || requestCode == R.id.menu_export || requestCode == R.id.menu_output)
//SAVE to a file
			intent = new Intent(this, FileActivity.class);
		else
//LOAD to a file
			intent = new Intent(this, FileActivity.class);

		intent.putExtra(REQUEST_CODE, requestCode);
		intent.setData(data);
		
		startActivityForResult(intent, requestCode);
	}
	

	PlotGraph plotGraph;
	
	private void startPlot() {
		if (plotGraph == null)
			return;
		Intent intent;
		intent = new Intent(this, PlotActivity.class);
		intent.putExtra(PlotActivity.PLOTGRAPH, plotGraph);
		startActivity(intent);
	}


	public void onClick(View v) {
		if(v == input)
		{
			inputAction();
			return;
		}
		
	}

	class EditTextInputStream extends InputStream {
		private StringBuffer buf = new StringBuffer(256);
		private boolean int_flag, in_read;
		@Override
		public int read() throws IOException {
			int c = -1;
			synchronized(buf) {
				in_read = true;
				while(buf.length() == 0 && !int_flag)
					try {
						buf.wait();
					} catch (InterruptedException e) {
						in_read = false;
						throw new IOException(getString(R.string.interrupt));
					}				
					if(int_flag)
					{
						int_flag = false;
						in_read = false;
						throw new IOException(getString(R.string.interrupt));
					}
					c = buf.charAt(0);
					buf.deleteCharAt(0);
					in_read = false;
			}
			return c;
		}
		
		public void interrupt() {
			synchronized (buf) {
				if(in_read) {
					this.int_flag = true;
					buf.setLength(0);
					buf.notifyAll();
				}
			}
			
		}
		void append(String s) {
			synchronized(buf) {
				buf.append(s);
				buf.notifyAll();
			}
		}
	}
	
	class EditTextOutputStream extends OutputStream {
		Handler handler = new Handler();
		
		@Override
		public void write(final int oneByte) throws IOException {
			final String string = String.valueOf((char)oneByte);
			post(string);
			
		}

		public void write(String string) {
			post(string);
		}

		private void post(final String string) {
			handler.post(new Runnable() {
				public void run() {
					output.append(string);
					output.setSelection(output.length());
					if(!jasymca.isAlive())
						finish();
				} 
			});
		}

		@Override
		public void write(byte[] buffer, int offset, int count)
				throws IOException {
			if(count == 0)
				return;
			final String string = new String(buffer, offset, count);
			post(string);
		}

		@Override
		public void write(byte[] buffer) throws IOException {
			if (buffer.length == 0)
				return;
			final String string = new String(buffer);
			post(string);
		}
		
	}

	Vector<String> history = new Vector<String>();
	int inHistory;

	private int historyLimit = 20;

	private PrintStream ps;
	
	// Here I need to change this stuff...
	private void inputAction() {
		String instring = input.getText().toString();
		instring = instring.replace("\n", "");
		if(instring.length() == 0)
			return;
		// Here I need to add "instring as the input"
		history.add(instring);
		if(history.size()>historyLimit )
			history.removeElementAt(0);
		inHistory = history.size();
		instring += "\n";
		input.setText("");
		interpret(instring);
	}
	
	private void upAction() {
		Log.d("HelloAndroid", "upAction");
		if(inHistory > 0)
		{
			String fromHistory = history.get(--inHistory);
			input.setText(fromHistory);
		}
	}
	private void downAction() {
		Log.d("HelloAndroid", "downAction");
		if(inHistory < history.size()-1) {
			String fromHistory = history.get(inHistory++);
			input.setText(fromHistory);
		} else if(inHistory == history.size()-1) {
			inHistory += 1;
			input.setText("");
		}
	}
	
	
	private void interpret(String instring) {
		output.append(instring);
		in.append(instring);
	}

	public boolean onKey(View v, int keyCode, KeyEvent event) {
//instinker up en down events!
		if(event.getAction() == KeyEvent.ACTION_UP)
			return false;
		if(v == input)
			switch (keyCode) {
			case KeyEvent.KEYCODE_ENTER:
			case KeyEvent.KEYCODE_DPAD_CENTER:
				inputAction();
				return true;
			case KeyEvent.KEYCODE_DPAD_UP:
				upAction();
				return true;
			case KeyEvent.KEYCODE_DPAD_DOWN:
				downAction();
				return true;
		}
		return false;
	}

	private Handler plotstarter;

	public void startPlot(final PlotGraph plotGraph) {
		if(!plotGraph.PlotLines.isEmpty())
		{
			plotstarter.post(new Runnable() {

				public void run() {
					symbols.this.plotGraph = plotGraph;
					startPlot();					
				}
			});
		}		
	}
}