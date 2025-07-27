package com.example.crimereportapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.util.ArrayList;
import java.util.List;

public class EmergencyContactsActivity extends AppCompatActivity {

    private LinearLayout contactsContainer;
    private Button btnAddContact;
    private Button btnSaveContacts;
    private SharedPreferences sharedPreferences;
    private List<EditText> contactFields;
    private static final int MAX_CONTACTS = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_contacts);

        // Initialize views
        contactsContainer = findViewById(R.id.contactsContainer);
        btnAddContact = findViewById(R.id.btnAddContact);
        btnSaveContacts = findViewById(R.id.btnSaveContacts);

        // Initialize storage
        sharedPreferences = getSharedPreferences("EmergencyContacts", MODE_PRIVATE);
        contactFields = new ArrayList<>();

        // Set up click listeners
        btnAddContact.setOnClickListener(v -> addContactField());
        btnSaveContacts.setOnClickListener(v -> saveContacts());

        // Load saved contacts
        loadSavedContacts();
    }

    private void addContactField() {
        if (contactFields.size() >= MAX_CONTACTS) {
            Toast.makeText(this, "Maximum " + MAX_CONTACTS + " contacts allowed", Toast.LENGTH_SHORT).show();
            return;
        }

        // Inflate contact field layout
        View contactView = LayoutInflater.from(this).inflate(R.layout.contact_field_item, null);
        CardView contactCard = contactView.findViewById(R.id.contactCard);
        EditText contactField = contactView.findViewById(R.id.contactField);
        Button removeBtn = contactView.findViewById(R.id.btnRemove);

        // Set up remove button
        removeBtn.setOnClickListener(v -> {
            contactsContainer.removeView(contactCard);
            contactFields.remove(contactField);
            updateAddButtonVisibility();
        });

        // Add to lists and update UI
        contactFields.add(contactField);
        contactsContainer.addView(contactCard);
        updateAddButtonVisibility();
    }

    private void updateAddButtonVisibility() {
        btnAddContact.setVisibility(contactFields.size() >= MAX_CONTACTS ? View.GONE : View.VISIBLE);
    }

    private void loadSavedContacts() {
        for (int i = 1; i <= MAX_CONTACTS; i++) {
            String contact = sharedPreferences.getString("contact" + i, "");
            if (!contact.isEmpty()) {
                addContactField();
                contactFields.get(contactFields.size() - 1).setText(contact);
            }
        }
        if (contactFields.isEmpty()) {
            addContactField(); // Add at least one field if no contacts are saved
        }
    }

    private void saveContacts() {
        if (contactFields.size() < MAX_CONTACTS) {
            Toast.makeText(this, "Please add " + MAX_CONTACTS + " emergency contacts", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean allValid = true;
        SharedPreferences.Editor editor = sharedPreferences.edit();

        for (int i = 0; i < contactFields.size(); i++) {
            String number = contactFields.get(i).getText().toString().trim();
            if (!isValidPhoneNumber(number)) {
                allValid = false;
                contactFields.get(i).setError("Enter valid 10-digit number");
            } else {
                editor.putString("contact" + (i + 1), number);
            }
        }

        if (allValid) {
            editor.apply();
            Toast.makeText(this, "Contacts saved successfully!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private boolean isValidPhoneNumber(String number) {
        return number.matches("\\d{10}");
    }
}