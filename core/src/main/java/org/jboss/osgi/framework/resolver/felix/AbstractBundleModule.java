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
import java.util.Map.Entry;

import org.apache.felix.framework.capabilityset.Attribute;
import org.apache.felix.framework.capabilityset.Capability;
import org.apache.felix.framework.capabilityset.Directive;
import org.apache.felix.framework.capabilityset.Requirement;
import org.apache.felix.framework.util.VersionRange;
import org.apache.felix.framework.util.manifestparser.CapabilityImpl;
import org.apache.felix.framework.util.manifestparser.RequirementImpl;
import org.jboss.logging.Logger;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.classloading.OSGiBundleCapability;
import org.jboss.osgi.framework.classloading.OSGiCapability;
import org.jboss.osgi.framework.classloading.OSGiFragmentHostRequirement;
import org.jboss.osgi.framework.classloading.OSGiPackageCapability;
import org.jboss.osgi.framework.classloading.OSGiPackageRequirement;
import org.jboss.osgi.framework.classloading.OSGiRequiredBundleRequirement;
import org.jboss.osgi.framework.classloading.OSGiRequirement;
import org.jboss.osgi.framework.metadata.PackageAttribute;
import org.jboss.osgi.framework.metadata.Parameter;
import org.jboss.osgi.framework.metadata.ParameterizedAttribute;
import org.jboss.osgi.framework.resolver.AbstractModule;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * An JBoss specific implementation of a Resolver Module
 *  
 * @author thomas.diesler@jboss.com
 * @since 31-May-2010
 */
abstract class AbstractBundleModule extends AbstractModule
{
   // Provide logging
   final Logger log = Logger.getLogger(AbstractBundleModule.class);

   private Map<Capability, OSGiCapability> capMap;
   private Map<OSGiRequirement, Requirement> reqMap;
   private Map<OSGiRequirement, Requirement> dynReqMap;

   public AbstractBundleModule(AbstractBundleState bundleState)
   {
      super(bundleState);
   }

   abstract List<OSGiCapability> getOSGiCapabilities();

   abstract List<OSGiRequirement> getOSGiRequirements();

   @Override
   public List<Capability> createCapabilities()
   {
      capMap = new LinkedHashMap<Capability, OSGiCapability>();
      for (OSGiCapability mccap : getOSGiCapabilities())
      {
         // Add a module capability and a host capability to all non-fragment bundles. 
         // A host capability is the same as a module capability, but with a different capability namespace. 
         // Module capabilities resolve required-bundle dependencies, while host capabilities resolve fragment-host dependencies.
         if (mccap instanceof OSGiBundleCapability)
         {
            OSGiBundleCapability osgicap = (OSGiBundleCapability)mccap;
            if (mccap.getBundleState().isFragment() == false)
            {
               List<Attribute> attrs = new ArrayList<Attribute>(2);
               attrs.add(new Attribute(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE, osgicap.getName(), false));
               attrs.add(new Attribute(Constants.BUNDLE_VERSION_ATTRIBUTE, osgicap.getVersion(), false));
               capMap.put(new CapabilityImpl(this, Capability.HOST_NAMESPACE, new ArrayList<Directive>(0), attrs), osgicap);
            }
            
            // Always add the module capability 
            List<Attribute> attrs = new ArrayList<Attribute>(2);
            attrs.add(new Attribute(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE, osgicap.getName(), false));
            attrs.add(new Attribute(Constants.BUNDLE_VERSION_ATTRIBUTE, osgicap.getVersion(), false));
            capMap.put(new CapabilityImpl(this, Capability.MODULE_NAMESPACE, new ArrayList<Directive>(0), attrs), osgicap);
         }

         // Add the package capabilities
         else if (mccap instanceof OSGiPackageCapability)
         {
            OSGiPackageCapability osgicap = (OSGiPackageCapability)mccap;
            Capability cap = packageCapability(osgicap);
            capMap.put(cap, osgicap);
         }
         else
         {
            throw new IllegalArgumentException("Unsupported capability: " + mccap);
         }
      }

      ArrayList<Capability> result = new ArrayList<Capability>(capMap.keySet());
      return Collections.unmodifiableList(result);
   }

