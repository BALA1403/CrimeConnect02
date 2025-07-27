package com.example.crimereportapp;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.crimereportapp.CrimeReport;

import java.util.List;

@Dao
public interface CrimeReportDao {

    @Insert
    void insertReport(com.example.crimereportapp.CrimeReport report);

    @Query("SELECT * FROM crime_reports ORDER BY id DESC")
    List<com.example.crimereportapp.CrimeReport> getAllReports();
}
