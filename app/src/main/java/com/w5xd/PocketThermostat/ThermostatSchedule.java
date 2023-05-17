package com.w5xd.PocketThermostat;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;

import android.content.res.Resources;
import android.net.http.AndroidHttpClient;

/*
 * Class to represent a week of the thermostat's schedule
 */
public class ThermostatSchedule {
	
	private class Setting
	{
		public int m_time;
		public int m_temperature;
	}
	
	public ThermostatSchedule()
	{	// init to midnight at 70 degrees
		m_settings = new Setting[NUM_PERIODS][];
		for (int i = 0; i < NUM_PERIODS; i++)
		{
			m_settings[i] = new Setting[NUM_DAYS_PER_WEEK];
			for (int j = 0; j < NUM_DAYS_PER_WEEK; j++)
				{
					m_settings[i][j] = new Setting();
					m_settings[i][j].m_time = 0;
					m_settings[i][j].m_temperature = 70;
				}
		}
	}
	
	public String getError() { return m_error;}
	private String m_error = "";
	
	private Setting[][] m_settings;
	
	public static final int MORNING = 0;
	public static final int DAYTIME = 1;
	public static final int EVENING = 2;
	public static final int NIGHTTIME = 3;
	public static final int NUM_PERIODS = 4;
	public static final int NUM_DAYS_PER_WEEK = 7;
	
	private static final int HTTP_POST_TIMEOUT_MSEC = 20 * 1000;
	private static final int HTTP_GET_TIMEOUT_MSEC = 20 * 1000;
	private static final int SOCKET_CONNECT_TIMOUT_MSEC = 5 * 1000;
	
	public enum  schedule_t {HEAT_SCHEDULE, COOL_SCHEDULE};
	
	public void readFromUrl(String url, schedule_t which, Resources res)
	{
		m_error = "";
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpParams params = httpclient.getParams();
		HttpConnectionParams.setConnectionTimeout(params, SOCKET_CONNECT_TIMOUT_MSEC);
		HttpConnectionParams.setSoTimeout(params, HTTP_GET_TIMEOUT_MSEC);
		String uri = url + "/tstat/program/";
		if (which == schedule_t.HEAT_SCHEDULE) uri += "heat";
		else uri += "cool";
			
		HttpGet httpGet = new HttpGet(uri);
		Json json = new Json();

		try {
			HttpResponse httpResponse = httpclient.execute(httpGet);
			HttpEntity entity = httpResponse.getEntity();
			BufferedReader in = new BufferedReader(new InputStreamReader(
					entity.getContent()));

			int nextChar;
			while ((nextChar = in.read()) >= 0) {
				json.nextChar((char) nextChar);
			}
			in.close();

		} catch (Exception e) {
			m_error = e.getMessage();
			if (m_error == null)
				m_error = e.toString();
			if ((m_error == null) || (m_error.length() == 0))
				m_error = res.getString(R.string.msgHttpError);
			return;
		}
		
		fromJson(json, res);
	}
	
	public void fromJson(Json json, Resources res)
	{
		for (int i = 0; i < NUM_DAYS_PER_WEEK; i++)
		{
			String [] daysSchedule = json.getArray(Integer.toString(i));
			if (daysSchedule == null)
			{
				m_error = res.getString(R.string.msgNullSchedule);
				return;
			}
			if (daysSchedule.length != (NUM_PERIODS * 2)) 
			{
				m_error = res.getString(R.string.msgIncompleteSchedule);
				return;
			}
			int j = 0;
			int period = 0;
			
			while (period <= NIGHTTIME)
			{
				m_settings[period][i].m_time = Integer.parseInt(daysSchedule[j++]);
				m_settings[period++][i].m_temperature = Integer.parseInt(daysSchedule[j++]);
			}
		}
	}
	
	public void toJson(Json json)
	{
		for (int j = 0; j < NUM_DAYS_PER_WEEK; j++)
		{
			int[] arr = new int [2 * NUM_PERIODS];
			int k = 0;
			for (int i = 0; i < NUM_PERIODS; i++)
			{
				arr[k++] = m_settings[i][j].m_time;
				arr[k++] = m_settings[i][j].m_temperature;
			}
			json.addValue(Integer.toString(j), arr);
		}		
	}
	
	private class WriteUrlBackground implements Runnable
	{
		private String m_url;		private schedule_t m_which;		private Resources m_res;
		WriteUrlBackground(String url, schedule_t which, Resources res)
		{
			m_url = url;			m_which = which;			m_res = res;
		}
		public void run()
		{			writeToUrlT(m_url, m_which, m_res);		}
	}
	
