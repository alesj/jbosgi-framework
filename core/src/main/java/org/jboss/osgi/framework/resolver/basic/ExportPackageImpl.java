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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.osgi.framework.metadata.Parameter;
import org.jboss.osgi.framework.metadata.ParameterizedAttribute;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * An abstraction of a package export.
 * 
 * @author thomas.diesler@jboss.com
 * @since 09-Nov-2009
 */
public class ExportPackageImpl extends NamedElementImpl implements ExportPackage
{
   private Set<ImportPackage> importers = new HashSet<ImportPackage>();
   
   public ExportPackageImpl(ResolverBundle owner, ParameterizedAttribute attr)
   {
      super(owner, attr);
   }

   public Version getVersion()
   {
      Parameter version = getParameterizedAttribute().getAttribute(Constants.VERSION_ATTRIBUTE);
      return Version.parseVersion(version != null ? version.getValue().toString() : null);
   }

   public Set<String> getUses()
   {
      return getDirectiveValues(Constants.USES_DIRECTIVE);
   }

   public Set<String> getExcludes()
   {
      Set<String> valueList = getDirectiveValues(Constants.EXCLUDE_DIRECTIVE);
      return valueList.isEmpty() ? null : valueList;
   }

   public Set<String> getIncludes()
   {
      Set<String> valueList = getDirectiveValues(Constants.INCLUDE_DIRECTIVE);
      return valueList.isEmpty() ? null : valueList;
   }

   public Set<String> getMandatory()
   {
      return getDirectiveValues(Constants.MANDATORY_DIRECTIVE);
   }

   @SuppressWarnings("unchecked")
   private Set<String> getDirectiveValues(String key)
   {
      Set<String> valueList = new HashSet<String>();
      Parameter directive = getParameterizedAttribute().getDirective(key);
      if (directive != null)
      {
         Object value = directive.getValue();
         if (value instanceof Collection<?>)
            valueList.addAll((Collection<String>)value);
         else if (value instanceof String)
            valueList.add((String)value);
         else
            throw new IllegalStateException("Invalid directive value: " + value);
      }
      return Collections.unmodifiableSet(valueList);
   }

   public boolean matchAttributes(ImportPackage importPackage)
   {
      if (importPackage == null)
         throw new IllegalArgumentException("Null importPackage");
      
      boolean match = true;
      Set<String> importAttributes = importPackage.getAttributes();
      Set<String> mandatoryAttributes = getMandatory();

      Set<String> ignoredAttributes = new HashSet<String>();
      ignoredAttributes.add(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE);
      ignoredAttributes.add(Constants.BUNDLE_VERSION_ATTRIBUTE);
      ignoredAttributes.add(Constants.VERSION_ATTRIBUTE);

      // Check the import attributes
      for(String attrKey : importAttributes)
      {
         if (ignoredAttributes.contains(attrKey) == false)
         {
            String impValue = (String)importPackage.getAttribute(attrKey);
            if (impValue != null)
               impValue = impValue.trim();
            
            String expValue = (String)getAttribute(attrKey);
            if (expValue != null)
               expValue = expValue.trim();
            
            if (impValue.equals(expValue) == false)
            {
               match = false;
               break;
            }
         }
      }
      
      // Check the mandatory export attributes
      for(String attrKey : mandatoryAttributes)
      {
         String expValue = (String)getAttribute(attrKey);
         if (expValue == null)
            throw new IllegalStateException("Cannot get mandatory attribute value: " + attrKey);
         
         String impValue = (String)importPackage.getAttribute(attrKey);
         if (impValue == null)
         {
            match = false;
            break;
         }
      }
      
      return match;
   }

   public Set<ImportPackage> getImporters()
   {
      return Collections.unmodifiableSet(importers);
   }
   
   public void addImporter(ImportPackage importer)
   {
      importers.add(importer);
   }
   
   public void removeImporter(ImportPackage importer)
   {
      importers.remove(importer);
   }
   
   public String toShortString()
   {
      AbstractResolverBundle owner = (AbstractResolverBundle)getOwner();
      return owner.toShortString() + super.toShortString();
   }
   
   public String toString()
   {
      return "ExportPackage[" + toShortString() + "]";
   }
}