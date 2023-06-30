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
import io.gejsi.pufferfish.models.Heatmap;
import io.gejsi.pufferfish.models.Measurement;

public class OnlineHeatmapListAdapter extends ArrayAdapter<Heatmap> {
  public OnlineHeatmapListAdapter(Context context, List<Heatmap> heatmaps) {
    super(context, 0, heatmaps);
  }

  @SuppressLint("SetTextI18n")
  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    if (convertView == null) {
      convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_heatmap, parent, false);
    }

    ImageView iconImageView = convertView.findViewById(R.id.icon);
    TextView fileNameTextView = convertView.findViewById(R.id.fileName);

    Heatmap heatmap = getItem(position);

    Measurement.Type measurementType = heatmap.getMeasurementType();
    if (measurementType == Measurement.Type.Noise)
      iconImageView.setImageResource(R.drawable.ic_sound);
    else if (measurementType == Measurement.Type.WiFi)
      iconImageView.setImageResource(R.drawable.ic_wifi);
    else if (measurementType == Measurement.Type.LTE)
      iconImageView.setImageResource(R.drawable.ic_lte);

    String[] timestampParts = heatmap.getTimestamp().split("_");
    String date = timestampParts[0];
    String time = timestampParts[1];
    fileNameTextView.setText(date + " · " + time);

    return convertView;
  }
}

