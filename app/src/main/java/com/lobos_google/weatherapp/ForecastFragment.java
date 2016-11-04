package com.lobos_google.weatherapp;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import java.util.Arrays;
import java.util.List;

/**
 * Created by gersonlobos on 12/10/16.
 */

public class ForecastFragment extends Fragment {

    ArrayAdapter<String> mForecastAdapter;

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
            FetchWeatherTask weatherTask=new FetchWeatherTask();
            weatherTask.execute("10566");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){

        String[] data = {
                "Mon 6/23 - Sunny - 31/17",
                "Tue 6/24 - Foggy - 21/8",
                "Wed 6/25 - Cloudy - 22/17",
                "Thurs 6/26 - Rainy - 18/11",
                "Fri 6/27 - Foggy - 21/10",
                "Sat 6/28 - TRAPPED IN WEATHERSTATION - 23/18",
                "Sun 6/29 - Sunny - 20/7"
        };
        List<String> weekForecast = new ArrayList<String>(Arrays.asList(data));
        mForecastAdapter =
                new ArrayAdapter<String>(
                        getActivity(), // The current context (this activity)
                        R.layout.list_item_forecast, // The name of the layout ID.
                        R.id.list_item_forecast_textview, // The ID of the textview to populate.
                        weekForecast);

        //---------------------------------

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(mForecastAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position, long l){
                String forecast = mForecastAdapter.getItem(position);
                //Toast.makeText(getActivity(),forecast,Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(getActivity(),DetailActivity.class)
                        .putExtra(Intent.EXTRA_TEXT,forecast);
                startActivity(intent);
            }
        });

        return rootView;
    }// End OnCreateView


    public class FetchWeatherTask extends AsyncTask<String, Void,String[]>{//<params, progress,result>

        private final String LOG_TAG= FetchWeatherTask.class.getSimpleName();


        private String getReadableDateString(long time){
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        private String formatHighLows(double high,double low){

            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }




        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            //these are the names of the Json object that need to be extracted
            final String OWN_LIST = "list";
            final String OWN_WEATHER = "weather";
            final String OWN_TEMPERATURE = "temp";
            final String OWN_MAX = "max";
            final String OWN_MIN = "min";
            final String OWN_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWN_LIST);

            //Log.v(LOG_TAG," weatherArray-->"+weatherArray.toString());
            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();

            String[] resultStrs = new String[numDays];

            for(int i=0;i<weatherArray.length(); i++){

                String day;
                String description;
                String highAndLow;

                //Ger the Json object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);
               // Log.v(LOG_TAG,"dayForecast: ("+1+") "+dayForecast.toString());

                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".

                long dateTime;
                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay+i);
                day = getReadableDateString(dateTime);


                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWN_WEATHER).getJSONObject(0);

              //  Log.v(LOG_TAG,"weatherObject OWN_WEATHER: "+weatherObject.toString());


                description = weatherObject.getString(OWN_DESCRIPTION);
                //description = weatherObject.opt(OWN_DESCRIPTION).toString();
              //  Log.v(LOG_TAG,"description: "+description);


                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.

                JSONObject temperatureObject = dayForecast.getJSONObject(OWN_TEMPERATURE);
                double high = temperatureObject.getDouble(OWN_MAX);
                double low  = temperatureObject.getDouble(OWN_MIN);

                highAndLow = formatHighLows(high,low);
                resultStrs[i]= day + " - " +description + " - " + highAndLow;
            }//end for

            for(String s: resultStrs){
                Log.v(LOG_TAG,"Forecast entry: " + s);
            }

          return resultStrs;
        }





        @Override
        protected String[] doInBackground(String... params){


            if(params.length == 0){
                return null;
            }

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String forecastJsonStr =null;
            String format = "json";
            String units = "metric";
            int numDays=7;

            try{
               // String baseUrl="http://api.openweathermap.org/data/2.5/forecast/daily?q=10566&mode=json&units=metric&cnt=7";
                //String apiKey= "&APPID="+ BuildConfig.MyOpenWeatherMapApiKey;

                final String FORECAST_BASE_URL="http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String QUERY_PARAM= "q";
                final String FORMAT_PARAM="mode";
                final String UNITS_PARAM="units";
                final String DAYS_PARAM="cnt";
                final String APPID_PARAM="APPID";

                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM,params[0])//string passed
                        .appendQueryParameter(FORMAT_PARAM,format)
                        .appendQueryParameter(UNITS_PARAM,units)
                        .appendQueryParameter(DAYS_PARAM,Integer.toString(numDays))
                        .appendQueryParameter(APPID_PARAM,BuildConfig.MyOpenWeatherMapApiKey)
                        .build();




                URL url = new URL(builtUri.toString());

             //   Log.v(LOG_TAG,"Built URI --->"+ builtUri.toString());


                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream= urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if(inputStream == null){
                    //nothing to do
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while((line = reader.readLine())!=null){//read until end of line

                        buffer.append(line +"\n");
                }// end while

                if (buffer.length() ==0){
                    return null;
                }

                forecastJsonStr = buffer.toString();
               // Log.v(LOG_TAG,"Forecast JSON String: "+forecastJsonStr);

                //-------------------------------
            }catch(IOException e){// IOException resolves all the errors asking for try and catch

                Log.e(LOG_TAG,"Error",e);
                return null;
            }finally {

                if (urlConnection!=null){
                    urlConnection.disconnect();
                }
                if(reader!=null){
                    try{
                       reader.close();
                    }catch(final IOException e){
                        Log.e(LOG_TAG,"Error closing stream",e);
                    }
                }//end if if(reader!=null)
            }//end finally


            try{//this is done due that the functions trows exception
                return getWeatherDataFromJson(forecastJsonStr,numDays);
            }catch(JSONException e){
                Log.e(LOG_TAG,e.getMessage(),e);
                e.printStackTrace();
            }

        // This will only happen if there was an error getting or parsing the forecast.
        return null;
        }// end doInBackground


        @Override
        protected void onPostExecute(String[] result) {//this gets triggered after do in background
            //super.onPostExecute(strings);
            if (result != null){

                mForecastAdapter.clear();
                for (String dayForecastStr : result){
                    mForecastAdapter.add(dayForecastStr);
                }
            }

        }

    }// End FetchWeatherTask


}// end ForecastFragment
