package com.csc510.smartweather.controller;

import com.csc510.smartweather.service.RecommendationsService;
import com.csc510.smartweather.service.WeatherCodesService;
import com.csc510.smartweather.utilities.RequestsHandler;
import com.csc510.smartweather.utilities.Utils;
import com.csc510.smartweather.dto.CurrentWeatherDTO;
import com.csc510.smartweather.provider.OpenWeatherProvider;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;


@Controller
public class IndexController {
    @Autowired
    private RecommendationsService recommendationsService;
    @Autowired
    private WeatherCodesService weatherCodesService;
    @Autowired
    private OpenWeatherProvider openWeatherProvider;
    @Autowired
    private RequestsHandler requestsHandler;

    private int weather_code;
    private final String GOOGLE_API_KEY = "REPLACE_THIS_WITH_KEY";

    @GetMapping("/")
    public String index(Model model,
                        HttpServletRequest request,
                        @RequestParam(value = "latitude", required = false) String latitude,
                        @RequestParam(value = "longitude", required = false) String longitude) {
        //Determine whether the latitude and longitude are received from the front end, if received, mark the session as located
        if (latitude != null && longitude != null) {
            request.getSession().setAttribute("located", true);
            request.getSession().setAttribute("latitude",latitude);
            request.getSession().setAttribute("longitude",longitude);
        }

        //Check whether the current session has been located, and pass the flag to the front end to determine whether to call the javascript positioning code
        Boolean located = (Boolean) request.getSession().getAttribute("located");
        if (located != null && located) {
            model.addAttribute("located",  true);
        } else {
            model.addAttribute("located", false);
        }

        String lat = (String)request.getSession().getAttribute("latitude");
        String lon = (String)request.getSession().getAttribute("longitude");

        if (lat != null && lon != null) {
            model.addAttribute("latitude",lat);
            model.addAttribute("longitude",lon);
            model.addAttribute("currentweather", openWeatherProvider.CurrentWeatherInfo(lat,lon));
            model.addAttribute("weather_forecast", openWeatherProvider.WeatherForecastInfo(lat,lon));
            request.getSession().setAttribute("weatherID", openWeatherProvider.CurrentWeatherInfo(lat,lon).getId());
            weather_code = (Integer)request.getSession().getAttribute("weatherID");
            model.addAttribute("weather_codes", weatherCodesService.getWeatherCode(weather_code));
            model.addAttribute("recommendations", recommendationsService.getRecommendations(weather_code));
            Map<String, String> params = new HashMap<>();
            params.put("latlng", lat+','+lon);
            params.put("key", GOOGLE_API_KEY);
            JSONObject locationJSON = requestsHandler.getRequestJSON("https://maps.googleapis.com/maps/api/geocode/json", params);
            String city = Utils.getCityFromJSON(locationJSON);
            model.addAttribute("city", city);
            request.getSession().setAttribute("city", city);
        }

        return "index";
    }

    @GetMapping("/search")
    public String search(Model model, @RequestParam(value = "searchStr") String searchStr) {
        model.addAttribute("weather_codes", weatherCodesService.getWeatherCode(weather_code));
        model.addAttribute("recommendations", recommendationsService.getRecommendations(weather_code));
        Map<String, String> params = new HashMap<>();
        params.put("address", searchStr);
        params.put("key", GOOGLE_API_KEY);
        JSONObject locationJSON = requestsHandler.getRequestJSON("https://maps.googleapis.com/maps/api/geocode/json", params);
        String[] latlong = new String[] {"", ""};
        if (locationJSON != null) {
            latlong = Utils.getLatLongFromJSON(locationJSON);
            model.addAttribute("located",  true);
            model.addAttribute("latitude",latlong[0]);
            model.addAttribute("longitude",latlong[1]);
            CurrentWeatherDTO cw = openWeatherProvider.CurrentWeatherInfo(latlong[0], latlong[1]);
            model.addAttribute("city", cw.getCity());
            model.addAttribute("currentweather", cw);
            model.addAttribute("weather_forecast", openWeatherProvider.WeatherForecastInfo(latlong[0],latlong[1]));
            weather_code = cw.getId();
            model.addAttribute("weather_codes", weatherCodesService.getWeatherCode(weather_code));
            model.addAttribute("recommendations", recommendationsService.getRecommendations(weather_code));
        }
        return "index";
    }


    @GetMapping("/sign-out")
    public String signOut(HttpServletRequest request, HttpServletResponse response) {
        //Handle exception

        request.getSession().removeAttribute("user");
        Cookie cookie = new Cookie("sw-token", null);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return "redirect:/";
    }
}
