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

import java.util.Map;
import java.util.StringTokenizer;

import org.jboss.classloader.spi.filter.ClassFilter;
import org.jboss.classloading.plugins.metadata.PackageCapability;
import org.jboss.classloading.spi.dependency.Module;
import org.jboss.classloading.spi.metadata.Requirement;
import org.jboss.classloading.spi.version.VersionRange;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.bundle.AbstractDeployedBundleState;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.bundle.OSGiBundleState;
import org.jboss.osgi.framework.metadata.OSGiMetaData;
import org.jboss.osgi.framework.metadata.PackageAttribute;
import org.jboss.osgi.framework.metadata.Parameter;
import org.jboss.osgi.framework.metadata.internal.AbstractVersionRange;
import org.jboss.osgi.framework.plugins.ResolverPlugin;
import org.jboss.osgi.framework.resolver.Resolver;
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
   private PackageAttribute metadata;

   /** The mandatory attributes */
   private String[] mandatoryAttributes;

   /**
    * Create a new OSGiPackageCapability.
    * 
    * @param bundleState the bundle state
    * @param metadata the export package metadata
    * @return the capability
    * @throws IllegalArgumentException for null metadata
    */
   @SuppressWarnings("deprecation")
   public static OSGiPackageCapability create(AbstractBundleState bundleState, PackageAttribute metadata)
   {
      if (bundleState == null)
         throw new IllegalArgumentException("Null bundle");

      String name = metadata.getAttribute();
      String versionString = metadata.getAttributeValue(Constants.VERSION_ATTRIBUTE, String.class);

      String oldVersionString = metadata.getAttributeValue(Constants.PACKAGE_SPECIFICATION_VERSION, String.class);
      if (oldVersionString != null)
      {
         if (versionString != null && versionString.equals(oldVersionString) == false)
            throw new IllegalStateException(Constants.VERSION_ATTRIBUTE + " of " + versionString + " does not match " + Constants.PACKAGE_SPECIFICATION_VERSION
                  + " of " + oldVersionString);
         if (versionString == null)
            versionString = oldVersionString;
      }

      Version version = null;
      if (versionString != null)
      {
         // Handle version strings with quotes 
         if (versionString.startsWith("\"") && versionString.endsWith("\""))
            versionString = versionString.substring(1, versionString.length() - 1);

         version = Version.parseVersion(versionString);
      }

      OSGiPackageCapability capability = new OSGiPackageCapability(bundleState, name, version, metadata);
      capability.setSplitPackagePolicy(SplitPackagePolicy.First);

      return capability;
   }

   private OSGiPackageCapability(AbstractBundleState bundleState, String name, Version version, PackageAttribute metadata)
   {
      super(name, version);
      this.bundleState = bundleState;
      this.metadata = metadata;

      String mandatory = metadata.getDirectiveValue(Constants.MANDATORY_DIRECTIVE, String.class);
      if (mandatory != null)
      {
         StringTokenizer tokens = new StringTokenizer(mandatory, ",");
         mandatoryAttributes = new String[tokens.countTokens()];
         int i = 0;
         while (tokens.hasMoreTokens())
            mandatoryAttributes[i++] = tokens.nextToken();
      }

      if (metadata.getAttribute(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE) != null)
         throw new IllegalStateException("You cannot specify " + Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE + " on an Export-Package");
      if (metadata.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE) != null)
         throw new IllegalStateException("You cannot specify " + Constants.BUNDLE_VERSION_ATTRIBUTE + " on an Export-Package");
   }

   public PackageAttribute getMetadata()
   {
      return metadata;
   }

   @Override
   public boolean resolves(Module reqModule, Requirement requirement)
   {
      if (super.resolves(reqModule, requirement) == false)
         return false;
      if (requirement instanceof OSGiPackageRequirement == false)
         return true;

      OSGiPackageRequirement osgireq = (OSGiPackageRequirement)requirement;
      if (matchAttributes(osgireq) == false)
         return false;

      OSGiBundleManager bundleManager = bundleState.getBundleManager();
      Resolver bundleResolver = bundleManager.getOptionalPlugin(ResolverPlugin.class);
      if (bundleResolver != null)
      {
         // Get the bundle associated with the requirement
         String reqLocation = reqModule.getContextName();
         AbstractBundleState reqBundle = bundleManager.getBundleByLocation(reqLocation);
         if (reqBundle == null)
            throw new IllegalStateException("Cannot get bundle for: " + reqLocation);
         
         // Get the exporter for this requirement
         if (reqBundle instanceof OSGiBundleState)
         {
            boolean match = bundleResolver.match(reqBundle, bundleState, osgireq);
            return match;
         }
      }

      return true;
   }

   /**
    * Get the Module associated with this capability
    * 
    * @return the module
    */
   public Module getModule()
   {
      Module module = null;
      if (bundleState instanceof AbstractDeployedBundleState)
      {
         AbstractDeployedBundleState depBundle = (AbstractDeployedBundleState)bundleState; 
         DeploymentUnit unit = depBundle.getDeploymentUnit();
         module = unit.getAttachment(Module.class);
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
      else // for non-wildcard, we intentionaly still use direct string equals
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
      PackageAttribute capParameters = metadata;
      PackageAttribute reqParameters = packageRequirement.getMetadata();

      boolean validMatch = true;

      // Check all the manadatory attributes are present
      if (validMatch == true && mandatoryAttributes != null)
      {
         for (String mand : mandatoryAttributes)
         {
            Parameter reqAttributeValue = reqParameters.getAttribute(mand);
            if (reqParameters == null || reqAttributeValue == null)
            {
               validMatch = false;
               break;
            }
         }
      }

      if (validMatch == true && reqParameters != null)
      {
         Map<String, Parameter> params = reqParameters.getAttributes();
         if (params != null && params.isEmpty() == false)
         {
            for (String name : params.keySet())
            {
               String otherValue = reqParameters.getAttributeValue(name, String.class);
               String ourValue = capParameters.getAttributeValue(name, String.class);

               if (Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE.equals(name))
               {
                  if (otherValue.equals(osgiMetaData.getBundleSymbolicName()) == false)
                     validMatch = false;
               }
               else if (Constants.BUNDLE_VERSION_ATTRIBUTE.equals(name))
               {
                  VersionRange range = (VersionRange)AbstractVersionRange.valueOf(otherValue);
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
         Map<String, Parameter> attributes = metadata.getAttributes();
         Map<String, Parameter> directives = metadata.getDirectives();
         for (Map.Entry<String, Parameter> entry : directives.entrySet())
            buffer.append(";" + entry.getKey() + ":=" + entry.getValue().getValue());
         for (Map.Entry<String, Parameter> entry : attributes.entrySet())
            buffer.append(";" + entry.getKey() + "=" + entry.getValue().getValue());
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
