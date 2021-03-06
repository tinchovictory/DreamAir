package itba.dreamair2;

import android.content.BroadcastReceiver;
import android.app.Activity;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import itba.dreamair2.adapters.CustomCards;
import itba.dreamair2.adapters.FavoritesAdapter;
import itba.dreamair2.fragments.FavoritesFragment;
import itba.dreamair2.fragments.FlightDetailFragment;
import itba.dreamair2.fragments.MapFragment;
import itba.dreamair2.fragments.OffersFragment;
import itba.dreamair2.fragments.SettingsFragment;
import itba.dreamair2.httprequests.DealResponse;
import itba.dreamair2.httprequests.FlightsResponse;
import itba.dreamair2.httprequests.StatusResponse;
import itba.dreamair2.notifications.AlarmReceiver;
import itba.dreamair2.contracts.BroadcastContract;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, GoogleApiClient.OnConnectionFailedListener,GoogleApiClient.ConnectionCallbacks,LocationListener,SearchView.OnQueryTextListener {

    private final int LOCATIONPERMISSION=12;
    private final static String FLIGHT_STATUS_BASEURL = "http://hci.it.itba.edu.ar/v1/api/status.groovy?method=getflightstatus";

    private ArrayList<Flight> flights;
    private CustomCards adapter;
    private ArrayList<Flight> savedFlights;
    private FavoritesAdapter favAdapter;
    private GoogleApiClient mGoogleApiClient;

    private BroadcastReceiver broadcastReceiver;
    private SearchView searchView;
    private AlarmReceiver alarm;
    ActionBarDrawerToggle toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        savedFlights = loadFlightsFromLocalStorage();
        if(savedFlights==null){
            savedFlights= new ArrayList<>();
        }


        flights= new ArrayList<>();
        adapter = new CustomCards(this,flights);
        favAdapter= new FavoritesAdapter(this,savedFlights);
        new HttpGetDeals().execute();

        Fragment fragment = FavoritesFragment.newInstance(favAdapter,savedFlights);
        getSupportFragmentManager().beginTransaction().replace(R.id.container,fragment).commit();
        getSupportFragmentManager().beginTransaction().replace(R.id.container,fragment).addToBackStack(null).commit();
        Intent intent = getIntent();
        String menuFragment = intent.getStringExtra("menuFragment");
        if (menuFragment != null) {
            if (menuFragment.equals("notificationItem")) {
                Flight flight = intent.getParcelableExtra("flight");
                loadFlightDetailFragment(flight);
            }
        }


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this,this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(BroadcastContract.UPDATE_NOTIFICATIONS)) {
                    updateNotifications();
                }
            }
        };
        alarm = new AlarmReceiver();

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
        //toggle.setDrawerIndicatorEnabled(true);
        setDrawerState(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.search));

        searchView.setOnQueryTextListener(this);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyApplication.activityResumed();
        registerReceiver(broadcastReceiver, new IntentFilter(BroadcastContract.UPDATE_NOTIFICATIONS));

        //updateNotificationsList
        updateNotifications();
    }

    @Override
    protected void onPause() {
        super.onPause();

        saveFlightsToLocalStorage(savedFlights);
        MyApplication.activityPaused();
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement


        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Fragment fragment;

        if (id == R.id.nav_flights) {
            fragment= getSupportFragmentManager().findFragmentById(R.id.fragment_favorites);
            if(fragment== null) {
                fragment = FavoritesFragment.newInstance(favAdapter,savedFlights);
            }
                getSupportFragmentManager().beginTransaction().replace(R.id.container,fragment).commit();

        } else if (id == R.id.nav_sale) {
            fragment= getSupportFragmentManager().findFragmentById(R.id.fragment_offers);
            if(fragment== null) {
                fragment = OffersFragment.newInstance(adapter,flights);
            }
            getSupportFragmentManager().beginTransaction().replace(R.id.container,fragment).commit();

        } else if (id == R.id.nav_notification) {
            fragment= getSupportFragmentManager().findFragmentById(R.id.fragment_map);
            if(fragment== null) {
                fragment = MapFragment.newInstance(flights);
            }
            getSupportFragmentManager().beginTransaction().replace(R.id.container,fragment).commit();
        }  else if (id == R.id.nav_settings) {
            getFragmentManager().beginTransaction().replace(R.id.container, new SettingsFragment()).addToBackStack(null).commit();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    public void addFavoriteFlight(Flight flight){
        if(savedFlights.contains(flight)){
            Toast.makeText(this,getString(R.string.DElETED_FLIGHT),Toast.LENGTH_SHORT).show();
            savedFlights.remove(flight);

        }else{
            Toast.makeText(this,getString(R.string.ADDED_FLIGHT),Toast.LENGTH_SHORT).show();
            savedFlights.add(flight);
        }
    }

    public void loadSettingsFragment() {

    }

    public void setDrawerState(boolean isEnabled) {
        if ( isEnabled ) {
            //mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            toggle.onDrawerStateChanged(DrawerLayout.LOCK_MODE_UNLOCKED);
            toggle.setDrawerIndicatorEnabled(true);
            toggle.syncState();
            getSupportActionBar().setHomeButtonEnabled(true);


        }
        else {
            //mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            toggle.onDrawerStateChanged(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            toggle.setDrawerIndicatorEnabled(false);
            toggle.syncState();
            getSupportActionBar().setHomeButtonEnabled(false);

        }
    }

    public void loadFlightDetailFragment(final Flight flight){
        toggle.setDrawerIndicatorEnabled(false);
        final FlightDetailFragment fragment = FlightDetailFragment.newInstance(flight);
        getSupportFragmentManager().beginTransaction().replace(R.id.container,fragment).addToBackStack(null).commit();
        //setDrawerState(false);
        //Busco el estado del vuelo
        String url = FLIGHT_STATUS_BASEURL + "&airline_id=" + flight.getAirlineID() + "&flight_number=" + flight.getNumber().substring(3) ;

        ApiConnection apiConnection = new ApiConnection(url) {
            @Override
            protected void onPostExecute(String result) {
                if(result == null) {
                    Toast.makeText(getApplicationContext(), getString(R.string.CONNECTION_FAILED),Toast.LENGTH_LONG);
                    return;
                }

                Gson gson = new Gson();
                Type listType = new TypeToken<StatusResponse>() {
                }.getType();

                StatusResponse response= gson.fromJson(result,listType);

                flight.setStatus(getFlightStatusString(response.getStatus().getStatus()));
                if(response.getStatus().getDeparture().getAirport().getGate() != null) {
                    flight.setGate(response.getStatus().getDeparture().getAirport().getTerminal() + response.getStatus().getDeparture().getAirport().getGate());
                } else {
                    flight.setGate(getString(R.string.gateNotFound));
                }

                try {
                    fragment.updateFlightGate(flight.getGate());
                    fragment.updateFlightStatus(flight.getStatus());
                }catch (Exception e){
                    Log.v("error","Rompemo todo");
                }
            }
        };
        apiConnection.execute();

    }

    public void deleteSavedFlight(final int adapterPosition) {
        final Flight flight =savedFlights.remove(adapterPosition);
        favAdapter.notifyItemRemoved(adapterPosition);
        Snackbar.make(findViewById(R.id.fragment_favorites), getString(R.string.DElETED_FLIGHT), Snackbar.LENGTH_LONG)
                        .setAction(getString(R.string.DELETED_ACTION), new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                savedFlights.add(adapterPosition,flight);
                                favAdapter.notifyItemInserted(adapterPosition);
                            }
                        }).show();

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.v("MAP","Location changed"+location.getLatitude()+" "+ location.getLongitude());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case LOCATIONPERMISSION: {
                if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    startLocationServices();
                }else {
                    // Permiso denegado
                    Log.v("MAP", "permiso denegado");
                }

            }

        }

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},LOCATIONPERMISSION);
        }else{
            startLocationServices();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    public void startLocationServices(){
        Log.v("MAP", "Starting location services");
        try {
            LocationRequest locationRequest = LocationRequest.create().setPriority(LocationRequest.PRIORITY_LOW_POWER);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
        }catch (SecurityException e){
            Log.v("MAP","Permission failed");
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        String params[]=query.split(" ");
        if(params.length<2){
            Toast.makeText(getApplicationContext(),"No existe el vuelo",Toast.LENGTH_SHORT).show();
            searchView.setIconified(true);
            return true;
        }
        String url = FLIGHT_STATUS_BASEURL + "&airline_id=" + params[0] + "&flight_number=" + params[1] ;

        new ApiConnection(url){

            @Override
            protected void onPostExecute(String result) {
                Log.v("Search",result);
                Gson gson = new Gson();
                Type listType = new TypeToken<StatusResponse>() {
                }.getType();

                StatusResponse response= gson.fromJson(result,listType);
                Log.v("Search",response.getStatus()+"");
                if(response.getStatus()==null){
                    Toast.makeText(getApplicationContext(),"No existe el vuelo",Toast.LENGTH_SHORT).show();
                }else{
                    loadFlightDetailFragment(new Flight(response.getStatus()));
                }

            }
        }.execute();
        searchView.setIconified(true);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }


    private class HttpGetFlights extends AsyncTask<DealResponse.DealsBean, Void, String> {
        DealResponse.DealsBean deal;
        int it;
        String to;
        String date;

        public HttpGetFlights(DealResponse.DealsBean deal, int it){
            this.deal=deal;
            this.it=it;
        }

        @Override
        protected String doInBackground(DealResponse.DealsBean... params) {

            HttpURLConnection urlConnection = null;
            Calendar today= Calendar.getInstance();
            today.add(Calendar.DATE,it);
            SimpleDateFormat sdf= new SimpleDateFormat("yyyy-MM-dd");


            String from="BUE";
            to=deal.getCity().getId();
            date=sdf.format(today.getTime());


            try {
                URL url = new URL("http://hci.it.itba.edu.ar/v1/api/booking.groovy?method=getonewayflights&from="+from+"&to="+to+"&dep_date="+date+"&adults=1&children=0&infants=0&sort_key=total&page_size=300");
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                return readStream(in);
            } catch (Exception exception) {
                exception.printStackTrace();

                return null;
            } finally {
                if (urlConnection != null)
                    urlConnection.disconnect();
            }


        }

        @Override
        protected void onPostExecute(String result) {

            if(result == null) {
                Toast.makeText(getApplicationContext(), getString(R.string.CONNECTION_FAILED),Toast.LENGTH_LONG);
                return;
            }

            if(it>12){
                return;
            }
            Gson gson = new Gson();
            Type listType = new TypeToken<FlightsResponse>() {
            }.getType();

            FlightsResponse response= gson.fromJson(result,listType);
            if(!response.getFlights().isEmpty()) {
                FlightsResponse.FlightsBean flight = response.getFlights().get(0);

                if (flight.getPrice().getTotal().getTotal() == deal.getPrice()) {
                    flights.add(new Flight(flight,deal));
                    adapter.notifyItemInserted(flights.size()-1);
                }
                else{
                    new HttpGetFlights(deal,it+1).execute();
                }
            }else{
                new HttpGetFlights(deal,it+1).execute();
            }



        }

        private String readStream(InputStream inputStream) {
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                int i = inputStream.read();
                while (i != -1) {
                    outputStream.write(i);
                    i = inputStream.read();
                }
                return outputStream.toString();
            } catch (IOException e) {
                return "";
            }
        }
    }

    private class HttpGetDeals extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {

            HttpURLConnection urlConnection = null;

            try {
                URL url = new URL("http://hci.it.itba.edu.ar/v1/api/booking.groovy?method=getflightdeals&from=BUE");
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                return readStream(in);
            } catch (Exception exception) {
                exception.printStackTrace();
                return null;
            } finally {
                if (urlConnection != null)
                    urlConnection.disconnect();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if(result == null) {
                Toast.makeText(getApplicationContext(), getString(R.string.CONNECTION_FAILED),Toast.LENGTH_LONG);
                return;
            }

            Gson gson = new Gson();
            Type listType = new TypeToken<DealResponse>() {
            }.getType();

            DealResponse response= gson.fromJson(result,listType);
            if(response.getDeals() == null) {
                Toast.makeText(getApplicationContext(), getString(R.string.CONNECTION_FAILED),Toast.LENGTH_LONG);
            }
            for(DealResponse.DealsBean deal :response.getDeals()){
                new HttpGetFlights(deal,2).execute(deal);
            }

        }

        private String readStream(InputStream inputStream) {
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                int i = inputStream.read();
                while (i != -1) {
                    outputStream.write(i);
                    i = inputStream.read();
                }
                return outputStream.toString();
            } catch (IOException e) {
                return "";
            }
        }
    }

    private void saveFlightsToLocalStorage(ArrayList<Flight> savedFlights) {
        Gson gson = new Gson();
        Type listType = new TypeToken<ArrayList<Flight>>() {
        }.getType();

        String ans= gson.toJson(savedFlights,listType);

        SharedPreferences sharedPref = MyApplication.getSharedPreferences();
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.FAVORITE_FLIGHTS), ans);
        editor.commit();
    }

    private ArrayList<Flight> loadFlightsFromLocalStorage() {
        Gson gson = new Gson();
        Type listType = new TypeToken<ArrayList<Flight>>() {
        }.getType();

        SharedPreferences sharedPref = MyApplication.getSharedPreferences();
        String str =sharedPref.getString(getString(R.string.FAVORITE_FLIGHTS),null);

        return gson.fromJson(str,listType);
    }

    private void updateNotifications() {
        for(final Flight flight : savedFlights) {

            String url = FLIGHT_STATUS_BASEURL + "&airline_id=" + flight.getAirlineID() + "&flight_number=" + flight.getNumber().substring(3) ;

            ApiConnection apiConnection = new ApiConnection(url) {
                @Override
                protected void onPostExecute(String result) {
                    if(result == null) {
                        return;
                    }
                    Gson gson = new Gson();
                        Type listType = new TypeToken<StatusResponse>() {
                    }.getType();

                    StatusResponse response= gson.fromJson(result,listType);

                    if(response.getStatus() != null) {

                        if(!flight.getStatus().equals(getFlightStatusString(response.getStatus().getStatus()))) {
                            flight.setStatus(getFlightStatusString(response.getStatus().getStatus()));
                            favAdapter.notifyDataSetChanged();

                            //pongo un toast
                            Toast.makeText( getApplicationContext(), getToastStatusString(flight.getNumber(), response.getStatus().getStatus()) ,Toast.LENGTH_SHORT  ).show();

                        }
                    }

                }
            };
            apiConnection.execute();
        }
    }

    private String getToastStatusString(String flight, String status) {
        String resp = getString(R.string.notificationToastStart);
        resp += " " +flight+" ";
        if(status.equals("S")) {
            resp += getString(R.string.notificationToastProgrammed);
        } else if(status.equals("A")) {
            resp += getString(R.string.notificationToastActive);
        } else if(status.equals("R")) {
            resp += getString(R.string.notificationToastDeviated);
        } else if(status.equals("L")) {
            resp += getString(R.string.notificationToastLanded);
        } else if(status.equals("C")) {
            resp += getString(R.string.notificationToastCancelled);
        }
        return resp;
    }

    private String getFlightStatusString(String status) {
        if(status.equals("S")) {
            return getString(R.string.flightStatusProgrammed);
        } else if(status.equals("A")) {
            return getString(R.string.flightStatusActive);
        } else if(status.equals("R")) {
            return getString(R.string.flightStatusDeviated);
        } else if(status.equals("L")) {
            return getString(R.string.flightStatusLanded);
        } else if(status.equals("C")) {
            return getString(R.string.flightStatusCancelled);
        }
        return "Not Found";
    }
}
