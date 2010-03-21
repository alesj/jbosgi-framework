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
package org.jboss.osgi.framework.bundle;

//$Id$

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.dependency.spi.Controller;
import org.jboss.dependency.spi.ControllerContext;
import org.jboss.dependency.spi.ControllerState;
import org.jboss.dependency.spi.tracker.ContextTracker;
import org.jboss.dependency.spi.tracker.ContextTracking;
import org.jboss.deployers.structure.spi.DeploymentRegistry;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.kernel.Kernel;
import org.jboss.kernel.spi.dependency.KernelController;
import org.jboss.kernel.spi.qualifier.QualifierMatchers;
import org.jboss.logging.Logger;
import org.jboss.metadata.plugins.loader.memory.MemoryMetaDataLoader;
import org.jboss.metadata.spi.MutableMetaData;
import org.jboss.metadata.spi.repository.MutableMetaDataRepository;
import org.jboss.metadata.spi.retrieval.MetaDataRetrieval;
import org.jboss.metadata.spi.retrieval.MetaDataRetrievalFactory;
import org.jboss.metadata.spi.scope.CommonLevels;
import org.jboss.metadata.spi.scope.ScopeKey;
import org.jboss.osgi.framework.plugins.ControllerContextPlugin;
import org.jboss.osgi.framework.plugins.FrameworkEventsPlugin;
import org.jboss.osgi.framework.plugins.ServiceManagerPlugin;
import org.jboss.osgi.framework.plugins.internal.AbstractPlugin;
import org.jboss.osgi.framework.util.KernelUtils;
import org.jboss.osgi.framework.util.NoFilter;
import org.osgi.framework.Bundle;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.service.ListenerHook;
import org.osgi.framework.hooks.service.ListenerHook.ListenerInfo;

/**
 * A plugin that manages OSGi services.
 * 
 * This implementation handles service integration with the MC.
 * 
 * [JBOSGI-141] Service integration with MC
 * 
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 * @author thomas.diesler@jboss.com
 * @since 20-Jan-2010
 */
public class ServiceManagerPluginImpl extends AbstractPlugin implements ServiceManagerPlugin
{
   // Provide logging
   final Logger log = Logger.getLogger(ServiceManagerPluginImpl.class);

   /** The kernel */
   private Kernel kernel;
   /** The previous context tracker */
   private ContextTracker previousTracker;
   /** The deployment registry */
   private DeploymentRegistry registry;

   /** Enable MDR usage */
   private boolean enableMDRUsage = true;
   
   public ServiceManagerPluginImpl(OSGiBundleManager bundleManager, DeploymentRegistry registry)
   {
      super(bundleManager);
      
      if (registry == null)
         throw new IllegalArgumentException("Null deployment registry");

      this.registry = registry;
   }

   public void setEnableMDRUsage(boolean mdrUsage)
   {
      this.enableMDRUsage = mdrUsage;
   }

   public void start()
   {
      kernel = getBundleManager().getKernel();
      if (enableMDRUsage == true)
         applyMDRUsage(true);
   }

   public void stop()
   {
      if (enableMDRUsage == true)
         applyMDRUsage(false);
   }

   @Override
   public ServiceReference[] getRegisteredServices(AbstractBundleState bundleState)
   {
      Set<ControllerContext> contexts = getRegisteredContexts(bundleState);
      if (contexts.isEmpty())
         return null;

      Set<ServiceReference> result = new HashSet<ServiceReference>();
      for (ControllerContext context : contexts)
      {
         ServiceReference ref = getServiceReferenceForContext(context);
         if (ref != null)
            result.add(ref);
      }
      if (result.isEmpty())
         return null;
      
      return result.toArray(new ServiceReference[result.size()]);
   }

   @Override
   public ServiceReference[] getServicesInUse(AbstractBundleState bundleState)
   {
      Set<ControllerContext> contexts = bundleState.getUsedContexts(bundleState);
      if (contexts == null || contexts.isEmpty())
         return null;

      List<ServiceReference> references = new ArrayList<ServiceReference>();
      for (ControllerContext context : contexts)
      {
         ServiceReference ref = getServiceReferenceForContext(context);
         if (ref != null)
            references.add(ref);
      }

      if (references.isEmpty())
         return null;
      
      return references.toArray(new ServiceReference[references.size()]);
   }