   @Override
   public List<Requirement> createRequirements()
   {
      reqMap = new LinkedHashMap<OSGiRequirement, Requirement>();
      for (OSGiRequirement mcreq : getOSGiRequirements())
      {
         if (mcreq instanceof OSGiRequiredBundleRequirement)
         {
            OSGiRequiredBundleRequirement osgireq = (OSGiRequiredBundleRequirement)mcreq;
            Requirement req = requireBundleRequiment(osgireq);
            reqMap.put(osgireq, req);
         }
         else if (mcreq instanceof OSGiFragmentHostRequirement)
         {
            OSGiFragmentHostRequirement osgireq = (OSGiFragmentHostRequirement)mcreq;
            Requirement req = fragmentHostRequirement(osgireq);
            reqMap.put(osgireq, req);
         }
         else if (mcreq instanceof OSGiPackageRequirement)
         {
            OSGiPackageRequirement osgireq = (OSGiPackageRequirement)mcreq;
            if (osgireq.isDynamic() == false)
            {
               Requirement req = packageRequirement(osgireq);
               reqMap.put(osgireq, req);
            }
         }
         else
         {
            throw new IllegalArgumentException("Unsupported requirement: " + mcreq);
         }
      }

      ArrayList<Requirement> result = new ArrayList<Requirement>(reqMap.values());
      return Collections.unmodifiableList(result);
   }

   @Override
   protected List<Requirement> createDynamicRequirements()
   {
      dynReqMap = new LinkedHashMap<OSGiRequirement, Requirement>();
      for (OSGiRequirement mcreq : getOSGiRequirements())
      {
         // Add the package requirements
         if (mcreq instanceof OSGiPackageRequirement)
         {
            OSGiPackageRequirement osgireq = (OSGiPackageRequirement)mcreq;
            if (osgireq.isDynamic() == true)
            {
               Requirement req = packageRequirement(osgireq);
               dynReqMap.put(osgireq, req);
            }
         }
      }

      ArrayList<Requirement> result = new ArrayList<Requirement>(dynReqMap.values());
      return Collections.unmodifiableList(result);
   }

   OSGiCapability getMappedCapability(Capability cap)
   {
      if (capMap == null)
         throw new IllegalStateException("Capability map not yet created for: " + getBundle());

      return capMap.get(cap);
   }

   Requirement getMappedRequirement(OSGiRequirement osgireq)
   {
      if (reqMap == null)
         throw new IllegalStateException("Requirement map not yet created for: " + getBundle());

      return reqMap.get(osgireq);
   }

   private Capability packageCapability(OSGiPackageCapability osgicap)
   {
      AbstractBundleState bundleState = osgicap.getBundleState();
      PackageAttribute metadata = osgicap.getMetadata();

      String symbolicName = null;
      Version bundleVersion = null;

      // Get the capabiliy attributes
      List<Attribute> attrs = new ArrayList<Attribute>();
      attrs.add(new Attribute(Capability.PACKAGE_ATTR, osgicap.getName(), false));
      for (Entry<String, Parameter> entry : metadata.getAttributes().entrySet())
      {
         String key = entry.getKey();
         Object value = (String)entry.getValue().getValue();
         if (Capability.VERSION_ATTR.equals(key))
            value = Version.parseVersion((String)value);
         else if (Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE.equals(key))
            symbolicName = (String)value;
         else if (Constants.BUNDLE_VERSION_ATTRIBUTE.equals(key))
            bundleVersion = Version.parseVersion((String)value);

         attrs.add(new Attribute(key, value, false));
      }

      // Now that we know that there are no bundle symbolic name and version
      // attributes, add them since the spec says they are there implicitly.
      if (symbolicName == null)
      {
         symbolicName = bundleState.getSymbolicName();
         attrs.add(new Attribute(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE, symbolicName, false));
      }
      if (bundleVersion == null)
      {
         bundleVersion = bundleState.getVersion();
         attrs.add(new Attribute(Constants.BUNDLE_VERSION_ATTRIBUTE, bundleVersion, false));
      }

      // Get the capabiliy directives
      List<Directive> dirs = new ArrayList<Directive>();
      for (Entry<String, Parameter> entry : metadata.getDirectives().entrySet())
         dirs.add(new Directive(entry.getKey(), entry.getValue().getValue()));

      CapabilityImpl cap = new CapabilityImpl(this, Capability.PACKAGE_NAMESPACE, dirs, attrs);
      return cap;
   }

