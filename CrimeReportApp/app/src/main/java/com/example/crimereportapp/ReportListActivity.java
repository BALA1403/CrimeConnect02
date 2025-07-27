package com.example.crimereportapp;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class ReportListActivity extends AppCompatActivity {

    private static final String TAG = "ReportListActivity";
    private ListView listView;
    private CrimeReportDBHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_list);

        listView = findViewById(R.id.listViewReports);
        db = CrimeReportDBHelper.getInstance(this);

        loadReports();
    }

    private void loadReports() {
        new Thread(() -> {
            try {
                // Use the direct method from CrimeReportDBHelper, not a DAO
                List<CrimeReport> reports = db.getAllReports();
                List<String> reportTitles = new ArrayList<>();

                for (CrimeReport report : reports) {
                    reportTitles.add(report.getTitle() + " - " + report.getTimestamp());
                }

                runOnUiThread(() -> {
                    if (reportTitles.isEmpty()) {
                        Toast.makeText(this, "No reports found", Toast.LENGTH_SHORT).show();
                    } else {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                                android.R.layout.simple_list_item_1, reportTitles);
                        listView.setAdapter(adapter);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading reports", e);
                runOnUiThread(() ->
                        Toast.makeText(this, "Error loading reports: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}