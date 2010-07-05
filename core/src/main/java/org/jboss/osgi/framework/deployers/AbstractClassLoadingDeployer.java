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
package org.jboss.osgi.framework.deployers;

// $Id$

import java.util.List;

import org.jboss.classloader.spi.ClassLoaderDomain;
import org.jboss.classloader.spi.ShutdownPolicy;
import org.jboss.classloading.spi.metadata.CapabilitiesMetaData;
import org.jboss.classloading.spi.metadata.ClassLoadingMetaData;
import org.jboss.classloading.spi.metadata.RequirementsMetaData;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractSimpleRealDeployer;
import org.jboss.deployers.structure.spi.ClassLoaderFactory;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.classloading.OSGiBundleCapability;
import org.jboss.osgi.framework.classloading.OSGiClassLoadingMetaData;
import org.jboss.osgi.framework.classloading.OSGiPackageCapability;
import org.jboss.osgi.framework.classloading.OSGiPackageRequirement;
import org.jboss.osgi.framework.classloading.OSGiRequiredBundleRequirement;
import org.jboss.osgi.framework.metadata.OSGiMetaData;
import org.jboss.osgi.framework.plugins.SystemPackagesPlugin;
import org.jboss.osgi.framework.resolver.XRequireBundleRequirement;
import org.jboss.osgi.framework.resolver.XBundleCapability;
import org.jboss.osgi.framework.resolver.XModule;
import org.jboss.osgi.framework.resolver.XPackageCapability;
import org.jboss.osgi.framework.resolver.XPackageRequirement;

/**
 * An abstract OSGi classloading deployer, that maps {@link OSGiMetaData} into {@link ClassLoadingMetaData}.
 * 
 * @author thomas.diesler@jboss.com
 * @since 07-Jan-2010
 */
public class AbstractClassLoadingDeployer extends AbstractSimpleRealDeployer<XModule>
{
   private ClassLoaderDomain domain;
   private ClassLoaderFactory factory;

   public AbstractClassLoadingDeployer()
   {
      super(XModule.class);
      addInput(AbstractBundleState.class);
      addInput(ClassLoadingMetaData.class);
      addOutput(ClassLoadingMetaData.class);
      setStage(DeploymentStages.POST_PARSE);
      setTopLevelOnly(true);
   }

   public void setDomain(ClassLoaderDomain domain)
   {
      this.domain = domain;
   }

   public void setFactory(ClassLoaderFactory factory)
   {
      this.factory = factory;
   }

   @Override
   public void deploy(DeploymentUnit unit, XModule module) throws DeploymentException
   {
      if (unit.isAttachmentPresent(ClassLoadingMetaData.class))
         return;

      AbstractBundleState bundleState = unit.getAttachment(AbstractBundleState.class);
      if (bundleState == null)
         throw new IllegalStateException("No bundle state");

      OSGiBundleManager bundleManager = bundleState.getBundleManager();
      SystemPackagesPlugin syspackPlugin = bundleManager.getPlugin(SystemPackagesPlugin.class);

      OSGiClassLoadingMetaData classLoadingMetaData = new OSGiClassLoadingMetaData();
      classLoadingMetaData.setName(bundleState.getSymbolicName());
      classLoadingMetaData.setVersion(bundleState.getVersion());
      classLoadingMetaData.setDomain(domain.getName());
      classLoadingMetaData.setShutdownPolicy(ShutdownPolicy.GARBAGE_COLLECTION);
      classLoadingMetaData.setJ2seClassLoadingCompliance(false);

      CapabilitiesMetaData capabilities = classLoadingMetaData.getCapabilities();
      RequirementsMetaData requirements = classLoadingMetaData.getRequirements();

      XBundleCapability bundleCap = module.getBundleCapability();
      OSGiBundleCapability bundleCapability = OSGiBundleCapability.create(bundleCap, bundleState);
      capabilities.addCapability(bundleCapability);

      // Required Bundles
      List<XRequireBundleRequirement> bundleReqs = module.getBundleRequirements();
      for (XRequireBundleRequirement bundleReq : bundleReqs)
      {
         OSGiRequiredBundleRequirement requirement = OSGiRequiredBundleRequirement.create(bundleReq, bundleState);
         requirements.addRequirement(requirement);
      }

      // Export-Package
      List<XPackageCapability> exports = module.getPackageCapabilities();
      for (XPackageCapability packageCap : exports)
      {
         OSGiPackageCapability packageCapability = OSGiPackageCapability.create(packageCap, bundleState);
         capabilities.addCapability(packageCapability);
      }

      // Import-Package
      List<XPackageRequirement> imports = module.getPackageRequirements();
      for (XPackageRequirement metadata : imports)
      {
         String packageName = metadata.getName();

         // [JBOSGI-329] SystemBundle not added as MC module
         if (syspackPlugin.isSystemPackage(packageName) == false)
         {
            OSGiPackageRequirement requirement = OSGiPackageRequirement.create(bundleState, metadata);
            requirements.addRequirement(requirement);
         }
      }

      // DynamicImport-Package
      List<XPackageRequirement> dynamicImports = module.getDynamicPackageRequirements();
      for (XPackageRequirement packageAttribute : dynamicImports)
      {
         String packageName = packageAttribute.getName();

         // [JBOSGI-329] SystemBundle not added as MC module
         if (syspackPlugin.isSystemPackage(packageName) == false)
         {
            OSGiPackageRequirement requirement = OSGiPackageRequirement.create(bundleState, packageAttribute);
            requirements.addRequirement(requirement);
         }
      }

      unit.addAttachment(ClassLoadingMetaData.class, classLoadingMetaData);

      // AnnotationMetaDataDeployer.ANNOTATION_META_DATA_COMPLETE
      unit.addAttachment("org.jboss.deployment.annotation.metadata.complete", Boolean.TRUE);

      // Add the OSGi ClassLoaderFactory if configured
      if (factory != null)
         unit.addAttachment(ClassLoaderFactory.class, factory);
   }
}
