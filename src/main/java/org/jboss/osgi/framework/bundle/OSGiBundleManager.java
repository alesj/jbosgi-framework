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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;

import org.jboss.dependency.spi.Controller;
import org.jboss.dependency.spi.ControllerContext;
import org.jboss.dependency.spi.ControllerState;
import org.jboss.dependency.spi.tracker.ContextTracker;
import org.jboss.deployers.client.spi.DeployerClient;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.attachments.MutableAttachments;
import org.jboss.deployers.spi.deployer.DeploymentStage;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.structure.spi.DeploymentRegistry;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.structure.spi.main.MainDeployerStructure;
import org.jboss.deployers.vfs.spi.client.VFSDeployment;
import org.jboss.deployers.vfs.spi.client.VFSDeploymentFactory;
import org.jboss.kernel.Kernel;
import org.jboss.kernel.spi.dependency.KernelController;
import org.jboss.logging.Logger;
import org.jboss.metadata.plugins.loader.memory.MemoryMetaDataLoader;
import org.jboss.metadata.spi.loader.MutableMetaDataLoader;
import org.jboss.metadata.spi.repository.MutableMetaDataRepository;
import org.jboss.metadata.spi.retrieval.MetaDataRetrieval;
import org.jboss.metadata.spi.retrieval.MetaDataRetrievalFactory;
import org.jboss.metadata.spi.scope.CommonLevels;
import org.jboss.metadata.spi.scope.ScopeKey;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.deployment.deployer.DeploymentFactory;
import org.jboss.osgi.framework.deployers.OSGiBundleActivatorDeployer;
import org.jboss.osgi.framework.metadata.OSGiMetaData;
import org.jboss.osgi.framework.metadata.PackageAttribute;
import org.jboss.osgi.framework.metadata.internal.AbstractOSGiMetaData;
import org.jboss.osgi.framework.plugins.AutoInstallPlugin;
import org.jboss.osgi.framework.plugins.BundleStoragePlugin;
import org.jboss.osgi.framework.plugins.FrameworkEventsPlugin;
import org.jboss.osgi.framework.plugins.PackageAdminPlugin;
import org.jboss.osgi.framework.plugins.Plugin;
import org.jboss.osgi.framework.plugins.ResolverPlugin;
import org.jboss.osgi.framework.plugins.ServicePlugin;
import org.jboss.osgi.framework.util.NoFilter;
import org.jboss.osgi.spi.util.BundleInfo;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.VirtualFile;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * OSGiBundleManager.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author Thomas.Diesler@jboss.com
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
public class OSGiBundleManager
{
   /** The log */
   private static final Logger log = Logger.getLogger(OSGiBundleManager.class);

   /** The bundle manager's bean name: OSGiBundleManager */
   public static final String BEAN_BUNDLE_MANAGER = "OSGiBundleManager";
   /** The framework version */
   private static String OSGi_FRAMEWORK_VERSION = "r4v42"; // [TODO] externalise
   /** The framework vendor */
   private static String OSGi_FRAMEWORK_VENDOR = "jboss.org"; // [TODO] externalise
   /** The framework language */
   private static String OSGi_FRAMEWORK_LANGUAGE = Locale.getDefault().getISO3Language(); // REVIEW correct?
   /** The os name */
   private static String OSGi_FRAMEWORK_OS_NAME;
   /** The os version */
   private static String OSGi_FRAMEWORK_OS_VERSION;
   /** The os version */
   private static String OSGi_FRAMEWORK_PROCESSOR;
   /** The bundles by id */
   private List<AbstractBundleState> bundles = new CopyOnWriteArrayList<AbstractBundleState>();
   /** The kernel */
   private Kernel kernel;
   /** The main deployer */
   private DeployerClient deployerClient;
   /** The deployment structure */
   private MainDeployerStructure deployerStructure;
   /** The deployment registry */
   private DeploymentRegistry registry;
   /** The instance metadata factory */
   private MetaDataRetrievalFactory factory;
   /** The executor */
   private Executor executor;
   /** The system bundle */
   private OSGiSystemState systemBundle;
   /** The registered manager plugins */
   private Map<Class<?>, Plugin> plugins = Collections.synchronizedMap(new LinkedHashMap<Class<?>, Plugin>());
   /** The frame work properties */
   private Map<String, Object> properties = new ConcurrentHashMap<String, Object>();
   /** The framework stop monitor*/
   private AtomicInteger stopMonitor = new AtomicInteger(0);

   static
   {
      AccessController.doPrivileged(new PrivilegedAction<Object>()
      {
         public Object run()
         {
            OSGi_FRAMEWORK_OS_NAME = System.getProperty("os.name");
            OSGi_FRAMEWORK_OS_VERSION = System.getProperty("os.version");
            OSGi_FRAMEWORK_PROCESSOR = System.getProperty("os.arch");

            System.setProperty("org.osgi.vendor.framework", "org.jboss.osgi.plugins.framework");
            return null;
         }
      });
   }

   /**
    * Create a new OSGiBundleManager.
    * 
    * @param kernel the kernel
    * @param deployerClient the deployer client
    * @param registry the deployment registry
    * @throws IllegalArgumentException for a null parameter
    */
   public OSGiBundleManager(Kernel kernel, DeployerClient deployerClient, DeploymentRegistry registry)
   {
      this(kernel, deployerClient, registry, null);
   }

   /**
    * Create a new OSGiBundleManager.
    * 
    * @param kernel the kernel
    * @param deployerClient the deployer client
    * @param registry the deployment registry
    * @param executor the executor
    * @throws IllegalArgumentException for a null parameter
    */
   public OSGiBundleManager(Kernel kernel, DeployerClient deployerClient, DeploymentRegistry registry, Executor executor)
   {
      if (kernel == null)
         throw new IllegalArgumentException("Null kernel");
      if (deployerClient == null)
         throw new IllegalArgumentException("Null deployerClient");
      if (deployerClient instanceof MainDeployerStructure == false)
         throw new IllegalArgumentException("Deployer client does not implement " + MainDeployerStructure.class.getName());
      if (registry == null)
         throw new IllegalArgumentException("Null deployment registry");

      this.kernel = kernel;
      this.deployerClient = deployerClient;
      this.deployerStructure = (MainDeployerStructure)deployerClient;
      this.registry = registry;

      // TODO thread factory
      if (executor == null)
         executor = Executors.newFixedThreadPool(10);

      this.executor = executor;
   }

   public void start()
   {
      // Create the system Bundle
      systemBundle = new OSGiSystemState();
      addBundle(systemBundle);

      applyMDRUsage(true);
   }

   public void stop()
   {
      applyMDRUsage(false);
   }

