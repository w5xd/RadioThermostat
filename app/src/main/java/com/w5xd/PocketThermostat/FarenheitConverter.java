package com.w5xd.PocketThermostat;

public class FarenheitConverter implements TemperatureUnitConverter
{
    public float toDisplay(float internal) {return internal;}
    public int toInternal(float display) {return (int)(display + 0.5f);}
    public int displayUnit() {return 0;}
}
