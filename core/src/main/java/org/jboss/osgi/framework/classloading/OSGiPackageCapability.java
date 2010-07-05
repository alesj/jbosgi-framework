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
package org.jboss.osgi.framework.classloading;

import java.util.List;
import java.util.Map;

import org.jboss.classloader.spi.filter.ClassFilter;
import org.jboss.classloading.plugins.metadata.PackageCapability;
import org.jboss.classloading.spi.dependency.Module;
import org.jboss.classloading.spi.metadata.Requirement;
import org.jboss.classloading.spi.version.VersionRange;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.bundle.DeployedBundleState;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.metadata.OSGiMetaData;
import org.jboss.osgi.framework.metadata.internal.AbstractVersionRange;
import org.jboss.osgi.framework.plugins.ResolverPlugin;
import org.jboss.osgi.framework.resolver.XPackageCapability;
import org.jboss.osgi.framework.resolver.XPackageRequirement;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * OSGiPackageCapability.
 * 
 * todo PackagePermission/EXPORT todo uses todo include/exclude
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author thomas.diesler@jboss.com
 * @version $Revision: 1.1 $
 */
public class OSGiPackageCapability extends PackageCapability implements OSGiCapability
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 3940667616588052822L;

   /** The bundle state */
   private AbstractBundleState bundleState;

   /** The export package */
   private XPackageCapability packageCap;

   /** The mandatory attributes */
   private List<String> mandatoryAttributes;

   /**
    * Create a new OSGiPackageCapability.
    * @param packageCap the export package metadata
    * @param bundleState the bundle state
    * 
    * @return the capability
    * @throws IllegalArgumentException for null metadata
    */
   public static OSGiPackageCapability create(XPackageCapability packageCap, AbstractBundleState bundleState)
   {
      if (bundleState == null)
         throw new IllegalArgumentException("Null bundle");

      String name = packageCap.getName();
      Version version = packageCap.getVersion();

      OSGiPackageCapability capability = new OSGiPackageCapability(packageCap, bundleState, name, version);
      capability.setSplitPackagePolicy(SplitPackagePolicy.First);

      return capability;
   }

   private OSGiPackageCapability(XPackageCapability packageCap, AbstractBundleState bundleState, String name, Version version)
   {
      super(name, version);
      this.bundleState = bundleState;
      this.packageCap = packageCap;

      mandatoryAttributes = packageCap.getMandatory();

      if (packageCap.getAttribute(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE) != null)
         throw new IllegalStateException("You cannot specify " + Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE + " on an Export-Package");
      if (packageCap.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE) != null)
         throw new IllegalStateException("You cannot specify " + Constants.BUNDLE_VERSION_ATTRIBUTE + " on an Export-Package");
      
      packageCap.addAttachment(OSGiCapability.class, this);
   }

   @Override
   public AbstractBundleState getBundleState()
   {
      return bundleState;
   }

   @Override
   public XPackageCapability getResolverElement()
   {
      return packageCap;
   }

   @Override
   public boolean resolves(Module reqModule, Requirement mcreq)
   {
      // The Domain creates PackageRequirements on the fly in Domain.getExportedPackagesInternal()
      // For this, we only match package name and version but not any OSGi constraints
      if (mcreq instanceof OSGiPackageRequirement == false)
      {
         boolean match = super.resolves(reqModule, mcreq);
         return match;
      }

      OSGiPackageRequirement osgireq = (OSGiPackageRequirement)mcreq;

      // Get the optional resolver
      OSGiBundleManager bundleManager = bundleState.getBundleManager();
      ResolverPlugin resolver = bundleManager.getOptionalPlugin(ResolverPlugin.class);

      // If there is no resolver or the requirement is dynamic and non-optional
      // match package name and version plus additional attributes
      if (resolver == null || (osgireq.isDynamic() && osgireq.isOptional() == false))
      {
         boolean match = super.resolves(reqModule, mcreq);
         match &= matchAttributes(osgireq);
         return match;
      }

      // Get the wired capability from the resolver
      OSGiCapability osgicap = resolver.getWiredCapability(osgireq);
      if (osgicap != null)
      {
         boolean match = (osgicap == this);
         return match;
      }

      return false;
   }

   @Override
   public OSGiModule getModule()
   {
      OSGiModule module = null;
      if (bundleState instanceof DeployedBundleState)
      {
         DeployedBundleState depBundle = (DeployedBundleState)bundleState;
         DeploymentUnit unit = depBundle.getDeploymentUnit();
         module = (OSGiModule)unit.getAttachment(Module.class);
         if (module == null)
            throw new IllegalStateException("Cannot obtain module from: " + bundleState);
      }
      return module;
   }

   public boolean matchNameAndVersion(OSGiPackageRequirement packageRequirement)
   {
      if (packageRequirement.isWildcard())
      {
         ClassFilter filter = packageRequirement.toClassFilter();
         if (filter.matchesPackageName(getName()) == false)
            return false;
      }
      else
      // for non-wildcard, we intentionaly still use direct string equals
      {
         if (getName().equals(packageRequirement.getName()) == false)
            return false;
      }

      boolean inRange = packageRequirement.getVersionRange().isInRange(getVersion());
      return inRange;
   }

   @SuppressWarnings("deprecation")
   public boolean matchAttributes(OSGiPackageRequirement packageRequirement)
   {
      OSGiMetaData osgiMetaData = bundleState.getOSGiMetaData();
      XPackageCapability cap = packageCap;
      XPackageRequirement req = packageRequirement.getResolverElement();

      boolean validMatch = true;

      // Check all the manadatory attributes are present
      if (validMatch == true && mandatoryAttributes != null)
      {
         for (String mand : mandatoryAttributes)
         {
            Object reqAttributeValue = req.getAttribute(mand);
            if (req == null || reqAttributeValue == null)
            {
               validMatch = false;
               break;
            }
         }
      }

      if (validMatch == true && req != null)
      {
         Map<String, Object> params = req.getAttributes();
         if (params != null && params.isEmpty() == false)
         {
            for (String name : params.keySet())
            {
               Object otherValue = req.getAttribute(name);
               Object ourValue = cap.getAttribute(name);

               if (Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE.equals(name))
               {
                  if (otherValue.equals(osgiMetaData.getBundleSymbolicName()) == false)
                     validMatch = false;
               }
               else if (Constants.BUNDLE_VERSION_ATTRIBUTE.equals(name))
               {
                  VersionRange range = (VersionRange)AbstractVersionRange.valueOf(otherValue.toString());
                  if (range.isInRange(osgiMetaData.getBundleVersion()) == false)
                     validMatch = false;
               }
               else if (Constants.PACKAGE_SPECIFICATION_VERSION.equals(name) || Constants.VERSION_ATTRIBUTE.equals(name))
               {
                  continue;
               }
               else
               {
                  if (ourValue == null || ourValue.equals(otherValue) == false)
                     validMatch = false;
               }

               if (validMatch == false)
                  break;
            }
         }
      }

      return validMatch;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj == this)
         return true;
      if (obj == null || obj instanceof OSGiPackageCapability == false)
         return false;
      if (super.equals(obj) == false)
         return false;

      return true;
   }

   private String shortString;

   public String toShortString()
   {
      if (shortString == null)
      {
         StringBuffer buffer = new StringBuffer(bundleState.getCanonicalName() + "[" + getName());
         Map<String, Object> attributes = packageCap.getAttributes();
         Map<String, String> directives = packageCap.getDirectives();
         buffer.append(";" + attributes);
         buffer.append(";" + directives);
         buffer.append("]");
         shortString = buffer.toString();
      }
      return shortString;
   }

   @Override
   protected void toString(StringBuffer buffer)
   {
      buffer.append(toShortString());
   }
}