   /**
    * Apply OSGi's MDR usage:
    * - add/remove system bundle as default context tracker
    * - add/remove instance metadata retrieval factory 
    *
    * @param register do we register or unregister
    */
   protected void applyMDRUsage(boolean register)
   {
      MutableMetaDataRepository repository = kernel.getMetaDataRepository().getMetaDataRepository();
      MetaDataRetrieval retrieval = repository.getMetaDataRetrieval(ScopeKey.DEFAULT_SCOPE);
      if (register && retrieval == null)
      {
         retrieval = new MemoryMetaDataLoader(ScopeKey.DEFAULT_SCOPE);
         repository.addMetaDataRetrieval(retrieval);
      }
      if (retrieval != null && retrieval instanceof MutableMetaDataLoader)
      {
         MutableMetaDataLoader mmdl = (MutableMetaDataLoader)retrieval;
         if (register)
         {
            mmdl.addMetaData(systemBundle, ContextTracker.class);
         }
         else
         {
            mmdl.removeMetaData(ContextTracker.class);
            if (mmdl.isEmpty())
               repository.removeMetaDataRetrieval(mmdl.getScope());
         }
      }

      if (register)
      {
         MetaDataRetrievalFactory mdrFactory = factory;
         if (mdrFactory == null)
         {
            Controller controller = kernel.getController();
            InstanceMetaDataRetrievalFactory imdrf = new InstanceMetaDataRetrievalFactory(controller);
            imdrf.addFactory(new OSGiServiceStateDictionaryFactory());
            imdrf.addFactory(new KernelDictionaryFactory(kernel.getConfigurator()));
            // TODO - JMX?
            mdrFactory = imdrf;
         }
         repository.addMetaDataRetrievalFactory(CommonLevels.INSTANCE, mdrFactory);
      }
      else
      {
         repository.removeMetaDataRetrievalFactory(CommonLevels.INSTANCE);
      }
   }

   /**
    * Set instance metadata factory.
    *
    * @param factory the instance metadata factory
    */
   public void setInstanceMetaDataFactory(MetaDataRetrievalFactory factory)
   {
      this.factory = factory;
   }

   /**
    * Put context to deployment mapping.
    *
    * @param context the context
    * @param unit the deployment
    * @return previous mapping value
    */
   DeploymentUnit putContext(ControllerContext context, DeploymentUnit unit)
   {
      return registry.putContext(context, unit);
   }

   /**
    * Remove context to deployment mapping.
    *
    * @param context the context
    * @param unit the deployment
    * @return is previous mapping value same as unit param
    */
   DeploymentUnit removeContext(ControllerContext context, DeploymentUnit unit)
   {
      return registry.removeContext(context, unit);
   }

   /**
    * Get bundle for user tracker.
    *
    * @param user the user tracker object
    * @return bundle state
    */
   AbstractBundleState getBundleForUser(Object user)
   {
      if (user instanceof AbstractBundleState)
         return (AbstractBundleState)user;
      else if (user instanceof ControllerContext)
         return getBundleForContext((ControllerContext)user);
      else
         throw new IllegalArgumentException("Unknown tracker type: " + user);
   }

   /**
    * Get bundle for context.
    *
    * @param context the context
    * @return bundle state
    */
   AbstractBundleState getBundleForContext(ControllerContext context)
   {
      if (context instanceof OSGiServiceState)
      {
         OSGiServiceState service = (OSGiServiceState)context;
         return service.getBundleState();
      }

      DeploymentUnit unit = registry.getDeployment(context);
      if (unit != null)
      {
         synchronized (unit)
         {
            OSGiBundleState bundleState = unit.getAttachment(OSGiBundleState.class);
            if (bundleState == null)
            {
               try
               {
                  bundleState = addDeployment(unit);
                  bundleState.startInternal();
                  unit.addAttachment(OSGiBundleState.class, bundleState);
               }
               catch (Throwable t)
               {
                  throw new RuntimeException("Cannot dynamically add generic bundle: " + unit, t);
               }
            }
            return bundleState;
         }
      }

      return systemBundle;
   }

   /**
    * Get service reference for context.
    *
    * @param context the context
    * @return service reference
    */
   ServiceReference getServiceReferenceForContext(ControllerContext context)
   {
      if (context instanceof OSGiServiceState)
      {
         OSGiServiceState service = (OSGiServiceState)context;
         return service.hasPermission() ? service.getReferenceInternal() : null;
      }

      AbstractBundleState bundleState = getBundleForContext(context);
      return new GenericServiceReferenceWrapper(context, bundleState);
   }

   /**
    * Get the kernel
    *
    * @return the kernel
    */
   public Kernel getKernel()
   {
      return kernel;
   }

   /**
    * Get the deployerClient.
    *
    * @return the deployerClient.
    */
   public DeployerClient getDeployerClient()
   {
      return deployerClient;
   }

   /**
    * Set the framework properties
    *
    * @param properties the properties
    */
   public void setProperties(Map<String, Object> props)
   {
      properties.putAll(props);

      // Init default framework properties
      if (getProperty(Constants.FRAMEWORK_VERSION) == null)
         setProperty(Constants.FRAMEWORK_VERSION, OSGi_FRAMEWORK_VERSION);
      if (getProperty(Constants.FRAMEWORK_VENDOR) == null)
         setProperty(Constants.FRAMEWORK_VENDOR, OSGi_FRAMEWORK_VENDOR);
      if (getProperty(Constants.FRAMEWORK_LANGUAGE) == null)
         setProperty(Constants.FRAMEWORK_LANGUAGE, OSGi_FRAMEWORK_LANGUAGE);
      if (getProperty(Constants.FRAMEWORK_OS_NAME) == null)
         setProperty(Constants.FRAMEWORK_OS_NAME, OSGi_FRAMEWORK_OS_NAME);
      if (getProperty(Constants.FRAMEWORK_OS_VERSION) == null)
         setProperty(Constants.FRAMEWORK_OS_VERSION, OSGi_FRAMEWORK_OS_VERSION);
      if (getProperty(Constants.FRAMEWORK_PROCESSOR) == null)
         setProperty(Constants.FRAMEWORK_PROCESSOR, OSGi_FRAMEWORK_PROCESSOR);
   }

   /**
    * Get a property
    *
    * @param key the property key
    * @return the property
    * @throws SecurityException if the caller doesn't have the relevant property permission
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
    * Set a property. This is used at the frame work init state.
    * 
    * @param key the prperty key
    * @param value the property value
    */
   public void setProperty(String key, String value)
   {
      properties.put(key, value);
   }

   /**
    * Get a plugin that is registered with the bundle manager.
    * 
    * @param <T> the pluging type
    * @param clazz the plugin type
    * @return the plugin
    * @throws IllegalStateException if the requested plugin class is not registered
    */
   @SuppressWarnings("unchecked")
   public <T extends Plugin> T getPlugin(Class<T> clazz)
   {
      T plugin = (T)plugins.get(clazz);
      if (plugin == null)
         throw new IllegalStateException("Cannot obtain plugin for: " + clazz.getName());

      return plugin;
   }

   /**
    * Get an optional plugin that is registered with the bundle manager.
    * 
    * @param <T> the pluging type
    * @param clazz the plugin type
    * @return The plugin instance or null if the requested plugin class is not registered
    */
   @SuppressWarnings("unchecked")
   public <T extends Plugin> T getOptionalPlugin(Class<T> clazz)
   {
      return (T)plugins.get(clazz);
   }

   /**
    * Add a plugin
    * 
    * @param plugin the plugin
    */
   public void addPlugin(Plugin plugin)
   {
      Class<?> key = getPluginKey(plugin);
      log.debug("Add plugin: " + key.getName());
      plugins.put(key, plugin);

      // In case a service plugin gets added after the framework is started
      if (isFrameworkActive() == true && plugin instanceof ServicePlugin)
      {
         ServicePlugin servicePlugin = (ServicePlugin)plugin;
         servicePlugin.startService();
      }
   }