   private Requirement packageRequirement(OSGiPackageRequirement osgireq)
   {
      PackageAttribute metadata = osgireq.getMetadata();

      // Get the requirements attributes
      List<Attribute> attrs = new ArrayList<Attribute>();
      attrs.add(new Attribute(Capability.PACKAGE_ATTR, osgireq.getName(), false));
      for (Entry<String, Parameter> entry : metadata.getAttributes().entrySet())
      {
         String key = entry.getKey();
         Object value = (String)entry.getValue().getValue();
         if (Capability.VERSION_ATTR.equals(key))
            value = VersionRange.parse((String)value);
         else if (Constants.BUNDLE_VERSION_ATTRIBUTE.equals(key))
            value = VersionRange.parse((String)value);

         attrs.add(new Attribute(key, value, false));
      }

      // Get the requirements directives
      List<Directive> dirs = new ArrayList<Directive>();
      for (Entry<String, Parameter> entry : metadata.getDirectives().entrySet())
         dirs.add(new Directive(entry.getKey(), entry.getValue().getValue()));

      RequirementImpl req = new RequirementImpl(this, Capability.PACKAGE_NAMESPACE, dirs, attrs);
      return req;
   }

   private Requirement requireBundleRequiment(OSGiRequiredBundleRequirement osgireq)
   {
      ParameterizedAttribute metadata = osgireq.getMetadata();

      // Get the requirements attributes
      List<Attribute> attrs = new ArrayList<Attribute>();
      attrs.add(new Attribute(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE, osgireq.getName(), false));
      for (Entry<String, Parameter> entry : metadata.getAttributes().entrySet())
      {
         String key = entry.getKey();
         Object value = (String)entry.getValue().getValue();
         if (Constants.BUNDLE_VERSION_ATTRIBUTE.equals(key))
            value = VersionRange.parse((String)value);

         attrs.add(new Attribute(key, value, false));
      }

      // Get the requirements directives
      List<Directive> dirs = new ArrayList<Directive>();
      for (Entry<String, Parameter> entry : metadata.getDirectives().entrySet())
         dirs.add(new Directive(entry.getKey(), entry.getValue().getValue()));

      RequirementImpl req = new RequirementImpl(this, Capability.MODULE_NAMESPACE, dirs, attrs);
      return req;
   }

   private Requirement fragmentHostRequirement(OSGiFragmentHostRequirement osgireq)
   {
      ParameterizedAttribute metadata = osgireq.getMetadata();

      // Get the requirements attributes
      List<Attribute> attrs = new ArrayList<Attribute>();
      attrs.add(new Attribute(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE, osgireq.getName(), false));
      for (Entry<String, Parameter> entry : metadata.getAttributes().entrySet())
      {
         String key = entry.getKey();
         Object value = (String)entry.getValue().getValue();
         if (Constants.BUNDLE_VERSION_ATTRIBUTE.equals(key))
            value = VersionRange.parse((String)value);

         attrs.add(new Attribute(key, value, false));
      }

      // Get the requirements directives
      List<Directive> dirs = new ArrayList<Directive>();
      for (Entry<String, Parameter> entry : metadata.getDirectives().entrySet())
         dirs.add(new Directive(entry.getKey(), entry.getValue().getValue()));

      RequirementImpl req = new RequirementImpl(this, Capability.HOST_NAMESPACE, dirs, attrs);
      return req;
   }
}