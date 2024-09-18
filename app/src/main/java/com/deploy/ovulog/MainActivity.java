package com.deploy.ovulog;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.deploy.ovulog.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * MainActivity class for the OvuLog period tracking app.
 * This activity allows users to input their last period date,
 * view the next expected period date, and receive notifications.
 */
public class MainActivity extends AppCompatActivity {

    // UI elements
    private EditText lastPeriodInput;
    private Button saveButton;
    private TextView nextPeriodInfo;

    // SharedPreferences for storing user data
    private SharedPreferences sharedPreferences;

    // Constants
    private static final String CHANNEL_ID = "PeriodTrackerChannel";
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        lastPeriodInput = findViewById(R.id.lastPeriodDateInput);
        saveButton = findViewById(R.id.saveButton);
        nextPeriodInfo = findViewById(R.id.nextPeriodInfo);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("PeriodTrackerPrefs", MODE_PRIVATE);

        // Load saved data and update UI
        loadLastPeriodDate();

        // Create notification channel for Android 8.0 and above
        createNotificationChannel();

        // Set click listener for save button
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveLastPeriodDate();
            }
        });
    }

    /**
     * Loads the last saved period date from SharedPreferences and updates the UI.
     */
    private void loadLastPeriodDate() {
        String lastPeriodDate = sharedPreferences.getString("lastPeriodDate", "");
        lastPeriodInput.setText(lastPeriodDate);
        updateNextPeriodInfo(lastPeriodDate);
    }

    /**
     * Saves the entered last period date to SharedPreferences and updates the UI.
     */
    private void saveLastPeriodDate() {
        String lastPeriodDate = lastPeriodInput.getText().toString();
        if (isValidDate(lastPeriodDate)) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("lastPeriodDate", lastPeriodDate);
            editor.apply();
            updateNextPeriodInfo(lastPeriodDate);
            scheduleNotification(lastPeriodDate);
            Toast.makeText(this, "Date saved", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Invalid date format. Use YYYY-MM-DD", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Validates if the input string is a valid date in the format YYYY-MM-DD.
     * @param dateStr The date string to validate
     * @return true if the date is valid, false otherwise
     */
    private boolean isValidDate(String dateStr) {
        try {
            dateFormat.parse(dateStr);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * Updates the UI with information about the next expected period date.
     * @param lastPeriodDate The date of the last period
     */
    private void updateNextPeriodInfo(String lastPeriodDate) {
        if (!lastPeriodDate.isEmpty()) {
            try {
                Date date = dateFormat.parse(lastPeriodDate);
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                calendar.add(Calendar.DAY_OF_MONTH, 28); // Assuming a 28-day cycle
                String nextPeriodDate = dateFormat.format(calendar.getTime());
                nextPeriodInfo.setText("Next period expected on: " + nextPeriodDate);
            } catch (ParseException e) {
                nextPeriodInfo.setText("Error calculating next period date");
            }
        } else {
            nextPeriodInfo.setText("Enter your last period date");
        }
    }

    /**
     * Schedules a notification for 3 days before the next expected period.
     * @param lastPeriodDate The date of the last period
     */
    private void scheduleNotification(String lastPeriodDate) {
        try {
            Date date = dateFormat.parse(lastPeriodDate);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.DAY_OF_MONTH, 25); // 3 days before next period

            Intent intent = new Intent(this, NotificationReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        } catch (ParseException e) {
            Toast.makeText(this, "Error scheduling notification", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Creates a notification channel for Android 8.0 (API level 26) and above.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Period Tracker Notifications";
            String description = "Channel for Period Tracker notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * BroadcastReceiver for handling and displaying notifications.
     */
    public static class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    // .setSmallIcon(R.drawable.ic_notification) // Uncomment and set a proper icon
                    .setContentTitle("Period Tracker")
                    .setContentText("Your period is expected in 3 days.")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true);

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(1, builder.build());
        }
    }
}