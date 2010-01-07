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
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.osgi.spi.NotImplementedException;
import org.osgi.framework.BundleException;

/**
 * OSGiSystemBundle.
 * 
 * @author Thomas.Diesler@jboss.com
 * @since 25-Dec-2009
 */
public class OSGiFragmentState extends AbstractDeployedBundleState
{
   /**
    * Create a new OSGiFragmentState
    */
   public OSGiFragmentState(DeploymentUnit unit)
   {
      super(unit);
   }

   public boolean isFragment()
   {
      return true;
   }
   
   @SuppressWarnings("rawtypes")
   public Enumeration findEntries(String path, String filePattern, boolean recurse)
   {
      throw new NotImplementedException();
   }

   public URL getEntry(String path)
   {
      throw new NotImplementedException();
   }

   @SuppressWarnings("rawtypes")
   public Enumeration getEntryPaths(String path)
   {
      throw new NotImplementedException();
   }

   public URL getResource(String name)
   {
      throw new NotImplementedException();
   }

   @SuppressWarnings("rawtypes")
   public Enumeration getResources(String name) throws IOException
   {
      throw new NotImplementedException();
   }

   @SuppressWarnings("rawtypes")
   public Class loadClass(String name) throws ClassNotFoundException
   {
      throw new NotImplementedException();
   }

   public void start(int options) throws BundleException
   {
      throw new BundleException("Cannot start fragment bundle: " + this);
   }

   public void stop(int options) throws BundleException
   {
      throw new BundleException("Cannot stop fragment bundle: " + this);
   }

   public void update() throws BundleException
   {
      throw new NotImplementedException();
   }

   public void update(InputStream input) throws BundleException
   {
      throw new NotImplementedException();
   }
}
