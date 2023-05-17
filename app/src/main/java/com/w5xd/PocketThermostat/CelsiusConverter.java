package com.w5xd.PocketThermostat;

public class CelsiusConverter implements TemperatureUnitConverter
{
    public float toDisplay(float internal) 
    {
        float val = (internal - 32.0f) * 5.0f / 9.0f;
        // round to nearest tenth
        val += 0.05f;   // round up to nearest 10th degree F
        float f = val - (float)(int)val; // fractional part
        f *= 10;
        f = 0.1f * (float)(int)f;
        return (int)val + f;
       }
    public int toInternal(float display)
    {
        float val = display * 9.0f / 5.0f + 32.0f;
        return (int)(val + 0.5f);
    }
    public int displayUnit() {return 1;}
}