   /**
    * Get the key for a given plugin
    *
    * @param plugin the plugin
    * @return the class
    */
   private Class<?> getPluginKey(Plugin plugin)
   {
      if (plugin == null)
         throw new IllegalArgumentException("Null plugin");

      Class<?> clazz = plugin.getClass();
      Class<?> key = getPluginKey(plugin, clazz);

      // If the plugin could not be added by Interface, use the clazz directly
      return (key != null ? key : clazz);
   }

   /**
    * Get the plugin key by scanning the interfaces for the given clazz.
    *
    * @param plugin the plugin
    * @param clazz the class
    * @return The first interface that extends Plugin.
    */
   private Class<?> getPluginKey(Plugin plugin, Class<?> clazz)
   {
      // Scan the interfaces on the class
      for (Class<?> interf : clazz.getInterfaces())
      {
         if (interf == Plugin.class || interf == ServicePlugin.class)
            continue;

         if (Plugin.class.isAssignableFrom(interf))
         {
            return interf;
         }
      }

      // Interface not found, try the plugin's superclass 
      Class<?> superclass = clazz.getSuperclass();
      if (Plugin.class.isAssignableFrom(superclass))
         return getPluginKey(plugin, superclass);

      return null;
   }

   /**
    * Remove a plugin
    * 
    * @param plugin the plugin to remove
    */
   public void removePlugin(Plugin plugin)
   {
      Class<?> key = getPluginKey(plugin);
      log.debug("Remove plugin: " + key.getName());
      plugins.remove(key);
   }

   /**
    * Are we active
    * @return true when the system is active
    */
   public boolean isFrameworkActive()
   {
      // We are active if the system bundle is ACTIVE
      AbstractBundleState bundleState = getSystemBundle();
      return bundleState.getState() == Bundle.ACTIVE;
   }

   /**
    * Install a bundle from an URL.
    * 
    * @param url the url of the bundle
    * @return the bundle state
    * @throws BundleException for any error
    */
   public OSGiBundleState installBundle(URL url) throws BundleException
   {
      if (url == null)
         throw new BundleException("Null url");

      return installBundle(url.toExternalForm(), null);
   }

   /**
    * Install a bundle from an input stream.
    * 
    * @param location the bundle's location identifier
    * @param input an optional input stream to read the bundle content from
    * @return the bundle state
    * @throws BundleException for any error
    */
   public OSGiBundleState installBundle(String location, InputStream input) throws BundleException
   {
      if (location == null)
         throw new BundleException("Null location");

      URL locationURL;

      // Get the location URL
      if (input != null)
      {
         locationURL = getBundleStorageLocation(input);
      }
      else
      {
         locationURL = getLocationURL(location);
      }

      // Get the root file
      VirtualFile root;
      try
      {
         root = VFS.getRoot(locationURL);
      }
      catch (IOException e)
      {
         throw new BundleException("Invalid bundle location=" + locationURL, e);
      }

      return install(root, location, false);
   }

   private URL getBundleStorageLocation(InputStream input) throws BundleException
   {
      try
      {
         BundleStoragePlugin plugin = getPlugin(BundleStoragePlugin.class);
         String path = plugin.getStorageDir(getSystemBundle()).getCanonicalPath();

         // [TODO] do properly
         File file = new File(path + "/bundle-" + System.currentTimeMillis() + ".jar");
         FileOutputStream fos = new FileOutputStream(file);
         VFSUtils.copyStream(input, fos);
         fos.close();

         URL locationURL = file.toURI().toURL();
         return locationURL;
      }
      catch (IOException ex)
      {
         throw new BundleException("Cannot store bundle from input stream", ex);
      }
   }

   /**
    * Install a bundle from a virtual file.
    *  
    * @param root the virtual file that point to the bundle
    * 
    * @return the bundle state
    * @throws BundleException for any error
    */
   public OSGiBundleState installBundle(VirtualFile root) throws BundleException
   {
      return install(root, root.toString(), false);
   }

   /*
    * Installs a bundle from the given virtual file.
    */
   private OSGiBundleState install(VirtualFile root, String location, boolean autoStart) throws BundleException
   {
      if (location == null)
         throw new IllegalArgumentException("Null location");

      BundleInfo info = BundleInfo.createBundleInfo(root, location);
      Deployment dep = DeploymentFactory.createDeployment(info);
      dep.setAutoStart(autoStart);

      return installBundle(dep);
   }

   /**
    * Install a bundle from a deployment. 
    *  
    * @param dep the deployment that represents the bundle
    * 
    * @return the bundle state
    * @throws BundleException for any error
    */
   public OSGiBundleState installBundle(Deployment dep) throws BundleException
   {
      // Create the deployment and deploy it
      try
      {
         VFSDeployment deployment = VFSDeploymentFactory.getInstance().createVFSDeployment(dep.getRoot());
         MutableAttachments att = (MutableAttachments)deployment.getPredeterminedManagedObjects();
         att.addAttachment(Deployment.class, dep);

         deployerClient.deploy(deployment);
         try
         {
            DeploymentUnit unit = deployerStructure.getDeploymentUnit(deployment.getName());
            OSGiBundleState bundleState = unit.getAttachment(OSGiBundleState.class);
            if (bundleState == null)
               throw new IllegalStateException("Unable to determine bundle state for " + deployment.getName());

            return bundleState;
         }
         catch (Exception e)
         {
            deployerClient.undeploy(deployment);
            throw e;
         }
      }
      catch (Exception ex)
      {
         Throwable cause = ex;
         if (ex instanceof DeploymentException)
         {
            cause = ex.getCause();
            if (cause instanceof BundleException)
               throw (BundleException)cause;
         }
         throw new BundleException("Error installing bundle from: " + dep, (cause != null ? cause : ex));
      }
   }

