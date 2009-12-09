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

import java.security.AccessControlContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.beans.info.spi.BeanInfo;
import org.jboss.dependency.plugins.AbstractControllerContext;
import org.jboss.dependency.spi.dispatch.InvokeDispatchContext;
import org.jboss.kernel.Kernel;
import org.jboss.kernel.spi.config.KernelConfigurator;
import org.jboss.osgi.framework.plugins.FrameworkEventsPlugin;
import org.jboss.osgi.framework.util.CaseInsensitiveDictionary;
import org.jboss.osgi.spi.util.BundleClassLoader;
import org.jboss.util.collection.ConcurrentSet;
import org.jboss.util.id.GUID;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * OSGiServiceState.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
public class OSGiServiceState extends AbstractControllerContext implements ServiceReference, ServiceRegistration, InvokeDispatchContext
{
   /** The get classloader permission */
   private static final RuntimePermission GET_CLASSLOADER_PERMISSION = new RuntimePermission("getClassLoader");

   /** Used to generate a unique id */
   private static final AtomicLong serviceIDGenerator = new AtomicLong();

   /** The bundle that registered the service */
   private AbstractBundleState bundleState;

   /** The service reference */
   private OSGiServiceReferenceWrapper serviceReference;

   /** The service registration */
   private OSGiServiceRegistrationWrapper serviceRegistration;

   /** The service id */
   private long serviceId = serviceIDGenerator.incrementAndGet();

   /** The service interfaces */
   private String[] clazzes;

   /** The service or service factory */
   private Object serviceOrFactory;

   /** The service factory provided service cache */
   private Map<AbstractBundleState, Object> serviceCache;

   /** The properties */
   private CaseInsensitiveDictionary properties;

   /** The using bundles */
   private Set<AbstractBundleState> usingBundles = new ConcurrentSet<AbstractBundleState>();

   /** The bean info */
   private BeanInfo beanInfo;

   /**
    * Create a new OSGiServiceState.
    * 
    * @param bundleState the bundle state
    * @param clazzes the interfaces
    * @param service the services
    * @param properties the properties
    * @throws IllegalArgumentException for a null parameter
    */
   @SuppressWarnings("unchecked")
   public OSGiServiceState(AbstractBundleState bundleState, String[] clazzes, Object service, Dictionary properties)
   {
      // name is random / unique, we use aliases
      super(GUID.asString(), OSGiControllerContextActions.ACTIONS, null, service);

      if (bundleState == null)
         throw new IllegalArgumentException("Null bundle state");
      if (clazzes == null || clazzes.length == 0)
         throw new IllegalArgumentException("Null or empty clazzes");

      for (String clazz : clazzes)
      {
         if (clazz == null)
            throw new IllegalArgumentException("Null class in: " + Arrays.toString(clazzes));
      }
      if (service == null)
         throw new IllegalArgumentException("Null service");

      this.bundleState = bundleState;
      this.clazzes = clazzes;
      this.serviceOrFactory = service;

      if (service instanceof ServiceFactory == false)
         checkObjClass(service);

      if (properties != null)
         this.properties = new CaseInsensitiveDictionary(properties);

      serviceRegistration = new OSGiServiceRegistrationWrapper(this);

      initOSGiScopeInfo();
   }

   /**
    * Get the serviceId.
    * 
    * @return the serviceId.
    */
   public long getServiceId()
   {
      return serviceId;
   }

   /**
    * Get the service ranking.
    * 
    * @return the service rankin.
    */
   public int getServiceRanking()
   {
      Object ranking = getProperty(Constants.SERVICE_RANKING);
      if (ranking != null && ranking instanceof Integer)
         return (Integer)ranking;
      return 0;
   }

   /**
    * Get the classes.
    * 
    * @return the classes.
    */
   public String[] getClasses()
   {
      return clazzes;
   }

   @Override
   protected void initScopeInfo()
   {
      // nothing
   }

   protected void initOSGiScopeInfo()
   {
      String className = null;
      Class<?> clazz = null;

      Object target = serviceOrFactory;
      if (target != null && target instanceof ServiceFactory == false)
         clazz = target.getClass();
      else if (clazzes.length == 1)
         className = clazzes[0];

      setScopeInfo(OSGiScopeInfo.createScopeInfo(getName(), className, clazz, this));
   }

   @Override
   public Object getTarget()
   {
      // get service directly
      return getService(bundleState);
   }

   public Object invoke(String name, Object[] parameters, String[] signature) throws Throwable
   {
      return getBeanInfo().invoke(getTarget(), name, signature, parameters);
   }

   public ClassLoader getClassLoader() throws Throwable
   {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null)
         sm.checkPermission(GET_CLASSLOADER_PERMISSION);

