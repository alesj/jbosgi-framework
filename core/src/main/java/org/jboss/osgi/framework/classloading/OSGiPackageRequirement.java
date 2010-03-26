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

import org.jboss.classloading.plugins.metadata.PackageRequirement;
import org.jboss.classloading.spi.dependency.Module;
import org.jboss.classloading.spi.version.VersionRange;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.bundle.AbstractDeployedBundleState;
import org.jboss.osgi.framework.metadata.PackageAttribute;
import org.jboss.osgi.framework.metadata.Parameter;
import org.jboss.osgi.framework.metadata.internal.AbstractVersionRange;
import org.osgi.framework.Constants;

/**
 * A classloading requirement that extends a {@link PackageRequirement} by OSGi metadata.
 *
 * todo PackagePermission/IMPORT
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author thomas.diesler@jboss.com
 * @version $Revision: 1.1 $
 */
public class OSGiPackageRequirement extends PackageRequirement
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 5109907232396093061L;

   /** The bundle state */
   private AbstractBundleState bundleState;

   /** The attributes */
   private PackageAttribute packageAttribute;

   /**
    * Create a new OSGiPackageRequirement.
    * 
    * @param bundleState the bundle state
    * @param packageAttribute the require package metadata
    * @return the requirement
    * @throws IllegalArgumentException for a null requirePackage
    */
   @SuppressWarnings("deprecation")
   public static OSGiPackageRequirement create(AbstractBundleState bundleState, PackageAttribute packageAttribute, boolean isDynamic)
   {
      if (bundleState == null)
         throw new IllegalArgumentException("Null bundle");
      if (packageAttribute == null)
         throw new IllegalArgumentException("Null require package");

      String name = packageAttribute.getAttribute();

      AbstractVersionRange range = null;
      String versionString = packageAttribute.getAttributeValue(Constants.VERSION_ATTRIBUTE, String.class);
      if (versionString != null)
      {
         range = (AbstractVersionRange)AbstractVersionRange.valueOf(versionString);
         String oldVersionString = packageAttribute.getAttributeValue(Constants.PACKAGE_SPECIFICATION_VERSION, String.class);
         if (oldVersionString != null && oldVersionString.equals(versionString) == false)
            throw new IllegalStateException(Constants.VERSION_ATTRIBUTE + " of " + versionString + " does not match " + Constants.PACKAGE_SPECIFICATION_VERSION
                  + " of " + oldVersionString);
      }
      else
      {
         versionString = packageAttribute.getAttributeValue(Constants.PACKAGE_SPECIFICATION_VERSION, String.class);
         if (versionString != null)
            range = (AbstractVersionRange)AbstractVersionRange.valueOf(versionString);
      }

      return new OSGiPackageRequirement(bundleState, name, range, packageAttribute, isDynamic);
   }

   /**
    * Create a new OSGiPackageRequirement.
    * 
    * @param bundleState the bundleState
    * @param name the name
    * @param versionRange the version range - pass null for all versions
    * @param packageAttribute the require package metadata
    * @throws IllegalArgumentException for a null name or requirePackage
    */
   public OSGiPackageRequirement(AbstractBundleState bundleState, String name, VersionRange versionRange, PackageAttribute packageAttribute, boolean isDynamic)
   {
      super(name, versionRange);
      if (bundleState == null)
         throw new IllegalArgumentException("Null bundleState");
      if (packageAttribute == null)
         throw new IllegalArgumentException("Null packageAttribute");

      this.bundleState = bundleState;
      this.packageAttribute = packageAttribute;

      // resolution:=optional
      String resolution = packageAttribute.getDirectiveValue(Constants.RESOLUTION_DIRECTIVE, String.class);
      if (Constants.RESOLUTION_OPTIONAL.equals(resolution))
         setOptional(true);

      // DynamicImport-Package
      if (isDynamic == true)
         setDynamic(true);
   }

   /**
    * Get the requirePackage metadata.
    * 
    * @return the requirePackage.
    */
   public PackageAttribute getPackageMetaData()
   {
      return packageAttribute;
   }

   /**
    * Get the Module associated with this requirement
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

   @Override
   public boolean equals(Object obj)
   {
      if (obj == this)
         return true;
      if (obj == null || obj instanceof OSGiPackageRequirement == false)
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
         Map<String, Parameter> attributes = packageAttribute.getAttributes();
         Map<String, Parameter> directives = packageAttribute.getDirectives();
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
