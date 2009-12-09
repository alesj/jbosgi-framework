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

import java.io.File;
import java.io.InputStream;
import java.security.Permission;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.logging.Logger;
import org.jboss.osgi.framework.metadata.OSGiMetaData;
import org.jboss.osgi.framework.plugins.BundleStoragePlugin;
import org.jboss.osgi.framework.plugins.FrameworkEventsPlugin;
import org.jboss.osgi.framework.plugins.LifecycleInterceptorServicePlugin;
import org.jboss.osgi.spi.NotImplementedException;
import org.jboss.osgi.spi.util.ConstantsHelper;
import org.jboss.util.collection.ConcurrentSet;
import org.jboss.virtual.VirtualFile;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.Version;

/**
 * BundleState.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author Thomas.Diesler@jboss.com
 * @version $Revision: 1.1 $
 */
public abstract class AbstractBundleState implements Bundle, BundleContext
{
   /** The log */
   private static final Logger log = Logger.getLogger(AbstractBundleState.class);

   /** The last modified time stamp */
   private long lastModified = System.currentTimeMillis();

   /** The bundle manager */
   private OSGiBundleManager bundleManager;

   /** The osgi metadata */
   private OSGiMetaData osgiMetaData;

   /** The bundle context */
   private BundleContext bundleContext;

   /** The bundle */
   private Bundle bundle;

   /** The bundle state */
   private AtomicInteger state = new AtomicInteger(Bundle.UNINSTALLED);

   /** The registered services in use */
   protected Set<OSGiServiceState> registeredServices = new ConcurrentSet<OSGiServiceState>();

   /** The services in use */
   protected final Map<OSGiServiceState, Integer> servicesInUse = new ConcurrentHashMap<OSGiServiceState, Integer>();
   
   /** The cached symbolic name */
   private String symbolicName;
   
   /** The cached version */
   private Version version;

   /**
    * Create a new BundleState for the system bundle.
    * 
    * @param osgiMetaData the osgi metadata
    * @throws IllegalArgumentException for a null parameter
    */
   AbstractBundleState(OSGiMetaData osgiMetaData)
   {
      if (osgiMetaData == null)
         throw new IllegalArgumentException("Null osgi metadata");
      this.osgiMetaData = osgiMetaData;
   }

   /**
    * Get the bundleManager.
    * 
    * @return the bundleManager.
    */
   public OSGiBundleManager getBundleManager()
   {
      if (bundleManager == null)
         throw new IllegalStateException("Bundle not installed: " + getCanonicalName());
      
      return bundleManager;
   }

   public String getSymbolicName()
   {
      if (symbolicName == null)
      {
         symbolicName = osgiMetaData.getBundleSymbolicName();
         if (symbolicName == null)
            throw new IllegalStateException("Cannot obtain " + Constants.BUNDLE_SYMBOLICNAME);
      }
      return symbolicName;
   }

   public Version getVersion()
   {
      if (version == null)
         version = osgiMetaData.getBundleVersion();
      
      return version;
   }

   public int getState()
   {
      return state.get();
   }

   public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(int signersType)
   {
      throw new NotImplementedException();
   }

   public synchronized BundleContext getBundleContext()
   {
      checkAdminPermission(AdminPermission.CONTEXT);
      return bundleContext;
   }

   public synchronized BundleContext createBundleContext()
   {
      if (bundleContext == null)
         bundleContext = new OSGiBundleContextWrapper(this);
      return bundleContext;
   }

   public synchronized void destroyBundleContext()
   {
      bundleContext = null;
   }

   public synchronized Bundle getBundle()
   {
      checkValidBundleContext();
      return getBundleInternal();
   }

   public synchronized Bundle getBundleInternal()
   {
      if (bundle == null)
         bundle = new OSGiBundleWrapper(this);
      return bundle;
   }

