package com.cubezonline.mylistViewvariants;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;

import lb.library.PinnedHeaderListView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater mInflater = LayoutInflater.from(MainActivity.this);
        setContentView(R.layout.activity_main);
        PinnedHeaderListView mListView = findViewById(android.R.id.list);
        mListView.setPinnedHeaderView(mInflater.inflate(R.layout.pinned_header_listview_side_header, mListView, false));
        mListView.setEnableHeaderTransparencyChanges(false);
        new ClassCreateList(this, mListView);
        //    mAdapter.getFilter().filter(mQueryText,new FilterListener() ...
        //You can also perform operations on selected item by using :
        //    mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() ...
    }

}
