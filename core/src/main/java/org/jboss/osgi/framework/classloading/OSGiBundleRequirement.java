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
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.metadata.Parameter;
import org.jboss.osgi.framework.metadata.ParameterizedAttribute;
import org.jboss.osgi.framework.metadata.internal.AbstractVersionRange;
import org.osgi.framework.Constants;

/**
 * OSGiBundleRequirement.
 * 
 * todo BundlePermission/REQUIRE
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author thomas.diesler@jboss.com
 * @version $Revision: 1.1 $
 */
public class OSGiBundleRequirement extends ModuleRequirement implements OSGiRequirement
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 4264597072894634275L;

   private AbstractBundleState bundleState;
   private ParameterizedAttribute metadata;
   private String visibility;
   private String resolution;

   public static OSGiBundleRequirement create(AbstractBundleState bundleState, ParameterizedAttribute metadata)
   {
      if (metadata == null)
         throw new IllegalArgumentException("Null metadata");

      String name = metadata.getAttribute();

      AbstractVersionRange range = null;
      String version = metadata.getAttributeValue(Constants.BUNDLE_VERSION_ATTRIBUTE, String.class);
      if (version != null)
         range = (AbstractVersionRange)AbstractVersionRange.valueOf(version);

      String visibility = metadata.getDirectiveValue(Constants.VISIBILITY_DIRECTIVE, String.class);
      String resolution = metadata.getDirectiveValue(Constants.RESOLUTION_DIRECTIVE, String.class);

      return new OSGiBundleRequirement(bundleState, name, range, visibility, resolution, metadata);
   }

   private OSGiBundleRequirement(AbstractBundleState bundleState, String name, VersionRange versionRange, String visdir, String resdir, ParameterizedAttribute metadata)
   {
      super(name, versionRange);

      if (bundleState == null)
         throw new IllegalArgumentException("Null bundleState");
      if (metadata == null)
         throw new IllegalArgumentException("Null metadata");

      this.bundleState = bundleState;
      this.metadata = metadata;

      visibility = visdir;
      if (visibility == null)
         visibility = Constants.VISIBILITY_PRIVATE;

      resolution = resdir;
      if (resolution == null)
         resolution = Constants.RESOLUTION_MANDATORY;

      if (Constants.VISIBILITY_REEXPORT.equals(visibility))
         setReExport(true);

      if (Constants.RESOLUTION_OPTIONAL.equals(resolution))
         setOptional(true);
   }

   @Override
   public AbstractBundleState getBundle()
   {
      return bundleState;
   }

   public ParameterizedAttribute getMetadata()
   {
      return metadata;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj == this)
         return true;
      if (obj == null || obj instanceof OSGiBundleRequirement == false)
         return false;
      if (super.equals(obj) == false)
         return false;

      return true;
   }

   @Override
   protected void toString(StringBuffer buffer)
   {
      super.toString(buffer);
      Map<String, Parameter> directives = metadata.getDirectives();
      if (directives.containsKey(Constants.VISIBILITY_DIRECTIVE) == false)
         buffer.append(Constants.VISIBILITY_DIRECTIVE + ":=" + visibility);
      if (directives.containsKey(Constants.RESOLUTION_DIRECTIVE) == false)
         buffer.append(Constants.RESOLUTION_DIRECTIVE + ":=" + resolution);
      Map<String, Parameter> attributes = metadata.getAttributes();
      if (attributes != null && attributes.isEmpty() == false)
         buffer.append(attributes);
   }
}
