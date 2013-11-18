package com.example.bespokelistimplementation;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;

import com.garinevans.fancylistview.FancyListView;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		FancyListView myListView = (FancyListView)findViewById(R.id.myListView);
		
		MyAdapter adapter = new MyAdapter();
		for(int i = 1; i < 25; i++){
			adapter.AddToList(String.format("Test %s", String.valueOf(i)));
		}
		
		myListView.setAdapter(adapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
		
		//MyListView test = new MyListView();
	}

}
