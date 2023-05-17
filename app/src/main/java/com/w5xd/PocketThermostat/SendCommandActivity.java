package com.w5xd.PocketThermostat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.EditText;
import android.widget.TextView;
import android.content.DialogInterface;
import android.content.Intent;

import java.text.NumberFormat;

public class SendCommandActivity extends Activity implements OnTouchListener
{
	private String m_url;
	private Spinner m_setTempSpin;
	
	private static final int DLG_HVAC_MODE = R.id.spinnerTmode;
	private static final int DLG_FAN_MODE = R.id.spinnerFmode;
	private static final int DLG_HOLD_MODE = R.id.spinnerHmode;
	private static final int DLG_UPDATE_NAME = R.id.buttonThermometerName;
	
	private TemperatureUnitConverter m_converter = null;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sendcommand);

		Intent intent = getIntent();
		m_url = intent.getExtras().getString(ThermostatState.pref + "url");
        m_converter = MainActivity.converterFromIntent(intent);
        recoverThermoState(intent);
		        
		Spinner s = (Spinner) findViewById(R.id.spinnerTmode);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.thermostat_modes,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		s.setAdapter(adapter);
		if (m_thermostatState.m_hvacMode >= 0)
			s.setSelection(m_thermostatState.m_hvacMode);
		s.setOnTouchListener(this);

		s = (Spinner) findViewById(R.id.spinnerFmode);
		adapter = ArrayAdapter.createFromResource(this, R.array.fan_modes,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		s.setAdapter(adapter);
		if (m_thermostatState.m_fanMode >= 0)
			s.setSelection(m_thermostatState.m_fanMode);
		s.setOnTouchListener(this);

		s = (Spinner) findViewById(R.id.spinnerHmode);
		adapter = ArrayAdapter.createFromResource(this, R.array.hold_modes,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		s.setAdapter(adapter);
		if (m_thermostatState.m_hold >= 0)
			s.setSelection(m_thermostatState.m_hold);
		s.setOnTouchListener(this);
		m_setTempSpin = s;
		
		findViewById(R.id.buttonThermometerName).setOnTouchListener(this);

		EditText tv = (EditText) findViewById(R.id.editTextTargetTemperature);
		tv.setText(NumberFormat.getInstance().format(m_converter.toDisplay((int)m_thermostatState.m_temperature)));

		m_setTempSpin.setEnabled(m_thermostatState.m_hvacMode > 0);

		TextView txv = (TextView) findViewById(R.id.textViewSendCommandMsg);
		txv.setText("");
		
		EditText et = (EditText) findViewById(R.id.editTextThermometerName);
		et.setText(intent.getExtras().getString(ThermostatState.pref + "name"));
	}

	public boolean onTouch(View v, MotionEvent e)
	{

		int id = v.getId();
		switch (id)
		{
		case R.id.spinnerFmode:
			showDialog(DLG_FAN_MODE);
			return true;
		case R.id.spinnerHmode:
			if (getTarget() >= 0)
				showDialog(DLG_HOLD_MODE);
			return true;
		case R.id.spinnerTmode:
			showDialog(DLG_HVAC_MODE);
			return true;
		case R.id.buttonThermometerName:
		    showDialog(DLG_UPDATE_NAME);
		    return true;
		}
		return false;
	}
	
	private void updateThermometerName()
	{
	    setSendCmdMsg();
	    String v;
	    EditText et = (EditText) findViewById(R.id.editTextThermometerName);
	    v = et.getText().toString();
	    new DelayUpdateName().execute(v);
	}

	private ThermostatState m_thermostatState;

	private void recoverThermoState(Intent intent)
	{
		m_thermostatState = new ThermostatState();
		m_thermostatState.fromBundle(intent.getExtras());
	}
	
	protected Dialog onCreateDialog(int id) {
	    Spinner s;
	   	    switch(id) {
	    case DLG_HVAC_MODE:
	    	s = (Spinner)findViewById(R.id.spinnerTmode);
	    	return spinnerToDialog(s, m_thermostatState.m_hvacMode);

	    case DLG_FAN_MODE:
	    	s = (Spinner)findViewById(R.id.spinnerFmode);
	    	return spinnerToDialog(s, m_thermostatState.m_fanMode);

	    case DLG_HOLD_MODE:
	    	s = (Spinner)findViewById(R.id.spinnerHmode);
	    	return spinnerToDialog(s, m_thermostatState.m_hold);
	    	
	    case DLG_UPDATE_NAME:
	        { 
	            AlertDialog.Builder builder = new AlertDialog.Builder(this);
	            builder.setMessage(R.string.msgUpdateName);
	            builder.setPositiveButton(R.string.msgOK, new DialogInterface.OnClickListener(){
	                public void onClick(DialogInterface dialog, int id){updateThermometerName();}
	            });
	            builder.setNegativeButton(R.string.msgCancel, new DialogInterface.OnClickListener(){
	                public void onClick(DialogInterface dialog, int id){dialog.cancel();}
	            });
	            return builder.create();
	        
	        }

	    }
	    return null;
	}	
	
	private void spinnerCommand(int whichSpinner, int pos)
	{
		boolean success = false;
		switch (whichSpinner)
		{
		case R.id.spinnerHmode:
			success = setHoldMode(pos);
			break;
		case R.id.spinnerFmode:
			success = setFanMode(pos);
			break;
		case R.id.spinnerTmode:
			success = setHvacMode(pos);
			break;
		}
		
		if (success) 
			((Spinner)findViewById(whichSpinner)).setSelection(pos);
	}
	
	private class SpinnerDlgListener implements DialogInterface.OnClickListener
	{
		private int m_whichSpinner;
		SpinnerDlgListener(int whichSpinner) { m_whichSpinner = whichSpinner;}
		public void onClick(DialogInterface dialog, int item) 
		{
			dialog.cancel();
			SendCommandActivity.this.removeDialog(m_whichSpinner);
			SendCommandActivity.this.spinnerCommand(m_whichSpinner, item);
		}
	}
	
	private Dialog spinnerToDialog(Spinner s, int who)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(s.getPrompt())
	       .setCancelable(true);
		Adapter a = s.getAdapter();
		CharSequence [] items = new CharSequence[a.getCount()];
    	for (int i = 0; i < a.getCount(); i++)
     		items[i] = a.getItem(i).toString();
    	builder.setSingleChoiceItems(items, who, new SpinnerDlgListener(s.getId()));
    	return builder.create();
	}

	private class Delay extends android.os.AsyncTask<String, Void, Void>
	{
		protected Void doInBackground(String... s)
		{
		    doDelayedRun(s);
		    return null;
		}
		protected void onPostExecute(Void r)
        {   PostDelayedRun();        }
	}
    private void doDelayedRun(String[] keys)
    {
        m_thermostatState.WriteToURL(m_url, getResources(), keys);
    }

    private void PostDelayedRun()
    {
        TextView tv = (TextView) findViewById(R.id.textViewSendCommandMsg);
        tv.setText(m_thermostatState.getError());
    }

	private class DelayUpdateName extends android.os.AsyncTask<String, Void, Integer>
	{
        protected Integer doInBackground(String... s)
        {
            doDelayedSetName(s[0]);
            return 0;
        }
        protected void onPostExecute(Integer r)
        {   PostDelayedRun();
        }
		}

	private void doDelayedSetName(String name)
	{
	    m_thermostatState.WriteNameToURL(m_url, getResources(), name);
	}

	private void setSendCmdMsg()
	{
		TextView tv = (TextView) findViewById(R.id.textViewSendCommandMsg);
		tv.setText(getResources().getString(R.string.msgReadingThermostat));
	}
	
	private boolean setHvacMode(int pos)
	{
		if ((pos > 0) && (m_thermostatState.m_hvacMode != 0))
		{
			TextView tv = (TextView) findViewById(R.id.textViewSendCommandMsg);
			tv.setText(getResources().getString(R.string.msgMustGoThruOff));
			return false;
		}
		m_thermostatState.m_hvacMode = pos;
		m_setTempSpin.setEnabled(m_thermostatState.m_hvacMode != 0);
		String[] keys = new String[1];
		keys[0] = "tmode";
		setSendCmdMsg();
		new Delay().execute(keys);
		return true;
	}
	
	private boolean setFanMode(int pos)
	{
		m_thermostatState.m_fanMode = pos;
		String[] keys = new String[1];
		keys[0] = "fmode";
		setSendCmdMsg();
        new Delay().execute(keys);
		return true;
	}
	
	private boolean setHoldMode(int pos)
	{
		int tgt = getTarget();
		if (tgt < 0)
		{
			return false;
		}
		if ((pos == 1) && (tgt == 0))
		{
			TextView msg = (TextView) findViewById(R.id.textViewSendCommandMsg);
			msg.setText(getResources().getString(R.string.msgInvalidNoBlankWithHold));			
			return false;
		}
		m_thermostatState.m_hold = pos;
		commandTempAndHmode();
		return true;
	}

	private int getTarget()
	{
		EditText et = (EditText) findViewById(R.id.editTextTargetTemperature);
		String val = et.getText().toString();
		if ((val == null) || (val.length() == 0)) 
			return 0;
		
		int target = m_converter.toInternal(Float.parseFloat(val));

		if ((target < ThermostatState.MIN_TARGET_TEMPERATURE)
				|| (target > ThermostatState.MAX_TARGET_TEMPERATURE))
		{
			TextView msg = (TextView) findViewById(R.id.textViewSendCommandMsg);
			msg.setText(getResources().getString(R.string.msgInvalidEntry));
			et.requestFocus();
			return -1;
		}
		return target;
	}

	private boolean commandTempAndHmode()
	{
		int target = getTarget();
		if (target < 0)
			return false;
		String[] keys = null;
		if (target == 0)
		{
			// turn off hold mode and nothing else
			keys = new String[1];
			keys[0] = "hold";
			
		}
		else switch (m_thermostatState.m_hvacMode)
		{
		case 1: // heat
			m_thermostatState.m_heatTarget = target;
			keys = new String[2];
			keys[0] = "t_heat";
			keys[1] = "hold";
			break;
		case 2: // cool
			m_thermostatState.m_coolTarget = target;
			keys = new String[2];
			keys[0] = "t_cool";
			keys[1] = "hold";
			break;
		case 3: // auto
			// FIXME don't know what to do
			break;
		default:
			break;
		}
		if (keys != null)
		{
			setSendCmdMsg();
            new Delay().execute(keys);
            return true;
		}
		return false;
	}
}
