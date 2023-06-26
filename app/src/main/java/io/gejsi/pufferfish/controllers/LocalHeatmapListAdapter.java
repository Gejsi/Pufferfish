package io.gejsi.pufferfish.controllers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import io.gejsi.pufferfish.R;
import io.gejsi.pufferfish.models.Measurement;

public class LocalHeatmapListAdapter extends ArrayAdapter<String> {
  public LocalHeatmapListAdapter(Context context, List<String> fileNames) {
    super(context, 0, fileNames);
  }

  @SuppressLint("SetTextI18n")
  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    if (convertView == null) {
      convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_heatmap, parent, false);
    }

    ImageView iconImageView = convertView.findViewById(R.id.icon);
    TextView fileNameTextView = convertView.findViewById(R.id.fileName);

    String fileName = getItem(position);
    String[] fileParts = fileName.split("_");

    Measurement.Type measurementType = Measurement.Type.valueOf(fileParts[1]);
    if (measurementType == Measurement.Type.Noise)
      iconImageView.setImageResource(R.drawable.ic_sound);
    else if (measurementType == Measurement.Type.WiFi)
      iconImageView.setImageResource(R.drawable.ic_wifi);
    else if (measurementType == Measurement.Type.LTE)
      iconImageView.setImageResource(R.drawable.ic_lte);

    String date = fileParts[2];
    String time = fileParts[3].substring(0, fileParts[3].length() - 5);
    fileNameTextView.setText(date + " · " + time);

    return convertView;
  }
}

