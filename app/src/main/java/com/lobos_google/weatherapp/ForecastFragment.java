package com.lobos_google.weatherapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import com.lobos_google.weatherapp.data.WeatherContract;
import java.util.Arrays;
import java.util.List;

/**
 * Created by gersonlobos on 12/10/16.
 */

public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>  {

    private static final int FORECAST_LOADER = 0;
    // For the forecast view we're showing only a small subset of the stored data.
    // Specify the columns we need.
    private static final String[] FORECAST_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_COORD_LAT,
            WeatherContract.LocationEntry.COLUMN_COORD_LONG
    };

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_LOCATION_SETTING = 5;
    static final int COL_WEATHER_CONDITION_ID = 6;
    static final int COL_COORD_LAT = 7;
    static final int COL_COORD_LONG = 8;




    private ForecastAdapter mForecastAdapter;

    public ForecastFragment(){
    }

    @Override
    public void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        inflater.inflate(R.menu.forecastfragment,menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id= item.getItemId();

        if (id==R.id.action_refresh){
            updateWeather();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


//90001

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){

        // The CursorAdapter will take data from our cursor and populate the ListView.
        mForecastAdapter = new ForecastAdapter(getActivity(), null, 0);


        //---------------------------------

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(mForecastAdapter);


        // We'll call our MainActivity
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // CursorAdapter returns a cursor at the correct position for getItem(), or null
                // if it cannot seek to that position.
                Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
                if (cursor != null) {
                    String locationSetting = Utility.getPreferredLocation(getActivity());
                    Intent intent = new Intent(getActivity(), DetailActivity.class)
                            .setData(WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                                    locationSetting, cursor.getLong(COL_WEATHER_DATE)
                            ));
                    startActivity(intent);
                }
            }
        });



        return rootView;
    }// End OnCreateView


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(FORECAST_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    // since we read the location when we create the loader, all we need to do is restart things
    void onLocationChanged( ) {
        updateWeather();
        getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
    }

    private void updateWeather(){
        FetchWeatherTask weatherTask = new FetchWeatherTask(getActivity());
        String location = Utility.getPreferredLocation(getActivity());
        weatherTask.execute(location);
    }


    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String locationSetting = Utility.getPreferredLocation(getActivity());

        // Sort order:  Ascending, by date.
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                locationSetting, System.currentTimeMillis());

        return new CursorLoader(getActivity(),
                weatherForLocationUri,
                FORECAST_COLUMNS,
                null,
                null,
                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mForecastAdapter.swapCursor(cursor);
            }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mForecastAdapter.swapCursor(null);
    }

