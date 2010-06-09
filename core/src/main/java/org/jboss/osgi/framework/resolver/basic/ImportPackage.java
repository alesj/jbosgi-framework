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

import org.jboss.osgi.framework.metadata.VersionRange;

/**
 * An abstraction of a package import. 
 * 
 * @author thomas.diesler@jboss.com
 * @since 09-Nov-2009
 */
public interface ImportPackage extends NamedElement
{
   /**
    * Get the version range for this import package.
    * 
    * @return the version range or [0.0.0,?) if there is no version range on the import
    */
   VersionRange getVersion();

   /**
    * The symbolic name of the exporting bundle.
    * 
    * @return null if this attribute is not set
    */
   String getBundleSymbolicName();

   /**
    * The version range of the exporting bundle.
    * 
    * @return null if this attribute is not set
    */
   VersionRange getBundleVersion();

   /**
    * True if the resolution directive for this package import is 'optional' 
    */
   boolean isOptional();
   
   /**
    * Get the exporter that this import  package is wired to.
    * 
    * @return Null if the import is not yet resolved. 
    */
   ExportPackage getExporter();
   
   /**
    * Set the exporter that this import  package is wired to.
    * 
    * @param exporter the exporter
    */
   void setExporter(ExportPackage exporter);
}