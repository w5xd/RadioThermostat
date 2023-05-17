package com.w5xd.PocketThermostat;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.params.HttpParams;
import org.apache.http.HttpEntity;
import org.apache.http.protocol.HTTP;

import android.content.Intent;
import android.content.res.Resources;
import android.net.http.AndroidHttpClient;
import android.os.Bundle;

import java.io.InputStreamReader;
import java.io.BufferedReader;

/*
 * Class to represent operating state of the thermostat
 */
public class ThermostatState
{

    public static final int MIN_TARGET_TEMPERATURE = 40;
    public static final int MAX_TARGET_TEMPERATURE = 90;

    public float m_temperature = -1;
    public int m_hvacMode = -1;
    public int m_fanMode = -1;
    public int m_override = -1;
    public int m_hold = -1;
    public float m_heatTarget = -1;
    public float m_coolTarget = -1;
    public int m_currentState = -1;

    private final int SOCKET_CONNECT_TIMOUT_MSEC = 4000;
    private final int HTTP_POST_TIMEOUT_MSEC = 15000;
    private final int HTTP_GET_TIMEOUT_MSEC = 15000;

    private void initState()
    {
        m_temperature = -1;
        m_hvacMode = -1;
        m_fanMode = -1;
        m_override = -1;
        m_hold = -1;
        m_currentState = -1;
        m_heatTarget = -1;
        m_coolTarget = -1;
    }

    public static final String pref = "com.w5xd.PocketThermostat.";

    public String getError()
    {
        return m_error;
    }

    private String m_error = "";

    public void toIntent(Intent intent)
    {
        Bundle b = new Bundle();
        toBundle(b);
        intent.putExtras(b);
    }

    public void toBundle(Bundle bundle)
    {
        bundle.putFloat(pref + "temperature", m_temperature);
        bundle.putInt(pref + "hvacMode", m_hvacMode);
        bundle.putInt(pref + "hold", m_hold);
        bundle.putFloat(pref + "coolTarget", m_coolTarget);
        bundle.putFloat(pref + "heatTarget", m_heatTarget);
        bundle.putInt(pref + "fanMode", m_fanMode);
        bundle.putInt(pref + "override", m_override);
        bundle.putInt(pref + "currentState", m_currentState);
    }

    public void fromBundle(Bundle bundle)
    {
        m_temperature = bundle.getFloat(pref + "temperature");
        m_coolTarget = bundle.getFloat(pref + "coolTarget");
        m_heatTarget = bundle.getFloat(pref + "heatTarget");
        m_currentState = bundle.getInt(pref + "currentState");
        m_fanMode = bundle.getInt(pref + "fanMode");
        m_override = bundle.getInt(pref + "override");
        m_hold = bundle.getInt(pref + "hold");
        m_hvacMode = bundle.getInt(pref + "hvacMode");
    }

    public Json doGet(String uri, Resources res)
    {
        m_error = "";
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpParams params = httpclient.getParams();
        HttpConnectionParams.setConnectionTimeout(params,
                SOCKET_CONNECT_TIMOUT_MSEC);
        HttpConnectionParams.setSoTimeout(params, HTTP_GET_TIMEOUT_MSEC);

        HttpGet httpGet = new HttpGet(uri);
        Json json = new Json();

        try
        {
            HttpResponse httpResponse = httpclient.execute(httpGet);
            HttpEntity entity = httpResponse.getEntity();
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    entity.getContent()));

