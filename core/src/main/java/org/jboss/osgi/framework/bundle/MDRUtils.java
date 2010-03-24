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
    * Get the source of a class for ServiceReference.isAssignable()
    * 
    * @param className the class name
    * @return the source or null if no source
    */
   private static Object getSource(AbstractBundleState bundleState, String className)
   {
      if (bundleState.getState() == Bundle.UNINSTALLED)
         return null;
      
      // [TODO] some more efficient way than using the class?
      OSGiBundleManager bundleManager = bundleState.getBundleManager();
      return bundleManager.loadClassFailsafe(bundleState, className);
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
    * Tests if the bundle that registered the ControllerContext (source bundle) and the specified user of the ControllerContext (target bundle)
    * use the same source for the package of the specified class name.
    * 
    * This method performs the following checks:
    * 
    *    1. Get the package name from the specified class name.
    *    2. For the bundle that registered the ControllerContext (source bundle); find the source for the package.
    *       If no source is found then return true if the source bundle is equal to the target bundle; otherwise return false.
    *    3. If the package source of the source bundle is equal to the package source of the target bundle then return true; 
    *       otherwise return false.
    *
    * @param context the context
    * @param sourceBundle the source bundle state
    * @param targetBundle the target bundle state
    * @param className the class name
    * @return true if the source bundle and the target bundle use the same source for the package of the specified class name; otherwise false.
    */
   public static boolean isAssignableTo(ControllerContext context, AbstractBundleState sourceBundle, AbstractBundleState targetBundle, String className)
   {
      if (context == null)
         throw new IllegalArgumentException("Null context");
      if (sourceBundle == null)
         throw new IllegalArgumentException("Null source bundle");
      if (targetBundle == null)
         throw new IllegalArgumentException("Null target bundle");
      if (className == null)
         throw new IllegalArgumentException("Null class name");
      
      if (sourceBundle == targetBundle)
         return true;

      if (KernelUtils.isUnregistered(context))
         return false;

      return isAssignableTo(sourceBundle, targetBundle, className);
   }

   /**
    * Is assignable.
    *
    * @param sourceBundle the source bundle
    * @param targetBundle the target bundle
    * @param className the class name
    * @return true if assignable, false otherwise
    */
   private static boolean isAssignableTo(AbstractBundleState sourceBundle, AbstractBundleState targetBundle, String className)
   {
      if (sourceBundle == null)
         throw new IllegalArgumentException("Null source bundle");
      if (className == null)
         throw new IllegalArgumentException("Null class name");
      
      // If no source is found return true if the source bundle is equal to the target bundle; otherwise return false
      Object source = getSource(sourceBundle, className);
      if (source == null)
         return sourceBundle.equals(targetBundle);

      Object target = getSource(targetBundle, className);
      if (target == null)
      {
         log.debug("Cannot load '" + className + "' from: " + targetBundle);
         return false;
      }

      boolean equals = source.equals(target);
      if (equals == false)
      {
         ClassLoader targetLoader = getClassLoader(target);
         ClassLoader sourceLoader = getClassLoader(source);
         StringBuffer buffer = new StringBuffer("Cannot assign '" + className + "' comming from different exporters");
         buffer.append("\n  source: ").append(sourceLoader);
         buffer.append("\n  target: ").append(targetLoader);
         log.warn(buffer.toString());
      }
      return equals;
   }

   /**
    * Is assignable to bundle.
    *
    * @param context the context
    * @param sourceBundle the bundle state
    * @param className the class name
    * @return true if assignable, false otherwise
    */
   public static boolean isAssignableTo(ControllerContext context, AbstractBundleState sourceBundle, String className)
   {
      if (sourceBundle == null)
         throw new IllegalArgumentException("Null bundle state");
      
      OSGiBundleManager manager = sourceBundle.getBundleManager();
      ControllerContextPlugin plugin = manager.getPlugin(ControllerContextPlugin.class);
      AbstractBundleState targetBundle = plugin.getBundleForContext(context);
      return isAssignableTo(context, sourceBundle, targetBundle, className);
   }

   /**
    * Is context assignable to bundle.
    *
    * @param context the context
    * @param sourceBundle the bundle state
    * @return true if assignable, false otherwise
    */
   public static boolean isAssignableTo(ControllerContext context, AbstractBundleState sourceBundle)
   {
      if (context == null)
         throw new IllegalArgumentException("Null context");
      if (sourceBundle == null)
         throw new IllegalArgumentException("Null bundle state");

      OSGiBundleManager manager = sourceBundle.getBundleManager();
      ControllerContextPlugin plugin = manager.getPlugin(ControllerContextPlugin.class);
      
      // context's bundle
      AbstractBundleState targetBundle = plugin.getBundleForContext(context);
      if (sourceBundle == targetBundle)
         return true;
      if (KernelUtils.isUnregistered(context))
         return false;

      String[] classes = getProperty(context, Constants.OBJECTCLASS, String[].class);
      if (classes == null)
         return false;

      for (String className : classes)
      {
         if (isAssignableTo(sourceBundle, targetBundle, className) == false)
            return false;
      }
      return true;
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