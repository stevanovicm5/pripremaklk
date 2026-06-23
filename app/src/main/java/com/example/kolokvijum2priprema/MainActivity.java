package com.example.kolokvijum2priprema;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.room.Room;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    TextView tvLocation;
    ImageButton btnCamera;
    ImageView imageView;
    Switch swPosts;
    Button btnDelete;
    private static final int NOTIF_REQUEST_CODE = 300;
    private SensorManager sensorManager;
    private Sensor gyroscope;

    private float gyroX;
    private float gyroY;
    private float gyroZ;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private FusedLocationProviderClient fusedLocationClient;

    AppDatabase db;
    boolean isFirstTime = true;

    private Sensor accelerometer;
    private float accX, accY, accZ;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvLocation = findViewById(R.id.tvLocation);
        btnCamera = findViewById(R.id.btnCamera);
        imageView = findViewById(R.id.imageView);
        swPosts = findViewById(R.id.swPosts);
        btnDelete = findViewById(R.id.btnDelete);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getLocation();

        db = Room.databaseBuilder(
                        getApplicationContext(),
                        AppDatabase.class,
                        "app_db"
                ).allowMainThreadQueries()
                .build();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {

            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        NOTIF_REQUEST_CODE
                );
            }
        }

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Bitmap bitmap =
                                (Bitmap) result.getData()
                                        .getExtras()
                                        .get("data");

                        imageView.setImageBitmap(bitmap);
                        Toast.makeText(
                                this,
                                "X: " + gyroX +
                                        " Y: " + gyroY +
                                        " Z: " + gyroZ,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
        btnCamera.setOnClickListener(v -> {

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.CAMERA
                        },
                        200
                );


                return;
            }
            Log.d(TAG, "onCreate:************** ");
            Intent intent =
                    new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            cameraLauncher.launch(intent);

        });
        sensorManager =
                (SensorManager) getSystemService(
                        SENSOR_SERVICE
                );

        gyroscope =
                sensorManager.getDefaultSensor(
                        Sensor.TYPE_GYROSCOPE
                );
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        ApiService apiService =
                RetrofitClient
                        .getInstance()
                        .create(ApiService.class);



        apiService.getPosts().enqueue(
                new Callback<List<Post>>() {

                    @Override
                    public void onResponse(
                            Call<List<Post>> call,
                            Response<List<Post>> response) {

                        if(response.isSuccessful()) {

                            List<Post> posts =
                                    response.body();

                            Log.d(
                                    "******************************API",
                                    posts.get(0).getTitle()
                            );
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<List<Post>> call,
                            Throwable t) {

                        Log.e(
                                "API",
                                t.getMessage()
                        );
                    }
                });
        swPosts.setOnCheckedChangeListener((buttonView, isChecked) -> {

            if (isChecked) {

                if (isFirstTime) {
                    loadFromApiAndSave();
                    isFirstTime = false;
                } else {
                    showFirstFromDb();

                }


            }
            else {
                checkContactsPermission();
                saveTextView();
                loadFirstContact();
            }
        });
        btnDelete.setOnClickListener(v -> {

            PostEntity first = db.postDao().getFirstPost();

            if (first != null) {

                db.postDao().deleteFirst();

                Toast.makeText(this, "Obrisan post", Toast.LENGTH_SHORT).show();

            } else {

                showNotification();
            }
        });

        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);

    }
    private void checkContactsPermission() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    400
            );
        }
    }
    private void loadFirstContact() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Cursor cursor = getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {

            String name = cursor.getString(
                    cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            );

            tvLocation.setText(name);

            cursor.close();
        }
    }
    private void saveTextView() {

        String text = tvLocation.getText().toString();

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("saved_text", text);
        editor.apply();
    }
    private void showNotification() {

        NotificationManager manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        String channelId = "posts_channel";

        NotificationChannel channel =
                new NotificationChannel(
                        channelId,
                        "Posts",
                        NotificationManager.IMPORTANCE_DEFAULT
                );

        manager.createNotificationChannel(channel);

        Notification notification =
                new Notification.Builder(this, channelId)
                        .setContentTitle("Info")
                        .setContentText("Nema više postova!")
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .build();

        manager.notify(1, notification);
    }
    private void showFirstFromDb() {

        PostEntity post = db.postDao().getFirstPost();

        if (post != null) {
            Toast.makeText(this, post.title, Toast.LENGTH_SHORT).show();
        }
    }
    private void loadFromApiAndSave() {

        ApiService apiService =
                RetrofitClient.getInstance().create(ApiService.class);

        apiService.getPosts().enqueue(new Callback<List<Post>>() {
            @Override
            public void onResponse(Call<List<Post>> call, Response<List<Post>> response) {

                if (response.isSuccessful() && response.body() != null) {

                    List<Post> posts = response.body();

                    List<PostEntity> entities = new ArrayList<>();

                    for (int i = 0; i < Math.min(10, posts.size()); i++) {

                        Post post = posts.get(i);

                        PostEntity entity = new PostEntity();
                        entity.id = post.getId();
                        entity.title = post.getTitle();

                        entities.add(entity);
                    }

                    db.postDao().insertAll(entities);

                    Log.d("DB", "Ubaceno 10 postova");
                }
            }

            @Override
            public void onFailure(Call<List<Post>> call, Throwable t) {
                Log.e("API", t.getMessage());
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();

        sensorManager.registerListener(
                this,
                gyroscope,
                SensorManager.SENSOR_DELAY_NORMAL
        );
        sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL
        );
    }
    @Override
    protected void onPause() {
        super.onPause();

        sensorManager.unregisterListener(this);
    }
    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {

            gyroX = event.values[0];
            gyroY = event.values[1];
            gyroZ = event.values[2];

        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            accX = event.values[0];
            accY = event.values[1];
            accZ = event.values[2];

            btnDelete.setText(
                    "X: " + accX +
                            " Y: " + accY +
                            " Z: " + accZ
            );
        }
    }
    @Override
    public void onAccuracyChanged(
            Sensor sensor,
            int accuracy) {

    }
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (grantResults.length == 0 ||
                grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (requestCode == 100) {

            getLocation();
        }
        if (requestCode == 200) {
            Intent intent =
                    new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            cameraLauncher.launch(intent);
        }
        if (requestCode == NOTIF_REQUEST_CODE) {
            Log.d("NOTIF", "Permisija odobrena");
        }
    }
    private void getLocation(){
        if(ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    100
            );
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {

                    if (location != null) {

                        double lat = location.getLatitude();
                        double lng = location.getLongitude();

                        tvLocation.setText(
                                lat + ", " + lng
                        );
                    }
                });
    }


}