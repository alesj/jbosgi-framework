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
package org.jboss.osgi.framework.resolver.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.felix.framework.capabilityset.Requirement;
import org.apache.felix.framework.resolver.Module;
import org.apache.felix.framework.resolver.ResolveException;
import org.apache.felix.framework.resolver.Wire;
import org.jboss.logging.Logger;
import org.jboss.osgi.framework.bundle.AbstractDeployedBundleState;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.bundle.OSGiBundleState;
import org.jboss.osgi.framework.bundle.OSGiSystemState;
import org.jboss.osgi.framework.classloading.OSGiRequirement;
import org.jboss.osgi.framework.plugins.ResolverPlugin;
import org.jboss.osgi.framework.plugins.internal.AbstractPlugin;
import org.osgi.framework.Bundle;

/**
 * An implementation of the JBossOSGi Resolver.
 *  
 * @author thomas.diesler@jboss.com
 * @since 31-May-2010
 */
public class ResolverPluginImpl extends AbstractPlugin implements ResolverPlugin
{
   // Provide logging
   final Logger log = Logger.getLogger(ResolverPluginImpl.class);
   
   private AbstractResolver resolver = new JBossResolver();

   public ResolverPluginImpl(OSGiBundleManager bundleManager)
   {
      super(bundleManager);
   }

   @Override
   public void addBundle(Bundle bundle)
   {
      ModuleExtension module = resolver.createModule(bundle);
      resolver.addModule(module);

      // Attach the resolver module to the deployment
      if (bundle.getBundleId() != 0)
      {
         AbstractDeployedBundleState bundleState = AbstractDeployedBundleState.assertBundleState(bundle);
         bundleState.getDeploymentUnit().addAttachment(ModuleExtension.class, module);
      }
   }

   @Override
   public void removeBundle(Bundle bundle)
   {
      ModuleExtension module = resolver.getModule(bundle);
      resolver.removeModule(module);

      // Remove the resolver module from the deployment
      if (bundle.getBundleId() != 0)
      {
         AbstractDeployedBundleState bundleState = AbstractDeployedBundleState.assertBundleState(bundle);
         bundleState.getDeploymentUnit().removeAttachment(ModuleExtension.class);
      }
   }

   @Override
   public List<Bundle> resolve(List<Bundle> bundles)
   {
      List<Bundle> resolved = new ArrayList<Bundle>();
      for (Bundle bundle : bundles)
      {
         ModuleExtension module = resolver.getModule(bundle);
         try
         {
            resolver.resolve(module);
            resolved.add(bundle);
         }
         catch (ResolveException ex)
         {
            log.debug("Cannot resolve requirement: " + ex.getRequirement());
         }
      }
      return Collections.unmodifiableList(resolved);
   }

   @Override
   public boolean match(Bundle importer, Bundle exporter, OSGiRequirement osgireq)
   {
      ModuleExtension impModule = resolver.getModule(importer);
      ModuleExtension expModule = resolver.getModule(exporter);
      
      if (impModule.isResolved() == false || expModule.isResolved() == false)
         return false;
      
      Requirement req = ((DeployedBundleModule)impModule).getMappedRequirement(osgireq);
      Wire wire = impModule.getWireForRequirement(req);
      if (wire != null)
      {
         boolean match = wire.getExporter() == expModule;
         return match;
      }

      // If we did not get a ResolverException, we can assume that 
      // all packages that do not have a wire, wire to itself
      return impModule == expModule;
   }

   static class JBossResolver extends AbstractResolver
   {
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
      public ModuleExtension createModule(Bundle bundle)
      {
         if (bundle.getBundleId() == 0)
         {
            return new SystemBundleModule((OSGiSystemState)bundle);
         }
         else
         {
            OSGiBundleState bundleState = OSGiBundleState.assertBundleState(bundle);
            return new DeployedBundleModule(bundleState);
         }
      }

      @Override
      public ModuleExtension getModule(Bundle bundle)
      {
         OSGiBundleState bundleState = OSGiBundleState.assertBundleState(bundle);
         ModuleExtension module = bundleState.getDeploymentUnit().getAttachment(ModuleExtension.class);
         if (module == null)
            throw new IllegalStateException("No module attached to: " + bundle);

         return module;
      }
   }
}