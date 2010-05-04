/*
* JBoss, Home of Professional Open Source
* Copyright 2009, Red Hat Middleware LLC, and individual contributors
* as indicated by the @author tags. See the copyright.txt file in the
* distribution for a full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.jboss.osgi.framework.plugins;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.util.platform.Java;
import org.osgi.framework.Constants;

/**
 * The provider for the Framework properties.
 * 
 * This cannot be handled by an ordinary framework plugin because
 * we need those props for setting up the classloader domain
 * 
 * @author thomas.diesler@jboss.com
 * @since 04-May-2010
 */
public class FrameworkProperties
{
   /** The framework execution environment */
   private static String OSGi_FRAMEWORK_EXECUTIONENVIRONMENT;
   /** The framework language */
   private static String OSGi_FRAMEWORK_LANGUAGE = Locale.getDefault().getISO3Language(); // REVIEW correct?
   /** The os name */
   private static String OSGi_FRAMEWORK_OS_NAME;
   /** The os version */
   private static String OSGi_FRAMEWORK_OS_VERSION;
   /** The os version */
   private static String OSGi_FRAMEWORK_PROCESSOR;
   /** The framework vendor */
   private static String OSGi_FRAMEWORK_VENDOR = "jboss.org";
   /** The framework version. This is the version of the org.osgi.framework package in r4v42 */
   private static String OSGi_FRAMEWORK_VERSION = "1.5";
   /** The frame work properties */
   private Map<String, Object> properties = new ConcurrentHashMap<String, Object>();

   static
   {
      AccessController.doPrivileged(new PrivilegedAction<Object>()
      {
         public Object run()
         {
            List<String> execEnvironments = new ArrayList<String>();
            if (Java.isCompatible(Java.VERSION_1_5))
               execEnvironments.add("J2SE-1.5");
            if (Java.isCompatible(Java.VERSION_1_6))
               execEnvironments.add("JavaSE-1.6");

            String envlist = execEnvironments.toString();
            envlist = envlist.substring(1, envlist.length() - 1);
            OSGi_FRAMEWORK_EXECUTIONENVIRONMENT = envlist;

            OSGi_FRAMEWORK_OS_NAME = System.getProperty("os.name");
            OSGi_FRAMEWORK_OS_VERSION = System.getProperty("os.version");
            OSGi_FRAMEWORK_PROCESSOR = System.getProperty("os.arch");

            System.setProperty("org.osgi.vendor.framework", "org.jboss.osgi.plugins.framework");
            return null;
         }
      });
   }

   public FrameworkProperties(Map<String, Object> props)
   {
      setProperties(props);
   }

   public void setProperties(Map<String, Object> props)
   {
      properties.putAll(props);

      // Init default framework properties
      if (getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT) == null)
         setProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT, OSGi_FRAMEWORK_EXECUTIONENVIRONMENT);
      if (getProperty(Constants.FRAMEWORK_LANGUAGE) == null)
         setProperty(Constants.FRAMEWORK_LANGUAGE, OSGi_FRAMEWORK_LANGUAGE);
      if (getProperty(Constants.FRAMEWORK_OS_NAME) == null)
         setProperty(Constants.FRAMEWORK_OS_NAME, OSGi_FRAMEWORK_OS_NAME);
      if (getProperty(Constants.FRAMEWORK_OS_VERSION) == null)
         setProperty(Constants.FRAMEWORK_OS_VERSION, OSGi_FRAMEWORK_OS_VERSION);
      if (getProperty(Constants.FRAMEWORK_PROCESSOR) == null)
         setProperty(Constants.FRAMEWORK_PROCESSOR, OSGi_FRAMEWORK_PROCESSOR);
      if (getProperty(Constants.FRAMEWORK_VENDOR) == null)
         setProperty(Constants.FRAMEWORK_VENDOR, OSGi_FRAMEWORK_VENDOR);
      if (getProperty(Constants.FRAMEWORK_VERSION) == null)
         setProperty(Constants.FRAMEWORK_VERSION, OSGi_FRAMEWORK_VERSION);
   }
   
   /**
    * Get a property
    */
   public String getProperty(String key)
   {
      Object value = properties.get(key);
      if (value == null)
         value = System.getProperty(key);

      if (value instanceof String == false)
         return null;

      return (String)value;
   }

   /**
    * Set a property. This is used at the framework init state.
    */
   public void setProperty(String key, String value)
   {
      properties.put(key, value);
   }
}
