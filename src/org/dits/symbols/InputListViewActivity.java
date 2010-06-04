package org.dits.symbols;

import java.util.ArrayList;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class InputListViewActivity extends ListActivity 
{
	protected static final int CONTEXTMENU_DELETEITEM = 0;
	
	private ProgressDialog m_ProgressDialog = null;
	private ArrayList<InputOutput> m_inputoutputs = null;
	private InputOutputAdapter m_adapter;
	private Runnable viewInputOutputs;
	
	public void onCreate(Bundle sack)
	{
		super.onCreate(sack);
		setContentView(R.layout.main);
		
		m_inputoutputs = new ArrayList<InputOutput>();
		this.m_adapter = new InputOutputAdapter(this, R.layout.row, m_inputoutputs);
		
		this.setListAdapter(this.m_adapter);
		
		
/*		viewInputOutputs = new Runnable(){
			public void run()
			{
				getInputOutputs();
			}
		};
		registerForContextMenu(getListView());*/
		
	}
	
	public void addElement(InputOutput k)
	{
		this.m_adapter.add(k);
		this.onContentChanged();
	}
	
/*	private  Runnable returnRes = new Runnable() {
        public void run() {
            if(m_inputoutputs != null && m_inputoutputs.size() > 0){
                m_adapter.notifyDataSetChanged();
                for(int i=0;i<m_inputoutputs.size();i++)
                m_adapter.add(m_inputoutputs.get(i));
            }
            m_ProgressDialog.dismiss();
            m_adapter.notifyDataSetChanged();
        }
      };
	
	private void getInputOutputs(){
        try{
        	m_inputoutputs = new ArrayList<InputOutput>();
      	  	Thread.sleep(500);
          } catch (Exception e) {}
          runOnUiThread(returnRes);
      }*/
	
//********************************************************
//********************************************************
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		menu.add(0, 0, 0, "Copy");
		menu.add(0, 1, 0,  "Delete");
}
	
    public boolean onContextItemSelected(MenuItem item) {
    	AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    	try {
    	    info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
    	} catch (ClassCastException e) {
    	    System.out.println("Bad menuInfo");
    	    return false;
    	}
    	long id = getListAdapter().getItemId(info.position);

		switch (item.getItemId()) {
		case 0:
			ClipboardManager temp = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
			temp.setText(m_adapter.getItem((int) id).getInput());
			return true;
		case 1:
			m_adapter.remove(m_adapter.getItem((int) id));
			return true;
		default:
			return super.onContextItemSelected(item);
		}	
	}
//********************************************************	
//********************************************************
	private class InputOutputAdapter extends ArrayAdapter<InputOutput>
	{
	
		private ArrayList<InputOutput> items;
		
		public InputOutputAdapter(Context context, int textViewResourceId, ArrayList<InputOutput> info)
		{
			super(context, textViewResourceId, info);
			this.items = info;
		}
		
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.row, null);
            }
            InputOutput o = items.get(position);
            if (o != null) {
                    TextView tt = (TextView) v.findViewById(R.id.toptext);
                    TextView bt = (TextView) v.findViewById(R.id.bottomtext);
                    if (tt != null) 
                    {
                          tt.setText(o.getInput());
                    }
                    if(bt != null){
                    		if(!o.isTerminated())
                    		{
                    			bt.setVisibility(0);
                    		}
                    		else
                    		{
                    			bt.setText(o.getOutput());
                    		}
                    	}
            }
            return v;
        }
		
	}
}
