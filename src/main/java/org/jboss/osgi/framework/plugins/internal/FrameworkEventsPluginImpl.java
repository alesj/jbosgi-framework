/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.osgi.framework.plugins.internal;

//$Id: SystemPackagesPluginImpl.java 92858 2009-08-27 10:58:32Z thomas.diesler@jboss.com $

import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jboss.logging.Logger;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.bundle.OSGiServiceState;
import org.jboss.osgi.framework.plugins.FrameworkEventsPlugin;
import org.jboss.osgi.framework.util.NoFilter;
import org.jboss.osgi.spi.util.ConstantsHelper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;

/**
 * A plugin that installs/starts bundles on framework startup.
 * 
 * @author thomas.diesler@jboss.com
 * @since 18-Aug-2009
 */
public class FrameworkEventsPluginImpl extends AbstractPlugin implements FrameworkEventsPlugin
{
   // Provide logging
   final Logger log = Logger.getLogger(FrameworkEventsPluginImpl.class);

   /** The active state of this plugin */
   private boolean active;
   /** The bundle listeners */
   private final Map<Bundle, List<BundleListener>> bundleListeners = new ConcurrentHashMap<Bundle, List<BundleListener>>();
   /** The framework listeners */
   private final Map<Bundle, List<FrameworkListener>> frameworkListeners = new ConcurrentHashMap<Bundle, List<FrameworkListener>>();
   /** The service listeners */
   private final Map<Bundle, List<ServiceListenerRegistration>> serviceListeners = new ConcurrentHashMap<Bundle, List<ServiceListenerRegistration>>();

   /** The executor service */
   private ExecutorService executorService;
   /** True for synchronous event delivery */
   private boolean synchronous;
   /** The set of bundle events taht cause an info log */
   private Set<Integer> infoEvents = new HashSet<Integer>();
   
   public FrameworkEventsPluginImpl(OSGiBundleManager bundleManager)
   {
      super(bundleManager);
      executorService = Executors.newCachedThreadPool();
      infoEvents.add(new Integer(BundleEvent.INSTALLED));
      infoEvents.add(new Integer(BundleEvent.STARTED));
      infoEvents.add(new Integer(BundleEvent.STOPPED));
      infoEvents.add(new Integer(BundleEvent.UNINSTALLED));
   }

   public void setSynchronous(boolean synchronous)
   {
      this.synchronous = synchronous;
   }

   public boolean isActive()
   {
      return active;
   }

   public void setActive(boolean active)
   {
      this.active = active;
   }

   public void addBundleListener(Bundle bundle, BundleListener listener)
   {
      if (listener == null)
         throw new IllegalArgumentException("Null listener");

      bundle = assertBundle(bundle);

      synchronized (bundleListeners)
      {
         List<BundleListener> listeners = bundleListeners.get(bundle);
         if (listeners == null)
         {
            listeners = new CopyOnWriteArrayList<BundleListener>();
            bundleListeners.put(bundle, listeners);
         }
         if (listeners.contains(listener) == false)
            listeners.add(listener);
      }
   }

   public void removeBundleListener(Bundle bundle, BundleListener listener)
   {
      if (listener == null)
         throw new IllegalArgumentException("Null listener");

      bundle = assertBundle(bundle);

      synchronized (bundleListeners)
      {
         List<BundleListener> listeners = bundleListeners.get(bundle);
         if (listeners != null)
         {
            if (listeners.size() > 1)
               listeners.remove(listener);
            else
               removeBundleListeners(bundle);
         }
      }
   }

   public void removeBundleListeners(Bundle bundle)
   {
      synchronized (bundleListeners)
      {
         bundle = assertBundle(bundle);
         bundleListeners.remove(bundle);
      }
   }

   public void addFrameworkListener(Bundle bundle, FrameworkListener listener)
   {
      if (listener == null)
         throw new IllegalArgumentException("Null listener");

      bundle = assertBundle(bundle);

      synchronized (frameworkListeners)
      {
         List<FrameworkListener> listeners = frameworkListeners.get(bundle);
         if (listeners == null)
         {
            listeners = new CopyOnWriteArrayList<FrameworkListener>();
            frameworkListeners.put(bundle, listeners);
         }
         if (listeners.contains(listener) == false)
            listeners.add(listener);
      }
   }

   public void removeFrameworkListener(Bundle bundle, FrameworkListener listener)
   {
      if (listener == null)
         throw new IllegalArgumentException("Null listener");

      bundle = assertBundle(bundle);

      synchronized (frameworkListeners)
      {
         List<FrameworkListener> listeners = frameworkListeners.get(bundle);
         if (listeners != null)
         {
            if (listeners.size() > 1)
               listeners.remove(listener);
            else
               removeFrameworkListeners(bundle);
         }
      }
   }

   public void removeFrameworkListeners(Bundle bundle)
   {
      synchronized (frameworkListeners)
      {
         bundle = assertBundle(bundle);
         frameworkListeners.remove(bundle);
      }
   }

