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

import org.jboss.classloader.spi.ImportType;
import org.jboss.classloading.plugins.metadata.PackageRequirement;
import org.jboss.classloading.spi.dependency.Module;
import org.jboss.classloading.spi.metadata.RequirementWithImportType;
import org.jboss.classloading.spi.version.VersionRange;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.bundle.DeployedBundleState;
import org.jboss.osgi.framework.metadata.internal.AbstractVersionRange;
import org.jboss.osgi.resolver.XPackageRequirement;
import org.osgi.framework.Constants;

/**
 * A classloading requirement that extends a {@link PackageRequirement} by OSGi metadata.
 *
 * todo PackagePermission/IMPORT
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author thomas.diesler@jboss.com
 * @version $Revision: 1.1 $
 */
public class OSGiPackageRequirement extends PackageRequirement implements RequirementWithImportType, OSGiRequirement
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 5109907232396093061L;

   /** The bundle state */
   private AbstractBundleState bundleState;

   /** The attributes */
   private XPackageRequirement packageReq;

   /**
    * Create a new OSGiPackageRequirement.
    */
   public static OSGiPackageRequirement create(AbstractBundleState bundleState, XPackageRequirement metadata)
   {
      if (bundleState == null)
         throw new IllegalArgumentException("Null bundle");
      if (metadata == null)
         throw new IllegalArgumentException("Null require package");

      String name = metadata.getName();

      String versionString = metadata.getVersionRange().toString();
      AbstractVersionRange range = AbstractVersionRange.valueOf(versionString);

      return new OSGiPackageRequirement(metadata, bundleState, name, range);
   }

   private OSGiPackageRequirement(XPackageRequirement packageReq, AbstractBundleState bundleState, String name, VersionRange versionRange)
   {
      super(name, versionRange);
      if (bundleState == null)
         throw new IllegalArgumentException("Null bundleState");
      if (packageReq == null)
         throw new IllegalArgumentException("Null packageAttribute");

      this.bundleState = bundleState;
      this.packageReq = packageReq;

      // resolution:=optional
      String resolution = packageReq.getResolution();
      if (Constants.RESOLUTION_OPTIONAL.equals(resolution))
         setOptional(true);

      // DynamicImport-Package
      if (packageReq.isDynamic() == true)
         setDynamic(true);
      
      packageReq.addAttachment(OSGiRequirement.class, this);
   }

   @Override
   public AbstractBundleState getBundleState()
   {
      return bundleState;
   }

   @Override
   public XPackageRequirement getResolverElement()
   {
      return packageReq;
   }

   @Override
   public ImportType getImportType()
   {
      // In OSGi package imports declared in DynamicImport-Package should be looked at 
      // AFTER the packages embedded into the bundle
      return (isDynamic() ? ImportType.AFTER : ImportType.BEFORE);
   }

   /**
    * Get the Module associated with this requirement
    * 
    * @return the module
    */
   public Module getModule()
   {
      Module module = null;
      if (bundleState instanceof DeployedBundleState)
      {
         DeployedBundleState depBundle = (DeployedBundleState)bundleState;
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
         Map<String, Object> attributes = packageReq.getAttributes();
         Map<String, String> directives = packageReq.getDirectives();
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
