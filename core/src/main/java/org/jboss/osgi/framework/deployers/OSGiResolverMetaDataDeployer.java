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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.classloading.spi.metadata.ClassLoadingMetaData;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractSimpleRealDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.metadata.OSGiMetaData;
import org.jboss.osgi.framework.metadata.PackageAttribute;
import org.jboss.osgi.framework.metadata.Parameter;
import org.jboss.osgi.framework.metadata.ParameterizedAttribute;
import org.jboss.osgi.framework.resolver.XModule;
import org.jboss.osgi.framework.resolver.XResolverBuilder;

/**
 * A deployer, that maps {@link OSGiMetaData} into the resolver {@link XModule} metadata.
 * 
 * @author thomas.diesler@jboss.com
 * @since 02-Jul-2010
 */
public class OSGiResolverMetaDataDeployer extends AbstractSimpleRealDeployer<OSGiMetaData>
{
   public OSGiResolverMetaDataDeployer()
   {
      super(OSGiMetaData.class);
      addInput(AbstractBundleState.class);
      addOutput(XModule.class);
      setStage(DeploymentStages.POST_PARSE);
      setTopLevelOnly(true);
   }

   @Override
   public void deploy(DeploymentUnit unit, OSGiMetaData osgiMetaData) throws DeploymentException
   {
      if (unit.isAttachmentPresent(ClassLoadingMetaData.class))
         return;

      AbstractBundleState bundleState = unit.getAttachment(AbstractBundleState.class);
      if (bundleState == null)
         throw new IllegalStateException("No bundle state");

      XResolverBuilder builder = XResolverBuilder.newBuilder(bundleState);
      builder.addHostCapability(bundleState.getSymbolicName(), bundleState.getVersion());

      // Required Bundles
      List<ParameterizedAttribute> requireBundles = osgiMetaData.getRequireBundles();
      if (requireBundles != null && requireBundles.isEmpty() == false)
      {
         for (ParameterizedAttribute metadata : requireBundles)
         {
            String name = metadata.getAttribute();
            Map<String, String> dirs = getDirectives(metadata);
            Map<String, String> atts = getAttributes(metadata);
            builder.addBundleRequirement(name, dirs, atts);
         }
      }

      // Export-Package
      List<PackageAttribute> exports = osgiMetaData.getExportPackages();
      if (exports != null && exports.isEmpty() == false)
      {
         for (PackageAttribute metadata : exports)
         {
            String name = metadata.getAttribute();
            Map<String, String> dirs = getDirectives(metadata);
            Map<String, String> atts = getAttributes(metadata);
            builder.addPackageCapability(name, dirs, atts);
         }
      }

      // Import-Package
      List<PackageAttribute> imports = osgiMetaData.getImportPackages();
      if (imports != null && imports.isEmpty() == false)
      {
         for (PackageAttribute metadata : imports)
         {
            String name = metadata.getAttribute();
            Map<String, String> dirs = getDirectives(metadata);
            Map<String, String> atts = getAttributes(metadata);
            builder.addPackageRequirement(name, dirs, atts);
         }
      }

      // DynamicImport-Package
      List<PackageAttribute> dynamicImports = osgiMetaData.getDynamicImports();
      if (dynamicImports != null && dynamicImports.isEmpty() == false)
      {
         for (PackageAttribute metadata : dynamicImports)
         {
            String name = metadata.getAttribute();
            Map<String, String> atts = getAttributes(metadata);
            builder.addDynamicPackageRequirement(name, atts);
         }
      }

      unit.addAttachment(XModule.class, builder.getModule());
   }

   private Map<String, String> getDirectives(ParameterizedAttribute metadata)
   {
      Map<String, String> dirs = new HashMap<String, String>();
      for(String key : metadata.getDirectives().keySet())
      {
         Parameter param = metadata.getDirective(key);
         dirs.put(key, param.getValue().toString());
      }
      return dirs;
   }

   private Map<String, String> getAttributes(ParameterizedAttribute metadata)
   {
      Map<String, String> atts = new HashMap<String, String>();
      for(String key : metadata.getAttributes().keySet())
      {
         Parameter param = metadata.getAttribute(key);
         atts.put(key, param.getValue().toString());
      }
      return atts;
   }
}
