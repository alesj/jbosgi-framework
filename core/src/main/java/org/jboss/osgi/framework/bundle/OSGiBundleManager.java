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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

import org.jboss.classloading.spi.metadata.ClassLoadingMetaData;
import org.jboss.deployers.client.spi.DeployerClient;
import org.jboss.deployers.client.spi.IncompleteDeploymentException;
import org.jboss.deployers.client.spi.IncompleteDeployments;
import org.jboss.deployers.client.spi.MissingDependency;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.attachments.MutableAttachments;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.structure.spi.main.MainDeployerStructure;
import org.jboss.kernel.Kernel;
import org.jboss.logging.Logger;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.deployment.deployer.DeploymentFactory;
import org.jboss.osgi.framework.deployers.AbstractDeployment;
import org.jboss.osgi.framework.deployers.OSGiBundleActivatorDeployer;
import org.jboss.osgi.framework.metadata.OSGiMetaData;
import org.jboss.osgi.framework.metadata.ParameterizedAttribute;
import org.jboss.osgi.framework.plugins.AutoInstallPlugin;
import org.jboss.osgi.framework.plugins.BundleStoragePlugin;
import org.jboss.osgi.framework.plugins.FrameworkEventsPlugin;
import org.jboss.osgi.framework.plugins.PackageAdminPlugin;
import org.jboss.osgi.framework.plugins.Plugin;
import org.jboss.osgi.framework.plugins.ResolverPlugin;
import org.jboss.osgi.framework.plugins.ServicePlugin;
import org.jboss.osgi.framework.util.URLHelper;
import org.jboss.osgi.spi.util.BundleInfo;
import org.jboss.osgi.vfs.AbstractVFS;
import org.jboss.osgi.vfs.VFSUtils;
import org.jboss.osgi.vfs.VirtualFile;
import org.jboss.util.platform.Java;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * OSGiBundleManager.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author thomas.diesler@jboss.com
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
public class OSGiBundleManager
{
   /** The log */
   private static final Logger log = Logger.getLogger(OSGiBundleManager.class);

   /** The bundle manager's bean name: OSGiBundleManager */
   public static final String BEAN_BUNDLE_MANAGER = "OSGiBundleManager";
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
   /** The bundles by id */
   private List<AbstractBundleState> allBundles = new CopyOnWriteArrayList<AbstractBundleState>();
   /** The kernel */
   private Kernel kernel;
   /** The main deployer */
   private DeployerClient deployerClient;
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