   public void addServiceListener(Bundle bundle, ServiceListener listener, Filter filter)
   {
      if (listener == null)
         throw new IllegalArgumentException("Null listener");

      bundle = assertBundle(bundle);

      synchronized (serviceListeners)
      {
         List<ServiceListenerRegistration> listeners = serviceListeners.get(bundle);
         if (listeners == null)
         {
            listeners = new CopyOnWriteArrayList<ServiceListenerRegistration>();
            serviceListeners.put(bundle, listeners);
         }

         ServiceListenerRegistration registration = new ServiceListenerRegistration(listener, filter);
         if (listeners.contains(registration) == false)
            listeners.add(registration);
      }
   }

   public void removeServiceListener(Bundle bundle, ServiceListener listener)
   {
      if (listener == null)
         throw new IllegalArgumentException("Null listener");

      bundle = assertBundle(bundle);

      synchronized (serviceListeners)
      {
         List<ServiceListenerRegistration> listeners = serviceListeners.get(bundle);
         if (listeners != null)
         {
            if (listeners.size() > 1)
               listeners.remove(listener);
            else
               removeServiceListeners(bundle);
         }
      }
   }

   public void removeServiceListeners(Bundle bundle)
   {
      synchronized (serviceListeners)
      {
         bundle = assertBundle(bundle);
         serviceListeners.remove(bundle);
      }
   }

   public void fireBundleEvent(final Bundle bundle, final int type)
   {
      // Get a snapshot of the current listeners
      final List<BundleListener> listeners = new ArrayList<BundleListener>();
      synchronized (bundleListeners)
      {
         for (Entry<Bundle, List<BundleListener>> entry : bundleListeners.entrySet())
         {
            for (BundleListener listener : entry.getValue())
            {
               listeners.add(listener);
            }
         }
      }

      // Expose the bundl wrapper not the state itself
      final BundleEvent event = new OSGiBundleEvent(type, assertBundle(bundle));
      final String typeName = ConstantsHelper.bundleEvent(event.getType());

      if (infoEvents.contains(event.getType()))
         log.info("Bundle " + typeName + ": " + bundle);
      else
         log.debug("Bundle " + typeName + ": " + bundle);

      // Nobody is interested
      if (listeners.isEmpty())
         return;

      // Are we active?
      if (getBundleManager().isFrameworkActive() == false)
         return;

      Runnable runnable = new Runnable()
      {
         public void run()
         {
            // Synchronous listeners first
            for (BundleListener listener : listeners)
            {
               try
               {
                  if (listener instanceof SynchronousBundleListener)
                     listener.bundleChanged(event);
               }
               catch (Throwable t)
               {
                  log.warn("Error while firing " + typeName + " for bundle " + bundle, t);
               }
            }

            // Normal listeners after, if required
            if (type != BundleEvent.STARTING && type != BundleEvent.STOPPING && type != BundleEvent.LAZY_ACTIVATION)
            {
               for (BundleListener listener : listeners)
               {
                  try
                  {
                     if (listener instanceof SynchronousBundleListener == false)
                        listener.bundleChanged(event);
                  }
                  catch (Throwable t)
                  {
                     log.warn("Error while firing " + typeName + " for bundle " + this, t);
                  }
               }
            }
         }
      };

      // Fire the event in a runnable
      fireEvent(runnable, synchronous);
   }

   public void fireFrameworkEvent(final Bundle bundle, final int type, final Throwable throwable)
   {
      // Get a snapshot of the current listeners
      final ArrayList<FrameworkListener> listeners = new ArrayList<FrameworkListener>();
      synchronized (frameworkListeners)
      {
         for (Entry<Bundle, List<FrameworkListener>> entry : frameworkListeners.entrySet())
         {
            for (FrameworkListener listener : entry.getValue())
            {
               listeners.add(listener);
            }
         }
      }

      // Nobody is interested
      if (listeners.isEmpty())
         return;

      // Are we active?
      if (getBundleManager().isFrameworkActive() == false)
         return;

      Runnable runnable = new Runnable()
      {
         public void run()
         {
            // Expose the wrapper not the state itself
            FrameworkEvent event = new OSGiFrameworkEvent(type, assertBundle(bundle), throwable);
            String typeName = ConstantsHelper.frameworkEvent(event.getType());

            log.info("Framwork " + typeName);

            // Nobody is interested
            if (frameworkListeners.isEmpty())
               return;

            // Are we active?
            if (getBundleManager().isFrameworkActive() == false)
               return;

            // Call the listeners
            for (FrameworkListener listener : listeners)
            {
               try
               {
                  listener.frameworkEvent(event);
               }
               catch (RuntimeException ex)
               {
                  log.warn("Error while firing " + typeName + " for framework", ex);
                  
                  // The Framework must publish a FrameworkEvent.ERROR if a callback to an
                  // event listener generates an unchecked exception - except when the callback
                  // happens while delivering a FrameworkEvent.ERROR
                  if (type != FrameworkEvent.ERROR)
                  {
                     fireFrameworkEvent(bundle, FrameworkEvent.ERROR, ex);
                  }
               }
               catch (Throwable t)
               {
                  log.warn("Error while firing " + typeName + " for framework", t);
               }
            }
         }
      };

      // Fire the event in a runnable
      fireEvent(runnable, synchronous);
   }

