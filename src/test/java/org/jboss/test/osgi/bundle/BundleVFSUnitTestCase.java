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
package org.jboss.test.osgi.bundle;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Test;

import org.jboss.osgi.framework.vfs.BundleVFSContextFactory;
import org.jboss.test.osgi.FrameworkTest;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.VirtualFileFilter;
import org.jboss.virtual.VirtualFileVisitor;
import org.jboss.virtual.VisitorAttributes;
import org.osgi.framework.Bundle;

/**
 * BundleVFSUnitTestCase.
 *
 * TODO test security
 * TODO test fragments
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
public class BundleVFSUnitTestCase extends FrameworkTest
{
   private BundleVFSContextFactory factory;

   public static Test suite()
   {
      return suite(BundleVFSUnitTestCase.class);
   }

   public BundleVFSUnitTestCase(String name)
   {
      super(name);
   }

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();

      factory = new BundleVFSContextFactory(getBundleManager());
      factory.start();
   }

   @Override
   protected void tearDown() throws Exception
   {
      factory.stop();
      factory = null;

      super.tearDown();
   }

   public void testBasicOps() throws Exception
   {
      Bundle bundle = addBundle("/bundles/entries/", "entries-simple");
      try
      {
         URI uri = createURI(bundle, null);
         assertBundleByURI(uri, 0, true, "root.xml");

         uri = createURI(bundle, "root.xml");
         assertBundleByURI(uri, 0, false, null);         

         uri = createURI(bundle, "META-INF");
         assertBundleByURI(uri, -1, true, "MANIFEST.MF");

         uri = createURI(bundle, "META-INF/MANIFEST.MF");
         assertBundleByURI(uri, 0, false, null);
      }
      finally
      {
         uninstall(bundle);
      }
   }

   public void testBasicNavigation() throws Exception
   {
      Bundle bundle = addBundle("/bundles/entries/", "entries-simple");
      try
      {
         URI uri = createURI(bundle, null);
         VirtualFile root = VFS.getRoot(uri);
         assertNotNull(root);

         VirtualFile metainf = root.getChild("META-INF");
         assertBundleByFile(metainf, -1, true, "MANIFEST.MF");
         List<VirtualFile> children = metainf.getChildren();
         assertTrue(children != null && children.size() == 1);
         assertEquals(root, metainf.getParent());

         VirtualFile manifest = metainf.getChild("MANIFEST.MF");
         assertBundleByFile(manifest, 0, false, null);
      }
      finally
      {
         uninstall(bundle);
      }
   }

   public void testVisitor() throws Exception
   {
      Bundle bundle = addBundle("/bundles/entries/", "entries-simple");
      try
      {
         URI uri = createURI(bundle, null);
         VirtualFile root = VFS.getRoot(uri);
         assertNotNull(root);

         List<VirtualFile> children = root.getChildren(new VirtualFileFilter()
         {
            public boolean accepts(VirtualFile file)
            {
               return file.getPathName().contains("META-INF");
            }
         });
         assertTrue(children != null && children.size() == 1);

         children = root.getChildrenRecursively(new VirtualFileFilter()
         {
            public boolean accepts(VirtualFile file)
            {
               return file.getPathName().contains("META-INF");
            }
         });
         assertTrue(children != null && children.size() == 2);

         final AtomicInteger counter = new AtomicInteger(0);
         root.visit(new VirtualFileVisitor()
         {
            public VisitorAttributes getAttributes()
            {
               return VisitorAttributes.RECURSE_LEAVES_ONLY;
            }

            public void visit(VirtualFile file)
            {
               if (file.getName().equals("entry2.xml")) counter.incrementAndGet();
            }
         });
         assertEquals(2, counter.get());
      }
      finally
      {
         uninstall(bundle);
      }
   }

   protected URI createURI(Bundle bundle, String path) throws Exception
   {
      if (path == null)
         path = "";
      if (path != null && path.startsWith("/") == false)
         path = "/" + path;
      
      return new URI("bundle", Long.toString(bundle.getBundleId()), path, null);
   }

   protected void assertBundleByURI(URI uri, int available, boolean hasChildren, String path) throws Exception
   {
      VFS vfs = VFS.getVFS(uri);
      assertNotNull(vfs);
      VirtualFile file = vfs.getRoot();
      assertNotNull(file);

      URI bURI = file.toURI();
      assertEquals(uri, bURI);
      URL url = uri.toURL();
      assertEquals(url, file.toURL());

      assertBundleByFile(file, available, hasChildren, path);

      // check url
      file = VFS.getRoot(url);
      assertBundleByFile(file, available, hasChildren, path);
   }

   protected void assertBundleByFile(VirtualFile file, int available, boolean hasChildren, String path) throws IOException
   {
      long lastModified = file.getLastModified();
      assertTrue(lastModified >= 0);

      InputStream is = file.openStream();
      try
      {
         assertTrue(is.available() > available);
      }
      finally
      {
         is.close();
      }

      List<VirtualFile> children = file.getChildren();
      assertEquals(hasChildren, children != null && children.isEmpty() == false);

      String cp = path;
      if (cp == null)
         cp = "rubbish"; // :-)

      VirtualFile child = file.getChild(cp);
      assertEquals(path != null, child != null);

      if (child != null)
         assertEquals(file, child.getParent());
   }
}