package net.postmodernapps.filterkhor.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Map;

public class ServiceDescription {
    public String country_code;
    public String timestamp;
    public Map<String, Technology> technologies;

    public static ServiceDescription fromJSONString(String jsonString)
    {
        GsonBuilder gsonBuilder = new GsonBuilder();
        //gsonBuilder.registerTypeAdapter(Date.class, new GmtDateTypeAdapter());
        Gson gson = gsonBuilder.setPrettyPrinting().create();
        ServiceDescription sd = gson.fromJson(jsonString, ServiceDescription.class);
        if (sd != null && sd.technologies != null) {
            // Store keys inside objects as well, for easier access
            for (String key : sd.technologies.keySet()) {
                Technology technology = sd.technologies.get(key);
                technology.name = key;
            }
        }
        return sd;
    }
}
