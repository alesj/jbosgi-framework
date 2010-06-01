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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.felix.framework.capabilityset.Attribute;
import org.apache.felix.framework.capabilityset.Capability;
import org.apache.felix.framework.capabilityset.Directive;
import org.apache.felix.framework.capabilityset.Requirement;
import org.apache.felix.framework.util.manifestparser.CapabilityImpl;
import org.apache.felix.framework.util.manifestparser.RequirementImpl;
import org.jboss.logging.Logger;
import org.jboss.osgi.framework.bundle.OSGiBundleState;
import org.jboss.osgi.framework.classloading.OSGiBundleCapability;
import org.jboss.osgi.framework.classloading.OSGiCapability;
import org.jboss.osgi.framework.classloading.OSGiModule;
import org.jboss.osgi.framework.classloading.OSGiPackageCapability;
import org.jboss.osgi.framework.classloading.OSGiPackageRequirement;
import org.jboss.osgi.framework.classloading.OSGiRequirement;
import org.jboss.osgi.framework.metadata.PackageAttribute;
import org.jboss.osgi.framework.metadata.Parameter;
import org.osgi.framework.Constants;

/**
 * An implementation of the Felix Module
 *  
 * @author thomas.diesler@jboss.com
 * @since 31-May-2010
 */
class ModuleExtensionImpl extends AbstractModule
{
   // Provide logging
   final Logger log = Logger.getLogger(ModuleExtensionImpl.class);

   private OSGiModule moduleDelegate;
   private Map<OSGiCapability, Capability> capMap;
   private Map<OSGiRequirement, Requirement> reqMap;

   public ModuleExtensionImpl(OSGiBundleState bundleState)
   {
      super(bundleState);
   }

   @Override
   public List<Capability> createCapabilities()
   {
      OSGiBundleState bundleState = (OSGiBundleState)getBundle();

      capMap = new LinkedHashMap<OSGiCapability, Capability>();
      if (getModuleDelegate().getCapabilities() != null)
      {
         for (org.jboss.classloading.spi.metadata.Capability mccap : getModuleDelegate().getCapabilities())
         {
            // Add a module capability and a host capability to all non-fragment bundles. 
            // A host capability is the same as a module capability, but with a different capability namespace. 
            // Module capabilities resolve required-bundle dependencies, while host capabilities resolve fragment-host dependencies.
            if (mccap instanceof OSGiBundleCapability)
            {
               if (bundleState.isFragment() == false)
               {
                  OSGiBundleCapability osgicap = (OSGiBundleCapability)mccap;
                  List<Attribute> attrs = new ArrayList<Attribute>(2);
                  attrs.add(new Attribute(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE, osgicap.getName(), false));
                  attrs.add(new Attribute(Constants.BUNDLE_VERSION_ATTRIBUTE, osgicap.getVersion(), false));
                  capMap.put(osgicap, new CapabilityImpl(this, Capability.HOST_NAMESPACE, new ArrayList<Directive>(0), attrs));
               }
            }

            // Add the package capabilities
            else if (mccap instanceof OSGiPackageCapability)
            {
               OSGiPackageCapability osgicap = (OSGiPackageCapability)mccap;
               PackageAttribute metadata = osgicap.getMetadata();

               // Get the capabiliy attributes
               List<Attribute> attrs = new ArrayList<Attribute>();
               attrs.add(new Attribute(Capability.PACKAGE_ATTR, osgicap.getName(), false));
               for (Entry<String, Parameter> entry : metadata.getAttributes().entrySet())
                  attrs.add(new Attribute(entry.getKey(), entry.getValue().getValue(), false));

               // Get the capabiliy directives
               List<Directive> dirs = new ArrayList<Directive>();
               for (Entry<String, Parameter> entry : metadata.getDirectives().entrySet())
                  dirs.add(new Directive(entry.getKey(), entry.getValue().getValue()));

               capMap.put(osgicap, new CapabilityImpl(this, Capability.PACKAGE_NAMESPACE, dirs, attrs));
            }
            else
            {
               log.warn("Unsupported capability: " + mccap);
            }
         }
      }

      ArrayList<Capability> result = new ArrayList<Capability>(capMap.values());

      // Always add the module capability 
      List<Attribute> attrs = new ArrayList<Attribute>(2);
      attrs.add(new Attribute(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE, bundleState.getSymbolicName(), false));
      attrs.add(new Attribute(Constants.BUNDLE_VERSION_ATTRIBUTE, bundleState.getVersion(), false));
      CapabilityImpl modcap = new CapabilityImpl(this, Capability.MODULE_NAMESPACE, new ArrayList<Directive>(0), attrs);
      result.add(0, modcap);

      return Collections.unmodifiableList(result);
   }

   @Override
   public List<Requirement> createRequirements()
   {
      reqMap = new LinkedHashMap<OSGiRequirement, Requirement>();
      if (getModuleDelegate().getRequirements() != null)
      {
         for (org.jboss.classloading.spi.metadata.Requirement mcreq : getModuleDelegate().getRequirements())
         {
            // Add the package requirements
            if (mcreq instanceof OSGiPackageRequirement)
            {
               OSGiPackageRequirement osgireq = (OSGiPackageRequirement)mcreq;
               PackageAttribute metadata = osgireq.getMetadata();

               // Get the requirements attributes
               List<Attribute> attrs = new ArrayList<Attribute>();
               attrs.add(new Attribute(Capability.PACKAGE_ATTR, osgireq.getName(), false));
               for (Entry<String, Parameter> entry : metadata.getAttributes().entrySet())
                  attrs.add(new Attribute(entry.getKey(), entry.getValue().getValue(), false));

               // Get the requirements directives
               List<Directive> dirs = new ArrayList<Directive>();
               for (Entry<String, Parameter> entry : metadata.getDirectives().entrySet())
                  dirs.add(new Directive(entry.getKey(), entry.getValue().getValue()));

               reqMap.put(osgireq, new RequirementImpl(this, Capability.PACKAGE_NAMESPACE, dirs, attrs));
            }
            else
            {
               log.warn("Unsupported requirement: " + mcreq);
            }
         }
      }

      ArrayList<Requirement> result = new ArrayList<Requirement>(reqMap.values());
      return Collections.unmodifiableList(result);
   }

   Capability getMappedCapability(OSGiCapability osgicap)
   {
      if (capMap == null)
         throw new IllegalStateException("Capability map not yet created for: " + getBundle());

      return capMap.get(osgicap);
   }

   Requirement getMappedRequirement(OSGiRequirement osgireq)
   {
      if (reqMap == null)
         throw new IllegalStateException("Requirement map not yet created for: " + getBundle());

      return reqMap.get(osgireq);
   }

   private OSGiModule getModuleDelegate()
   {
      if (moduleDelegate == null)
      {
         OSGiBundleState bundleState = (OSGiBundleState)getBundle();
         moduleDelegate = (OSGiModule)bundleState.getDeploymentUnit().getAttachment(org.jboss.classloading.spi.dependency.Module.class);
         if (moduleDelegate == null)
            throw new IllegalStateException("No OSGiModule attached to: " + bundleState);
      }
      return moduleDelegate;
   }
}