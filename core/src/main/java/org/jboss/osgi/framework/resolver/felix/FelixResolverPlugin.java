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

import org.jboss.logging.Logger;
import org.jboss.osgi.framework.bundle.DeployedBundleState;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.plugins.internal.AbstractPlugin;
import org.jboss.osgi.framework.resolver.XModule;
import org.jboss.osgi.framework.resolver.XResolver;
import org.jboss.osgi.framework.resolver.XResolverCallback;

/**
 * An implementation of the JBossOSGi Resolver.
 *  
 * @author thomas.diesler@jboss.com
 * @since 31-May-2010
 */
public class FelixResolverPlugin extends AbstractPlugin implements XResolver
{
   // Provide logging
   final Logger log = Logger.getLogger(FelixResolverPlugin.class);

   private XResolver resolver;

   public FelixResolverPlugin(OSGiBundleManager bundleManager, XResolver resolver)
   {
      super(bundleManager);
      this.resolver = resolver;
   }

   @Override
   public void addModule(XModule module)
   {
      resolver.addModule(module);

      // Attach the resolver module to the deployment
      if (module.getBundle().getBundleId() != 0)
      {
         DeployedBundleState bundleState = DeployedBundleState.assertBundleState(module.getBundle());
         bundleState.getDeploymentUnit().addAttachment(XModule.class, module);
      }
   }

   @Override
   public void removeModule(XModule module)
   {
      resolver.removeModule(module);

      // Remove the resolver module from the deployment
      if (module.getBundle().getBundleId() != 0)
      {
         DeployedBundleState bundleState = DeployedBundleState.assertBundleState(module.getBundle());
         bundleState.getDeploymentUnit().removeAttachment(XModule.class);
      }
   }

   @Override
   public XModule findHost(XModule fragModule)
   {
      return resolver.findHost(fragModule);
   }

   @Override
   public void setCallbackHandler(XResolverCallback callback)
   {
      resolver.setCallbackHandler(callback);
   }

   @Override
   public void resolve(XModule module)
   {
      resolver.resolve(module);
   }

   /*
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
   */

   /*
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
   */

   /*
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
   */

   /*
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
   */
}