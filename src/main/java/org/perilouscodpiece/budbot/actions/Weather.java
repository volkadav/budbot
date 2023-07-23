package org.perilouscodpiece.budbot.actions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class Weather {
    @VisibleForTesting
    protected static final ObjectMapper om = new ObjectMapper()
            .setPropertyNamingStrategy(new PropertyNamingStrategies.SnakeCaseStrategy())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Escaper urlEscaper = UrlEscapers.urlPathSegmentEscaper();

    @Data
    @AllArgsConstructor
    public static class CityData {
        private Double latitude, longitude;
        private String timezone; // for the open-meteo api, would need this for forecast info
    }
    public static Map<String, CityData> shortcutCities = Map.of(
            "seattle", new CityData(47.6062, -122.3321, "America/Los_Angeles"), // lat, lon
            "austin", new CityData(30.2672, -97.7431, "America/Chicago"),
            "glasgow", new CityData(55.8617, -4.295905, "Europe/London"),
            "edmonton", new CityData(53.631611, -113.323975, "America/Edmonton"),
            "london", new CityData(51.5072,-0.1276, "Europe/London"),
            "nyc", new CityData(40.7128, -74.0060, "America/New_York"),
            "sf", new CityData(37.7749, -122.4194, "America/Los_Angeles"),
            "chicago", new CityData(41.8781, -87.6298, "America/Chicago"),
            "boston", new CityData(42.3601, -71.0589, "America/New_York")
    );

    public enum WMOcode {
        // values taken from https://open-meteo.com/en/docs (scroll down)
        UNKNOWN(-1, "unknown code"),
        CLEAR_SKY(0, "clear sky"),
        MAINLY_CLEAR(1, "mainly clear"),
        PARTLY_CLOUD(2, "partly cloudy"),
        OVERCAST(3, "overcast"),
        FOG(45, "fog"),
        DEPOSITING_RIME_FOG(48, "depositing rime fog"),
        LIGHT_DRIZZLE(51, "light drizzle"),
        MODERATE_DRIZZLE(52, "moderate drizzle"),
        DENSE_DRIZZLE(53, "dense drizzle"),
        LIGHT_FREEZING_DRIZZLE(56, "light freezing drizzle"),
        DENSE_FREEZING_DRIZZLE(7, "dense freezing drizzle"),
        LIGHT_RAIN(61, "slight rain"),
        MODERATE_RAIN(63, "moderate rain"),
        HEAVY_RAIN(65, "heavy rain"),
        LIGHT_FREEZING_RAIN(66, "light freezing rain"),
        HEAVY_FREEZING_RAIN(67, "heavy freezing rain"),
        SLIGHT_SNOW(71, "slight snow"),
        MODERATE_SNOW(73, "moderate snow"),
        HEAVY_SNOW(75, "heavy snow"),
        SNOW_GRAINS(77, "snow grains"),
        SLIGHT_RAIN_SHOWERS(80, "slight rain showers"),
        MODERATE_RAIN_SHOWERS(81, "moderate rain showers"),
        VIOLENT_RAIN_SHOWERS(82, "violent rain showers"),
        SLIGHT_SNOW_SHOWERS(85, "slight snow showers"),
        HEAVY_SNOW_SHOWERS(86, "heavy snow showers"),
        THUNDERSTORM(95, "thunderstorm"),
        // apparently these next two are only used in central europe?
        THUNDERSTORM_SLIGHT_HAIL(96, "thunderstorm with slight hail"),
        THUNDERSTORM_HEAVY_HAIL(99, "thunderstorm with heavy hail");

        private static final Map<Integer, WMOcode> BY_CODE = new HashMap<>();

        static {
            for (WMOcode code : values()) {
                BY_CODE.put(code.wmoCode, code);
            }
        }

        final int wmoCode;
        final String wmoDesc;

        private WMOcode(int code, String desc) {
            this.wmoCode = code;
            this.wmoDesc = desc;
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static WMOcode valueOfCode(@JsonProperty("weathercode") Integer wmoCode) {
            return BY_CODE.getOrDefault(wmoCode, UNKNOWN);
        }
    }

    // used to deserialize the open-meteo json
    @Data
    public static class CurrentWeather {
        private double temperature;
        private double windspeed;
        private int winddirection; // degrees, North = 0
        private WMOcode weathercode;
        private boolean isDay;
        private String time; // YYYY-MM-DDTHH:MMM format, fwiw

        public static String degreeDirectionToCardinal(int windDirection) {
            if (windDirection < 0 || windDirection > 360) {
                return "bad direction";
            }

            String cardinalDirection;
            if (windDirection >= 348 || windDirection < 11) {
                cardinalDirection = "N";
            } else if (windDirection < 34) {
                cardinalDirection = "NNE";
            } else if (windDirection < 56) {
                cardinalDirection = "NE";
            } else if (windDirection < 79) {
                cardinalDirection = "ENE";
            } else if (windDirection < 101) {
                cardinalDirection = "E";
            } else if (windDirection < 124) {
                cardinalDirection = "ESE";
            } else if (windDirection < 146) {
                cardinalDirection = "SE";
            } else if (windDirection < 168) {
                cardinalDirection = "SSE";
            } else if (windDirection < 191) {
                cardinalDirection = "S";
            } else if (windDirection < 214) {
                cardinalDirection = "SSW";
            } else if (windDirection < 236) {
                cardinalDirection = "SW";
            } else if (windDirection < 259) {
                cardinalDirection = "WSW";
            } else if (windDirection < 281) {
                cardinalDirection = "W";
            } else if (windDirection < 304) {
                cardinalDirection = "WNW";
            } else if (windDirection < 326) {
                cardinalDirection = "NW";
            } else {
                cardinalDirection = "NNW";
            }

            return cardinalDirection;
        }

        @Override
        public String toString() {
            return "Weather @ " + getTime() + " " + (isDay() ? "(day)" : "(night)") + ": " +
                    getTemperature() + "°C, " +
                    getWeathercode().wmoDesc + ", " +
                    "winds from " +
                    degreeDirectionToCardinal(getWinddirection()) +
                    " at " + getWindspeed() + " kph";
        }
    }

    // for now, only handle the current weather case, forecast later
    @Data
    public static class OpenMeteoResponse {
        private double latitude;
        private double longitude;
        private double generationtimeMs;
        private int utcOffsetSeconds;
        private String timezone;
        private String timezoneAbbreviation;
        private int elevation;
        private CurrentWeather currentWeather;
    }

    public static String getCurrentWeather(String location) {
        Double lat, lon;
        String tz;

        String lookup = location.trim().toLowerCase();
        if (shortcutCities.containsKey(lookup)) {
            var cityData = shortcutCities.get(lookup);
            lat = cityData.getLatitude();
            lon = cityData.getLongitude();
            tz = cityData.getTimezone();
        } else if (location.contains(",")) {
            // try parsing it as double,double
            List<String> numbers = Splitter.on(',').splitToList(location);
            if (numbers.size() != 2) {
                return "unrecognized location: " + location;
            }
            try {
                lat = Double.parseDouble(numbers.get(0));
                lon = Double.parseDouble(numbers.get(1));
            } catch (NumberFormatException nfe) {
                return "unparseable location: " + location;
            }
            tz = null; // there exist services/libraries that can try to map lat/lon to tz, but for now...
        } else {
            return "please either supply a shortcut city name or a latitude,longitude pair";
        }

        String currentWeatherURL = "https://api.open-meteo.com/v1/forecast?latitude={LAT}&longitude={LON}&current_weather=true&forecast_days=0"
                .replace("{LAT}", urlEscaper.escape(lat.toString()))
                .replace("{LON}", urlEscaper.escape(lon.toString()));

        if (!Strings.isNullOrEmpty(tz)) {
            currentWeatherURL += "&timezone=" + urlEscaper.escape(tz);
        }

        // todo: cache result to be nice to counterparty (10 minutes?)
        // todo: maybe use ratelimiter from guava too?
        log.info("weather URL: {}", currentWeatherURL);
        String responseBody;
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(new URI(currentWeatherURL))
                                                          .timeout(Duration.of(5, ChronoUnit.SECONDS))
                                                          .GET()
                                                          .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            String body = response.body();

            if (statusCode != 200) {
                log.warn("Non-200 response code from open-meteo API: {}, response body: {}", statusCode, body);
                return "Non-200 response code from open-meteo API: " + statusCode;
            } else {
                log.info("200 OK from open-meteo");
            }

            if (body.contains("error")) {
                return "looks like open-meteo is returning an error: " + body;
            }

            OpenMeteoResponse omResponse = om.readValue(body, OpenMeteoResponse.class);
            return omResponse.getCurrentWeather().toString();
        } catch (URISyntaxException use) {
            return "URISyntaxException: " + use.getMessage();
        } catch (IOException ioe) {
            return "IOException: " + ioe.getMessage();
        } catch (InterruptedException ie) {
            return "InterruptedException: " + ie.getMessage();
        }
    }
}