            int nextChar;
            while ((nextChar = in.read()) >= 0)
                json.nextChar((char) nextChar);
            in.close();

        } catch (Exception e)
        {
            String sdbg = e.getMessage();
            m_error = sdbg;
            if ((m_error == null) || (m_error == ""))
                m_error = e.toString();
            if ((m_error == null) || (m_error.length() == 0))
                m_error = res.getString(R.string.msgHttpError);
        }
        return json;
    }

    private class PostBackground implements Runnable
    {
    	private String m_uri;    	private String m_obj;    	private Resources m_res;
    	PostBackground(String uri, String obj, Resources res)
    	{
    		m_uri = uri;    		m_obj = obj;    		m_res = res;
    	}
    	public void run()
    	{    		doPostT(m_uri, m_obj, m_res);    	}
    }
    public void doPost(String uri, Object obj, Resources res)
    {
     	Thread t = new Thread(new PostBackground(uri, obj.toString(), res));
    	t.start();
    	try {
    		t.join();
    	}
    	catch (Exception e)
    	{
    		m_error = e.getMessage();
    	}
    }
    public void doPostT(String uri, String obj, Resources res)
    {
        m_error = "";
        HttpClient  httpclient = AndroidHttpClient.newInstance("Pocket Thermostat"); //new DefaultHttpClient(); 
        
        HttpParams params = httpclient.getParams();
        HttpConnectionParams.setConnectionTimeout(params,
                SOCKET_CONNECT_TIMOUT_MSEC);
        HttpConnectionParams.setSoTimeout(params, HTTP_POST_TIMEOUT_MSEC);
        
        HttpPost httpPost = new HttpPost(uri);
        // Headers from curl -trace-ascii -d 'yada yada yada'
        //httpPost.addHeader("User-Agent", "Pocket Thermostat");
        httpPost.addHeader("Accept", "*/*");
        httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");

        Json ret = new Json();

        try
        {
            StringEntity eentity = new StringEntity(obj, HTTP.UTF_8);
            eentity.setContentType("application/x-www-form-urlencoded");
            httpPost.setEntity(eentity);
            HttpResponse rresponse = httpclient.execute(httpPost);
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    rresponse.getEntity().getContent()));

            int nextChar;
            while ((nextChar = in.read()) >= 0)
                ret.nextChar((char) nextChar);
            in.close();

        } catch (Exception e)
        {
            m_error = e.getMessage();
            if ((m_error == null) || (m_error.length() == 0))
                m_error = e.toString();
            if ((m_error == null) || (m_error.length() == 0))
                m_error = res.getString(R.string.msgHttpError);
        }
    }

    public void WriteToURL(String url, Resources res, String[] keys)
    {
        Json json = new Json();
        for (int j = 0; j < keys.length; j++)
        {
            if (keys[j].equals("tmode"))
                json.addValue("tmode", m_hvacMode);
            else if (keys[j].equals("fmode"))
                json.addValue("fmode", m_fanMode);
            else if (keys[j].equals("hold"))
                json.addValue("hold", m_hold);
            else if (keys[j].equals("t_heat"))
                json.addValue("t_heat", (int) m_heatTarget);
            else if (keys[j].equals("t_cool"))
                json.addValue("t_cool", (int) m_coolTarget);
        }
        String uri = url + "/tstat";
        doPost(uri, json, res);
    }

    public void ReadFromURL(String url, Resources res)
    {
        initState();
        Json json = doGet(url + "/tstat", res);
        fromJson(json);
    }

    public void WriteNameToURL(String url, Resources res, String name)
    {
        String uri = url + "/sys/name";
        Json json = new Json();
        json.addValue("name", name);
        doPost(uri, json, res);
    }
 
    public String ReadNameFromURL(String url, Resources res)
    {
        String ret = "";
        Json json = doGet(url + "/sys/name", res);
        if (json.m_map.containsKey("name"))
        {
            ret = json.m_map.get("name");
            if (ret.length() >= 2)
                ret = ret.substring(1, ret.length() - 1);
        }
        return ret;
    }

    private void fromJson(Json json)
    {
        if (json.m_map.containsKey("temp"))
            m_temperature = Float.parseFloat(json.m_map.get("temp"));
        if (json.m_map.containsKey("tmode"))
            m_hvacMode = Integer.parseInt(json.m_map.get("tmode"));
        if (json.m_map.containsKey("fmode"))
            m_fanMode = Integer.parseInt(json.m_map.get("fmode"));
        if (json.m_map.containsKey("override"))
            m_override = Integer.parseInt(json.m_map.get("override"));
        if (json.m_map.containsKey("hold"))
            m_hold = Integer.parseInt(json.m_map.get("hold"));
        if (json.m_map.containsKey("tstate"))
            m_currentState = Integer.parseInt(json.m_map.get("tstate"));
        if (json.m_map.containsKey("t_heat"))
            m_heatTarget = Float.parseFloat(json.m_map.get("t_heat"));
        if (json.m_map.containsKey("t_cool"))
            m_coolTarget = Float.parseFloat(json.m_map.get("t_cool"));
    }

}
