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
package org.jboss.osgi.framework.plugins.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.jboss.osgi.framework.bundle.DeployedBundleState;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.classloading.OSGiCapability;
import org.jboss.osgi.framework.classloading.OSGiRequirement;
import org.jboss.osgi.framework.plugins.ResolverPlugin;
import org.jboss.osgi.framework.plugins.SystemPackagesPlugin;
import org.jboss.osgi.resolver.XCapability;
import org.jboss.osgi.resolver.XModule;
import org.jboss.osgi.resolver.XModuleBuilder;
import org.jboss.osgi.resolver.XRequirement;
import org.jboss.osgi.resolver.XResolver;
import org.jboss.osgi.resolver.XResolverFactory;
import org.jboss.osgi.resolver.XWire;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * The OSGi Resolver plugin.
 *  
 * @author thomas.diesler@jboss.com
 * @since 31-May-2010
 */
public class ResolverPluginImpl extends AbstractPlugin implements ResolverPlugin
{
   // Provide logging
   final Logger log = Logger.getLogger(ResolverPluginImpl.class);

   private XResolver resolver;

   public ResolverPluginImpl(OSGiBundleManager bundleManager, XResolver resolver)
   {
      super(bundleManager);
      this.resolver = resolver;
   }

   @Override
   public void addBundle(Bundle bundle)
   {
      XModule module;
      if (bundle.getBundleId() == 0)
      {
         XModuleBuilder builder = XResolverFactory.getModuleBuilder();
         module = builder.createModule(0, bundle.getSymbolicName(), bundle.getVersion());
         builder.addBundleCapability(bundle.getSymbolicName(), bundle.getVersion());
         module.addAttachment(Bundle.class, bundle);
         
         SystemPackagesPlugin plugin = getPlugin(SystemPackagesPlugin.class);
         for (String packageSpec : plugin.getSystemPackages(true))
         {
            String packname = packageSpec;
            Version version = Version.emptyVersion;

            int versionIndex = packname.indexOf(";version=");
            if (versionIndex > 0)
            {
               packname = packageSpec.substring(0, versionIndex);
               version = Version.parseVersion(packageSpec.substring(versionIndex + 9));
            }

            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put(Constants.VERSION_ATTRIBUTE, version);
            
            builder.addPackageCapability(packname, null, attrs);
         }
      }
      else
      {
         DeployedBundleState bundleState = DeployedBundleState.assertBundleState(bundle);
         module = bundleState.getDeploymentUnit().getAttachment(XModule.class);
      }
      
      // Attach the bundle to the module and add it to the resolver
      module.addAttachment(Bundle.class, bundle);
      resolver.addModule(module);
   }

   @Override
   public void removeBundle(Bundle bundle)
   {
      resolver.removeModule(bundle.getBundleId());
   }

   @Override
   public List<Bundle> resolve(List<Bundle> bundles)
   {
      List<XModule> modules = new ArrayList<XModule>();
      if (bundles == null)
      {
         modules = resolver.getModules();
      }
      else
      {
         for (Bundle bundle : bundles)
         {
            XModule module = resolver.findModuleById(bundle.getBundleId());
            if (module == null)
               throw new IllegalStateException("Module not registered for: " + bundle);
            
            modules.add(module);
         }
      }
      
      List<Bundle> result = new ArrayList<Bundle>();
      modules = resolver.resolve(modules);
      for (XModule module : modules)
      {
         Bundle bundle = module.getAttachment(Bundle.class);
         if (bundle == null)
            throw new IllegalStateException("Cannot obtain associated bundle from: " + module);
         
         result.add(bundle);
      }
      
      return Collections.unmodifiableList(result);
   }

   @Override
   public OSGiCapability getWiredCapability(OSGiRequirement osgireq)
   {
      XRequirement req = osgireq.getResolverElement();
      XWire wire = req.getWire();
      if (wire == null || wire.getCapability() == null)
         return null;
      
      XCapability cap = wire.getCapability();
      OSGiCapability osgicap = cap.getAttachment(OSGiCapability.class);
      return osgicap;
   }

}