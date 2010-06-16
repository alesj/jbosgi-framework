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
package org.jboss.osgi.framework.resolver.felix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.framework.capabilityset.Capability;
import org.apache.felix.framework.capabilityset.Requirement;
import org.apache.felix.framework.resolver.Module;
import org.apache.felix.framework.resolver.ResolveException;
import org.apache.felix.framework.resolver.Wire;
import org.jboss.logging.Logger;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.bundle.DeployedBundleState;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.bundle.OSGiFragmentState;
import org.jboss.osgi.framework.bundle.OSGiSystemState;
import org.jboss.osgi.framework.classloading.OSGiCapability;
import org.jboss.osgi.framework.classloading.OSGiPackageCapability;
import org.jboss.osgi.framework.classloading.OSGiPackageRequirement;
import org.jboss.osgi.framework.classloading.OSGiRequirement;
import org.jboss.osgi.framework.plugins.ResolverPlugin;
import org.jboss.osgi.framework.plugins.internal.AbstractPlugin;
import org.jboss.osgi.framework.resolver.AbstractModule;
import org.jboss.osgi.framework.resolver.AbstractResolverPlugin;
import org.osgi.framework.Bundle;

/**
 * An implementation of the JBossOSGi Resolver.
 *  
 * @author thomas.diesler@jboss.com
 * @since 31-May-2010
 */
public class FelixResolverPlugin extends AbstractPlugin implements ResolverPlugin
{
   // Provide logging
   final Logger log = Logger.getLogger(FelixResolverPlugin.class);

   private JBossResolver resolver = new JBossResolver();

   public FelixResolverPlugin(OSGiBundleManager bundleManager)
   {
      super(bundleManager);
   }

   @Override
   public void addBundle(Bundle bundle)
   {
      AbstractModule module = resolver.createModule(bundle);
      resolver.addModule(module);

      // Attach the resolver module to the deployment
      if (bundle.getBundleId() != 0)
      {
         DeployedBundleState bundleState = DeployedBundleState.assertBundleState(bundle);
         bundleState.getDeploymentUnit().addAttachment(AbstractModule.class, module);
      }
   }

   @Override
   public void removeBundle(Bundle bundle)
   {
      AbstractModule module = resolver.getModule(bundle);
      resolver.removeModule(module);

      // Remove the resolver module from the deployment
      if (bundle.getBundleId() != 0)
      {
         DeployedBundleState bundleState = DeployedBundleState.assertBundleState(bundle);
         bundleState.getDeploymentUnit().removeAttachment(AbstractModule.class);
      }
   }

   @Override
   public List<Bundle> resolve(List<Bundle> bundles)
   {
      List<Bundle> resolved = new ArrayList<Bundle>();
      for (Bundle bundle : bundles)
      {
         AbstractModule module = resolver.getModule(bundle);
         if (failsafeResolve(module) == true)
            resolved.add(bundle);
      }
      return Collections.unmodifiableList(resolved);
   }

   @Override
   public OSGiCapability getWiredCapability(OSGiRequirement osgireq)
   {
      AbstractBundleState importer = osgireq.getBundleState();
      AbstractBundleModule impModule = resolver.getModule(importer);

      // Lazily resolve the importer and retry
      if (impModule.isResolved() == false && failsafeResolve(impModule) == true)
         return getWiredCapability(osgireq);

      // If the importer is not resolved, we cannot return a wire
      if (impModule.isResolved() == false)
         return null;
      
      
      // Get the potential wire for the requirement
      Requirement req = impModule.getMappedRequirement(osgireq);
      OSGiCapability osgicap = getWiredCapability(impModule, req);
      
      if (osgicap == null && importer.isFragment())
      {
         OSGiFragmentState fragState = OSGiFragmentState.assertBundleState(importer);
         AbstractBundleModule hostModule = resolver.getModule(fragState.getFragmentHost());
         osgicap = getWiredCapability(hostModule, req);
      }
      
      // Felix does not maintain wires to capabilies provided by the same bundle
      if (osgicap == null && osgireq instanceof OSGiPackageRequirement)
      {
         OSGiPackageRequirement packreq = (OSGiPackageRequirement)osgireq;

         // For non-dynamic package imports check if the importer also 
         // also provides a matching capability
         if (packreq.isDynamic() == false || packreq.isOptional())
         {
            for (OSGiCapability aux : impModule.getOSGiCapabilities())
            {
               if (aux instanceof OSGiPackageCapability)
               {
                  OSGiPackageCapability packcap = (OSGiPackageCapability)aux;
                  if (packcap.matchNameAndVersion(packreq) && packcap.matchAttributes(packreq))
                  {
                     osgicap = packcap;
                     break;
                  }
               }
            }
         }
      }
      
      return osgicap;
   }

