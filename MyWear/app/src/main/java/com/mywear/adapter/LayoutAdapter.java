package com.mywear.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mywear.ListItem;
import com.mywear.R;

import java.util.List;

public class LayoutAdapter extends ArrayAdapter<ListItem> {
    private int resourceId;

    public LayoutAdapter(Context context, int textViewResourceId, List<ListItem> objects) {
        super(context, textViewResourceId, objects);
        resourceId = textViewResourceId;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ListItem listitem = getItem(position);
        View view = LayoutInflater.from(getContext()).inflate(resourceId,parent,false);
        ImageView ItemImage = (ImageView) view.findViewById(R.id.list_item_img);
        ItemImage.setImageResource(listitem.getImgId());
        return view;
    }
}
