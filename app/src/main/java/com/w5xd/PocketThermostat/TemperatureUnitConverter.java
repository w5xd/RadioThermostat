package com.w5xd.PocketThermostat;

public interface TemperatureUnitConverter
{
    float toDisplay(float internal);
    int toInternal(float display);
    int displayUnit();
}