   public void fireServiceEvent(Bundle bundle, int type, final OSGiServiceState service)
   {
      // Get a snapshot of the current listeners
      final ArrayList<ServiceListenerRegistration> listeners = new ArrayList<ServiceListenerRegistration>();
      synchronized (serviceListeners)
      {
         for (Entry<Bundle, List<ServiceListenerRegistration>> entry : serviceListeners.entrySet())
         {
            for (ServiceListenerRegistration listener : entry.getValue())
            {
               listeners.add(listener);
            }
         }
      }

      // Expose the wrapper not the state itself
      final ServiceEvent event = new OSGiServiceEvent(type, service.getReferenceInternal());
      final String typeName = ConstantsHelper.serviceEvent(event.getType());

      log.info("Service " + typeName + ": " + service);

      // Nobody is interested
      if (listeners.isEmpty())
         return;

      // Are we active?
      if (getBundleManager().isFrameworkActive() == false)
         return;

      Runnable runnable = new Runnable()
      {
         public void run()
         {
            // Call the listeners
            for (ServiceListenerRegistration registration : listeners)
            {
               try
               {
                  if (registration.filter.match(service))
                  {
                     AccessControlContext accessControlContext = registration.accessControlContext;
                     if (accessControlContext == null || service.hasPermission(accessControlContext))
                        registration.listener.serviceChanged(event);
                  }
               }
               catch (Throwable t)
               {
                  log.warn("Error while firing " + typeName + " for service " + service, t);
               }
            }
         }
      };

      // Fire the event in a runnable
      fireEvent(runnable, synchronous);
   }

   private Bundle assertBundle(Bundle bundle)
   {
      if (bundle == null)
         throw new IllegalArgumentException("Null bundle");

      // Expose the wrapper not the state itself
      if (bundle instanceof AbstractBundleState)
         bundle = ((AbstractBundleState)bundle).getBundleInternal();

      return bundle;
   }

   private void fireEvent(Runnable runnable, boolean synchronous)
   {
      if (synchronous)
      {
         runnable.run();
      }
      else
      {
         executorService.execute(runnable);
      }
   }

   /**
    * Filter and AccessControl for service events
    */
   static class ServiceListenerRegistration
   {
      // Any filter
      Filter filter;
      ServiceListener listener;

      // Any access control context
      AccessControlContext accessControlContext;

      /**
       * Create a new ServiceListenerRegistration.
       *
       * @param listener service listener
       * @param filter the filter
       */
      public ServiceListenerRegistration(ServiceListener listener, Filter filter)
      {
         if (listener == null)
            throw new IllegalArgumentException("Null listener");

         if (filter == null)
            filter = NoFilter.INSTANCE;

         this.listener = listener;
         this.filter = filter;

         if (System.getSecurityManager() != null)
            accessControlContext = AccessController.getContext();
      }

      @Override
      public int hashCode()
      {
         return listener.hashCode();
      }

      @Override
      public boolean equals(Object obj)
      {
         if (obj instanceof ServiceListenerRegistration == false)
            return false;

         ServiceListenerRegistration other = (ServiceListenerRegistration)obj;
         return other.listener.equals(listener) && other.filter.equals(filter);
      }
   }

   static class OSGiFrameworkEvent extends FrameworkEvent
   {
      private static final long serialVersionUID = 6505331543651318189L;

      public OSGiFrameworkEvent(int type, Bundle bundle, Throwable throwable)
      {
         super(type, bundle, throwable);
      }

      @Override
      public String toString()
      {
         return "FrameworkEvent[type=" + ConstantsHelper.frameworkEvent(getType()) + ",source=" + getSource() + "]";
      }
   }

   static class OSGiBundleEvent extends BundleEvent
   {
      private static final long serialVersionUID = -2705304702665185935L;

      public OSGiBundleEvent(int type, Bundle bundle)
      {
         super(type, bundle);
      }

      @Override
      public String toString()
      {
         return "BundleEvent[type=" + ConstantsHelper.bundleEvent(getType()) + ",source=" + getSource() + "]";
      }
   }

   static class OSGiServiceEvent extends ServiceEvent
   {
      private static final long serialVersionUID = 62018288275708239L;

      public OSGiServiceEvent(int type, ServiceReference reference)
      {
         super(type, reference);
      }

      @Override
      public String toString()
      {
         return "ServiceEvent[type=" + ConstantsHelper.serviceEvent(getType()) + ",source=" + getSource() + "]";
      }
   }
}