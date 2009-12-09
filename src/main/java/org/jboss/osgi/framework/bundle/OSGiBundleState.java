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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.logging.Logger;
import org.jboss.osgi.framework.metadata.OSGiMetaData;
import org.jboss.osgi.framework.plugins.PackageAdminPlugin;
import org.jboss.virtual.VirtualFile;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

/**
 * BundleState.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author Thomas.Diesler@jboss.com
 * @version $Revision: 1.1 $
 */
public class OSGiBundleState extends AbstractBundleState
{
   /** The log */
   private static final Logger log = Logger.getLogger(OSGiBundleState.class);

   /** Used to generate a unique id */
   private static final AtomicLong bundleIDGenerator = new AtomicLong();

   /** The bundle id */
   private long bundleId;

   /** The bundle location */
   private String location;

   /** The deployment unit */
   private DeploymentUnit unit;
   
   /**
    * Create a new BundleState.
    * 
    * @param location The string representation of this bundle's location identifier 
    * @param osgiMetaData the osgi metadata
    * @param unit the deployment unit
    * @throws IllegalArgumentException for a null parameter
    */
   public OSGiBundleState(String location, OSGiMetaData osgiMetaData, DeploymentUnit unit)
   {
      super(osgiMetaData);
      
      if (location == null)
         throw new IllegalArgumentException("Null bundle location");
      if (unit == null)
          throw new IllegalArgumentException("Null deployment unit");
      
      this.unit = unit;
      this.location = location;
      
      this.bundleId = bundleIDGenerator.incrementAndGet();
      unit.getMutableMetaData().addMetaData(unit, DeploymentUnit.class);
   }

   public long getBundleId()
   {
      return bundleId;
   }

   /**
    * Get the unit.
    * 
    * @return the unit.
    */
   public DeploymentUnit getDeploymentUnit()
   {
      return unit;
   }

   public String getLocation()
   {
      return location;
   }

   public URL getEntry(String path)
   {
      checkInstalled();
      if (noAdminPermission(AdminPermission.RESOURCE))
         return null;

      DeploymentUnit unit = getDeploymentUnit();
      if (unit instanceof VFSDeploymentUnit)
      {
         VFSDeploymentUnit vfsDeploymentUnit = (VFSDeploymentUnit)unit;

         if (path.startsWith("/"))
            path = path.substring(1);
         return vfsDeploymentUnit.getResourceLoader().getResource(path);
      }
      return null;
   }

   @SuppressWarnings("unchecked")
   public Enumeration getEntryPaths(String path)
   {
      checkInstalled();
      if (noAdminPermission(AdminPermission.RESOURCE))
         return null;

      DeploymentUnit unit = getDeploymentUnit();
      if (unit instanceof VFSDeploymentUnit)
      {
         VFSDeploymentUnit vfsDeploymentUnit = (VFSDeploymentUnit)unit;
         VirtualFile root = vfsDeploymentUnit.getRoot();
         if (path.startsWith("/"))
            path = path.substring(1);
         try
         {
            VirtualFile child = root.getChild(path);
            if (child != null)
               return new VFSEntryPathsEnumeration(root, child);
         }
         catch (IOException e)
         {
            throw new RuntimeException("Error determining entry paths for " + root + " path=" + path);
         }

      }
      return null;
   }

   @SuppressWarnings("unchecked")
   public Enumeration findEntries(String path, String filePattern, boolean recurse)
   {
      if (path == null)
         throw new IllegalArgumentException("Null path");

      checkInstalled();
      if (noAdminPermission(AdminPermission.RESOURCE))
         return null;

      // [TODO] fragments
      resolveBundle();

      if (filePattern == null)
         filePattern = "*";

      DeploymentUnit unit = getDeploymentUnit();
      if (unit instanceof VFSDeploymentUnit)
      {
         VFSDeploymentUnit vfsDeploymentUnit = (VFSDeploymentUnit)unit;
         VirtualFile root = vfsDeploymentUnit.getRoot();
         if (path.startsWith("/"))
            path = path.substring(1);
         try
         {
            VirtualFile child = root.getChild(path);
            if (child != null)
               return new VFSFindEntriesEnumeration(root, child, filePattern, recurse);
         }
         catch (IOException e)
         {
            throw new RuntimeException("Error finding entries for " + root + " path=" + path + " pattern=" + filePattern + " recurse=" + recurse);
         }

      }
      return null;
   }

   public Class<?> loadClass(String name) throws ClassNotFoundException
   {
      checkInstalled();
      checkAdminPermission(AdminPermission.CLASS);
      // [TODO] bundle fragment

      if (resolveBundle() == false)
         throw new ClassNotFoundException("Cannot load class: " + name);

      ClassLoader classLoader = getDeploymentUnit().getClassLoader();
      return classLoader.loadClass(name);
   }

   /**
    * Try to resolve the bundle
    * @return true when resolved
    */
   boolean resolveBundle()
   {
      PackageAdminPlugin packageAdmin = getBundleManager().getPlugin(PackageAdminPlugin.class);
      return packageAdmin.resolveBundles(new Bundle[] { this });
   }

