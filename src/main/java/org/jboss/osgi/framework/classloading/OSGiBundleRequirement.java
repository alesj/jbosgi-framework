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

import org.jboss.classloading.plugins.metadata.ModuleRequirement;
import org.jboss.classloading.spi.version.VersionRange;
import org.jboss.osgi.framework.metadata.Parameter;
import org.jboss.osgi.framework.metadata.ParameterizedAttribute;
import org.jboss.osgi.framework.metadata.internal.AbstractVersionRange;
import org.osgi.framework.Constants;

/**
 * OSGiBundleRequirement.
 * 
 * todo BundlePermission/REQUIRE
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author Thomas.Diesler@jboss.com
 * @version $Revision: 1.1 $
 */
public class OSGiBundleRequirement extends ModuleRequirement
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 4264597072894634275L;
   
   /** The attributes */
   private ParameterizedAttribute requireBundle;

   /**
    * Create a new OSGiBundleRequirement.
    * 
    * @param requireBundle the require bundle metadata
    * @return the requirement
    * @throws IllegalArgumentException for a null requireBundle
    */
   public static OSGiBundleRequirement create(ParameterizedAttribute requireBundle)
   {
      if (requireBundle == null)
         throw new IllegalArgumentException("Null require bundle");

      String name = requireBundle.getAttribute();
      
      AbstractVersionRange range = null;
      String version = requireBundle.getAttributeValue(Constants.BUNDLE_VERSION_ATTRIBUTE, String.class);
      if (version != null)
         range = (AbstractVersionRange) AbstractVersionRange.valueOf(version);

      return new OSGiBundleRequirement(name, range, requireBundle);
   }
   
   /**
    * Create a new OSGiBundleRequirement.
    * 
    * @param name the name
    * @param versionRange the version range - pass null for all versions
    * @param requireBundle the require bundle metadata
    * @throws IllegalArgumentException for a null name or requireBundle
    */
   public OSGiBundleRequirement(String name, VersionRange versionRange, ParameterizedAttribute requireBundle)
   {
      super(name, versionRange);
      if (requireBundle == null)
         throw new IllegalArgumentException("Null requireBundle");
      this.requireBundle = requireBundle;

      String visibility = requireBundle.getDirectiveValue(Constants.VISIBILITY_DIRECTIVE, String.class);
      if (Constants.VISIBILITY_REEXPORT.equals(visibility))
         setReExport(true);

      String resolution = requireBundle.getDirectiveValue(Constants.RESOLUTION_DIRECTIVE, String.class);
      if (Constants.RESOLUTION_OPTIONAL.equals(resolution))
         setOptional(true);
   }
   
   /**
    * Get the requireBundle metadata.
    * 
    * @return the requireBundle.
    */
   public ParameterizedAttribute getRequireBundle()
   {
      return requireBundle;
   }
   
   @Override
   public boolean equals(Object obj)
   {
      if (obj == this)
         return true;
      if (obj == null || obj instanceof OSGiBundleRequirement == false)
         return false;
      if (super.equals(obj) ==false)
         return false;
      
      return true;
   }

   @Override
   protected void toString(StringBuffer buffer)
   {
      super.toString(buffer);
      Map<String, Parameter> parameters = requireBundle.getAttributes();
      if (parameters != null && parameters.isEmpty() == false)
         buffer.append(" attributes=").append(parameters);
   }
}
