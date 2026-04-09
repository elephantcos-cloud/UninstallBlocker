package com.example.locker;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int REQUEST_CODE_ENABLE_ADMIN = 1;
    private DevicePolicyManager mDPM;
    private ComponentName mAdminName;
    private SharedPreferences prefs;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("AppLockerPrefs", MODE_PRIVATE);
        mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        mAdminName = new ComponentName(this, AdminReceiver.class);
        statusText = findViewById(R.id.status_text);

        if (!checkAllPermissions()) {
            statusText.setText("Setting up permissions...");
            requestAllPermissions();
        } else {
            statusText.setText("Permissions granted");
            setupPasswordUI();
        }
    }

    private boolean checkAllPermissions() {
        boolean overlay = Settings.canDrawOverlays(this);
        boolean writeSettings = Settings.System.canWrite(this);
        boolean admin = mDPM.isAdminActive(mAdminName);
        return overlay && writeSettings && admin;
    }

    private void requestAllPermissions() {
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mAdminName);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Required to prevent uninstallation.");
        startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);

        Intent overlayIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivity(overlayIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }

        Intent writeIntent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:" + getPackageName()));
        startActivity(writeIntent);

        Toast.makeText(this, "Please enable Accessibility Service", Toast.LENGTH_LONG).show();
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        
        new android.os.Handler().postDelayed(() -> finishAffinity(), 5000);
    }

    private void setupPasswordUI() {
        EditText passInput = findViewById(R.id.password_input);
        Button saveBtn = findViewById(R.id.save_btn);
        String savedPass = prefs.getString("MasterPassword", null);
        
        if (savedPass == null) {
            saveBtn.setOnClickListener(v -> {
                String pass = passInput.getText().toString();
                if (!pass.isEmpty()) {
                    prefs.edit().putString("MasterPassword", pass).apply();
                    Toast.makeText(this, "Password Set Successfully", Toast.LENGTH_SHORT).show();
                    finishAffinity();
                } else {
                    statusText.setText("Please enter a password");
                }
            });
        } else {
            saveBtn.setOnClickListener(v -> {
                if (passInput.getText().toString().equals(savedPass)) {
                    Toast.makeText(this, "Access Granted", Toast.LENGTH_SHORT).show();
                    statusText.setText("Access Granted");
                } else {
                    statusText.setText("Wrong Password");
                    Toast.makeText(this, "Wrong Password", Toast.LENGTH_SHORT).show();
                    finishAffinity();
                }
            });
        }
    }
}
