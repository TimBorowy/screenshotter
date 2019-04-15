package com.timborowy.screenshotter.Activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
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
import com.timborowy.screenshotter.Adapters.GalleryImageAdapter;
import com.timborowy.screenshotter.Interfaces.IRecyclerViewClickListener;
import com.timborowy.screenshotter.R;
import com.timborowy.screenshotter.VolleyMultipartRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class GalleryActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;


    final String[] images = new String[50];
    final String JSONUrl = "https://upload.borowy.nl/api/1";
    final String UploadUrl = "https://upload.borowy.nl/api/upload";

    String UploadKey;
    Boolean RefreshUI;

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_TAKE_PHOTO = 1;

    String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery_nav_drawer);

        recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
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
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: go to camera view using the FAB
                Snackbar.make(view, "Adding photos is not yet posible", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
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
                        Toast.makeText(getApplicationContext(),"Got API data", Toast.LENGTH_SHORT).show();

                        // Handle JSON response in separate function
                        handleResponse(response);
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Handle Volley/API error
                        Toast.makeText(getApplicationContext(),"API is offline", Toast.LENGTH_SHORT).show();

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

        Log.d("screenshotter", response.toString());

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

            Intent intent = getIntent();

            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            //imageView.setImageBitmap(imageBitmap);

            //String photoURI = data.getStringExtra(MediaStore.EXTRA_OUTPUT);
            Toast.makeText(getApplicationContext(), "Took photo", Toast.LENGTH_SHORT);



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
                Toast.makeText(getApplicationContext(), "Error saving file", Toast.LENGTH_SHORT);
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);

                Toast.makeText(getApplicationContext(), photoURI.toString(), Toast.LENGTH_SHORT);

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
                Log.d("screenshotter", resultResponse);
                try {
                    JSONObject result = new JSONObject(resultResponse);
                    String imageURL = result.getString("URL");


                    if (imageURL.contains("https")) {
                        // tell everybody you have successfully uploaded image and posted strings
                        Log.i("Message", imageURL);

                        // refresh UI

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
                            errorMessage = message+" Something is getting wrong";
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

                params.put("key", UploadKey);
                return params;
            }

            @Override
            protected Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();
                // file name could found file base or direct access from real path
                // for now just get bitmap data from ImageView
                params.put("file_input", new DataPart("file.jpg", getFileDataFromBitmap(imageBitmap), "image/jpg"));

                return params;
            }
        };

        // Add the request to the RequestQueue.
        queue.add(multipartRequest);

        //VolleySingleton.getInstance(getBaseContext()).addToRequestQueue(multipartRequest);
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






    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
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
            dispatchTakePictureIntent();

        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

}
