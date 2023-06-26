package io.gejsi.pufferfish.controllers;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.gejsi.pufferfish.R;
import io.gejsi.pufferfish.databinding.ActivityMainBinding;
import io.gejsi.pufferfish.models.Heatmap;
import io.gejsi.pufferfish.utils.HeatmapUtils;

public class MainActivity extends AppCompatActivity {
  private ActivityMainBinding binding;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivityMainBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    Toolbar toolbar = binding.toolbar;
    setSupportActionBar(toolbar);

    FloatingActionButton fab = binding.fab;
    fab.setOnClickListener(v -> {
      AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
      builder.setTitle("Select the type of measurement you want to perform");

      // Define the list of measurement types
      String[] measurementTypes = {"Noise", "WiFi", "LTE"};

      // Set the radio buttons for the measurement types
      builder.setSingleChoiceItems(measurementTypes, -1, (dialog, which) -> {
        String selectedMeasurementType = measurementTypes[which];

        Intent intent = new Intent(MainActivity.this, MapsActivity.class);
        intent.putExtra("measurementType", selectedMeasurementType);
        startActivity(intent);

        dialog.dismiss();
      });

      AlertDialog dialog = builder.create();
      dialog.show();
    });

    ImageButton profileButton = binding.me;
    profileButton.setOnClickListener(view -> {
      Intent intent = new Intent(MainActivity.this, UserActivity.class);
      startActivity(intent);
    });

    ImageButton settingsButton = binding.settingsButton;
    settingsButton.setOnClickListener(view -> {
      Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
      startActivity(intent);
    });

    TabLayout tabs = binding.tabLayout;
    ViewFlipper viewFlipper = binding.viewFlipper;
    tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
      @Override
      public void onTabSelected(TabLayout.Tab tab) {
        int position = tab.getPosition();
        viewFlipper.setDisplayedChild(position);
      }

      @Override
      public void onTabUnselected(TabLayout.Tab tab) {
      }

      @Override
      public void onTabReselected(TabLayout.Tab tab) {
      }
    });

  }

  @Override
  protected void onResume() {
    super.onResume();

    fillLocalList();
    fillOnlineList();
  }

  private void fillLocalList() {
    List<String> files = HeatmapUtils.getLocalFiles(this.getApplicationContext().fileList());
    ListView localHeatmaps = findViewById(R.id.localHeatmapsList);
    LocalHeatmapListAdapter localListAdapter = new LocalHeatmapListAdapter(this, files);
    localHeatmaps.setAdapter(localListAdapter);
    localHeatmaps.setOnItemClickListener((parent, v, position, id) -> {
      String fileName = files.get(position);

      AlertDialog.Builder builder = new AlertDialog.Builder(parent.getContext());
      builder.setTitle("Actions")
              .setMessage("What do you will you do with this heatmap?")
              .setPositiveButton("Open", (dialog, which) -> {
                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                intent.putExtra("measurementType", fileName.split("_")[1]);
                intent.putExtra("fileName", fileName);
                startActivity(intent);
              })
              .setNegativeButton("Delete", (dialog, which) -> {
                deleteHeatmap(fileName);
                // update the ListView
                files.remove(position);
                localListAdapter.notifyDataSetChanged();
              })
              .setNeutralButton("Sync online", (dialog, which) -> {
                HeatmapUtils.syncHeatmap(this, fileName);
              })
              .show();
    });
  }

  private void fillOnlineList() {
    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    if (currentUser == null) {
      findViewById(R.id.not_logged).setVisibility(View.VISIBLE);
      findViewById(R.id.online_desc).setVisibility(View.GONE);
    } else {
      findViewById(R.id.not_logged).setVisibility(View.GONE);
      findViewById(R.id.online_desc).setVisibility(View.VISIBLE);

      ListView onlineHeatmaps = findViewById(R.id.onlineHeatmapsList);
      FirebaseDatabase database = FirebaseDatabase.getInstance(getString(R.string.db));
      DatabaseReference heatmapsRef = database.getReference("heatmaps").child(currentUser.getUid());
      List<Heatmap> heatmapList = new ArrayList<>();

      OnlineHeatmapListAdapter onlineListAdapter = new OnlineHeatmapListAdapter(MainActivity.this, heatmapList);
      onlineHeatmaps.setAdapter(onlineListAdapter);

      CompletableFuture<DataSnapshot> dataSnapshotFuture = new CompletableFuture<>();
      heatmapsRef.addValueEventListener(new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
          dataSnapshotFuture.complete(dataSnapshot);
        }
        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
          heatmapList.clear();
          onlineListAdapter.notifyDataSetChanged();
        }
      });


      dataSnapshotFuture.thenAccept(dataSnapshot -> {
        for (DataSnapshot heatmapSnapshot : dataSnapshot.getChildren()) {
          String heatmapKey = heatmapSnapshot.getKey();
          Heatmap heatmap = heatmapSnapshot.getValue(Heatmap.class);
          heatmapList.add(heatmap);
        }

        // update data after fetching
        onlineListAdapter.notifyDataSetChanged();
      });


      AdapterView.OnItemClickListener dialogHandler = (parent, v, position, id) -> {
        Heatmap heatmap = heatmapList.get(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(parent.getContext());
        builder.setTitle("Actions")
                .setMessage("What do you will you do with this heatmap?")
                .setPositiveButton("Open", (dialog, which) -> {
                  Log.d("Test", "opened online");
                })
                .setNegativeButton("Delete", (dialog, which) -> {
                  Log.d("Test", "deleted online");
                })
                .show();
      };

      onlineHeatmaps.setOnItemClickListener(dialogHandler);
    }
  }

  private void deleteHeatmap(String fileName) {
    this.deleteFile(fileName);
  }
}