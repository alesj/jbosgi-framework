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
import org.jboss.deployers.client.spi.IncompleteDeploymentException;
import org.jboss.deployers.plugins.classloading.AbstractDeploymentClassLoaderPolicyModule;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStage;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.logging.Logger;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.bundle.DeployedBundleState;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.bundle.OSGiBundleState;
import org.jboss.osgi.framework.bundle.OSGiFragmentState;
import org.jboss.osgi.framework.bundle.OSGiSystemState;
import org.jboss.osgi.framework.plugins.PackageAdminPlugin;
import org.jboss.osgi.framework.plugins.ResolverPlugin;
import org.jboss.osgi.framework.plugins.SystemPackagesPlugin;
import org.jboss.osgi.framework.plugins.internal.AbstractServicePlugin;
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
         DeployedBundleState bundleState = (DeployedBundleState)absBundleState;
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
      AbstractBundleState fragmentHost = bundleState.getFragmentHost();
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
      throw new NotImplementedException();
      
      // This method returns to the caller immediately and then performs the following steps on a separate thread:
      //
      //   1. Compute a graph of bundles starting with the specified bundles. If no bundles are specified, 
      //      compute a graph of bundles starting with bundle updated or uninstalled since the last call to this method. 
      //      Add to the graph any bundle that is wired to a package that is currently exported by a bundle in the graph. 
      //      The graph is fully constructed when there is no bundle outside the graph that is wired to a bundle in the graph. 
      //      The graph may contain UNINSTALLED bundles that are currently still exporting packages.
      //
      //   2. Each bundle in the graph that is in the ACTIVE state will be stopped as described in the Bundle.stop method.
      //
      //   3. Each bundle in the graph that is in the RESOLVED state is unresolved and thus moved to the INSTALLED state. 
      //      The effect of this step is that bundles in the graph are no longer RESOLVED.
      //
      //   4. Each bundle in the graph that is in the UNINSTALLED state is removed from the graph and is now completely removed from the Framework.
      //
      //   5. Each bundle in the graph that was in the ACTIVE state prior to Step 2 is started as described in the Bundle.start method, 
      //      causing all bundles required for the restart to be resolved. It is possible that, as a result of the previous steps, 
      //      packages that were previously exported no longer are. Therefore, some bundles may be unresolvable until another bundle 
      //      offering a compatible package for export has been installed in the Framework.
      //
      //   6. A framework event of type FrameworkEvent.PACKAGES_REFRESHED is fired.
      //
      // For any exceptions that are thrown during any of these steps, a FrameworkEvent of type ERROR is fired containing the exception. 
      // The source bundle for these events should be the specific bundle to which the exception is related. If no specific bundle can 
      // be associated with the exception then the System Bundle must be used as the source bundle for the event.

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
      ResolverPlugin bundleResolver = getBundleManager().getOptionalPlugin(ResolverPlugin.class);
      if (bundleResolver != null)
      {
         // Resolve the bundles through the resolver. The return is the list of
         // bundles that the external resolver could actually resolve
         for (Bundle aux : bundleResolver.resolve(unresolvedBundles))
         {
            OSGiBundleState bundleState = OSGiBundleState.assertBundleState(aux);
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
         AbstractBundleState bundleState = itBundles.next();
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
      if (advanceBundlesToClassloader(resolvableBundles) == false)
      {
         resetBundleDeploymentStates(resolvableBundles);
         allResolved = false;
      }
      
      return allResolved;
   }

   private boolean advanceBundlesToClassloader(List<OSGiBundleState> resolvableBundles) 
   {
      DeployerClient deployerClient = getBundleManager().getDeployerClient();

      // Remember the state of every deployment unit
      for (OSGiBundleState bundleState: resolvableBundles)
      {
         DeploymentUnit unit = bundleState.getDeploymentUnit();
         unit.addAttachment(StateTuple.class, new StateTuple(unit));
      }

      // Change to DeploymentStage CLASSLOADER 
      for (OSGiBundleState bundleState: resolvableBundles)
      {
         try
         {
            DeploymentUnit unit = bundleState.getDeploymentUnit();
            deployerClient.change(unit.getName(), DeploymentStages.CLASSLOADER);

            // Advance the attached fragments to CLASSLOADER 
            for (OSGiFragmentState fragment : bundleState.getAttachedFragments())
            {
               String fragUnitName = fragment.getDeploymentUnit().getName();
               deployerClient.change(fragUnitName, DeploymentStages.CLASSLOADER);
            }
         }
         catch (DeploymentException ex)
         {
            log.error("Error resolving bundle: " + bundleState, ex);
         }
      }
      
      // Check that every deployment could reach the desired stage
      try
      {
         deployerClient.checkComplete();
         return true;
      }
      catch (DeploymentException ex)
      {
         log.error("Error resolving bundles: " + resolvableBundles, ex);
         if (ex instanceof IncompleteDeploymentException)
         {
            // TODO relay better error message to caller
            // IncompleteDeploymentException idex = (IncompleteDeploymentException)ex;
         }
         return false;
      }
   }

   private void resetBundleDeploymentStates(List<OSGiBundleState> resolvableBundles) 
   {
      for (OSGiBundleState bundleState: resolvableBundles)
      {
         DeploymentUnit unit = bundleState.getDeploymentUnit();
         StateTuple stateTuple = unit.removeAttachment(StateTuple.class);
         if (stateTuple != null)
         {
            stateTuple.reset(unit);
         }
      }
   }

   private boolean verifyExecutionEnvironment(AbstractBundleState bundleState, boolean errorOnFail) throws BundleException
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
   
   static class StateTuple
   {
      ControllerState requiredState;
      DeploymentStage requiredStage;
      
      StateTuple(DeploymentUnit unit)
      {
         ControllerContext context = unit.getAttachment(ControllerContext.class);
         requiredState = context.getRequiredState();
         requiredStage = unit.getRequiredStage();
      }
      
      void reset(DeploymentUnit unit)
      {
         ControllerContext context = unit.getAttachment(ControllerContext.class);
         context.setRequiredState(requiredState);
         unit.setRequiredStage(requiredStage);
      }
   };
}