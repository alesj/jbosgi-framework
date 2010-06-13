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

/**
 * OSGiBundleRequirement.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author thomas.diesler@jboss.com
 * @since 11-Jun-2010
 */
abstract class OSGiBundleRequirement extends ModuleRequirement implements OSGiRequirement
{
   private static final long serialVersionUID = 1L;
   
   private AbstractBundleState bundleState;
   private ParameterizedAttribute metadata;

   OSGiBundleRequirement(AbstractBundleState bundleState, String name, VersionRange versionRange, ParameterizedAttribute metadata)
   {
      super(name, versionRange);

      if (bundleState == null)
         throw new IllegalArgumentException("Null bundleState");
      if (metadata == null)
         throw new IllegalArgumentException("Null metadata");

      this.bundleState = bundleState;
      this.metadata = metadata;
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
      Map<String, Parameter> attributes = getMetadata().getAttributes();
      if (attributes != null && attributes.isEmpty() == false)
         buffer.append("," + attributes);
   }
}