   @Override
   public ServiceReference[] getAllServiceReferences(AbstractBundleState bundle, String clazz, String filterStr) throws InvalidSyntaxException
   {
      Filter filter = NoFilter.INSTANCE;
      if (filterStr != null)
         filter = FrameworkUtil.createFilter(filterStr);

      Collection<ServiceReference> services = getServices(bundle, clazz, filter, false);
      if (services == null || services.isEmpty())
         return null;

      return services.toArray(new ServiceReference[services.size()]);
   }

   @Override
   public Set<Bundle> getUsingBundles(OSGiServiceState serviceState)
   {
      AbstractBundleState bundleState = serviceState.getBundleState();
      OSGiBundleManager manager = bundleState.getBundleManager();
      ControllerContextPlugin plugin = manager.getPlugin(ControllerContextPlugin.class);

      ContextTracker contextTracker = serviceState.getContextTracker();
      Set<Object> users = contextTracker.getUsers(serviceState);
      Set<Bundle> bundles = new HashSet<Bundle>();
      for (Object user : users)
      {
         AbstractBundleState abs = plugin.getBundleForUser(user);
         bundles.add(abs.getBundleInternal());
      }
      return bundles;
   }
   
   @Override
   public Object getService(AbstractBundleState bundleState, ServiceReference reference)
   {
      if (reference == null)
         throw new IllegalArgumentException("Null reference");
      
      ControllerContextHandle handle = (ControllerContextHandle)reference;
      ControllerContext context = handle.getContext();
      if (KernelUtils.isUnregistered(context)) // we're probably not installed anymore
         return null;

      ContextTracking ct = (ContextTracking)context;
      Object target = ct.getTarget(bundleState);
      return target;
   }

   @Override
   public ServiceReference getServiceReference(AbstractBundleState bundle, String clazz)
   {
      if (clazz == null)
         throw new IllegalArgumentException("Null clazz");
      
      Collection<ServiceReference> services = getServices(bundle, clazz, null, true);
      if (services == null || services.isEmpty())
         return null;

      return services.iterator().next();
   }

   @Override
   public ServiceReference[] getServiceReferences(AbstractBundleState bundle, String clazz, String filterStr) throws InvalidSyntaxException
   {
      Filter filter = NoFilter.INSTANCE;
      if (filterStr != null)
         filter = FrameworkUtil.createFilter(filterStr);

      Collection<ServiceReference> services = getServices(bundle, clazz, filter, true);
      if (services == null || services.isEmpty())
         return null;

      return services.toArray(new ServiceReference[services.size()]);
   }

   @Override
   @SuppressWarnings("rawtypes")
   public OSGiServiceState registerService(AbstractBundleState bundleState, String[] clazzes, Object service, Dictionary properties)
   {
      // Immediately after registration of a {@link ListenerHook}, the ListenerHook.added() method will be called 
      // to provide the current collection of service listeners which had been added prior to the hook being registered.
      Collection<ListenerInfo> listenerInfos = null;
      if (service instanceof ListenerHook)
      {
         FrameworkEventsPlugin eventsPlugin = getPlugin(FrameworkEventsPlugin.class);
         listenerInfos = eventsPlugin.getServiceListenerInfos(null);
      }
      
      OSGiServiceState result = new OSGiServiceState(bundleState, clazzes, service, properties);
      result.internalRegister();
      try
      {
         Controller controller = kernel.getController();
         controller.install(result);
      }
      catch (Throwable t)
      {
         getBundleManager().fireError(bundleState, "installing service to MC in", t);
         throw new RuntimeException(t);
      }

      if (bundleState instanceof OSGiBundleState)
      {
         putContext(result, ((OSGiBundleState)bundleState).getDeploymentUnit());
      }

      // Call the newly added ListenerHook.added() method
      if (service instanceof ListenerHook)
      {
         ListenerHook listenerHook = (ListenerHook)service;
         listenerHook.added(listenerInfos);
      }
      
      // This event is synchronously delivered after the service has been registered with the Framework. 
      FrameworkEventsPlugin eventsPlugin = getPlugin(FrameworkEventsPlugin.class);
      eventsPlugin.fireServiceEvent(bundleState, ServiceEvent.REGISTERED, result);
      
      return result;
   }

