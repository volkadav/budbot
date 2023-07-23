package org.perilouscodpiece.budbot.actions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WeatherTests {
    @Test
    void WMOcodeSimpleTest() {
        Weather.WMOcode code = Weather.WMOcode.valueOfCode(3);
        assertEquals(code, Weather.WMOcode.OVERCAST);
    }

    @Test
    void OpenMeteoResponseDeserializationTest() {
        String sampleResponse = """
    {"latitude":55.875,
     "longitude":-4.3125,
     "generationtime_ms":1.271963119506836,
     "utc_offset_seconds":3600,
     "timezone":"Europe/London",
     "timezone_abbreviation":"BST",
     "elevation":6.0,
     "current_weather":{
         "temperature":12.4,
         "windspeed":3.4,
         "winddirection":72.0,
         "weathercode":2,
         "is_day":0,
         "time":"2023-07-23T23:00"
         }
      }""";
        try {
            Weather.OpenMeteoResponse response = Weather.om.readValue(sampleResponse, Weather.OpenMeteoResponse.class);
            assertEquals(55.875, response.getLatitude());
            assertEquals(-4.3125, response.getLongitude());
            assertEquals("Europe/London", response.getTimezone());

            Weather.CurrentWeather weather = response.getCurrentWeather();
            assertEquals(12.4, weather.getTemperature());
            assertEquals(3.4, weather.getWindspeed());
            assertEquals(72, weather.getWinddirection());
            assertFalse(weather.isDay());
            assertEquals("2023-07-23T23:00", weather.getTime());
            assertEquals(Weather.WMOcode.valueOfCode(2), weather.getWeathercode());
        } catch (Exception e) {
            fail(e.getClass().getName() + ": " + e.getMessage());
        }
    }
}
