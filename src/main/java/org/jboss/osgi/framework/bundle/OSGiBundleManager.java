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
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;

import org.jboss.dependency.spi.Controller;
import org.jboss.dependency.spi.ControllerContext;
import org.jboss.dependency.spi.ControllerState;
import org.jboss.deployers.client.spi.DeployerClient;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.attachments.MutableAttachments;
import org.jboss.deployers.spi.deployer.DeploymentStage;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.structure.spi.main.MainDeployerStructure;
import org.jboss.deployers.vfs.spi.client.VFSDeployment;
import org.jboss.deployers.vfs.spi.client.VFSDeploymentFactory;
import org.jboss.kernel.Kernel;
import org.jboss.logging.Logger;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.deployment.deployer.DeploymentFactory;
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
import org.jboss.util.collection.ConcurrentSet;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.VirtualFile;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
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

   /** The string representation of this bundle's location identifier. */
   public static final String PROPERTY_BUNDLE_LOCATION = "org.jboss.osgi.bundle.location";

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

   /** The registered services */
   private Set<OSGiServiceState> registeredServices = new ConcurrentSet<OSGiServiceState>();

   /** The kernel */
   private Kernel kernel;

   /** The main deployer */
   private DeployerClient deployerClient;

   /** The deployment structure */
   private MainDeployerStructure deployerStructure;

   /** The executor */
   private Executor executor;

   /** The system bundle */
   private OSGiSystemState systemBundle;

   /** The registered manager plugins */
   private Map<Class<?>, Plugin> plugins = Collections.synchronizedMap(new LinkedHashMap<Class<?>, Plugin>());

   /** The frame work properties */
   private Map<String, Object> properties = new ConcurrentHashMap<String, Object>();

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
      this.deployerStructure = (MainDeployerStructure)deployerClient;

      // TODO thread factory
      if (executor == null)
         executor = Executors.newFixedThreadPool(10);

      this.executor = executor;
   }

   public void start()
   {
      // createSystemBundle
      Manifest manifest = new Manifest();
      Attributes attributes = manifest.getMainAttributes();
      attributes.put(new Name(Constants.BUNDLE_NAME), Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
      attributes.put(new Name(Constants.BUNDLE_SYMBOLICNAME), Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
      OSGiMetaData systemMetaData = new AbstractOSGiMetaData(manifest);
      addBundle(systemBundle = new OSGiSystemState(this, systemMetaData));
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
   public void setProperties(Map<String, Object> properties)
   {
      this.properties = properties;

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
    */
   private Class<?> getPluginKey(Plugin plugin)
   {
      Class<?> clazz = plugin.getClass();
      Class<?> key = getPluginKey(plugin, clazz);

      // If the plugin could not be added by Interface, use the clazz directly
      return (key != null ? key : clazz);
   }

   /**
    * Get the plugin key by scanning the interfaces for the given clazz.
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
         try
         {
            BundleStoragePlugin plugin = getPlugin(BundleStoragePlugin.class);
            String path = plugin.getStorageDir(getSystemBundle()).getCanonicalPath();

            // [TODO] do properly
            File file = new File(path + "/bundle-" + System.currentTimeMillis() + ".jar");
            FileOutputStream fos = new FileOutputStream(file);
            VFSUtils.copyStream(input, fos);
            fos.close();

            locationURL = file.toURI().toURL();
         }
         catch (IOException ex)
         {
            throw new BundleException("Cannot store bundle from input stream", ex);
         }
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
   public AbstractBundleState installBundle(Deployment dep) throws BundleException
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

      DeploymentUnit unit = bundleState.getDeploymentUnit();
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

      OSGiMetaData osgiMetaData = unit.getAttachment(OSGiMetaData.class);
      if (osgiMetaData == null)
      {
         Manifest manifest = unit.getAttachment(Manifest.class);
         // [TODO] we need a mechanism to construct an OSGiMetaData from an easier factory
         if (manifest == null)
            manifest = new Manifest();
         // [TODO] populate some bundle information
         Attributes attributes = manifest.getMainAttributes();
         attributes.put(new Name(Constants.BUNDLE_NAME), unit.getName());
         attributes.put(new Name(Constants.BUNDLE_SYMBOLICNAME), unit.getName());
         osgiMetaData = new AbstractOSGiMetaData(manifest);
      }

      String location = (String)unit.getAttachment(PROPERTY_BUNDLE_LOCATION);
      if (location == null)
         location = unit.getName();

      OSGiBundleState bundleState = new OSGiBundleState(location, osgiMetaData, unit);
      addBundle(bundleState);
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

      bundleState.changeState(Bundle.INSTALLED);

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
    * Start a bundle
    * 
    * @param bundleState the bundle state
    * @throws BundleException the bundle exception
    */
   public void startBundle(OSGiBundleState bundleState) throws BundleException
   {
      // Resolve all INSTALLED bundles through the PackageAdmin
      PackageAdmin packageAdmin = getPlugin(PackageAdminPlugin.class);
      packageAdmin.resolveBundles(null);

      try
      {
         String name = bundleState.getDeploymentUnit().getName();
         deployerClient.change(name, DeploymentStages.INSTALLED);
         deployerClient.checkComplete(name);
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
    * @param bundleState the bundle state
    * @throws BundleException the bundle exception
    */
   public void stopBundle(OSGiBundleState bundleState) throws BundleException
   {
      try
      {
         String name = bundleState.getDeploymentUnit().getName();
         deployerClient.change(name, DeploymentStages.CLASSLOADER);
      }
      catch (DeploymentException e)
      {
         Throwable t = e.getCause();
         if (t instanceof BundleException)
            throw (BundleException)t;
         throw new BundleException("Error stopping " + bundleState, e);
      }
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
   Collection<OSGiServiceState> getServices(AbstractBundleState bundle, String clazz, Filter filter, boolean checkAssignable)
   {
      if (filter == null)
         filter = NoFilter.INSTANCE;

      if (registeredServices.isEmpty())
         return null;

      // Don't check assignabilty for the system bundle
      if (bundle.getBundleId() == 0)
         checkAssignable = false;

      // review: optimise this, e.g. index by class
      // Use a sorted set to order services according to spec
      Set<OSGiServiceState> result = new TreeSet<OSGiServiceState>(ServiceComparator.INSTANCE);
      for (OSGiServiceState service : registeredServices)
      {
         // Check the state, filter and permission
         if (service.isUnregistered() == false && filter.match(service) && service.hasPermission())
         {
            // Check any passed class matches
            if (clazz == null || service.matchClass(clazz))
            {
               // Check the assignability
               if (checkAssignable == false || service.isAssignable(bundle))
                  result.add(service);
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
      Collection<OSGiServiceState> services = getServices(bundle, clazz, null, true);
      if (services == null || services.isEmpty())
         return null;

      // If multiple such services exist, the service with the highest ranking (as specified in its SERVICE_RANKING property) is returned.
      // If there is a tie in ranking, the service with the lowest service ID (as specified in its SERVICE_ID property); 
      // that is, the service that was registered first is returned.

      long bestId = 0;
      int bestRanking = 0;
      ServiceReference bestMatch = null;
      for (OSGiServiceState service : services)
      {
         long id = service.getServiceId();
         int ranking = service.getServiceRanking();
         if (bestMatch == null || ranking > bestRanking || (ranking == bestRanking && id < bestId))
         {
            bestMatch = service.getReferenceInternal();
            bestRanking = ranking;
            bestId = id;
         }
      }

      return bestMatch;
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
      Collection<OSGiServiceState> services = getServices(bundle, clazz, filter, checkAssignable);
      if (services == null || services.isEmpty())
         return null;

      ServiceReference[] result = new ServiceReference[services.size()];

      int i = 0;
      for (OSGiServiceState service : services)
         result[i++] = service.getReferenceInternal();

      return result;
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
      registeredServices.add(result);

      try
      {
         Controller controller = kernel.getController();
         controller.install(result);
      }
      catch (Throwable t)
      {
         fireError(bundleState, "installing service to MC in", t);

         registeredServices.remove(result);
         result.internalUnregister();

         throw new RuntimeException(t);
      }

      FrameworkEventsPlugin plugin = getPlugin(FrameworkEventsPlugin.class);
      plugin.fireServiceEvent(bundleState, ServiceEvent.REGISTERED, result);

      return result;
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

      FrameworkEventsPlugin plugin = getPlugin(FrameworkEventsPlugin.class);
      plugin.fireServiceEvent(serviceState.getBundleState(), ServiceEvent.UNREGISTERING, serviceState);

      registeredServices.remove(serviceState);
      serviceState.internalUnregister();
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
      OSGiServiceReferenceWrapper serviceReference = (OSGiServiceReferenceWrapper)reference;
      OSGiServiceState serviceState = serviceReference.getServiceState();
      return getService(bundleState, serviceState);
   }

   /**
    * Get a service
    *
    * @param bundleState the bundle that requests the service
    * @param serviceState the service state
    * @return the service
    */
   Object getService(AbstractBundleState bundleState, OSGiServiceState serviceState)
   {
      Object result = serviceState.getService(bundleState);
      if (result != null)
         bundleState.addServiceInUse(serviceState);
      return result;
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
      OSGiServiceReferenceWrapper serviceReference = (OSGiServiceReferenceWrapper)reference;
      OSGiServiceState serviceState = serviceReference.getServiceState();
      return ungetService(bundleState, serviceState);
   }

   /**
    * Unget a service
    * 
    * @param bundleState the bundle state
    * @param service the service
    * @return true when the service is still in use by the bundle
    */
   boolean ungetService(AbstractBundleState bundleState, OSGiServiceState service)
   {
      return bundleState.removeServiceInUse(service);
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
    * Init the Framework
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

      // [TODO] Be at start level 0

      // [TODO] Have event handling enabled

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

      // Create the system bundle context
      systemBundle.createBundleContext();

      // Start registered service plugins
      for (Plugin plugin : plugins.values())
      {
         if (plugin instanceof ServicePlugin)
         {
            ServicePlugin servicePlugin = (ServicePlugin)plugin;
            servicePlugin.startService();
         }
      }

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
    * Stop the framework
    */
   public void stopFramework()
   {
      AbstractBundleState systemBundle = getSystemBundle();
      if (systemBundle.getState() != Bundle.ACTIVE)
         return;

      systemBundle.changeState(Bundle.STOPPING);
      for (AbstractBundleState bundleState : getBundles())
      {
         if (bundleState != systemBundle)
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

      systemBundle.changeState(Bundle.RESOLVED);
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

   /**
    * ServiceComparator, reverses the order of compareTo
    */
   static class ServiceComparator implements Comparator<OSGiServiceState>
   {
      public static ServiceComparator INSTANCE = new ServiceComparator();

      public int compare(OSGiServiceState o1, OSGiServiceState o2)
      {
         return -o1.compareTo(o2);
      }
   }
}