      return BundleClassLoader.createClassLoader(getBundle());
   }

   public Object get(String name) throws Throwable
   {
      return getBeanInfo().getProperty(getTarget(), name);
   }

   public void set(String name, Object value) throws Throwable
   {
      getBeanInfo().setProperty(getTarget(), name, value);
   }

   /**
    * Get bean info.
    *
    * @return the bean info
    */
   protected BeanInfo getBeanInfo()
   {
      if (isUnregistered())
         return null;

      if (beanInfo == null)
      {
         try
         {
            Kernel kernel = bundleState.getBundleManager().getKernel();
            KernelConfigurator configurator = kernel.getConfigurator();
            Object service = getTarget(); // should not be null, we're not unregistered
            beanInfo = configurator.getBeanInfo(service.getClass());
         }
         catch (Throwable t)
         {
            throw new RuntimeException(t);
         }
      }
      return beanInfo;
   }

   /**
    * Get the service.
    * 
    * @param bundleState the bundle that requested the service
    * @return the service.
    */
   Object getService(AbstractBundleState bundleState)
   {
      // [TODO] fix race condition with unregistration
      if (isUnregistered())
         return null;

      checkPermission("get", false);

      Object service = serviceOrFactory;
      if (serviceOrFactory instanceof ServiceFactory)
      {
         if (serviceCache == null)
            serviceCache = new ConcurrentHashMap<AbstractBundleState, Object>();

         service = serviceCache.get(bundleState);
         if (service == null)
         {
            ServiceFactory serviceFactory = (ServiceFactory)serviceOrFactory;
            try
            {
               service = checkObjClass(serviceFactory.getService(bundleState.getBundle(), getRegistration()));
               serviceCache.put(bundleState, service);
            }
            catch (Throwable t)
            {
               log.error("Error from getService for " + this, t);
               FrameworkEventsPlugin plugin = bundleState.getBundleManager().getPlugin(FrameworkEventsPlugin.class);
               plugin.fireFrameworkEvent(bundleState, FrameworkEvent.ERROR, new BundleException("Error using service factory:" + serviceFactory, t));
               return null;
            }
         }
      }
      return service;
   }

   /**
    * Get the service registration
    * 
    * @return the service registration
    */
   public ServiceRegistration getRegistration()
   {
      return serviceRegistration;
   }

   public ServiceReference getReference()
   {
      checkUnregistered();
      return getReferenceInternal();
   }

   public ServiceReference getReferenceInternal()
   {
      if (serviceReference == null)
         serviceReference = new OSGiServiceReferenceWrapper(this);
      return serviceReference;
   }

   public Bundle getBundle()
   {
      if (isUnregistered())
         return null;
      return bundleState.getBundleInternal();
   }

   /**
    * Get the bundleState.
    * 
    * @return the bundleState.
    */
   public AbstractBundleState getBundleState()
   {
      return bundleState;
   }

   public Object getProperty(String key)
   {
      if (key == null)
         return null;
      if (Constants.SERVICE_ID.equalsIgnoreCase(key))
         return getServiceId();
      if (Constants.OBJECTCLASS.equalsIgnoreCase(key))
         return getClasses();
      if (properties == null)
         return null;
      return properties.get(key);
   }

   public String[] getPropertyKeys()
   {
      ArrayList<String> result = new ArrayList<String>();
      if (properties != null)
      {
         Enumeration<String> keys = properties.keys();
         while (keys.hasMoreElements())
            result.add(keys.nextElement());
      }
      result.add(Constants.SERVICE_ID);
      result.add(Constants.OBJECTCLASS);
      return result.toArray(new String[result.size()]);
   }

   @SuppressWarnings("unchecked")
   public void setProperties(Dictionary properties)
   {
      checkUnregistered();

      if (properties == null)
         this.properties = null;
      else
         this.properties = new CaseInsensitiveDictionary(properties);

      FrameworkEventsPlugin plugin = bundleState.getBundleManager().getPlugin(FrameworkEventsPlugin.class);
      plugin.fireServiceEvent(bundleState, ServiceEvent.MODIFIED, this);
   }

   /**
    * Mark a bundle as using this service
    * 
    * @param bundleState the bundle
    */
   void addUsingBundle(AbstractBundleState bundleState)
   {
      usingBundles.add(bundleState);
   }

   /**
    * Unmark a bundle as using this service
    * 
    * @param bundleState the bundle
    */
   void removeUsingBundle(AbstractBundleState bundleState)
   {
      usingBundles.remove(bundleState);
   }

   public Bundle[] getUsingBundles()
   {
      if (usingBundles.isEmpty())
         return null;

      Set<Bundle> result = new HashSet<Bundle>();
      for (AbstractBundleState bundleState : usingBundles)
         result.add(bundleState.getBundleInternal());
      return result.toArray(new Bundle[result.size()]);
   }

   public boolean isAssignableTo(Bundle bundle, String className)
   {
      if (bundle == null)
         throw new IllegalArgumentException("Null bundle");
      if (className == null)
         throw new IllegalArgumentException("Null class name");

      if (bundle instanceof OSGiBundleWrapper == false)
         throw new IllegalArgumentException("Unknown bundle: " + bundle);

      OSGiBundleWrapper wrapper = (OSGiBundleWrapper)bundle;
      AbstractBundleState bundleState = wrapper.getBundleState();
      return isAssignableTo(bundleState, className);
   }

   /**
    * Check the isAssignableTo
    * 
    * @param other the bundle state
    * @param className the class name
    * @return true when assignable
    */
   boolean isAssignableTo(AbstractBundleState other, String className)
   {
      if (className == null)
         throw new IllegalArgumentException("Null class name");

      if (other == bundleState)
         return true;

      if (isUnregistered())
         return false;

      Class<?> source = (Class<?>)bundleState.getSource(className);
      if (source == null)
         throw new IllegalStateException("Cannot load '" + className + "' from: " + bundleState);

      Class<?> otherSource = (Class<?>)other.getSource(className);
      if (otherSource == null)
      {
         log.debug("Cannot load '" + className + "' from: " + other);
         return false;
      }

      boolean equals = source.equals(otherSource);
      if (equals == false)
      {
         ClassLoader otherLoader = otherSource.getClassLoader();
         ClassLoader sourceLoader = source.getClassLoader();
         StringBuffer buffer = new StringBuffer("Cannot assign '" + className + "' comming from different exporters");
         buffer.append("\n  service: ").append(sourceLoader);
         buffer.append("\n  request: ").append(otherLoader);
         log.warn(buffer.toString());
      }
      return equals;
   }

   /**
    * Check the isAssignable
    * 
    * @param bundle the bundle state
    * @return true when assignable
    */
   boolean isAssignable(AbstractBundleState bundle)
   {
      if (bundle == bundleState)
         return true;

      if (isUnregistered())
         return false;

      for (String clazz : getClasses())
      {
         if (isAssignableTo(bundle, clazz) == false)
            return false;
      }
      return true;
   }

   /**
    * Match the class
    * 
    * @param className the class name
    * @return true when the class name matches
    */
   boolean matchClass(String className)
   {
      if (clazzes == null || clazzes.length == 0)
         return false;

      for (String clazz : clazzes)
      {
         if (className.equals(clazz))
            return true;
      }
      return false;
   }

   public void unregister()
   {
      checkUnregistered();

      try
      {
         bundleState.unregisterService(this);
      }
      finally
      {
         synchronized (this)
         {
            serviceRegistration = null;
         }
      }
   }

   public int compareTo(Object reference)
   {
      if (reference == null)
         throw new IllegalArgumentException("Null reference");

      OSGiServiceState other;
      if (reference instanceof OSGiServiceState)
         other = (OSGiServiceState)reference;
      else if (reference instanceof OSGiServiceReferenceWrapper)
         other = ((OSGiServiceReferenceWrapper)reference).getServiceState();
      else
         throw new IllegalArgumentException(reference + " is not a service reference");

      long thisServiceId = this.getServiceId();
      long otherServiceId = other.getServiceId();
      if (thisServiceId == otherServiceId)
         return 0;

      int thisRanking = this.getServiceRanking();
      int otherRanking = other.getServiceRanking();
      int ranking = thisRanking - otherRanking;
      if (ranking != 0)
         return ranking;

      if (thisServiceId > otherServiceId)
         return -1;
      else
         return +1;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj == null)
         return false;

      OSGiServiceState other;
      if (obj instanceof OSGiServiceState)
         other = (OSGiServiceState)obj;
      else if (obj instanceof OSGiServiceReferenceWrapper)
         other = ((OSGiServiceReferenceWrapper)obj).getServiceState();
      else
         return false;
      return this == other;
   }

   @Override
   public int hashCode()
   {
      return toString().hashCode();
   }

   @Override
   public String toString()
   {
      StringBuilder builder = new StringBuilder();
      builder.append("Service{");
      builder.append("id=").append(getServiceId());
      builder.append(" classes=").append(Arrays.asList(getClasses()));
      builder.append("}");
      return builder.toString();
   }

   public String toLongString()
   {
      StringBuilder builder = new StringBuilder();
      builder.append("Service{");
      builder.append("id=").append(getServiceId());
      builder.append(" bundle=").append(getBundleState().getCanonicalName());
      builder.append(" classes=").append(Arrays.asList(getClasses()));
      builder.append(serviceOrFactory instanceof ServiceFactory ? " factory=" : " service=").append(serviceOrFactory);
      if (properties != null)
         builder.append(" properties=").append(properties);
      if (usingBundles.isEmpty() == false)
         builder.append(" using=").append(usingBundles);
      builder.append("}");
      return builder.toString();
   }

   /**
    * Register the service
    */
   void internalRegister()
   {
      checkPermission("register", true);
      getBundleState().addRegisteredService(this);
   }

   /**
    * Unregister the service
    */
   void internalUnregister()
   {
      if (usingBundles.isEmpty() == false)
      {
         for (AbstractBundleState using : usingBundles)
         {
            if (using.ungetService(this) == false)
            {
               if (serviceOrFactory instanceof ServiceFactory)
               {
                  ServiceFactory serviceFactory = (ServiceFactory)serviceOrFactory;
                  try
                  {
                     Object service = serviceCache.remove(using);
                     serviceFactory.ungetService(using.getBundle(), getRegistration(), service);
                  }
                  catch (Throwable t)
                  {
                     log.warn("Error from ungetService for " + this, t);
                     FrameworkEventsPlugin plugin = bundleState.getBundleManager().getPlugin(FrameworkEventsPlugin.class);
                     plugin.fireFrameworkEvent(bundleState, FrameworkEvent.WARNING, new BundleException("Error using service factory:" + serviceFactory, t));
                  }
               }
            }
         }
      }

      getBundleState().removeRegisteredService(this);
      serviceOrFactory = null;
   }

   /**
    * Check an object matches the specified classes
    * 
    * @param object the object
    * @return the object if all is ok
    */
   private Object checkObjClass(Object object)
   {
      if (object == null)
         throw new IllegalArgumentException("Null object");

      for (String className : getClasses())
      {
         try
         {
            Class<?> clazz = getBundleState().loadClass(className);
            // [TODO] show classloader information all interfaces for debugging purposes
            if (clazz.isInstance(object) == false)
               throw new IllegalArgumentException(object.getClass().getName() + " does not implement " + className);
         }
         catch (ClassNotFoundException e)
         {
            throw new IllegalArgumentException(object.getClass().getName() + " cannot load class: " + className, e);
         }
      }
      return object;
   }

   /**
    * Check whether the caller has permission
    * 
    * @param action the action to check
    * @param all whether all permissions are required
    */
   void checkPermission(String action, boolean all)
   {
      SecurityManager sm = System.getSecurityManager();
      if (sm == null)
         return;

      String[] clazzes = getClasses();
      SecurityException se = null;
      for (String clazz : clazzes)
      {
         try
         {
            ServicePermission permission = new ServicePermission(clazz, action);
            sm.checkPermission(permission);
            if (all == false)
               return;
         }
         catch (SecurityException e)
         {
            if (all)
               throw e;
            se = e;
         }
      }
      if (se != null)
         throw se;
   }

   /**
    * Check whether the caller has permission
    * 
    * @param accessControlContext access control context
    * @param action the action to check
    * @param all whether all permissions are required
    */
   void checkPermission(AccessControlContext accessControlContext, String action, boolean all)
   {
      if (System.getSecurityManager() == null)
         return;

      String[] clazzes = getClasses();
      SecurityException se = null;
      for (String clazz : clazzes)
      {
         try
         {
            ServicePermission permission = new ServicePermission(clazz, action);
            accessControlContext.checkPermission(permission);
            if (all == false)
               return;
         }
         catch (SecurityException e)
         {
            if (all)
               throw e;
            se = e;
         }
      }
      if (se != null)
         throw se;
   }

   /**
    * Check whether the caller has permission to this object
    * 
    * @return true when the caller has permission
    */
   boolean hasPermission()
   {
      try
      {
         checkPermission("get", false);
         return true;
      }
      catch (SecurityException ignored)
      {
      }
      return false;
   }

   /**
    * Check whether the caller has permission to this object
    * 
    * @param accessControlContext access control context
    * @return true when the caller has permission
    */
   public boolean hasPermission(AccessControlContext accessControlContext)
   {
      try
      {
         checkPermission(accessControlContext, "get", false);
         return true;
      }
      catch (SecurityException ignored)
      {
      }
      return false;
   }

   /**
    * Check if the service is unregistered
    * 
    * @throws IllegalStateException if unregistered
    */
   private void checkUnregistered()
   {
      if (isUnregistered())
         throw new IllegalStateException("Service is unregistered: " + this);
   }

   /**
    * @return true when the service is unregistered
    */
   synchronized boolean isUnregistered()
   {
      return serviceRegistration == null;
   }
}
