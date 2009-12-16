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
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.beans.info.spi.BeanInfo;
import org.jboss.dependency.plugins.AbstractControllerContext;
import org.jboss.dependency.spi.ControllerContext;
import org.jboss.dependency.spi.ScopeInfo;
import org.jboss.dependency.spi.dispatch.InvokeDispatchContext;
import org.jboss.dependency.spi.tracker.ContextTracker;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.kernel.Kernel;
import org.jboss.kernel.spi.config.KernelConfigurator;
import org.jboss.metadata.spi.scope.CommonLevels;
import org.jboss.metadata.spi.scope.Scope;
import org.jboss.metadata.spi.scope.ScopeKey;
import org.jboss.osgi.framework.plugins.FrameworkEventsPlugin;
import org.jboss.osgi.framework.util.CaseInsensitiveDictionary;
import org.jboss.osgi.spi.util.BundleClassLoader;
import org.jboss.util.collection.CollectionsFactory;
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
   /** The alias constant */
   private static final String SERVICE_ALIAS = "service.alias";

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
   private CaseInsensitiveDictionary properties;

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
      // name is random / unique, we use aliases
      super(GUID.asString(), getAlias(properties), OSGiControllerContextActions.ACTIONS, null, service);

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

      if (properties != null)
         this.properties = new CaseInsensitiveDictionary(properties);

      serviceRegistration = new OSGiServiceRegistrationWrapper(this);

      initOSGiScopeInfo();
   }

   /**
    * Check if there is an alias in properties.
    *
    * @param properties the properties
    * @return alias or null
    */
   protected static Set<Object> getAlias(Dictionary<String, Object> properties)
   {
      if (properties != null)
      {
         Set<Object> aliases = null;
         Enumeration<String> keys = properties.keys();
         while (keys.hasMoreElements())
         {
            String key = keys.nextElement();
            if (key.startsWith(SERVICE_ALIAS))
            {
               if (aliases == null)
                  aliases = CollectionsFactory.createLazySet();

               Object alias = properties.get(key);
               aliases.add(alias);
            }
         }
         return aliases;
      }
      return null;
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
         clazz = manager.loadClass(bundleState, className);
      }

      ScopeInfo info = OSGiScopeInfo.createScopeInfo(getName(), className, clazz, this);
      setScopeInfo(info);

      ScopeKey scope;
      ScopeKey mutableScope;
      if (bundleState instanceof OSGiBundleState)
      {
         OSGiBundleState obs = (OSGiBundleState)bundleState;
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

   protected Object getActualUser(ControllerContext context)
   {
      OSGiBundleManager manager = bundleState.getBundleManager();
      return manager.getBundleForContext(context);
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

   public Object invoke(String name, Object[] parameters, String[] signature) throws Throwable
   {
      return getBeanInfo().invoke(getTarget(), name, signature, parameters);
   }

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
    * Unget from cache.
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

         ContextTracker ct = getContextTracker();
         int count = ct.getUsedByCount(this, bundleState);
         if (count == 0) // remove & unget
         {
            serviceCache.remove(bundleState);
            ServiceFactory serviceFactory = (ServiceFactory)serviceOrFactory;
            try
            {
               serviceFactory.ungetService(bundleState, getRegistration(), service);
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

   @SuppressWarnings({ "unchecked", "rawtypes" })
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

   public Bundle[] getUsingBundles()
   {
      ContextTracker ct = getContextTracker();
      if (ct == null)
         return null;

      OSGiBundleManager manager = bundleState.getBundleManager();
      Set<Object> users = ct.getUsers(this);
      Set<Bundle> bundles = new HashSet<Bundle>();
      for (Object user : users)
      {
         AbstractBundleState abs = manager.getBundleForUser(user);
         bundles.add(abs.getBundleInternal());
      }
      return bundles.toArray(new Bundle[bundles.size()]);
   }

   public boolean isAssignableTo(Bundle bundle, String className)
   {
      return MDRUtils.isAssignableTo(this, bundleState, bundle, className);
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

      ControllerContext other;
      if (reference instanceof ControllerContext)
         other = (ControllerContext)reference;
      else if (reference instanceof ControllerContextHandle)
         other = ((ControllerContextHandle)reference).getContext();
      else
         throw new IllegalArgumentException(reference + " is not a service reference");

      Long otherServiceId = MDRUtils.getId(other);
      if (otherServiceId == null)
         return -1; // TODO?

      long thisServiceId = getServiceId();
      if (thisServiceId == otherServiceId)
         return 0;

      Integer otherRanking = MDRUtils.getRanking(other);
      int thisRanking = getServiceRanking();
      int ranking = thisRanking - otherRanking;
      if (ranking != 0)
         return ranking;

      return (thisServiceId > otherServiceId) ? -1 : 1;
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
      builder.append(isServiceFactory ? " factory=" : " service=").append(serviceOrFactory);
      if (properties != null)
         builder.append(" properties=").append(properties);
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
    * Unregister the service
    */
   void internalUnregister()
   {
      ContextTracker ct = getContextTracker();
      if (ct != null) // nobody used us?
      {
         Set<Object> users = ct.getUsers(this);
         if (users.isEmpty() == false)
         {
            Set<AbstractBundleState> used = new HashSet<AbstractBundleState>();
            OSGiBundleManager manager = bundleState.getBundleManager();
            for (Object user : users)
            {
               AbstractBundleState using = manager.getBundleForUser(user);
               if (used.add(using)) // add so we don't do duplicate work
               {
                  int count = ct.getUsedByCount(this, using);
                  while(count > 0)
                  {
                     using.ungetContext(this); // ungetService will cleanup service cache
                     count--;
                  }
               }
            }
         }
      }

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
