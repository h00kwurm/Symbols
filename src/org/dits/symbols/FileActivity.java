/* Jasymca

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

import java.io.File;
import java.io.FilenameFilter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.method.KeyListener;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnKeyListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * @author Wim
 *
 */
public class FileActivity extends ListActivity implements OnKeyListener  {

	private static final String[] EMPTY_LIST = new String[0];

	private static final int MENU_DELETE = 1;
	private static final int MENU_OPEN = 2;
	private static final int MENU_MKDIR = 3;
	
	private String[] filelist;

	private File currentDir;

	private FilenameFilter filter = new FilenameFilter() {

		public boolean accept(File dir, String name) {
			String text = searchBox.getText().toString();
			return text.length() == 0 || name.startsWith(text);
		} 
		
	};

	private EditText searchBox;

	ArrayAdapter<String> arrayAdapter;

	private int count;

	/* (non-Javadoc)
	 * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		String select = getListView().getItemAtPosition(position).toString();
		Log.d("FileActivity", "pos = " + position + " id =" + id  + " str =" + select);
		enter(select);
	}

	/**
	 * @param select
	 */
	private void enter(String select) {
		File file = new File(currentDir, select);
		if(file.isDirectory())
		{
			String[] newfiles = file.list();
// don't decent into a empty directory. NO, save a file into an empty directory.
//			if(newfiles == null || newfiles.length == 0)
//				return;
			currentDir = file;
			filelist = newfiles;
			count++;
			refresh();
			return;
		}
		
		Uri uri = Uri.fromFile(file);
		Intent result = new Intent();
		result.setData(uri);
		setResult(RESULT_OK, result);
		finish();
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.file);
		getListView().setOnCreateContextMenuListener(this);
		searchBox = (EditText)findViewById(R.id.searchbox);
		searchBox.setOnKeyListener(this);
		searchBox.setOnCreateContextMenuListener(this);
		Intent intent = getIntent();
		Uri r = intent.getData();
		if(r != null && r.getScheme().equals("file"))
		{
			String filepath= r.getPath();
			File file = new File(filepath);
			if(file.isFile())
			{	
				String end = r.getLastPathSegment();
				searchBox.setText(end);
				file = file.getParentFile();
			}
			currentDir = file;
		} else
			currentDir = getFilesDir(); // default path
		Log.d("FileActivity", "Start in " + currentDir);
		
		filelist = currentDir.list();
		if(filelist == null)
			filelist  = EMPTY_LIST;
		Arrays.sort(filelist);

		List<String> dynamicList = new ArrayList<String>();
		arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dynamicList);
		setListAdapter(arrayAdapter);
		refresh();
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if(item.getItemId() == MENU_MKDIR)
		{
			String dir = searchBox.getText().toString();
			File file = new File(currentDir, dir);
			file.mkdirs();
			if(file.isDirectory())
			{
				currentDir = file;
				searchBox.setText("");
				refreshDir();
				return true;
			}
		}
		
		if(item.getItemId() == MENU_DELETE)
		{
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
			int pos = info.position;
			Log.d("onselected", "delete " + pos);
			if(pos >=0 && pos < arrayAdapter.getCount())
			{
				String name = arrayAdapter.getItem(pos);
				Log.d("onselected", "name=" + name);
				if( new File(currentDir, name).delete()) 
				{
					refreshDir();					
				}
			}
			return true;
			
		}
		// TODO Auto-generated method stub
		return super.onContextItemSelected(item);
	}

	/**
	 * 
	 */
	private void refreshDir() {
		filelist = currentDir.list();
		if(filelist == null)
			filelist = EMPTY_LIST;
		refresh();
	}

	
	/**
	 * 
	 */
	private void refresh() {
		arrayAdapter.setNotifyOnChange(false);
		arrayAdapter.clear();
		for(int i = 0; i < filelist.length; i++) {
			String name = filelist[i];
			if(filter.accept(currentDir, name))
				arrayAdapter.add(name);
		}
		arrayAdapter.notifyDataSetChanged();
		getListView().invalidate();
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		menu.add(0, MENU_OPEN, 0, "Select");
		if(v == searchBox)
			menu.add(0, MENU_MKDIR, 0, "new folder");
		menu.add(0, MENU_DELETE, 0, R.string.delete);
		if(menuInfo instanceof AdapterContextMenuInfo)
		{
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
			int pos = info.position;
			menu.setHeaderTitle(arrayAdapter.getItem(pos));
		}
	}

	public boolean onKey(View v, int keyCode, KeyEvent event) {
		if(v == searchBox)
			refresh();
		if(event.getAction() == KeyEvent.ACTION_UP)
			return false;
		if(keyCode == KeyEvent.KEYCODE_SLASH)
			return true;
		if(keyCode == KeyEvent.KEYCODE_ENTER)
		{
			String name = searchBox.getText().toString();
			enter(name);
			return true;
		}
		if(keyCode == KeyEvent.KEYCODE_BACK && this.count-- > 0)
		{
			File parentDir = currentDir.getParentFile();
			if(parentDir != null)
				currentDir = parentDir;
			refreshDir();
			return true;
		}
		return false;
	}


}