   /**
    * Updates a bundle from an InputStream. 
    */
   public void updateBundle(OSGiBundleState bundleState, InputStream in) throws BundleException
   {
      // If the specified InputStream is null, the Framework must create the InputStream from which to read the updated bundle by interpreting, 
      // in an implementation dependent manner, this bundle's Bundle-UpdateLocation Manifest header, if present, or this bundle's original location.
      URL updateURL = bundleState.getOSGiMetaData().getBundleUpdateLocation();
      if (in == null)
      {
         try
         {
            if (updateURL == null)
               throw new IllegalStateException("Cannot obtain Bundle-UpdateLocation for: " + bundleState);

            in = updateURL.openStream();
         }
         catch (IOException ex)
         {
            throw new BundleException("Cannot obtain update input stream for: " + bundleState, ex);
         }
      }

      // If this bundle's state is ACTIVE, it must be stopped before the update and started after the update successfully completes. 
      boolean activeBeforeUpdate = (bundleState.getState() == Bundle.ACTIVE);

      // If this bundle's state is UNINSTALLED then an IllegalStateException is thrown. 
      if (bundleState.getState() == Bundle.UNINSTALLED)
         throw new IllegalStateException("Bundle already uninstalled: " + this);

      // If this bundle has exported any packages that are imported by another bundle, these packages must not be updated. 
      // Instead, the previous package version must remain exported until the PackageAdmin.refreshPackages method has been 
      // has been called or the Framework is relaunched. 

      // If this bundle's state is ACTIVE, STARTING  or STOPPING, this bundle is stopped as described in the Bundle.stop method. 
      // If Bundle.stop throws an exception, the exception is rethrown terminating the update.
      if (bundleState.getState() == Bundle.ACTIVE || bundleState.getState() == Bundle.STARTING || bundleState.getState() == Bundle.STOPPING)
      {
         stopBundle(bundleState);
      }

      // The updated version of this bundle is read from the input stream and installed. 
      // If the Framework is unable to install the updated version of this bundle, the original version of this bundle must be restored 
      // and a BundleException must be thrown after completion of the remaining steps.
      String location = (updateURL != null ? updateURL.toExternalForm() : bundleState.getCanonicalName() + "/update");
      OSGiBundleState updatedBundleState = null;
      BundleException throwAfterUpdate = null;
      try
      {
         URL storageLocation = getBundleStorageLocation(in);
         VirtualFile root = VFS.getRoot(storageLocation);

         BundleInfo info = BundleInfo.createBundleInfo(root, location);
         Deployment dep = DeploymentFactory.createDeployment(info);
         dep.addAttachment(OSGiBundleState.class, bundleState);
         dep.setBundleUpdate(true);
         dep.setAutoStart(false);

         updatedBundleState = installBundle(dep);
      }
      catch (Exception ex)
      {
         if (ex instanceof BundleException)
            throwAfterUpdate = (BundleException)ex;
         else
            throwAfterUpdate = new BundleException("Cannot install updated bundle from: " + location, ex);

         if (activeBeforeUpdate)
         {
            startBundle(bundleState);
         }
      }

      // If the updated version of this bundle was successfully installed, a bundle event of type BundleEvent.UPDATED is fired
      if (updatedBundleState != null)
      {
         FrameworkEventsPlugin plugin = getPlugin(FrameworkEventsPlugin.class);
         plugin.fireBundleEvent(updatedBundleState, BundleEvent.UPDATED);

         // If this bundle's state was originally ACTIVE, the updated bundle is started as described in the Bundle.start method. 
         // If Bundle.start throws an exception, a Framework event of type FrameworkEvent.ERROR is fired containing the exception
         if (activeBeforeUpdate)
         {
            startBundle(updatedBundleState);
         }
      }

      // A BundleException must be thrown after completion of the remaining steps
      if (throwAfterUpdate != null)
      {
         throw throwAfterUpdate;
      }
   }

   /**
    * Uninstall a bundle
    * 
    * @param bundleState the bundle
    * @throws BundleException for any error
    */
   public void uninstallBundle(OSGiBundleState bundleState) throws BundleException
   {
      long id = bundleState.getBundleId();
      if (id == 0)
         throw new IllegalArgumentException("Cannot uninstall system bundle");

      if (getBundleById(id) == null)
         throw new BundleException(bundleState + " not installed");

      for (DeploymentUnit unit : bundleState.getDeploymentUnits())
      {
         try
         {
            deployerClient.undeploy(unit.getName());
            bundleState.modified();
         }
         catch (DeploymentException e)
         {
            throw new BundleException("Unable to uninstall " + bundleState, e);
         }
      }
   }

   /**
    * Add a deployment
    * 
    * @param unit the deployment unit
    * @return the bundle state
    * @throws IllegalArgumentException for a null parameter
    */
   public OSGiBundleState addDeployment(DeploymentUnit unit)
   {
      if (unit == null)
         throw new IllegalArgumentException("Null unit");

      OSGiMetaData metaData = unit.getAttachment(OSGiMetaData.class);
      if (metaData == null)
      {
         Manifest manifest = unit.getAttachment(Manifest.class);
         // [TODO] we need a mechanism to construct an OSGiMetaData from an easier factory
         if (manifest == null)
            manifest = new Manifest();
         // [TODO] populate some bundle information
         Attributes attributes = manifest.getMainAttributes();
         attributes.put(new Name(Constants.BUNDLE_NAME), unit.getName());
         attributes.put(new Name(Constants.BUNDLE_SYMBOLICNAME), unit.getName());
         metaData = new AbstractOSGiMetaData(manifest);
         unit.addAttachment(OSGiMetaData.class, metaData);
      }

      // The bundle location is not necessarily the bundle root url
      // The framework is expected to preserve the location passed into installBundle(String)
      Deployment dep = unit.getAttachment(Deployment.class);
      String location = (dep != null ? dep.getLocation() : unit.getName());

      // In case of Bundle.update() the OSGiBundleState should be attached
      // we add the DeploymentUnit 
      OSGiBundleState bundleState = (dep != null ? dep.getAttachment(OSGiBundleState.class) : null);
      if (bundleState != null)
         bundleState.addDeploymentUnit(unit);

      // Create a new OSGiBundleState and add it to the manager
      if (bundleState == null)
      {
         bundleState = new OSGiBundleState(location, unit);
         addBundle(bundleState);
      }
      return bundleState;
   }

   /**
    * Generate a name for the deployment unit
    * 
    * todo some better solution
    * 
    * @param unit the deployment unit
    * @return the name
    */
   protected String generateName(DeploymentUnit unit)
   {
      StringBuilder result = new StringBuilder();
      String name = unit.getName();
      for (int i = 0; i < name.length(); ++i)
      {
         char c = name.charAt(i);
         if (Character.isJavaIdentifierPart(c))
            result.append(c);
         else
            result.append('_');
      }
      return result.toString();
   }

   /**
    * Add a bundle
    * 
    * @param bundleState the bundle state
    * @throws IllegalArgumentException for a null bundle state
    */
   public void addBundle(AbstractBundleState bundleState)
   {
      if (bundleState == null)
         throw new IllegalArgumentException("Null bundle state");

      validateBundle(bundleState);

      bundleState.setBundleManager(this);
      bundles.add(bundleState);

      // Only fire the INSTALLED event if this is not an update
      boolean fireEvent = true;
      if (bundleState instanceof OSGiBundleState)
      {
         DeploymentUnit unit = ((OSGiBundleState)bundleState).getDeploymentUnit();
         Deployment dep = unit.getAttachment(Deployment.class);
         fireEvent = (dep == null || dep.isBundleUpdate() == false);
      }

      bundleState.changeState(Bundle.INSTALLED, fireEvent);

      // Add the bundle to the resolver
      // Note, plugins are not registered when the system bundle is added 
      ResolverPlugin bundleResolver = getOptionalPlugin(ResolverPlugin.class);
      if (bundleResolver != null)
         bundleResolver.addBundle(bundleState);

      log.debug("Added: " + bundleState);
   }