//    //===================================================
//
//    public class FetchWeatherTask extends AsyncTask<String, Void,String[]>{//<params, progress,result>
//
//        private final String LOG_TAG= FetchWeatherTask.class.getSimpleName();
//
//
//        private String getReadableDateString(long time){
//            // Because the API returns a unix timestamp (measured in seconds),
//            // it must be converted to milliseconds in order to be converted to valid date.
//            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
//            return shortenedDateFormat.format(time);
//        }
//
//        private String formatHighLows(double high,double low,String unitType){
//
//            if (unitType.equals(getString(R.string.pref_units_imperial))){
//                high = (high * 1.8) + 32;
//                low = (low * 1.8) + 32;
//            }else if(!unitType.equals(getString(R.string.pref_units_imperial))){
//                Log.d(LOG_TAG,"Unit type not fount "+unitType);
//            }
//
//            long roundedHigh = Math.round(high);
//            long roundedLow = Math.round(low);
//
//            String highLowStr = roundedHigh + "/" + roundedLow;
//            return highLowStr;
//        }
//
//
//
//
//        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
//                throws JSONException {
//
//            //these are the names of the Json object that need to be extracted
//            final String OWN_LIST = "list";
//            final String OWN_WEATHER = "weather";
//            final String OWN_TEMPERATURE = "temp";
//            final String OWN_MAX = "max";
//            final String OWN_MIN = "min";
//            final String OWN_DESCRIPTION = "main";
//
//            JSONObject forecastJson = new JSONObject(forecastJsonStr);
//            JSONArray weatherArray = forecastJson.getJSONArray(OWN_LIST);
//
//            //Log.v(LOG_TAG," weatherArray-->"+weatherArray.toString());
//            Time dayTime = new Time();
//            dayTime.setToNow();
//
//            // we start at the day returned by local time. Otherwise this is a mess.
//            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);
//
//            // now we work exclusively in UTC
//            dayTime = new Time();
//
//            String[] resultStrs = new String[numDays];
//            // Data is fetched in Celsius by default.
//            // If user prefers to see in Fahrenheit, convert the values here.
//            // We do this rather than fetching in Fahrenheit so that the user can
//            // change this option without us having to re-fetch the data once
//            // we start storing the values in a database.
//
//            SharedPreferences sharedPrefs =
//                    PreferenceManager.getDefaultSharedPreferences(getActivity());
//
//            String unitType = sharedPrefs.getString(
//                    getString(R.string.pref_units_key),
//                    getString(R.string.pref_units_metric)
//            );
//
//            for(int i=0;i<weatherArray.length(); i++){
//
//                String day;
//                String description;
//                String highAndLow;
//
//                //Ger the Json object representing the day
//                JSONObject dayForecast = weatherArray.getJSONObject(i);
//               // Log.v(LOG_TAG,"dayForecast: ("+1+") "+dayForecast.toString());
//
//                // The date/time is returned as a long.  We need to convert that
//                // into something human-readable, since most people won't read "1400356800" as
//                // "this saturday".
//
//                long dateTime;
//                // Cheating to convert this to UTC time, which is what we want anyhow
//                dateTime = dayTime.setJulianDay(julianStartDay+i);
//                day = getReadableDateString(dateTime);
//
//
//                // description is in a child array called "weather", which is 1 element long.
//                JSONObject weatherObject = dayForecast.getJSONArray(OWN_WEATHER).getJSONObject(0);
//
//              //  Log.v(LOG_TAG,"weatherObject OWN_WEATHER: "+weatherObject.toString());
//
//
//                description = weatherObject.getString(OWN_DESCRIPTION);
//                //description = weatherObject.opt(OWN_DESCRIPTION).toString();
//              //  Log.v(LOG_TAG,"description: "+description);
//
//
//                // Temperatures are in a child object called "temp".  Try not to name variables
//                // "temp" when working with temperature.  It confuses everybody.
//
//                JSONObject temperatureObject = dayForecast.getJSONObject(OWN_TEMPERATURE);
//                double high = temperatureObject.getDouble(OWN_MAX);
//                double low  = temperatureObject.getDouble(OWN_MIN);
//
//                highAndLow = formatHighLows(high,low,unitType);
//                resultStrs[i]= day + " - " +description + " - " + highAndLow;
//            }//end for
//
//            for(String s: resultStrs){
//                Log.v(LOG_TAG,"Forecast entry: " + s);
//            }
//
//          return resultStrs;
//        }
//
//
//
//
//
//        @Override
//        protected String[] doInBackground(String... params){
//
//
//            if(params.length == 0){
//                return null;
//            }
//
//            HttpURLConnection urlConnection = null;
//            BufferedReader reader = null;
//
//            String forecastJsonStr =null;
//            String format = "json";
//            String units = "metric";
//            int numDays=7;
//
//            try{
//               // String baseUrl="http://api.openweathermap.org/data/2.5/forecast/daily?q=10566&mode=json&units=metric&cnt=7";
//                //String apiKey= "&APPID="+ BuildConfig.MyOpenWeatherMapApiKey;
//
//                final String FORECAST_BASE_URL="http://api.openweathermap.org/data/2.5/forecast/daily?";
//                final String QUERY_PARAM= "q";
//                final String FORMAT_PARAM="mode";
//                final String UNITS_PARAM="units";
//                final String DAYS_PARAM="cnt";
//                final String APPID_PARAM="APPID";
//
//                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
//                        .appendQueryParameter(QUERY_PARAM,params[0])//string passed
//                        .appendQueryParameter(FORMAT_PARAM,format)
//                        .appendQueryParameter(UNITS_PARAM,units)
//                        .appendQueryParameter(DAYS_PARAM,Integer.toString(numDays))
//                        .appendQueryParameter(APPID_PARAM,BuildConfig.MyOpenWeatherMapApiKey)
//                        .build();
//
//
//
//
//                URL url = new URL(builtUri.toString());
//
//             //   Log.v(LOG_TAG,"Built URI --->"+ builtUri.toString());
//
//
//                urlConnection = (HttpURLConnection) url.openConnection();
//                urlConnection.setRequestMethod("GET");
//                urlConnection.connect();
//
//                InputStream inputStream= urlConnection.getInputStream();
//                StringBuffer buffer = new StringBuffer();
//                if(inputStream == null){
//                    //nothing to do
//                    return null;
//                }
//                reader = new BufferedReader(new InputStreamReader(inputStream));
//
//                String line;
//                while((line = reader.readLine())!=null){//read until end of line
//
//                        buffer.append(line +"\n");
//                }// end while
//
//                if (buffer.length() ==0){
//                    return null;
//                }
//
//                forecastJsonStr = buffer.toString();
//               // Log.v(LOG_TAG,"Forecast JSON String: "+forecastJsonStr);
//
//                //-------------------------------
//            }catch(IOException e){// IOException resolves all the errors asking for try and catch
//
//                Log.e(LOG_TAG,"Error",e);
//                return null;
//            }finally {
//
//                if (urlConnection!=null){
//                    urlConnection.disconnect();
//                }
//                if(reader!=null){
//                    try{
//                       reader.close();
//                    }catch(final IOException e){
//                        Log.e(LOG_TAG,"Error closing stream",e);
//                    }
//                }//end if if(reader!=null)
//            }//end finally
//
//
//            try{//this is done due that the functions trows exception
//                return getWeatherDataFromJson(forecastJsonStr,numDays);
//            }catch(JSONException e){
//                Log.e(LOG_TAG,e.getMessage(),e);
//                e.printStackTrace();
//            }
//
//        // This will only happen if there was an error getting or parsing the forecast.
//        return null;
//        }// end doInBackground
//
//
//        @Override
//        protected void onPostExecute(String[] result) {//this gets triggered after do in background
//            //super.onPostExecute(strings);
//            if (result != null){
//
//                mForecastAdapter.clear();
//                for (String dayForecastStr : result){
//                    mForecastAdapter.add(dayForecastStr);
//                }
//            }
//
//        }
//
//    }// End FetchWeatherTask


}// end ForecastFragment
