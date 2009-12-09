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
package org.jboss.osgi.framework.resolver.internal.drools;

import org.jboss.osgi.framework.resolver.ExportPackage;
import org.jboss.osgi.framework.resolver.ImportPackage;
import org.jboss.osgi.framework.resolver.internal.ExportPackageImpl;
import org.jboss.osgi.framework.resolver.internal.ImportPackageImpl;

/**
 * A Resolver that is based on the Drools rule engine.
 * 
 * @author thomas.diesler@jboss.com
 * @since 16-Nov-2009
 */
public class WireCandidate
{
   private ImportPackageImpl importPackage;
   private ExportPackageImpl exportPackage;

   public ImportPackage getImportPackage()
   {
      return importPackage;
   }

   public void setImportPackage(ImportPackage importPackage)
   {
      this.importPackage = (ImportPackageImpl)importPackage;
   }

   public ExportPackage getExportPackage()
   {
      return exportPackage;
   }

   public void setExportPackage(ExportPackage exportPackage)
   {
      this.exportPackage = (ExportPackageImpl)exportPackage;
   }

   @Override
   public String toString()
   {
      return "WireCandidate[" + importPackage.toShortString() + " --> " + exportPackage.toShortString() + "]";
   }
   

}