   /**
    * Validate the bundle
    * 
    * @param bundleState the bundle state
    */
   @SuppressWarnings("deprecation")
   private void validateBundle(AbstractBundleState bundleState)
   {
      OSGiMetaData metaData = bundleState.getOSGiMetaData();
      if (metaData == null)
         return;

      String symbolicName = metaData.getBundleSymbolicName();
      if (symbolicName == null)
         throw new IllegalStateException("No bundle symbolic name " + bundleState);

      int manifestVersion = metaData.getBundleManifestVersion();
      if (manifestVersion > 2)
         throw new IllegalStateException("Unsupported manifest version " + manifestVersion + " for " + bundleState);

      for (AbstractBundleState bundle : getBundles())
      {
         OSGiMetaData other = bundle.getOSGiMetaData();
         if (symbolicName.equals(other.getBundleSymbolicName()))
         {
            if (other.isSingleton() && metaData.isSingleton())
               throw new IllegalStateException("Cannot install singleton " + bundleState + " another singleton is already installed: " + bundle.getLocation());
            if (other.getBundleVersion().equals(metaData.getBundleVersion()))
               throw new IllegalStateException("Cannot install " + bundleState + " a bundle with that name and version is already installed: "
                     + bundle.getLocation());
         }
      }

      List<PackageAttribute> importPackages = metaData.getImportPackages();
      if (importPackages != null && importPackages.isEmpty() == false)
      {
         Set<String> packages = new HashSet<String>();
         for (PackageAttribute packageAttribute : importPackages)
         {
            String packageName = packageAttribute.getAttribute();
            if (packages.contains(packageName))
               throw new IllegalStateException("Duplicate import of package " + packageName + " for " + bundleState);
            packages.add(packageName);

            if (packageName.startsWith("java."))
               throw new IllegalStateException("Not allowed to import java.* for " + bundleState);

            String version = packageAttribute.getAttributeValue(Constants.VERSION_ATTRIBUTE, String.class);
            String specificationVersion = packageAttribute.getAttributeValue(Constants.PACKAGE_SPECIFICATION_VERSION, String.class);
            if (version != null && specificationVersion != null && version.equals(specificationVersion) == false)
               throw new IllegalStateException(packageName + " version and specification version should be the same for " + bundleState);
         }
      }

      List<PackageAttribute> exportPackages = metaData.getExportPackages();
      if (exportPackages != null && exportPackages.isEmpty() == false)
      {
         for (PackageAttribute packageAttribute : exportPackages)
         {
            String packageName = packageAttribute.getAttribute();
            if (packageName.startsWith("java."))
               throw new IllegalStateException("Not allowed to export java.* for " + bundleState);
         }
      }
   }

   /**
    * Remove a bundle
    * 
    * @param bundleState the bundle state
    * @throws IllegalArgumentException for a null bundle state
    */
   public void removeBundle(OSGiBundleState bundleState)
   {
      if (bundleState == null)
         throw new IllegalArgumentException("Null bundle state");

      bundleState.uninstallInternal();
      bundleState.setBundleManager(null);

      // Remove the bundle from the resolver
      ResolverPlugin bundleResolver = getOptionalPlugin(ResolverPlugin.class);
      if (bundleResolver != null)
         bundleResolver.removeBundle(bundleState);

      bundles.remove(bundleState);
      log.debug("Removed " + bundleState.getCanonicalName());
   }

   /**
    * Get the system bundle
    * 
    * @return the system bundle
    */
   public OSGiSystemState getSystemBundle()
   {
      return systemBundle;
   }

   /**
    * Get the system bundle context
    * 
    * @return the system bundle context
    */
   public BundleContext getSystemContext()
   {
      return systemBundle.getBundleContext();
   }

   /**
    * Get a deployment
    * 
    * @param id the id of the bundle
    * @return the deployment or null if there is no bundle with that id
    */
   public DeploymentUnit getDeployment(long id)
   {
      if (id == 0)
         throw new IllegalArgumentException("Cannot get deployment from system bundle");

      OSGiBundleState bundleState = (OSGiBundleState)getBundleById(id);
      if (bundleState == null)
         return null;

      return bundleState.getDeploymentUnit();
   }

   /**
    * Get a bundle by id
    * 
    * @param id the id of the bundle
    * @return the bundle or null if there is no bundle with that id
    */
   public AbstractBundleState getBundleById(long id)
   {
      AbstractBundleState result = null;
      for (AbstractBundleState aux : bundles)
      {
         if (id == aux.getBundleId())
         {
            result = aux;
            break;
         }
      }
      return result;
   }

   /**
    * Get a bundle by symbolic name and version
    *
    * @param symbolicName the symbolic name of the bundle
    * @param version the version of the bundle
    * @return the bundle or null if there is no bundle with that id
    */
   public AbstractBundleState getBundle(String symbolicName, Version version)
   {
      AbstractBundleState result = null;
      for (AbstractBundleState aux : bundles)
      {
         String auxName = aux.getSymbolicName();
         Version auxVersion = aux.getVersion();
         if (auxName.equals(symbolicName) && auxVersion.equals(version))
         {
            result = aux;
            break;
         }
      }
      return result;
   }

   /**
    * Get the underlying bundle state for a bundle
    * 
    * @param bundle the bundle
    * @return the bundle state
    * @throws IllegalArgumentException if it is an unrecognised bundle
    */
   public AbstractBundleState getBundleState(Bundle bundle)
   {
      if (bundle instanceof OSGiBundleWrapper)
         bundle = ((OSGiBundleWrapper)bundle).getBundleState();

      if (bundle instanceof AbstractBundleState == false)
         throw new IllegalArgumentException("Cannot obtain bunde state from: " + bundle);

      return (AbstractBundleState)bundle;
   }

   /**
    * Get a bundle by location
    * 
    * @param location the location of the bundle
    * @return the bundle or null if there is no bundle with that location
    */
   public AbstractBundleState getBundleByLocation(String location)
   {
      if (location == null)
         throw new IllegalArgumentException("Null location");

      AbstractBundleState result = null;

      for (AbstractBundleState aux : bundles)
      {
         String auxLocation = aux.getLocation();
         if (location.equals(auxLocation))
         {
            result = aux;
            break;
         }

         // Fallback to the deployment name
         else if (aux instanceof OSGiBundleState)
         {
            DeploymentUnit unit = ((OSGiBundleState)aux).getDeploymentUnit();
            if (location.equals(unit.getName()))
            {
               result = aux;
               break;
            }
         }
      }
      return result;
   }

   /**
    * Get all the bundles
    * 
    * @return the bundles
    */
   public Collection<AbstractBundleState> getBundles()
   {
      return Collections.unmodifiableList(bundles);
   }

   /**
    * Get the bundles with the given state
    * 
    * @param state the bundle state
    * @return the bundles
    */
   public Collection<AbstractBundleState> getBundles(int state)
   {
      List<AbstractBundleState> bundles = new ArrayList<AbstractBundleState>();
      for (AbstractBundleState aux : getBundles())
      {
         if (aux.getState() == state)
            bundles.add(aux);
      }
      return bundles;
   }

   /**
    * Resolve a bundle
    * 
    * @param bundleState the bundle state
    * @param errorOnFail whether to throw an error if it cannot be resolved
    * @return true when resolved
    */
   public boolean resolveBundle(OSGiBundleState bundleState, boolean errorOnFail)
   {
      int state = bundleState.getState();
      if (state != Bundle.INSTALLED)
         return true;

      DeploymentUnit unit = bundleState.getDeploymentUnit();
      ControllerContext context = unit.getAttachment(ControllerContext.class);

      ControllerState requiredState = context.getRequiredState();
      DeploymentStage requiredStage = unit.getRequiredStage();

      // TODO [JBDEPLOY-226] Allow multiple deployments to change state at once
      try
      {
         deployerClient.change(unit.getName(), DeploymentStages.CLASSLOADER);
         deployerClient.checkComplete(unit.getName());
         bundleState.changeState(Bundle.RESOLVED);
         return true;
      }
      catch (DeploymentException ex)
      {
         unit.setRequiredStage(requiredStage);
         context.setRequiredState(requiredState);

         if (errorOnFail)
            throw new IllegalStateException("Error resolving bundle: " + bundleState, ex);

         return false;
      }
   }

