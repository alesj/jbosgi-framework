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
package org.jboss.osgi.framework.resolver;

import java.util.Map;

import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * The abstract implementation of a {@link XPackageRequirement}.
 *
 * @author thomas.diesler@jboss.com
 * @since 02-Jul-2010
 */
class AbstractPackageRequirement extends AbstractRequirement implements XPackageRequirement
{
   private XVersionRange versionRange = XVersionRange.infiniteRange;
   private String resolution;
   private boolean dynamic;

   public AbstractPackageRequirement(AbstractModule module, String name, Map<String, String> dirs, Map<String, String> atts, boolean dynamic)
   {
      super(module, name, dirs, atts);

      this.dynamic = dynamic;
      if (dynamic == false)
      {
         String dir = getDirective(Constants.RESOLUTION_DIRECTIVE);
         resolution = (dir != null ? dir : Constants.RESOLUTION_MANDATORY);
      }
      
      String att = getAttribute(Constants.VERSION_ATTRIBUTE);
      if (att != null)
         versionRange = XVersionRange.parse(att);
   }

   @Override
   public String getResolution()
   {
      return resolution;
   }

   @Override
   public XVersionRange getVersionRange()
   {
      return versionRange;
   }

   @Override
   public String getBundleSymbolicName()
   {
      return getAttribute(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE);
   }

   @Override
   public Version getBundleVersion()
   {
      String att = getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE);
      return (att != null ? Version.parseVersion(att) : null);
   }

   public boolean isDynamic()
   {
      return dynamic;
   }
}