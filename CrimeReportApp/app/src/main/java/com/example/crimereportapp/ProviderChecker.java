package com.example.crimereportapp;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

public class ProviderChecker {

    // Method to check if a provider is available
    public boolean isProviderAvailable(Context context, String authority) {
        try {
            context.getPackageManager().getProviderInfo(ComponentName.unflattenFromString(authority), 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    // Example usage of the isProviderAvailable method
    public void checkProvider(Context context, String authority) {
        if (isProviderAvailable(context, authority)) {
            // Provider is available, proceed with your logic
            Log.d("ProviderChecker", "Provider is available");
        } else {
            // Provider is not available, handle the absence gracefully
            Log.e("ProviderChecker", "Provider not available");
        }
    }
}