   /**
    * Start a bundle.
    * 
    * Stating a bundle is done in an attempt to move the bundle's DeploymentUnit to state INSTALLED.
    * A failure to resolve the bundle is fatal, the bundle should remain in state INSTALLED.
    * A failure in BundleActivator.start() is a normal condition not handled by the deployment layer.
    * 
    * @see OSGiBundleActivatorDeployer
    * @see OSGiBundleState#startInternal()
    */
   public void startBundle(OSGiBundleState bundleState) throws BundleException
   {
      // If this bundle's state is UNINSTALLED then an IllegalStateException is thrown. 
      if (bundleState.getState() == Bundle.UNINSTALLED)
         throw new IllegalStateException("Bundle already uninstalled: " + this);

      // [TODO] If this bundle is in the process of being activated or deactivated then this method must wait for activation or deactivation 
      // to complete before continuing. If this does not occur in a reasonable time, a BundleException is thrown to indicate this bundle was 
      // unable to be started.

      // If this bundle's state is ACTIVE then this method returns immediately. 
      if (bundleState.getState() == Bundle.ACTIVE)
         return;

      // [TODO] If the START_TRANSIENT option is not set then set this bundle's autostart setting to Started with declared activation  
      // if the START_ACTIVATION_POLICY option is set or Started with eager activation if not set. When the Framework is restarted 
      // and this bundle's autostart setting is not Stopped, this bundle must be automatically started.

      // If this bundle's state is not RESOLVED, an attempt is made to resolve this bundle. If the Framework cannot resolve this bundle, 
      // a BundleException is thrown.
      if (bundleState.getState() != Bundle.RESOLVED)
      {
         // Resolve all INSTALLED bundles through the PackageAdmin
         PackageAdmin packageAdmin = getPlugin(PackageAdminPlugin.class);
         packageAdmin.resolveBundles(null);

         if (bundleState.getState() != Bundle.RESOLVED)
            throw new BundleException("Cannot resolve bundle: " + bundleState);
      }

      // [TODO] If the START_ACTIVATION_POLICY option is set and this bundle's declared activation policy is lazy then:
      //    * If this bundle's state is STARTING then this method returns immediately.
      //    * This bundle's state is set to STARTING.
      //    * A bundle event of type BundleEvent.LAZY_ACTIVATION is fired.
      //    * This method returns immediately and the remaining steps will be followed when this bundle's activation is later triggered.

      try
      {
         DeploymentUnit unit = bundleState.getDeploymentUnit();
         deployerClient.change(unit.getName(), DeploymentStages.INSTALLED);
         deployerClient.checkComplete(unit.getName());

         // The potential BundleException is attached by the OSGiBundleActivatorDeployer
         BundleException startEx = unit.removeAttachment(BundleException.class);
         if (startEx != null)
         {
            // Reset the deployment unit to stage classloader
            deployerClient.change(unit.getName(), DeploymentStages.CLASSLOADER);
            deployerClient.checkComplete(unit.getName());

            throw startEx;
         }
      }
      catch (DeploymentException ex)
      {
         Throwable cause = ex.getCause();
         if (cause instanceof BundleException)
            throw (BundleException)cause;

         throw new BundleException("Error starting " + bundleState, (cause != null ? cause : ex));
      }
   }

   /**
    * Stop a bundle
    * 
    * Stopping a bundle is done in an attempt to move the bundle's DeploymentUnit to state CLASSLOADER.
    * 
    * @see OSGiBundleActivatorDeployer
    * @see OSGiBundleState#stopInternal()
    */
   public void stopBundle(OSGiBundleState bundleState) throws BundleException
   {
      // If this bundle's state is UNINSTALLED then an IllegalStateException is thrown. 
      if (bundleState.getState() == Bundle.UNINSTALLED)
         throw new IllegalStateException("Bundle already uninstalled: " + this);

      // [TODO] If this bundle is in the process of being activated or deactivated then this method must wait for activation or deactivation 
      // to complete before continuing. If this does not occur in a reasonable time, a BundleException is thrown to indicate this bundle 
      // was unable to be stopped.

      // [TODO] If the STOP_TRANSIENT option is not set then then set this bundle's persistent autostart setting to to Stopped. 
      // When the Framework is restarted and this bundle's autostart setting is Stopped, this bundle must not be automatically started. 

      // If this bundle's state is not STARTING or ACTIVE then this method returns immediately
      if (bundleState.getState() != Bundle.STARTING && bundleState.getState() != Bundle.ACTIVE)
         return;

      try
      {
         DeploymentUnit unit = bundleState.getDeploymentUnit();
         deployerClient.change(unit.getName(), DeploymentStages.CLASSLOADER);
         deployerClient.checkComplete(unit.getName());

         // The potential BundleException is attached by the OSGiBundleActivatorDeployer
         BundleException stopEx = unit.removeAttachment(BundleException.class);
         if (stopEx != null)
         {
            throw stopEx;
         }
      }
      catch (DeploymentException ex)
      {
         Throwable cause = ex.getCause();
         if (cause instanceof BundleException)
            throw (BundleException)cause;

         throw new BundleException("Error stopping " + bundleState, (cause != null ? cause : ex));
      }
   }

