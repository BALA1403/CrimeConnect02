package com.example.crimereportapp;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sos_alerts")
public class SOSAlert {
    @PrimaryKey(autoGenerate = true)
    private long id;

    private String contacts;
    private String location;
    private long timestamp;

    // Getters and setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getContacts() {
        return contacts;
    }

    public void setContacts(String contacts) {
        this.contacts = contacts;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}