   /**
    * Create a new OSGiBundleManager.
    * 
    * @param kernel the kernel
    * @param deployerClient the deployer client
    * @param registry the deployment registry
    * @throws IllegalArgumentException for a null parameter
    */
   public OSGiBundleManager(Kernel kernel, DeployerClient deployerClient)
   {
      this(kernel, deployerClient, null);
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
   public OSGiBundleManager(Kernel kernel, DeployerClient deployerClient, Executor executor)
   {
      if (kernel == null)
         throw new IllegalArgumentException("Null kernel");
      if (deployerClient == null)
         throw new IllegalArgumentException("Null deployerClient");
      if (deployerClient instanceof MainDeployerStructure == false)
         throw new IllegalArgumentException("Deployer client does not implement " + MainDeployerStructure.class.getName());

      this.kernel = kernel;
      this.deployerClient = deployerClient;

      // TODO thread factory
      if (executor == null)
         executor = Executors.newFixedThreadPool(10);

      this.executor = executor;
   }

   public void start()
   {
      // Create the system Bundle
      systemBundle = new OSGiSystemState(this);
      addBundle(systemBundle);
   }

   public void stop()
   {
      // nothing to do
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
   public AbstractBundleState installBundle(URL url) throws BundleException
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
   public AbstractBundleState installBundle(String location, InputStream input) throws BundleException
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
         root = AbstractVFS.getRoot(locationURL);
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
   public AbstractBundleState installBundle(VirtualFile root) throws BundleException
   {
      return install(root, root.toString(), false);
   }

   /*
    * Installs a bundle from the given virtual file.
    */
   private AbstractBundleState install(VirtualFile root, String location, boolean autoStart) throws BundleException
   {
      if (location == null)
         throw new IllegalArgumentException("Null location");

      Deployment dep;
      try
      {
         BundleInfo info = BundleInfo.createBundleInfo(root, location);
         dep = DeploymentFactory.createDeployment(info);
         dep.setAutoStart(autoStart);
      }
      catch (RuntimeException ex)
      {
         throw new BundleException("Cannot install bundle: " + root, ex);
      }

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
   public AbstractBundleState installBundle(Deployment dep) throws BundleException
   {
      // Create the deployment and deploy it
      try
      {
         org.jboss.deployers.client.spi.Deployment deployment = AbstractDeployment.createDeployment(dep.getRoot());
         MutableAttachments att = (MutableAttachments)deployment.getPredeterminedManagedObjects();
         att.addAttachment(Deployment.class, dep);

         // In case of update the OSGiBundleState is attached
         AbstractBundleState bundleState = dep.getAttachment(AbstractBundleState.class);
         if (bundleState != null)
            att.addAttachment(AbstractBundleState.class, bundleState);

         deployerClient.deploy(deployment);
         try
         {
            MainDeployerStructure deployerStructure = (MainDeployerStructure)deployerClient;
            DeploymentUnit unit = deployerStructure.getDeploymentUnit(deployment.getName());
            bundleState = unit.getAttachment(AbstractBundleState.class);
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
      catch (IncompleteDeploymentException ex)
      {
         String errorMessage = getIncompleteDeploymentInfo(dep, ex);
         throw new BundleException(errorMessage);
      }
      catch (Exception ex)
      {
         throw new BundleException("Error installing bundle from: " + dep, ex);
      }
   }

   private String getIncompleteDeploymentInfo(Deployment dep, IncompleteDeploymentException ex)
   {
      IncompleteDeployments deployments = ex.getIncompleteDeployments();

      StringWriter stringWriter = new StringWriter();
      PrintWriter printWriter = new PrintWriter(stringWriter);
      printWriter.println("Error installing bundle from: " + dep);

      // Contexts in error 
      Collection<Throwable> contextsError = deployments.getContextsInError().values();
      if (contextsError.size() > 0)
      {
         printWriter.println("\nContext Errors:");
         for (Throwable th : contextsError)
         {
            printWriter.println("\n");
            th.printStackTrace(printWriter);
         }
      }

      // Deployments in error 
      Collection<Throwable> deploymentsInError = deployments.getDeploymentsInError().values();
      if (deploymentsInError.size() > 0)
      {
         printWriter.println("\nDeployment Errors:");
         for (Throwable th : deploymentsInError)
         {
            printWriter.println("\n");
            th.printStackTrace(printWriter);
         }
      }

      // Missing Dependencies 
      Collection<Set<MissingDependency>> missingDependencies = deployments.getContextsMissingDependencies().values();
      if (missingDependencies.size() > 0)
      {
         printWriter.println("\nMissing Dependencies:");
         for (Set<MissingDependency> missDeps : missingDependencies)
         {
            for (MissingDependency missDep : missDeps)
            {
               printWriter.println("\n  " + missDep);
            }
         }
      }

      // Missing Deployers 
      Collection<String> missingDeployers = deployments.getDeploymentsMissingDeployer();
      if (missingDeployers.size() > 0)
      {
         printWriter.println("\nMissing Deployers:");
         for (String missDep : missingDeployers)
         {
            printWriter.println("\n  " + missDep);
         }
      }

      String errorMessage = stringWriter.toString();
      return errorMessage;
   }

   /**
    * Updates a bundle from an InputStream. 
    */
   public void updateBundle(AbstractDeployedBundleState bundleState, InputStream in) throws BundleException
   {
      // If the specified InputStream is null, the Framework must create the InputStream from which to read the updated bundle by interpreting, 
      // in an implementation dependent manner, this bundle's Bundle-UpdateLocation Manifest header, if present, or this bundle's original location.
      URL updateURL = bundleState.getOSGiMetaData().getBundleUpdateLocation();
      if (updateURL == null)
      {
         // This updates the bundle from its original location 
         VirtualFile root = bundleState.getRoot();
         updateURL = URLHelper.toURL(root);
      }
      if (in == null)
      {
         try
         {
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
      AbstractBundleState updatedBundleState = null;
      BundleException throwAfterUpdate = null;
      try
      {
         URL storageLocation = getBundleStorageLocation(in);
         VirtualFile root = AbstractVFS.getRoot(storageLocation);

         BundleInfo info = BundleInfo.createBundleInfo(root, location);
         Deployment dep = DeploymentFactory.createDeployment(info);
         dep.addAttachment(AbstractBundleState.class, bundleState);
         dep.setBundleUpdate(true);
         dep.setAutoStart(false);

         updatedBundleState = installBundle(dep);
         updatedBundleState.updateLastModified();
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
            if (updatedBundleState.isFragment() == false)
               startBundle((AbstractDeployedBundleState)updatedBundleState);
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
   public void uninstallBundle(AbstractDeployedBundleState bundleState) throws BundleException
   {
      long id = bundleState.getBundleId();
      if (getBundleById(id) == null)
         throw new BundleException(bundleState + " not installed");

      // If this bundle's state is ACTIVE, STARTING or STOPPING, this bundle is stopped 
      // as described in the Bundle.stop method.
      int state = bundleState.getState();
      if (state == Bundle.ACTIVE || state == Bundle.STARTING || state == Bundle.STOPPING)
      {
         try
         {
            if (bundleState.isFragment() == false)
               stopBundle((AbstractDeployedBundleState)bundleState);
         }
         catch (Exception ex)
         {
            // If Bundle.stop throws an exception, a Framework event of type FrameworkEvent.ERROR is
            // fired containing the exception
            fireError(bundleState, "Error stopping bundle: " + bundleState, ex);
         }
      }

      DeploymentException depEx = null;
      for (DeploymentUnit unit : bundleState.getDeploymentUnits())
      {
         try
         {
            deployerClient.undeploy(unit.getName());
            bundleState.updateLastModified();
         }
         catch (DeploymentException ex)
         {
            log.error("Cannot undeploy: " + unit.getName(), depEx = ex);
         }
      }

      // Rethrow deployment exception 
      if (depEx != null)
      {
         Throwable cause = depEx.getCause();
         if (cause instanceof BundleException)
            throw (BundleException)cause;

         throw new BundleException("Unable to uninstall " + bundleState, cause);
      }
   }

   /**
    * Add a deployment to the manager, which creates the bundle state.
    * 
    * Note, the bundle state is not yet added to the manager.
    */
   public AbstractBundleState addDeployment(DeploymentUnit unit)
   {
      if (unit == null)
         throw new IllegalArgumentException("Null unit");

      // In case of Bundle.update() the OSGiBundleState is attached
      AbstractBundleState absBundle = unit.getAttachment(AbstractBundleState.class);
      if (absBundle != null)
      {
         // Add the DeploymentUnit to the OSGiBundleState
         OSGiBundleState depBundle = (OSGiBundleState)absBundle;
         depBundle.addDeploymentUnit(unit);
      }
      else
      {
         OSGiMetaData osgiMetaData = unit.getAttachment(OSGiMetaData.class);
         ParameterizedAttribute fragmentHost = osgiMetaData.getFragmentHost();
         if (fragmentHost != null)
         {
            // Create a new OSGiFragmentState
            OSGiFragmentState fragmentState = new OSGiFragmentState(this, unit);
            absBundle = fragmentState;
         }
         else
         {
            // Create a new OSGiBundleState
            AbstractDeployedBundleState bundleState = new OSGiBundleState(this, unit);
            absBundle = bundleState;
         }
      }

      // Attach the abstract bundle state
      unit.addAttachment(AbstractBundleState.class, absBundle);
      
      if (absBundle.isFragment())
         unit.addAttachment(OSGiFragmentState.class, (OSGiFragmentState)absBundle);
      else
         unit.addAttachment(OSGiBundleState.class, (OSGiBundleState)absBundle);
      
      return absBundle;
   }

   /**
    * Add a bundle to the manager.
    * 
    * Note, the bundle must be metadata complete when it is added to the manager.
    * An extender might pickup the INSTALLED event and use PackageAdmin to examine the 
    * exported packages for example.
    * 
    * @param bundleState the bundle state
    * @throws IllegalArgumentException for a null bundle state
    */
   public void addBundle(AbstractBundleState bundleState)
   {
      if (bundleState == null)
         throw new IllegalArgumentException("Null bundle state");

      // Do nothing if this is a bundle update
      if (bundleState instanceof OSGiBundleState)
      {
         DeploymentUnit unit = ((AbstractDeployedBundleState)bundleState).getDeploymentUnit();
         if (unit.getAttachment(ClassLoadingMetaData.class) == null)
            throw new IllegalStateException("Cannot obtain ClassLoadingMetaData");
            
         Deployment dep = unit.getAttachment(Deployment.class);
         if (dep != null && dep.isBundleUpdate())
            return;
      }

      validateBundle(bundleState);
      
      allBundles.add(bundleState);
      try
      {
         bundleState.changeState(Bundle.INSTALLED, true);

         // Add the bundle to the resolver
         // Note, plugins are not registered when the system bundle is added 
         ResolverPlugin bundleResolver = getOptionalPlugin(ResolverPlugin.class);
         if (bundleResolver != null)
            bundleResolver.addBundle(bundleState);
      }
      catch (RuntimeException rte)
      {
         allBundles.remove(bundleState);
         throw rte;
      }
      log.debug("Added: " + bundleState);
   }

   /**
    * Validate the bundle
    * 
    * @param bundleState the bundle state
    */
   private void validateBundle(AbstractBundleState bundleState)
   {
      OSGiMetaData osgiMetaData = bundleState.getOSGiMetaData();
      if (osgiMetaData == null)
         return;

      OSGiBundleValidator validator;
      
      // Delegate to the validator for the appropriate revision
      if (osgiMetaData.getBundleManifestVersion() > 1)
         validator = new OSGiBundleValidatorR4(this);
      else
         validator = new OSGiBundleValidatorR3(this);

      validator.validateBundle(bundleState);
   }

   /**
    * Remove a bundle
    * 
    * @param bundleState the bundle state
    * @throws IllegalArgumentException for a null bundle state
    */
   public void removeBundle(AbstractBundleState bundleState)
   {
      if (bundleState == null)
         throw new IllegalArgumentException("Null bundle state");

      bundleState.uninstallInternal();

      // Remove the bundle from the resolver
      ResolverPlugin bundleResolver = getOptionalPlugin(ResolverPlugin.class);
      if (bundleResolver != null)
         bundleResolver.removeBundle(bundleState);

      allBundles.remove(bundleState);
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

      AbstractDeployedBundleState bundleState = (AbstractDeployedBundleState)getBundleById(id);
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
      for (AbstractBundleState aux : allBundles)
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
      for (AbstractBundleState aux : allBundles)
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

      for (AbstractBundleState aux : allBundles)
      {
         String auxLocation = aux.getLocation();
         if (location.equals(auxLocation))
         {
            result = aux;
            break;
         }

         // Fallback to the deployment name
         else if (aux instanceof AbstractDeployedBundleState)
         {
            DeploymentUnit unit = ((AbstractDeployedBundleState)aux).getDeploymentUnit();
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
      List<AbstractBundleState> bundles = new ArrayList<AbstractBundleState>();
      for (AbstractBundleState aux : allBundles)
      {
         bundles.add(aux);
      }
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
      for (AbstractBundleState aux : allBundles)
      {
         if (aux.getState() == state)
            bundles.add(aux);
      }
      return Collections.unmodifiableList(bundles);
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
   public void startBundle(AbstractDeployedBundleState bundleState) throws BundleException
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

            // Rethrow the attached BundleException
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
   public void stopBundle(AbstractDeployedBundleState bundleState) throws BundleException
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
            // Rethrow the attached BundleException
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
   Class<?> loadClassFailsafe(Bundle bundle, String clazz)
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
