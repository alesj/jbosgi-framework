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
package org.jboss.osgi.framework.bundle;

import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;

import org.jboss.dependency.spi.ControllerContext;
import org.jboss.logging.Logger;
import org.jboss.metadata.spi.MetaData;
import org.jboss.metadata.spi.scope.CommonLevels;
import org.jboss.metadata.spi.scope.ScopeLevel;
import org.jboss.osgi.framework.plugins.ControllerContextPlugin;
import org.jboss.osgi.framework.util.KernelUtils;
import org.jboss.util.collection.Iterators;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 * MetaDataRepository utils.
 *
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
public class MDRUtils
{
   /** The log */
   private static final Logger log = Logger.getLogger(MDRUtils.class);

   /** The empty dictionary */
   private static final Dictionary<String, Object> EMPTY = new EmptyDictonary<String, Object>();

   /**
    * Get metadata.
    *
    * @param context the context
    * @param level the scope level
    * @return metadata
    */
   private static MetaData getMetaData(ControllerContext context, ScopeLevel level)
   {
      if (context == null)
         throw new IllegalArgumentException("Null context");

      MetaData metaData = context.getScopeInfo().getMetaData();
      if (level != null && metaData != null)
         metaData = metaData.getScopeMetaData(level);
      return metaData;
   }

   /**
    * Get context's properties.
    *
    * @param context the context
    * @return the properties
    */
   @SuppressWarnings({"unchecked"})
   static Dictionary<String, Object> getProperties(ControllerContext context)
   {
      MetaData metaData = getMetaData(context, CommonLevels.INSTANCE);
      if (metaData != null)
      {
         Dictionary<String, Object> properties = metaData.getMetaData(Dictionary.class);
         if (properties != null)
            return properties;
      }
      return EMPTY;
   }

   /**
    * Get property.
    *
    * @param context the context
    * @param key the property key
    * @param expectedType the expected type
    * @return found property or null
    */
   static <T> T getProperty(ControllerContext context, String key, Class<T> expectedType)
   {
      return getProperty(context, key, expectedType, null);
   }

   /**
    * Get property.
    *
    * @param context the context
    * @param key the property key
    * @param expectedType the expected type
    * @param defaultValue the default value
    * @return found property or null
    */
   static <T> T getProperty(ControllerContext context, String key, Class<T> expectedType, T defaultValue)
   {
      if (key == null)
         throw new IllegalArgumentException("Null key");
      if (expectedType == null)
         throw new IllegalArgumentException("Null expected type");

      Dictionary<String, Object> properties = getProperties(context);
      Object result = properties.get(key);
      if (result != null && expectedType.isInstance(result) == false)
      {
         if (defaultValue == null)
            throw new IllegalArgumentException("Illegal result type: " + result + ", expected: " + expectedType);
         else
            result = defaultValue;
      }

      if (result == null)
         result = defaultValue;

      return expectedType.cast(result);
   }

   /**
    * Is assignable to bundle.
    *
    * @param context the context
    * @param bundleState the bundle state
    * @param otherBundle the other bundle
    * @param className the class name
    * @return true if assignable, false otherwise
    */
   private static boolean isAssignableTo(ControllerContext context, AbstractBundleState bundleState, AbstractBundleState otherBundle, String className)
   {
      if (bundleState == otherBundle)
         return true;

      if (KernelUtils.isUnregistered(context))
         return false;

      return isAssignableTo(bundleState, otherBundle, className);
   }

   /**
    * Is assignable.
    *
    * @param bundleState the bundle state
    * @param other the other bundle
    * @param className the class name
    * @return true if assignable, false otherwise
    */
   private static boolean isAssignableTo(AbstractBundleState bundleState, AbstractBundleState other, String className)
   {
      Object source = bundleState.getSource(className);
      if (source == null)
         throw new IllegalStateException("Cannot load '" + className + "' from: " + bundleState);

      Object otherSource = other.getSource(className);
      if (otherSource == null)
      {
         log.debug("Cannot load '" + className + "' from: " + other);
         return false;
      }

      boolean equals = source.equals(otherSource);
      if (equals == false)
      {
         ClassLoader otherLoader = getClassLoader(otherSource);
         ClassLoader sourceLoader = getClassLoader(source);
         StringBuffer buffer = new StringBuffer("Cannot assign '" + className + "' comming from different exporters");
         buffer.append("\n  service: ").append(sourceLoader);
         buffer.append("\n  request: ").append(otherLoader);
         log.warn(buffer.toString());
      }
      return equals;
   }

   /**
    * Get classloader.
    *
    * @param instance the instance
    * @return instance's classloader
    */
   private static ClassLoader getClassLoader(Object instance)
   {
      return (instance instanceof Class<?>) ? Class.class.cast(instance).getClassLoader() : instance.getClass().getClassLoader();
   }

   /**
    * Match class.
    *
    * @param context the context
    * @param clazz the class to match
    * @return true if the class matches any of the classes
    */
   public static boolean matchClass(ControllerContext context, String clazz)
   {
      String[] classes = getClasses(context);
      return classes != null && Arrays.asList(classes).contains(clazz);
   }

   /**
    * Get service ranking.
    *
    * @param context the context
    * @return the ranking or null
    */
   public static Integer getRanking(ControllerContext context)
   {
      return getProperty(context, Constants.SERVICE_RANKING, Integer.class, 0);
   }

