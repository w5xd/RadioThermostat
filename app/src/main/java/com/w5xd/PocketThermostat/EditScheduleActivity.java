package com.w5xd.PocketThermostat;

import java.text.NumberFormat;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.ArrayAdapter;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class EditScheduleActivity extends Activity implements OnClickListener,
		OnItemSelectedListener, OnTouchListener {

	private ThermostatSchedule.schedule_t m_whichSked = ThermostatSchedule.schedule_t.HEAT_SCHEDULE;
	private ThermostatSchedule m_heatSched = new ThermostatSchedule();
	private ThermostatSchedule m_coolSched = new ThermostatSchedule();
	private TemperatureUnitConverter m_converter = null;
	private int[][] m_timeEditIds = new int[ThermostatSchedule.NUM_DAYS_PER_WEEK][];
	private int[][] m_tempEditIds = new int[ThermostatSchedule.NUM_DAYS_PER_WEEK][];

	public EditScheduleActivity() {
		for (int i = 0; i < ThermostatSchedule.NUM_DAYS_PER_WEEK; i++)
		{
			m_timeEditIds[i] = new int [ThermostatSchedule.NUM_PERIODS];
			m_tempEditIds[i] = new int [ThermostatSchedule.NUM_PERIODS];
		}
		m_timeEditIds[0][0] = R.id.editTextTime0_0;
		m_timeEditIds[0][1] = R.id.editTextTime0_1;
		m_timeEditIds[0][2] = R.id.editTextTime0_2;
		m_timeEditIds[0][3] = R.id.editTextTime0_3;
		m_tempEditIds[0][0] = R.id.editTextTemperature0_0;
		m_tempEditIds[0][1] = R.id.editTextTemperature0_1;
		m_tempEditIds[0][2] = R.id.editTextTemperature0_2;
		m_tempEditIds[0][3] = R.id.editTextTemperature0_3;

		m_timeEditIds[1][0] = R.id.editTextTime1_0;
		m_timeEditIds[1][1] = R.id.editTextTime1_1;
		m_timeEditIds[1][2] = R.id.editTextTime1_2;
		m_timeEditIds[1][3] = R.id.editTextTime1_3;
		m_tempEditIds[1][0] = R.id.editTextTemperature1_0;
		m_tempEditIds[1][1] = R.id.editTextTemperature1_1;
		m_tempEditIds[1][2] = R.id.editTextTemperature1_2;
		m_tempEditIds[1][3] = R.id.editTextTemperature1_3;

		m_timeEditIds[2][0] = R.id.editTextTime2_0;
		m_timeEditIds[2][1] = R.id.editTextTime2_1;
		m_timeEditIds[2][2] = R.id.editTextTime2_2;
		m_timeEditIds[2][3] = R.id.editTextTime2_3;
		m_tempEditIds[2][0] = R.id.editTextTemperature2_0;
		m_tempEditIds[2][1] = R.id.editTextTemperature2_1;
		m_tempEditIds[2][2] = R.id.editTextTemperature2_2;
		m_tempEditIds[2][3] = R.id.editTextTemperature2_3;

		m_timeEditIds[3][0] = R.id.editTextTime3_0;
		m_timeEditIds[3][1] = R.id.editTextTime3_1;
		m_timeEditIds[3][2] = R.id.editTextTime3_2;
		m_timeEditIds[3][3] = R.id.editTextTime3_3;
		m_tempEditIds[3][0] = R.id.editTextTemperature3_0;
		m_tempEditIds[3][1] = R.id.editTextTemperature3_1;
		m_tempEditIds[3][2] = R.id.editTextTemperature3_2;
		m_tempEditIds[3][3] = R.id.editTextTemperature3_3;

		m_timeEditIds[4][0] = R.id.editTextTime4_0;
		m_timeEditIds[4][1] = R.id.editTextTime4_1;
		m_timeEditIds[4][2] = R.id.editTextTime4_2;
		m_timeEditIds[4][3] = R.id.editTextTime4_3;
		m_tempEditIds[4][0] = R.id.editTextTemperature4_0;
		m_tempEditIds[4][1] = R.id.editTextTemperature4_1;
		m_tempEditIds[4][2] = R.id.editTextTemperature4_2;
		m_tempEditIds[4][3] = R.id.editTextTemperature4_3;

		m_timeEditIds[5][0] = R.id.editTextTime5_0;
		m_timeEditIds[5][1] = R.id.editTextTime5_1;
		m_timeEditIds[5][2] = R.id.editTextTime5_2;
		m_timeEditIds[5][3] = R.id.editTextTime5_3;
		m_tempEditIds[5][0] = R.id.editTextTemperature5_0;
		m_tempEditIds[5][1] = R.id.editTextTemperature5_1;
		m_tempEditIds[5][2] = R.id.editTextTemperature5_2;
		m_tempEditIds[5][3] = R.id.editTextTemperature5_3;

		m_timeEditIds[6][0] = R.id.editTextTime6_0;
		m_timeEditIds[6][1] = R.id.editTextTime6_1;
		m_timeEditIds[6][2] = R.id.editTextTime6_2;
		m_timeEditIds[6][3] = R.id.editTextTime6_3;
		m_tempEditIds[6][0] = R.id.editTextTemperature6_0;
		m_tempEditIds[6][1] = R.id.editTextTemperature6_1;
		m_tempEditIds[6][2] = R.id.editTextTemperature6_2;
		m_tempEditIds[6][3] = R.id.editTextTemperature6_3;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.schedule);
		
		m_converter = MainActivity.converterFromIntent(getIntent());
		
		if (savedInstanceState != null) 
		{
			m_whichSked = savedInstanceState.getBoolean("m_whichSked") ? ThermostatSchedule.schedule_t.COOL_SCHEDULE
					: ThermostatSchedule.schedule_t.HEAT_SCHEDULE;
			
			String ss = savedInstanceState.getString("m_heatSched");
			if (ss != null) {
				Json js = new Json();
				js.fromString(ss);
				m_heatSched.fromJson(js, getResources());
			}
			ss = savedInstanceState.getString("m_coolSched");
			if (ss != null) {
				Json js = new Json();
				js.fromString(ss);
				m_coolSched.fromJson(js, getResources());
			}
		}

		Spinner s = (Spinner) findViewById(R.id.spinnerSchedule);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.schedules, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		s.setAdapter(adapter);
		int p = m_whichSked == ThermostatSchedule.schedule_t.COOL_SCHEDULE ? 1
				: 0;
		s.setSelection(p);
		s.setOnItemSelectedListener(this);
		s.setOnTouchListener(this);

		Button btn = (Button) findViewById(R.id.buttonReadSchedule);
		btn.setOnClickListener(this);
		
		// updateDisplayFromCurrentSchedule(); happens once at beginning anyway cuz of Spinner
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.schedule_options, menu);
		return true;
	}

    private class Delay extends android.os.AsyncTask<Integer, Void, ThermostatSchedule>
    {
        protected ThermostatSchedule doInBackground(Integer... x)
        {            return readSchedule();        }

        protected void onPostExecute(ThermostatSchedule ts)
        {        PostReadSchedule(ts);       }
    }

	public void onClick(View v) {
		int id = v.getId();
		if (id == R.id.buttonReadSchedule) {
			TextView tv = (TextView) findViewById(R.id.textViewSchedStatus);
			tv.setText(getResources().getString(R.string.msgReadingThermostat));
			new Delay().execute(0);
		}
	}

	private class Delay2 extends android.os.AsyncTask<Integer,Void,ThermostatSchedule>
	{
	    protected ThermostatSchedule doInBackground(Integer... x)
        {	return		copyToThermostat();		}
	    protected void onPostExecute(ThermostatSchedule ts)
        {         PostCopyToThermostat(ts);       }
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.menuSchedOptionsCopyToThermo:
            {
                if (this.validateEntries())
                {
                    updateScheduleFromCurrentDisplay();
                    ((TextView) (findViewById(R.id.textViewSchedStatus)))
                            .setText(getResources().getString(
                                    R.string.msgReadingThermostat));
                   new Delay2().execute(0);
                }
            }
            return true;

            case R.id.menuSchedOptionsMakeLocalCopy:
            {
                if (this.validateEntries())
                {
                    updateScheduleFromCurrentDisplay();
                    SharedPreferences.Editor ed = getPreferences(
                            Context.MODE_PRIVATE).edit();
                    Json json = new Json();
                    if (m_whichSked == ThermostatSchedule.schedule_t.HEAT_SCHEDULE)
                    {
                        m_heatSched.toJson(json);
                        ed.putString("heatingSchedule", json.toString());
                    } else if (m_whichSked == ThermostatSchedule.schedule_t.COOL_SCHEDULE)
                    {
                        m_coolSched.toJson(json);
                        ed.putString("coolingSchedule", json.toString());
                    }
                    ed.commit();
                }
            }
            return true;
            case R.id.menuSchedOptionsUseLocalCopy:
            {
                SharedPreferences sp = getPreferences(Context.MODE_PRIVATE);
                Json json = new Json();
                if (m_whichSked == ThermostatSchedule.schedule_t.HEAT_SCHEDULE)
                {
                    json.fromString(sp.getString("heatingSchedule", ""));
                    m_heatSched.fromJson(json, getResources());
                } else if (m_whichSked == ThermostatSchedule.schedule_t.COOL_SCHEDULE)
                {
                    json.fromString(sp.getString("coolingSchedule", ""));
                    m_coolSched.fromJson(json, getResources());
                }
                updateDisplayFromCurrentSchedule();
            }
            return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

	private ThermostatSchedule copyToThermostat()
	{
		ThermostatSchedule ts = (m_whichSked == ThermostatSchedule.schedule_t.COOL_SCHEDULE) ? 
		        m_coolSched	: m_heatSched;
		ts.writeToUrl(
				getIntent().getExtras().getString(ThermostatState.pref + "url"),
				m_whichSked, getResources());
		return ts;
	}

	private void PostCopyToThermostat(ThermostatSchedule ts)
    {
        ((TextView) findViewById(R.id.textViewSchedStatus)).setText(ts
                .getError());
    }

	private ThermostatSchedule readSchedule()
	{
		ThermostatSchedule ts = new ThermostatSchedule();
		ts.readFromUrl(
				getIntent().getExtras().getString(ThermostatState.pref + "url"),
				m_whichSked, getResources());
		return ts;
	}

	private void PostReadSchedule(ThermostatSchedule ts)
    {
        String err = ts.getError();
        TextView tv = (TextView) findViewById(R.id.textViewSchedStatus);
        tv.setText(err);
        if (err.length() == 0) {
            if (m_whichSked == ThermostatSchedule.schedule_t.COOL_SCHEDULE)
                m_coolSched = ts;
            else
                m_heatSched = ts;
            updateDisplayFromCurrentSchedule();
        }
    }

	// mode spinner handling....
	public void onItemSelected(AdapterView<?> parent, View view, int pos,
			long id) {
		int vid = parent.getId();
		if (vid == R.id.spinnerSchedule) {
			m_whichSked = (pos == 0) ? ThermostatSchedule.schedule_t.HEAT_SCHEDULE
					: ThermostatSchedule.schedule_t.COOL_SCHEDULE;
			updateDisplayFromCurrentSchedule();
		}
	}

	public void onNothingSelected(AdapterView<?> parent) {	}

	public boolean onTouch(View v, MotionEvent e) {
		int id = v.getId();
		if (id == R.id.spinnerSchedule) { // don't bring up heat/cool change
											// selector if data on screen is bad
			if (!validateEntries())
				return true;
			else
				updateScheduleFromCurrentDisplay();
		}
		return false;
	}

	private void updateDisplayFromCurrentSchedule() {
		ThermostatSchedule ts = (m_whichSked == ThermostatSchedule.schedule_t.HEAT_SCHEDULE) ? m_heatSched
				: m_coolSched;
		
		for (int dow = 0; dow < m_tempEditIds.length; dow++)
		{
			for (int i = 0; i < m_tempEditIds[dow].length; i++) {
				int t = ts.getProgramTemp(i, dow);
				float val = m_converter.toDisplay(t);
				String s = NumberFormat.getInstance().format(val);
				TextView tv;
				tv = (TextView) findViewById(m_tempEditIds[dow][i]);
				tv.setText(s);
				tv = (TextView) findViewById(m_timeEditIds[dow][i]);
				tv.setText(ts.getProgramTime(i, dow));
			}
		}
	}

    private void updateScheduleFromCurrentDisplay()
    {
        ThermostatSchedule ts = (m_whichSked == ThermostatSchedule.schedule_t.HEAT_SCHEDULE) ? 
                m_heatSched : m_coolSched;
        TextView tv;
        for (int dow = 0; dow < m_tempEditIds.length; dow++)
        {
            for (int i = 0; i < m_tempEditIds[dow].length; i++)
            {
                tv = (TextView) findViewById(m_tempEditIds[dow][i]);
                ts.setProgramTemp(i, dow, m_converter.toInternal(Float
                        .parseFloat(tv.getText().toString())));

                tv = (TextView) findViewById(m_timeEditIds[dow][i]);
                ts.setProgramTime(i, dow, ThermostatSchedule
                        .convertTimeString(tv.getText().toString()));
            }
        }
    }

	private boolean validateEntries() {
		int vb = validateTimeBoxes();
		TextView tv = (TextView) (findViewById(R.id.textViewSchedStatus));
		EditText et;
		if (vb < 0) {
			vb = validateTempBoxes();
			if (vb < 0) {
				tv.setText("");
				return true;
			} else {
				et = (EditText) findViewById(vb);
				et.requestFocus();
			}
		} else {
			et = (EditText) findViewById(vb);
			et.requestFocus();
		}
		tv.setText(getResources().getString(R.string.msgInvalidScheduleEntry));
		return false;
	}

	private int validateTimeBoxes() {
		for (int dow=0; dow < m_timeEditIds.length; dow++) 
		{
		int lastVal = -1; // must sort in increasing order of time.
		for (int i = 0; i < m_timeEditIds[dow].length; i++) {
			TextView tv = (TextView) findViewById(m_timeEditIds[dow][i]);
			int thisVal = ThermostatSchedule.convertTimeString(tv.getText()
					.toString());
			if (thisVal < 0)
				return m_timeEditIds[dow][i];
			if (thisVal < lastVal)
				return m_timeEditIds[dow][i];;
			lastVal = thisVal;
		}
		}
		return -1;
	}

	private int validateTempBoxes() {
		for (int dow = 0; dow < m_tempEditIds.length; dow++)
		{
		for (int i = 0; i < m_tempEditIds[dow].length; i++) {
			TextView tv = (TextView) findViewById(m_tempEditIds[dow][i]);
			int val = m_converter.toInternal(Float.parseFloat(tv.getText().toString()));
			if ((val < ThermostatState.MIN_TARGET_TEMPERATURE)
					|| (val > ThermostatState.MAX_TARGET_TEMPERATURE))
				return m_tempEditIds[dow][i];
		}
		}
		return -1;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		Json js = new Json();
		m_coolSched.toJson(js);
		outState.putString("m_coolSched", js.toString());
		js = new Json();
		m_heatSched.toJson(js);
		outState.putString("m_heatSched", js.toString());
		outState.putBoolean("m_whichSked",
				m_whichSked == ThermostatSchedule.schedule_t.COOL_SCHEDULE);
	}
}