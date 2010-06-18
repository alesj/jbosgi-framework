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
package org.jboss.test.osgi.launch;

// $Id$

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStreamReader;

import org.jboss.osgi.framework.launch.OSGiFrameworkFactory;
import org.jboss.osgi.spi.util.ConstantsHelper;
import org.jboss.osgi.spi.util.ServiceLoader;
import org.junit.Assume;
import org.junit.Test;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

/**
 * Test OSGi System bundle access
 * 
 * [JBOSGI-316] Verify integrity of the jboss-osgi-framework-all.jar
 * 
 * @author thomas.diesler@jboss.com
 * @since 27-Jul-2009
 */
public class FrameworkLaunchTestCase
{
   @Test
   public void testFrameworkLaunch() throws Exception
   {
      FrameworkFactory factory = ServiceLoader.loadService(FrameworkFactory.class);
      Framework framework = factory.newFramework(null);

      assertEquals("BundleId == 0", 0, framework.getBundleId());
      assertEquals("SymbolicName", "system.bundle", framework.getSymbolicName());

      String state = ConstantsHelper.bundleState(framework.getState());
      assertEquals("INSTALLED", state);

      framework.init();

      state = ConstantsHelper.bundleState(framework.getState());
      assertEquals("STARTING", state);

      framework.start();

      state = ConstantsHelper.bundleState(framework.getState());
      assertEquals("ACTIVE", state);
      
      // This method is expected to return immediately
      // The stop process is started by another thread
      framework.stop();
      
      state = ConstantsHelper.bundleState(framework.getState());
      assertEquals("ACTIVE", state);
      
      framework.waitForStop(2000);
      
      state = ConstantsHelper.bundleState(framework.getState());
      assertEquals("RESOLVED", state);
   }

   @Test
   public void testFrameworkAllLaunch() throws Exception
   {
      // Get the aggregated jboss-osgi-framework-all.jar
      File[] files = new File("./target").listFiles(new FilenameFilter()
      {
         public boolean accept(File dir, String name)
         {
            return name.startsWith("jboss-osgi-framework-") && name.endsWith("-all.jar");
         }
      });
      
      // Assume that the jboss-osgi-framework-all.jar exists
      Assume.assumeTrue(files.length == 1);
      
      // Run the java command
      String alljar = files[0].getAbsolutePath();
      String cmd = "java -cp " + alljar + " " + OSGiFrameworkFactory.class.getName();
      Process proc = Runtime.getRuntime().exec(cmd);
      int exitValue = proc.waitFor();
      
      // Delete/move the jboss-osgi-framework.log
      File logfile = new File("./generated/jboss-osgi-framework.log");
      if (logfile.exists())
      {
         File logdir = logfile.getParentFile();
         File targetdir = new File("./target");
         if (targetdir.exists())
            logfile.renameTo(new File("./target/framework-jbosgi316.log"));
         else
            logfile.delete();
         
         logdir.delete();
      }
      
      // Generate the error message and fail
      if (exitValue != 0)
      {
         StringBuffer failmsg = new StringBuffer("Error running command: " + cmd + "\n");
         BufferedReader errReader = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
         String line = errReader.readLine();
         while(line != null)
         {
            failmsg.append("\n" + line);
            line = errReader.readLine();
         }
         
         fail(failmsg.toString());
      }
   }
}