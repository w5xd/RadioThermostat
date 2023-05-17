package com.w5xd.PocketThermostat;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener
{
	private ThermostatState m_thermostatState = new ThermostatState();
    private String m_name = "";
    private String[] m_prevUrls;
    private String m_url;
    private boolean m_urlDialogCreated = false;
    private static final int MAX_PREV_URLS = 4;
    public enum TemperatureUnits {UNITS_CELSIUS, UNITS_FAHRENHEIT};
    private TemperatureUnits m_units;
    private TemperatureUnitConverter m_converter = null;
    private HashMap<String,String> m_names = new HashMap<String,String>();
		
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
         // listeners
        Button btn = (Button) findViewById(R.id.buttonSchedule);
        btn.setOnClickListener(this);
        btn = (Button) findViewById(R.id.buttonChange);
        btn.setOnClickListener(this);
        btn = (Button) findViewById(R.id.buttonRefresh);
        btn.setOnClickListener(this);
        
        // recover saved unit preference
        SharedPreferences sp = getPreferences(Context.MODE_PRIVATE);
        switch (sp.getInt("units", 0))
        {
            case 0: setUnits(TemperatureUnits.UNITS_FAHRENHEIT); break;
            case 1: setUnits(TemperatureUnits.UNITS_CELSIUS); break;
        }
        
        // recover saved m_names
        String names = sp.getString("names", "");
        if ((names != null) && (names.length() > 0))
        {
            try
            {
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(Base64.decode(names, Base64.DEFAULT));
            java.io.ObjectInputStream ois = new java.io.ObjectInputStream(bais);
            HashMap<String,String> m2 = (HashMap<String,String>)ois.readObject();
            if (m2 != null)
                m_names = m2;
            } catch (Exception e) {}
        }        
        
        // set URL from preferences
        EditText et = (EditText)findViewById(R.id.editTextUrl);
        String prevUrl = sp.getString("url", "");
        et.setText(prevUrl);
       
        // recover saved state, e.g. landscape/portrait transition
        if (savedInstanceState != null)
        {
        	try
        	{
        	m_thermostatState.fromBundle(savedInstanceState);
        	m_name = savedInstanceState.getString("thermostatName");
        	}
        	catch (Exception e) {}
        }
                
        setDisplayFromState();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        findViewById(R.id.buttonPrevUrl).setOnClickListener(this);
        TextView tv = (TextView)findViewById(R.id.textThermostatName);
        tv.setText(m_name);
        setupPrevUrls();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_options, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menuOptionsClearConfirm:
                clearPrevUrls();
                return true;
            case R.id.menuOptionsC:
                changeUnits(TemperatureUnits.UNITS_CELSIUS);
                 return true;
            case R.id.menuOptionsF:
                changeUnits(TemperatureUnits.UNITS_FAHRENHEIT);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    private void changeUnits(TemperatureUnits u)
    {
        if (m_units != u)
        {
            setUnits(u);
            setDisplayFromState();
            SharedPreferences.Editor ed = getPreferences(Context.MODE_PRIVATE).edit();
            ed.putInt("units", m_converter.displayUnit());
            ed.commit();
        }
    }
    
    private void setUnits(TemperatureUnits u)
    {
        m_units = u;
        switch (u)
        {
        case UNITS_CELSIUS:            m_converter = new CelsiusConverter();            break;
        case UNITS_FAHRENHEIT:            m_converter = new FarenheitConverter();            break;
        }        
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        MenuItem miC = menu.findItem(R.id.menuOptionsC);
        MenuItem miF = menu.findItem(R.id.menuOptionsF);
        miC.setChecked(m_units == TemperatureUnits.UNITS_CELSIUS);
        miF.setChecked(m_units == TemperatureUnits.UNITS_FAHRENHEIT);
        return super.onPrepareOptionsMenu(menu);
    }
    
    private void setupPrevUrls()
    {
        SharedPreferences sp = getPreferences(Context.MODE_PRIVATE);
        Vector<String> prev = new Vector<String>();
        String prevUrl = sp.getString("url", "");
        if (prevUrl.length() > 0) 
            prev.add(prevUrl);
        for (int i = 0; i < MAX_PREV_URLS; i++)
        {
            String key = "url" + Integer.toString(i+1);
            prevUrl = sp.getString(key, "");
            if (prevUrl.length() > 0) prev.add(prevUrl);
        }        
        m_prevUrls= new String[prev.size()];
        prev.toArray(m_prevUrls);
        Button btn = (Button) findViewById(R.id.buttonPrevUrl);
        btn.setEnabled(m_prevUrls == null ? false : (m_prevUrls.length > 1));  
        if (m_urlDialogCreated)
            removeDialog(R.id.buttonPrevUrl);
        m_urlDialogCreated = false;
   }
    
    private void clearPrevUrls()
    {
        SharedPreferences.Editor ed = getPreferences(Context.MODE_PRIVATE).edit();
        for (int i = 0; i < MAX_PREV_URLS; i++)
        {
            String key = "url" + Integer.toString(i+1);
            ed.remove(key);
        }
        ed.remove("names");
        ed.commit();
        if (m_urlDialogCreated)
            removeDialog(R.id.buttonPrevUrl);
        m_urlDialogCreated = false;
    }
          
    private void setDisplayFromState()
    {
        TextView tv;
        TextView tv1;
        NumberFormat nf = NumberFormat.getInstance();
        Resources res = getResources();

        tv = (TextView) findViewById(R.id.textViewCurrentTemp);
        tv1 = (TextView) findViewById(R.id.textViewCurrentTempTenths);
        float current = m_thermostatState.m_temperature;
        if (current > 0)
        {
            current = m_converter.toDisplay(current);
            tv.setText(nf.format((int) current));
            int tenths = (int) ((current - (int) current) * 10.0f);
            if (tenths == 0)
                tv1.setText("");
            else
                tv1.setText(nf.format(tenths));
        } else
        {
            tv.setText("  --  ");
            tv1.setText("");
        }

        String[] vals = res.getStringArray(R.array.fan_modes);
        tv = (TextView) findViewById(R.id.textViewFanMode);
        if ((m_thermostatState.m_fanMode >= 0)
                && (m_thermostatState.m_fanMode < vals.length))
            tv.setText(vals[m_thermostatState.m_fanMode]);
        else
            tv.setText("-");

        vals = res.getStringArray(R.array.thermostat_states);
        tv = (TextView) findViewById(R.id.textViewHvacState);
        if ((m_thermostatState.m_currentState >= 0)
                && (m_thermostatState.m_currentState < vals.length))
            tv.setText(vals[m_thermostatState.m_currentState]);
        else
            tv.setText("-");

        tv = (TextView) findViewById(R.id.textViewHvacMode);
        vals = res.getStringArray(R.array.thermostat_modes);
        if ((m_thermostatState.m_hvacMode >= 0)
                && (m_thermostatState.m_hvacMode < vals.length))
            tv.setText(vals[m_thermostatState.m_hvacMode]);
        else
            tv.setText("-");

        tv = (TextView) findViewById(R.id.textViewTarget);
        tv1 = (TextView) findViewById(R.id.textViewTargetTempTenths);
        float target = m_thermostatState.m_heatTarget;
        if (target < 0)
            target = m_thermostatState.m_coolTarget;
        if (target > 0)
        {
            target = m_converter.toDisplay(target);
            tv.setText(nf.format((int) target));
            int tenths = (int) ((target - (int) target) * 10.0f);
            if (tenths == 0)
                tv1.setText("");
            else
                tv1.setText(nf.format(tenths));
        }
        else
        {
            tv.setText("  --  ");
            tv1.setText("");
        }

        tv = (TextView) findViewById(R.id.textViewScheduleState);
        vals = res.getStringArray(R.array.hold_states);
        if ((m_thermostatState.m_hold >= 0)
                && (m_thermostatState.m_hold < vals.length))
            tv.setText(vals[m_thermostatState.m_hold]);
        else
            tv.setText("-");

        tv = (TextView) findViewById(R.id.textViewMainError);
        tv.setText(m_thermostatState.getError());
    }
      
	// get the Url from EditText and save preference, too
	private String url()
	{
		EditText et = (EditText)findViewById(R.id.editTextUrl);
		String s = et.getText().toString();
		m_url = s;
		int i;
		for (i = 0; i < m_prevUrls.length; i++)
		{
			if (s.compareToIgnoreCase(m_prevUrls[i]) == 0)
				break;
		}
		
		SharedPreferences.Editor ed = getPreferences(Context.MODE_PRIVATE).edit();
		ed.putString("url", s);
		
		// save most recent MAX_PREV_URLS
		int k = 1;
		for (int j = 0; j < m_prevUrls.length; j++)
		{
			if (j == i)
				continue;
			if (j >= MAX_PREV_URLS)
			    break;
			String key = "url" + Integer.toString(k++);
			ed.putString(key,  m_prevUrls[j]);
		}
		ed.commit();
        setupPrevUrls();
		return s;
	}
	
	private class Delay extends android.os.AsyncTask<Void,Void,ThermostatState> {
        protected ThermostatState doInBackground(Void... x)	{	return readState();	}
        protected void onPostExecute(ThermostatState nextState)
        {            PostReadState(nextState);        }
    }
	
	private class DelayGetName extends android.os.AsyncTask<Void,Void,String> {
        protected String doInBackground(Void... x)	{	return readName();	}
        protected void onPostExecute(String name)
        {            PostReadName(name);        }
    }
	
	private ThermostatState readState() {
        ThermostatState newState = new ThermostatState();
        String url = this.url();
        newState.ReadFromURL(url, getResources());
        return newState;
    }
    private void PostReadState(ThermostatState newState){
		m_thermostatState = newState;
		setDisplayFromState();
		new DelayGetName().execute();
	}
	
    private String readName() {
       return m_thermostatState.ReadNameFromURL(m_url, getResources());
    }
    private void PostReadName(String name) {
        m_name = name;
        TextView tv = (TextView) findViewById(R.id.textThermostatName);
        tv.setText(m_name);
        if (m_name != null && m_name.length() > 0)
        {
            m_names.put(m_url, m_name);
            try
            {
                // persist mapping from URL to user-chosen name
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(baos);
                oos.writeObject(m_names);
                String s = Base64.encodeToString(baos.toByteArray(),  Base64.DEFAULT);
                SharedPreferences.Editor ed = getPreferences(Context.MODE_PRIVATE).edit();
                ed.putString("names", s);
                ed.commit();
            } catch (Exception e)
            {
            }
        }
    }
	
	public static TemperatureUnitConverter converterFromIntent(Intent intent)
	{
	    switch (intent.getExtras().getInt(ThermostatState.pref + "units", 0))
        {
            case 0:
                return new FarenheitConverter();
            default:
               return new CelsiusConverter();
        }
	}
	
    public void onClick(View v)
    {
    	int id = v.getId();
    	if (id == R.id.buttonSchedule)
    	{
	    	Intent intent = new Intent(this, com.w5xd.PocketThermostat.EditScheduleActivity.class);
	    	intent.putExtra(ThermostatState.pref + "url", url());
	    	intent.putExtra(ThermostatState.pref + "units", m_converter.displayUnit());
	    	startActivity(intent);
    	}
    	else if (id == R.id.buttonChange)
    	{
	    	Intent intent = new Intent(this, com.w5xd.PocketThermostat.SendCommandActivity.class);
	    	m_thermostatState.toIntent(intent);
	    	intent.putExtra(ThermostatState.pref + "url", url());
            intent.putExtra(ThermostatState.pref + "units", m_converter.displayUnit());
            intent.putExtra(ThermostatState.pref + "name", m_name);
	    	startActivity(intent);
    	}
    	else if (id == R.id.buttonRefresh)
    	{	// UI won't update until we return, so update text, return, and do socket later...
    		TextView tv = (TextView)findViewById(R.id.textViewMainError);
    		tv.setText(getResources().getString(R.string.msgReadingThermostat));
    		new Delay().execute();
    	}
    	else if (id == R.id.buttonPrevUrl)
    	{
    		showDialog(id);
    	}
    }
           
	public void selectPreviousUrl(int pos) 
	{
		if ((pos >= 0) && (pos < m_prevUrls.length)) 
		{
			EditText et = (EditText) findViewById(R.id.editTextUrl);
			et.setText(m_prevUrls[pos]);
		}
	}
    
	private class DlgListener implements DialogInterface.OnClickListener
	{
	    public void onClick(DialogInterface dialog, int item)
		{
			dialog.cancel();		
			MainActivity.this.selectPreviousUrl(item);
		}
	}	
	
	protected Dialog onCreateDialog(int id) 
	{
		if (id == R.id.buttonPrevUrl)
			return prevUrlDialog(id);
		return null;
	}
	
	private Dialog prevUrlDialog(int who)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getResources().getString(R.string.urlListLabel))
	       .setCancelable(true);
		CharSequence[] items = new CharSequence[m_prevUrls.length];
		for (int i = 0; i < m_prevUrls.length; i++) 
		{
		    String pres = m_prevUrls[i];
		    if (m_names.containsKey(pres))
		          pres = m_names.get(pres) + " (" + pres + ")";
		     items[i] = pres;
		}
    	builder.setSingleChoiceItems(items, -1, new DlgListener());
    	m_urlDialogCreated = true;
    	return builder.create();
	}  
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		m_thermostatState.toBundle(outState);
		outState.putString("thermostatName", m_name);
	}
 }