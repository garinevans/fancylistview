package com.example.bespokelistimplementation;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class MyAdapter extends BaseAdapter {

	private List<String> mList;
	
	@Override
	public int getCount() { 
		return mList.size();
	}

	@Override
	public Object getItem(int position) {
		return mList.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}
		
	public void AddToList(String theString){
		if(mList == null)
			mList = new ArrayList<String>();
	
		mList.add(theString);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		parent.getContext();
		LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.mylistitem, parent, false);
		
		String theString = mList.get(position);
		TextView textView = (TextView)view.findViewById(R.id.textView);
		textView.setText(theString);
		
		return view;
	}

}
