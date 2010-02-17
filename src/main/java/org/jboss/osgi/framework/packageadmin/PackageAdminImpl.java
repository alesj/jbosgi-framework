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
package org.jboss.osgi.framework.packageadmin;

//$Id: StartLevelImpl.java 93118 2009-09-02 08:24:44Z thomas.diesler@jboss.com $

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jboss.classloading.plugins.metadata.PackageCapability;
import org.jboss.classloading.spi.dependency.ClassLoading;
import org.jboss.classloading.spi.dependency.Module;
import org.jboss.classloading.spi.metadata.CapabilitiesMetaData;
import org.jboss.classloading.spi.metadata.Capability;
import org.jboss.classloading.spi.metadata.ClassLoadingMetaData;
import org.jboss.deployers.plugins.classloading.AbstractDeploymentClassLoaderPolicyModule;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.logging.Logger;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.bundle.AbstractDeployedBundleState;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.bundle.OSGiBundleState;
import org.jboss.osgi.framework.bundle.OSGiFragmentState;
import org.jboss.osgi.framework.bundle.OSGiSystemState;
import org.jboss.osgi.framework.plugins.PackageAdminPlugin;
import org.jboss.osgi.framework.plugins.ResolverPlugin;
import org.jboss.osgi.framework.plugins.SystemPackagesPlugin;
import org.jboss.osgi.framework.plugins.internal.AbstractServicePlugin;
import org.jboss.osgi.framework.resolver.Resolver;
import org.jboss.osgi.framework.resolver.ResolverBundle;
import org.jboss.osgi.spi.NotImplementedException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;

/**
 * An implementation of the {@link PackageAdmin}.
 * 
 * [TODO] [JBOSGI-149] Fully implement PackageAdmin
 * 
 * @author thomas.diesler@jboss.com
 * @since 03-Sep-2009
 */
public class PackageAdminImpl extends AbstractServicePlugin implements PackageAdminPlugin
{
   /** The log */
   private static final Logger log = Logger.getLogger(PackageAdminImpl.class);

   public PackageAdminImpl(OSGiBundleManager bundleManager)
   {
      super(bundleManager);
   }

   public void startService()
   {
      BundleContext sysContext = getSystemContext();
      sysContext.registerService(PackageAdmin.class.getName(), this, null);
   }

   public void stopService()
   {
   }

   @SuppressWarnings("rawtypes")
   public Bundle getBundle(Class clazz)
   {
      if (clazz == null)
         throw new IllegalArgumentException("Null class");

      ClassLoader classLoader = clazz.getClassLoader();
      Module module = ClassLoading.getModuleForClassLoader(classLoader);
      if (module instanceof AbstractDeploymentClassLoaderPolicyModule)
      {
         AbstractDeploymentClassLoaderPolicyModule deploymentModule = (AbstractDeploymentClassLoaderPolicyModule)module;
         DeploymentUnit unit = deploymentModule.getDeploymentUnit();
         AbstractBundleState bundleState = unit.getAttachment(AbstractBundleState.class);
         if (bundleState != null)
         {
            // Return the fragment's host bundle
            if (bundleState.isFragment())
               bundleState = ((OSGiFragmentState)bundleState).getFragmentHost();

            return bundleState.getBundleInternal();
         }
      }
      return null;
   }

   public int getBundleType(Bundle bundle)
   {
      AbstractBundleState bundleState = AbstractBundleState.assertBundleState(bundle);
      return bundleState.isFragment() ? BUNDLE_TYPE_FRAGMENT : 0;
   }

   public Bundle[] getBundles(String symbolicName, String versionRange)
   {
      throw new NotImplementedException();
   }

   public ExportedPackage getExportedPackage(String name)
   {
      throw new NotImplementedException();
   }

   public ExportedPackage[] getExportedPackages(Bundle bundle)
   {
      List<ExportedPackage> exported = new ArrayList<ExportedPackage>();

      AbstractBundleState absBundleState = AbstractBundleState.assertBundleState(bundle);
      if (absBundleState instanceof OSGiSystemState)
      {
         OSGiSystemState bundleState = (OSGiSystemState)absBundleState;
         SystemPackagesPlugin plugin = bundleState.getBundleManager().getPlugin(SystemPackagesPlugin.class);

         // [TODO] include package versions
         for (String packageName : plugin.getSystemPackages(false))
         {
            exported.add(new SystemExportedPackage(bundleState, packageName));
         }
      }
      else
      {
         AbstractDeployedBundleState bundleState = (AbstractDeployedBundleState)absBundleState;
         ClassLoadingMetaData metaData = bundleState.getDeploymentUnit().getAttachment(ClassLoadingMetaData.class);
         if (metaData == null)
            throw new IllegalStateException("Cannot obtain ClassLoadingMetaData");

         CapabilitiesMetaData capabilities = metaData.getCapabilities();
         for (Capability capability : capabilities.getCapabilities())
         {
            if (capability instanceof PackageCapability)
            {
               exported.add(new CapabilityExportedPackage(bundleState, (PackageCapability)capability));
            }
         }
      }

      if (exported.size() == 0)
         return null;

      ExportedPackage[] result = new ExportedPackage[exported.size()];
      exported.toArray(result);

      return result;
   }

