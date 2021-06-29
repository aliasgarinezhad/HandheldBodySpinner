package com.jeanwest.reader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MyListAdapterSub extends ArrayAdapter<String> {

    private final Activity context;
    private final ArrayList<String> title;
    private final ArrayList<String> spec;
    private final ArrayList<String> scanned;
    private final ArrayList<String> all;
    private final ArrayList<String> notScanned;

    public MyListAdapterSub(Activity context, ArrayList<String> title, ArrayList<String> spec, ArrayList<String> scanned, ArrayList<String> all, ArrayList<String> notScanned) {
        super(context, R.layout.list, title);
        // TODO Auto-generated constructor stub

        this.context=context;
        this.title = title;
        this.spec = spec;
        this.all = all;
        this.notScanned = notScanned;
        this.scanned = scanned;

    }

    @SuppressLint("DefaultLocale")
    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater=context.getLayoutInflater();
        View rowView=inflater.inflate(R.layout.list_sub, null,true);

        TextView titleText = rowView.findViewById(R.id.titleViewSub);
        TextView specText = rowView.findViewById(R.id.specViewSub);
        TextView scannedText = rowView.findViewById(R.id.scannedViewSub);
        TextView allText = rowView.findViewById(R.id.notScannedViewSub);
        TextView notScannedText = rowView.findViewById(R.id.extraViewSub);

        try {

            titleText.setText(title.get(position));
            specText.setText(spec.get(position));
            scannedText.setText(scanned.get(position));
            allText.setText(all.get(position));
            notScannedText.setText(notScanned.get(position));
        }
        catch (ArrayIndexOutOfBoundsException e) {
            Toast.makeText(context, String.format("title: %d spec: %d scanned: %d all: %d notScanned: %d", title.size(), spec.size(), scanned.size(), all.size(), notScanned.size()), Toast.LENGTH_LONG).show();
        }
        return rowView;
    };
}