   public Bundle getBundle(long id)
   {
      checkValidBundleContext();
      AbstractBundleState bundleState = getBundleManager().getBundleById(id);
      return bundleState != null ? bundleState.getBundleInternal() : null;
   }

   public Bundle[] getBundles()
   {
      checkValidBundleContext();

      Collection<AbstractBundleState> bundleStates = getBundleManager().getBundles();
      if (bundleStates.isEmpty())
         return new Bundle[0];

      List<Bundle> bundles = new ArrayList<Bundle>(bundleStates.size());
      for (AbstractBundleState bundleState : bundleStates)
         bundles.add(bundleState.getBundleInternal());
      
      return bundles.toArray(new Bundle[bundles.size()]);
   }

   public long getLastModified()
   {
      return lastModified;
   }

   void modified()
   {
      lastModified = System.currentTimeMillis();
   }

   /**
    * Get the osgiMetaData.
    * 
    * @return the osgiMetaData.
    */
   public OSGiMetaData getOSGiMetaData()
   {
      return osgiMetaData;
   }

   @SuppressWarnings("unchecked")
   public Dictionary getHeaders()
   {
      return getHeaders(null);
   }

   @SuppressWarnings("unchecked")
   public Dictionary getHeaders(String locale)
   {
      checkAdminPermission(AdminPermission.METADATA);
      return getOSGiMetaData().getHeaders(locale);
   }

   public String getProperty(String key)
   {
      checkValidBundleContext();
      return getBundleManager().getProperty(key);
   }

   public File getDataFile(String filename)
   {
      checkValidBundleContext();
      BundleStoragePlugin storagePlugin = getBundleManager().getOptionalPlugin(BundleStoragePlugin.class);
      return storagePlugin != null ? storagePlugin.getDataFile(this, filename) : null;
   }

   public boolean hasPermission(Object permission)
   {
      if (permission == null || permission instanceof Permission == false)
         return false;

      SecurityManager sm = System.getSecurityManager();
      if (sm == null)
         return true;

      // [TODO] hasPermission
      return true;
   }

   public Filter createFilter(String filter) throws InvalidSyntaxException
   {
      checkValidBundleContext();
      return FrameworkUtil.createFilter(filter);
   }

   public void addServiceListener(ServiceListener listener)
   {
      addServiceListenerInternal(listener, null);
   }

   public void addServiceListener(ServiceListener listener, String filter) throws InvalidSyntaxException
   {
      Filter theFilter = null;
      if (filter != null)
         theFilter = createFilter(filter);
      addServiceListenerInternal(listener, theFilter);
   }

   public void addServiceListenerInternal(ServiceListener listener, Filter filter)
   {
      checkValidBundleContext();

      FrameworkEventsPlugin plugin = getBundleManager().getPlugin(FrameworkEventsPlugin.class);
      plugin.addServiceListener(this, listener, filter);
   }

   public void removeServiceListener(ServiceListener listener)
   {
      checkValidBundleContext();

      FrameworkEventsPlugin plugin = getBundleManager().getPlugin(FrameworkEventsPlugin.class);
      plugin.removeServiceListener(this, listener);
   }

   /**
    * Add a registered service
    * 
    * @param serviceState the service
    */
   void addRegisteredService(OSGiServiceState serviceState)
   {
      registeredServices.add(serviceState);
   }

   /**
    * Remove a registered service
    * 
    * @param serviceState the service
    */
   void removeRegisteredService(OSGiServiceState serviceState)
   {
      registeredServices.remove(serviceState);
   }

   public ServiceReference[] getRegisteredServices()
   {
      checkInstalled();

      if (registeredServices.isEmpty())
         return null;

      Set<ServiceReference> result = new HashSet<ServiceReference>(registeredServices.size());
      for (OSGiServiceState service : registeredServices)
      {
         if (service.hasPermission())
            result.add(service.getReferenceInternal());
      }
      if (result.isEmpty())
         return null;
      return result.toArray(new ServiceReference[result.size()]);
   }

