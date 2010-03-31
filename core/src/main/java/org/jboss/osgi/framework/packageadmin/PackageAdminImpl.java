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
import org.jboss.dependency.spi.ControllerContext;
import org.jboss.dependency.spi.ControllerState;
import org.jboss.deployers.client.spi.DeployerClient;
import org.jboss.deployers.plugins.classloading.AbstractDeploymentClassLoaderPolicyModule;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStage;
import org.jboss.deployers.spi.deployer.DeploymentStages;
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
import org.osgi.framework.Constants;
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
      if (bundle != null)
      {
         exported = getExportedPackagesInternal(bundle);
      }
      else
      {
         for (Bundle aux : getBundleManager().getBundles())
         {
            exported.addAll(getExportedPackagesInternal(aux));
         }
      }

      if (exported.size() == 0)
         return null;

      ExportedPackage[] result = new ExportedPackage[exported.size()];
      exported.toArray(result);

      return result;
   }

   private List<ExportedPackage> getExportedPackagesInternal(Bundle bundle)
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
            throw new IllegalStateException("Cannot obtain ClassLoadingMetaData for: " + bundle);

         CapabilitiesMetaData capabilities = metaData.getCapabilities();
         for (Capability capability : capabilities.getCapabilities())
         {
            if (capability instanceof PackageCapability)
            {
               exported.add(new CapabilityExportedPackage(bundleState, (PackageCapability)capability));
            }
         }
      }
      return exported;
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
         bundles.add(aux.getBundleInternal());

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

      // The list of bundle deployments which should be able to advance to CLASSLOADER stage
      List<OSGiBundleState> resolvableBundles = new ArrayList<OSGiBundleState>();

      // Check if the external resolver plugin is available
      Resolver bundleResolver = getBundleManager().getOptionalPlugin(ResolverPlugin.class);
      if (bundleResolver != null)
      {
         // Resolve the bundles through the resolver. The return is the list of
         // bundles that the external resolver could actually resolve
         for (ResolverBundle aux : bundleResolver.resolve(unresolvedBundles))
         {
            OSGiBundleState bundleState = OSGiBundleState.assertBundleState(aux.getBundle());
            resolvableBundles.add(bundleState);
         }
      }
      else
      {
         // Every unresolved bundle is automatically copied to the list of resolvable bundles
         for (Bundle aux : unresolvedBundles)
         {
            OSGiBundleState bundleState = OSGiBundleState.assertBundleState(aux);
            resolvableBundles.add(bundleState);
         }
      }

      // Remove the bundles that do not pass the execution env check
      Iterator<OSGiBundleState> itBundles = resolvableBundles.iterator();
      while(itBundles.hasNext())
      {
         OSGiBundleState bundleState = itBundles.next();
         try
         {
            verifyExecutionEnvironment(bundleState, true);
         }
         catch (BundleException ex)
         {
            itBundles.remove();
         }
      }
      
      // We can only return true if all bundles are resolvable.
      boolean allResolved = resolvableBundles.containsAll(unresolvedBundles);

      // Advance the bundles to stage CLASSLOADER and check at the end
      advanceBundlesToClassloader(resolvableBundles);
      try
      {
         DeployerClient deployerClient = getBundleManager().getDeployerClient();
         deployerClient.checkComplete();
      }
      catch (DeploymentException ex)
      {
         log.error("Error resolving bundles: " + resolvableBundles, ex);
         allResolved = false;
         
         // Reset the required state for bundles that didn't get resolved
         for (OSGiBundleState bundleState : resolvableBundles)
         {
            if (bundleState.getState() == Bundle.INSTALLED)
            {
               DeploymentUnit unit = bundleState.getDeploymentUnit();
               unit.setRequiredStage(DeploymentStages.DESCRIBE);
               ControllerContext ctx = unit.getAttachment(ControllerContext.class);
               ctx.setRequiredState(ControllerState.newState(DeploymentStages.DESCRIBE.getName()));
            }
         }
      }
      
      return allResolved;
   }

   private void advanceBundlesToClassloader(List<OSGiBundleState> resolvableBundles) 
   {
      for (OSGiBundleState bundleState: resolvableBundles)
      {
         // If the bundle is in any other state but INSTALLED there is nothing to do
         if (bundleState.getState() != Bundle.INSTALLED)
            continue;

         DeploymentUnit unit = bundleState.getDeploymentUnit();
         String unitName = unit.getName();

         ControllerContext context = unit.getAttachment(ControllerContext.class);
         ControllerState requiredState = context.getRequiredState();
         DeploymentStage requiredStage = unit.getRequiredStage();

         try
         {
            DeployerClient deployerClient = getBundleManager().getDeployerClient();
            deployerClient.change(unitName, DeploymentStages.CLASSLOADER);

            // Advance the attached fragments to CLASSLOADER 
            for (OSGiFragmentState fragment : bundleState.getAttachedFragments())
            {
               String fragUnitName = fragment.getDeploymentUnit().getName();
               deployerClient.change(fragUnitName, DeploymentStages.CLASSLOADER);
            }
         }
         catch (DeploymentException ex)
         {
            unit.setRequiredStage(requiredStage);
            context.setRequiredState(requiredState);
            unit.addAttachment(DeploymentException.class, ex);
            ex.printStackTrace();
            log.error("Error resolving bundle: " + bundleState, ex);
         }
      }
   }

   private boolean verifyExecutionEnvironment(OSGiBundleState bundleState, boolean errorOnFail) throws BundleException
   {
      // A bundle can only resolve if the framework is running on a VM which
      // implements one of the listed required execution environments. 
      List<String> reqExecEnvs = bundleState.getOSGiMetaData().getRequiredExecutionEnvironment();
      if (reqExecEnvs == null)
         return true;

      boolean foundExecEnv = false;
      String fwExecEnvs = getBundleManager().getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);
      for (String aux : reqExecEnvs)
      {
         if (fwExecEnvs.contains(aux))
         {
            foundExecEnv = true;
            break;
         }
      }
      
      if (foundExecEnv == false)
      {
         String msg = "Cannot find required execution environment " + reqExecEnvs + ", we have: " + fwExecEnvs;
         if (errorOnFail == true)
            throw new BundleException(msg);

         log.error(msg);
      }
      
      return foundExecEnv;
   }
}