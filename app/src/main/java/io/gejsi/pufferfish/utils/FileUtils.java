package io.gejsi.pufferfish.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FileUtils {
  @SuppressWarnings("ResultOfMethodCallIgnored")
  public static void importDatabaseDump(Activity activity, Uri zipUri) {
    try {
      InputStream inputStream = activity.getContentResolver().openInputStream(zipUri);
      if (inputStream != null) {
        // Create a temporary directory to extract the files
        File tempDir = new File(activity.getCacheDir(), "temp");
        if (!tempDir.exists()) {
          tempDir.mkdirs();
        }

        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        ZipEntry zipEntry;

        while ((zipEntry = zipInputStream.getNextEntry()) != null) {
          File jsonFile = new File(tempDir, zipEntry.getName());

          FileOutputStream fileOutputStream = new FileOutputStream(jsonFile);
          byte[] buffer = new byte[1024];
          int length;
          while ((length = zipInputStream.read(buffer)) > 0) {
            fileOutputStream.write(buffer, 0, length);
          }
          fileOutputStream.close();

          saveImportedFile(activity, jsonFile);
        }

        zipInputStream.close();
        deleteDirectory(tempDir);

        // Show a toast indicating successful import
        Toast.makeText(activity, "Database dump imported successfully", Toast.LENGTH_SHORT).show();
      }
    } catch (IOException e) {
      e.printStackTrace();
      Toast.makeText(activity, "Error while importing the database dump.", Toast.LENGTH_SHORT).show();
    }
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  private static void deleteDirectory(File directory) {
    File[] contents = directory.listFiles();
    if (contents != null) {
      for (File f : contents) {
        deleteDirectory(f);
      }
    }

    directory.delete();
  }

  private static void saveImportedFile(Activity activity, File jsonFile) {
    try {
      StringBuilder jsonBuilder = new StringBuilder();
      BufferedReader reader = new BufferedReader(new FileReader(jsonFile));
      String line;
      while ((line = reader.readLine()) != null) {
        jsonBuilder.append(line);
      }
      reader.close();

      String fileName = jsonFile.getName();
      String fileContent = jsonBuilder.toString();
      FileOutputStream fileOutputStream = activity.openFileOutput(fileName, Context.MODE_PRIVATE);
      fileOutputStream.write(fileContent.getBytes());
      fileOutputStream.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void exportDatabaseDump(Activity activity) {
    List<String> jsonFiles = Arrays.stream(activity.fileList())
            .filter(fileName -> fileName.startsWith("Heatmap_") && fileName.endsWith(".json"))
            .collect(Collectors.toList());

    try {
      File zipFile = createZipFile(activity, jsonFiles);
      Uri fileUri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", zipFile);

      Intent intent = new Intent(Intent.ACTION_SEND);
      intent.setType("application/zip");
      intent.putExtra(Intent.EXTRA_STREAM, fileUri);
      intent.putExtra(Intent.EXTRA_SUBJECT, "Database Dump");
      intent.putExtra(Intent.EXTRA_TEXT, "Pufferfish: attached heatmaps database dump.");
      intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

      Intent chooser = Intent.createChooser(intent, "Share heatmaps dump.");
      List<ResolveInfo> resolveInfoList = activity.getPackageManager().queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY);
      for (ResolveInfo resolveInfo : resolveInfoList) {
        String packageName = resolveInfo.activityInfo.packageName;
        activity.grantUriPermission(packageName, fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
      }

      activity.startActivity(chooser);
    } catch (IOException e) {
      e.printStackTrace();
      Toast.makeText(activity, "Error while exporting the database dump.", Toast.LENGTH_SHORT).show();
    }
  }

  private static File createZipFile(Activity activity, List<String> jsonFiles) throws IOException {
    File zipFile = new File(activity.getCacheDir(), "heatmaps_dump.zip");

    FileOutputStream fileOutputStream = new FileOutputStream(zipFile);
    ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);

    byte[] buffer = new byte[1024];
    int length;
    for (String jsonFile : jsonFiles) {
      FileInputStream fileInputStream = activity.openFileInput(jsonFile);
      ZipEntry zipEntry = new ZipEntry(jsonFile);
      zipOutputStream.putNextEntry(zipEntry);
      while ((length = fileInputStream.read(buffer)) > 0) {
        zipOutputStream.write(buffer, 0, length);
      }
      fileInputStream.close();
    }

    zipOutputStream.close();
    fileOutputStream.close();

    return zipFile;
  }

  public static List<String> getLocalFiles(String[] fileList) {
    return Arrays.stream(fileList)
            .filter(fileName -> fileName.startsWith("Heatmap_") && fileName.endsWith(".json"))
            .sorted((fileName1, fileName2) -> {
              // Split the file names into parts
              String[] fileParts1 = fileName1.split("_");
              String[] fileParts2 = fileName2.split("_");
              String date1 = fileParts1[2];
              String time1 = fileParts1[3].substring(0, fileParts1[3].length() - 5);
              String date2 = fileParts2[2];
              String time2 = fileParts2[3].substring(0, fileParts2[3].length() - 5);

              // compare the date and time values
              int dateComparison = date2.compareTo(date1);
              if (dateComparison != 0) {
                return dateComparison; // sort by date in descending order
              } else {
                return time2.compareTo(time1); // sort by time in descending order
              }
            })
            .collect(Collectors.toList());
  }
}
