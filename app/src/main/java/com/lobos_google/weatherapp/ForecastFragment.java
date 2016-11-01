package com.lobos_google.weatherapp;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.HttpURLConnection;
import java.net.URL;
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
            weatherTask.execute();
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


        //---------------------------------
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(mForecastAdapter);

        return rootView;
    }// End OnCreateView


    public class FetchWeatherTask extends AsyncTask<Void, Void,Void>{

        private final String LOG_TAG= FetchWeatherTask.class.getSimpleName();


        @Override
        protected Void doInBackground(Void... params){

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String forecastJsonStr =null;

            try{
                String baseUrl="http://api.openweathermap.org/data/2.5/forecast/daily?q=10566&mode=json&units=metric&cnt=7";
                String apiKey= "&APPID="+ BuildConfig.MyOpenWeatherMapApiKey;
                URL url = new URL(baseUrl.concat(apiKey));


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
                Log.v(LOG_TAG,"Forecast JSON String: "+forecastJsonStr);

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
            }

        return null;
        }// end doInBackground

    }// End FetchWeatherTask


}// end ForecastFragment
