/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.osgi.framework.deployers;

// $Id$

import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.structure.ContextInfo;
import org.jboss.deployers.vfs.plugins.structure.AbstractVFSStructureDeployer;
import org.jboss.deployers.vfs.spi.structure.StructureContext;
import org.jboss.osgi.vfs.AbstractVFS;
import org.jboss.osgi.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.osgi.framework.Constants;

/**
 * Determine the structure of a Bundle deployment.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 16-Apr-2009
 */
public class OSGiBundleStructureDeployer extends AbstractVFSStructureDeployer
{
   /**
    * Sets the default relative order.
    */
   public OSGiBundleStructureDeployer()
   {
      // WARStructure:  1000
      // JARStructure: 10000
      setRelativeOrder(500);
   }

   @Override
   public boolean determineStructure(StructureContext structureContext) throws DeploymentException
   {
      ContextInfo context = null;
      VirtualFile root = structureContext.getRoot();
      
      try
      {
         // This file is not for me, because I'm only interested
         // in root deployments that contain a MANIFEST.MF
         Manifest manifest = VFSUtils.getManifest(AbstractVFS.adapt(root));
         if (root != structureContext.getFile() || manifest == null)
            return false;

         // This file is also not for me, because I need to see Bundle-SymbolicName
         Attributes attribs = manifest.getMainAttributes();
         String symbolicName = attribs.getValue(Constants.BUNDLE_SYMBOLICNAME);
         if (symbolicName == null)
            return false;

         // Create a context for this jar file with META-INF as the location for metadata
         context = createContext(structureContext, "META-INF");

         // Add a classpath entry for every Bundle-ClassPath element
         String classPath = attribs.getValue(Constants.BUNDLE_CLASSPATH);
         if (classPath == null)
         {
            // No Bundle-ClassPath, just add the root
            addClassPath(structureContext, root, true, false, context);
         }
         else
         {
            String[] classPathArr = classPath.split(",");
            for (String path : classPathArr)
            {
               path = path.trim();
               if (path.length() > 0)
               {
                  // The Framework must ignore any unrecognized parameters
                  int semicolon = path.indexOf(';');
                  if (semicolon > 0)
                     path = path.substring(0, semicolon);
                  
                  if (path.equals("."))
                  {
                     // Add the root
                     addClassPath(structureContext, root, true, false, context);
                  }
                  else
                  {
                     // [TODO] publish a Framework Event of type INFO
                     // [TODO] locate the class path entry in attached fragments
                     VirtualFile child = root.getChild(path);
                     addClassPath(structureContext, child, true, false, context);
                  }
               }
            }
         }

         // We don't process children as potential subdeployments

         return true;
      }
      catch (Exception e)
      {
         // Remove the invalid context
         if (context != null)
            structureContext.removeChild(context);

         throw DeploymentException.rethrowAsDeploymentException("Error determining structure: " + root.getName(), e);
      }
   }
}