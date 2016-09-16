package de.iisys.liferay.portlet.wikiexport;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;

public class ConfigLoader {
	
	private final String PROPERTIES = "schub-conf";
	
	private Map<String, String> fProperties;
	
	public ConfigLoader() {
		fProperties = new HashMap<String, String>();
		loadConfig();
	}
	
	public String getProp(String key) {
		return fProperties.get(key);
	}

	private void loadConfig()
    {
        //read properties file
        final ClassLoader loader = Thread.currentThread()
            .getContextClassLoader();
        ResourceBundle rb = ResourceBundle.getBundle(PROPERTIES,
            Locale.getDefault(), loader);
        
        String key = null;
        String value = null;
        Enumeration<String> keys = rb.getKeys();
        while (keys.hasMoreElements())
        {
            key = keys.nextElement();
            value = rb.getString(key);

            fProperties.put(key, value);
        }
    }
}
