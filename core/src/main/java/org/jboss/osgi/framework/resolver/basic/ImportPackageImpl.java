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
package org.jboss.osgi.framework.resolver.basic;

import org.jboss.osgi.framework.metadata.Parameter;
import org.jboss.osgi.framework.metadata.ParameterizedAttribute;
import org.jboss.osgi.framework.metadata.VersionRange;
import org.jboss.osgi.framework.metadata.internal.AbstractVersionRange;
import org.osgi.framework.Constants;

/**
 * An abstraction of a package import. 
 * 
 * @author thomas.diesler@jboss.com
 * @since 09-Nov-2009
 */
public class ImportPackageImpl extends NamedElementImpl implements ImportPackage
{
   private ExportPackage exporter;

   public ImportPackageImpl(ResolverBundle owner, ParameterizedAttribute attr)
   {
      super(owner, attr);
   }

   public VersionRange getVersion()
   {
      VersionRange versionRange = AbstractVersionRange.valueOf("0.0.0");
      Parameter version = getParameterizedAttribute().getAttribute(Constants.VERSION_ATTRIBUTE);
      if (version != null)
         versionRange = AbstractVersionRange.valueOf((String)version.getValue());
      return versionRange;
   }

   public String getBundleSymbolicName()
   {
      return (String)getAttribute(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE);
   }

   public VersionRange getBundleVersion()
   {
      Parameter version = getParameterizedAttribute().getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE);
      return (version != null ? AbstractVersionRange.valueOf((String)version.getValue()) : null);
   }

   public boolean isOptional()
   {
      boolean optional = false;
      Parameter resolution = getParameterizedAttribute().getDirective(Constants.RESOLUTION_DIRECTIVE);
      if (resolution != null)
         optional = Constants.RESOLUTION_OPTIONAL.equals(resolution.getValue());
      return optional;
   }

   public ExportPackage getExporter()
   {
      return exporter;
   }

   public void setExporter(ExportPackage exporter)
   {
      this.exporter = exporter;
      ExportPackageImpl exporterImpl = (ExportPackageImpl)exporter;
      exporterImpl.addImporter(this);

   }

   public String toShortString()
   {
      AbstractResolverBundle owner = (AbstractResolverBundle)getOwner();
      return owner.toShortString() + super.toShortString();
   }
   
   public String toString()
   {
      return "ImportPackage[" + toShortString() + "]";
   }
}