   @Override
   public void unregisterService(OSGiServiceState serviceState)
   {
      AbstractBundleState bundleState = serviceState.getBundleState();
      
      // This event is synchronously delivered before the service has completed unregistering. 
      FrameworkEventsPlugin plugin = getPlugin(FrameworkEventsPlugin.class);
      plugin.fireServiceEvent(bundleState, ServiceEvent.UNREGISTERING, serviceState);
      
      internalUnregister(serviceState);
      if (bundleState instanceof OSGiBundleState)
         removeContext(serviceState, ((OSGiBundleState)bundleState).getDeploymentUnit());
      
      Controller controller = kernel.getController();
      controller.uninstall(serviceState.getName());
   }

   private void internalUnregister(OSGiServiceState serviceState)
   {
      AbstractBundleState bundleState = serviceState.getBundleState();
      ContextTracker ct = serviceState.getContextTracker();
      if (ct != null) // nobody used us?
      {
         Set<Object> users = ct.getUsers(serviceState);
         if (users.isEmpty() == false)
         {
            Set<AbstractBundleState> used = new HashSet<AbstractBundleState>();
            OSGiBundleManager manager = bundleState.getBundleManager();
            ControllerContextPlugin plugin = manager.getPlugin(ControllerContextPlugin.class);
            
            for (Object user : users)
            {
               AbstractBundleState using = plugin.getBundleForUser(user);
               if (used.add(using) == true) 
               {
                  // ungetService will cleanup service cache
                  int count = ct.getUsedByCount(serviceState, using);
                  while (count > 0)
                  {
                     using.ungetContext(serviceState); 
                     count--;
                  }
               }
            }
         }
      }
      serviceState.clearTarget();
   }
   
   @Override
   public boolean ungetService(AbstractBundleState bundleState, ServiceReference reference)
   {
      if (reference == null)
         throw new IllegalArgumentException("Null reference");

      ControllerContextHandle serviceReference = (ControllerContextHandle)reference;
      ControllerContext context = serviceReference.getContext();
      if (KernelUtils.isUnregistered(context))
         return false;
      
      return bundleState.removeContextInUse(context);
   }

   @Override
   public void unregisterServices(AbstractBundleState bundleState)
   {
      unregisterContexts(bundleState);
   }

   /**
    * Apply OSGi's MDR usage:
    * - add/remove system bundle as default context tracker
    * - add/remove instance metadata retrieval factory 
    *
    * @param register do we register or unregister
    */
   private void applyMDRUsage(boolean register)
   {
      MutableMetaDataRepository repository = kernel.getMetaDataRepository().getMetaDataRepository();
      MetaDataRetrieval retrieval = repository.getMetaDataRetrieval(ScopeKey.DEFAULT_SCOPE);
      if (register && retrieval == null)
      {
         retrieval = new MemoryMetaDataLoader(ScopeKey.DEFAULT_SCOPE);
         repository.addMetaDataRetrieval(retrieval);
      }
      if (retrieval != null && retrieval instanceof MutableMetaData)
      {
         MutableMetaData mmd = (MutableMetaData)retrieval;
         if (register)
         {
            OSGiSystemState systemBundle = getBundleManager().getSystemBundle();
            previousTracker = mmd.addMetaData(systemBundle, ContextTracker.class);
         }
         else
         {
            if (previousTracker == null)
            {
               mmd.removeMetaData(ContextTracker.class);
               if (retrieval.isEmpty())
                  repository.removeMetaDataRetrieval(retrieval.getScope());
            }
            else
            {
               mmd.addMetaData(previousTracker, ContextTracker.class);
            }
         }
      }

      // osgi ldap filter parsing and matching
      FilterParserAndMatcher fpm = FilterParserAndMatcher.INSTANCE;
      QualifierMatchers matchers = QualifierMatchers.getInstance();

      if (register)
      {
         matchers.addParser(fpm);
         matchers.addMatcher(fpm);

         MetaDataRetrievalFactory mdrFactory = getMetaDataRetrievalFactory();
         repository.addMetaDataRetrievalFactory(CommonLevels.INSTANCE, mdrFactory);
      }
      else
      {
         repository.removeMetaDataRetrievalFactory(CommonLevels.INSTANCE);

         matchers.removeParser(fpm.getHandledContent());
         matchers.removeMatcher(fpm.getHandledType());
      }
   }