   /**
    * Get service id.
    *
    * @param context the context
    * @return the id or null
    */
   public static Long getId(ControllerContext context)
   {
      return getProperty(context, Constants.SERVICE_ID, Long.class);
   }

   /**
    * Get classes.
    *
    * @param context the context
    * @return the exsposed classes
    */
   public static String[] getClasses(ControllerContext context)
   {
      return getProperty(context, Constants.OBJECTCLASS, String[].class);  
   }

   /**
    * Is context assignable to bundle.
    *
    * @param context the context
    * @param bundleState the bundle state
    * @return true if assignable, false otherwise
    */
   public static boolean isAssignableTo(ControllerContext context, AbstractBundleState bundleState)
   {
      if (context == null)
         throw new IllegalArgumentException("Null context");
      if (bundleState == null)
         throw new IllegalArgumentException("Null bundle state");

      OSGiBundleManager manager = bundleState.getBundleManager();
      ControllerContextPlugin plugin = manager.getPlugin(ControllerContextPlugin.class);
      
      // context's bundle
      AbstractBundleState other = plugin.getBundleForContext(context);
      if (bundleState == other)
         return true;
      if (KernelUtils.isUnregistered(context))
         return false;

      String[] classes = getProperty(context, Constants.OBJECTCLASS, String[].class);
      if (classes == null)
         return false;

      for (String className : classes)
      {
         if (isAssignableTo(bundleState, other, className) == false)
            return false;
      }
      return true;
   }

   /**
    * Is assignable to bundle.
    *
    * @param context the context
    * @param bundleState the bundle state
    * @param className the class name
    * @return true if assignable, false otherwise
    */
   public static boolean isAssignableTo(ControllerContext context, AbstractBundleState bundleState, String className)
   {
      if (context == null)
         throw new IllegalArgumentException("Null context");
      if (bundleState == null)
         throw new IllegalArgumentException("Null bundle state");
      if (className == null)
         throw new IllegalArgumentException("Null class name");

      OSGiBundleManager manager = bundleState.getBundleManager();
      ControllerContextPlugin plugin = manager.getPlugin(ControllerContextPlugin.class);
      AbstractBundleState other = plugin.getBundleForContext(context);
      return isAssignableTo(context, bundleState, other, className);
   }

   /**
    * Is assignable to bundle.
    *
    * @param context the context
    * @param bundleState the bundle state
    * @param bundle the other bundle
    * @param className the class name
    * @return true if assignable, false otherwise
    */
   public static boolean isAssignableTo(ControllerContext context, AbstractBundleState bundleState, Bundle bundle, String className)
   {
      if (context == null)
         throw new IllegalArgumentException("Null context");
      if (bundleState == null)
         throw new IllegalArgumentException("Null bundle state");
      if (bundle == null)
         throw new IllegalArgumentException("Null bundle");
      if (className == null)
         throw new IllegalArgumentException("Null class name");

      AbstractBundleState otherBundle;
      if (bundle instanceof AbstractBundleState)
         otherBundle = (AbstractBundleState)bundle;
      else if (bundle instanceof OSGiBundleWrapper)
         otherBundle = ((OSGiBundleWrapper)bundle).getBundleState();
      else
         throw new IllegalArgumentException("Illegal bundle type: " + bundle);

      return isAssignableTo(context, bundleState, otherBundle, className);
   }

   /**
    * Compare to.
    *
    * @param context the context
    * @param reference the other reference
    * @return compare value
    */
   public static int compareTo(ControllerContext context, Object reference)
   {
      if (context == null)
         throw new IllegalArgumentException("Null context");
      if (reference == null)
         throw new IllegalArgumentException("Null reference");

      ControllerContext other;
      if (reference instanceof ControllerContext)
         other = (ControllerContext)reference;
      else if (reference instanceof ControllerContextHandle)
         other = ((ControllerContextHandle)reference).getContext();
      else
         throw new IllegalArgumentException(reference + " is not a service reference");

      Long thisServiceId = getId(context);
      Long otherServiceId = getId(other);
      if (thisServiceId == null && otherServiceId == null)
         return 0;
      if (otherServiceId == null)
         return -1; // TODO?
      if (thisServiceId == null)
         return 1; // TODO?
      if (thisServiceId - otherServiceId == 0)
         return 0;

      int thisRanking = getRanking(context);
      int otherRanking = getRanking(other);
      int ranking = thisRanking - otherRanking;
      if (ranking != 0)
         return ranking;

      return (thisServiceId > otherServiceId) ? -1 : 1;
   }

   @SuppressWarnings({"unchecked"})
   private static class EmptyDictonary<K, V> extends Dictionary<K, V>
   {
      public int size()
      {
         return 0;
      }

      public boolean isEmpty()
      {
         return true;
      }

      public Enumeration<K> keys()
      {
         return Iterators.toEnumeration(Collections.emptySet().iterator());
      }

      public Enumeration<V> elements()
      {
         return Iterators.toEnumeration(Collections.emptySet().iterator());
      }

      public V get(Object key)
      {
         return null;
      }

      public V put(K key, V value)
      {
         return null;
      }

      public V remove(Object key)
      {
         return null;
      }
   }
}