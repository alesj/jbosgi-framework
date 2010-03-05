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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentResourceLoader;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.framework.deployers.AbstractDeployment;
import org.jboss.osgi.framework.metadata.OSGiMetaData;
import org.jboss.osgi.framework.plugins.PackageAdminPlugin;
import org.jboss.osgi.vfs.VirtualFile;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * The abstract state of a user deployed {@link Bundle} or Fragment.
 * 
 * @author Thomas.Diesler@jboss.com
 * @since 25-Dec-2009
 */
public abstract class AbstractDeployedBundleState extends AbstractBundleState
{
   /** Used to generate a unique id */
   private static final AtomicLong bundleIDGenerator = new AtomicLong();

   /** The bundle id */
   private long bundleId;

   /** The bundle location */
   private String location;

   /** The bundle root file */
   private VirtualFile rootFile;

   /** The list of deployment units */
   private List<DeploymentUnit> units = new ArrayList<DeploymentUnit>();

   /** The headers localized with the default locale */
   Dictionary<String, String> headersOnUninstall;

   /**
    * Create a new BundleState.
    * @throws IllegalArgumentException for a null parameter
    */
   public AbstractDeployedBundleState(OSGiBundleManager bundleManager, DeploymentUnit unit)
   {
      super(bundleManager);

      if (unit == null)
         throw new IllegalArgumentException("Null deployment unit");

      // The bundle location is not necessarily the bundle root url
      // The framework is expected to preserve the location passed into installBundle(String)
      Deployment dep = unit.getAttachment(Deployment.class);
      location = (dep != null ? dep.getLocation() : unit.getName());
      rootFile = (dep != null ? dep.getRoot() : AbstractDeployment.getRoot(unit));

      bundleId = bundleIDGenerator.incrementAndGet();

      addDeploymentUnit(unit);
   }

   /**
    * Get the root file for this bundle 
    */
   public VirtualFile getRoot()
   {
      return rootFile;
   }

   @Override
   public OSGiMetaData getOSGiMetaData()
   {
      DeploymentUnit unit = getDeploymentUnit();
      OSGiMetaData osgiMetaData = unit.getAttachment(OSGiMetaData.class);
      return osgiMetaData;
   }

   public long getBundleId()
   {
      return bundleId;
   }

   /**
    * Get the DeploymentUnit that was added last.
    * 
    * Initially, an OSGiBundleState is associated with just one DeploymentUnit.
    * A sucessful call to {@link #update()} or its variants pushes an additional
    * DeploymentUnit to the stack.   
    * 
    * @return the unit that corresponds to the last sucessful update.
    */
   public DeploymentUnit getDeploymentUnit()
   {
      int index = (units.size() - 1);
      return units.get(index);
   }

   /**
    * Add a DeploymentUnit to the list.
    * 
    * @see {@link OSGiBundleManager#updateBundle(DeployedBundleState, InputStream)}
    */
   void addDeploymentUnit(DeploymentUnit unit)
   {
      unit.getMutableMetaData().addMetaData(unit, DeploymentUnit.class);
      units.add(unit);
   }

   /**
    * Get the list of DeploymentUnits.
    * 
    * @see {@link OSGiBundleManager#uninstallBundle(DeployedBundleState)}
    */
   List<DeploymentUnit> getDeploymentUnits()
   {
      return Collections.unmodifiableList(units);
   }

   public String getLocation()
   {
      return location;
   }

   @Override
   public Dictionary<String, String> getHeaders(String locale)
   {
      // This method must continue to return Manifest header information while this bundle is in the UNINSTALLED state, 
      // however the header values must only be available in the raw and default locale values
      if (getState() == Bundle.UNINSTALLED)
         return headersOnUninstall;

      return super.getHeaders(locale);
   }

   public URL getEntry(String path)
   {
      checkInstalled();
      if (noAdminPermission(AdminPermission.RESOURCE))
         return null;

      return getEntryInternal(path);
   }

   @SuppressWarnings("rawtypes")
   public Enumeration getEntryPaths(String path)
   {
      checkInstalled();
      if (noAdminPermission(AdminPermission.RESOURCE))
         return null;

      try
      {
         return rootFile.getEntryPaths(path);
      }
      catch (IOException e)
      {
         throw new RuntimeException("Error determining entry paths for " + rootFile + " path=" + path);
      }
   }

   @Override
   protected URL getEntryInternal(String path)
   {
      VFSDeploymentUnit unit = (VFSDeploymentUnit)getDeploymentUnit();
      if (path.startsWith("/"))
         path = path.substring(1);

      VFSDeploymentResourceLoader loader = unit.getResourceLoader();
      URL resource = loader.getResource(path);
      return resource;
   }

   public void uninstall() throws BundleException
   {
      checkAdminPermission(AdminPermission.LIFECYCLE);

      // If this bundle's state is UNINSTALLED then an IllegalStateException is thrown
      if (getState() == Bundle.UNINSTALLED)
         throw new IllegalStateException("Bundle already uninstalled: " + this);

      // Cache the headers in the default locale 
      headersOnUninstall = getHeaders(null);

      getBundleManager().uninstallBundle(this);
   }

   @SuppressWarnings("rawtypes")
   public Enumeration findEntries(String path, String pattern, boolean recurse)
   {
      if (path == null)
         throw new IllegalArgumentException("Null path");

      checkInstalled();
      if (noAdminPermission(AdminPermission.RESOURCE))
         return null;

      resolveBundle();

      try
      {
         return rootFile.findEntries(path, pattern, recurse);
      }
      catch (IOException e)
      {
         throw new RuntimeException("Error finding entries for " + rootFile + " path=" + path + " pattern=" + pattern + " recurse=" + recurse);
      }
   }

   /**
    * Try to resolve the bundle
    * @return true when resolved
    */
   protected boolean resolveBundle()
   {
      PackageAdminPlugin packageAdmin = getBundleManager().getPlugin(PackageAdminPlugin.class);
      return packageAdmin.resolveBundles(new Bundle[] { this });
   }
}
