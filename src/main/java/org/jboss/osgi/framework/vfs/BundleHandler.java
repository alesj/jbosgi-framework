/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.osgi.framework.vfs;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.plugins.context.AbstractVirtualFileHandler;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VirtualFileHandler;
import org.jboss.deployers.vfs.spi.structure.helpers.AbstractStructureDeployer;
import org.osgi.framework.Bundle;

/**
 * Bundle handler.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
class BundleHandler extends AbstractVirtualFileHandler
{
   /** The serialVersionUID */
   private static final long serialVersionUID = 6650185906199900589L;

   private VirtualFile root; // bundle's root
   private VirtualFile file;
   private Bundle bundle;

   private volatile String relativePath;
   private volatile URI uri;
   private volatile VirtualFileHandler handler;

   public BundleHandler(VFSContext context, VirtualFile root, VirtualFile file, Bundle bundle) throws IOException
   {
      this(context, LazyVirtualFileHandler.create(file.getParent()), root, file, bundle);
   }

   private BundleHandler(VFSContext context, VirtualFileHandler parent, VirtualFile root, VirtualFile file, Bundle bundle) throws IOException
   {
      super(context, parent, file.getName());
      this.root = root;
      this.file = file;
      this.bundle = bundle;
   }

   protected VirtualFileHandler getHandler() throws IOException
   {
      if (handler == null)
         handler = LazyVirtualFileHandler.create(file);

      return handler;
   }

   protected String getRelativePath(boolean checkStart, boolean checkEnd)
   {
      if (relativePath == null)
         relativePath = AbstractStructureDeployer.getRelativePath(root, file);
      
      StringBuilder path = new StringBuilder(relativePath);
      if (checkStart && relativePath.startsWith("/") == false)
         path.insert(0, '/');
      if (checkEnd && relativePath.endsWith("/") == false)
         path.append('/');

      return path.toString();
   }

   protected VirtualFileHandler createChildHandler(VirtualFile child) throws IOException
   {
      return new BundleHandler(getVFSContext(), this, root, child, bundle);
   }

   public URI toURI() throws URISyntaxException
   {
      if (uri == null)
         uri = new URI("bundle", Long.toString(bundle.getBundleId()), getRelativePath(true, false), null);

      return uri;
   }

   public long getLastModified() throws IOException
   {
      return bundle.getLastModified();
   }

   public InputStream openStream() throws IOException
   {
      bundle.getResource(getRelativePath(false, false)); // permission check
      
      return file.openStream();
   }

   public List<VirtualFileHandler> getChildren(boolean ignoreErrors) throws IOException
   {
      bundle.findEntries(getRelativePath(false, false), null, false); // permission check

      List<VirtualFile> children = file.getChildren();
      if (children != null && children.isEmpty() == false)
      {
         List<VirtualFileHandler> handlers = new ArrayList<VirtualFileHandler>(children.size());
         for (VirtualFile child : children)
         {
            handlers.add(createChildHandler(child));
         }
         return handlers;
      }
      return Collections.emptyList();
   }

   public VirtualFileHandler getChild(String path) throws IOException
   {
      String fullPath = getRelativePath(false, true) + path; 
      URL entry = bundle.getEntry(fullPath); // permission check
      if (entry == null)
         return null;

      VirtualFile child = file.getChild(path); // the child should exist, since entry does
      return createChildHandler(child);
   }

   //---------------------------------------------------------

   @Override
   protected URL toInternalVfsUrl() throws MalformedURLException, URISyntaxException
   {
      return toURI().toURL();
   }

   public long getSize() throws IOException
   {
      return file.getSize();
   }

   public boolean exists() throws IOException
   {
      return file.exists();
   }

   public boolean isLeaf() throws IOException
   {
      return file.isLeaf();
   }

   public boolean isHidden() throws IOException
   {
      return file.isHidden();
   }

   public boolean removeChild(String name) throws IOException
   {
      return getHandler().removeChild(name);
   }

   public boolean isNested() throws IOException
   {
      return VFSUtils.isNestedFile(file);
   }
}