package org.jboss.osgi.framework.plugins;

import java.util.Map;

/**
 * The provider for the Framework properties.
 * 
 * This cannot be handled by an ordinary framework plugin because
 * we need those props for setting up the classloader domain
 * 
 * @author thomas.diesler@jboss.com
 * @since 04-May-2010
 */
public interface FrameworkProperties
{
   /**
    * Set the property map.
    */
   void setProperties(Map<String, Object> props);

   /**
    * Get a property
    */
   String getProperty(String key);

   /**
    * Set a property
    */
   void setProperty(String key, String value);

}