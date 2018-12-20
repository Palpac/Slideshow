package palpac.slideshow;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    static String sdcard_path, diapo_path, directory_path, directory_path_saved;
    static int diapo_index = 0; // Use STATIC to save value...
    static int count_onCreate; // ...even if user rotate device...
    static float screen_height, screen_width, diapo_height, diapo_width;
    TouchImageView  image_view_diapo; //... because orientation change = appRestart...
    static List<File> diapo_list; // ... and do onCreate again (new diapo list) + reset view
    static Bitmap diapo_bitmap;
    static boolean is_playing = true, first_run, random_checkbox, recursive_checkbox, random_saved, recursive_saved;
    Timer timer;
    MyTimerTask myTimerTask;
    static int timer_delay, timer_delay_saved;
    Intent settings_intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        image_view_diapo = findViewById(R.id.image_view_diapo);
        image_view_diapo.setOnClickListener(image_view_diapo_listener);

        sdcard_path = Environment.getExternalStorageDirectory().getAbsolutePath();

        maj_preferences(); // Read preferences (timer delay & directory path & fist run)

        hideSystemUI(); // Real full screen

        if (first_run) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this); //  Open preferences
            SharedPreferences.Editor editor = preferences.edit(); // Edit mode
            editor.putBoolean("First_run", false); // Write first run flag
            editor.apply(); // Save value
            settings_intent = new Intent(MainActivity.this, Settings.class);
            startActivityForResult(settings_intent, 0155); // Lance Settings
        }

        if (!(count_onCreate == 1055)) { // Trick to detect 1st onCreate (to avoid onOrientationChange)

            try {
                if (recursive_checkbox) {
                    diapo_list = getListFiles2(new File(directory_path)); // Get all .jpg files in directory (recursive)
                }
                else {
                    diapo_list = getListFiles(new File(directory_path)); // Get all .jpg files in directory (recursive)
                }
                //Toast.makeText(getApplicationContext(), "Size : " + diapo_list.size(), Toast.LENGTH_SHORT).show();
                if (random_checkbox) {
                    Collections.shuffle(diapo_list); // Shuffle the list
                }
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Error, please select valid directory.", Toast.LENGTH_LONG).show();
                save_default_path();
                settings_intent = new Intent(MainActivity.this, Settings.class);
                startActivityForResult(settings_intent, 0155); // Lance Settings
            }

            if (diapo_list.size()>0) {
                diapo_path = diapo_list.get(0).getPath(); // Get 1st diapo path
                show_diapo(diapo_path); // Show 1st diapo
                if (!first_run) { // Avoid starting slideshow on settings activity
                    create_timer(); // Start diaporama
                }
            }
            else {
                cancel_timer();
                Toast.makeText(getApplicationContext(), "No picture found ! Please select another directory.", Toast.LENGTH_LONG).show();
                save_default_path();
                settings_intent = new Intent(MainActivity.this, Settings.class);
                startActivityForResult(settings_intent, 0155); // Lance Settings
            }

            count_onCreate = 1055;

        }
        else { // onCreate already launched, only update
            diapo_path = diapo_list.get(diapo_index).getPath();
            show_diapo(diapo_path); // Update diapo
            if (is_playing) { // Update timer task
                cancel_timer();
                create_timer();
            }
        }


        //////////////////////////////////////////////////////////////////////////////////////////// ON SWIPE LISTENER
        image_view_diapo.setOnTouchListener( new OnSwipeTouchListener(getApplicationContext()) {
            public void onSwipeTop() {
                //Toast.makeText(getApplicationContext(), "top", Toast.LENGTH_SHORT).show();
            }
            public void onSwipeLeft() {
                diapo_index++; // Increase index
                if (diapo_index >= diapo_list.size() ) {
                    diapo_index = 0;
                }
                //Toast.makeText(getApplicationContext(), "Index : "+diapo_index, Toast.LENGTH_SHORT).show();
                diapo_path = diapo_list.get(diapo_index).getPath();
                show_diapo(diapo_path); // Show diapo
            }
            public void onSwipeRight() {
                diapo_index--; // Decrease index
                if (diapo_index < 0 ) {
                    diapo_index = diapo_list.size()-1;
                }
                //Toast.makeText(getApplicationContext(), "Index : "+diapo_index, Toast.LENGTH_SHORT).show();
                diapo_path = diapo_list.get(diapo_index).getPath();
                show_diapo(diapo_path); // Show diapo
            }
            public void onSwipeBottom() {
                //Toast.makeText(getApplicationContext(), "bottom", Toast.LENGTH_SHORT).show();
            }

        });

        //////////////////////////////////////////////////////////////////////////////////////////// LONG CLICK listener
        image_view_diapo.setOnLongClickListener( new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //Toast.makeText(getApplicationContext(), "Settings", Toast.LENGTH_SHORT).show();
                directory_path_saved = directory_path; // Save current data to detect changes
                timer_delay_saved = timer_delay;
                random_saved = random_checkbox;
                recursive_saved = recursive_checkbox;
                if (is_playing) { // If diaporama playing
                    cancel_timer(); // Cancel during settings activity
                    is_playing = true; // But keep flag on to restart on return
                }
                settings_intent = new Intent(MainActivity.this, Settings.class);
                startActivityForResult(settings_intent, 0155); // Launch Settings
                return false;
            }
        });


    } // onCreate end

    @Override
    protected void onPause() {
        if (is_playing) { // If diaporama playing
            cancel_timer(); // Cancel
            is_playing = true; // But keep flag on, to restart on return
        }
        super.onPause();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////// RETURN from Settings
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        maj_preferences(); // Read settings
        if (!directory_path.equals(directory_path_saved) || !(recursive_checkbox == recursive_saved) || !(random_checkbox == random_saved)) { // Detect changes
            //Toast.makeText(getApplicationContext(), "Detected!", Toast.LENGTH_LONG).show();
            try {
                if (recursive_checkbox) {
                    diapo_list = getListFiles2(new File(directory_path)); // Update list (recursive)
                }
                else {
                    diapo_list = getListFiles(new File(directory_path)); // Update list (non recursive)
                }
                //Toast.makeText(getApplicationContext(), "New size : " + diapo_list.size(), Toast.LENGTH_SHORT).show();
                if (random_checkbox) {
                    Collections.shuffle(diapo_list); // Shuffle list
                }
                diapo_index = 0; // Reset index
                if (diapo_list.size() > 0) {
                    diapo_path = diapo_list.get(diapo_index).getPath();
                    show_diapo(diapo_path); // Update diapo
                }
                else {
                    Toast.makeText(getApplicationContext(), "No picture found ! Please select another directory.", Toast.LENGTH_LONG).show();
                    save_default_path();
                    settings_intent = new Intent(MainActivity.this, Settings.class);
                    startActivityForResult(settings_intent, 0155); // Lance Settings
                    return;
                }
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Error, please select valid directory.", Toast.LENGTH_LONG).show();
                save_default_path();
                settings_intent = new Intent(MainActivity.this, Settings.class);
                startActivityForResult(settings_intent, 0155); // Lance Settings
                return;
            }
        }
        if (is_playing) { // If diaporama was playing
            //Toast.makeText(getApplicationContext(), "No changes", Toast.LENGTH_LONG).show();
            create_timer(); // Restart
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////// MAJ PREFERENCES
    private void maj_preferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        timer_delay = preferences.getInt("Timer_delay", 8);
        directory_path = preferences.getString("Directory_path", sdcard_path);
        first_run = preferences.getBoolean("First_run", true);
        random_checkbox = preferences.getBoolean("Random", true);
        recursive_checkbox = preferences.getBoolean("Recursive", true);
        //Toast.makeText(getApplicationContext(), "Delay = "+timer_delay+" Path = "+directory_path+"1st run : "+first_run, Toast.LENGTH_SHORT).show();
        //Toast.makeText(getApplicationContext(), "Random = "+random_checkbox+" Recursive = "+recursive_checkbox, Toast.LENGTH_SHORT).show();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////// SAVE DEFAULT PATH
    private void save_default_path() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this); //  Open preferences
        SharedPreferences.Editor editor = preferences.edit(); // Edit mode
        editor.putString("Directory_path", sdcard_path);
        editor.apply(); // Save values
    }


    //////////////////////////////////////////////////////////////////////////////////////////////// SIMPLE CLICK Listener
    private View.OnClickListener image_view_diapo_listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(is_playing) {
                Toast.makeText(getApplicationContext(), "Pause", Toast.LENGTH_SHORT).show();
                cancel_timer();
            }
            else {
                Toast.makeText(getApplicationContext(), "Play", Toast.LENGTH_SHORT).show();
                cancel_timer();
                create_timer(); // Create timer
            }
        }
    };

    //////////////////////////////////////////////////////////////////////////////////////////////// NON RECURSIVE PIC FILES LIST
    private List<File> getListFiles(File parentDir) {
        List<File> inFiles = new ArrayList<>();
        Queue<File> files = new LinkedList<>();
        files.addAll(Arrays.asList(parentDir.listFiles()));
        while (!files.isEmpty()) {
            File file = files.remove();
            if (!(file.getName().startsWith("."))) { // Ignore .dir
                if (!file.isDirectory()) {
                    if (file.getName().toLowerCase().endsWith(".jpg") || file.getName().toLowerCase().endsWith(".png")
                            || file.getName().toLowerCase().endsWith(".gif") || file.getName().toLowerCase().endsWith(".bmp")
                            || file.getName().toLowerCase().endsWith(".webp")) {
                        inFiles.add(file);
                    }
                }
            }
        }
        return inFiles;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////// RECURSIVE PIC FILES LIST
    private List<File> getListFiles2(File parentDir) {
        List<File> inFiles = new ArrayList<>();
        Queue<File> files = new LinkedList<>();
        files.addAll(Arrays.asList(parentDir.listFiles()));
        while (!files.isEmpty()) {
            File file = files.remove();
            if (!(file.getName().startsWith("."))) { // Ignore .dir
                if (file.isDirectory()) {
                    files.addAll(Arrays.asList(file.listFiles()));
                }
                else if (file.getName().toLowerCase().endsWith(".jpg") || file.getName().toLowerCase().endsWith(".png")
                        || file.getName().toLowerCase().endsWith(".gif") || file.getName().toLowerCase().endsWith(".bmp")
                        || file.getName().toLowerCase().endsWith(".webp")) {
                    inFiles.add(file);
                }
            }
        }
        return inFiles;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////// For REAL FULLSCREEN
    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }
    // Shows the system bars by removing all the flags
    // except for the ones that make the content appear under the system bars.
    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////// Get SCREEN SIZE
    private void get_screen_size() {
        screen_height = Resources.getSystem().getDisplayMetrics().widthPixels;
        screen_width = Resources.getSystem().getDisplayMetrics().heightPixels;
        //Toast.makeText(getApplicationContext(),  "Screen : " + screen_height + " " + screen_width, Toast.LENGTH_SHORT).show();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////// SHOW DIAPO
    private void show_diapo(String diapo_path) {
        get_screen_size(); // Get screen height and width
        diapo_bitmap = BitmapFactory.decodeFile(diapo_path);  // Create bitmap
        diapo_height = diapo_bitmap.getHeight();
        diapo_width = diapo_bitmap.getWidth();

        int orientation = getResources().getConfiguration().orientation; // Get device orientoation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) { // Scale bitmap in landscape
            diapo_bitmap = Bitmap.createScaledBitmap(diapo_bitmap,Math.round(diapo_width/(diapo_height/screen_height)),Math.round(screen_height),true);
        } else { // In portrait
            diapo_bitmap = Bitmap.createScaledBitmap(diapo_bitmap,Math.round(screen_width),Math.round(diapo_height/(diapo_width/screen_width)),true);
        }
        //diapo_bitmap = Bitmap.createScaledBitmap(diapo_bitmap,Math.round(diapo_width),Math.round(diapo_height),true);
        image_view_diapo.setBackgroundColor(getResources().getColor(R.color.black)); // Reset background
        image_view_diapo.setImageBitmap(diapo_bitmap); // Show diapo
    }

    class MyTimerTask extends TimerTask {

        @Override
        public void run() {

            runOnUiThread(new Runnable(){

                @Override
                public void run() {
                    //Toast.makeText(getApplicationContext(),  "Task", Toast.LENGTH_SHORT).show();
                    diapo_index++; // Increase index
                    //Toast.makeText(getApplicationContext(), "Index : "+diapo_index, Toast.LENGTH_SHORT).show();
                    if (diapo_index >= diapo_list.size() ) {
                        diapo_index = 0;
                    }
                    diapo_path = diapo_list.get(diapo_index).getPath();
                    show_diapo(diapo_path); // Show diapo
                }});
        }

    }

    //////////////////////////////////////////////////////////////////////////////////////////////// TIMER VOIDS
    private void create_timer() {
        timer = new Timer(); // Create new timer
        myTimerTask = new MyTimerTask(); // And new timer task
        timer.schedule(myTimerTask, timer_delay*1000, timer_delay*1000); // Delay task scheduled
        is_playing = true;
    }
    private void cancel_timer() {
        if (timer!=null){ // Cancel timer if exists
            timer.cancel();
            timer = null;
        }
        is_playing = false;
    }


} // activity end