	public void writeToUrl(String url, schedule_t which, Resources res)
	{
		Thread r = new Thread(new WriteUrlBackground(url, which, res));
		r.start();
		try
		{
			r.join();
		}
		catch (Exception e)
		{
			m_error = e.getMessage();
		}
	}
	
	public void writeToUrlT(String url, schedule_t which, Resources res)
	{
		m_error = "";
		HttpClient httpclient = AndroidHttpClient.newInstance("Pocket Thermostat");
		HttpParams params = httpclient.getParams();
		
		HttpConnectionParams.setConnectionTimeout(params, SOCKET_CONNECT_TIMOUT_MSEC);
		HttpConnectionParams.setSoTimeout(params, HTTP_POST_TIMEOUT_MSEC);
		String uri = url + "/tstat/program/";
		if (which == schedule_t.HEAT_SCHEDULE) uri += "heat";
		else uri += "cool";
		
		HttpPost httpPost = new HttpPost(uri);
		// Headers chosen because thermostat docs say use curl:
		//      curl -trace-ascii -d 'yada yada yada'
		
	    httpPost.addHeader("Accept", "*/*");
	    httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
	    
		Json json = new Json();
		
		toJson(json);
		
		Json ret = new Json();
	    
	    try
	    {
		    StringEntity eentity = new StringEntity(json.toString(),  HTTP.UTF_8);
		    eentity.setContentType("application/x-www-form-urlencoded");
		    httpPost.setEntity(eentity);		  
		    HttpResponse rresponse = httpclient.execute(httpPost);
			BufferedReader in = new BufferedReader(new InputStreamReader(
					rresponse.getEntity().getContent()));

			int nextChar;
			while ((nextChar = in.read()) >= 0) {
				ret.nextChar((char) nextChar);
			}
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
	
	String getProgramTime(int period, int dayOfWeek)
	{
		int val = m_settings[period][dayOfWeek].m_time;
		int hrs = val / 60;
		int min = val % 60;
		boolean pm = hrs > 12;
		if (pm) hrs -= 12;
		else if (hrs == 0) hrs = 12;
		StringBuffer sb = new StringBuffer();
		sb.append(Integer.toString(hrs));
		sb.append(':');
		if (min < 10) sb.append('0');
		sb.append(Integer.toString(min));
		sb.append(pm ? "PM" : "AM");
		return sb.toString();
	}
	
	public int getProgramTemp(int period, int dayOfWeek)
	{
		return m_settings[period][dayOfWeek].m_temperature;
	}
	
	public void setProgramTemp(int period, int dayOfWeek, int val)
	{
		m_settings[period][dayOfWeek].m_temperature = val;
	}
	
	public void setProgramTime(int period, int dayOfWeek, int time)
	{
		m_settings[period][dayOfWeek].m_time = time;
	}
	
	public static int convertTimeString(String s)
	{
		int hrs = 0;
		int hrsDigits = 0;
		int minDigits = 0;
		int minutes = 0;
		int state = 0;
		int AMPM = 0;
		for (int i = 0; i < s.length(); i++)
		{
			char c = s.charAt(i);
			switch (state)
			{
				case 0:
					if ((c >= '0') && (c <= '9'))
					{
						hrsDigits += 1;
						hrs *= 10;
						hrs += c - '0';
					}
					else if (c == ':')
						state += 1;
					else
						return -1;
					break;
				case 1:
					if ((c >= '0') && (c <= '9'))
					{
						minDigits += 1;
						minutes *= 10;
						minutes += c - '0';
					}
					else 
					{
						state += 1;
						if ((c == 'A') || (c == 'a'))
							AMPM = 1;
						else if ((c=='P') || (c == 'p'))
							AMPM = 2;
					}
					break;
				case 2:
					if ((c == 'M') || (c == 'm'))
						state += 1;
					else
						return -1;
					break;
				default:
					return -1;
			}
		}
		
		if ((hrsDigits == 0) || (hrsDigits > 2))
			return -1;
		if (minDigits != 2) 
			return -1;
		if (AMPM == 0)
			return -1;
		if (hrs > 12)
			return -1;
		if (minutes > 59)
			return -1;
		
		if (AMPM == 1)
		{
			hrs %= 12;
			return hrs * 60 + minutes;
		}
		else if (AMPM == 2)
		{
			hrs += 12;
			return hrs * 60 + minutes;
		}
		return -1;
	}
}