   /**
    * True if the use count of a service for this bundle is grater that 0.
    * 
    * @param serviceState the service
    * @return true if counter is bigger than zero, false otherwise
    */
   boolean isServiceInUse(OSGiServiceState serviceState)
   {
      synchronized (servicesInUse)
      {
         Integer count = servicesInUse.get(serviceState);
         return (count != null && count > 0);
      }
   }

   /**
    * Increment the use count of a service for this bundle
    * 
    * @param serviceState the service
    */
   void addServiceInUse(OSGiServiceState serviceState)
   {
      synchronized (servicesInUse)
      {
         Integer count = servicesInUse.get(serviceState);
         if (count == null)
            servicesInUse.put(serviceState, 1);
         else
            servicesInUse.put(serviceState, ++count);
      }
      serviceState.addUsingBundle(this);
   }

   /**
    * Decrement the use count of a service for this bundle
    * 
    * @param serviceState the service
    * @return true when the service is still in use by the bundle
    */
   boolean removeServiceInUse(OSGiServiceState serviceState)
   {
      synchronized (servicesInUse)
      {
         Integer count = servicesInUse.get(serviceState);
         if (count == null)
         {
            return false;
         }
         else if (count == 1)
         {
            servicesInUse.remove(serviceState);
            serviceState.removeUsingBundle(this);
            return false;
         }
         else
         {
            servicesInUse.put(serviceState, --count);
         }
      }
      return true;
   }

   public ServiceReference[] getServicesInUse()
   {
      checkInstalled();

      synchronized (servicesInUse)
      {
         Collection<OSGiServiceState> inUse = servicesInUse.keySet();
         if (inUse.isEmpty())
            return null;

         Set<ServiceReference> result = new HashSet<ServiceReference>(inUse.size());
         for (OSGiServiceState service : inUse)
         {
            if (service.hasPermission())
               result.add(service.getReferenceInternal());
         }
         if (result.isEmpty())
            return null;
         return result.toArray(new ServiceReference[result.size()]);
      }
   }

