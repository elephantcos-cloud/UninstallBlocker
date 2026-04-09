package com.example.locker;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.EditText;
import android.widget.Toast;

public class LockService extends AccessibilityService {

    private static final String PREFS_NAME = "AppLockerPrefs";
    private String correctPassword = "1234";
    private boolean isAskingPassword = false;
    private int attemptCount = 0;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 50;
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS | 
                     AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        setServiceInfo(info);
        
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        correctPassword = prefs.getString("MasterPassword", "1234");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
            String className = event.getClassName() != null ? event.getClassName().toString() : "";
            
            if (packageName.contains("com.android.settings") || 
                packageName.contains("com.google.android.packageinstaller") ||
                packageName.contains("com.android.packageinstaller") ||
                className.contains("Uninstall") ||
                className.contains("ManagePermissions")) {
                
                if (!isAskingPassword) {
                    attemptCount++;
                    performGlobalAction(GLOBAL_ACTION_HOME);
                    
                    if (attemptCount >= 3) {
                        showPasswordDialog();
                        attemptCount = 0;
                    }
                }
            }
        }
    }

    private void showPasswordDialog() {
        isAskingPassword = true;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Master Password");
        builder.setMessage("This action is protected");
        
        final EditText input = new EditText(this);
        input.setHint("Enter password");
        builder.setView(input);
        
        builder.setPositiveButton("Unlock", (dialog, which) -> {
            if (input.getText().toString().equals(correctPassword)) {
                Toast.makeText(LockService.this, "Access Granted", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(LockService.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                isAskingPassword = false;
                attemptCount = 0;
            } else {
                Toast.makeText(LockService.this, "Wrong Password", Toast.LENGTH_SHORT).show();
                performGlobalAction(GLOBAL_ACTION_HOME);
                isAskingPassword = false;
            }
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            performGlobalAction(GLOBAL_ACTION_HOME);
            isAskingPassword = false;
        });
        
        builder.setCancelable(false);
        
        AlertDialog dialog = builder.create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);
        dialog.show();
    }

    @Override
    public void onInterrupt() {}
}