   private MetaDataRetrievalFactory getMetaDataRetrievalFactory()
   {
      MetaDataRetrievalFactory mdrFactory;
      InstanceMetaDataRetrievalFactory imdrf = new InstanceMetaDataRetrievalFactory(kernel);
      imdrf.addFactory(new OSGiServiceStateDictionaryFactory());
      imdrf.addFactory(new KernelDictionaryFactory(kernel.getConfigurator()));
      // TODO - JMX?
      mdrFactory = imdrf;
      return mdrFactory;
   }

   private Collection<ServiceReference> getServices(AbstractBundleState bundle, String clazz, Filter filter, boolean checkAssignable)
   {
      Set<ControllerContext> contexts;
      KernelController controller = kernel.getController();

      // Don't check assignabilty for the system bundle
      boolean isSystemBundle = (bundle.getBundleId() == 0);
      if (isSystemBundle)
         checkAssignable = false;

      // TODO - a bit slow for system bundle
      if (clazz != null && isSystemBundle == false)
      {
         Class<?> type = getBundleManager().loadClassFailsafe(bundle, clazz);
         if (type == null)
            return null; // or check all?

         contexts = controller.getContexts(type, ControllerState.INSTALLED);
      }
      else
      {
         contexts = controller.getContextsByState(ControllerState.INSTALLED);
      }

      if (contexts == null || contexts.isEmpty())
         return null;

      if (filter == null)
         filter = NoFilter.INSTANCE;

      List<ControllerContext> sorted = new ArrayList<ControllerContext>(contexts);
      Collections.sort(sorted, ContextComparator.INSTANCE); // Sort by the spec, should bubble up
      Collection<ServiceReference> result = new ArrayList<ServiceReference>();
      for (ControllerContext context : sorted)
      {
         // re-check?? -- we already only get INSTALLED 
         if (KernelUtils.isUnregistered(context) == false)
         {
            ServiceReference ref = getServiceReferenceForContext(context);
            if (filter.match(ref) && hasPermission(context))
            {
               if (clazz == null || isSystemBundle == false || MDRUtils.matchClass(context, clazz))
               {
                  // Check the assignability
                  if (checkAssignable == false || MDRUtils.isAssignableTo(context, bundle))
                     result.add(ref);
               }
            }
         }
      }
      return result;
   }

   /**
    * Get service reference for context.
    *
    * @param context the context
    * @return service reference
    */
   private ServiceReference getServiceReferenceForContext(ControllerContext context)
   {
      if (context instanceof OSGiServiceState)
      {
         OSGiServiceState service = (OSGiServiceState)context;
         return service.hasPermission() ? service.getReferenceInternal() : null;
      }

      OSGiBundleManager manager = getBundleManager();
      ControllerContextPlugin plugin = manager.getPlugin(ControllerContextPlugin.class);
      AbstractBundleState bundleState = plugin.getBundleForContext(context);
      return new GenericServiceReferenceWrapper(context, bundleState);
   }

   /**
    * Do we have a permission to use context.
    *
    * @param context the context
    * @return true if allowed to use context, false otherwise
    */
   private boolean hasPermission(ControllerContext context)
   {
      // TODO - make thisa generic, w/o casting
      if (context instanceof OSGiServiceState)
      {
         OSGiServiceState serviceState = (OSGiServiceState)context;
         return serviceState.hasPermission();
      }
      return true;
   }

   private DeploymentUnit putContext(ControllerContext context, DeploymentUnit unit)
   {
      return registry.putContext(context, unit);
   }

   private DeploymentUnit removeContext(ControllerContext context, DeploymentUnit unit)
   {
      return registry.removeContext(context, unit);
   }

   private Set<ControllerContext> getRegisteredContexts(AbstractBundleState bundleState)
   {
      if (bundleState instanceof OSGiBundleState == false)
         return Collections.emptySet();

      DeploymentUnit unit = ((OSGiBundleState)bundleState).getDeploymentUnit();
      return registry.getContexts(unit);
   }

   private void unregisterContexts(AbstractBundleState bundleState)
   {
      if (bundleState instanceof OSGiBundleState)
      {
         DeploymentUnit unit = ((OSGiBundleState)bundleState).getDeploymentUnit();
         Set<ControllerContext> contexts = registry.getContexts(unit);
         for (ControllerContext context : contexts)
         {
            if (context instanceof ServiceRegistration)
            {
               ServiceRegistration service = (ServiceRegistration)context;
               service.unregister();
            }
         }
      }
   }
}