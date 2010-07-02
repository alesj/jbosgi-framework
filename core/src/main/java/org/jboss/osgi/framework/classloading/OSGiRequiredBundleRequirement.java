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

import org.jboss.classloading.spi.version.VersionRange;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.metadata.internal.AbstractVersionRange;
import org.jboss.osgi.framework.resolver.XBundleRequirement;
import org.osgi.framework.Constants;

/**
 * An {@link OSGiBundleRequirement} for a Require-Bundle attribute.
 * 
 * @author thomas.diesler@jboss.com
 * @since 11-Jun-2010
 */
public class OSGiRequiredBundleRequirement extends OSGiBundleRequirement
{
   private static final long serialVersionUID = 1L;

   public static OSGiRequiredBundleRequirement create(AbstractBundleState bundleState, XBundleRequirement metadata)
   {
      String name = metadata.getName();

      String versionStr = metadata.getVersionRange().toString();
      AbstractVersionRange versionRange = (AbstractVersionRange)AbstractVersionRange.valueOf(versionStr);

      return new OSGiRequiredBundleRequirement(bundleState, name, versionRange, metadata);
   }

   private OSGiRequiredBundleRequirement(AbstractBundleState bundleState, String name, VersionRange versionRange, XBundleRequirement metadata)
   {
      super(bundleState, name, versionRange, metadata);

      String visibility = metadata.getVisibility();
      if (Constants.VISIBILITY_REEXPORT.equals(visibility))
         setReExport(true);

      String resolution = metadata.getResolution();
      if (Constants.RESOLUTION_OPTIONAL.equals(resolution))
         setOptional(true);
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj == this)
         return true;
      if (obj == null || obj instanceof OSGiRequiredBundleRequirement == false)
         return false;
      if (super.equals(obj) == false)
         return false;

      return true;
   }

   @Override
   protected void toString(StringBuffer buffer)
   {
      super.toString(buffer);
      Map<String, String> directives = getMetadata().getDirectives();
      buffer.append(";" + directives);
      Map<String, String> attributes = getMetadata().getAttributes();
      buffer.append(";" + attributes);
   }
}
