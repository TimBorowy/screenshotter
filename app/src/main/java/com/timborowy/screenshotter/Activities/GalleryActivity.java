package com.timborowy.screenshotter.Activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.timborowy.screenshotter.Adapters.GalleryImageAdapter;
import com.timborowy.screenshotter.Interfaces.IRecyclerViewClickListener;
import com.timborowy.screenshotter.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

public class GalleryActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;
    final String[] images = new String[50];
    final String JSONUrl = "https://upload.borowy.nl/api/1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

       /* Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);*/

        recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        layoutManager = new GridLayoutManager(this,2);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);

        // make json request to upload api
        makeRequest();
    }


    protected void makeRequest(){
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);


        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest
                (Request.Method.GET, JSONUrl, null, new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {
                        // when ready, handle response
                        handleResponse(response);
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Handle error

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

}