   public URL getResource(String name)
   {
      checkInstalled();
      if (noAdminPermission(AdminPermission.RESOURCE))
         return null;
      
      // [TODO] bundle fragment
      if (resolveBundle() == false)
         return getDeploymentUnit().getResourceLoader().getResource(name);
      
      return getDeploymentUnit().getClassLoader().getResource(name);
   }

   @SuppressWarnings("unchecked")
   public Enumeration getResources(String name) throws IOException
   {
      checkInstalled();
      if (noAdminPermission(AdminPermission.RESOURCE))
         return null;

      // [TODO] bundle fragment 
      if (resolveBundle() == false)
         return getDeploymentUnit().getResourceLoader().getResources(name);
      
      return getDeploymentUnit().getClassLoader().getResources(name);
   }

   // [TODO] options
   public void start(int options) throws BundleException
   {
      checkInstalled();
      checkAdminPermission(AdminPermission.EXECUTE);

      if (getState() == ACTIVE)
         return;

      getBundleManager().startBundle(this);
   }

   // [TODO] options
   public void stop(int options) throws BundleException
   {
      checkInstalled();
      checkAdminPermission(AdminPermission.EXECUTE);

      if (getState() != ACTIVE)
         return;

      getBundleManager().stopBundle(this);
   }

   /**
    * Start internal
    * 
    * [TODO] Start Level Service & START_TRANSIENT? 
    * [TODO] START_ACTIVATION_POLICY 
    * [TODO] LAZY_ACTIVATION 
    * [TODO] locks 
    * [TODO] options
    * 
    * @throws Throwable for any error
    */
   public void startInternal() throws Throwable
   {
      // Bundle extenders catch the STARTING event and might expect a valid context
      createBundleContext();
      changeState(STARTING);

      try
      {
         OSGiMetaData metaData = getOSGiMetaData();
         if (metaData == null)
            throw new IllegalStateException("Cannot obtain OSGi meta data");

         // Do we have a bundle activator
         String bundleActivatorClassName = metaData.getBundleActivator();
         if (bundleActivatorClassName != null)
         {
            Object result = loadClass(bundleActivatorClassName).newInstance();
            if (result instanceof BundleActivator == false)
               throw new BundleException(bundleActivatorClassName + " is not an implementation of " + BundleActivator.class.getName());

            BundleActivator bundleActivator = (BundleActivator)result;
            unit.addAttachment(BundleActivator.class, bundleActivator);

            bundleActivator.start(getBundleContext());
         }

         if (getState() != STARTING)
            throw new BundleException("Bundle has been uninstalled: " + this);

         changeState(ACTIVE);
      }
      catch (Throwable t)
      {
         changeState(STOPPING);
         // TODO stop the bundle
         destroyBundleContext();
         changeState(RESOLVED);
         throw t;
      }
   }

   /**
    * Stop Internal
    * 
    * [TODO] Start Level Service & STOP_TRANSIENT? [TODO] locks [TODO] options
    * 
    * @throws Throwable for any error
    */
   public void stopInternal() throws Throwable
   {
      changeState(STOPPING);

      Throwable rethrow = null;
      try
      {
         BundleActivator bundleActivator = getDeploymentUnit().getAttachment(BundleActivator.class);
         BundleContext bundleContext = getBundleContext();
         if (bundleActivator != null && bundleContext != null)
         {
            try
            {
               bundleActivator.stop(bundleContext);
            }
            catch (Throwable t)
            {
               rethrow = t;
            }
         }

         for (OSGiServiceState service : registeredServices)
         {
            try
            {
               service.unregister();
            }
            catch (Throwable t)
            {
               log.debug("Error unregistering service: " + service, t);
            }
         }

         for (Map.Entry<OSGiServiceState, Integer> inUse : servicesInUse.entrySet())
         {
            OSGiServiceState service = inUse.getKey();
            Integer count = inUse.getValue();
            for (int i = 0; i < count; ++i)
            {
               try
               {
                  getBundleManager().ungetService(this, service);
               }
               catch (Throwable t)
               {
                  log.debug("Error ungetting service: " + service, t);
               }
            }
         }

         if (getState() != STOPPING)
            throw new BundleException("Bundle has been uninstalled: " + getCanonicalName());
      }
      finally
      {
         if (getState() == STOPPING)
            changeState(RESOLVED);
         destroyBundleContext();
         getDeploymentUnit().removeAttachment(BundleActivator.class);
      }

      if (rethrow != null)
         throw rethrow;
   }

   public void update(InputStream in) throws BundleException
   {
      checkAdminPermission(AdminPermission.LIFECYCLE); // [TODO] extension bundles
      // [TODO] update
      throw new UnsupportedOperationException("update");
   }

   public void uninstall() throws BundleException
   {
      checkAdminPermission(AdminPermission.LIFECYCLE); // [TODO] extension bundles
      getBundleManager().uninstallBundle(this);
   }

   public static OSGiBundleState assertBundleState(Bundle bundle)
   {
      if (bundle == null)
         throw new IllegalArgumentException("Null bundle");
      
      if (bundle instanceof OSGiBundleWrapper)
         bundle = ((OSGiBundleWrapper)bundle).getBundleState();
   
      if (bundle instanceof OSGiBundleState == false)
         throw new IllegalArgumentException("Not an OSGiBundleState: " + bundle);
   
      return (OSGiBundleState)bundle;
   }
}
