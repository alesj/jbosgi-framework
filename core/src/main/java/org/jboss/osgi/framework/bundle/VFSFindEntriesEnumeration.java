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
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.VirtualFileVisitor;
import org.jboss.virtual.VisitorAttributes;
import org.jboss.virtual.plugins.vfs.helpers.MatchAllVirtualFileFilter;

/**
 * VFSFindEntriesEnumeration.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
class VFSFindEntriesEnumeration implements Enumeration<URL>
{
   /** The paths */
   private Iterator<URL> paths;

   /**
    * Convert file pattern (RFC 1960-based Filter) into a RegEx pattern
    * 
    * @param filePattern the file pattern
    * @return Regular expressions pattern
    */
   private static Pattern convertToPattern(String filePattern)
   {
      filePattern = filePattern.replace("*", ".*");
      return Pattern.compile("^" + filePattern + "$");
   }

   /**
    * Create a new VFSFindEntriesEnumeration.
    * 
    * @param root the root file
    * @param file the file to enumerate
    * @param filePattern the file pattern
    * @param recurse whether to recurse
    * @throws IOException for any error
    */
   public VFSFindEntriesEnumeration(VirtualFile root, VirtualFile file, String filePattern, boolean recurse) throws IOException
   {
      if (root == null)
         throw new IllegalArgumentException("Null root");
      if (file == null)
         throw new IllegalArgumentException("Null file");

      String rootPath = root.getPathName();
      VisitorAttributes attributes = new VisitorAttributes();
      attributes.setIncludeRoot(false);
      attributes.setLeavesOnly(true);
      if (recurse)
         attributes.setRecurseFilter(MatchAllVirtualFileFilter.INSTANCE);
      VisitorImpl visitor = new VisitorImpl(rootPath, filePattern, attributes);
      file.visit(visitor);
      this.paths = visitor.paths.iterator();
   }

   public boolean hasMoreElements()
   {
      return paths.hasNext();
   }

   public URL nextElement()
   {
      return paths.next();
   }
   
   static class VisitorImpl implements VirtualFileVisitor
   {
      ArrayList<URL> paths = new ArrayList<URL>();

      Pattern filter;
      String rootPath;
      VisitorAttributes attributes;
      
      VisitorImpl(String rootPath, String filter, VisitorAttributes attributes)
      {
         this.rootPath = rootPath;
         this.filter = convertToPattern(filter);
         this.attributes = attributes;
      }

      public VisitorAttributes getAttributes()
      {
         return attributes;
      }

      public void visit(VirtualFile virtualFile)
      {
         // See if the filter matches
         Matcher matcher = filter.matcher(virtualFile.getName());
         if (matcher.find() == false)
            return;
         
         try
         {
            paths.add(virtualFile.toURL());
         }
         catch (Exception e)
         {
            throw new RuntimeException("Error visiting " + virtualFile, e);
         }
      }
   }
}
