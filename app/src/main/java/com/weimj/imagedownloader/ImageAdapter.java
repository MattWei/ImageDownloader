package com.weimj.imagedownloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by weimj on 15/1/27.
 */
public class ImageAdapter extends BaseAdapter {
    private Context mContext;
    private List<String> mImageFiles = new ArrayList<String>();

    public ImageAdapter(Context c) {
        mContext = c;
    }

    public int getCount() {
        //return mImageFiles.size();
        return 20;
    }

    public void addImages(List<String> images) {
        mImageFiles.addAll(images);
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {  // if it's not recycled, initialize some attributes
            imageView = new ImageView(mContext);
            imageView.setLayoutParams(new GridView.LayoutParams(
                    convertView.getWidth(), convertView.getHeight()));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(8, 8, 8, 8);
        } else {
            imageView = (ImageView) convertView;
        }

        Bitmap myBitmap = BitmapFactory.decodeFile(mImageFiles.get(position));
        imageView.setImageBitmap(myBitmap);

        return imageView;
    }
}