   public ExportedPackage[] getExportedPackages(String name)
   {
      List<ExportedPackage> exported = new ArrayList<ExportedPackage>();
      
      for (AbstractBundleState auxBundle : getBundleManager().getBundles())
      {
         ExportedPackage[] exportedPackages = getExportedPackages(auxBundle);
         if (exportedPackages != null)
         {
            for (ExportedPackage auxPackage : exportedPackages)
            {
               if (auxPackage.getName().equals(name))
                  exported.add(auxPackage);
            }
         }
      }
      
      if (exported.size() == 0)
         return null;

      ExportedPackage[] result = new ExportedPackage[exported.size()];
      exported.toArray(result);

      return result;
   }

   public Bundle[] getFragments(Bundle bundle)
   {
      AbstractBundleState absBundleState = AbstractBundleState.assertBundleState(bundle);
      if (absBundleState instanceof OSGiBundleState == false)
         return null;
      
      List<Bundle> bundles = new ArrayList<Bundle>();
      
      OSGiBundleState bundleState = (OSGiBundleState)absBundleState;
      List<OSGiFragmentState> fragments = bundleState.getAttachedFragments();
      for (OSGiFragmentState aux : fragments)
         bundles.add(aux.getBundle());
      
      if (bundles.isEmpty())
         return null;
      
      return bundles.toArray(new Bundle[bundles.size()]);
   }

   public Bundle[] getHosts(Bundle bundle)
   {
      AbstractBundleState absBundleState = AbstractBundleState.assertBundleState(bundle);
      if (absBundleState instanceof OSGiFragmentState == false)
         return null;
      
      List<Bundle> bundles = new ArrayList<Bundle>();
      
      // [TODO] Add support for multiple hosts
      OSGiFragmentState bundleState = (OSGiFragmentState)absBundleState;
      OSGiBundleState fragmentHost = bundleState.getFragmentHost();
      if (fragmentHost != null)
      {
         bundles.add(fragmentHost.getBundle());
      }
      
      if (bundles.isEmpty())
         return null;
      
      return bundles.toArray(new Bundle[bundles.size()]);
   }

   public RequiredBundle[] getRequiredBundles(String symbolicName)
   {
      // [TODO] getRequiredBundles(String symbolicName)
      return null;
   }

   public void refreshPackages(Bundle[] bundles)
   {
      // [TODO] refreshPackages(Bundle[] bundles)
   }

   public boolean resolveBundles(Bundle[] bundleArr)
   {
      // Collect the bundles that are in state INSTALLED
      List<Bundle> unresolvedBundles = new ArrayList<Bundle>();
      if (bundleArr == null)
      {
         for (Bundle bundle : getBundleManager().getBundles(Bundle.INSTALLED))
         {
            AbstractBundleState bundleState = AbstractBundleState.assertBundleState(bundle);
            if (bundleState.isFragment() == false)
               unresolvedBundles.add(bundleState);
         }
      }
      else
      {
         for (Bundle bundle : bundleArr)
         {
            if (bundle.getState() == Bundle.INSTALLED)
               unresolvedBundles.add(OSGiBundleState.assertBundleState(bundle));
         }
      }

      if (unresolvedBundles.isEmpty())
         return true;

      List<OSGiBundleState> resolvableBundles = new ArrayList<OSGiBundleState>();

      // Check if the external resolver plugin is available
      Resolver bundleResolver = getBundleManager().getOptionalPlugin(ResolverPlugin.class);
      if (bundleResolver != null)
      {
         // Resolve the bundles through the resolver
         for (ResolverBundle aux : bundleResolver.resolve(unresolvedBundles))
            resolvableBundles.add(OSGiBundleState.assertBundleState(aux.getBundle()));
      }
      else
      {
         // Every unresolved bundle is automatically copied
         // to the list of externaly resolved bundles
         for (Bundle aux : unresolvedBundles)
            resolvableBundles.add(OSGiBundleState.assertBundleState(aux));
      }

      boolean allResolved = resolvableBundles.containsAll(unresolvedBundles);

      // TODO [JBDEPLOY-226] Allow multiple deployments to change state at once
      int resolved = 1;
      while (resolved > 0)
      {
         resolved = 0;
         Iterator<OSGiBundleState> it = resolvableBundles.iterator();
         while (it.hasNext())
         {
            OSGiBundleState bundleState = it.next();
            try
            {
               boolean bundleResolved = getBundleManager().resolveBundle(bundleState, false);
               if (bundleResolved)
               {
                  it.remove();
                  resolved++;
               }
            }
            catch (BundleException ex)
            {
               // ignore
            }
         }
      }

      // Sanity check, that the controller could actually also resolve these bundles
      if (resolvableBundles.isEmpty() == false)
      {
         log.error("Controller could not resolve: " + resolvableBundles);
         for (OSGiBundleState bundleState : resolvableBundles)
         {
            try
            {
               getBundleManager().resolveBundle(bundleState, true);
            }
            catch (Exception ex)
            {
               log.debug("Cannot resolve: " + bundleState, ex);
            }
         }
      }

      allResolved = allResolved && resolvableBundles.isEmpty();
      return allResolved;
   }
}