   private OSGiCapability getWiredCapability(AbstractBundleModule impModule, Requirement req)
   {
      OSGiCapability osgicap = null;
      Wire wire = impModule.getWireForRequirement(req);
      if (wire != null)
      {
         Capability wiredcap = wire.getCapability();
         Bundle expBundle = wire.getExporter().getBundle();
         AbstractBundleModule expModule = resolver.getModule(expBundle);
         osgicap = expModule.getMappedCapability(wiredcap);
         if (osgicap == null)
            throw new IllegalStateException("Cannot find capability mapping for: " + wire);
      }
      return osgicap;
   }

   @Override
   public List<OSGiRequirement> getUnresolvedRequirements(Bundle bundle)
   {
      List<OSGiRequirement> result = new ArrayList<OSGiRequirement>();
      AbstractBundleModule module = resolver.getModule(bundle);
      for (OSGiRequirement req : module.getOSGiRequirements())
      {
         OSGiCapability cap = getWiredCapability(req);
         if (cap == null)
            result.add(req);
      }
      return Collections.unmodifiableList(result);
   }

   @Override
   public Map<OSGiRequirement, OSGiCapability> getWiring(Bundle bundle)
   {
      Map<OSGiRequirement, OSGiCapability> result = new LinkedHashMap<OSGiRequirement, OSGiCapability>();
      AbstractBundleModule module = resolver.getModule(bundle);
      for (OSGiRequirement req : module.getOSGiRequirements())
      {
         OSGiCapability cap = getWiredCapability(req);
         result.put(req, cap);
      }
      return Collections.unmodifiableMap(result);
   }

   private boolean failsafeResolve(AbstractModule module)
   {
      try
      {
         resolver.resolve(module);
         return true;
      }
      catch (ResolveException ex)
      {
         log.debug("Cannot resolve requirement: " + ex.getRequirement());
         return false;
      }
   }

   static class JBossResolver extends AbstractResolverPlugin
   {
      private SystemBundleModule sysModule;
      
      @Override
      public boolean acquireGlobalLock()
      {
         // nothing to do
         return true;
      }

      @Override
      public void releaseGlobalLock()
      {
         // nothing to do
      }

      @Override
      public void markBundleResolved(Module module)
      {
         // nothing to do
      }

      @Override
      public AbstractModule createModule(Bundle bundle)
      {
         if (bundle.getBundleId() == 0)
         {
            OSGiSystemState bundleState = OSGiSystemState.assertBundleState(bundle);
            sysModule = new SystemBundleModule(bundleState);
            return sysModule;
         }
         else
         {
            DeployedBundleState bundleState = DeployedBundleState.assertBundleState(bundle);
            return new DeployedBundleModule(bundleState);
         }
      }

      @Override
      public AbstractBundleModule getModule(Bundle bundle)
      {
         AbstractBundleModule result = null;
         if (bundle.getBundleId() == 0)
         {
            result = sysModule;
         }
         else
         {
            DeployedBundleState bundleState = DeployedBundleState.assertBundleState(bundle);
            result = (AbstractBundleModule)bundleState.getDeploymentUnit().getAttachment(AbstractModule.class);
         }
         
         if (result == null)
            throw new IllegalStateException("No module attached to: " + bundle);

         return result;
      }
   }
}