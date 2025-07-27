package com.example.crimereportapp;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface SOSAlertDao {
    @Insert
    long insert(SOSAlert alert);

    @Update
    void update(SOSAlert alert);

    @Delete
    void delete(SOSAlert alert);

    @Query("SELECT * FROM sos_alerts ORDER BY timestamp DESC")
    List<SOSAlert> getAllAlerts();

    @Query("SELECT * FROM sos_alerts WHERE id = :id")
    SOSAlert getAlertById(long id);
}