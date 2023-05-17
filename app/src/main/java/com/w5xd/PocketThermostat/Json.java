package com.w5xd.PocketThermostat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/*
 * Class to parse and create Json strings for Http GET and POST
 */
public class Json {
	
	public HashMap<String,String> m_map = new HashMap<String,String>();
	private StringBuffer sbTag=null;
	private StringBuffer sbValue=null;
	
	private final int START = 0;
	private final int BEGINTAG = 1;
	private final int ENDTAG = 2;
	private final int VALUE_SEPARATOR = 3;
	private final int VALUE_END = 4;
	private final int BRACE = 5;
	private final int BRACKET = 6;
	private int readState = START;
	
	
	private int m_depth = 0;
	public void nextChar(char c)
	{
		switch (readState)
		{
		case START:
			if (c == '{')
				readState += 1;
			break;
		case BEGINTAG:	
			if (c ==  '"')
			{
				readState += 1;
				sbTag =  new StringBuffer();
				sbValue = new StringBuffer();
			}
			break;
		case ENDTAG:
			if (c == '"')
				readState += 1;
			else
				sbTag.append(c);
			break;
		case VALUE_SEPARATOR:
			if (c == ':')
				readState += 1;
			break;
		case VALUE_END:
			if ((c == ',') || (c == '}'))
			{
				String key = sbTag.toString();
				String val = sbValue.toString();
				m_map.put(key, val);
				readState = BEGINTAG;
			}
			else
			{
				sbValue.append(c);
				if (c == '{')
				{
					readState = BRACE;
					m_depth++;
				}
				else if (c == '[')
				{
					m_depth++;
					readState = BRACKET;
				}
			}
			break;
		case BRACE:
			sbValue.append(c);
			if (c == '{')
				m_depth++;
			else if ((c == '}') && (--m_depth==0))
			{
				readState = BEGINTAG;
				m_map.put(sbTag.toString(), sbValue.toString());
			}
			break;
		case BRACKET:
			sbValue.append(c);
			if (c == '[')
				m_depth++;
			else if ((c == ']') && (--m_depth == 0))
			{
				readState = BEGINTAG;
				m_map.put(sbTag.toString(), sbValue.toString());
			}
			break;
		}
	}
	
	public String[] getArray(String key)
	{
		if (m_map.containsKey(key))
		{
			String s = m_map.get(key);
			if (s.startsWith("["))
			{
				Vector<String> v = new Vector<String>();
				StringBuffer sb = new StringBuffer();
				for (int i = 1; i < s.length(); i++)
				{
					char c = s.charAt(i);
					if ((c == ',') || (c == ']'))
					{
						v.add(sb.toString());
						sb = new StringBuffer();
					}
					else
						sb.append(c);
				}
				String[] ret = new String[v.size()];
				v.toArray(ret);
				return ret;
			}
		}
		return null;
	}
	
	public void addValue(String key, int[] arr)
	{
		StringBuffer sb = new StringBuffer();
		sb.append('[');
		for (int i = 0; i < arr.length; i++)
		{
			if (i != 0) sb.append(',');
			sb.append(Integer.toString(arr[i]));
		}
		sb.append(']');
		m_map.put(key,  sb.toString());
	}
	
	public void addValue(String key, int val)
	{
		m_map.put(key,  Integer.toString(val));
	}
	
	public void addValue(String key, String val)
	{
	    m_map.put(key,  "\"" + val + "\"");
	}
	
	@Override
	public String toString()
	{
		List<String> keyValues = new ArrayList<String>(m_map.keySet());
		Collections.sort(keyValues);
		StringBuffer sb = new StringBuffer();
		sb.append('{');
		Iterator<String> keys = keyValues.iterator();
		boolean isFirst = true;
		while (keys.hasNext())
		{
			String key = keys.next();
			if (!isFirst)
				sb.append(',');
			isFirst = false;
			sb.append('"');
			sb.append(key);
			sb.append('"');
			sb.append(':');
			sb.append(m_map.get(key));
		}
		sb.append('}');
		return sb.toString();
	}
	
	public void fromString(String s)
	{
		for (int i = 0; i < s.length(); i++)
			nextChar(s.charAt(i));
	}
}
