package com.timborowy.screenshotter.Activities;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.timborowy.screenshotter.Adapters.GalleryImageAdapter;
import com.timborowy.screenshotter.Interfaces.IRecyclerViewClickListener;
import com.timborowy.screenshotter.R;
import com.timborowy.screenshotter.VolleyMultipartRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
//import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;


public class GalleryActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;


    final String[] images = new String[50];
    final String JSONUrl = "https://upload.borowy.nl/api/1";
    final String UploadUrl = "https://upload.borowy.nl/api/upload";
    //final String UploadUrl = "http://164.132.226.87:3333/upload";

    private String UploadKey;
    private Boolean RefreshUI;
    Uri ImagePath;

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_TAKE_PHOTO = 1;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    public final static String LOG_TAG = "screenshotter";
    public final static int REQUEST_LAST_LOCATION_PERMISSION = 1;
    public final static int REQUEST_LOCATION_UPDATES_PERMISSION = 2;

    private boolean locationUpdates = true;
    private Location currentLocation;

    String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery_nav_drawer);

        recyclerView = findViewById(R.id.recyclerview);
        layoutManager = new GridLayoutManager(this,2);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);


        // Get upload key from preferences
        UploadKey = PreferenceManager.getDefaultSharedPreferences(this).getString("upload_key", "");
        // Get refresh preferences
        RefreshUI = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("refresh_ui_switch", false);


        // Make json request to upload api
        makeRequest();

        // Setup toolbar in this view
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Take a new photo with a FAB press
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Launch camera activity
                dispatchTakePictureIntent();
            }
        });

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        // GPS location code
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        createLocationRequest();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.d(LOG_TAG, "location unknown");
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    Log.d(LOG_TAG, "location: " + location.getLatitude() + ", " + location.getLongitude());

                    // Set current location to new location
                    currentLocation = location;
                }
            }
        };

        getLastKnownLocation();

    }


    @Override
    protected void onResume() {
        if (locationUpdates) {
            startLocationUpdates();
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
        super.onPause();
    }


    private void getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            String permissions[] = {Manifest.permission.ACCESS_FINE_LOCATION};
            ActivityCompat.requestPermissions(this, permissions, REQUEST_LAST_LOCATION_PERMISSION);
            Log.d(LOG_TAG, "No location permission");
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object
                            Log.d(LOG_TAG, "Last location available");
                        } else {
                            Log.d(LOG_TAG, "Last location unknown");
                        }
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (permissions.length == 0) {
            // no permissions in array, break early
            return;
        }

        switch (requestCode) {
            case REQUEST_LAST_LOCATION_PERMISSION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(LOG_TAG, "Permission granted");
                    getLastKnownLocation();
                } else {
                    Log.d(LOG_TAG, "Permission denied");
                }
                break;
            case REQUEST_LOCATION_UPDATES_PERMISSION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(LOG_TAG, "Permission granted (updates)");
                    startLocationUpdates();
                } else {
                    Log.d(LOG_TAG, "Permission denied (updates)");
                    locationUpdates = false;
                }

        }
    }

    private void createLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            String permissions[] = {Manifest.permission.ACCESS_FINE_LOCATION};
            ActivityCompat.requestPermissions(this, permissions, REQUEST_LOCATION_UPDATES_PERMISSION);

            Log.d(LOG_TAG, "No location permission (update)");
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                null  /*Looper*/ );
    }


    protected void makeRequest(){
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);


        // Make Request to image api
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest
                (Request.Method.GET, JSONUrl, null, new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {
                        // when ready, handle response
                        Log.i(LOG_TAG, "Got API data");

                        // Handle JSON response in separate function
                        handleResponse(response);
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Handle Volley/API error
                        Log.i(LOG_TAG, "API is offline");

                    }
                });

        // Add the request to the RequestQueue.
        queue.add(jsonArrayRequest);
    }

    protected String makeImageURL(JSONObject screenshot){
        // Get the current student (json object) data
        try{

            String slug = screenshot.getString("slug");
            String extension = screenshot.getString("name");

            // create full image url
            return "https://upload.borowy.nl/i/"+slug+"."+extension;

        } catch (Exception e){

            e.printStackTrace();
            return "Error";
        }
    }


    protected void handleResponse(JSONArray response){

        Log.d(LOG_TAG, response.toString());

        try{
            // Loop through the array elements
            for(int i=0;i<response.length();i++){
                // Get current json object
                JSONObject screenshot = response.getJSONObject(i);

                images[i] = makeImageURL(screenshot);

                // When clicked, go to fullscreen detail view
                final IRecyclerViewClickListener listener = new IRecyclerViewClickListener() {
                    @Override
                    public void onClick(View view, int position) {

                        Intent i = new Intent(getApplicationContext(), FullScreenActivity.class);

                        // Pass through image details
                        i.putExtra("IMAGES", images);
                        i.putExtra("POSITION", position);
                        startActivity(i);
                    }
                };

                // insert images into recycler view
                GalleryImageAdapter galleryImageAdapter = new GalleryImageAdapter(this, images, listener);
                recyclerView.setAdapter(galleryImageAdapter);
            }
        }catch (JSONException e){
            e.printStackTrace();
        }
    }


    // Handle photo that camera has taken
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {


            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");

            Toast.makeText(getApplicationContext(), "Took photo", Toast.LENGTH_SHORT).show();

            sendImageRequest(imageBitmap);
            //sendImageRequest(currentPhotoPath);
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        //Log.d(LOG_TAG, currentPhotoPath);
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Toast.makeText(getApplicationContext(), "Error saving file", Toast.LENGTH_SHORT).show();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);

                //Toast.makeText(getApplicationContext(), photoURI.toString(), Toast.LENGTH_SHORT);
                ImagePath = photoURI;
                Log.d(LOG_TAG, ImagePath.toString());

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    protected void sendImageRequest(final Bitmap imageBitmap){

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);

        VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(Request.Method.POST, UploadUrl, new Response.Listener<NetworkResponse>() {
            @Override
            public void onResponse(NetworkResponse response) {
                String resultResponse = new String(response.data);
                Log.d(LOG_TAG, resultResponse);
                try {
                    JSONObject result = new JSONObject(resultResponse);
                    String imageURL = result.getString("URL");


                    if (imageURL.contains("https")) {
                        // successfully uploaded
                        Log.i("Message", imageURL);
                        Toast.makeText(getApplicationContext(), "Photo successfully uploaded", Toast.LENGTH_SHORT).show();

                        // Refresh UI if setting is set
                        if(RefreshUI){
                            makeRequest();
                        }
                    } else {
                        Log.i("Unexpected", result.toString());
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse networkResponse = error.networkResponse;
                String errorMessage = "Unknown error";
                if (networkResponse == null) {
                    if (error.getClass().equals(TimeoutError.class)) {
                        errorMessage = "Request timeout";
                    } else if (error.getClass().equals(NoConnectionError.class)) {
                        errorMessage = "Failed to connect server";
                    }
                } else {
                    String result = new String(networkResponse.data);
                    try {
                        JSONObject response = new JSONObject(result);
                        String status = response.getString("status");
                        String message = response.getString("message");

                        Log.e("Error Status", status);
                        Log.e("Error Message", message);

                        if (networkResponse.statusCode == 404) {
                            errorMessage = "Resource not found";
                        } else if (networkResponse.statusCode == 401) {
                            errorMessage = message+" Please login again";
                        } else if (networkResponse.statusCode == 400) {
                            errorMessage = message+ " Check your inputs";
                        } else if (networkResponse.statusCode == 500) {
                            errorMessage = message+" Something went wrong";
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                Log.i("Error", errorMessage);
                error.printStackTrace();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();

                // Set Upload parameters like the upload key and location data
                params.put("key", UploadKey);
                params.put("location_data", currentLocation.getLatitude() + ", " + currentLocation.getLongitude());
                return params;
            }

            @Override
            protected Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();
                // file name could found file base or direct access from real path
                // for now just get bitmap data from ImageView
                params.put("file_input", new DataPart("file.jpg", getFileDataFromBitmap(imageBitmap), "image/jpg"));
                //params.put("file_input", new DataPart("file.jpg", getFileDataFromUri(ImagePath), "image/jpg"));
                //params.put("file_input", new DataPart("file.jpg", convertImageToByte(ImagePath), "image/jpg"));

                return params;
            }
        };

        // Add the request to the RequestQueue.
        queue.add(multipartRequest);
    }

    /**
     * Turn drawable into byte array.
     *
     * @param bitmapImage data
     * @return byte array
     */
    public static byte[] getFileDataFromBitmap(Bitmap bitmapImage) {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmapImage.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }


    /**
     * Turn image into byte array.
     *
     * @param imagePath data
     * @return byte array
     */
    public byte[] getFileDataFromUri(Uri imagePath) {

        try{

            Bitmap bitmapImage = MediaStore.Images.Media.getBitmap(getContentResolver(), imagePath);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmapImage.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e){
            //TODO: handle this.
            e.printStackTrace();
        }

        return new ByteArrayOutputStream().toByteArray();
    }

    public byte[] convertImageToByte(Uri uri){
        byte[] data=null;
        try {
            ContentResolver cr = getBaseContext().getContentResolver();
            InputStream inputStream = cr.openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            data = baos.toByteArray();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return data;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.navigation_drawer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {

            // start settings activity
            Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(i);

        } else if (id == R.id.action_cast){
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
            //dispatchTakePictureIntent();

        } else if (id == R.id.nav_gallery) {
            // Handle the gallery action

        } else if (id == R.id.nav_slideshow) {
            // Handle slideshow action

        } else if (id == R.id.nav_manage) {
            // Handle manage action

        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

}