   public ServiceReference[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException
   {
      checkValidBundleContext();
      return getBundleManager().getServiceReferences(this, clazz, filter, false);
   }

   public Object getService(ServiceReference reference)
   {
      checkValidBundleContext();

      if (reference == null)
         throw new IllegalArgumentException("Null reference");

      return getBundleManager().getService(this, reference);
   }

   Object getService(OSGiServiceState serviceState)
   {
      return getBundleManager().getService(this, serviceState);  
   }

   public ServiceReference getServiceReference(String clazz)
   {
      checkValidBundleContext();
      if (clazz == null)
         throw new IllegalArgumentException("Null clazz");
      return getBundleManager().getServiceReference(this, clazz);
   }

   public ServiceReference[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException
   {
      checkValidBundleContext();
      return getBundleManager().getServiceReferences(this, clazz, filter, true);
   }

   @SuppressWarnings("unchecked")
   public ServiceRegistration registerService(String clazz, Object service, Dictionary properties)
   {
      if (clazz == null)
         throw new IllegalArgumentException("Null class");
      return registerService(new String[] { clazz }, service, properties);
   }

   @SuppressWarnings("unchecked")
   public ServiceRegistration registerService(String[] clazzes, Object service, Dictionary properties)
   {
      checkValidBundleContext();

      OSGiServiceState serviceState = getBundleManager().registerService(this, clazzes, service, properties);
      return serviceState.getRegistration();
   }

   /**
    * Unregister a service
    * 
    * @param serviceState the service state
    */
   void unregisterService(OSGiServiceState serviceState)
   {
      getBundleManager().unregisterService(serviceState);
   }

   public boolean ungetService(ServiceReference reference)
   {
      if (reference == null)
         throw new IllegalArgumentException("Null reference");

      // Check if the service is still in use by this bundle
      OSGiServiceState serviceState = ((OSGiServiceReferenceWrapper)reference).getServiceState();
      if (isServiceInUse(serviceState) == false)
         return false;

      checkValidBundleContext();

      return getBundleManager().ungetService(this, reference);
   }

   boolean ungetService(OSGiServiceState state)
   {
      return getBundleManager().ungetService(this, state);
   }

   public void addBundleListener(BundleListener listener)
   {
      checkValidBundleContext();

      if (listener instanceof SynchronousBundleListener)
         checkAdminPermission(AdminPermission.LISTENER);

      FrameworkEventsPlugin plugin = getBundleManager().getPlugin(FrameworkEventsPlugin.class);
      plugin.addBundleListener(this, listener);
   }

   public void removeBundleListener(BundleListener listener)
   {
      checkValidBundleContext();

      if (listener instanceof SynchronousBundleListener)
         checkAdminPermission(AdminPermission.LISTENER);

      FrameworkEventsPlugin plugin = getBundleManager().getPlugin(FrameworkEventsPlugin.class);
      plugin.removeBundleListener(this, listener);
   }

   public void start() throws BundleException
   {
      start(0);
   }

   public void stop() throws BundleException
   {
      stop(0);
   }

   public void update() throws BundleException
   {
      checkAdminPermission(AdminPermission.LIFECYCLE); // [TODO] extension bundles
      // [TODO] update
      throw new UnsupportedOperationException("update");
   }

   void uninstallInternal()
   {
      changeState(Bundle.UNINSTALLED);

      FrameworkEventsPlugin plugin = getBundleManager().getPlugin(FrameworkEventsPlugin.class);
      plugin.removeFrameworkListeners(this);
      plugin.removeBundleListeners(this);
      plugin.removeServiceListeners(this);
   }

   public void addFrameworkListener(FrameworkListener listener)
   {
      checkValidBundleContext();

      FrameworkEventsPlugin plugin = getBundleManager().getPlugin(FrameworkEventsPlugin.class);
      plugin.addFrameworkListener(this, listener);
   }

   public void removeFrameworkListener(FrameworkListener listener)
   {
      checkValidBundleContext();

      FrameworkEventsPlugin plugin = getBundleManager().getPlugin(FrameworkEventsPlugin.class);
      plugin.removeFrameworkListener(this, listener);
   }

   public Bundle installBundle(String location) throws BundleException
   {
      return installBundle(location, null);
   }

   public Bundle installBundle(String location, InputStream input) throws BundleException
   {
      checkValidBundleContext();
      checkAdminPermission(AdminPermission.LIFECYCLE); // [TODO] extension bundles

      AbstractBundleState bundleState = getBundleManager().installBundle(location, input);
      return bundleState.getBundleInternal();
   }

   public Bundle installBundle(VirtualFile root) throws BundleException
   {
      checkValidBundleContext();
      checkAdminPermission(AdminPermission.LIFECYCLE); // [TODO] extension bundles

      AbstractBundleState bundleState = getBundleManager().installBundle(root);
      return bundleState.getBundleInternal();
   }

   @Override
   public String toString()
   {
      return "Bundle{" + getCanonicalName() + "}";
   }

   /**
    * Get the canonical name of the bundle
    * 
    * @return the canonical name
    */
   public String getCanonicalName()
   {
      return getSymbolicName() + "-" + getVersion();
   }

   /**
    * Set the bundle manager
    * 
    * @param bundleManager the bundle manager or null to uninstall the bundle
    */
   void setBundleManager(OSGiBundleManager bundleManager)
   {
      if (bundleManager != null && this.bundleManager != null)
         throw new IllegalStateException("Bundle " + this + " is already installed");

      this.bundleManager = bundleManager;
   }

   /**
    * Get the source of a class for ServiceReference.isAssignable()
    * 
    * @param className the class name
    * @return the source or null if no source
    */
   Object getSource(String className)
   {
      // [TODO] some more efficient way than using the class?
      try
      {
         return loadClass(className);
      }
      catch (ClassNotFoundException e)
      {
         return null;
      }
   }

   /**
    * Change the state of the bundle
    * 
    * @param state the new state
    */
   public void changeState(int state)
   {
      int previous = getState();
      
      // Get the corresponding bundle event type
      int bundleEventType;
      switch (state)
      {
         case Bundle.STARTING:
            bundleEventType = BundleEvent.STARTING;
            break;
         case Bundle.ACTIVE:
            bundleEventType = BundleEvent.STARTED;
            break;
         case Bundle.STOPPING:
            bundleEventType = BundleEvent.STOPPING;
            break;
         case Bundle.UNINSTALLED:
            bundleEventType = BundleEvent.UNINSTALLED;
            break;
         case Bundle.INSTALLED:
         {
            if (previous == Bundle.RESOLVED)
               bundleEventType = BundleEvent.UNRESOLVED;
            else
               bundleEventType = BundleEvent.INSTALLED;
            break;
         }
         case Bundle.RESOLVED:
         {
            if (previous == Bundle.STOPPING)
               bundleEventType = BundleEvent.STOPPED;
            else
               bundleEventType = BundleEvent.RESOLVED;
            break;
         }
         default:
            throw new IllegalArgumentException("Unknown bundle state: " + state);
      }
      
      // Invoke the bundle lifecycle interceptors
      if (getBundleManager().isFrameworkActive() && getBundleId() != 0)
      {
         LifecycleInterceptorServicePlugin plugin = getBundleManager().getPlugin(LifecycleInterceptorServicePlugin.class);
         plugin.handleStateChange(state, getBundleInternal());
      }
      
      this.state.set(state);
      log.debug(this + " change state=" + ConstantsHelper.bundleState(state));

      // Fire the bundle event
      if (getBundleManager().isFrameworkActive())
      {
         FrameworkEventsPlugin plugin = getBundleManager().getPlugin(FrameworkEventsPlugin.class);
         plugin.fireBundleEvent(this, bundleEventType);
      }
   }

   /**
    * Check the bundle is installed
    * 
    * @throws IllegalStateException when the bundle is not installed
    */
   protected void checkInstalled()
   {
      if ((getState() & Bundle.UNINSTALLED) != 0)
         throw new IllegalStateException("Bundle " + getCanonicalName() + " is not installed");
   }

   /**
    * Check a bundle context is still valid
    * 
    * @return the bundle context
    * @throws IllegalArgumentException when the context is no longer valid
    */
   protected synchronized BundleContext checkValidBundleContext()
   {
      BundleContext result = this.bundleContext;
      if (result == null)
         throw new IllegalStateException("Bundle context is no longer valid: " + getCanonicalName());
      return result;
   }

   /**
    * Check the admin permission
    * 
    * @param what what permission to check
    * @throws SecurityException when the caller does not have the AdminPermission and a security manager is installed
    */
   protected void checkAdminPermission(String what)
   {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null)
         sm.checkPermission(new AdminPermission(this, what));
   }

   /**
    * Checks if we have the admin permission
    * 
    * @param what the permission to check
    * @return true if the caller doesn't have the permission
    */
   protected boolean noAdminPermission(String what)
   {
      try
      {
         checkAdminPermission(what);
         return false;
      }
      catch (SecurityException e)
      {
         return true;
      }
   }

   public static AbstractBundleState assertBundleState(Bundle bundle)
   {
      if (bundle == null)
         throw new IllegalArgumentException("Null bundle");
      
      if (bundle instanceof OSGiBundleWrapper)
         bundle = ((OSGiBundleWrapper)bundle).getBundleState();
   
      if (bundle instanceof AbstractBundleState == false)
         throw new IllegalArgumentException("Not an AbstractBundleState: " + bundle);
   
      return (AbstractBundleState)bundle;
   }
}
