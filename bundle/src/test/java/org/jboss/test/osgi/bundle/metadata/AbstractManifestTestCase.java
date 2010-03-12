/*
* JBoss, Home of Professional Open Source
* Copyright 2006, JBoss Inc., and individual contributors as indicated
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
package org.jboss.test.osgi.bundle.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.Manifest;

import org.jboss.osgi.testing.OSGiRuntimeHelper;
import org.jboss.test.BaseTestCase;

/**
 * Uses Manifest.mf file for actual tests.
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 * @author Thomas.Diesler@jboss.com
 */
public abstract class AbstractManifestTestCase extends BaseTestCase
{
   protected AbstractManifestTestCase(String name)
   {
      super(name);
   }

   protected String createName(String prefix)
   {
      if (prefix == null)
         prefix = "";
      return "bundles/metadata/" + prefix + "Manifest.mf";
   }

   protected Manifest getManifest(String name) throws IOException
   {

      InputStream is = getManifestInputStream(name);
      try
      {
         return new Manifest(is);
      }
      finally
      {
         is.close();
      }
   }

   protected InputStream getManifestInputStream(String name) throws IOException
   {
      URL url = new OSGiRuntimeHelper().getResourceURL(name);
      if (url == null)
         fail(name + " not found");
      return url.openStream();
   }
}
