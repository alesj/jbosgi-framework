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
import java.net.URI;
import java.net.URL;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.bundle.OSGiBundleState;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.plugins.context.AbstractVFSContext;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * Bundle vfs context.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class BundleVFSContext extends AbstractVFSContext
{
   private String name;
   private VirtualFileHandler root;

   public BundleVFSContext(URI rootURI, OSGiBundleManager manager) throws IOException
   {
      super(rootURI);

      name = parseName(rootURI);

      OSGiBundleState bundleState = getBundleState(rootURI, manager);
      String path = parsePath(rootURI);
      URL resource = bundleState.getEntry(path); // permission check
      if (resource == null)
         throw new IllegalArgumentException("No such resource: " + path + " in bundle: " + bundleState);

      DeploymentUnit unit = bundleState.getDeploymentUnit();
      if (unit instanceof VFSDeploymentUnit == false)
         throw new IllegalArgumentException("Cannot handle non VFS deployments: " + unit);

      VFSDeploymentUnit vdu = VFSDeploymentUnit.class.cast(unit);
      VirtualFile duRoot = vdu.getRoot();
      VirtualFile duFile = vdu.getFile(path); // should exist, resource != null
      root = new BundleHandler(this, duRoot, duFile, bundleState);
   }

   /**
    * Parse context name from uri.
    *
    * @param uri the uri
    * @return parsed context's name
    */
   protected String parseName(URI uri)
   {
      return uri.getHost();
   }

   /**
    * Parse resource path from uri.
    *
    * @param uri the uri
    * @return parsed resource path
    */
   protected String parsePath(URI uri)
   {
      String path = uri.getPath();
      if (path == null)
         path = "";

      return path;
   }

   /**
    * Get bundle state.
    *
    * @param uri the uri
    * @param manager the osgi manager
    * @return bundle state or exception if no such bundle exists
    */
   protected OSGiBundleState getBundleState(URI uri, OSGiBundleManager manager)
   {
      String host = uri.getHost();
      long id = Long.parseLong(host);
      if (id == 0)
         throw new IllegalArgumentException("Cannot handle system bundle, it's too abstract.");

      AbstractBundleState abs = manager.getBundleById(id);
      if (abs == null)
         throw new IllegalArgumentException("No such bundle: " + id);

      return OSGiBundleState.class.cast(abs); // should be able to cast, as it's not system
   }

   public String getName()
   {
      return name;
   }

   public VirtualFileHandler getRoot() throws IOException
   {
      return root;
   }
}
