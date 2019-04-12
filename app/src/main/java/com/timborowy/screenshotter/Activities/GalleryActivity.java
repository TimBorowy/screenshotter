package com.timborowy.screenshotter.Activities;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.timborowy.screenshotter.Adapters.GalleryImageAdapter;
import com.timborowy.screenshotter.Interfaces.IRecyclerViewClickListener;
import com.timborowy.screenshotter.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class GalleryActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;
    final String[] images = new String[50];
    final String JSONUrl = "https://upload.borowy.nl/api/1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery_nav_drawer);

        recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        layoutManager = new GridLayoutManager(this,2);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);

        // make json request to upload api
        makeRequest();



        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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


        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest
                (Request.Method.GET, JSONUrl, null, new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {
                        // when ready, handle response
                        Toast.makeText(getApplicationContext(),"Got API data", Toast.LENGTH_SHORT).show();
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


    protected void handleResponse(JSONArray response){
        //textView.setText("Response: " + response.toString());
        Log.d("screenshotter", response.toString());

        try{
            // Loop through the array elements
            for(int i=0;i<response.length();i++){
                // Get current json object
                JSONObject screenshot = response.getJSONObject(i);

                // Get the current student (json object) data
                String slug = screenshot.getString("slug");
                String extension = screenshot.getString("name");

                // create full image url
                images[i] = "https://upload.borowy.nl/i/"+slug+"."+extension;


                // when clicked, go to fullscreen detail view
                final IRecyclerViewClickListener listener = new IRecyclerViewClickListener() {
                    @Override
                    public void onClick(View view, int position) {
                        // open full screen activity with image clicked

                        Intent i = new Intent(getApplicationContext(), FullScreenActivity.class);
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
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

}
