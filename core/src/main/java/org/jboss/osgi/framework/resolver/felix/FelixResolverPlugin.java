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
import java.util.List;

import org.apache.felix.framework.capabilityset.Requirement;
import org.apache.felix.framework.resolver.Module;
import org.apache.felix.framework.resolver.ResolveException;
import org.apache.felix.framework.resolver.Wire;
import org.jboss.logging.Logger;
import org.jboss.osgi.framework.bundle.DeployedBundleState;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.bundle.OSGiSystemState;
import org.jboss.osgi.framework.classloading.OSGiRequirement;
import org.jboss.osgi.framework.plugins.ResolverPlugin;
import org.jboss.osgi.framework.plugins.internal.AbstractPlugin;
import org.jboss.osgi.framework.resolver.AbstractResolverPlugin;
import org.jboss.osgi.framework.resolver.AbstractModule;
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

   private AbstractResolverPlugin resolver = new JBossResolver();

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
   public boolean match(Bundle importer, Bundle exporter, OSGiRequirement osgireq)
   {
      AbstractModule impModule = resolver.getModule(importer);
      AbstractModule expModule = resolver.getModule(exporter);

      // Lazily resolve the exporter and retry
      if (expModule.isResolved() == false)
         return failsafeResolve(expModule) && match(importer, exporter, osgireq);

      // Lazily resolve the importer and retry
      if (impModule.isResolved() == false)
         return failsafeResolve(impModule) && match(importer, exporter, osgireq);

      // A dynamic requirement does not match a specific module
      if (osgireq.isDynamic() == true && osgireq.isOptional() == false)
         return false;

      // Get the potential wire for the requirement and see if it matches the given exporter 
      Requirement req = ((DeployedBundleModule)impModule).getMappedRequirement(osgireq);
      Wire wire = impModule.getWireForRequirement(req);
      if (wire != null)
      {
         Module wireExporter = wire.getExporter();
         return wireExporter == expModule;
      }

      // If we did not get a ResolverException, we can assume that 
      // all packages that do not have a wire, wire to itself
      if (impModule == expModule)
         return true;

      return false;
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
            return new SystemBundleModule(bundleState);
         }
         else
         {
            DeployedBundleState bundleState = DeployedBundleState.assertBundleState(bundle);
            return new DeployedBundleModule(bundleState);
         }
      }

      @Override
      public AbstractModule getModule(Bundle bundle)
      {
         DeployedBundleState bundleState = DeployedBundleState.assertBundleState(bundle);
         AbstractModule module = bundleState.getDeploymentUnit().getAttachment(AbstractModule.class);
         if (module == null)
            throw new IllegalStateException("No module attached to: " + bundle);

         return module;
      }
   }
}