   /**
    * Load class from a bundle.
    * If it cannot be loaded, return null.
    *
    * @param bundle the bundle to load from
    * @param clazz the class
    * @return class or null
    */
   Class<?> loadClass(Bundle bundle, String clazz)
   {
      try
      {
         return bundle.loadClass(clazz);
      }
      catch (ClassNotFoundException e)
      {
         return null;
      }
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

   /**
    * Get services
    * 
    * @param bundle the referencing bundle
    * @param clazz any class
    * @param filter any filter
    * @param checkAssignable whether to check isAssignable
    * @return the services
    */
   Collection<ServiceReference> getServices(AbstractBundleState bundle, String clazz, Filter filter, boolean checkAssignable)
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
         Class<?> type = loadClass(bundle, clazz);
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
         if (isUnregistered(context) == false)
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
    * Get service reference
    * 
    * @param bundle the referencing bundle
    * @param clazz any class
    * @return the reference
    */
   ServiceReference getServiceReference(AbstractBundleState bundle, String clazz)
   {
      Collection<ServiceReference> services = getServices(bundle, clazz, null, true);
      if (services == null || services.isEmpty())
         return null;

      return services.iterator().next();
   }

   /**
    * Get service references
    * 
    * @param bundle the referencing bundle
    * @param clazz any class
    * @param filter any filter
    * @param checkAssignable whether to check isAssignable
    * @return the services
    */
   ServiceReference[] getServiceReferences(AbstractBundleState bundle, String clazz, Filter filter, boolean checkAssignable)
   {
      Collection<ServiceReference> services = getServices(bundle, clazz, filter, checkAssignable);
      if (services == null || services.isEmpty())
         return null;

      return services.toArray(new ServiceReference[services.size()]);
   }

   /**
    * Get service references
    * 
    * @param bundle the referencing bundle
    * @param clazz any class
    * @param filterStr any filter
    * @param checkAssignable whether to check isAssignable
    * @return the services
    * @throws InvalidSyntaxException when the filter is invalid
    */
   ServiceReference[] getServiceReferences(AbstractBundleState bundle, String clazz, String filterStr, boolean checkAssignable) throws InvalidSyntaxException
   {
      Filter filter = NoFilter.INSTANCE;
      if (filterStr != null)
         filter = FrameworkUtil.createFilter(filterStr);

      return getServiceReferences(bundle, clazz, filter, checkAssignable);
   }

   /**
    * Register a service
    * 
    * @param bundleState the bundle
    * @param clazzes the classes to implement
    * @param service the service
    * @param properties the properties
    * @return the service state
    */
   @SuppressWarnings("rawtypes")
   OSGiServiceState registerService(AbstractBundleState bundleState, String[] clazzes, Object service, Dictionary properties)
   {
      OSGiServiceState result = new OSGiServiceState(bundleState, clazzes, service, properties);
      result.internalRegister();
      try
      {
         Controller controller = kernel.getController();
         controller.install(result);
      }
      catch (Throwable t)
      {
         fireError(bundleState, "installing service to MC in", t);
         throw new RuntimeException(t);
      }

      FrameworkEventsPlugin plugin = getPlugin(FrameworkEventsPlugin.class);
      plugin.fireServiceEvent(bundleState, ServiceEvent.REGISTERED, result);

      return result;
   }

   /**
    * Get registered contexts for bundle.
    *
    * @param bundleState the owning bundle
    * @return registered contexts
    */
   Set<ControllerContext> getRegisteredContext(OSGiBundleState bundleState)
   {
      DeploymentUnit unit = bundleState.getDeploymentUnit();
      return registry.getContexts(unit);
   }

   /**
    * Unregister a service
    * 
    * @param serviceState the service state
    */
   void unregisterService(OSGiServiceState serviceState)
   {
      Controller controller = kernel.getController();
      controller.uninstall(serviceState.getName());

      serviceState.internalUnregister();

      FrameworkEventsPlugin plugin = getPlugin(FrameworkEventsPlugin.class);
      plugin.fireServiceEvent(serviceState.getBundleState(), ServiceEvent.UNREGISTERING, serviceState);
   }

   /**
    * Unregister contexts.
    *
    * @param bundleState the stopping bundle
    */
   void unregisterContexts(OSGiBundleState bundleState)
   {
      DeploymentUnit unit = bundleState.getDeploymentUnit();
      Set<ControllerContext> contexts = registry.getContexts(unit);
      for (ControllerContext context : contexts)
      {
         unregisterContext(context);
      }
   }

   /**
    * Unregister context.
    *
    * @param context the context
    */
   private static void unregisterContext(ControllerContext context)
   {
      if (context instanceof ServiceRegistration)
      {
         ServiceRegistration service = (ServiceRegistration)context;
         service.unregister();
      }
   }

   /**
    * Get a service
    * 
    * @param bundleState the bundle that requests the service
    * @param reference the service reference
    * @return the service
    */
   Object getService(AbstractBundleState bundleState, ServiceReference reference)
   {
      ControllerContextHandle handle = (ControllerContextHandle)reference;
      ControllerContext context = handle.getContext();
      if (isUnregistered(context)) // we're probably not installed anymore
         return null;

      return bundleState.addContextInUse(context);
   }

   /**
    * Is the context undergisted.
    *
    * @param context the context
    * @return true if the context is unregisted, false otherwise
    */
   static boolean isUnregistered(ControllerContext context)
   {
      Controller controller = context.getController();
      return controller == null || controller.getStates().isBeforeState(context.getState(), ControllerState.INSTALLED);
   }

   /**
    * Unget a service
    * 
    * @param bundleState the bundle state
    * @param reference the service reference
    * @return true when the service is still in use by the bundle
    */
   boolean ungetService(AbstractBundleState bundleState, ServiceReference reference)
   {
      if (reference == null)
         throw new IllegalArgumentException("Null reference");

      ControllerContextHandle serviceReference = (ControllerContextHandle)reference;
      ControllerContext context = serviceReference.getContext();
      return ungetContext(bundleState, context);
   }

   /**
    * Unget a context
    * 
    * @param bundleState the bundle state
    * @param context the context
    * @return true when the context is still in use by the bundle
    */
   boolean ungetContext(AbstractBundleState bundleState, ControllerContext context)
   {
      return bundleState.removeContextInUse(context);
   }

   /**
    * Get the executor.
    * 
    * @return the executor.
    */
   Executor getExecutor()
   {
      return executor;
   }

   /**
    * Initialize this Framework. 
    * 
    * After calling this method, this Framework must:
    * - Be in the Bundle.STARTING state.
    * - Have a valid Bundle Context.
    * - Be at start level 0.
    * - Have event handling enabled.
    * - Have reified Bundle objects for all installed bundles.
    * - Have registered any framework services. For example, PackageAdmin, ConditionalPermissionAdmin, StartLevel.
    * 
    * This Framework will not actually be started until start is called.
    * 
    * This method does nothing if called when this Framework is in the Bundle.STARTING, Bundle.ACTIVE or Bundle.STOPPING states. 
    */
   public void initFramework()
   {
      // Log INFO about this implementation
      String implTitle = getClass().getPackage().getImplementationTitle();
      String implVersion = getClass().getPackage().getImplementationVersion();
      log.info(implTitle + " - " + implVersion);

      int state = systemBundle.getState();

      // This method does nothing if called when this Framework is in the STARTING, ACTIVE or STOPPING state
      if (state == Bundle.STARTING || state == Bundle.ACTIVE || state == Bundle.STOPPING)
         return;

      // Put into the STARTING state
      systemBundle.changeState(Bundle.STARTING);

      // Create the system bundle context
      systemBundle.createBundleContext();

      // [TODO] Be at start level 0

      // Have event handling enabled
      FrameworkEventsPlugin eventsPlugin = getPlugin(FrameworkEventsPlugin.class);
      eventsPlugin.setActive(true);

      // Have registered any framework services.
      for (Plugin plugin : plugins.values())
      {
         if (plugin instanceof ServicePlugin)
         {
            ServicePlugin servicePlugin = (ServicePlugin)plugin;
            servicePlugin.startService();
         }
      }

      // Cleanup the storage area
      String storageClean = getProperty(Constants.FRAMEWORK_STORAGE_CLEAN);
      BundleStoragePlugin storagePlugin = getOptionalPlugin(BundleStoragePlugin.class);
      if (storagePlugin != null)
         storagePlugin.cleanStorage(storageClean);
   }

   /**
    * Start the framework
    * 
    * @throws BundleException for any error
    */
   public void startFramework() throws BundleException
   {
      // If this Framework is not in the STARTING state, initialize this Framework
      if (systemBundle.getState() != Bundle.STARTING)
         initFramework();

      // All installed bundles must be started
      AutoInstallPlugin autoInstall = getOptionalPlugin(AutoInstallPlugin.class);
      if (autoInstall != null)
      {
         autoInstall.installBundles();
         autoInstall.startBundles();
      }

      // Add the system bundle to the resolver
      ResolverPlugin bundleResolver = getOptionalPlugin(ResolverPlugin.class);
      if (bundleResolver != null)
         bundleResolver.addBundle(systemBundle);

      // This Framework's state is set to ACTIVE
      systemBundle.changeState(Bundle.ACTIVE);

      // A framework event of type STARTED is fired
      FrameworkEventsPlugin plugin = getPlugin(FrameworkEventsPlugin.class);
      plugin.fireFrameworkEvent(systemBundle, FrameworkEvent.STARTED, null);
   }

   /**
    * Stop this Framework.
    * 
    * The method returns immediately to the caller after initiating the following steps to be taken on another thread.
    * 
    * 1. This Framework's state is set to Bundle.STOPPING.
    * 2. All installed bundles must be stopped without changing each bundle's persistent autostart setting. 
    * 3. Unregister all services registered by this Framework.
    * 4. Event handling is disabled.
    * 5. This Framework's state is set to Bundle.RESOLVED.
    * 6. All resources held by this Framework are released. This includes threads, bundle class loaders, open files, etc.
    * 7. Notify all threads that are waiting at waitForStop that the stop operation has completed.
    * 
    * After being stopped, this Framework may be discarded, initialized or started. 
    */
   public void stopFramework()
   {
      int beforeCount = stopMonitor.get();
      Runnable stopcmd = new Runnable()
      {
         public void run()
         {
            synchronized (stopMonitor)
            {
               // Start the stop process
               stopMonitor.addAndGet(1);

               // This Framework's state is set to Bundle.STOPPING
               systemBundle.changeState(Bundle.STOPPING);

               // If this Framework implements the optional Start Level Service Specification, 
               // then the start level of this Framework is moved to start level zero (0), as described in the Start Level Service Specification. 

               // All installed bundles must be stopped without changing each bundle's persistent autostart setting
               for (AbstractBundleState bundleState : getBundles())
               {
                  if (bundleState != systemBundle)
                  {
                     try
                     {
                        // [TODO] don't change the  persistent state
                        bundleState.stop();
                     }
                     catch (Exception ex)
                     {
                        // Any exceptions that occur during bundle stopping must be wrapped in a BundleException and then 
                        // published as a framework event of type FrameworkEvent.ERROR
                        fireError(bundleState, "stopping bundle", ex);
                     }
                  }
               }

               // Stop registered service plugins
               List<Plugin> reverseServicePlugins = new ArrayList<Plugin>(plugins.values());
               Collections.reverse(reverseServicePlugins);
               for (Plugin plugin : reverseServicePlugins)
               {
                  if (plugin instanceof ServicePlugin)
                  {
                     ServicePlugin servicePlugin = (ServicePlugin)plugin;
                     servicePlugin.stopService();
                  }
               }

               // Event handling is disabled
               FrameworkEventsPlugin eventsPlugin = getPlugin(FrameworkEventsPlugin.class);
               eventsPlugin.setActive(false);

               // This Framework's state is set to Bundle.RESOLVED
               systemBundle.changeState(Bundle.RESOLVED);

               // All resources held by this Framework are released
               systemBundle.destroyBundleContext();

               // Notify all threads that are waiting at waitForStop that the stop operation has completed
               stopMonitor.notifyAll();
            }
         }
      };
      executor.execute(stopcmd);

      // Wait for the stop thread
      while (stopMonitor.get() == beforeCount)
      {
         try
         {
            Thread.sleep(100);
         }
         catch (InterruptedException ex)
         {
            // ignore
         }
      }
   }

   /**
    * Wait until this Framework has completely stopped. 
    * 
    * The stop and update methods on a Framework performs an asynchronous stop of the Framework. 
    * This method can be used to wait until the asynchronous stop of this Framework has completed. 
    * This method will only wait if called when this Framework is in the Bundle.STARTING, Bundle.ACTIVE, or Bundle.STOPPING states. 
    * Otherwise it will return immediately.
    * 
    * A Framework Event is returned to indicate why this Framework has stopped.
    */
   public FrameworkEvent waitForStop(long timeout) throws InterruptedException
   {
      int state = systemBundle.getState();

      // Only wait when this Framework is in Bundle.STARTING, Bundle.ACTIVE, or Bundle.STOPPING state
      if (state != Bundle.STARTING && state != Bundle.ACTIVE && state != Bundle.STOPPING)
         return new FrameworkEvent(FrameworkEvent.STOPPED, systemBundle, null);

      long timeoutTime = System.currentTimeMillis() + timeout;
      synchronized (stopMonitor)
      {
         while (state != Bundle.RESOLVED && System.currentTimeMillis() < timeoutTime)
         {
            stopMonitor.wait(timeout);
            state = systemBundle.getState();
         }
      }

      if (System.currentTimeMillis() > timeoutTime)
         return new FrameworkEvent(FrameworkEvent.WAIT_TIMEDOUT, systemBundle, null);

      return new FrameworkEvent(FrameworkEvent.STOPPED, systemBundle, null);
   }

   /**
    * Stop the framework
    */
   public void restartFramework()
   {
      AbstractBundleState systemBundle = getSystemBundle();
      if (systemBundle.getState() != Bundle.ACTIVE)
         return;

      for (AbstractBundleState bundleState : getBundles())
      {
         if (bundleState != systemBundle && bundleState.getState() == Bundle.ACTIVE)
         {
            try
            {
               // [TODO] don't change the  persistent state
               bundleState.stop();
            }
            catch (Throwable t)
            {
               fireWarning(bundleState, "stopping bundle", t);
            }
            try
            {
               bundleState.start();
            }
            catch (Throwable t)
            {
               fireError(bundleState, "starting bundle", t);
            }
         }
      }
   }

   private URL getLocationURL(String location) throws BundleException
   {
      // Try location as URL
      URL url = null;
      try
      {
         url = new URL(location);
      }
      catch (MalformedURLException e)
      {
         // ignore
      }

      // Try location as File
      if (url == null)
      {
         try
         {
            File file = new File(location);
            if (file.exists())
               url = file.toURI().toURL();
         }
         catch (MalformedURLException e)
         {
            // ignore
         }
      }

      if (url == null)
         throw new BundleException("Unable to handle location=" + location);

      return url;
   }

   /**
    * Fire a framework error
    * 
    * @param bundle the bundle
    * @param context the msg context
    * @param t the throwable
    */
   void fireError(Bundle bundle, String context, Throwable t)
   {
      FrameworkEventsPlugin plugin = getPlugin(FrameworkEventsPlugin.class);
      if (t instanceof BundleException)
         plugin.fireFrameworkEvent(bundle, FrameworkEvent.ERROR, t);
      else if (bundle != null)
         plugin.fireFrameworkEvent(bundle, FrameworkEvent.ERROR, new BundleException("Error " + context + " bundle: " + bundle, t));
      else
         plugin.fireFrameworkEvent(systemBundle, FrameworkEvent.ERROR, new BundleException("Error " + context, t));
   }

   /**
    * Fire a framework error
    * 
    * @param bundle the bundle
    * @param context the msg context
    * @param t the throwable
    */
   void fireWarning(Bundle bundle, String context, Throwable t)
   {
      FrameworkEventsPlugin plugin = getPlugin(FrameworkEventsPlugin.class);
      if (t instanceof BundleException)
         plugin.fireFrameworkEvent(bundle, FrameworkEvent.WARNING, t);
      else if (bundle != null)
         plugin.fireFrameworkEvent(bundle, FrameworkEvent.WARNING, new BundleException("Error " + context + " bundle: " + bundle, t));
      else
         plugin.fireFrameworkEvent(systemBundle, FrameworkEvent.WARNING, new BundleException("Error " + context, t));
   }
}
