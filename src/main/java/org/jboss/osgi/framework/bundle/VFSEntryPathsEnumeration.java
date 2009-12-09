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
package org.jboss.osgi.framework.bundle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import org.jboss.virtual.VirtualFile;

/**
 * VFSEntryPathsEnumeration.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
class VFSEntryPathsEnumeration implements Enumeration<String>
{
   /** The paths */
   private Iterator<String> paths;

   /**
    * Fix the path
    * 
    * @param file the file to fix
    * @param rootPath the root path
    * @return the fixxed path
    */
   private String fixPath(VirtualFile file, String rootPath)
   {
      try
      {
         String result = file.getPathName();
         int length = rootPath.length();
         if (length != 0)
            result = result.substring(length);
         if (file.isLeaf() == false && result.endsWith("/") == false)
            result += "/";
         return result;
      }
      catch (IOException e)
      {
         throw new RuntimeException("Error fixing path for " + file, e);
      }
   }
   
   /**
    * Create a new VFSEntryPathsEnumeration.
    * 
    * @param root the root file
    * @param file the file to enumerate
    * @throws IOException for any error
    */
   public VFSEntryPathsEnumeration(VirtualFile root, VirtualFile file) throws IOException
   {
      if (root == null)
         throw new IllegalArgumentException("Null root");
      if (file == null)
         throw new IllegalArgumentException("Null file");
      
      String rootPath = root.getPathName();
      ArrayList<String> paths = new ArrayList<String>();
      paths.add(fixPath(file, rootPath));
      
      List<VirtualFile> children = file.getChildrenRecursively();
      for (VirtualFile child : children)
         paths.add(fixPath(child, rootPath));
      this.paths = paths.iterator();
   }

   public boolean hasMoreElements()
   {
      return paths.hasNext();
   }

   public String nextElement()
   {
      return paths.next();
   }
}
