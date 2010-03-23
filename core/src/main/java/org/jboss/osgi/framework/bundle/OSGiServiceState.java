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

// $Id: $

import java.security.AccessControlContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.beans.info.spi.BeanInfo;
import org.jboss.dependency.spi.ControllerContext;
import org.jboss.dependency.spi.ScopeInfo;
import org.jboss.dependency.spi.dispatch.InvokeDispatchContext;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.kernel.Kernel;
import org.jboss.kernel.spi.config.KernelConfigurator;
import org.jboss.metadata.spi.scope.CommonLevels;
import org.jboss.metadata.spi.scope.Scope;
import org.jboss.metadata.spi.scope.ScopeKey;
import org.jboss.osgi.framework.metadata.CaseInsensitiveDictionary;
import org.jboss.osgi.framework.plugins.ControllerContextPlugin;
import org.jboss.osgi.framework.plugins.FrameworkEventsPlugin;
import org.jboss.osgi.framework.plugins.ServiceManagerPlugin;
import org.jboss.osgi.spi.util.BundleClassLoader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * OSGiServiceState.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 * @author thomas.diesler@jboss.com
 * @version $Revision: 1.1 $
 */
public class OSGiServiceState extends OSGiControllerContext implements ServiceReference, ServiceRegistration, InvokeDispatchContext
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

   /** Is this service facotry */
   private boolean isServiceFactory;

   /** The service factory provided service cache */
   private Map<AbstractBundleState, Object> serviceCache;

   /** The properties */
   private CaseInsensitiveDictionary prevProperties;
   private CaseInsensitiveDictionary currProperties;

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
   @SuppressWarnings({ "rawtypes", "unchecked" })
   public OSGiServiceState(AbstractBundleState bundleState, String[] clazzes, Object service, Dictionary properties)
   {
      super(service, properties);

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
      this.isServiceFactory = (service instanceof ServiceFactory);

      if (isServiceFactory == false)
         checkObjClass(service);

      if (properties == null)
         properties = new Hashtable();
      
      properties.put(Constants.SERVICE_ID, getServiceId());
      properties.put(Constants.OBJECTCLASS, getClasses());
      this.currProperties = new CaseInsensitiveDictionary(properties);

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
      if (isServiceFactory == false)
      {
         clazz = target.getClass();
      }
      else if (clazzes.length == 1)
      {
         className = clazzes[0];
         OSGiBundleManager manager = bundleState.getBundleManager();
         clazz = manager.loadClassFailsafe(bundleState, className);
      }

      ScopeInfo info = OSGiScopeInfo.createScopeInfo(getName(), className, clazz, this);
      setScopeInfo(info);

      ScopeKey scope;
      ScopeKey mutableScope;
      if (bundleState instanceof OSGiBundleState)
      {
         AbstractDeployedBundleState obs = (AbstractDeployedBundleState)bundleState;
         DeploymentUnit unit = obs.getDeploymentUnit();
         scope = unit.getScope();
         mutableScope = unit.getMutableScope();
      }
      else
      {
         // TODO - what to do for system bundle?
         scope = new ScopeKey(CommonLevels.SERVER, "JBoss");
         mutableScope = null;
      }
      mergeScopes(info.getScope(), scope);
      mergeScopes(info.getMutableScope(), mutableScope);
   }

   /**
    * Merge scope keys.
    *
    * @param contextKey the context key
    * @param unitKey the unit key
    */
   protected static void mergeScopes(ScopeKey contextKey, ScopeKey unitKey)
   {
      if (contextKey == null)
         return;
      if (unitKey == null)
         return;

      Collection<Scope> unitScopes = unitKey.getScopes();
      if (unitScopes == null || unitScopes.isEmpty())
         return;

      for (Scope scope : unitScopes)
         contextKey.addScope(scope);
   }

   @Override
   public Object getTarget()
   {
      return isServiceFactory ? null : serviceOrFactory;
   }

   void clearTarget()
   {
      serviceOrFactory = null;
   }

   protected Object getActualUser(ControllerContext context)
   {
      OSGiBundleManager manager = bundleState.getBundleManager();
      ControllerContextPlugin plugin = manager.getPlugin(ControllerContextPlugin.class);
      return plugin.getBundleForContext(context);
   }

   protected Object getTargetForActualUser(Object user)
   {
      if (user instanceof AbstractBundleState)
         return getService(AbstractBundleState.class.cast(user));
      else
         return getTarget();
   }

   protected Object ungetTargetForActualUser(Object user)
   {
      if (user instanceof AbstractBundleState)
         return ungetService(AbstractBundleState.class.cast(user));
      else
         return getTarget();
   }

   @Override
   public Object invoke(String name, Object[] parameters, String[] signature) throws Throwable
   {
      return getBeanInfo().invoke(getTarget(), name, signature, parameters);
   }

   @Override
   public ClassLoader getClassLoader() throws Throwable
   {
      return getClassLoaderInternal();
   }

   private ClassLoader getClassLoaderInternal()
   {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null)
         sm.checkPermission(GET_CLASSLOADER_PERMISSION);

      return BundleClassLoader.createClassLoader(getBundle());
   }

   @Override
   public Object get(String name) throws Throwable
   {
      return getBeanInfo().getProperty(getTarget(), name);
   }

   @Override
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
      if (isServiceFactory)
         throw new IllegalArgumentException("Cannot use DispatchContext on ServiceFactory: " + this);

      if (isUnregistered())
         return null;

      if (beanInfo == null)
      {
         try
         {
            OSGiBundleManager manager = bundleState.getBundleManager();
            Kernel kernel = manager.getKernel();
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
      if (isServiceFactory)
      {
         if (serviceCache == null)
            serviceCache = new ConcurrentHashMap<AbstractBundleState, Object>();

         service = serviceCache.get(bundleState);
         if (service == null)
         {
            ServiceFactory serviceFactory = (ServiceFactory)serviceOrFactory;
            
            // If the service object returned by the ServiceFactory object is not an instanceof all the classes named when 
            // the service was registered or the ServiceFactory object throws an exception, null is returned and a Framework 
            // event of type FrameworkEvent.ERROR containing a ServiceException  describing the error is fired. 
            try
            {
               service = serviceFactory.getService(bundleState.getBundle(), getRegistration());
            }
            catch (Throwable t)
            {
               String msg = "Cannot get service from: " + serviceFactory;
               ServiceException serviceException = new ServiceException(msg, ServiceException.FACTORY_EXCEPTION, t);
               log.error("Exception in ServiceFactory.getService()", serviceException);
               FrameworkEventsPlugin plugin = bundleState.getBundleManager().getPlugin(FrameworkEventsPlugin.class);
               plugin.fireFrameworkEvent(bundleState, FrameworkEvent.ERROR, serviceException);
               return null;
            }
            try
            {
               service = checkObjClass(service);
               serviceCache.put(bundleState, service);
            }
            catch (Throwable t)
            {
               String msg = "Cannot get service from: " + serviceFactory;
               ServiceException serviceException = new ServiceException(msg, ServiceException.FACTORY_ERROR);
               log.error("Invalid type from ServiceFactory.getService()", serviceException);
               FrameworkEventsPlugin plugin = bundleState.getBundleManager().getPlugin(FrameworkEventsPlugin.class);
               plugin.fireFrameworkEvent(bundleState, FrameworkEvent.ERROR, serviceException);
               return null;
            }
         }
      }
      return service;
   }

   /**
    * Unget the service from cache.
    *
    * @param bundleState the bundle state
    * @return ungot service
    */
   Object ungetService(AbstractBundleState bundleState)
   {
      if (isUnregistered())
         return null;

      Object service = serviceOrFactory;
      if (isServiceFactory)
      {
         if (serviceCache == null)
            return null;

         service = serviceCache.get(bundleState);

         // Call ungetService if this is the last reference
         int count = getContextTracker().getUsedByCount(this, bundleState);
         if (count == 1)
         {
            serviceCache.remove(bundleState);
            ServiceFactory serviceFactory = (ServiceFactory)serviceOrFactory;
            try
            {
               serviceFactory.ungetService(bundleState.getBundle(), getRegistration(), service);
            }
            catch (Throwable t)
            {
               log.warn("Error from ungetService for " + this, t);
               FrameworkEventsPlugin plugin = bundleState.getBundleManager().getPlugin(FrameworkEventsPlugin.class);
               plugin.fireFrameworkEvent(bundleState, FrameworkEvent.WARNING, new BundleException("Error using service factory:" + serviceFactory, t));
            }
         }
      }
      return service;
   }

   ServiceRegistration getRegistration()
   {
      return serviceRegistration;
   }

   @Override
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

   @Override
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

   @Override
   public Object getProperty(String key)
   {
      if (key == null)
         return null;
      return currProperties.get(key);
   }

   @Override
   public String[] getPropertyKeys()
   {
      ArrayList<String> result = new ArrayList<String>();
      if (currProperties != null)
      {
         Enumeration<String> keys = currProperties.keys();
         while (keys.hasMoreElements())
            result.add(keys.nextElement());
      }
      return result.toArray(new String[result.size()]);
   }

   @Override
   @SuppressWarnings({ "rawtypes", "unchecked" })
   public void setProperties(Dictionary properties)
   {
      checkUnregistered();

      // Remember the previous properties for a potential
      // delivery of the MODIFIED_ENDMATCH event
      prevProperties = currProperties;
      
      if (properties == null)
         properties = new Hashtable();
      
      properties.put(Constants.SERVICE_ID, getServiceId());
      properties.put(Constants.OBJECTCLASS, getClasses());
      currProperties = new CaseInsensitiveDictionary(properties);

      // This event is synchronously delivered after the service properties have been modified. 
      FrameworkEventsPlugin plugin = bundleState.getBundleManager().getPlugin(FrameworkEventsPlugin.class);
      plugin.fireServiceEvent(bundleState, ServiceEvent.MODIFIED, this);
   }

   
   @SuppressWarnings("rawtypes")
   public Dictionary getPreviousProperties()
   {
      return prevProperties;
   }

   @Override
   public Bundle[] getUsingBundles()
   {
      OSGiBundleManager manager = bundleState.getBundleManager();
      ServiceManagerPlugin plugin = manager.getPlugin(ServiceManagerPlugin.class);
      Set<Bundle> bundles = plugin.getUsingBundles(this);
      if (bundles.size() == 0)
         return null;

      return bundles.toArray(new Bundle[bundles.size()]);
   }

   @Override
   public boolean isAssignableTo(Bundle bundle, String className)
   {
      return MDRUtils.isAssignableTo(this, bundleState, bundle, className);
   }

   @Override
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

   /*
    * If this ServiceReference and the specified ServiceReference have the same service id they are equal. 
    * This ServiceReference is less than the specified ServiceReference if it has a lower service ranking 
    * and greater if it has a higher service ranking. 
    * 
    * Otherwise, if this ServiceReference and the specified ServiceReference have the same service ranking, 
    * this ServiceReference is less than the specified ServiceReference if it has a higher service id and 
    * greater if it has a lower service id.     
    */
   @Override
   public int compareTo(Object reference)
   {
      if (reference == null)
         throw new IllegalArgumentException("Null reference");

      ControllerContext other;
      if (reference instanceof ControllerContext)
         other = (ControllerContext)reference;
      else if (reference instanceof ControllerContextHandle)
         other = ((ControllerContextHandle)reference).getContext();
      else
         throw new IllegalArgumentException(reference + " is not a service reference");

      Comparator<ControllerContext> comparator = ContextComparator.getInstance();
      return comparator.compare(this, other);
   }

   String toLongString()
   {
      StringBuilder builder = new StringBuilder();
      String desc = (String)getProperty(Constants.SERVICE_DESCRIPTION);
      builder.append("Service{");
      builder.append("id=").append(getServiceId());
      builder.append(desc != null ? ",desc=" + desc : "");
      builder.append(",bundle=").append(getBundleState().getCanonicalName());
      builder.append(",classes=").append(Arrays.asList(getClasses()));
      builder.append(isServiceFactory ? ",factory=" : ",service=").append(serviceOrFactory);
      builder.append(",props=").append(currProperties);
      builder.append("}");
      return builder.toString();
   }

   /**
    * Register the service
    */
   void internalRegister()
   {
      checkPermission("register", true);
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
      Object desc = getProperty(Constants.SERVICE_DESCRIPTION);
      Object rank = getProperty(Constants.SERVICE_RANKING);
      builder.append("Service{");
      builder.append("id=").append(getServiceId());
      builder.append(rank != null ? ",rank=" + rank : "");
      builder.append(desc != null ? ",desc=" + desc : "");
      builder.append(",classes=").append(Arrays.asList(getClasses()));
      builder.append("}");
      return builder.toString();
   }
}
