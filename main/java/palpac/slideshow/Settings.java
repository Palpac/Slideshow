package palpac.slideshow;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class Settings extends AppCompatActivity {

    EditText timer_delay_edit;
    TextView textview_path;
    String choosen_directory, sdcard_path;
    int timer_delay_value;
    public int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 0;
    CheckBox random, recursive;
    Boolean random_checked, recursive_checked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        timer_delay_edit = findViewById(R.id.time_delay);
        textview_path = findViewById(R.id.path);
        random = findViewById(R.id.random);
        recursive = findViewById(R.id.recursive);

        sdcard_path = Environment.getExternalStorageDirectory().getAbsolutePath();

        read_preferences();

    }

    private void read_preferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this); //  Open preferences
        timer_delay_value = preferences.getInt("Timer_delay", 8); // Read saved values
        choosen_directory = preferences.getString("Directory_path", sdcard_path);
        timer_delay_edit.setText(String.valueOf(timer_delay_value));
        textview_path.setText(choosen_directory);
        random_checked = preferences.getBoolean("Random", true);
        if (random_checked) {
            random.setChecked(true);
        }
        else {
            random.setChecked(false);
        }
        recursive_checked = preferences.getBoolean("Recursive", true);
        if (recursive_checked) {
            recursive.setChecked(true);
        }
        else  {
            recursive.setChecked(false);
        }
    }

    public void onClick_button (View view) {
        switch (view.getId()) {
            case R.id.choose_dir:
                int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if (permissionCheck == PackageManager.PERMISSION_GRANTED) { // Permission granted
                    //Toast.makeText(getApplicationContext(), "SDcard : permission granded", Toast.LENGTH_LONG).show();
                    SimpleFileDialog FileOpenDialog = new SimpleFileDialog(this, "FolderChoose",
                            new SimpleFileDialog.SimpleFileDialogListener() {
                                @Override
                                public void onChosenDir(String chosenDir) {
                                    choosen_directory = chosenDir; // Update path chosen
                                    textview_path.setText(choosen_directory);
                                }
                            });
                    FileOpenDialog.Default_File_Name = "";
                    FileOpenDialog.chooseFile_or_Dir();
                } else {
                    //Toast.makeText(getApplicationContext(),  "Asking for read/write SDcard permission", Toast.LENGTH_LONG).show();
                    ActivityCompat.requestPermissions(Settings.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                }
                break;
            case R.id.rate:
                try
                {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
                }
                catch (ActivityNotFoundException e)
                {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id="+getPackageName())));
                }
                break;
            case R.id.donate:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.me/davidpalpacuer")));
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) { // Handle user permission response
        if (requestCode == MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE) {
            // Request for Write SDcard permission.
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission has been granted.
                SimpleFileDialog FileOpenDialog =  new SimpleFileDialog(this, "FileOpen",
                        new SimpleFileDialog.SimpleFileDialogListener() {
                            @Override
                            public void onChosenDir(String chosenDir) {
                                choosen_directory = chosenDir; // Update path choosen
                                textview_path.setText(choosen_directory);
                            }
                        });
                FileOpenDialog.Default_File_Name = "";
                FileOpenDialog.chooseFile_or_Dir();
            } else {
                // Permission request was denied.
                Toast.makeText(getApplicationContext(),  "Application need permission", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void onBackPressed() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this); //  Open preferences
        SharedPreferences.Editor editor = preferences.edit(); // Edit mode
        timer_delay_value = Integer.valueOf(timer_delay_edit.getText().toString()); // Read delay
        editor.putInt("Timer_delay", timer_delay_value); // Write delay
        editor.putString("Directory_path", choosen_directory);
        random_checked = random.isChecked();
        editor.putBoolean("Random", random_checked);
        recursive_checked = recursive.isChecked();
        editor.putBoolean("Recursive", recursive_checked);
        editor.apply(); // Save values
        super.onBackPressed();
    }

}



