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

import java.util.Set;

import org.osgi.framework.Version;

/**
 * An abstraction of a package export.
 * 
 * @author thomas.diesler@jboss.com
 * @since 09-Nov-2009
 */
public interface ExportPackage extends NamedElement
{
   /**
    * Get the version of this export.
    * 
    * @return the version or '0.0.0' if there is no version on the export
    */
   Version getVersion();
   
   /**
    * Get the set of packages that is used by this export.
    * 
    * @return the list of uses
    */
   Set<String> getUses();
   
   /**
    * Get the set of mandadtory attributes declared by this export.
    * 
    * @return An empty list if there are no mandatory exports declared.
    */
   Set<String> getMandatory();
   
   /**
    * Get the set of included classes in this package export.
    * 
    * @return Null if there are no includes defined.
    */
   Set<String> getIncludes();
   
   /**
    * Get the set of excluded classes in this package export.
    * 
    * @return Null if there are no excludes defined.
    */
   Set<String> getExcludes();

   /**
    * Match the attributes of the given export package
    * 
    * In order for an import definition to be resolved to an export definition, 
    * the values of the attributes specified by the import definition must match the values
    * of the attributes of the export definition. By default, a match is not prevented 
    * if the export definition contains attributes that do not occur in the import definition.
    *  
    * The mandatory directive in the export definition can reverse this by listing all 
    * attributes that the Framework must match in the import definition.
    *  
    * @return true if the attributes match
    */
   boolean matchAttributes(ImportPackage importPackage);
   
   /**
    * Get the current set of importers of this export package.
    * 
    * @return An empty set if there is no importer
    */
   Set<ImportPackage> getImporters();

   /**
    * Add an importer of this export package.
    * 
    * @param importer the importer
    */
   void addImporter(ImportPackage importer);
   
   /**
    * Remove an importer of this export package.
    * 
    * @param importer the importer
    */
   void removeImporter(ImportPackage importer);
}